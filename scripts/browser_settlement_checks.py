"""Automatic settlement, deliberate retries and responsive real CRM views."""
from decimal import Decimal
from playwright.sync_api import expect


def verify_automatic_settlement(page, base, output):
    def amount(selector):
        return Decimal(page.locator(selector).inner_text().replace('zł','').replace('\xa0','').replace(' ','').replace(',','.').strip())
    page.goto(base+'/today?date=2026-10-10&finance=month')
    before=amount('[data-today-revenue]')
    page.goto(base+'/calendar?day=2026-10-10&add=true');f=page.locator('#booking-form')
    f.locator('[data-category-choice="1"]').click();f.locator('[name=qty_2]').fill('1');f.locator('[name=historical]').check();f.locator('[name=name]').fill('Automatyczna zapłata QA')
    f.get_by_role('button',name='Zapisz zlecenie',exact=True).click();expect(page.locator('#booking-editor')).not_to_be_visible()
    job=page.locator('#agenda-orders [data-order]').filter(has_text='Automatyczna zapłata QA').get_attribute('data-order')
    page.goto(base+'/work/'+job);expect(page.locator('#payment-form')).to_have_count(0)
    f=page.locator('#complete-form');f.locator('[name=final_total]').fill('600');f.locator('[name=chem_0]').select_option('ZERO');f.locator('[name=no_notes]').check()
    page.context.set_offline(True);f.get_by_role('button',name='Zapisz wykonanie',exact=True).click();expect(f.locator('.review-error')).to_contain_text('połączenia');expect(f.locator('[name=final_total]')).to_have_value('600');page.context.set_offline(False)
    committed=[]
    def lost_response(route):
        committed.append(route.fetch().status);route.abort()
    page.route('**/day-closeout/'+job,lost_response,times=1)
    f.get_by_role('button',name='Zapisz wykonanie',exact=True).click();expect(f.get_by_role('button',name='Zapisz wykonanie',exact=True)).to_be_enabled();assert committed==[200]
    # The acknowledged state is unknown: deliberate retry must not duplicate revenue.
    f.get_by_role('button',name='Zapisz wykonanie',exact=True).click();expect(f.locator('.review-error')).to_contain_text('innym oknie')
    page.reload();expect(page.locator('#settlement')).to_contain_text('Zapłacono · 600.00 zł')
    expect(page.locator('#payments')).to_have_count(0)
    page.goto(base+'/today?date=2026-10-10&finance=month');assert amount('[data-today-revenue]')==before+Decimal('600');expect(page.locator('.today-dues')).to_have_count(0)
    page.goto(base+'/work/'+job);f=page.locator('#complete-form');f.locator('[name=final_total]').fill('550');f.get_by_role('button',name='Zapisz wykonanie',exact=True).click();expect(page.locator('#settlement')).to_contain_text('550.00 zł')
    page.goto(base+'/today?date=2026-10-10&finance=month');assert amount('[data-today-revenue]')==before+Decimal('550')
    for path in ['/work/'+job,'/dashboard?date=2026-10-10&range=month&mode=actual','/today?date=2026-10-10&finance=month']:
        page.goto(base+path)
        for width in (360,390,768,1440):
            page.set_viewport_size({'width':width,'height':844})
            if not page.evaluate('document.documentElement.scrollWidth<=innerWidth'):
                print(page.evaluate("[...document.querySelectorAll('main *')].filter(e=>e.getBoundingClientRect().right>innerWidth).map(e=>({tag:e.tagName,cls:e.className,id:e.id,width:e.getBoundingClientRect().width,right:e.getBoundingClientRect().right,text:e.textContent.slice(0,70)})).slice(0,16)"),flush=True)
                page.screenshot(path=str(output/'settlement-overflow.png'),full_page=True)
                raise AssertionError((path,width))
            expect(page.get_by_role('button',name='Dodaj wpłatę',exact=True)).to_have_count(0)
            page.screenshot(path=str(output/('settlement-'+path.split('?')[0].strip('/').replace('/','-')+'-'+str(width)+'.png')),full_page=True)
    print('PASS automatic settlement: real completion, offline retained fields, lost committed response/retry, lower final price, no manual payments/dues, correct October revenue, four widths.',flush=True)
