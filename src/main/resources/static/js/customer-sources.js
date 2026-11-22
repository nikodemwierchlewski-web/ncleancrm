(() => {
  'use strict';
  const section = document.querySelector('#customer-sources');
  if (!section) return;
  const picker = section.querySelector('.source-picker');
  const charts = [...section.querySelectorAll('[data-source-series]')];
  const update = () => {
    const key = picker.querySelector('input:checked')?.value;
    if (!charts.some(chart => chart.dataset.sourceSeries === key)) return;
    charts.forEach(chart => { chart.hidden = chart.dataset.sourceSeries !== key; });
  };
  picker.addEventListener('change', update);
  update();
  picker.hidden = false;
})();
