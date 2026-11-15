/* Refresh CSRF before a deliberate write; never replay a mutation automatically. */
(() => {
 const original=window.fetch.bind(window);
 addEventListener('beforeunload',e=>{if(window.NcleanDrafts?.hasDirty()){e.preventDefault();e.returnValue='';}});
 // One native-form transport; feature-owned async forms preventDefault before this handler.
 addEventListener('submit',async event=>{
  const form=event.target;if(event.defaultPrevented)return;const target=Nclean.formTarget(form);
  if(target.method!=='POST'||['/login','/logout'].includes(Nclean.path(target.url)))return;
  event.preventDefault();if(form.dataset.saving)return;
  const body=new FormData(form);if(event.submitter?.name)body.append(event.submitter.name,event.submitter.value);
  let notice=form.querySelector('.session-form-error,.form-error');if(!notice){notice=document.createElement('p');notice.className='session-form-error form-error';notice.setAttribute('role','alert');form.prepend(notice);}notice.classList.add('session-form-error');notice.setAttribute('role','alert');
  const button=event.submitter||form.querySelector('button[type=submit],button:not([type])');form.dataset.saving='true';form.setAttribute('aria-busy','true');if(button)button.disabled=true;
  try{
   const response=await fetch(target.url,{method:'POST',body});
   if(!response.ok){const json=response.headers.get('content-type')?.includes('application/json');let message;
    if(json){const result=await response.json();message=result?.message||result?.error;}else{const parsed=new DOMParser().parseFromString(await response.text(),'text/html');message=parsed.querySelector('[role=alert]')?.textContent;}
    throw Error(message||'Zapis nie powiódł się. Sprawdź dane lub sesję. Wpisane pola pozostają w tej karcie.');}
   window.NcleanDrafts?.saved(form);
   const destination=new URL(response.redirected?response.url:location.href);const action=Nclean.path(target.url);
   if(target.id==='payment-form'||action.includes('/payments/'))destination.hash='payments';if(target.id==='fuel-purchase-form')destination.hash='purchases';
   if(destination.href===location.href)location.reload();else location.assign(destination.href);
  }catch(e){notice.textContent=e.message;for(let p=notice.parentElement;p&&p!==form;p=p.parentElement)if(p.tagName==='DETAILS')p.open=true;notice.scrollIntoView({block:'nearest'});}finally{delete form.dataset.saving;delete form.dataset.motionNativePending;form.removeAttribute('aria-busy');if(button)button.disabled=false;}
 });
 window.fetch=async(input,options={})=>{
  input=Nclean.url(input);
  const method=(options.method||'GET').toUpperCase(),url=new URL(typeof input==='string'?input:input.url,location.href),body=options.body;
  if(url.origin!==location.origin||['GET','HEAD','OPTIONS'].includes(method)||!(body instanceof FormData||body instanceof URLSearchParams))return original(input,options);
  try{
   const response=await original(Nclean.url('/session'),{cache:'no-store',headers:{Accept:'application/json'}});
   if(!response.ok||response.redirected||!response.headers.get('content-type')?.includes('application/json')){
    let banner=document.querySelector('#session-recovery');if(!banner){banner=document.createElement('aside');banner.id='session-recovery';banner.setAttribute('role','alert');banner.className='notice';const text=document.createElement('p');text.textContent='Sesja wygasła. Szkic pozostaje w tej karcie. Po zalogowaniu ponów zapis.';const link=document.createElement('a');link.href=Nclean.url('/login');link.target='_blank';link.rel='noopener';link.textContent='Zaloguj się w nowej karcie';banner.append(text,link);(document.querySelector('dialog[open]')||document.body).prepend(banner);}
    return new Response(JSON.stringify({message:'Zaloguj się w nowej karcie, a następnie ponów zapis. Szkic zachowano.',error:'Sesja wygasła.'}),{status:401,headers:{'content-type':'application/json'}});
   }
   const csrf=await response.json();if(typeof csrf.token!=='string'||csrf.parameter!=='_csrf')throw Error('Sesja wymaga odnowienia.');document.querySelectorAll('input[name="_csrf"]').forEach(n=>n.value=csrf.token);body.set(csrf.parameter,csrf.token);document.querySelector('#session-recovery')?.remove();
  }catch{return new Response(JSON.stringify({message:'Brak połączenia. Zachowano pola — ponów zapis.',error:'Brak połączenia.'}),{status:503,headers:{'content-type':'application/json'}});}
  const response=await original(input,options);
  if(response.status===403||response.redirected&&Nclean.path(response.url)==='/login'){
   // A logout racing the preflight still requires a deliberate retry.
   return new Response(JSON.stringify({message:'Sesja lub CSRF zmieniły się. Zaloguj się w nowej karcie i ponów zapis. Pola zachowano.',error:'Sprawdź sesję i ponów zapis.'}),{status:403,headers:{'content-type':'application/json'}});
  }
  return response;
 };
})();
