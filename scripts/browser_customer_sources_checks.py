"""Acquisition bars on the real CRM. Only for a fresh isolated synthetic installation."""
from datetime import date, timedelta
from decimal import Decimal
from urllib.parse import urlencode
from uuid import uuid4
from playwright.sync_api import expect


def verify_customer_sources(page, base, output):
    def post(path, values):
        token = page.locator('input[name=_csrf]').first.input_value()
        response = page.request.post(base + path, form={**values, '_csrf': token})
        assert response.ok, (path, response.status, response.text()[:250])
        return response

    def number(selector):
        return Decimal(''.join(page.locator(selector).inner_text().replace('zł', '').split()).replace(',', '.'))

    def report(mode='actual', chosen=None):
        return base + '/dashboard?' + urlencode({'date': chosen or day, 'range': 'day', 'mode': mode})

    page.goto(base + '/orders')
    day = (date.fromisoformat(page.locator('#booking-form [name=day]').input_value()) - timedelta(days=1)).isoformat()
    service_id = page.locator('#booking-form .service-line input').first.get_attribute('name')[4:]
    page.goto(report())
    expect(page.locator('#customer-sources .source-empty')).to_be_visible()
    expect(page.locator('[data-source-total-clients]')).to_have_text('0')
    expect(page.locator('[data-source-total-bookings]')).to_have_text('0')

    def booking(name, phone, source, price, customer=None, historical=False, done=True):
        values = {'calendar': '1', 'token': str(uuid4()), 'day': day, 'starts': '09:00', 'name': name,
                  'phone': phone, 'address': 'Adres QA', 'source': source, 'note': 'Ustalenia testowe',
                  'qty_' + service_id: '1', 'price_' + service_id: str(price)}
        if customer:
            values['customer'] = customer
        if historical:
            values.update(historical='true', name='', phone='', address='')
        booking_id = post('/orders', values).json()['id']
        page.goto(base + '/work/' + str(booking_id))
        customer_id = None if historical else page.get_by_role('link', name='Klient', exact=True).get_attribute('href').rsplit('/', 1)[1]
        if done and not historical:
            version = page.locator('#complete-form [name=version]').input_value()
            post('/day-closeout/' + str(booking_id), {'date': day, 'version': version, 'outcome': 'DONE',
                 'final_total': str(price), 'minutes': '30', 'note': 'Wykonanie QA', 'chem_0': 'ZERO', 'actualVersion_0': '0'})
        return customer_id

    google = booking('Google QA', '500002101', 'GOOGLE', 100)
    booking('', '', 'UNKNOWN', 50, customer=google)
    booking('Facebook QA', '500002102', 'FACEBOOK', 200)
    booking('', '', 'UNKNOWN', 30, historical=True)
    booking('OLX QA', '500002103', 'OLX', 300, done=False)
    page.goto(report())
    section = page.locator('#customer-sources')
    expect(page.locator('[data-source-total-clients]')).to_have_text('2')
    expect(page.locator('[data-source-total-bookings]')).to_have_text('4')
    assert number('[data-source-total-revenue]') == Decimal('380')
    expect(section).to_contain_text('Zlecenia bez powiązanej kartoteki: 1')
    expect(section).to_contain_text('Niepotwierdzone kwoty końcowe: 1')
    expect(page.locator('[data-source-series=clients] [data-source-code=GOOGLE]')).to_contain_text('1 · 50,0%')
    section.locator('[name=source-metric][value=bookings]').check()
    expect(page.locator('[data-source-series=bookings]')).to_be_visible()
    expect(page.locator('[data-source-series=clients]')).not_to_be_visible()
    expect(page.locator('[data-source-series=bookings] [data-source-code=GOOGLE]')).to_contain_text('2 · 50,0%')
    section.locator('[name=source-metric][value=revenue]').check()
    expect(page.locator('[data-source-series=revenue] [data-source-code=GOOGLE]')).to_contain_text('150,00 zł · 39,5%')
    page.locator('#dashboard-mode').select_option('plan')
    page.get_by_role('button', name='Pokaż podsumowanie', exact=True).click()
    expect(page.locator('[data-source-total-clients]')).to_have_text('3')
    expect(page.locator('[data-source-total-bookings]')).to_have_text('5')
    assert number('[data-source-total-revenue]') == Decimal('680')
    page.get_by_role('link', name='Poprzedni okres', exact=True).click()
    expect(page.locator('#customer-sources .source-empty')).to_be_visible()
    page.get_by_role('link', name='Następny okres', exact=True).click()
    assert number('[data-source-total-revenue]') == Decimal('680')

    # A correction of the existing customer source reattributes all their visits, without changing revenue.
    page.goto(base + '/customers?query=500002101')
    form = page.locator('article.card form').first
    values = form.evaluate('(f)=>Object.fromEntries(new FormData(f))')
    values.update(source='REFERRAL', referral_note='Polecenie QA')
    post('/customers', values)
    page.goto(report())
    expect(page.locator('[data-source-series=clients] [data-source-code=REFERRAL]')).to_contain_text('1 · 50,0%')
    expect(page.locator('[data-source-series=clients] [data-source-code=GOOGLE]')).to_contain_text('0 · 0,0%')
    assert number('[data-source-total-revenue]') == Decimal('380')
    page.locator('.source-explanation').evaluate('(e)=>e.open=true')
    expect(page.locator('.source-explanation')).to_contain_text('obecnego źródła w kartotece')

    for period in ['week', 'month']:
        page.goto(base + '/dashboard?' + urlencode({'date': day, 'range': period, 'mode': 'actual'}))
        expect(page.locator('[data-source-total-clients]')).to_have_text('2')
        expect(page.locator('[data-source-total-bookings]')).to_have_text('4')
        assert number('[data-source-total-revenue]') == Decimal('380')
    page.goto(report())

    # Native radio keyboard behaviour controls the visible series; no pointer required.
    radio = page.locator('[name=source-metric][value=clients]')
    radio.focus()
    page.keyboard.press('ArrowRight')
    expect(page.locator('[name=source-metric][value=bookings]')).to_be_checked()
    expect(page.locator('[data-source-series=bookings]')).to_be_visible()
    page.emulate_media(reduced_motion='reduce')
    for mode, chosen in [('actual', day), ('plan', day), ('actual', (date.fromisoformat(day)-timedelta(days=1)).isoformat())]:
        page.goto(report(mode, chosen))
        for metric in ['clients', 'bookings', 'revenue']:
            page.locator('[name=source-metric][value=' + metric + ']').check()
            chart = page.locator('[data-source-series=' + metric + ']')
            expect(chart).to_be_visible()
            assert chart.locator('li').count() == 6
            for width in [360, 390, 768, 1440]:
                page.set_viewport_size({'width': width, 'height': 844})
                assert page.evaluate('document.documentElement.scrollWidth <= innerWidth'), (mode, metric, width)
                assert chart.locator('.source-track span').evaluate_all('(bars)=>bars.every(b=>b.getBoundingClientRect().width<=b.parentElement.clientWidth+1)')
                if mode == 'actual' and chosen == day and metric == 'revenue' and width in [390, 1440]:
                    page.locator('#customer-sources').screenshot(path=str(output / f'customer-sources-{width}.png'))

    # The same numbers remain readable if JavaScript is unavailable.
    plain_context = page.context.browser.new_context(storage_state=page.context.storage_state(), java_script_enabled=False)
    try:
        plain = plain_context.new_page()
        plain.goto(report())
        expect(plain.locator('[data-source-total-clients]')).to_have_text('2')
        for metric in ['clients', 'bookings', 'revenue']:
            expect(plain.locator('[data-source-series=' + metric + ']')).to_be_visible()
        expect(plain.locator('.source-picker')).not_to_be_visible()
    finally:
        plain_context.close()
    anonymous = page.context.browser.new_context()
    try:
        visitor = anonymous.new_page()
        visitor.goto(report())
        assert visitor.url.endswith('/login'), visitor.url
        assert visitor.locator('#customer-sources').count() == 0
    finally:
        anonymous.close()
    print('PASS customer sources: repeated visits, clients/orders/revenue, missing source/identity/price, actual/plan, day/week/month and period navigation, profile correction, 36 responsive chart views, keyboard, reduced motion, no-JS fallback and login protection.', flush=True)
