(() => {
  'use strict';
  const $ = selector => document.querySelector(selector);
  $('#go-back')?.addEventListener('click', () => history.back());

  // The same guarded submission is used by calendar panels and the booking form.
  window.NcleanForms = {
    async submit(form, error, onSaved) {
      if (form.dataset.saving) return;
      const body = new URLSearchParams(new FormData(form));
      body.set('calendar', '1');
      form.dataset.saving = '1';
      const button = form.querySelector('button[type="submit"], #save-areas, button:not([type])');
      if (button) button.disabled = true;
      form.setAttribute('aria-busy', 'true');
      error.textContent = 'Zapisywanie…';
      try {
        const response = await fetch(form.action, {method: 'POST', body, headers: {Accept: 'application/json'}});
        const json = response.headers.get('content-type')?.includes('application/json');
        const result = json ? await response.json() : {};
        if (!response.ok || !json) throw Error(result.message || 'Nie udało się zapisać. Sprawdź połączenie i zalogowanie, a potem spróbuj ponownie.');
        onSaved();
      } catch (exception) {
        error.textContent = exception.message;
        delete form.dataset.saving;
        form.removeAttribute('aria-busy');
        if (button) button.disabled = false;
      }
    }
  };

  const form = $('#booking-form');
  if (!form) return;
  const lines = [...form.querySelectorAll('.service-line')];
  const categories = [...form.querySelectorAll('[data-category-choice]')];
  const summary = $('#booking-summary');
  const status = $('#customer-search-status');
  const money = value => new Intl.NumberFormat('pl-PL', {style: 'currency', currency: 'PLN'}).format(value);

  function total() {
    let cents = 0;
    summary.replaceChildren();
    for (const line of lines) {
      const input = line.querySelector('input');
      const quantity = Number(input.value || 0);
      if (!(quantity > 0)) continue;
      const amount = Math.round(Number(line.dataset.price) * quantity * 100);
      cents += amount;
      const item = document.createElement('li');
      const label = document.createElement('span');
      const name = line.querySelector('strong').textContent;
      label.textContent = `${name} · ${quantity} ${line.dataset.unit} · ${money(amount / 100)}`;
      const remove = document.createElement('button');
      remove.type = 'button';
      remove.textContent = '×';
      remove.setAttribute('aria-label', `Usuń z wyceny: ${name}`);
      remove.addEventListener('click', () => { input.value = '0'; total(); });
      item.append(label, remove);
      summary.append(item);
    }
    $('#booking-empty').hidden = summary.children.length > 0;
    $('#booking-total').textContent = money(cents / 100);
  }

  function filter(category) {
    for (const line of lines) {
      line.hidden = line.dataset.category !== category;
      // Hidden categories keep enabled inputs: every selected quantity is submitted.
    }
    for (const button of categories) button.setAttribute('aria-pressed', String(button.dataset.categoryChoice === category));
  }
  for (const button of categories) button.addEventListener('click', () => filter(button.dataset.categoryChoice));
  filter(categories[0]?.dataset.categoryChoice);

  for (const line of lines) {
    const input = line.querySelector('input');
    const wrap = document.createElement('span');
    wrap.className = 'stepper';
    input.before(wrap);
    for (const [label, delta] of [['−', -1], ['+', 1]]) {
      const button = document.createElement('button');
      button.type = 'button';
      button.textContent = label;
      button.setAttribute('aria-label', `${delta > 0 ? 'Zwiększ' : 'Zmniejsz'} ilość: ${line.querySelector('strong').textContent}`);
      button.addEventListener('click', () => {
        input.value = String(Math.max(0, Math.min(10000, Number(input.value || 0) + delta)));
        total();
      });
      wrap.append(button);
      if (delta < 0) wrap.append(input);
    }
  }
  form.addEventListener('input', total);
  form.addEventListener('invalid', event => {
    const line = event.target.closest('.service-line');
    if (line) { filter(line.dataset.category); line.scrollIntoView({block: 'nearest'}); }
  }, true);
  total();

  const search = $('#customer-search');
  const results = $('#customer-results');
  const lookupStatus = $('#customer-lookup-status');
  const customer = $('#booking-customer');
  const fields = $('#new-customer');
  const contactName = form.elements.namedItem('name');
  const phone = form.elements.namedItem('phone');
  let timer, sequence = 0, suggestedPhone = '', suggestedName = '';

  function suggestContact() {
    const value = search.value.trim();
    const number = value.replace(/[\s()-]/g, '');
    if (/^\+?\d{9,15}$/.test(number)) {
      if (!phone.value || phone.value === suggestedPhone) { phone.value = number; suggestedPhone = number; }
    } else if (value && !/[\d+]/.test(value)) {
      if (!contactName.value || contactName.value === suggestedName) { contactName.value = value; suggestedName = value; }
    }
  }

  function pick(person) {
    clearTimeout(timer);
    sequence++;
    customer.value = person.id;
    contactName.value = person.name;
    phone.value = person.phone;
    $('#booking-address').value = person.address;
    $('#selected-customer-label').textContent = `${person.name} · ${person.phone}`;
    $('#selected-customer').hidden = false;
    $('#customer-picker').hidden = true;
    fields.hidden = true;
    results.replaceChildren();
    results.hidden = true;
    lookupStatus.textContent = '';
  }

  search.addEventListener('input', () => {
    suggestContact();
    clearTimeout(timer);
    const current = ++sequence;
    const query = search.value.trim();
    results.replaceChildren();
    results.hidden = true;
    if (!query) { lookupStatus.textContent = ''; return; }
    timer = setTimeout(async () => {
      try {
        const body = new URLSearchParams({query, _csrf: form.querySelector('[name="_csrf"]').value});
        const response = await fetch('/customers/lookup', {method: 'POST', body, headers: {Accept: 'application/json'}});
        if (!response.ok || !response.headers.get('content-type')?.includes('application/json')) throw Error();
        const people = await response.json();
        if (current !== sequence) return;
        for (const person of people) {
          const item = document.createElement('li');
          const button = document.createElement('button');
          button.type = 'button';
          const name = document.createElement('strong');
          name.textContent = person.name;
          const number = document.createElement('small');
          number.textContent = person.phone;
          button.append(name, number);
          button.addEventListener('click', () => pick(person));
          item.append(button);
          results.append(item);
        }
        results.hidden = people.length === 0;
        lookupStatus.textContent = people.length ? 'Wybierz kontakt albo uzupełnij dane nowego klienta.' : 'Nowy klient — uzupełnij dane poniżej.';
      } catch (_) {
        if (current === sequence) lookupStatus.textContent = 'Wyszukiwanie jest niedostępne. Możesz wpisać dane klienta poniżej.';
      }
    }, 250);
  });
  search.addEventListener('keydown', event => {
    if (event.key === 'Enter' || event.key === 'ArrowDown') {
      event.preventDefault();
      results.querySelector('button')?.focus();
    }
    if (event.key === 'Escape') results.hidden = true;
  });
  $('#change-customer').addEventListener('click', () => {
    sequence++;
    customer.value = '';
    suggestedName = contactName.value;
    suggestedPhone = phone.value;
    $('#selected-customer').hidden = true;
    $('#customer-picker').hidden = false;
    fields.hidden = false;
    lookupStatus.textContent = 'Wybierz inny kontakt lub popraw dane nowego klienta.';
    search.focus();
    search.select();
  });
  for (const input of [contactName, phone]) input.addEventListener('input', () => {
    if (customer.value) $('#selected-customer-label').textContent = `${contactName.value} · ${phone.value}`;
  });
  $('#edit-contact').addEventListener('click', () => { fields.hidden = !fields.hidden; if (!fields.hidden) contactName.focus(); });

  form.addEventListener('submit', event => {
    event.preventDefault();
    if (!lines.some(line => Number(line.querySelector('input').value) > 0)) {
      status.textContent = 'Dodaj przynajmniej jedną usługę.';
      return;
    }
    window.NcleanForms.submit(form, status, () => {
      const day = encodeURIComponent(form.elements.namedItem('day').value);
      location.href = `${$('.daily') ? '/calendar' : '/orders'}?day=${day}&saved=1`;
    });
  });
})();
