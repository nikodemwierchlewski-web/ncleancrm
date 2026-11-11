(() => {
  let notice;
  function connection(){if(!navigator.onLine){if(!notice){notice=document.createElement('p');notice.className='offline-notice';notice.setAttribute('role','status');notice.textContent='Brak połączenia. Pozostań na stronie — wpisane pola są zachowane w tej karcie. Ponów zapis po odzyskaniu połączenia.';document.body.prepend(notice);}}else{notice?.remove();notice=null;}}
  addEventListener('online',connection);addEventListener('offline',connection);connection();
  // Keep private forms in memory on failed network requests; no localStorage or offline queue.
  document.querySelectorAll('.work-panel form, .records form[method=post]:not(#booking-form):not(.actual-usage-form):not(#service-photo-form)').forEach(form=>form.addEventListener('submit',async event=>{
    if(form.matches('.day-review-form')||event.defaultPrevented||(/\/photos\/.*\/delete$/.test(form.getAttribute('action')||''))||form.getAttribute('id')==='service-photo-form'||form.getAttribute('id')==='photo-time-form')return;event.preventDefault();const button=event.submitter;if(button)button.disabled=true;
    try{const response=await fetch(form.getAttribute('action'),{method:'POST',body:new URLSearchParams(new FormData(form))});
      if(response.redirected){if(new URL(response.url).pathname==='/login')throw new Error('Sesja wygasła. Otwórz logowanie w nowej karcie, potem ponów zapis tutaj.');const target=new URL(response.url);if(form.getAttribute('id')==='payment-form'||form.getAttribute('action').includes('/payments/'))target.hash='payments';if(form.getAttribute('id')==='fuel-purchase-form')target.hash='purchases';if(target.href===location.href)location.reload();else location.assign(target.href);return;}
      const text=await response.text();if(response.status===400){const parsed=new DOMParser().parseFromString(text,'text/html');const message=parsed.querySelector('[role=alert]')?.textContent||'Sprawdź formularz.';let error=form.querySelector('.form-error');if(!error){error=document.createElement('p');error.className='form-error cost-error';error.setAttribute('role','alert');form.prepend(error);}error.textContent=message;error.scrollIntoView({block:'nearest'});return;}
      if(!response.ok)throw new Error('Zapis nie powiódł się. Sprawdź połączenie i ponów; jeśli sesja wygasła, zaloguj się ponownie.');location.reload();
    }catch(error){let message=form.querySelector('.form-error');if(!message){message=document.createElement('p');message.className='form-error cost-error';message.setAttribute('role','alert');form.prepend(message);}message.textContent=error.message;}
    finally{if(button)button.disabled=false;}
  }));
  let deferred;addEventListener('beforeinstallprompt',event=>{event.preventDefault();deferred=event;const button=document.querySelector('#install-app');if(button)button.hidden=false;});document.querySelector('#install-app')?.addEventListener('click',async()=>{await deferred?.prompt();deferred=null;document.querySelector('#install-app').hidden=true;});
  if('serviceWorker' in navigator&&window.isSecureContext)navigator.serviceWorker.register('/sw.js').catch(()=>{});
})();
