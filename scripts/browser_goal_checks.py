"""Real UI checks for the explicit main goal, run by verify-browser.py."""
from playwright.sync_api import expect


def verify_main_goal(page, base, output):
    page.goto(base + '/notes')
    page.locator('[data-note-new]').click()
    form = page.locator('#note-form')
    form.locator('[name=kind]').select_option('GOAL')
    form.locator('[name=title]').fill('Przygotować sezon — cel testowy')
    form.locator('[name=pinned]').check()
    for text in ('Wybrać zdjęcia', 'Uzupełnić ofertę'):
        form.locator('#add-note-step').click()
        form.locator('[data-step-text]').last.fill(text)
    form.locator('#add-note-step').click()
    form.locator('[data-step-text]').last.fill('Krok tymczasowy do usunięcia')
    form.locator('[data-step-text]').last.blur()
    form.locator('[data-remove-step]').last.click(position={'x': 38, 'y': 22})
    expect(form.locator('.note-step-editor')).to_have_count(2)
    form.get_by_role('button', name='Zapisz', exact=True).click()
    expect(page.locator('#note-editor')).not_to_be_visible()
    assert page.request.get(base + '/notes/main-goal').json()['selected'] is False
    card = page.locator('.memo-card').filter(has_text='Przygotować sezon — cel testowy')
    card.get_by_role('button', name='Ustaw jako główny cel', exact=True).click()
    expect(card.locator('.memo-main')).to_be_visible()
    goal_id = page.request.get(base + '/notes/main-goal').json()['goal']['id']
    for width in (360, 390, 768, 1440):
        page.set_viewport_size({'width': width, 'height': 844})
        page.goto(base + '/dashboard')
        goal = page.locator('.main-goal-card')
        expect(goal.locator('h2')).to_have_text('Przygotować sezon — cel testowy')
        expect(goal.locator('.goal-count')).to_have_text('0 z 2 kroków')
        assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
        goal.scroll_into_view_if_needed()
        for control in goal.locator('button,a').all():
            box = control.bounding_box()
            assert box['height'] >= 44, (width, box)
        page.screenshot(path=str(output / ('main-goal-dashboard-' + str(width) + '.png')), full_page=True)
        page.goto(base + '/calendar')
        expect(page.locator('.main-goal-compact')).to_have_count(0)
        expect(page.locator('#month-overview')).to_be_visible()
        assert len(page.locator('.calendar-grid').evaluate('e=>getComputedStyle(e).gridTemplateColumns.split(" ")'))==7
        assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
        page.screenshot(path=str(output / ('main-goal-calendar-' + str(width) + '.png')), full_page=True)
    page.goto(base + '/dashboard')
    page.evaluate("window.qaGoals=[];document.addEventListener('nclean:committed',e=>qaGoals.push(e.detail.kind))")
    goal = page.locator('.main-goal-card')
    path = '**/notes/' + str(goal_id) + '/action'
    page.route(path, lambda r: r.fulfill(status=503, content_type='application/json', body='{"message":"Próba błędu. Ponów zapis."}'), times=1)
    goal.get_by_role('button', name='□ Wybrać zdjęcia', exact=True).click()
    expect(goal.locator('.main-goal-error')).to_contain_text('Ponów zapis')
    expect(goal.locator('.goal-count')).to_have_text('0 z 2 kroków')
    assert page.evaluate('qaGoals.length') == 0
    goal.get_by_role('button', name='□ Wybrać zdjęcia', exact=True).click()
    expect(goal.locator('.goal-count')).to_have_text('1 z 2 kroków')
    expect(goal.get_by_role('button', name='□ Uzupełnić ofertę', exact=True)).to_be_focused()
    goal.get_by_role('button', name='□ Uzupełnić ofertę', exact=True).click()
    expect(goal.locator('.goal-count')).to_have_text('2 z 2 kroków')
    assert not page.request.get(base + '/notes/main-goal').json()['goal']['completed']
    goal.get_by_role('button', name='✓ Potwierdź osiągnięcie celu', exact=True).click()
    expect(goal).to_have_class('main-goal-card is-achieved')
    assert page.evaluate("qaGoals.filter(k=>k==='goal').length") == 1
    expect(page.locator('.tycoon-badge.is-earned').filter(has_text='Cel osiągnięty')).to_have_count(1)
    page.reload()
    expect(goal).to_have_class('main-goal-card is-achieved')
    expect(page.locator('.nclean-stars')).to_have_count(0)
    page.goto(base + '/notes?state=done')
    card = page.locator('[data-note-id="' + str(goal_id) + '"]')
    card.get_by_role('button', name='Archiwizuj', exact=True).click()
    expect(card).to_have_count(0)
    assert page.request.get(base + '/notes/main-goal').json()['selected'] is False
    page.goto(base + '/notes')
    page.locator('[data-note-new]').click()
    form = page.locator('#note-form')
    form.locator('[name=kind]').select_option('GOAL')
    form.locator('[name=title]').fill('Cel bez kroków')
    form.locator('[name=mainGoal]').check()
    form.get_by_role('button', name='Zapisz', exact=True).click()
    expect(page.locator('#note-editor')).not_to_be_visible()
    page.goto(base + '/dashboard')
    expect(page.locator('.goal-count')).to_contain_text('bez kroków')
    assert page.request.get(base + '/notes/main-goal').json()['goal']['percent'] is None
    page.emulate_media(reduced_motion='reduce')
    page.locator('.main-goal-card').get_by_role('button', name='✓ Cel osiągnięty', exact=True).click()
    expect(page.locator('.main-goal-card')).to_have_class('main-goal-card is-achieved')
    expect(page.locator('.nclean-stars')).to_have_count(0)
    page.emulate_media(reduced_motion='no-preference')
    print('PASS main goal: explicit selection, four widths, touch controls, steps,503 retry, explicit completion, no reload celebration, archive, no fake percent, reduced motion.', flush=True)
