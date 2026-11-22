"""Customer type, category values and returning visits on a fresh synthetic CRM only."""
from datetime import date, timedelta
from decimal import Decimal
from urllib.parse import urlencode
from uuid import uuid4
from playwright.sync_api import expect


def verify_customer_insights(page, base, output):
    def post(path, values):
        token = page.locator('input[name=_csrf]').first.input_value()
        response = page.request.post(base + path, form={**values, '_csrf': token})
        assert response.ok, (path, response.status, response.text()[:250])
        return response

    def amount(selector):
        return Decimal(''.join(page.locator(selector).inner_text().replace('zł', '').split()).replace(',', '.'))

    page.goto(base + '/orders')
    day = (date.fromisoformat(page.locator('#booking-form [name=day]').input_value()) - timedelta(days=1)).isoformat()
    earlier = (date.fromisoformat(day)-timedelta(days=20)).isoformat()
    page.goto(base + '/settings/services')
    for label in ['Kategoria A QA', 'Kategoria B QA']:
        post('/settings/services/category', {'name': label})
    page.reload()
    categories = {}
    for label in ['Kategoria A QA', 'Kategoria B QA']:
        categories[label] = page.locator('.service-category').evaluate_all("(els,label)=>els.find(e=>e.querySelector(':scope > summary').textContent.trim()===label).dataset.categoryId", label)
        post('/settings/services', {'category': categories[label], 'name': label + ' usługa', 'unit': 'szt.', 'price': '100', 'minutes': '30', 'active': 'on'})
    page.reload()
    services = {}
    for label, category in categories.items():
        services[label] = page.locator('.service-category[data-category-id="' + category + '"] .service-card').first.get_attribute('data-service-id')
    a, b = services['Kategoria A QA'], services['Kategoria B QA']
    cat_a, cat_b = categories['Kategoria A QA'], categories['Kategoria B QA']
    page.goto(base + '/customers')
    post('/customers', {'name': 'Firma QA', 'phone': '500004101', 'address': 'Adres QA', 'customer_type': 'BUSINESS', 'source': 'GOOGLE'})
    people = post('/customers/lookup', {'query': '500004101'}).json()
    person = people[0]
    assert person['customer_type'] == 'BUSINESS'
    old = post('/orders', {'calendar': '1', 'token': str(uuid4()), 'day': earlier, 'starts': '09:00',
               'customer': str(person['id']), 'customer_version': str(person['version']), 'customer_type': 'BUSINESS',
               'address': 'Adres QA', 'note': 'Starsza wizyta QA', 'qty_' + a: '1', 'price_' + a: '40'}).json()['id']

    def finish(booking_id, price, split=None):
        page.goto(base + '/work/' + str(booking_id))
        f = page.locator('#complete-form')
        values = {'date': f.locator('[name=date]').input_value(), 'version': f.locator('[name=version]').input_value(),
                  'outcome': 'DONE', 'final_total': str(price), 'minutes': '30', 'note': 'Wykonanie QA'}
        if split:
            values.update(allocations='1', **split)
        post('/day-closeout/' + str(booking_id), values)

    finish(old, 40)
    page.goto(base + '/orders?day=' + day)
    f = page.locator('#booking-form')
    f.locator('#customer-search').fill('500004101')
    f.locator('#customer-results button').first.click()
    expect(f.locator('[name=customer_type]')).to_have_value('BUSINESS')
    expect(f.locator('[name=customer_version]')).to_have_value(str(person['version']))
    category = f.locator('.service-line:has([name=qty_' + a + '])').get_attribute('data-category')
    f.locator('[data-category-choice="' + category + '"]').click()
    f.locator('[name=qty_' + a + ']').fill('2')
    f.locator('[name=price_' + a + ']').fill('50')
    f.locator('[name=note]').fill('Biznesowa realizacja QA')
    f.get_by_role('button', name='Zapisz zlecenie', exact=True).click()
    page.wait_for_url('**/orders?day=**')
    first = page.locator('article[id^=order-]').first.get_attribute('id')[6:]
    page.goto(base + '/work/' + first)
    expect(page.locator('main')).to_contain_text('Rodzaj klienta: Biznesowy')
    finish(first, 100)

    # Repeat pre-fills customer type. A lost response must not update the profile twice.
    page.goto(base + '/orders?' + urlencode({'repeat': first, 'day': day}))
    f = page.locator('#booking-form')
    expect(f.locator('[name=customer_type]')).to_have_value('BUSINESS')
    f.locator('[name=customer_type]').select_option('PRIVATE')
    f.locator('[name=note]').fill('Prywatna realizacja QA')
    f.locator('input[type=checkbox][required]').check()
    committed = []
    def lost_response(route):
        committed.append(route.fetch().status)
        route.abort()
    page.route('**/orders', lost_response, times=1)
    f.get_by_role('button', name='Zapisz zlecenie', exact=True).click()
    expect(page.locator('#customer-search-status')).not_to_contain_text('Zapisywanie')
    assert committed == [200], committed
    expect(f.locator('[name=customer_type]')).to_have_value('PRIVATE')
    expect(f.locator('[name=note]')).to_have_value('Prywatna realizacja QA')
    f.get_by_role('button', name='Zapisz zlecenie', exact=True).click()
    page.wait_for_url('**/orders?day=**')
    second = page.locator('article[id^=order-]').first.get_attribute('id')[6:]
    finish(second, 300)
    page.goto(base + '/work/' + first)
    expect(page.locator('main')).to_contain_text('Rodzaj klienta: Biznesowy')

    mixed = post('/orders', {'calendar': '1', 'token': str(uuid4()), 'day': day, 'starts': '10:00',
                 'name': 'Prywatny QA', 'phone': '500004102', 'customer_type': 'PRIVATE', 'address': 'Adres QA',
                 'note': 'Dwie kategorie QA', 'qty_' + a: '1', 'qty_' + b: '1'}).json()['id']
    finish(mixed, 300)
    def report(kind='ALL', chosen=None, period='day'):
        return base + '/dashboard?' + urlencode({'date': chosen or day, 'range': period, 'mode': 'actual', 'insightType': kind})
    page.goto(report())
    section = page.locator('#customer-insights')
    expect(section.locator('[data-insight-count]')).to_have_text('3')
    assert amount('[data-insight-average]') == Decimal('233.33')
    assert amount('[data-insight-median]') == Decimal('300')
    expect(section).to_contain_text('bez pełnego podziału na kategorie i kwoty: 1')
    ca = section.locator('[data-insight-category="' + cat_a + '"]')
    cb = section.locator('[data-insight-category="' + cat_b + '"]')
    expect(ca.locator('[data-category-count]')).to_have_text('2')
    expect(ca.locator('[data-category-average]')).to_have_text('200,00 zł')
    expect(cb.locator('[data-category-median]')).to_have_text('—')
    expect(section.locator('[data-insight-new]')).to_have_text('1')
    expect(section.locator('[data-insight-returning]')).to_have_text('1')
    expect(section.locator('[data-insight-repeat-visits]')).to_have_text('2')
    assert amount('[data-insight-repeat-revenue]') == Decimal('400')
    assert amount('[data-insight-return-days]') == Decimal('10')

    # Explicit final allocations make the two category samples eligible without changing the overall median.
    finish(mixed, 300, {'amount_0': '120', 'amount_1': '180'})
    page.goto(report())
    expect(page.locator('[data-insight-category="' + cat_a + '"] [data-category-median]')).to_have_text('120,00 zł')
    expect(page.locator('[data-insight-category="' + cat_b + '"] [data-category-median]')).to_have_text('180,00 zł')
    assert amount('[data-insight-average]') == Decimal('233.33')
    page.locator('.insight-filter [name=insightType]').select_option('BUSINESS')
    page.get_by_role('button', name='Filtruj statystyki', exact=True).click()
    expect(page.locator('[data-insight-count]')).to_have_text('1')
    assert amount('[data-insight-average]') == Decimal('100')
    expect(page.locator('[data-insight-returning-share]')).to_have_text('100,0%')
    # Period navigation preserves the local filter.
    page.get_by_role('link', name='Poprzedni okres', exact=True).click()
    expect(page.locator('.insight-filter [name=insightType]')).to_have_value('BUSINESS')
    expect(page.locator('[data-insight-count]')).to_have_text('0')
    expect(page.locator('[data-insight-median]')).to_have_text('—')
    page.get_by_role('link', name='Następny okres', exact=True).click()
    assert amount('[data-insight-average]') == Decimal('100')
    page.goto(report('PRIVATE'))
    expect(page.locator('[data-insight-count]')).to_have_text('2')
    assert amount('[data-insight-average]') == Decimal('300')
    page.goto(report('UNKNOWN'))
    expect(page.locator('[data-insight-count]')).to_have_text('0')
    expect(page.locator('[data-insight-median]')).to_have_text('—')

    # Stale customer metadata must preserve all booking fields for correction.
    page.goto(base + '/orders?' + urlencode({'repeat': first, 'day': day}))
    f = page.locator('#booking-form')
    old_version = f.locator('[name=customer_version]').input_value()
    f.locator('[name=note]').fill('Zachowaj tę notatkę QA')
    f.locator('input[type=checkbox][required]').check()
    f.locator('[name=customer_type]').select_option('BUSINESS')
    post('/customers', {'id': str(person['id']), 'version': old_version, 'name': 'Firma QA', 'phone': '500004101',
                       'address': 'Adres QA', 'active': 'on', 'customer_type': 'BUSINESS'})
    f.get_by_role('button', name='Zapisz zlecenie', exact=True).click()
    expect(page.locator('#customer-search-status')).to_contain_text('Kartoteka klienta zmieniła się')
    expect(f.locator('[name=note]')).to_have_value('Zachowaj tę notatkę QA')
    expect(f.locator('[name=customer_type]')).to_have_value('BUSINESS')

    page.emulate_media(reduced_motion='reduce')
    for target in [report(), report('BUSINESS'), report('UNKNOWN'), base + '/orders?day=' + day, base + '/work/' + first]:
        page.goto(target)
        for width in [360, 390, 768, 1440]:
            page.set_viewport_size({'width': width, 'height': 844})
            assert page.evaluate('document.documentElement.scrollWidth <= innerWidth'), (target, width)
            if target == report() and width in [390, 1440]:
                page.locator('#customer-insights').screenshot(path=str(output / f'customer-insights-{width}.png'))
    for period in ['week', 'month']:
        page.goto(report('BUSINESS', period=period))
        expect(page.locator('.insight-filter [name=insightType]')).to_have_value('BUSINESS')
        expected = Decimal('70') if period == 'month' and earlier[:7] == day[:7] else Decimal('100')
        assert amount('[data-insight-average]') == expected
    plain_context = page.context.browser.new_context(storage_state=page.context.storage_state(), java_script_enabled=False)
    try:
        plain = plain_context.new_page()
        plain.goto(report('BUSINESS'))
        expect(plain.locator('[data-insight-count]')).to_have_text('1')
        expect(plain.locator('[data-insight-average]')).to_have_text('100,00 zł')
    finally:
        plain_context.close()
    print('PASS customer insights: UI business/private selection and repeat prefill, lost-response retry, immutable visit type, category average/median and explicit split, new/returning history, local type/period filters, stale profile preserves fields, 20 responsive views, reduced motion and no-JS read.', flush=True)
