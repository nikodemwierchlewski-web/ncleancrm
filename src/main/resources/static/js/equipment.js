(() => {
  'use strict';
  let busy = false, opener = null;
  const dialog = document.querySelector('#equipment-editor');
  const content = document.querySelector('[data-equipment-editor-content]');
  const live = document.querySelector('.equipment-live');
  const url = path => window.Nclean ? Nclean.url(path) : path;
  document.addEventListener('click', async event => {
    const close = event.target.closest('[data-equipment-close]');
    if (close && !busy) dialog?.close();
    const link = event.target.closest('[data-equipment-edit]');
    if (!link || !dialog || busy) return;
    event.preventDefault();
    // Keep the current form and its values until the user closes it deliberately.
    if (dialog.open) return;
    busy = true; opener = link;
    if (live) live.textContent = 'Otwieranie formularza…';
    try {
      const response = await fetch(link.href, {headers: {Accept: 'text/html'}});
      if (!response.ok || response.redirected) throw Error('Sprawdź połączenie i zalogowanie.');
      const doc = new DOMParser().parseFromString(await response.text(), 'text/html');
      const form = doc.querySelector('.equipment-edit-form');
      if (!form) throw Error('Nie udało się otworzyć formularza.');
      content.replaceChildren(form);
      dialog.showModal();
      form.querySelector('input:not([type=hidden]),select,textarea')?.focus();
      if (live) live.textContent = '';
    } catch (error) { if (live) live.textContent = error.message; }
    finally { busy = false; }
  });
  dialog?.addEventListener('cancel', event => { if (busy) event.preventDefault(); });
  dialog?.addEventListener('close', () => opener?.focus({preventScroll: true}));
  document.addEventListener('submit', async event => {
    const form = event.target;
    if (!form.matches('.equipment-form')) return;
    event.preventDefault();
    if (busy || !form.reportValidity()) return;
    busy = true;
    const error = form.querySelector('.equipment-error');
    const body = new URLSearchParams(new FormData(form));
    const controls = [...form.querySelectorAll('input,select,textarea,button')].map(node => ({node, disabled: node.disabled}));
    controls.forEach(c => c.node.disabled = true);
    form.setAttribute('aria-busy', 'true');
    error.textContent = 'Zapisywanie…';
    try {
      const response = await fetch(form.action, {method: 'POST', body, headers: {Accept: 'application/json'}});
      let result;
      try { result = await response.json(); }
      catch { throw Error('Sprawdź połączenie i sesję. Pola zachowano. Możesz ponowić ten sam zapis.'); }
      if (!response.ok || response.redirected || result.saved !== true)
        throw Error(result.message || 'Nie udało się zapisać. Sprawdź połączenie i sesję.');
      const creating = body.has('name') && body.has('kind') && !body.get('id');
      if (creating) location.assign(url('/equipment/' + result.id));
      else if (!dialog) location.assign(url('/equipment'));
      else location.reload();
    } catch (failure) { error.textContent = failure.message || 'Brak połączenia. Pola zachowano.'; }
    finally {
      busy = false;
      controls.forEach(c => c.node.disabled = c.disabled);
      form.removeAttribute('aria-busy');
    }
  });
})();
