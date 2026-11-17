"""Real October cash costs and separate cumulative full-to-full counters."""
import base64
import json
from decimal import Decimal
from urllib.parse import urljoin, urlparse, parse_qs
from playwright.sync_api import expect


def verify_full_tank(page, base, output):
    def value(selector):return Decimal(page.locator(selector).inner_text().replace('zł','').replace('\xa0','').replace(' ','').replace(',','.').strip())
    def finance():
        page.goto(base+'/today?date=2026-10-12&finance=month')
        return value('[data-today-revenue]'),value('[data-today-profit]')
    page.goto(base+'/calendar?day=2026-10-10&add=true');f=page.locator('#booking-form');f.locator('[data-category-choice="1"]').click();f.locator('[name=qty_2]').fill('1');f.locator('[name=historical]').check();f.locator('[name=name]').fill('Tankowania październik QA');f.get_by_role('button',name='Zapisz zlecenie',exact=True).click();expect(page.locator('#booking-editor')).not_to_be_visible();job=page.locator('#agenda-orders [data-order]').filter(has_text='Tankowania październik QA').get_attribute('data-order')
    page.goto(base+'/work/'+job);f=page.locator('#complete-form');f.locator('[name=final_total]').fill('1000');f.locator('[name=chem_0]').select_option('ZERO');f.locator('[name=no_notes]').check();f.get_by_role('button',name='Zapisz wykonanie',exact=True).click();expect(page).to_have_url(base+'/work/'+job+'?saved=1')
    income,before=finance()
    for target,meter in [('CAR','100000'),('GENERATOR','100')]:
        page.goto(base+'/fuel?date=2026-10-01');section=page.locator('#parameters')
        if not section.evaluate('e=>e.open'):section.locator('summary').first.click()
        f=page.locator('#fuel-baseline-form');f.locator('[name=target][value='+target+']').check();f.locator('[name=started_at]').fill('2026-10-01T08:00');f.locator('[name=meter]').fill(meter)
        with page.expect_navigation():f.get_by_role('button',name='Zapisz stan początkowy',exact=True).click()
    assert finance()[1]==before
    page.goto(base+'/fuel?date=2026-10-01');page.locator('#parameters>summary').click();f=page.locator('#fuel-baseline-form');f.locator('[name=target][value=CAR]').check();assert f.locator('[name=meter]').evaluate('e=>e.checkValidity()')
    page.goto(base+'/fuel?date=2026-10-12');f=page.locator('#fuel-purchase-form');count=page.locator('#purchases .fuel-history-row').count();f.locator('[name=purchased_at]').fill('2026-10-12T12:00');f.locator('[name=meter]').fill('100500');f.locator('[name=litres]').fill('40');f.locator('[name=amount]').fill('200');expect(page.locator('#full-unit-price')).to_have_text('5,000 zł/l')
    page.route('**/fuel/purchases',lambda r:r.fulfill(status=503,content_type='application/json',body='{"message":"Próba awarii. Ponów zapis."}'),times=1);f.get_by_role('button',name='Zapisz tankowanie',exact=True).click();expect(f.locator('.form-error')).to_contain_text('Próba awarii');expect(f.locator('[name=amount]')).to_have_value('200');f.get_by_role('button',name='Zapisz tankowanie',exact=True).click();expect(page.locator('#purchases .fuel-history-row')).to_have_count(count+1)
    page.goto(base+'/fuel?date=2026-10-12');f=page.locator('#fuel-purchase-form');f.locator('[name=target][value=GENERATOR]').check();expect(f.locator('[data-meter-label]')).to_contain_text('motogodziny');f.locator('[name=purchased_at]').fill('2026-10-12T12:00');f.locator('[name=meter]').fill('110');f.locator('[name=litres]').fill('10');f.locator('[name=amount]').fill('50');f.get_by_role('button',name='Zapisz tankowanie',exact=True).click();expect(page.locator('#purchases .fuel-history-row')).to_have_count(count+2)
    expect(page.locator('#consumption')).not_to_have_attribute('open','');page.locator('#consumption>summary').click();expect(page.locator('#consumption')).to_contain_text('8.000 l/100 km');expect(page.locator('#consumption')).to_contain_text('1.000 l/mth')
    assert finance()[1]==before-Decimal('250')
    page.goto(base+'/fuel?date=2026-10-12');row=page.locator('#purchases .fuel-history-row').filter(has_text='Samochód').first;edit=row.get_by_role('link',name='Edytuj',exact=True).get_attribute('href');car_id=int(parse_qs(urlparse(edit).query)['purchase'][0]);row.get_by_role('link',name='Edytuj',exact=True).click();f=page.locator('#fuel-purchase-form');f.locator('[name=amount]').fill('210');f.get_by_role('button',name='Zapisz tankowanie',exact=True).click();expect(page.locator('.fuel-expense-total')).to_contain_text('260.00')
    row=page.locator('#purchases .fuel-history-row').filter(has_text='Samochód').first;row.locator('.removal-form button').click();page.locator('#removal-confirm').get_by_role('button',name='Usuń',exact=True).click();expect(page.locator('#purchases .fuel-history-row').filter(has_text='Samochód').first).to_contain_text('Anulowane')
    assert finance()[1]==before-Decimal('50')
    page.goto(base+'/fuel?date=2026-10-12');row=page.locator('#purchases .fuel-history-row').filter(has_text='Samochód').first;row.locator('.removal-form button').click();page.locator('#removal-confirm').get_by_role('button',name='Przywróć',exact=True).click();expect(page.locator('.fuel-expense-total')).to_contain_text('260.00');assert finance()[1]==before-Decimal('260')
    page.goto(base+'/fuel?month=2026-10');page.locator('.full-tank-heading a').click();expect(page.locator('[data-finance-filter]')).to_have_value('month');assert value('[data-today-profit]')==before-Decimal('260');page.goto(base+'/fuel?month=2026-10')
    for width in (360,390,768,1440):
        page.set_viewport_size({'width':width,'height':844});assert page.evaluate('document.documentElement.scrollWidth<=innerWidth');expect(page.locator('#fuel-purchase-form [name=amount]')).to_be_visible();expect(page.locator('#fuel-purchase-form [name=litre_price]')).to_have_count(0);expect(page.locator('#fuel-purchase-form [name=fuel_type]')).to_have_count(0);page.screenshot(path=str(output/('full-tank-'+str(width)+'.png')),full_page=True)
    # Export only this synthetic schema's fuel view for the standalone HTML.
    rows=page.locator('#purchases .fuel-history-row');generator_id=None
    for row in rows.all():
        if 'Agregat' in row.inner_text():generator_id=int(parse_qs(urlparse(row.get_by_role('link',name='Edytuj',exact=True).get_attribute('href')).query)['purchase'][0]);break
    assert generator_id is not None
    (output/'fuel-snapshot.html').write_text(page.content())
    state={'month':'2026-10','income':{'2026-10':str(income)},'otherCosts':{'2026-10':str(income-before)},'baselines':{'CAR':{'when':'2026-10-01T08:00','meter':'100000','version':0},'GENERATOR':{'when':'2026-10-01T08:00','meter':'100','version':0}},'entries':[{'id':car_id,'target':'CAR','when':'2026-10-12T12:00','meter':'100500','litres':'40','amount':'210','version':3,'cancelled':False},{'id':generator_id,'target':'GENERATOR','when':'2026-10-12T12:00','meter':'110','litres':'10','amount':'50','version':0,'cancelled':False}]}
    (output/'fuel-demo-state.json').write_text(json.dumps(state,ensure_ascii=False))
    print('PASS full-tank: two baseline counters, amount/litres,503 retry, separate diesel/petrol, October profit, edit/delete/restore, measured l/100km and l/mth, four widths.',flush=True)
