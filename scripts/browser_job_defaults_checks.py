"""Real job defaults and equipment use; run only on a fresh synthetic QA installation."""
from playwright.sync_api import expect


def verify_job_defaults(page, base, output):
    def post(path, values):
        token = page.locator('input[name=_csrf]').first.input_value()
        response = page.request.post(base + path, form={**values, '_csrf': token})
        assert response.ok, (path, response.status, response.text()[:300])
        return response

    page.goto(base + '/equipment')
    page.get_by_role('link', name='+ Dodaj sprzęt', exact=True).click()
    f = page.locator('#equipment-editor form')
    f.get_by_label('Nazwa', exact=True).fill('Odkurzacz — standard QA')
    f.get_by_role('button', name='Zapisz', exact=True).click()
    expect(page.get_by_role('heading', name='Odkurzacz — standard QA', exact=True)).to_be_visible()
    asset_url = page.url
    page.get_by_role('link', name='+ Licznik', exact=True).click()
    f = page.locator('#equipment-editor form')
    f.get_by_label('Nazwa licznika / podzespołu', exact=True).fill('Silnik')
    f.get_by_role('button', name='Zapisz', exact=True).click()
    expect(page.get_by_role('heading', name='Silnik', exact=True)).to_be_visible()

    page.goto(base + '/settings/services')
    service = page.locator('.service-card').first
    service_id = service.get_attribute('data-service-id')
    minutes = int(service.locator('[name=minutes]').input_value()) * 2
    service_version = service.locator('.service-recipe [name=version]').input_value()
    post('/settings/services/chemicals', {'name': 'Chemia domyślna QA', 'packageUnit': 'l', 'packageQuantity': '1', 'packagePrice': '20'})
    page.reload()
    chemical_id = page.locator('.chemical-card').filter(has_text='Chemia domyślna QA').get_attribute('data-chemical-id')
    post('/settings/services/' + service_id + '/chemistry', {'version': service_version, 'defined': 'on', 'standard_' + chemical_id: '10'})

    standards_url = base + '/equipment/standards?service=' + service_id
    page.goto(standards_url)
    page.get_by_text('+ Przypisz licznik', exact=True).click()
    f = page.locator('form.equipment-form').first
    counter_id = f.locator('[name=counterId] option').last.get_attribute('value')
    f.locator('[name=counterId]').select_option(counter_id)
    f.locator('[name=minutes]').fill('12.5')
    committed = []
    def lost_response(route):
        committed.append(route.fetch().status)
        route.abort()
    page.route('**/equipment/standards', lost_response, times=1)
    f.get_by_role('button', name='Zapisz standard', exact=True).click()
    expect(f.locator('.equipment-error')).not_to_contain_text('Zapisywanie')
    assert committed == [200], committed
    expect(f.locator('[name=minutes]')).to_have_value('12.5')
    f.get_by_role('button', name='Zapisz standard', exact=True).click()
    expect(page.locator('.equipment-plan')).to_have_count(1)

    page.goto(base + '/orders')
    f = page.locator('#booking-form')
    line = f.locator('.service-line:has([name=qty_' + service_id + '])')
    category = line.get_attribute('data-category')
    f.locator('[data-category-choice="' + category + '"]').click()
    f.locator('[name=qty_' + service_id + ']').fill('2')
    assert not f.locator('[name=name]').evaluate('(e)=>e.required')
    f.locator('[name=phone]').fill('500000903')
    f.locator('[name=address]').fill('Adres testowy')
    expect(f.locator('[name=note]')).to_be_visible()
    expect(f.locator('[name=note]')).to_have_attribute('required', '')
    f.get_by_role('button', name='Zapisz zlecenie', exact=True).click()
    assert not f.locator('[name=note]').evaluate('(e)=>e.checkValidity()')
    expect(f.locator('[name=phone]')).to_have_value('500000903')
    f.locator('[name=note]').fill('   ')
    with page.expect_response(lambda r: r.request.method == 'POST' and r.url.split('?')[0] == base + '/orders') as rejected:
        f.get_by_role('button', name='Zapisz zlecenie', exact=True).click()
    assert rejected.value.status == 400
    expect(page.locator('#customer-search-status')).to_contain_text('notatkę')
    expect(f.locator('[name=note]')).to_have_value('   ')
    expect(f.locator('[name=qty_' + service_id + ']')).to_have_value('2')
    f.locator('[name=note]').fill('Ustalenia testowe')
    day = f.locator('[name=day]').input_value()
    f.get_by_role('button', name='Zapisz zlecenie', exact=True).click()
    page.wait_for_url('**/orders?day=**')
    expect(page.locator('main')).to_contain_text('+48500000903')
    page.locator('article[id^=order-]').get_by_role('link', name='Wykonanie i płatności', exact=True).click()
    job_url = page.url
    booking_id = job_url.rsplit('/', 1)[1]
    expect(page.get_by_role('heading', name='+48500000903', exact=False)).to_be_visible()
    expect(page.locator('#job-equipment')).to_contain_text('25.0000 min')
    f = page.locator('#complete-form')
    f.locator('[name=outcome][value=DONE]').check()
    f.locator('.completion-extra').evaluate('(e)=>e.open=true')
    expect(f.locator('[name=minutes]')).to_have_value('')
    expect(f.locator('[name=minutes]')).to_have_attribute('placeholder', f'Standard: {minutes} min')
    f.locator('[name=chem_0]').select_option('MEASURED')
    expect(f.locator('[name=usage_0_' + chemical_id + ']')).to_have_value('20.000')
    f.locator('[name=chem_0]').select_option('KEEP')
    f.locator('[name=note]').fill('Wykonanie testowe')
    f.get_by_role('button', name='Zapisz wykonanie', exact=True).click()
    page.wait_for_url('**/work/' + booking_id + '?saved=1')
    f = page.locator('#complete-form')
    expect(f.locator('[name=chem_0] option:checked')).to_have_text('Zachowaj standard (szacunek)')
    expect(f.locator('[name=minutes]')).to_have_value(str(minutes))
    page.locator('.completion-extra').evaluate('(e)=>e.open=true')
    expect(page.locator('.completion-extra')).to_contain_text('Zapisany czas pochodzi ze standardu')
    page.goto(asset_url)
    expect(page.locator('main')).to_contain_text('Szacunek: 0.42 h')
    expect(page.locator('main')).to_contain_text('Z ujętych zleceń: 0.42 h')
    expect(page.locator('main')).to_contain_text('Brak odczytu')

    usage_url = base + '/equipment/jobs/' + booking_id
    page.goto(usage_url)
    f = page.locator('form.equipment-form')
    f.locator('[name=minutes_' + counter_id + ']').fill('30')
    page.context.set_offline(True)
    f.get_by_role('button', name='Zapisz czasy sprzętu', exact=True).click()
    expect(f.locator('.equipment-error')).not_to_contain_text('Zapisywanie')
    expect(f.locator('[name=minutes_' + counter_id + ']')).to_have_value('30')
    page.context.set_offline(False)
    f.get_by_role('button', name='Zapisz czasy sprzętu', exact=True).click()
    expect(page.locator('#job-equipment')).to_contain_text('30.00 min')
    # An older form must preserve typed values on a version conflict.
    old = page.context.new_page()
    old.goto(usage_url)
    f = page.locator('form.equipment-form')
    f.locator('[name=noEquipment]').check()
    f.get_by_role('button', name='Zapisz czasy sprzętu', exact=True).click()
    expect(page.locator('#job-equipment')).to_contain_text('Zadeklarowano: nie użyto sprzętu')
    stale = old.locator('form.equipment-form')
    stale.locator('[name=minutes_' + counter_id + ']').fill('45')
    stale.get_by_role('button', name='Zapisz czasy sprzętu', exact=True).click()
    expect(stale.locator('.equipment-error')).to_contain_text('innym oknie')
    expect(stale.locator('[name=minutes_' + counter_id + ']')).to_have_value('45')
    old.close()
    f = page.locator('form.equipment-form')
    f.locator('[name=noEquipment]').uncheck()
    f.locator('[name=minutes_' + counter_id + ']').fill('')
    f.get_by_role('button', name='Zapisz czasy sprzętu', exact=True).click()
    expect(page.locator('#job-equipment')).to_contain_text('25.0000 min')

    page.goto(job_url)
    f = page.locator('#complete-form')
    f.locator('[name=chem_0]').select_option('MEASURED')
    f.locator('[name=usage_0_' + chemical_id + ']').fill('7')
    f.locator('.completion-extra').evaluate('(e)=>e.open=true')
    f.locator('[name=minutes]').fill('17')
    f.get_by_role('button', name='Zapisz wykonanie', exact=True).click()
    expect(page.locator('[name=chem_0] option:checked')).to_have_text('Zachowaj zapisany pomiar')
    page.locator('.completion-extra').evaluate('(e)=>e.open=true')
    expect(page.locator('[name=minutes]')).to_have_value('17')
    page.goto(asset_url)
    expect(page.locator('main')).to_contain_text('Szacunek: 0.42 h')
    for route in (standards_url, usage_url, job_url, base + '/day-closeout?date=' + day, asset_url, base + '/today', base + '/orders?day=' + day):
        page.goto(route)
        for width in (360, 390, 768, 1440):
            page.set_viewport_size({'width': width, 'height': 844})
            assert page.evaluate('document.documentElement.scrollWidth <= innerWidth'), (route, width)
            if '/orders?day=' in route and width == 390:
                page.locator('#booking-form [name=note]').scroll_into_view_if_needed()
                page.screenshot(path=str(output / 'booking-note-390.png'), full_page=True)
            if route == usage_url and width in (390, 1440):
                page.screenshot(path=str(output / f'job-usage-{width}.png'), full_page=True)
    page.emulate_media(reduced_motion='reduce')
    page.goto(usage_url)
    page.keyboard.press('Tab')
    assert page.locator(':focus').count() == 1
    assert page.request.post(base + '/equipment/jobs/usage', form={'bookingId': booking_id}).status == 403
    print('PASS job standards, lost-response retry, phone-only customer, required creation note and preserved rejected fields, standard chemistry/time without photos, custom measurements, equipment completion/correction/zero/reset, offline values, stale version, 28 responsive views, keyboard, reduced motion, CSRF.', flush=True)
