/* Refresh CSRF before a deliberate write; never replay a mutation automatically. */
(() => {
 const original=window.fetch.bind(window);
 const dirty=new WeakSet();let submitting=false;
 document.addEventListener('input',e=>{if(e.target.form&&!['/login','/logout'].includes(e.target.form.getAttribute('action')))dirty.add(e.target.form);});
 document.addEventListener('change',e=>{if(e.target.form&&!['/login','/logout'].includes(e.target.form.getAttribute('action')))dirty.add(e.target.form);});
 addEventListener('beforeunload',e=>{if(!submitting&&[...document.forms].some(f=>dirty.has(f))){e.preventDefault();e.returnValue='';}});
 window.NcleanDrafts={saved:form=>dirty.delete(form)};
 // Native forms share the same recovery as the existing async forms. Failed requests keep the DOM and files.
 addEventListener('submit',async event=>{
  const form=event.target;if(event.defaultPrevented||form.method.toUpperCase()!=='POST'||['/login','/logout'].includes(form.getAttribute('action')))return;
  event.preventDefault();if(form.dataset.saving)return;
  const body=new FormData(form);if(event.submitter?.name)body.append(event.submitter.name,event.submitter.value);
  let notice=form.querySelector('.session-form-error');if(!notice){notice=document.createElement('p');notice.className='session-form-error';notice.setAttribute('role','alert');form.append(notice);}
  const button=event.submitter;form.dataset.saving='true';if(button)button.disabled=true;
  try{
   const response=await fetch(form.action,{method:'POST',body});
   if(!response.ok){const json=response.headers.get('content-type')?.includes('application/json');const result=json?await response.json():null;throw Error(result?.message||result?.error||'Sprawdź dane lub sesję. Wpisane pola pozostają w tej karcie.');}
   dirty.delete(form);submitting=true;location.assign(response.url||location.href);
  }catch(e){notice.textContent=e.message;}finally{delete form.dataset.saving;if(button)button.disabled=false;}
 });
 window.fetch=async(input,options={})=>{
  const method=(options.method||'GET').toUpperCase(),url=new URL(typeof input==='string'?input:input.url,location.href),body=options.body;
  if(url.origin!==location.origin||['GET','HEAD','OPTIONS'].includes(method)||!(body instanceof FormData||body instanceof URLSearchParams))return original(input,options);
  try{
   const response=await original('/session',{cache:'no-store',headers:{Accept:'application/json'}});
   if(!response.ok||response.redirected||!response.headers.get('content-type')?.includes('application/json')){
    let banner=document.querySelector('#session-recovery');if(!banner){banner=document.createElement('aside');banner.id='session-recovery';banner.setAttribute('role','alert');banner.className='notice';const text=document.createElement('p');text.textContent='Sesja wygasła. Szkic pozostaje w tej karcie. Po zalogowaniu ponów zapis.';const link=document.createElement('a');link.href='/login';link.target='_blank';link.rel='noopener';link.textContent='Zaloguj się w nowej karcie';banner.append(text,link);(document.querySelector('dialog[open]')||document.body).prepend(banner);}
    return new Response(JSON.stringify({message:'Zaloguj się w nowej karcie, a następnie ponów zapis. Szkic zachowano.',error:'Sesja wygasła.'}),{status:401,headers:{'content-type':'application/json'}});
   }
   const csrf=await response.json();if(typeof csrf.token!=='string'||csrf.parameter!=='_csrf')throw Error('Sesja wymaga odnowienia.');document.querySelectorAll('input[name="_csrf"]').forEach(n=>n.value=csrf.token);body.set(csrf.parameter,csrf.token);document.querySelector('#session-recovery')?.remove();
  }catch{return new Response(JSON.stringify({message:'Brak połączenia. Zachowano pola — ponów zapis.',error:'Brak połączenia.'}),{status:503,headers:{'content-type':'application/json'}});}
  const response=await original(input,options);
  if(response.status===403||response.redirected&&new URL(response.url).pathname==='/login'){
   // A logout racing the preflight still requires a deliberate retry.
   return new Response(JSON.stringify({message:'Sesja lub CSRF zmieniły się. Zaloguj się w nowej karcie i ponów zapis. Pola zachowano.',error:'Sprawdź sesję i ponów zapis.'}),{status:403,headers:{'content-type':'application/json'}});
  }
  if(response.ok&&!response.redirected){
   const form=[...document.forms].find(f=>f.action===url.href);if(form)dirty.delete(form);
  }
  return response;
 };
})();
