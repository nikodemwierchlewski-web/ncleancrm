/* One context-path contract for native navigation and the session-aware HTTP client. */
(() => {
  'use strict';
  const base = (document.querySelector('meta[name="crm-base"]')?.content || '/').replace(/\/$/, '');
  function url(value) {
    if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')) return value;
    if (!base || value === base || value.startsWith(base + '/') || value.startsWith(base + '?')) return value;
    return base + value;
  }
  function path(value = location.pathname) {
    const pathname = new URL(value, location.origin === 'null' ? 'https://preview.invalid' : location.origin).pathname;
    return base && (pathname === base || pathname.startsWith(base + '/')) ? pathname.slice(base.length) || '/' : pathname;
  }
  function formTarget(form) {
    const origin=location.origin==='null'?'https://preview.invalid':location.origin;
    const current=/^https?:/.test(location.href||'')?location.href:origin+(location.pathname.startsWith('/')?location.pathname:'/');
    return {url:new URL(url(form.getAttribute('action')||current),current).href,
      method:(form.getAttribute('method')||'GET').toUpperCase(),id:form.getAttribute('id')||''};
  }
  window.Nclean = Object.freeze({base, url, path, formTarget});
})();
