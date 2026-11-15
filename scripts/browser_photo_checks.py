"""Photo controls regression, called only inside verify-browser's disposable database."""
import base64
from playwright.sync_api import expect


def verify_photo_controls(page, base):
    page.goto(base + '/calendar?add=true')
    form = page.locator('#booking-form')
    form.locator('[data-category-choice="1"]').click()
    form.locator('[name=qty_2]').fill('1')
    form.locator('[name=name]').fill('Kontrola zdjęć QA')
    form.locator('[name=phone]').fill('500000099')
    form.locator('[name=address]').fill('Adres syntetyczny')
    day = form.locator('[name=day]').input_value()
    form.get_by_role('button', name='Zapisz zlecenie', exact=True).click()
    expect(page.locator('#booking-editor')).not_to_be_visible()
    entry = page.locator('#agenda-orders [data-order]').filter(has_text='Kontrola zdjęć QA')
    expect(entry).to_have_count(1)
    booking = entry.get_attribute('data-order')
    for width in (360, 390, 768, 1440):
        page.set_viewport_size({'width': width, 'height': 844})
        page.goto(base + '/work/' + booking)
        page.locator('#photos>summary').click()
        for help_button in page.locator('#photos .ncleanek-help:visible').all():
            rect = help_button.bounding_box()
            assert 44 <= rect['width'] <= 45 and 44 <= rect['height'] <= 45, rect
        page.locator('[data-service-files]').first.set_input_files({
            'name': 'zdjecie.png', 'mimeType': 'image/png',
            'buffer': base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAIAAAACUFjqAAAAFUlEQVR4nGPccfcXA27AhEduBEsDACFJAqNlg1pJAAAAAElFTkSuQmCC')})
        page.route('**/work/' + booking + '/photos', lambda r: r.fulfill(status=500,content_type='application/json',body='{"error":"Błąd próbny — ponów"}'), times=1)
        page.get_by_role('button', name='Dodaj zdjęcia', exact=True).click()
        expect(page.locator('#photos .photo-status').first).to_contain_text('ponów')
        page.get_by_role('button', name='Dodaj zdjęcia', exact=True).click()
        expect(page.locator('#photos .photo-status').first).to_contain_text('Zapisano')
        page.locator('#photos details>summary').click()
        form = page.locator('#photo-time-form')
        form.locator('[name=start]').fill(day + 'T09:00')
        form.locator('[name=end]').fill(day + 'T09:45')
        before_version = int(form.locator('[name=version]').input_value())
        button = form.get_by_role('button', name='Zapisz godziny', exact=True)
        button.evaluate("e=>e.removeAttribute('type')")
        button.click()
        expect(page.locator('#photos .photo-status').first).to_contain_text('Zapisano')
        expect(page.locator('#service-photo-form [name=version]')).to_have_value(str(before_version + 1))
        expect(page.locator('#complete-form [name=minutes]')).to_have_value('45')
        button = page.locator('#photos .photo-grid button').first
        button.evaluate("e=>e.removeAttribute('type')")
        button.click()
        expect(page.locator('#photos .photo-grid article')).to_have_count(0)
        expect(page.locator('#complete-form [name=minutes]')).to_have_value('45')
    print('PASS photo controls: upload500/retry, manual time and delete; four widths; buttons without explicit type.', flush=True)
