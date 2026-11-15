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

  const compact=matchMedia('(max-width:900px)'),agenda=$('#day-agenda'),sheet=$('#agenda-sheet');let returnToAgenda=false;
  function showAgenda(preserve=false){if(!compact.matches)return;const previous=openers.get(sheet);$('#agenda-sheet-content').append(agenda);open(sheet);if(document.activeElement===document.body||openers.get(sheet)===document.body)openers.set(sheet,cellFor(selected)?.querySelector('.day-select'));if(preserve&&previous)openers.set(sheet,previous);}
  function restoreAgenda(){if(!sheet.open)$('#agenda-slot').append(agenda);}
  compact.addEventListener('change',()=>{if(!compact.matches){returnToAgenda=false;if(sheet.open)sheet.close();}restoreAgenda();});
  function open(dialog) {
    if(dialog!==sheet&&sheet.open){sheet.close();returnToAgenda=true;}
    openers.set(dialog, document.activeElement);
    if (!dialog.open) dialog.showModal();
  }
  function select(day, updateUrl = true) {
    const previous=selected;selected = day;
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

    if (updateUrl) {
      const url = new URL(location.href);
      url.searchParams.set('day', day);
      url.searchParams.delete('saved');
      history.replaceState(null, '', url);
      document.dispatchEvent(new CustomEvent('crm:day',{detail:{day,previous}}));
    }
  }
  function choose(day) {
    if (!cellFor(day)) {
      location.href = Nclean.url(`/calendar?day=${encodeURIComponent(day)}`);
      return;
    }
    select(day);
    showAgenda();
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
    cell.dataset.count = `${count} zlec.`;const countLabel=document.createElement('small');countLabel.className='calendar-count';countLabel.textContent=count?count+' zlec.':'';cell.append(countLabel);const dayButton=cell.querySelector('.day-select');dayButton.setAttribute('aria-label',cell.dataset.dateLabel+'; '+count+' zleceń; '+(cell.querySelector('.day-areas').textContent.trim()||'bez obszaru')+' — pokaż szczegóły');
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
    if (event.target.closest('.add-booking,[data-add-booking]')) {
      $('#booking-form [name=day]').value = selected;
      open($('#booking-editor'));
      return;
    }
    if (event.target.closest('[data-close]')) event.target.closest('dialog').close();
  });
  for (const dialog of document.querySelectorAll('dialog')) {
    dialog.addEventListener('close', () => {if(dialog===sheet)restoreAgenda();else if(returnToAgenda&&compact.matches){returnToAgenda=false;showAgenda(true);return;}openers.get(dialog)?.focus({preventScroll:true});});
    dialog.addEventListener('click', event => {
      if (event.target !== dialog) return;
      const box = dialog.getBoundingClientRect();
      if (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom) dialog.close();
    });
  }
  $('#edit-day-areas').addEventListener('click', showAreas);
  $('#jump-month').addEventListener('change', event => {
    if (event.target.validity.valid && event.target.value) location.href = Nclean.url(`/calendar?month=${event.target.value}`);
  });
  $('#clear-areas').addEventListener('click', () => document.querySelectorAll('[data-area-option] input').forEach(input => input.checked = false));
  document.addEventListener('submit', event => {
    const form = event.target;
    if (!form.matches('#day-area-form,#restore-weekly-form,.order-update') || event.defaultPrevented) return;
    event.preventDefault();
    window.NcleanForms.submit(form, form.querySelector('.save-error'), () => {
      const day = form.querySelector('[name=day]')?.value || selected;
      location.href = Nclean.url(`/calendar?day=${encodeURIComponent(day)}&saved=1`);
    });
  });
  const updateHeight = () => document.documentElement.style.setProperty('--available-height', `${window.visualViewport?.height || innerHeight}px`);
  window.visualViewport?.addEventListener('resize', updateHeight);
  updateHeight();
  select(selected, false);
  if(main.dataset.areaErrors==='true')showAreas();
  if(new URLSearchParams(location.search).get('saved')&&!new URLSearchParams(location.search).has('add'))showAgenda();
  if(new URLSearchParams(location.search).get('add')==='true'){ $('#booking-form [name=day]').value=selected;open($('#booking-editor')); }
  if ($('#calendar-status').textContent) setTimeout(() => { $('#calendar-status').textContent = ''; }, 4000);
})();
