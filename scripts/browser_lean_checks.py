"""User journeys for optional controls. Uses the real authenticated Spring application."""
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
from playwright.sync_api import expect


def verify_lean_disclosures(page, base, output):
    day=datetime.now(ZoneInfo('Europe/Warsaw')).date()
    for width in (360,390,768,1440):
        page.set_viewport_size({'width':width,'height':844});page.goto(base+'/today')
        disclosure=page.locator('.finance-filter-disclosure');expect(disclosure).not_to_have_attribute('open','')
        assert page.locator('#today-orders').bounding_box()['y']<page.locator('#today-finance').bounding_box()['y'];expect(page.locator('[data-finance-filter]')).not_to_be_visible();expect(page.locator('[data-today-revenue]')).to_be_visible();expect(page.locator('.finance-caveat')).to_be_visible()
        summary=disclosure.locator('summary');summary.focus();page.keyboard.press('Enter');expect(page.locator('[data-finance-filter]')).to_be_visible()
        f=page.locator('.finance-filters');f.locator('[data-finance-filter]').select_option('custom');f.locator('[name=from]').fill((day-timedelta(days=3)).isoformat());f.locator('[name=to]').fill(day.isoformat());summary.click();expect(f.locator('[name=from]')).not_to_be_visible();summary.click();expect(f.locator('[name=from]')).to_have_value((day-timedelta(days=3)).isoformat())
        f.get_by_role('button',name='Pokaż',exact=True).click();expect(page.locator('.finance-filter-disclosure')).not_to_have_attribute('open','');expect(page.locator('.today-period-label')).to_have_text('Własny zakres');expect(page.locator('[data-chart-select] option')).to_have_count(4)
        expect(page.locator('[data-chart-select]')).not_to_be_visible()
        cell=page.locator('.pixel-donut svg [data-chart-index]:not([data-chart-index="-1"])').first
        if cell.count():cell.click();expect(page.locator('[data-chart-select]')).to_be_visible()
        else:page.locator('.chart-days>summary').click();expect(page.locator('[data-chart-select]')).to_be_visible()
        assert page.evaluate('document.documentElement.scrollWidth<=innerWidth')
        page.screenshot(path=str(output/('lean-today-'+str(width)+'.png')),full_page=True)
        page.goto(base+'/notes');page.locator('[data-note-new]').click();f=page.locator('#note-form');expect(f.locator('[name=title]')).to_be_visible();expect(f.locator('[name=due]')).not_to_be_visible();expect(f.locator('[data-reminder-add]')).not_to_be_visible()
        options=f.locator('.note-options');options.locator('summary').click();f.locator('[name=due]').fill((day+timedelta(days=5)).isoformat());f.locator('[name=due_time]').fill('14:30');f.locator('[name=todayPinned]').check();options.locator('summary').click();expect(f.locator('[data-note-options-status]')).to_contain_text('Dzisiaj')
        f.locator('[name=title]').fill('LEAN '+str(width));rd=f.locator('.reminder-disclosure');rd.locator('summary').click();f.locator('[data-reminder-add]').click();f.locator('[data-field=amount]').fill('-1');rd.locator('summary').click();f.get_by_role('button',name='Zapisz',exact=True).click();expect(f.locator('[data-field=amount]')).to_be_visible();expect(f.locator('[data-field=amount]')).to_be_focused();expect(f.locator('[name=title]')).to_have_value('LEAN '+str(width));f.locator('[data-field=amount]').fill('30');rd.locator('summary').click()
        expect(f.locator('[data-reminder-count]')).to_have_text('1 przypomnienie');assert f.evaluate('e=>e.scrollWidth<=e.clientWidth');page.screenshot(path=str(output/('lean-note-'+str(width)+'.png')))
        page.route('**/notes',lambda r:r.fulfill(status=503,content_type='application/json',body='{"message":"Kontrolowany błąd. Ponów zapis."}'),times=1)
        f.get_by_role('button',name='Zapisz',exact=True).click();expect(f.locator('.note-error')).to_contain_text('Ponów');expect(f.locator('[name=due_time]')).to_have_value('14:30');f.get_by_role('button',name='Zapisz',exact=True).click();expect(page.locator('#note-editor')).not_to_be_visible()
        card=page.locator('.memo-card').filter(has_text='LEAN '+str(width));card.locator('[data-note-edit]').click();expect(f.locator('[name=due_time]')).to_have_value('14:30');expect(f.locator('[name=todayPinned]')).to_be_checked();expect(f.locator('[data-reminder-count]')).to_have_text('1 przypomnienie');expect(f.locator('[data-field=amount]')).not_to_be_visible();page.locator('[data-note-close]').click()
        page.goto(base+'/costs');expect(page.locator('.cost-filter-disclosure')).not_to_have_attribute('open','');expect(page.locator('[data-cost-new]')).to_be_visible();page.locator('.cost-filter-disclosure>summary').click();f=page.locator('.cost-period-form');f.locator('[name=q]').fill('LEAN');f.locator('[name=cancelled]').check();f.get_by_role('button',name='Pokaż',exact=True).click();expect(page.locator('.active-cost-filters')).to_contain_text('LEAN');expect(page.locator('.cost-filter-disclosure')).not_to_have_attribute('open','')
        page.locator('.cost-tabs').get_by_role('link',name='Samochód',exact=True).click();expect(page.locator('.active-cost-filters')).to_contain_text('LEAN');assert 'q=LEAN' in page.url and 'cancelled=true' in page.url;page.locator('.cost-period-links a').last.click();expect(page.locator('.active-cost-filters')).to_contain_text('LEAN');page.locator('.cost-filter-disclosure>summary').click();page.get_by_role('link',name='Wyczyść wyszukiwanie i anulowane',exact=True).click();expect(page.locator('.active-cost-filters')).to_have_count(0);assert page.evaluate('document.documentElement.scrollWidth<=innerWidth')
        page.goto(base+'/fuel');expect(page.locator('#fuel-purchase-form')).to_be_visible();expect(page.locator('#consumption')).not_to_have_attribute('open','');page.goto(base+'/fuel#consumption');expect(page.locator('#consumption')).to_have_attribute('open','');page.locator('#consumption>summary').click();assert page.evaluate('document.documentElement.scrollWidth<=innerWidth')
    print('PASS LEAN: hidden filters and active scope, keyboard/click, draft retention, ring details, folded note values persisted, hidden invalid field reveal/focus,503 retry, cost filters/navigation/reset, fuel anchors, four widths.',flush=True)
