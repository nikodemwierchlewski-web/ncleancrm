/* Cosmetic feedback only. Business forms and saved data have their own handlers. */
(() => {
  'use strict';
  document.documentElement.dataset.uiTheme = 'pixel-tycoon';
  const selector = '#calendar-status,.cost-live,.note-live,.save-success';
  const last = new WeakMap();
  function markSaved() {
    for (const node of document.querySelectorAll(selector)) {
      const text = node.textContent.trim();
      if (last.get(node) === text) continue;
      last.set(node, text);
      if (!/^Zapisano(?:\b|[.!])/i.test(text)) continue;
      node.classList.remove('tycoon-saved');
      requestAnimationFrame(() => node.classList.add('tycoon-saved'));
    }
  }
  new MutationObserver(markSaved).observe(document.body, {
    childList: true, subtree: true, characterData: true
  });
  markSaved();
})();
