// Cache only the public, self-contained offline notice; never private pages or files.
const cacheName='nclean-public-offline-v2';
self.addEventListener('install',event=>event.waitUntil(caches.open(cacheName).then(c=>c.add('/offline.html')).then(()=>self.skipWaiting())));
self.addEventListener('activate',event=>event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==cacheName).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',event=>{
 if(event.request.mode!=='navigate')return;
 event.respondWith(fetch(event.request).catch(()=>caches.match('/offline.html').then(page=>page||Response.error())));
});
