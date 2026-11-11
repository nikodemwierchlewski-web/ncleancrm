(() => {
  'use strict';
  const $ = selector => document.querySelector(selector);
  const main = $('.daily');
  if (!main) return;
  const cells = [...document.querySelectorAll('[data-calendar-day]')];
  const parse = value => new Date(`${value}T12:00:00`);
  const iso = date => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  let selected = main.dataset.selected;
  const openers = new WeakMap();
  const cellFor = day => cells.find(cell => cell.dataset.calendarDay === day);

  function open(dialog) {
    openers.set(dialog, document.activeElement);
    if (!dialog.open) dialog.showModal();
  }
  function renderWeek() {
    const start = parse(selected);
    start.setDate(start.getDate() - (start.getDay() + 6) % 7);
    $('#week-strip').replaceChildren();
    for (let index = 0; index < 7; index++) {
      const date = new Date(start);
      date.setDate(date.getDate() + index);
      const day = iso(date), button = document.createElement('button');
      button.type = 'button';
      button.dataset.weekDay = day;
      button.setAttribute('aria-pressed', String(day === selected));
      button.classList.toggle('selected', day === selected);
      const weekday = document.createElement('small');
      weekday.textContent = date.toLocaleDateString('pl-PL', {weekday: 'short'});
      button.append(weekday, document.createTextNode(String(date.getDate())));
      const cell = cellFor(day);
      let description = date.toLocaleDateString('pl-PL', {weekday: 'long', day: 'numeric', month: 'long'});
      if (cell) {
        const bands = document.createElement('span');
        bands.className = 'week-bands';
        for (const band of cell.querySelectorAll('.area-bands i')) bands.append(band.cloneNode(true));
        button.append(bands);
        description += `; ${cell.querySelector('.day-areas').textContent.trim() || 'bez obszaru'}; ${cell.querySelectorAll('.booking-chip').length} zleceń`;
      }
      button.setAttribute('aria-label', description);
      $('#week-strip').append(button);
    }
  }
  function select(day, updateUrl = true) {
    selected = day;
    const cell = cellFor(day);
    for (const item of cells) item.classList.toggle('is-selected', item === cell);
    const label = cell.dataset.dateLabel;
    $('#agenda-title').textContent = label;
    $('#selected-date-label').textContent = parse(day).toLocaleDateString('pl-PL', {day: 'numeric', month: 'long'});
    $('#agenda-areas').replaceChildren(...[...cell.querySelector('.day-areas').children].map(node => node.cloneNode(true)));
    $('#agenda-orders').replaceChildren();
    for (const order of cell.querySelectorAll('.booking-chip')) {
      const copy = order.cloneNode(true);
      copy.classList.remove('extra-booking');
      $('#agenda-orders').append(copy);
    }
    if (!$('#agenda-orders').children.length) {
      const empty = document.createElement('p');
      empty.textContent = 'Brak zleceń. Dodaj pierwsze przyciskiem + Zlecenie.';
      $('#agenda-orders').append(empty);
    }
    $('#day-agenda').scrollTop = 0;
    $('#area-date').value = day;
    $('#restore-weekly-date').value=day;
    $('#area-source').textContent=cell.dataset.scheduleSource;
    $('#area-editor-date').textContent = label;
    $('#area-editor-date').dateTime = day;
    renderWeek();
    if (updateUrl) {
      const url = new URL(location.href);
      url.searchParams.set('day', day);
      url.searchParams.delete('saved');
      history.replaceState(null, '', url);
    }
  }
  function choose(day) {
    if (!cellFor(day)) {
      location.href = `/calendar?day=${encodeURIComponent(day)}`;
      return;
    }
    select(day);
    main.classList.remove('month-open');
    $('#toggle-month').setAttribute('aria-expanded', 'false');
  }
  function showAreas() {
    const assigned = new Set([...cellFor(selected).querySelectorAll('[data-assigned-area-id]')].map(node => node.dataset.assignedAreaId));
    for (const option of document.querySelectorAll('[data-area-option]')) {
      const checked = assigned.has(option.dataset.areaOption);
      option.querySelector('input').checked = checked;
      option.hidden = option.dataset.active !== 'true' && !checked;
    }
    $('#empty-area-options').hidden = [...document.querySelectorAll('[data-area-option]')].some(option => !option.hidden);
    $('#area-details').open = true;
    open($('#area-editor'));
  }
  for (const cell of cells) {
    const color = cell.querySelector('.area-bands i');
    if (color) cell.style.setProperty('--area-color', color.style.background);
    const count = cell.querySelectorAll('.booking-chip').length;
    cell.dataset.count = `${count} zlec.`;
    cell.classList.toggle('has-orders', count > 0);
  }
  document.addEventListener('click', event => {
    const day = event.target.closest('[data-select-day],[data-week-day]');
    if (day) { choose(day.dataset.selectDay || day.dataset.weekDay); return; }
    const order = event.target.closest('[data-order]');
    if (order) {
      $('#order-content').replaceChildren($(`#order-${order.dataset.order}`).content.cloneNode(true));
      open($('#order-editor'));
      return;
    }
    const cell = event.target.closest('[data-calendar-day]');
    if (cell) { choose(cell.dataset.calendarDay); return; }
    if (event.target.closest('.add-booking')) {
      $('#booking-form [name=day]').value = selected;
      open($('#booking-editor'));
      return;
    }
    if (event.target.closest('[data-close]')) event.target.closest('dialog').close();
  });
  for (const dialog of document.querySelectorAll('dialog')) {
    dialog.addEventListener('close', () => openers.get(dialog)?.focus({preventScroll: true}));
    dialog.addEventListener('click', event => {
      if (event.target !== dialog) return;
      const box = dialog.getBoundingClientRect();
      if (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom) dialog.close();
    });
  }
  $('#edit-day-areas').addEventListener('click', showAreas);
  $('#toggle-month').addEventListener('click', () => {
    const shown = main.classList.toggle('month-open');
    $('#toggle-month').setAttribute('aria-expanded', String(shown));
  });
  $('#jump-month').addEventListener('change', event => {
    if (event.target.validity.valid && event.target.value) location.href = `/calendar?month=${event.target.value}`;
  });
  for (const [id, delta] of [['previous-week', -7], ['next-week', 7]]) {
    $(`#${id}`).addEventListener('click', () => {
      const date = parse(selected);
      date.setDate(date.getDate() + delta);
      const day = iso(date);
      if (day >= '2000-01-01' && day <= '2100-12-31') choose(day);
    });
  }
  $('#clear-areas').addEventListener('click', () => document.querySelectorAll('[data-area-option] input').forEach(input => input.checked = false));
  document.addEventListener('submit', event => {
    const form = event.target;
    if (!form.matches('#day-area-form,#restore-weekly-form,.order-update') || event.defaultPrevented) return;
    event.preventDefault();
    window.NcleanForms.submit(form, form.querySelector('.save-error'), () => {
      const day = form.querySelector('[name=day]')?.value || selected;
      location.href = `/calendar?day=${encodeURIComponent(day)}&saved=1`;
    });
  });
  const updateHeight = () => document.documentElement.style.setProperty('--available-height', `${window.visualViewport?.height || innerHeight}px`);
  window.visualViewport?.addEventListener('resize', updateHeight);
  updateHeight();
  select(selected, false);
  if(new URLSearchParams(location.search).get('add')==='true'){ $('#booking-form [name=day]').value=selected;open($('#booking-editor')); }
  if ($('#calendar-status').textContent) setTimeout(() => { $('#calendar-status').textContent = ''; }, 4000);
})();
