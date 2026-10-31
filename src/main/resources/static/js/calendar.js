(() => {
  'use strict';
  const dialog = document.querySelector('#area-editor');
  if (dialog && typeof dialog.showModal !== 'function') return;
  document.documentElement.classList.remove('no-js');
  if (dialog) {
    const date = document.querySelector('#area-date');
    const label = document.querySelector('#area-editor-date');
    const options = [...dialog.querySelectorAll('[data-area-option]')];
    const count = document.querySelector('#selected-area-count');
    const emptyOptions = document.querySelector('#empty-area-options');
    let opener = null;
    const updateCount = () => {
      const total = options.filter(option => option.querySelector('input').checked).length;
      count.textContent = total ? `Wybrane obszary: ${total}` : 'Dzień bez oznaczenia';
      emptyOptions.hidden = options.some(option => !option.hidden);
    };
    const focusEditor = () => (dialog.querySelector('.form-errors:not([hidden])') || dialog.querySelector('[data-area-option]:not([hidden]) input') || dialog.querySelector('.dialog-close'))?.focus({preventScroll:true});
    document.addEventListener('click', event => {
      const link = event.target.closest('a[data-calendar-day]');
      if (!link || event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
      event.preventDefault();
      opener = link;
      const assigned = new Set([...link.querySelectorAll('[data-assigned-area-id]')].map(area => area.dataset.assignedAreaId));
      date.value = link.dataset.calendarDay;
      label.dateTime = date.value;
      label.textContent = link.dataset.dateLabel;
      for (const option of options) {
        const selected = assigned.has(option.dataset.areaOption);
        option.querySelector('input').checked = selected;
        option.hidden = option.dataset.active !== 'true' && !selected;
      }
      dialog.querySelectorAll('.form-errors').forEach(error => error.hidden = true);
      updateCount(); dialog.showModal(); focusEditor();
    });
    dialog.querySelectorAll('.dialog-close').forEach(link => link.addEventListener('click', event => {
      if (!dialog.open) return;
      event.preventDefault(); dialog.close();
    }));
    dialog.addEventListener('close', () => opener?.focus({preventScroll:true}));
    dialog.addEventListener('click', event => {
      if (event.target !== dialog) return;
      const box = dialog.getBoundingClientRect();
      if (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom) dialog.close();
    });
    dialog.addEventListener('change', updateCount);
    document.querySelector('#clear-areas').addEventListener('click', () => { options.forEach(option => option.querySelector('input').checked = false); updateCount(); });
    if (dialog.open) {
      opener = [...document.querySelectorAll('[data-calendar-day]')].find(day => day.dataset.calendarDay === date.value);
      dialog.close(); dialog.showModal(); focusEditor();
    }
    updateCount();
  }
  const name = document.querySelector('#area-name');
  const color = document.querySelector('#area-color');
  const picker = document.querySelector('#area-color-picker');
  if (name && color && picker) {
    const previewName = document.querySelector('#area-name-preview');
    const previewColor = document.querySelector('#area-color-preview');
    const update = () => {
      previewName.textContent = name.value.trim() || 'Nowy obszar';
      if (/^#[0-9a-f]{6}$/i.test(color.value)) { picker.value = color.value; previewColor.style.backgroundColor = color.value; }
    };
    name.addEventListener('input', update); color.addEventListener('input', update);
    picker.addEventListener('input', () => { color.value = picker.value.toUpperCase(); update(); });
    update();
  }
})();
