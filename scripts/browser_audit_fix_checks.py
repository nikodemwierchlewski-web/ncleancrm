"""Audit-fix browser regression; use only the disposable live runner."""
from uuid import uuid4
from playwright.sync_api import expect


def verify_audit_fixes(page, base, output):
    def post(path, fields):
        csrf=page.locator('input[name=_csrf]').first.input_value()
        return page.request.post(base+path,form={'_csrf':csrf,**fields})

    page.goto(base+'/settings/services')
    service=page.locator('.service-card').first
    sid=service.get_attribute('data-service-id')
    version=service.locator('.service-recipe [name=version]').input_value()
    assert post('/settings/services/chemicals',{'name':'Środek audyt QA','packageUnit':'ml','packageQuantity':'100','packagePrice':'10'}).ok
    page.reload()
    chemical=page.locator('.chemical-card').filter(has_text='Środek audyt QA').get_attribute('data-chemical-id')
    assert post('/settings/services/'+sid+'/chemistry',{'version':version,'defined':'on','standard_'+chemical:'10'}).ok
    page.goto(base+'/orders')
    f=page.locator('#booking-form')
    category=f.locator('.service-line:has([name=qty_'+sid+'])').get_attribute('data-category')
    f.locator('[data-category-choice="'+category+'"]').click()
    f.locator('[name=qty_'+sid+']').fill('1')
    f.locator('[name=price_'+sid+']').fill('100')
    f.locator('[name=phone]').fill('500008801')
    f.locator('[name=address]').fill('Adres QA')
    f.locator('[name=note]').fill('Pierwotne ustalenia QA')
    token=f.locator('[name=token]').input_value()
    committed=[]
    def lost(route):
        committed.append(route.fetch().status)
        route.abort()
    page.route('**/orders',lost,times=1)
    f.get_by_role('button',name='Zapisz zlecenie',exact=True).click()
    expect(page.locator('#customer-search-status')).not_to_contain_text('Zapisywanie')
    assert committed==[200],committed
    f.locator('[name=price_'+sid+']').fill('900')
    f.locator('[name=note]').fill('Zmienione ustalenia QA')
    with page.expect_response(lambda r:r.request.method=='POST' and r.url.split('?')[0]==base+'/orders') as conflict:
        f.get_by_role('button',name='Zapisz zlecenie',exact=True).click()
    assert conflict.value.status==409
    expect(f.locator('[name=note]')).to_have_value('Zmienione ustalenia QA')
    expect(f.locator('[name=price_'+sid+']')).to_have_value('900')
    expect(f.locator('[name=token]')).to_have_value(token)
    link=page.locator('#replay-recovery a')
    expect(link).to_be_visible();expect(link).to_have_attribute('target','_blank')
    saved_url=link.get_attribute('href');bid=saved_url.rsplit('/',1)[1]
    for width in (360,390,768,1440):
        page.set_viewport_size({'width':width,'height':844})
        assert page.evaluate('document.documentElement.scrollWidth<=innerWidth')
    f.locator('[name=note]').fill('Pierwotne ustalenia QA');f.locator('[name=price_'+sid+']').fill('100')
    f.get_by_role('button',name='Zapisz zlecenie',exact=True).click();page.wait_for_url('**/orders?day=**')
    expect(page.locator('article[id^=order-]')).to_have_count(1)
    page.goto(base+'/work/'+bid)
    expect(page.locator('main')).to_contain_text('Pierwotne ustalenia QA')
    expect(page.locator('main')).not_to_contain_text('Zmienione ustalenia QA')
    f=page.locator('#complete-form')
    assert post('/day-closeout/'+bid,{'date':f.locator('[name=date]').input_value(),'version':f.locator('[name=version]').input_value(),'outcome':'DONE','final_total':'100','note':'Wykonanie QA'}).ok
    assert post('/orders/'+bid+'/items/0/actual',{'version':'1','actual_'+chemical:'8'}).ok
    assert post('/orders/'+bid+'/items/0/actual',{'version':'2','actual_'+chemical:'9'}).ok
    page.reload()
    history=page.locator('details').filter(has=page.locator('summary',has_text='Historia zużycia chemii'))
    history.locator('summary').click()
    expect(history).to_contain_text('8.000 ml');expect(history).to_contain_text('9.000 ml')
    expect(history).to_contain_text('equipment-qa')
    for width in (360,390,768,1440):
        page.set_viewport_size({'width':width,'height':844})
        assert page.evaluate('document.documentElement.scrollWidth<=innerWidth')
        if width in (390,1440):page.screenshot(path=str(output/f'audit-fixes-{width}.png'),full_page=True)
    page.goto(base+'/notes')
    fields={'token':str(uuid4()),'kind':'NOTE','title':'Pierwotna notatka QA','content':'Treść QA'}
    first=post('/notes',fields);assert first.ok
    changed=post('/notes',{**fields,'title':'Zmieniona notatka QA'});assert changed.status==409
    assert changed.json()['href'].endswith(str(first.json()['id']))
    assert post('/notes',fields).json()['id']==first.json()['id']
    print('PASS audit fixes: lost-response -> edited fields rejected with 409 and a context-aware recovery link; original retry creates no duplicate; chemistry history retains 8->9, author and standard; note replay conflict; 8 responsive views.',flush=True)
