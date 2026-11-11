(() => {
  'use strict';
  const main = document.querySelector('main');
  if (main && !main.id) main.id = 'main-content';
  // Calendar already exposes its own skip target.
  const skip = document.querySelector('.app-header .skip-link');
  if (main && skip) skip.href = '#' + main.id;
  const openTarget = hash => {
    const target = document.getElementById(hash.replace(/^#/, ''));
    if (!target) return;
    for (let node = target; node; node = node.parentElement) {
      if (node.tagName === 'DETAILS') node.open = true;
    }
    requestAnimationFrame(() => target.scrollIntoView({block:'start'}));
  };
  if (location.hash) openTarget(location.hash);
  addEventListener('hashchange', () => openTarget(location.hash));
  // Repeated shortcuts must work even when the hash has not changed.
  document.addEventListener('click', event => {
    const link = event.target.closest('a[href^="#"]');
    if (link && link.hash.length > 1) openTarget(link.hash);
  });
  document.querySelectorAll('details').forEach(detail => {
    if (detail.querySelector('[role=alert]:not(:empty)')) detail.open = true;
  });
  // Hidden optional sections must expose invalid inputs before browser focus.
  document.addEventListener('invalid', event => {
    for (let node=event.target.parentElement; node; node=node.parentElement) {
      if (node.tagName === 'DETAILS') node.open = true;
    }
  }, true);
  const viewport = window.visualViewport;
  const adaptKeyboard = () => {
    const editing = document.activeElement?.matches('input,textarea,select');
    document.body.classList.toggle('keyboard-open', Boolean(editing && viewport && innerHeight - viewport.height > 120));
  };
  viewport?.addEventListener('resize', adaptKeyboard);
  document.addEventListener('focusin', adaptKeyboard);
  document.addEventListener('focusout', () => requestAnimationFrame(adaptKeyboard));
})();
