"""Real user journeys for Today, the monthly phone calendar, reminders and quick fuel."""
from datetime import datetime
from zoneinfo import ZoneInfo
from playwright.sync_api import expect


def verify_today_workspace(page, base, output):
    day = datetime.now(ZoneInfo('Europe/Warsaw')).date().isoformat()
    def note(kind,title,pin=False,main=False,reminder=False,steps=()):
        page.goto(base+'/notes');page.locator('[data-note-new]').click();f=page.locator('#note-form')
        f.locator('[name=kind]').select_option(kind);f.locator('[name=title]').fill(title)
        if pin:f.locator('[name=todayPinned]').check()
        if main:f.locator('[name=mainGoal]').check()
        for text in steps:f.locator('#add-note-step').click();f.locator('[data-step-text]').last.fill(text)
        if reminder:
            f.locator('.reminder-editor>summary').click();f.locator('[name=reminder_frequency]').select_option('INTERVAL');f.locator('[name=reminder_interval]').fill('999');f.locator('[name=reminder_frequency]').select_option('DAILY');f.locator('[name=reminder_starts]').fill(day);f.locator('[name=reminder_time]').fill('00:00')
        f.get_by_role('button',name='Zapisz',exact=True).click();expect(page.locator('#note-editor')).not_to_be_visible()
        return page.locator('.memo-card').filter(has_text=title).get_attribute('data-note-id')
    main=note('GOAL','Główny cel pulpitu',True,True,True,('Wybrać zdjęcia','Uzupełnić ofertę'))
    memo=note('NOTE','Pod ręką — lista zakupów',True)
    task=note('TASK','Cykliczna kontrola wyposażenia',True,False,True)
    bookings=[]
    for i in range(8):
        page.goto(base+'/calendar?day='+day+'&add=true');f=page.locator('#booking-form');f.locator('[data-category-choice="1"]').click();f.locator('[name=qty_2]').fill('1');f.locator('[name=name]').fill('Plan mobilny QA '+str(i));f.locator('[name=phone]').fill('50000009'+str(i));f.locator('[name=address]').fill('ul. Łąkowa 5/'+str(i+1)+', Wrocław');f.locator('[name=starts]').fill(f'{8+i:02d}:00');f.get_by_role('button',name='Zapisz zlecenie',exact=True).click();expect(page.locator('#booking-editor')).not_to_be_visible();entry=page.locator('#agenda-orders [data-order]').filter(has_text='Plan mobilny QA '+str(i));expect(entry).to_have_count(1);bookings.append(entry.get_attribute('data-order'))
    # Two actual services give the chart real income; all other visits stay planned.
    for booking in bookings[:2]:
        page.goto(base+'/work/'+booking);f=page.locator('#complete-form');f.locator('[name=outcome][value=DONE]').check();f.locator('[name=final_total]').fill('150');f.locator('[name=chem_0]').select_option('ZERO');f.locator('[name=no_notes]').check();f.locator('.photo-exceptions>summary').click();f.locator('[name=before_exception]').fill('Brak zgody — test');f.locator('[name=after_exception]').fill('Brak zgody — test');f.get_by_role('button',name='Zapisz wykonanie',exact=True).click();page.wait_for_url('**/work/'+booking+'?saved=1')
    for width in (360,390,768,1440):
        page.set_viewport_size({'width':width,'height':844});page.goto(base+'/today?date='+day)
        expect(page.locator('.main-goal-card h2')).to_have_text('Główny cel pulpitu');expect(page.locator('.today-notes [data-note-id="'+main+'"]').first).to_have_count(0)
        expect(page.locator('.today-reminders [data-reminder="'+main+'"]')).to_have_count(0);expect(page.locator('.main-goal-card [data-reminder="'+main+'"]')).to_have_count(1)
        expect(page.locator('.today-notes [data-note-id="'+task+'"]')).to_have_count(0);expect(page.locator('.today-reminders [data-reminder="'+task+'"]')).to_have_count(1)
        expect(page.locator('.today-notes [data-note-id="'+memo+'"]')).to_have_count(1);expect(page.locator('.area-labels')).to_have_count(0)
        assert page.locator('[data-chart-select] option').count()>=28
        expect(page.locator('[data-today-revenue]')).to_contain_text('300,00')
        assert page.locator('.today-order').count()>=8
        for entry in page.locator('.today-order').filter(has_text='Plan mobilny QA').all():
            button=entry.locator('.navigate-button');expect(button).to_be_visible();href=button.get_attribute('href');assert 'destination=' in href and '%C5%81' in href and 'Wroc' in href
            box=button.bounding_box();assert box['width']>=44 and box['height']>=44
        assert 'Pixelify' not in page.locator('.today-date').evaluate('e=>getComputedStyle(e).fontFamily')
        assert page.evaluate('document.documentElement.scrollWidth<=innerWidth')
        page.screenshot(path=str(output/('today-'+str(width)+'.png')),full_page=True)
        page.get_by_role('link',name='Tydzień',exact=True).click();expect(page.locator('[data-chart-select] option')).to_have_count(7);assert day in page.locator('[data-chart-select] option').evaluate_all('options=>options.map(o=>o.dataset.date)');page.locator('[data-chart-select]').select_option('0');expect(page.locator('[data-chart-value]')).to_contain_text('przychód')
        page.goto(base+'/calendar?day='+day);expect(page.locator('#month-overview')).to_be_visible();expect(page.locator('#week-strip')).to_have_count(0);assert len(page.locator('.calendar-grid').evaluate('e=>getComputedStyle(e).gridTemplateColumns.split(" ")'))==7
        if width<=900:
            expect(page.locator('#agenda-slot')).not_to_be_visible();page.locator('[data-select-day="'+day+'"]').first.click();expect(page.locator('#agenda-sheet')).to_be_visible();expect(page.locator('#agenda-orders [data-order]')).not_to_have_count(0);page.locator('#agenda-orders [data-order]').first.click();expect(page.locator('#order-editor')).to_be_visible();page.locator('#order-editor [data-close]').first.click();expect(page.locator('#agenda-sheet')).to_be_visible();page.locator('#agenda-sheet [data-close]').click();expect(page.locator('#agenda-sheet')).not_to_be_visible();expect(page.locator('[data-select-day="'+day+'"]').first).to_be_focused()
        assert page.evaluate('document.documentElement.scrollWidth<=innerWidth');page.screenshot(path=str(output/('calendar-month-'+str(width)+'.png')),full_page=True)
    page.set_viewport_size({'width':390,'height':844});page.goto(base+'/calendar?day='+day);page.locator('[data-select-day="'+day+'"]').first.click();page.locator('#edit-day-areas').click();page.set_viewport_size({'width':1440,'height':844});page.locator('#area-editor [data-close]').first.click();page.set_viewport_size({'width':390,'height':844});page.locator('.add-booking').click();page.locator('#booking-editor [data-close]').first.click();expect(page.locator('#agenda-sheet')).not_to_be_visible()
    page.set_viewport_size({'width':390,'height':844});page.goto(base+'/calendar?day='+day);page.locator('[data-select-day="'+day+'"]').first.click();page.locator('#edit-day-areas').click();expect(page.locator('#area-editor')).to_be_visible();page.locator('#clear-areas').click();page.locator('[data-area-option="1"] input').check();page.locator('[data-area-option="2"] input').check();page.locator('#save-areas').click();page.wait_for_url('**saved=1');expect(page.locator('#agenda-sheet')).to_be_visible();expect(page.locator('#agenda-areas .area-chip')).to_have_count(2);page.locator('#agenda-sheet [data-close]').click();expect(page.locator('[data-select-day="'+day+'"]').first).to_be_focused()
    page.goto(base+'/today');card=page.locator('.today-reminders [data-reminder="'+task+'"]');card.scroll_into_view_if_needed();expect(card).to_have_attribute('data-seen','true');page.route('**/notes/reminders/'+task,lambda r:r.fulfill(status=503,content_type='application/json',body='{"message":"Próba błędu. Ponów zapis."}'),times=1);card.get_by_role('button',name='Zrobione na dziś',exact=True).click();expect(page.locator('[data-reminder-error]')).to_contain_text('Ponów');expect(card).to_be_visible();card.get_by_role('button',name='Zrobione na dziś',exact=True).click();expect(card).to_have_count(0);expect(page.locator('.today-notes [data-note-id="'+task+'"]')).to_have_count(1)
    page.reload();expect(page.locator('.today-reminders [data-reminder="'+task+'"]')).to_have_count(0)
    main_reminder=page.locator('.main-goal-card [data-reminder="'+main+'"]');main_reminder.locator('select[name=minutes]').select_option('15');main_reminder.get_by_role('button',name='Odłóż',exact=True).click();expect(main_reminder).to_have_count(0)
    # Quick tank entry has four visible fields; no daily or generator reading is needed.
    page.goto(base+'/fuel?date='+day);f=page.locator('#fuel-purchase-form');f.locator('[name=odometer]').fill('200000');f.locator('[name=litres]').fill('32,5');f.locator('[name=litre_price]').fill('6,29');expect(page.locator('#fuel-total')).to_contain_text('204,43');page.locator('#purchase-details>summary').click();f.locator('[name=amount_override]').check();f.locator('[name=amount]').fill('0,001');f.locator('[name=amount_override]').uncheck();expect(f.locator('[name=amount]')).to_be_disabled();page.locator('#purchase-details>summary').click()
    page.route('**/fuel/purchases',lambda r:r.fulfill(status=503,content_type='application/json',body='{"message":"Próba błędu tankowania."}'),times=1);f.get_by_role('button',name='Zapisz tankowanie',exact=True).click();expect(f.locator('.form-error')).to_contain_text('Próba błędu');expect(f.locator('[name=litres]')).to_have_value('32,5');f.get_by_role('button',name='Zapisz tankowanie',exact=True).click();page.wait_for_url('**/fuel?date='+day+'&saved=1#purchases');expect(page.locator('#purchases')).to_contain_text('204.43')
    count=page.locator('#purchases .fuel-history-row').count()
    for width in (360,390,768,1440):
        page.set_viewport_size({'width':width,'height':844});page.goto(base+'/fuel?date='+day);f=page.locator('#fuel-purchase-form');f.locator('[name=odometer]').fill('200100');f.locator('[name=litres]').fill('10');f.locator('[name=litre_price]').fill('6.50');expect(page.locator('#fuel-total')).to_contain_text('65,00');assert page.evaluate('document.documentElement.scrollWidth<=innerWidth');page.screenshot(path=str(output/('quick-fuel-'+str(width)+'.png')),full_page=True)
    f.get_by_role('button',name='Zapisz tankowanie',exact=True).click();page.wait_for_url('**/fuel?date='+day+'&saved=1#purchases');expect(page.locator('#purchases .fuel-history-row')).to_have_count(count+1)
    print('PASS Today workspace: 8 visits/navigation, main goal and pin/reminder deduplication, actual revenue/month/week, four month grids/mobile sheet/focus, daily occurrence503 retry/done/snooze, quick fuel commas/rounding503 retry/two fills/four widths.',flush=True)
