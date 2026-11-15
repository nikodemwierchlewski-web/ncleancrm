(() => {
  let notice;
  function connection(){if(!navigator.onLine){if(!notice){notice=document.createElement('p');notice.className='offline-notice';notice.setAttribute('role','status');notice.textContent='Brak połączenia. Pozostań na stronie — wpisane pola są zachowane w tej karcie. Ponów zapis po odzyskaniu połączenia.';document.body.prepend(notice);}}else{notice?.remove();notice=null;}}
  addEventListener('online',connection);addEventListener('offline',connection);connection();
  // Form transport, session recovery and dirty-state handling belong to session.js.
  let deferred;addEventListener('beforeinstallprompt',event=>{event.preventDefault();deferred=event;const button=document.querySelector('#install-app');if(button)button.hidden=false;});document.querySelector('#install-app')?.addEventListener('click',async()=>{await deferred?.prompt();deferred=null;document.querySelector('#install-app').hidden=true;});
  if('serviceWorker' in navigator&&window.isSecureContext)navigator.serviceWorker.register(Nclean.url('/sw.js'),{scope:Nclean.url('/')}).catch(()=>{});
})();
