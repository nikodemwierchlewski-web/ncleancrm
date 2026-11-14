#!/usr/bin/env python3
"""Conservative Linux operations. Never delete a database/volume or restore over one."""
import argparse
import contextlib
import datetime
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import urllib.request
import zipfile

IDENTIFIER = re.compile(r'^[a-z][a-z0-9_]{0,62}$')


def run(argv, env=None):
    # Arguments contain identifiers and paths, never passwords; libpq reads its service/passfile.
    return subprocess.run(argv, check=True, env=env, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout


def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


class Operations:
    def __init__(self, config):
        self.config = config
        self.base = Path(config['base_dir'])
        self.backups = Path(config['backup_dir'])
        for path in (self.base, self.backups):
            if not path.is_absolute() or len(path.parts) < 3:
                raise ValueError('Use dedicated absolute application and backup directories.')
        self.database = config['expected_database']
        if not IDENTIFIER.fullmatch(self.database) or not self.database.startswith('nclean_crm'):
            raise ValueError('This tool is restricted to a separate nclean_crm database.')
        for key in ('db_service', 'restore_service', 'restore_owner'):
            if not IDENTIFIER.fullmatch(config[key]):
                raise ValueError('Invalid service/role identifier.')
        self.env = dict(os.environ, PGSERVICE=config['db_service'])
        self.restore_env = dict(os.environ, PGSERVICE=config['restore_service'])

    def guard(self):
        actual = run(['psql', '-X', '-A', '-t', '-v', 'ON_ERROR_STOP=1', '-c', 'SELECT current_database()'], self.env).strip()
        if actual != self.database:
            raise ValueError('Connected database differs from expected_database; no operation performed.')

    @contextlib.contextmanager
    def lock(self):
        self.backups.mkdir(parents=True, exist_ok=True, mode=0o700)
        with (self.backups / '.operations.lock').open('a') as stream:
            fcntl.flock(stream, fcntl.LOCK_EX | fcntl.LOCK_NB)
            yield

    def backup(self):
        self.guard()
        stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
        target = self.backups / ('nclean-' + stamp + '.dump')
        partial = target.with_suffix('.partial')
        try:
            # O_EXCL plus mode 0600: never overwrite a previous backup.
            with partial.open('xb') as stream:
                os.chmod(partial, 0o600)
                subprocess.run(['pg_dump', '-Fc', '--no-owner', '--no-acl'], env=self.env,
                               stdout=stream, stderr=subprocess.PIPE, check=True)
                stream.flush()
                os.fsync(stream.fileno())
            run(['pg_restore', '--list', str(partial)], self.env)
            if partial.stat().st_size == 0:
                raise ValueError('Empty backup.')
            checksum = digest(partial)
            os.link(partial, target)
            metadata = {'format': 1, 'database': self.database, 'created_utc': stamp,
                        'file': target.name, 'bytes': target.stat().st_size, 'sha256': checksum,
                        'includes': 'All database tables, binary service photos and receipt images'}
            with target.with_suffix('.json').open('x') as stream:
                json.dump(metadata, stream, indent=2)
            os.chmod(target.with_suffix('.json'), 0o600)
            print(json.dumps({'backup': str(target), 'sha256': checksum}))
            return target
        finally:
            partial.unlink(missing_ok=True)

    def restore_check(self, backup, target):
        if not re.fullmatch(r'nclean_restore_[a-z0-9_]{1,40}', target):
            raise ValueError('Restore target must be a NEW nclean_restore_* database.')
        metadata = json.loads(backup.with_suffix('.json').read_text())
        if metadata['database'] != self.database or metadata['sha256'] != digest(backup):
            raise ValueError('Backup identity/checksum mismatch. No database created.')
        # createdb fails on an existing target. No --clean and no DROP anywhere.
        run(['createdb', '--maintenance-db=' + self.database, '--owner=' + self.config['restore_owner'], target], self.restore_env)
        run(['pg_restore', '--exit-on-error', '--single-transaction', '--no-owner', '--no-acl',
             '--role=' + self.config['restore_owner'], '--dbname=' + target, str(backup)], self.restore_env)
        result = run(['psql', '-X', '-A', '-t', '--dbname=' + target, '-v', 'ON_ERROR_STOP=1', '-c', """
            SELECT json_build_object(
              'orders',(SELECT count(*) FROM booking),
              'service_photos',(SELECT count(*) FROM booking_photo),
              'photo_digest',(SELECT md5(COALESCE(string_agg(id::text||':'||md5(image),',' ORDER BY id),'')) FROM booking_photo),
              'receipts',(SELECT count(*) FROM fuel_receipt),
              'receipt_digest',(SELECT md5(COALESCE(string_agg(id::text||':'||md5(image),',' ORDER BY id),'')) FROM fuel_receipt),
              'migrations',(SELECT count(*) FROM flyway_schema_history WHERE success AND type='SQL'))
        """], self.restore_env)
        print(json.dumps({'restored_database': target, 'verification': json.loads(result)}))

    def update(self, jar, release, checksum):
        if not re.fullmatch(r'[0-9a-f]{7,40}', release) or digest(jar) != checksum:
            raise ValueError('Release identifier or JAR SHA-256 mismatch.')
        with zipfile.ZipFile(jar) as archive:
            if 'BOOT-INF/classes/pl/nclean/crm/CrmApplication.class' not in archive.namelist():
                raise ValueError('Not an executable ncLEAN CRM JAR.')
        current = self.base / 'current'
        releases = self.base / 'releases'
        previous = current.resolve(strict=True)
        if not current.is_symlink() or previous.parent != releases.resolve() or not (previous / 'nclean-crm.jar').is_file():
            raise ValueError('Existing installation identity is invalid. Initial installation is separate.')
        destination = releases / release
        if destination.exists():
            if not (destination / 'nclean-crm.jar').is_file() or digest(destination / 'nclean-crm.jar') != checksum:
                raise ValueError('Existing release differs; it will never be overwritten.')
        else:
            destination.mkdir(mode=0o750)
            shutil.copyfile(jar, destination / 'nclean-crm.jar')
            os.chmod(destination / 'nclean-crm.jar', 0o640)
            if os.geteuid() == 0:
                group = releases.stat().st_gid
                os.chown(destination, -1, group)
                os.chown(destination / 'nclean-crm.jar', -1, group)
        if digest(destination / 'nclean-crm.jar') != checksum:
            raise ValueError('Copied JAR checksum mismatch.')
        self.guard()
        run(['systemctl', 'stop', 'nclean-crm'])
        try:
            backup = self.backup()
        except Exception:
            # No new application/migration has run, so restarting the previous version is safe.
            run(['systemctl', 'start', 'nclean-crm'])
            raise
        link = self.base / ('.current-' + release)
        link.symlink_to(destination)
        os.replace(link, current)
        try:
            run(['systemctl', 'start', 'nclean-crm'])
            url = self.config['health_url']
            from urllib.parse import urlparse
            if urlparse(url).hostname not in ('127.0.0.1', 'localhost', '::1'):
                raise ValueError('Health check must use the private loopback listener.')
            deadline = time.monotonic() + int(self.config.get('health_timeout', 90))
            while time.monotonic() < deadline:
                try:
                    with urllib.request.urlopen(url, timeout=3) as response:
                        if response.status == 200 and response.geturl() == url and 'ncLEAN' in response.read(65536).decode('utf-8'):
                            print(json.dumps({'updated': release, 'previous_release': previous.name, 'backup': str(backup)}))
                            return
                except (OSError, UnicodeError):
                    pass
                time.sleep(1)
            raise ValueError('New version did not pass readiness check.')
        except Exception:
            run(['systemctl', 'stop', 'nclean-crm'])
            # Database may already be migrated. Do not automatically start old code or restore over new writes.
            raise RuntimeError('Update stopped. Keep both releases and the backup; inspect migrations before recovery.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=Path, required=True)
    sub = parser.add_subparsers(dest='command', required=True)
    sub.add_parser('backup')
    restore = sub.add_parser('restore-check')
    restore.add_argument('--backup', type=Path, required=True)
    restore.add_argument('--database', required=True)
    update = sub.add_parser('update')
    update.add_argument('--jar', type=Path, required=True)
    update.add_argument('--release', required=True)
    update.add_argument('--sha256', required=True)
    args = parser.parse_args()
    os.umask(0o077)
    operations = Operations(json.loads(args.config.read_text()))
    with operations.lock():
        if args.command == 'backup':
            operations.backup()
        elif args.command == 'restore-check':
            operations.restore_check(args.backup, args.database)
        else:
            operations.update(args.jar, args.release, args.sha256)


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        # Do not dump libpq diagnostics/connection strings or credentials into shared logs.
        print('Operation failed: ' + (str(error) if not isinstance(error, subprocess.CalledProcessError)
                                     else 'database/service command failed; inspect protected administrator logs.'), file=sys.stderr)
        sys.exit(1)
