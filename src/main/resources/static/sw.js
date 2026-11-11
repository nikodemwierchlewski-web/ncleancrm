// Network only: no authenticated pages, APIs, images or receipts are cached.
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', event => event.waitUntil(self.clients.claim()));
self.addEventListener('fetch', event => {
  if (event.request.mode !== 'navigate') return;
  event.respondWith(fetch(event.request).catch(() => new Response(
    '<!doctype html><html lang="pl"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Brak połączenia</title><body><h1>Brak połączenia</h1><p>Wróć do aplikacji po odzyskaniu sieci. Dane prywatne nie są zapisywane offline.</p><button onclick="location.reload()">Ponów</button></body></html>',
    {status: 503, headers: {'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store'}}
  )));
});
