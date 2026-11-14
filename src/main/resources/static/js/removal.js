/* Explicit reversible corrections; no write until confirmation, no customer data in storage. */
(() => {
 'use strict';
 let pending=null,busy=false,opener=null;
 const key='nclean.removal.undo',dialog=document.createElement('dialog');
 dialog.id='removal-confirm';dialog.className='removal-dialog';dialog.setAttribute('aria-labelledby','removal-title');dialog.setAttribute('aria-describedby','removal-description');
 const title=document.createElement('h2');title.id='removal-title';const text=document.createElement('p');text.id='removal-description';
 const error=document.createElement('p');error.className='removal-error';error.setAttribute('role','alert');
 const actions=document.createElement('div');actions.className='removal-actions';
 const cancel=document.createElement('button');cancel.type='button';cancel.className='button secondary';cancel.textContent='Zostaw';
 const accept=document.createElement('button');accept.type='button';accept.className='button removal-button';
 actions.append(cancel,accept);dialog.append(title,text,error,actions);document.body.append(dialog);
 const close=()=>{if(!busy)dialog.close();};cancel.onclick=close;dialog.addEventListener('cancel',e=>{if(busy)e.preventDefault();});
 dialog.addEventListener('close',()=>{pending=null;if(opener?.isConnected)opener.focus({preventScroll:true});});
 document.addEventListener('submit',event=>{
  const form=event.target;if(!form.matches('.removal-form'))return;
  event.preventDefault();if(busy||dialog.open)return;
  pending=form;opener=event.submitter||document.activeElement;
  const restore=form.elements.restore.value==='true',kind=form.elements.kind.value;
  title.textContent=restore?'Przywrócić wpis?':kind==='equipment'?'Archiwizować sprzęt?':'Usunąć wpis?';
  const label=form.dataset.removalLabel||'Wybrany wpis';
  text.textContent=restore?label+' wróci do bieżących danych.':label+' zniknie z bieżących widoków. Historia pozostanie zachowana; wpis można przywrócić.';
  delete dialog.dataset.completed;error.textContent='';accept.textContent=restore?'Przywróć':kind==='equipment'?'Archiwizuj':'Usuń';dialog.showModal();cancel.focus();
 },true);
 accept.addEventListener('click',async()=>{
  if(!pending||busy||dialog.dataset.completed)return;const form=pending,body=new URLSearchParams(new FormData(form));body.set('confirmed','true');busy=true;accept.disabled=true;cancel.disabled=true;dialog.setAttribute('aria-busy','true');error.textContent='';
  try{
   const response=await fetch('/manage/remove',{method:'POST',body,headers:{Accept:'application/json'}});
   let result;try{result=await response.json();}catch{throw Error('Sprawdź połączenie i zalogowanie. Niczego nie potwierdzono; możesz ponowić.');}
   if(!response.ok||response.redirected||!result.saved)throw Error(result.message||'Operacja nie powiodła się. Sprawdź aktualne dane.');
   try{if(result.removed)sessionStorage.setItem(key,JSON.stringify({kind:result.kind,id:result.id,version:result.version,at:Date.now()}));else sessionStorage.removeItem(key);}catch{}
   dialog.dataset.completed='true';document.dispatchEvent(new CustomEvent('nclean:removed',{detail:result}));
   const route=result.kind==='booking'?'/calendar?day='+encodeURIComponent(result.day):['category','service','chemical'].includes(result.kind)?'/settings/services':location.pathname+location.search;
   location.assign(route);
  }catch(e){error.textContent=e.message;}finally{busy=false;accept.disabled=false;cancel.disabled=false;dialog.removeAttribute('aria-busy');}
 });
 try{
  const saved=JSON.parse(sessionStorage.getItem(key));
  if(saved&&Date.now()-saved.at<300000&&['booking','category','service','chemical','expense','fuel','equipment'].includes(saved.kind)&&Number.isSafeInteger(saved.id)&&Number.isSafeInteger(saved.version)){
   const csrf=document.querySelector('input[name=_csrf]')?.value;if(!csrf)return;
   const notice=document.createElement('div');notice.className='removal-notice';notice.setAttribute('role','status');const copy=document.createElement('span');copy.textContent='Usunięto z bieżących widoków.';
   const form=document.createElement('form');form.className='removal-form';form.method='post';form.action='/manage/remove';form.dataset.removalLabel='Ostatnio usunięty wpis';
   for(const [name,value]of Object.entries({kind:saved.kind,id:saved.id,version:saved.version,restore:'true',_csrf:csrf})){const i=document.createElement('input');i.type='hidden';i.name=name;i.value=String(value);form.append(i);}
   const undo=document.createElement('button');undo.type='submit';undo.className='button secondary';undo.textContent='Cofnij usunięcie';form.append(undo);notice.append(copy,form);document.querySelector('main')?.prepend(notice);
  }
 }catch{}
})();
