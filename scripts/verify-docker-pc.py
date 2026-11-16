"""Linux release check: disposable Docker PC stack, real browser and PostgreSQL.

Requires both PC images built, Docker Compose >=2.24.4, Python
Playwright and Chromium. Never uses the PC or development installation's data.
"""
import base64
import hashlib
import json
import os
from pathlib import Path
import pty
import select
import shutil
import socket
import subprocess
import tempfile
import time
import uuid

from playwright.sync_api import expect, sync_playwright

ROOT = Path(__file__).resolve().parents[1]
DOCKER = ['docker', '--host=unix:///var/run/docker.sock']
TABLES = ['flyway_schema_history', 'district', 'day_district', 'crm_customer',
          'booking', 'booking_item', 'booking_photo', 'fuel_receipt', 'fuel_purchase', 'fuel_baseline']


def main():
    suffix = uuid.uuid4().hex[:12]
    project = 'ncleancrm-pc-qa-' + suffix
    volume = project + '-data'
    password = 'QA-' + uuid.uuid4().hex
    environment = os.environ.copy()
    for key in list(environment):
        if key.startswith(('CRM_', 'SPRING_', 'DOCKER_HOST', 'DOCKER_CONTEXT',
                           'DOCKER_TLS', 'DOCKER_CERT_PATH', 'COMPOSE_')):
            environment.pop(key)
    environment.update(CRM_PC_UID=str(os.getuid()), CRM_PC_GID=str(os.getgid()),
                       CRM_PC_VERSION='local')
    with socket.socket() as probe:
        probe.bind(('127.0.0.1', 0))
        port = probe.getsockname()[1]

    with tempfile.TemporaryDirectory(prefix='docker-pc-qa-', dir=ROOT / '.local') as directory:
        work = Path(directory)
        secrets = work / 'secrets'
        backups = work / 'backups'
        secrets.mkdir(mode=0o700)
        backups.mkdir(mode=0o700)
        override = work / 'override.yaml'
        override.write_text('''services:
  app:
    ports: !override ["127.0.0.1:%s:8081"]
  init:
    volumes:
      - %s
  ops:
    volumes:
      - %s
volumes:
  pc-data:
    name: %s
secrets:
  app_config:
    file: %s
  crm_db_password:
    file: %s
  postgres_admin_password:
    file: %s
''' % (port, json.dumps(str(secrets) + ':/workspace/.local'),
       json.dumps(str(backups) + ':/backups'), volume,
       json.dumps(str(secrets / 'application-local.properties')),
       json.dumps(str(secrets / 'postgres-password')),
       json.dumps(str(secrets / 'postgres-admin-password'))))
        compose = DOCKER + ['compose', '-p', project, '-f', str(ROOT / 'compose.pc.yaml'),
                            '-f', str(override)]

        def run(arguments, ok=True):
            result = subprocess.run(arguments, cwd=ROOT, env=environment,
                                    stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
            if ok and result.returncode:
                raise RuntimeError(result.stdout[-6000:])
            if not ok:
                assert result.returncode != 0, 'Unsafe operation unexpectedly succeeded'
            return result.stdout

        def sql(statement, database='nclean_crm'):
            return run(compose + ['exec', '-T', 'postgres', 'psql', '-U', 'crm_bootstrap',
                                  '-d', database, '-At', '-v', 'ON_ERROR_STOP=1', '-c', statement]).strip()

        def fingerprint(database='nclean_crm'):
            return {table: sql("SELECT count(*)||':'||md5(coalesce(string_agg(t::text,'' ORDER BY t::text),'')) FROM "
                               + table + ' t', database) for table in TABLES}

        def interactive_init():
            primary, terminal = pty.openpty()
            process = subprocess.Popen(compose + ['--profile', 'tools', 'run', '--rm', '--no-deps', '-it', 'init'],
                                       cwd=ROOT, env=environment, stdin=terminal, stdout=terminal, stderr=terminal)
            os.close(terminal)
            output = b''
            prompts = [b'Password for local-owner', b'Repeat password:']
            deadline = time.monotonic() + 90
            try:
                while process.poll() is None:
                    if time.monotonic() > deadline:
                        process.kill()
                        raise RuntimeError('Interactive initializer timeout')
                    if select.select([primary], [], [], 0.2)[0]:
                        try:
                            output += os.read(primary, 65536)
                        except OSError:
                            break
                        if prompts and prompts[0] in output:
                            output = b''
                            prompts.pop(0)
                            os.write(primary, password.encode() + b'\n')
                assert process.wait(timeout=10) == 0, output.decode(errors='replace').replace(password, '[redacted]')
                assert not prompts
            finally:
                os.close(primary)
                if process.poll() is None:
                    process.kill()
                    process.wait()

        run(DOCKER + ['volume', 'create', volume])
        try:
            interactive_init()
            original_secrets = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in secrets.iterdir()}
            run(compose + ['--profile', 'tools', 'run', '--rm', '--no-deps', '-T', 'init'], ok=False)
            assert original_secrets == {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in secrets.iterdir()}
            print('PASS interactive initializer and refusal to overwrite secrets', flush=True)
            run(compose + ['up', '-d', '--no-build', '--wait', '--wait-timeout', '180'])
            assert sql('SELECT count(*) FROM flyway_schema_history WHERE success') == '20'
            assert not json.loads(run(DOCKER + ['inspect', project + '-postgres-1']))[0]['HostConfig']['PortBindings']
            assert json.loads(run(DOCKER + ['inspect', project + '-app-1']))[0]['HostConfig']['ReadonlyRootfs']

            base = 'http://127.0.0.1:' + str(port)
            png = base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAIAAAACUFjqAAAAFUlEQVR4nGPccfcXA27AhEduBEsDACFJAqNlg1pJAAAAAElFTkSuQmCC')
            with sync_playwright() as playwright:
                browser = playwright.chromium.launch(headless=True, executable_path=shutil.which('chromium'))
                page = browser.new_page(viewport={'width': 390, 'height': 844})
                assert page.request.get(base + '/calendar', max_redirects=0).status == 302
                page.goto(base + '/login')
                page.locator('[name=username]').fill('local-owner')
                page.locator('[name=password]').fill(password)
                page.get_by_role('button', name='Zaloguj się', exact=True).click()
                expect(page).to_have_url(base + '/calendar')
                assert page.request.post(base + '/orders', form={'day': '2026-11-15'}).status == 403
                for width in (360, 390, 768, 1440):
                    page.set_viewport_size({'width': width, 'height': 844})
                    for route in ('/calendar', '/dashboard', '/customers', '/settings'):
                        page.goto(base + route)
                        assert page.evaluate('document.documentElement.scrollWidth <= innerWidth'), (width, route)
                page.goto(base + '/calendar?add=true')
                form = page.locator('#booking-form')
                form.locator('[data-category-choice="1"]').click()
                form.locator('[name=qty_2]').fill('1')
                form.locator('[name=name]').fill('Klient syntetyczny Docker QA')
                form.locator('[name=phone]').fill('500000099')
                form.locator('[name=address]').fill('Adres testowy Docker QA')
                form.get_by_role('button', name='Zapisz zlecenie', exact=True).click()
                expect(page.locator('#booking-editor')).not_to_be_visible()
                booking = page.locator('#agenda-orders [data-order]').filter(has_text='Klient syntetyczny Docker QA').get_attribute('data-order')
                page.goto(base + '/work/' + booking)
                page.locator('#photos>summary').click()
                page.locator('[data-service-files]').first.set_input_files({'name': 'qa.png', 'mimeType': 'image/png', 'buffer': png})
                page.get_by_role('button', name='Dodaj zdjęcia', exact=True).click()
                expect(page.locator('#photos .photo-status').first).to_contain_text('Zapisano')
                assert sql('SELECT count(*) FROM booking_photo') == '1'
                # A synthetic binary receipt, independent of the service-photo upload.
                sql("INSERT INTO fuel_receipt(id,image) VALUES ('" + str(uuid.uuid4()) + "',decode('" + png.hex() + "','hex'))")
                sql("INSERT INTO fuel_baseline(target,started_at,reading,request_token,updated_by) VALUES ('GENERATOR',TIMESTAMP '2026-10-01 08:00',100,'" + str(uuid.uuid4()) + "','docker-qa')")
                sql("INSERT INTO fuel_purchase(request_token,purchased_at,target,fuel_type,amount,litres,litre_price,full_tank,generator_meter,records_complete,metered_full) VALUES ('" + str(uuid.uuid4()) + "',TIMESTAMP '2026-10-12 12:00','GENERATOR','PETROL',50,10,5,TRUE,110,TRUE,TRUE)")
                saved = fingerprint()
                saved_image = bytes.fromhex(sql("SELECT encode(image,'hex') FROM booking_photo LIMIT 1"))
                run(compose + ['stop'])
                run(compose + ['up', '-d', '--no-build', '--wait', '--wait-timeout', '180'])
                assert fingerprint() == saved
                run(compose + ['up', '-d', '--no-build', '--force-recreate', '--wait', '--wait-timeout', '180'])
                assert fingerprint() == saved
                # A fresh browser session after recreation must log in again.
                page.context.clear_cookies()
                page.goto(base + '/login')
                page.locator('[name=username]').fill('local-owner')
                page.locator('[name=password]').fill(password)
                page.get_by_role('button', name='Zaloguj się', exact=True).click()
                page.goto(base + '/work/' + booking)
                expect(page.locator('body')).to_contain_text('Klient syntetyczny Docker QA')
                photo_id = sql('SELECT id FROM booking_photo LIMIT 1')
                photo = page.request.get(base + '/photos/' + photo_id)
                assert photo.status == 200 and photo.body() == saved_image
                browser.close()
            print('PASS login, CSRF, four widths, order/photo save, restart and forced recreation with binary data intact', flush=True)
            run(compose + ['--profile', 'tools', 'run', '--rm', 'ops', 'backup'])
            backup = next(backups.glob('*.dump'))
            target = 'nclean_restore_' + suffix
            restore = compose + ['--profile', 'tools', 'run', '--rm', '-e', 'BACKUP_FILE=' + backup.name,
                                 '-e', 'RESTORE_DATABASE=' + target, 'ops', 'restore-test']
            run(restore)
            assert fingerprint(target) == saved
            assert 'already exists' in run(restore, ok=False)
            assert 'not empty' in run(compose + ['--profile', 'tools', 'run', '--rm', '-e', 'BACKUP_FILE=' + backup.name, 'ops', 'import-empty'], ok=False)
            run(compose + ['down'])
            run(DOCKER + ['volume', 'inspect', volume])
            # A second, empty volume proves migration of existing data without
            # ever dropping tables or writing over a populated CRM database.
            import_volume = volume + '-import'
            run(DOCKER + ['volume', 'create', import_volume])
            override.write_text(override.read_text().replace('name: ' + volume, 'name: ' + import_volume))
            run(compose + ['up', '-d', '--no-build', '--wait', '--wait-timeout', '180', 'postgres'])
            run(compose + ['--profile', 'tools', 'run', '--rm', '-e', 'BACKUP_FILE=' + backup.name, 'ops', 'import-empty'])
            run(compose + ['up', '-d', '--no-build', '--wait', '--wait-timeout', '180', 'app'])
            assert fingerprint() == saved
            with backup.open('ab') as stream:
                stream.write(b'corrupt')
            assert 'checksum mismatch' in run(restore, ok=False)
            assert fingerprint() == saved
            run(compose + ['down'])
            run(DOCKER + ['volume', 'inspect', volume])
            run(DOCKER + ['volume', 'inspect', import_volume])
            print('PASS backup/restore binary fingerprints, import into empty PC, existing target/import/corruption refusal and external volumes survive down', flush=True)
        except Exception:
            print(run(compose + ['logs', '--tail', '40', 'app', 'postgres']))
            raise
        finally:
            subprocess.run(compose + ['down', '--remove-orphans'], cwd=ROOT, env=environment,
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            # Only the random volume created by this invocation is removed.
            subprocess.run(DOCKER + ['volume', 'rm', volume], env=environment,
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            subprocess.run(DOCKER + ['volume', 'rm', volume + '-import'], env=environment,
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


if __name__ == '__main__':
    main()
