"""Browser regressions on a disposable local PostgreSQL schema (never production).

Requires a built JAR, initialized local Compose database, Python Playwright and
Chromium. --owner-file points to a private JSON {username,password} matching
the local owner's configuration. Credentials and application logs stay local.
"""
import argparse
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import time
import urllib.request
import uuid

from playwright.sync_api import expect, sync_playwright


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--owner-file', type=Path, required=True)
    parser.add_argument('--port', type=int, default=18096)
    parser.add_argument('--chromium', default=shutil.which('chromium'))
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    jar = root / 'target/nclean-crm-0.0.1-SNAPSHOT.jar'
    if not jar.is_file():
        parser.error('Build first with Maven Wrapper clean verify.')
    owner = json.loads(args.owner_file.read_text())
    schema = 'browser_qa_' + uuid.uuid4().hex
    run_dir = root / '.local' / schema
    run_dir.mkdir(parents=True)
    shutil.copy2(jar, run_dir / 'app.jar')  # Do not run a JAR that clean replaces.
    environment = os.environ.copy()
    for key in list(environment):
        if key.startswith(('CRM_', 'SPRING_', 'DOCKER_HOST', 'DOCKER_CONTEXT',
                           'DOCKER_TLS', 'DOCKER_CERT_PATH')):
            environment.pop(key)
    docker = ['docker']
    if os.name != 'nt':
        docker += ['--host=unix:///var/run/docker.sock']
    docker += ['compose', 'exec', '-T', 'postgres', 'psql', '-U',
               'crm_bootstrap', '-d', 'nclean_crm', '-v', 'ON_ERROR_STOP=1', '-c']

    def sql(statement):
        result = subprocess.run(docker + [statement], cwd=root, env=environment,
                                capture_output=True, text=True)
        if result.returncode:
            raise RuntimeError('Local QA schema operation failed. Check local Docker/PostgreSQL.')

    base = 'http://127.0.0.1:' + str(args.port)
    with socket.socket() as probe:
        probe.bind(('127.0.0.1', args.port))
    process = None
    created = False
    try:
        sql('CREATE SCHEMA ' + schema + ' AUTHORIZATION crm_dev')
        created = True
        java = str(Path(os.environ['JAVA_HOME']) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')) if os.environ.get('JAVA_HOME') else 'java'
        url = 'jdbc:postgresql://127.0.0.1:55433/nclean_crm?currentSchema=' + schema
        with (run_dir / 'app.log').open('wb') as log:
            process = subprocess.Popen([
                java, '-jar', str(run_dir / 'app.jar'),
                '--spring.profiles.active=local', '--server.address=127.0.0.1',
                '--server.port=' + str(args.port), '--spring.datasource.url=' + url,
                '--spring.flyway.url=' + url, '--spring.flyway.default-schema=' + schema,
                '--spring.flyway.schemas=' + schema,
            ], cwd=root, env=environment, stdout=log, stderr=subprocess.STDOUT)
        deadline = time.monotonic() + 180
        while time.monotonic() < deadline:
            if process.poll() is not None:
                raise RuntimeError('QA application stopped; inspect private .local/' + schema + '/app.log')
            try:
                with urllib.request.urlopen(base + '/login', timeout=1) as response:
                    if response.status == 200:
                        break
            except OSError:
                pass
            time.sleep(.2)
        else:
            raise RuntimeError('QA application did not become ready.')

        with sync_playwright() as p:
            launch = {'headless': True, 'args': ['--no-sandbox']}
            if args.chromium:
                launch['executable_path'] = args.chromium
            browser = p.chromium.launch(**launch)
            page = browser.new_page(viewport={'width': 390, 'height': 844})
            page.set_default_timeout(12000)
            errors = []
            page.on('pageerror', lambda e: errors.append(str(e)))
            page.goto(base + '/login')
            page.locator('[name=username]').fill(owner['username'])
            page.locator('[name=password]').fill(owner['password'])
            with page.expect_navigation():
                page.locator('button[type=submit]').click()
            page.goto(base + '/customers')

            for name, phone in [('Klient Alfa QA', '500000001'), ('Klient Beta QA', '500000002')]:
                page.get_by_text('Dodaj klienta', exact=True).first.click()
                form = page.locator('form[method=post][action="/customers"]').first
                form.locator('[name=name]').fill(name)
                form.locator('[name=phone]').fill(phone)
                with page.expect_navigation():
                    form.get_by_role('button', name='Dodaj klienta', exact=True).click()
            search = page.locator('form.search')
            search.locator('[name=query]').fill('Alfa')
            with page.expect_navigation():
                search.get_by_role('button', name='Szukaj', exact=True).click()
            expect(page.locator('main>article')).to_have_count(1)
            expect(page.locator('main>article')).to_contain_text('Klient Alfa QA')
            expect(page.locator('[name=query]')).to_have_value('Alfa')
            assert 'query=Alfa' in page.url
            page.reload()
            expect(page.locator('main>article')).to_have_count(1)
            print('PASS customer search and reload retain filter/results.', flush=True)

            page.goto(base + '/settings/costs')
            rows = page.locator('#cost-rows .cost-row')
            rows.first.get_by_role('button', name='Usuń', exact=True).click()
            page.locator('#add-cost').click()
            expect(page.locator('#cost-rows .cost-row:visible')).to_have_count(3)
            for _ in range(47):
                page.locator('#add-cost').click()
            expect(rows).to_have_count(50)
            page.locator('#add-cost').click()
            expect(page.locator('#add-cost')).to_be_disabled()
            rows.first.get_by_role('button', name='Usuń', exact=True).click()
            page.locator('#add-cost').click()
            expect(page.locator('#cost-rows .cost-row:visible')).to_have_count(50)
            expect(rows).to_have_count(50)
            print('PASS monthly cost rows can be removed/reused, including the 50-row limit.', flush=True)

            page.goto(base + '/customers')
            page.get_by_text('Dodaj klienta', exact=True).first.click()
            form = page.locator('form[method=post][action="/customers"]').first
            form.locator('[name=name]').fill('Próba ponowienia QA')
            form.locator('[name=phone]').fill('500000003')
            form.locator('[name=notes]').fill('Ten tekst ma pozostać po błędzie.')
            requests = []
            page.on('request', lambda r: requests.append(r.url) if r.method == 'POST' and r.url.endswith('/customers') else None)
            page.route('**/customers', lambda r: r.fulfill(status=503, body='Controlled QA error'))
            form.evaluate('f=>{f.dispatchEvent(new Event("submit",{bubbles:true,cancelable:true}));f.dispatchEvent(new Event("submit",{bubbles:true,cancelable:true}));}')
            expect(form.locator('.form-error')).to_contain_text('Zapis nie powiódł się')
            assert len(requests) == 1, 'Two concurrent submit events must issue one request.'
            expect(form.locator('[name=notes]')).to_have_value('Ten tekst ma pozostać po błędzie.')
            expect(form.get_by_role('button', name='Dodaj klienta', exact=True)).to_be_enabled()
            page.unroute('**/customers')
            with page.expect_navigation():
                form.get_by_role('button', name='Dodaj klienta', exact=True).click()
            expect(page.locator('main>article')).to_have_count(3)
            assert len(requests) == 2
            assert not errors, errors
            browser.close()
            print('PASS concurrent submission guard, preserved draft after503, successful retry; no JS errors.', flush=True)
    finally:
        if process and process.poll() is None:
            process.terminate()
            process.wait(timeout=30)
        if created:
            sql('DROP SCHEMA ' + schema + ' CASCADE')
        if process and process.returncode not in (None, 0, 143, -15):
            print('Private QA logs retained in .local/' + schema)


if __name__ == '__main__':
    main()
