"""Exercise a real pending day: link, editing, postpone and mute preferences."""
import argparse
import json
import shutil
from pathlib import Path
from urllib.parse import urlparse

from playwright.sync_api import expect, sync_playwright


def run(browser, url, embedded, output, storage_state=None):
    ctx = browser.new_context(viewport={'width': 390, 'height': 844}, storage_state=storage_state)
    page = ctx.new_page()
    errors = []
    page.on('pageerror', lambda e: errors.append(str(e)))
    page.goto(url)

    def scope():
        return page.frame(name='screen') if embedded else page

    def navigate(route):
        if embedded:
            scope().evaluate('(r)=>window.previewNavigate(r)', route)
        else:
            page.goto(url.rstrip('/') + route)
        expect(scope().locator('.ncleanek-launch')).to_be_visible()

    chip = lambda: scope().locator('.ncleanek-reminder-chip')
    expect(chip()).to_have_js_property('hidden',False)
    href = chip().locator('a').get_attribute('href')
    assert href.startswith('/day-closeout?date=')
    expect(chip().locator('span')).to_contain_text('Do uzupełnienia:')
    # A passive reminder must not open a panel or steal keyboard focus.
    expect(scope().locator('#ncleanek-panel')).not_to_be_visible()
    assert scope().evaluate('document.activeElement===document.body')
    if chip().is_visible():
        chip().get_by_role('button', name='Odłóż przypomnienie na 20 minut').click()
    else:
        # A parked bubble yields to controls; the same reminder remains in help.
        scope().locator('.ncleanek-launch').click()
        scope().locator('#ncleanek-panel').get_by_role('button',name='Opcje',exact=True).click()
        scope().locator('#ncleanek-panel').get_by_role('button',name='Później',exact=True).click()
        page.keyboard.press('Escape')
    expect(chip()).not_to_be_visible()
    until = scope().evaluate("Number(sessionStorage.getItem('nclean.guide.snooze-until'))")
    assert 19 * 60_000 < until - page.evaluate('Date.now()') <= 20 * 60_000
    navigate('/customers')
    expect(chip()).not_to_be_visible()
    # Expiry restores the reminder without changing or closing any records.
    scope().evaluate("sessionStorage.setItem('nclean.guide.snooze-until',String(Date.now()-1))")
    page.wait_for_timeout(80)
    navigate('/calendar')
    expect(chip()).to_have_js_property('hidden',False)
    scope().locator('.add-booking').click()
    expect(scope().locator('#booking-form')).to_be_visible()
    expect(chip()).not_to_be_visible()
    scope().locator('#booking-form [name=name]').fill('Zachowany szkic')
    expect(chip()).not_to_be_visible()
    expect(scope().locator('#booking-form [name=name]')).to_have_value('Zachowany szkic')
    page.keyboard.press('Escape')
    expect(chip()).to_have_js_property('hidden',False)
    scope().locator('.ncleanek-launch').click()
    expect(chip()).not_to_be_visible()
    panel = scope().locator('#ncleanek-panel')
    panel.get_by_role('button', name='Opcje', exact=True).click()
    panel.get_by_role('button', name='Wycisz na dziś', exact=True).click()
    page.keyboard.press('Escape')
    expect(chip()).not_to_be_visible()
    prefs = json.loads(scope().evaluate("localStorage.getItem('nclean.guide.preferences.v1')"))
    assert len(prefs['muted']) == 10
    navigate('/customers')
    expect(chip()).not_to_be_visible()
    scope().evaluate("localStorage.setItem('nclean.guide.preferences.v1',JSON.stringify({hidden:false,seen:[],muted:''}))")
    page.wait_for_timeout(80)
    navigate('/calendar')
    expect(chip()).to_have_js_property('hidden',False)
    if chip().is_visible():
        chip().locator('a').click()
    else:
        scope().locator('.ncleanek-launch').click()
        scope().locator('#ncleanek-panel').get_by_role('button',name='Opcje',exact=True).click()
        scope().locator('#ncleanek-panel').get_by_role('link',name='Przejdź do dokumentacji →',exact=True).click()
    expect(scope().locator('.closeout')).to_be_visible()
    expect(scope().locator('#ncleanek-panel')).not_to_be_visible()
    assert not errors, errors
    output.mkdir(parents=True, exist_ok=True)
    (output / 'reminder-results.json').write_text(json.dumps({
        'pending_day_link': href, 'passive_no_focus_or_modal': True,
        'snooze_20_minutes_across_routes_and_expiry': True,
        'mute_today_across_routes': True, 'hidden_during_editing_and_help': True,
        'draft_preserved': True, 'opens_authoritative_closeout': True,
        'javascript_errors': errors,
    }, indent=2))
    ctx.close()
    print('PASS reminders: actual pending day link, no focus stealing, postpone/expiry/mute across routes, draft and closeout.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', required=True)
    parser.add_argument('--embedded', action='store_true')
    parser.add_argument('--storage-state')
    parser.add_argument('--output', type=Path, default=Path('/tmp/ncleanek-reminder-test'))
    args = parser.parse_args()
    if urlparse(args.url).hostname not in ('localhost', '127.0.0.1'):
        parser.error('Use an isolated local test application or synthetic preview.')
    with sync_playwright() as p:
        browser = p.chromium.launch(executable_path=shutil.which('chromium'), headless=True, args=['--no-sandbox'])
        run(browser, args.url, args.embedded, args.output, args.storage_state)
        browser.close()
