"""Acceptance checks for the isolated, memory-only equipment HTML preview.
Run: python scripts/verify-equipment-preview.py
Requires Python Playwright and Chromium (CHROMIUM_PATH overrides /usr/bin/chromium).
No Spring application or database is started.
"""
import os
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from threading import Thread
from pathlib import Path
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
class QuietHandler(SimpleHTTPRequestHandler):
    def log_message(self, *args):
        pass

server = ThreadingHTTPServer(('127.0.0.1', 0), partial(QuietHandler, directory=str(ROOT)))
Thread(target=server.serve_forever, daemon=True).start()
URL = f'http://127.0.0.1:{server.server_port}/preview/nclean-equipment.html'

with sync_playwright() as p:
    browser = p.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH', '/usr/bin/chromium'), args=['--no-sandbox'])
    page = browser.new_page(viewport={'width': 390, 'height': 844})
    errors, requests = [], []
    page.on('pageerror', lambda error: errors.append(str(error)))
    page.on('request', lambda request: requests.append(request.url))
    page.goto(URL)
    def action(name):
        page.get_by_role('button', name=name, exact=True).click()
    def save():
        action('Zapisz w podglądzie')
    def field(label, value):
        page.get_by_label(label, exact=True).fill(value)
    def navigate(hash):
        page.goto(URL + hash)
    def no_overflow():
        assert page.evaluate('document.documentElement.scrollWidth <= innerWidth'), 'Horizontal page overflow'
    for width in (360, 390, 768, 1440):
        page.set_viewport_size({'width': width, 'height': 900})
        for hash in ('#equipment', '#asset-1', '#asset-2', '#asset-3', '#asset-4', '#today', '#summary'):
            navigate(hash)
            no_overflow()
    print('PASS responsive: 28 views at 360/390/768/1440 px')
    page.set_viewport_size({'width': 390, 'height': 844})
    navigate('#equipment')
    field('Szukaj sprzętu', 'zzzz')
    assert page.locator('#asset-list .empty').count() == 1
    field('Szukaj sprzętu', '')
    page.get_by_label('Status', exact=True).select_option('archived')
    assert page.locator('#asset-list .card').count() == 1
    assert 'Wentylator' in page.locator('#asset-list').inner_text()
    page.get_by_label('Status', exact=True).select_option('active')
    page.get_by_label('Kategoria', exact=True).select_option('Pojazdy')
    assert page.locator('#asset-list .card').count() == 1
    page.get_by_label('Kategoria', exact=True).select_option('all')
    action('+ Dodaj sprzęt')
    field('Nazwa', 'Testowy ekstraktor <b>bez HTML</b>')
    save()
    assert '<b>bez HTML</b>' in page.locator('#asset-list').inner_text()
    assert page.locator('#asset-list h2 b').count() == 0
    assert page.locator('#asset-list .card').count() == 4
    print('PASS filtering, empty states, adding asset, escaped input')
    field('Data graniczna (opcjonalna)', '2026-06-01')
    navigate('#asset-1')
    page.get_by_label('Zakres historii').select_option('historical')
    assert 'Zakup urządzenia' in page.locator('#history-list').inner_text()
    assert 'Kontrola przewodu' not in page.locator('#history-list').inner_text()
    page.get_by_label('Zakres historii').select_option('current')
    assert 'Zakup urządzenia' not in page.locator('#history-list').inner_text()
    assert 'Kontrola przewodu' in page.locator('#history-list').inner_text()
    page.get_by_label('Zakres historii').select_option('all')
    action('Archiwizuj')
    assert page.get_by_role('button', name='Przywróć aktywność').count() == 1
    assert 'Zakup urządzenia' in page.locator('#history-list').inner_text()
    action('Przywróć aktywność')
    print('PASS history boundary, archive/restore preserves history')
    navigate('#asset-2')
    action('+ Odczyt')
    field('Rzeczywisty stan licznika','95')
    save()
    assert 'nie może cofać' in page.locator('#form-error').inner_text()
    field('Rzeczywisty stan licznika','101')
    save()
    assert 'Próg przekroczony o 1 h' in page.locator('main').inner_text()
    navigate('#today')
    oil = page.locator('.maintenance').filter(has=page.get_by_role('heading', name='Wymiana oleju', exact=True))
    oil.get_by_role('button', name='Zapisz wykonanie').click()
    save()
    assert 'Podaj rzeczywisty odczyt' in page.locator('#form-error').inner_text()
    field('Odczyt licznika (h, opcjonalnie)','101')
    field('Koszt (zł, opcjonalnie)','80.50')
    field('Notatka','Wymieniono olej w symulacji')
    save()
    assert page.locator('.maintenance').filter(has=page.get_by_role('heading', name='Wymiana oleju', exact=True)).count() == 0
    navigate('#asset-2')
    assert 'próg 201 h' in page.locator('main').inner_text()
    assert '18.05.2027' in page.locator('main').inner_text()
    assert page.locator('#history-list').inner_text().count('Wymiana oleju') == 1
    assert '80,50' in page.locator('#history-list').inner_text()
    print('PASS readings, backward-reading validation, service validation, next occurrence, single history entry')
    action('+ Plan serwisu')
    field('Nazwa czynności','Kontrola testowa')
    save()
    assert 'Podaj datę lub próg' in page.locator('#form-error').inner_text()
    field('Najbliższy termin (opcjonalnie)', '2026-11-19')
    save()
    navigate('#today')
    test = page.locator('.maintenance').filter(has=page.get_by_role('heading', name='Kontrola testowa', exact=True))
    test.get_by_role('button', name='Odrocz', exact=True).click()
    save()
    assert page.locator('.maintenance').filter(has=page.get_by_role('heading', name='Kontrola testowa', exact=True)).count() == 0
    navigate('#asset-2')
    test = page.locator('.maintenance').filter(has=page.get_by_role('heading', name='Kontrola testowa', exact=True))
    assert 'Termin dzisiaj' in test.inner_text()
    assert 'Odroczono do 20.11.2026' in test.inner_text()
    # Overdue filter reminder remains overdue in card after snoozing.
    overdue = page.locator('.maintenance').filter(has=page.get_by_role('heading', name='Kontrola filtra powietrza', exact=True))
    overdue.get_by_role('button', name='Odrocz', exact=True).click()
    save()
    overdue = page.locator('.maintenance').filter(has=page.get_by_role('heading', name='Kontrola filtra powietrza', exact=True))
    assert 'Termin przekroczony o 3 dni' in overdue.inner_text()
    assert 'Odroczono do 20.11.2026' in overdue.inner_text()
    navigate('#today')
    assert 'Kontrola filtra powietrza' not in page.locator('main').inner_text()
    assert 'Kontrola filtra powietrza' not in page.locator('#helper-text').inner_text()
    print('PASS plan validation, snooze, original overdue state, Today/helper synchronization')
    navigate('#summary')
    assert '8200,00' in ''.join(page.locator('#result').inner_text().split())
    page.get_by_label('Uwzględnij amortyzację ekonomiczną').check()
    assert '8100,00' in ''.join(page.locator('#result').inner_text().split())
    page.get_by_label('Uwzględnij amortyzację ekonomiczną').uncheck()
    assert '8200,00' in ''.join(page.locator('#result').inner_text().split())
    assert not page.locator('#depreciation-detail').is_visible()
    print('PASS optional depreciation, default off, reversible presentation')
    navigate('#asset-1')
    action('Edytuj dane')
    field('Nazwa','Nazwa po edycji')
    save()
    assert page.get_by_role('heading', name='Nazwa po edycji', exact=True).count() == 1
    action('+ Odczyt')
    page.keyboard.press('Escape')
    assert not page.locator('#editor').is_visible()
    page.emulate_media(reduced_motion='reduce')
    assert page.locator('.button').first.evaluate('(e)=>getComputedStyle(e).transitionDuration') == '0s'
    page.reload()
    assert page.get_by_role('heading', name='Santoemma Sabrina Mini', exact=True).count() == 1
    assert 'Nazwa po edycji' not in page.locator('main').inner_text()
    assert not errors, errors
    assert all(url.startswith((URL, 'data:')) for url in requests), requests
    assert not page.evaluate('localStorage.length || sessionStorage.length')
    print('PASS editing, Escape, reduced motion, reset on reload, no external requests/storage, no JS errors')
    browser.close()

server.shutdown()
server.server_close()
