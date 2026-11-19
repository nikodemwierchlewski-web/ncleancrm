"""Real authenticated CRM checks; call only against an isolated synthetic installation."""
from playwright.sync_api import expect


def verify_equipment(page, base, output):
    page.goto(base + '/equipment')
    page.get_by_role('link', name='+ Dodaj sprzęt', exact=True).click()
    f = page.locator('#equipment-editor form')
    f.get_by_label('Nazwa', exact=True).fill('Ekstraktor QA')
    f.get_by_label('Producent / model', exact=True).fill('Model testowy')
    # Offline failure must keep the form and token for a deliberate retry.
    token = f.locator('[name=token]').input_value()
    page.context.set_offline(True)
    f.get_by_role('button', name='Zapisz', exact=True).click()
    expect(f.locator('.equipment-error')).not_to_contain_text('Zapisywanie')
    expect(f.get_by_label('Nazwa', exact=True)).to_have_value('Ekstraktor QA')
    assert f.locator('[name=token]').input_value() == token
    page.context.set_offline(False)
    f.get_by_role('button', name='Zapisz', exact=True).click()
    expect(page.get_by_role('heading', name='Ekstraktor QA', exact=True)).to_be_visible()
    asset_url = page.url
    asset_id = asset_url.rsplit('/', 1)[1]
    page.get_by_role('link', name='+ Licznik', exact=True).click()
    f = page.locator('#equipment-editor form')
    f.get_by_label('Nazwa licznika / podzespołu', exact=True).fill('Pompa')
    f.get_by_role('button', name='Zapisz', exact=True).click()
    expect(page.get_by_role('heading', name='Pompa', exact=True)).to_be_visible()
    page.get_by_role('link', name='+ Odczyt / szacunek', exact=True).click()
    f = page.locator('#equipment-editor form')
    f.get_by_label('Wartość licznika', exact=True).fill('101')
    f.get_by_role('button', name='Zapisz', exact=True).click()
    expect(page.locator('.equipment-number')).to_contain_text('101')
    page.get_by_role('link', name='+ Plan serwisu', exact=True).click()
    f = page.locator('#equipment-editor form')
    f.get_by_label('Nazwa czynności', exact=True).fill('Kontrola pompy QA')
    f.get_by_label('Licznik (opcjonalny)', exact=True).select_option(label='Pompa (h)')
    f.get_by_label('Pierwszy próg licznika', exact=True).fill('100')
    f.get_by_label('Powtarzaj co ile jednostek licznika', exact=True).fill('100')
    f.get_by_role('button', name='Zapisz', exact=True).click()
    expect(page.locator('.equipment-plan')).to_contain_text('Próg przekroczony o 1 h')
    # Both the main work screen and help read the same plan.
    page.goto(base + '/today')
    expect(page.locator('#equipment-maintenance')).to_contain_text('Kontrola pompy QA')
    context = page.request.get(base + '/help/context').json()
    assert context['equipmentReminder']['id'].startswith('equipment-plan:')
    page.locator('#equipment-maintenance').get_by_role('link', name='Zapisz wykonanie', exact=True).click()
    f = page.locator('#equipment-editor form')
    f.get_by_label('Rzeczywisty odczyt', exact=True).fill('101')
    f.get_by_label('Koszt (zł, opcjonalnie)', exact=True).fill('80.50')
    f.get_by_label('Notatka', exact=True).fill('Serwis QA')
    committed = []
    def lost_response(route):
        committed.append(route.fetch().status)
        route.abort()
    page.route('**/equipment/execute', lost_response, times=1)
    f.get_by_role('button', name='Zapisz', exact=True).click()
    expect(f.locator('.equipment-error')).not_to_contain_text('Zapisywanie')
    assert committed == [200], committed
    f.get_by_role('button', name='Zapisz', exact=True).click()
    expect(page.locator('#equipment-maintenance')).not_to_contain_text('Kontrola pompy QA')
    assert 'equipmentReminder' not in page.request.get(base + '/help/context').json()
    page.goto(asset_url)
    expect(page.locator('.equipment-plan')).to_contain_text('201.00')
    assert page.locator('.equipment-row').filter(has_text='Serwis QA').count() == 1
    expect(page.locator('main')).to_contain_text('80.50 zł')
    page.get_by_role('button', name='Archiwizuj', exact=True).click()
    expect(page.get_by_role('button', name='Przywróć aktywność', exact=True)).to_be_visible()
    expect(page.locator('main')).to_contain_text('Serwis QA')
    page.get_by_role('button', name='Przywróć aktywność', exact=True).click()
    expect(page.get_by_role('button', name='Archiwizuj', exact=True)).to_be_visible()
    # Actual Thymeleaf pages, not the acceptance HTML.
    for path in ['/equipment', '/equipment/' + asset_id, '/today']:
        page.goto(base + path)
        for width in (360, 390, 768, 1440):
            page.set_viewport_size({'width': width, 'height': 844})
            assert page.evaluate('document.documentElement.scrollWidth <= innerWidth'), (path, width)
            if path.startswith('/equipment/') and width in (390, 1440):
                page.screenshot(path=str(output / f'equipment-live-{width}.png'), full_page=True)
    page.goto(asset_url)
    page.get_by_role('link', name='Edytuj dane', exact=True).click()
    expect(page.locator('#equipment-editor')).to_be_visible()
    page.keyboard.press('Escape')
    expect(page.locator('#equipment-editor')).not_to_be_visible()
    # CSRF rejection and lack of accidental writes on a GET.
    assert page.request.post(base + '/equipment/archive', form={'assetId': asset_id, 'version': '0', 'active': 'false'}).status == 403
    print('PASS real CRM: create, retained offline fields/token, counters, overdue plan, Today/help, lost-response retry, one cost/history entry, archive/restore, 12 responsive views, Escape, CSRF.', flush=True)
