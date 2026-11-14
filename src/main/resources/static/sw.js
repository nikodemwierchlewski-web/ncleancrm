// Only an anonymous, self-contained offline notice. Never cache private responses.
const scope = new URL(self.registration.scope);
const namespace = 'nclean-crm-offline:' + scope.pathname + ':';
const cacheName = namespace + 'v3';
const offlineUrl = new URL('offline.html', scope).href;
self.addEventListener('install', event => event.waitUntil(
  caches.open(cacheName).then(cache => cache.add(offlineUrl)).then(() => self.skipWaiting())
));
self.addEventListener('activate', event => event.waitUntil(
  caches.keys().then(keys => Promise.all(keys.filter(key => key.startsWith(namespace) && key !== cacheName)
    .map(key => caches.delete(key)))).then(() => self.clients.claim())
));
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);
  if (event.request.mode !== 'navigate' || url.origin !== scope.origin || !url.pathname.startsWith(scope.pathname)) return;
  event.respondWith(fetch(event.request).catch(() => caches.match(offlineUrl).then(page => page || Response.error())));
});
