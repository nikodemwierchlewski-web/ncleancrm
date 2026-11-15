"""Contextual reminder and finance-filter journeys on the real authenticated CRM."""
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
from playwright.sync_api import expect


def verify_entity_reminders(page, base, output):
    day = datetime.now(ZoneInfo('Europe/Warsaw')).date()
    future = (day + timedelta(days=5)).isoformat()
    page.goto(base+'/today')
    booking = page.locator('.today-order:not(.muted-row)').first.get_attribute('data-today-order')
    page.goto(base+'/work/'+booking)
    panel = page.locator('[data-reminder-source=BOOKING]')
    for amount, unit in [('30','minutes'),('3','hours'),('2','days')]:
        panel.locator('[data-reminder-add]').click()
        row = panel.locator('.entity-reminder-row').last
        row.locator('[data-field=amount]').fill(amount)
        row.locator('[data-field=unit]').select_option(unit)
    page.route('**/api/v1/reminders',lambda r:r.fulfill(status=503,content_type='application/json',body='{"message":"Błąd próby. Ponów zapis."}'),times=1)
    panel.locator('[data-reminder-save]').click()
    expect(panel.locator('[data-reminder-error]')).to_contain_text('Ponów')
    expect(panel.locator('.entity-reminder-row')).to_have_count(3)
    panel.locator('[data-reminder-save]').click()
    expect(panel.locator('[data-reminder-error]')).to_contain_text('Zapisano')
    page.reload();expect(panel.locator('.entity-reminder-row')).to_have_count(3)
    # One compact editor, atomic note + plan save and preserved input after validation.
    page.goto(base+'/notes');page.locator('[data-note-new]').click()
    form=page.locator('#note-form');form.locator('[name=kind]').select_option('GOAL');form.locator('[name=title]').fill('QA — termin i przypomnienia')
    form.locator('[data-reminder-add]').click();form.locator('[data-field=amount]').fill('17');form.locator('[data-field=unit]').select_option('days')
    form.get_by_role('button',name='Zapisz',exact=True).click()
    expect(form.locator('.note-error')).to_contain_text('termin')
    expect(form.locator('[name=title]')).to_have_value('QA — termin i przypomnienia')
    form.locator('[name=due]').fill(future);form.locator('[name=due_time]').fill('14:30')
    form.locator('[data-reminder-add]').click();row=form.locator('.entity-reminder-row').last;row.locator('[data-field=mode]').select_option('AT');row.locator('[data-field=at]').fill(future+'T08:15')
    form.get_by_role('button',name='Zapisz',exact=True).click();expect(page.locator('#note-editor')).not_to_be_visible()
    note=page.locator('.memo-card').filter(has_text='QA — termin i przypomnienia').get_attribute('data-note-id')
    page.goto(base+'/notes?entry='+note);expect(form).to_be_visible();expect(form.locator('.entity-reminder-row')).to_have_count(2);expect(form.locator('[name=due_time]')).to_have_value('14:30');page.locator('[data-note-close]').click()
    for width in (360,390,768,1440):
        page.set_viewport_size({'width':width,'height':844})
        page.goto(base+'/work/'+booking);expect(panel.locator('.entity-reminder-row')).to_have_count(3)
        assert page.evaluate('document.documentElement.scrollWidth<=innerWidth')
        page.screenshot(path=str(output/('entity-booking-'+str(width)+'.png')),full_page=True)
        page.goto(base+'/notes?entry='+note);expect(form).to_be_visible();expect(form.locator('.entity-reminder-row')).to_have_count(2)
        assert form.evaluate('e=>e.scrollWidth<=e.clientWidth')
        page.screenshot(path=str(output/('entity-goal-'+str(width)+'.png')))
        page.locator('[data-note-close]').click()
        page.goto(base+'/today?date='+day.isoformat())
        expect(page.locator('.today-reminders')).to_have_count(0);expect(page.locator('[data-finance-filter]')).to_have_value('month-to-date')
        assert page.locator('[data-chart-select] option').count()==day.day
        assert page.locator('.pixel-donut svg rect').count()>200
        filters=page.locator('.finance-filters');filters.locator('[data-finance-filter]').select_option('custom');filters.locator('[name=from]').fill((day-timedelta(days=7)).isoformat());filters.locator('[name=to]').fill(day.isoformat());filters.get_by_role('button',name='Pokaż',exact=True).click();expect(page.locator('[data-chart-select] option')).to_have_count(8)
        assert page.evaluate('document.documentElement.scrollWidth<=innerWidth')
        page.screenshot(path=str(output/('entity-finance-'+str(width)+'.png')),full_page=True)
    # Disable a reminder without deleting its saved occurrence history.
    page.goto(base+'/work/'+booking);expect(panel.locator('.entity-reminder-row')).to_have_count(3);panel.locator('.entity-reminder-row .icon-button').first.click();panel.locator('[data-reminder-save]').click();expect(panel.locator('[data-reminder-error]')).to_contain_text('Zapisano');page.reload();expect(panel.locator('.entity-reminder-row')).to_have_count(2)
    print('PASS contextual reminders: booking minutes/hours/days,503 retry, atomic goal validation/date/time/absolute reminder, deep link, removal, custom finance/to-date pixel ring and four widths.',flush=True)
