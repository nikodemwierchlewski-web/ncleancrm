"""Economic depreciation on the real CRM; requires an isolated synthetic installation."""
from calendar import monthrange
from decimal import Decimal, ROUND_HALF_UP
from urllib.parse import urlencode
from playwright.sync_api import expect


def verify_depreciation(page, base, output):
    def amount(selector):
        return Decimal(''.join(page.locator(selector).inner_text().replace('zł', '').split()).replace(',', '.'))
    def asset(name):
        page.goto(base + '/equipment')
        page.get_by_role('link', name='+ Dodaj sprzęt', exact=True).click()
        f = page.locator('#equipment-editor form')
        f.get_by_label('Nazwa', exact=True).fill(name)
        f.get_by_role('button', name='Zapisz', exact=True).click()
        expect(page.get_by_role('heading', name=name, exact=True)).to_be_visible()
        return page.url
    url = asset('Ekstraktor — amortyzacja QA')
    asset('Sprzęt bez planu QA')
    page.goto(url)
    expect(page.locator('#economic-depreciation')).to_contain_text('Brak planu')
    page.get_by_role('link', name='Ustaw amortyzację', exact=True).click()
    f = page.locator('#equipment-editor form')
    month = f.locator('[name=startMonth]').input_value()
    first = month[:4] + '-01'
    f.get_by_label('Wartość początkowa (zł)', exact=True).fill('3600')
    f.get_by_label('Przewidywana wartość końcowa (zł)', exact=True).fill('0')
    f.get_by_label('Pierwszy miesiąc', exact=True).fill(first)
    f.get_by_label('Łączny okres od pierwszego miesiąca', exact=True).fill('36')
    committed = []
    def lost_response(route):
        committed.append(route.fetch().status)
        route.abort()
    page.route('**/equipment/depreciation', lost_response, times=1)
    f.get_by_role('button', name='Zapisz', exact=True).click()
    expect(f.locator('.equipment-error')).not_to_contain_text('Zapisywanie')
    assert committed == [200], committed
    expect(f.get_by_label('Wartość początkowa (zł)', exact=True)).to_have_value('3600')
    f.get_by_role('button', name='Zapisz', exact=True).click()
    expect(page.locator('[data-economic-monthly]')).to_be_visible()
    assert amount('[data-economic-monthly]') == Decimal('100.00')
    assert page.locator('#economic-depreciation details .equipment-row').count() == 1
    dash = base + '/dashboard?' + urlencode({'date': month + '-01', 'range': 'month', 'mode': 'actual'})
    page.goto(dash)
    expect(page.get_by_label('Uwzględnij amortyzację ekonomiczną', exact=True)).not_to_be_checked()
    before = amount('#remaining')
    assert amount('[data-period-depreciation]') == Decimal('100.00')
    expect(page.locator('.economic-warning')).to_contain_text('Brak planu')
    page.get_by_label('Uwzględnij amortyzację ekonomiczną', exact=True).check()
    page.get_by_role('button', name='Zastosuj', exact=True).click()
    expect(page.get_by_label('Uwzględnij amortyzację ekonomiczną', exact=True)).to_be_checked()
    assert amount('#remaining') == before - Decimal('100.00')
    assert amount('[data-after-depreciation]') == amount('#remaining')
    page.get_by_role('link', name='Poprzedni okres', exact=True).click()
    expect(page.get_by_label('Uwzględnij amortyzację ekonomiczną', exact=True)).to_be_checked()
    page.goto(dash + '&depreciation=true')
    page.get_by_label('Uwzględnij amortyzację ekonomiczną', exact=True).uncheck()
    page.get_by_role('button', name='Zastosuj', exact=True).click()
    assert amount('#remaining') == before
    # A fresh report defaults to the original financial view.
    page.goto(dash)
    expect(page.get_by_label('Uwzględnij amortyzację ekonomiczną', exact=True)).not_to_be_checked()
    today = base + '/today?' + urlencode({'finance': 'month', 'periodDate': month + '-01'})
    page.goto(today)
    base_profit = amount('[data-today-profit]')
    page.get_by_label('Uwzględnij amortyzację ekonomiczną', exact=True).check()
    page.get_by_role('button', name='Zastosuj', exact=True).click()
    assert amount('[data-today-profit]') == base_profit - Decimal('100.00')
    daily = page.locator('[data-chart-select] option').evaluate_all('(els)=>els.map(e=>e.dataset.profit)')
    assert sum(map(Decimal, daily)) == amount('[data-today-profit]')
    for route in [url, dash + '&depreciation=true', today + '&depreciation=true']:
        page.goto(route)
        for width in (360, 390, 768, 1440):
            page.set_viewport_size({'width': width, 'height': 844})
            assert page.evaluate('document.documentElement.scrollWidth<=innerWidth'), (route, width)
            if '/dashboard?' in route and width in (390, 1440):
                page.screenshot(path=str(output / f'depreciation-summary-{width}.png'), full_page=True)
    page.goto(base + '/today?' + urlencode({'finance': 'month-to-date', 'periodDate': month + '-15', 'depreciation': 'true'}))
    partial = (Decimal('100.00') * Decimal(15) / Decimal(monthrange(int(month[:4]), int(month[5:]))[1])).quantize(Decimal('.01'), rounding=ROUND_HALF_UP)
    assert amount('[data-period-depreciation]') == partial
    daily = page.locator('[data-chart-select] option').evaluate_all('(els)=>els.map(e=>e.dataset.profit)')
    assert sum(map(Decimal, daily)) == amount('[data-today-profit]')
    # Change the lifetime, preserving previously completed months.
    page.goto(url)
    page.get_by_role('link', name='Zmień amortyzację', exact=True).click()
    f = page.locator('#equipment-editor form')
    expect(f.locator('[name=startMonth]')).to_have_attribute('readonly', 'readonly')
    f.get_by_label('Łączny okres od pierwszego miesiąca', exact=True).fill('48')
    f.get_by_role('button', name='Zapisz', exact=True).click()
    expect(page.locator('#equipment-editor')).not_to_be_visible()
    completed = int(month[5:]) - 1
    expected = ((Decimal(3600) - Decimal(100) * completed) / Decimal(48 - completed)).quantize(Decimal('.01'), rounding=ROUND_HALF_UP)
    expect(page.locator('[data-economic-monthly]')).to_be_visible()
    assert amount('[data-economic-monthly]') == expected
    assert page.locator('#economic-depreciation details .equipment-row').count() == 2
    if completed:
        previous = month[:4] + '-' + str(int(month[5:]) - 1).zfill(2)
        page.goto(base + '/dashboard?' + urlencode({'date': previous + '-01', 'range': 'month', 'depreciation': 'true'}))
        assert amount('[data-period-depreciation]') == Decimal('100.00')
    page.goto(url)
    page.get_by_role('link', name='Zmień amortyzację', exact=True).click()
    f = page.locator('#equipment-editor form')
    f.get_by_label('Naliczaj amortyzację ekonomiczną', exact=True).uncheck()
    f.get_by_role('button', name='Zapisz', exact=True).click()
    expect(page.locator('#economic-depreciation')).to_contain_text('Wyłączona od')
    assert amount('[data-economic-monthly]') == Decimal('0.00')
    page.goto(dash + '&depreciation=true')
    assert amount('[data-period-depreciation]') == Decimal('0.00')
    assert amount('#remaining') == before
    print('PASS real economic depreciation: 3600/36=100, retained lost-response retry, single revision, opt-in/off report, missing plan warning, period navigation, matching Today daily totals, partial month cents, preserved closed months, stop, 12 responsive views.', flush=True)
