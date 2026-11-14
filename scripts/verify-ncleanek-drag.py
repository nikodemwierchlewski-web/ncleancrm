"""Check the reported drag/resize/scroll regressions in Chromium.

Use --embedded for the self-contained HTML preview, otherwise pass the URL of
an authenticated local application and (optionally) --storage-state.
"""
import argparse
import json
import re
import shutil
from pathlib import Path
from urllib.parse import urlparse

from playwright.sync_api import expect, sync_playwright


KEY = 'nclean.puppet.position.v1'
ROUTES = ['/calendar', '/customers', '/settings', '/settings/areas', '/notes', '/costs', '/dashboard']


def box(scope):
    return scope.locator('.ncleanek-dock').evaluate(
        'e=>{const r=e.getBoundingClientRect();return {x:r.x,y:r.y,width:r.width,height:r.height};}')


def same(actual, expected):
    assert all(abs(actual[k] - expected[k]) < .15 for k in expected), (actual, expected)


def drag(page, scope, x, y, touch=None):
    actor = scope.locator('.ncleanek-launch')
    before = box(scope)
    outer = actor.bounding_box()
    dx, dy = outer['x'] - before['x'], outer['y'] - before['y']
    sx, sy = outer['x'] + 22, outer['y'] + 25
    tx, ty = x + dx + 22, y + dy + 25
    if touch:
        touch.send('Input.dispatchTouchEvent', {'type': 'touchStart', 'touchPoints': [{'x': sx, 'y': sy}]})
        touch.send('Input.dispatchTouchEvent', {'type': 'touchMove', 'touchPoints': [{'x': tx, 'y': ty}]})
    else:
        page.mouse.move(sx, sy)
        page.mouse.down()
        page.mouse.move(tx, ty, steps=8)
    page.wait_for_timeout(40)
    expect(scope.locator('.ncleanek-dock')).to_have_class(re.compile('.*is-dragging.*'))
    same(box(scope), {'x': x, 'y': before['y'], 'width': 44, 'height': 60})
    if touch:
        touch.send('Input.dispatchTouchEvent', {'type': 'touchEnd', 'touchPoints': []})
    else:
        page.mouse.up()
    page.wait_for_timeout(400)
    same(box(scope), {'x': x, 'y': before['y'], 'width': 44, 'height': 60})
    expect(scope.locator('#ncleanek-panel')).not_to_be_visible()
    saved = scope.evaluate(f'localStorage.getItem({json.dumps(KEY)})')
    assert saved
    return saved


def run(browser, url, embedded, output, storage_state=None):
    errors, results = [], []
    for width in (360, 390, 768, 1440):
        ctx = browser.new_context(viewport={'width': width, 'height': 844}, storage_state=storage_state)
        page = ctx.new_page()
        page.on('pageerror', lambda e: errors.append(str(e)))
        page.goto(url)

        def scope():
            return page.frame(name='screen') if embedded else page

        def navigate(route):
            if embedded:
                if route == '/calendar':
                    page.locator('#preview-route').select_option(route)
                else:
                    scope().evaluate('(route)=>window.previewNavigate(route)', route)
            else:
                page.goto(url.rstrip('/') + route)
            expect(scope().locator('.ncleanek-launch')).to_be_visible()
            page.wait_for_timeout(120)

        navigate('/calendar')
        same(box(scope()), {'width': 44, 'height': 60})
        viewport=scope().evaluate('({w:innerWidth,h:innerHeight})');initial=box(scope());assert abs(initial['x']-(viewport['w']-54))<1
        # Grab at the torso: the grab point must stay under the same finger.
        for x, y in ((35, 180), (width - 85, 350), (100, 260), (14, 140), (14, 300)):
            saved = drag(page, scope(), x, y)
            before = box(scope())
            scope().evaluate("""() => {
                const p=document.createElement('p');p.textContent='Regresja aktualizacji treści';
                document.body.append(p);p.hidden=true;
                window.scrollTo(0,450);
                document.dispatchEvent(new Event('nclean:committed'));
            }""")
            page.wait_for_timeout(400)
            same(box(scope()), before)
            assert scope().evaluate(f'localStorage.getItem({json.dumps(KEY)})') == saved

        anchored = box(scope())
        for route in ROUTES:
            navigate(route)
            same(box(scope()), anchored)
            assert scope().evaluate(f'localStorage.getItem({json.dumps(KEY)})') == saved
            assert scope().evaluate('document.documentElement.scrollWidth<=innerWidth')
            results.append({'width': width, 'route': route, 'stable': True})

        navigate('/calendar')
        # Cancel and a second pointer cannot overwrite the last chosen position.
        actor = scope().locator('.ncleanek-launch')
        r = actor.bounding_box()
        page.mouse.move(r['x'] + 22, r['y'] + 25)
        page.mouse.down()
        page.mouse.move(r['x'] + 85, r['y'] + 70, steps=5)
        page.wait_for_timeout(30)
        during = box(scope())
        actor.dispatch_event('pointerup', {'pointerId': 99, 'pointerType': 'touch'})
        same(box(scope()), during)
        actor.dispatch_event('pointercancel', {'pointerId': 1, 'pointerType': 'mouse'})
        page.mouse.up()
        same(box(scope()), anchored)
        assert scope().evaluate(f'localStorage.getItem({json.dumps(KEY)})') == saved

        actor.focus()
        page.keyboard.press('ArrowUp')
        page.keyboard.press('ArrowDown')
        same(box(scope()), anchored)
        page.keyboard.press('ArrowRight')
        same(box(scope()), {'x': anchored['x'] + 24, 'y': anchored['y'], 'width': 44, 'height': 60})
        page.keyboard.press('ArrowLeft')
        same(box(scope()), anchored)
        page.keyboard.press('Home')
        same(box(scope()), {'x': width - 54, 'y': anchored['y'], 'width': 44, 'height': 60})
        page.set_viewport_size({'width': width + 20, 'height': 944})
        page.wait_for_timeout(180)
        same(box(scope()), {'x': width + 20 - 54, 'width': 44, 'height': 60})
        page.set_viewport_size({'width': width, 'height': 844})
        page.wait_for_timeout(180)
        # Use the supported keyboard movement to start left of the save area.
        for _ in range(width // 48 + 1):
            page.keyboard.press('Shift+ArrowLeft')
        anchored = box(scope())
        same(anchored, {'x': 10, 'width': 44, 'height': 60})

        scope().locator('.add-booking').click()
        form = scope().locator('#booking-form')
        expect(form).to_be_visible()
        page.wait_for_timeout(120)
        same(box(scope()), anchored)
        scope().locator('[name=name]').fill('Szkic testowy')
        expect(form.locator('[name=name]')).to_have_value('Szkic testowy')
        # Deliberately drop over the submit bar: save must receive pointer input.
        footer = form.locator('.booking-submit')
        footer.scroll_into_view_if_needed()
        page.wait_for_timeout(100)
        r = footer.locator('button').evaluate('e=>{const r=e.getBoundingClientRect();return {x:r.x,y:r.y,width:r.width,height:r.height};}')
        target_x = max(12, min(width - 56, r['x'] + r['width'] - 35))
        drag(page, scope(), target_x, 300)
        if box(scope())['y']+60>r['y'] and box(scope())['y']<r['y']+r['height']:
            expect(scope().locator('.ncleanek-dock')).to_have_class(re.compile('.*is-yielding.*'))
            expect(actor).to_have_css('pointer-events', 'none')
        expect(form.locator('[name=name]')).to_have_value('Szkic testowy')
        save = form.get_by_role('button', name='Zapisz zlecenie', exact=True)
        assert save.evaluate('e=>{const r=e.getBoundingClientRect();return e.contains(document.elementFromPoint(r.x+r.width/2,r.y+r.height/2));}')
        page.keyboard.press('Escape')
        expect(form).not_to_be_visible()
        scope().evaluate("document.dispatchEvent(new Event('ncleanek:position-reset'))")
        page.wait_for_timeout(120)
        actor.click()
        expect(scope().locator('#ncleanek-panel .ncleanek-step')).to_contain_text('1 z ')
        page.keyboard.press('Escape')
        page.screenshot(path=str(output / f'stable-{width}.png'))
        ctx.close()

    # Trusted touch events reproduce the mobile gesture, not synthetic mouse events.
    ctx = browser.new_context(viewport={'width': 390, 'height': 844}, has_touch=True, is_mobile=True,
                              storage_state=storage_state)
    page = ctx.new_page()
    page.on('pageerror', lambda e: errors.append(str(e)))
    page.goto(url)
    scope = page.frame(name='screen') if embedded else page
    actor = scope.locator('.ncleanek-launch')
    expect(actor).to_be_visible()
    cdp = ctx.new_cdp_session(page)
    for x, y in ((40, 190), (260, 380), (100, 270)):
        drag(page, scope, x, y, touch=cdp)
    r = actor.bounding_box()
    page.touchscreen.tap(r['x'] + 22, r['y'] + 25)
    expect(scope.locator('#ncleanek-panel .ncleanek-step')).to_contain_text('1 z ')
    page.keyboard.press('Escape')
    page.emulate_media(reduced_motion='reduce')
    expect(scope.locator('.ncleanek-puppet-body')).to_have_css('animation-name', 'none')
    assert not errors, errors
    ctx.close()
    report = {'route_width_checks': results, 'drag_drop_checks': 27, 'cancel_foreign_pointer': True, 'horizontal_keyboard_home_and_resize': True,
              'form_draft_and_save_accessible': True, 'touch_drag_and_tap': True,
              'reduced_motion': True, 'javascript_errors': errors}
    (output / 'stable-drag-results.json').write_text(json.dumps(report, indent=2))
    print('PASS stable drag: 27 precise horizontal drops, 28 route/width checks, scroll, cancel, form/save, touch and reduced motion.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', required=True)
    parser.add_argument('--embedded', action='store_true')
    parser.add_argument('--storage-state')
    parser.add_argument('--output', type=Path, default=Path('/tmp/ncleanek-stable-test'))
    args = parser.parse_args()
    if urlparse(args.url).hostname not in ('localhost', '127.0.0.1'):
        parser.error('Use a local application or a locally served synthetic demo.')
    args.output.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(executable_path=shutil.which('chromium'), headless=True,
                                              args=['--no-sandbox'])
        run(browser, args.url, args.embedded, args.output, args.storage_state)
        browser.close()
