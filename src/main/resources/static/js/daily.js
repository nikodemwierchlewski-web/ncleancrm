(() => {
 'use strict';
 const $=s=>document.querySelector(s),main=$('.daily');if(!main)return;
 const cells=[...document.querySelectorAll('[data-calendar-day]')],mobile=()=>matchMedia('(max-width:900px)').matches;
 let selected=main.dataset.selected,opener=null;
 const dayCell=()=>cells.find(c=>c.dataset.calendarDay===selected);
 const iso=d=>`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
 const parsed=s=>new Date(s+'T12:00:00');
 const open=dialog=>{opener=document.activeElement;if(!dialog.open)dialog.showModal();};
 function cards(target){target.replaceChildren();const list=dayCell()?.querySelectorAll('.booking-chip')||[];for(const item of list){const copy=item.cloneNode(true);copy.classList.remove('extra-booking');target.append(copy);}if(!list.length){const p=document.createElement('p');p.textContent='Brak zleceń. Dodaj pierwsze przyciskiem + Zlecenie.';target.append(p);}}
 function select(day,changeUrl=true){selected=day;for(const c of cells)c.classList.toggle('is-selected',c.dataset.calendarDay===day);$('#agenda-title').textContent=dayCell()?.dataset.dateLabel||day;$('#agenda-areas').replaceChildren(...[...(dayCell()?.querySelector('.day-areas').children||[])].map(n=>n.cloneNode(true)));cards($('#agenda-orders'));$('#area-date').value=day;$('#area-editor-date').textContent=dayCell()?.dataset.dateLabel||day;$('#area-editor-date').dateTime=day;renderWeek();if(changeUrl){const u=new URL(location.href);u.searchParams.set('day',day);u.searchParams.set('saved','1');history.replaceState(null,'',u);}}
 function renderWeek(){const start=parsed(selected);start.setDate(start.getDate()-(start.getDay()+6)%7);$('#week-strip').replaceChildren();for(let i=0;i<7;i++){const d=new Date(start);d.setDate(d.getDate()+i);const date=iso(d),b=document.createElement('button');b.type='button';b.dataset.weekDay=date;b.setAttribute('aria-label',d.toLocaleDateString('pl-PL',{weekday:'long',day:'numeric',month:'long'}));b.setAttribute('aria-pressed',String(date===selected));b.classList.toggle('selected',date===selected);const label=document.createElement('small');label.textContent=d.toLocaleDateString('pl-PL',{weekday:'short'});b.append(label,document.createTextNode(String(d.getDate())));const cell=cells.find(c=>c.dataset.calendarDay===date);if(cell){const bands=document.createElement('span');bands.className='week-bands';for(const band of cell.querySelectorAll('.area-bands i'))bands.append(band.cloneNode(true));b.append(bands);const areas=cell.querySelector('.day-areas').textContent.trim();b.setAttribute('aria-label',b.getAttribute('aria-label')+'; '+(areas||'bez obszaru')+'; '+cell.querySelectorAll('.booking-chip').length+' zleceń');}$('#week-strip').append(b);}}
 function choose(day,panel=true){if(!cells.some(c=>c.dataset.calendarDay===day)){location.href='/calendar?day='+encodeURIComponent(day)+'&saved=1';return;}select(day);if(panel&&mobile()){main.classList.remove('month-open');$('#toggle-month').setAttribute('aria-expanded','false');}if(panel&&!mobile())showDay();}
 function showDay(areas=false){const ids=new Set([...dayCell().querySelectorAll('[data-assigned-area-id]')].map(n=>n.dataset.assignedAreaId));for(const option of document.querySelectorAll('[data-area-option]')){const checked=ids.has(option.dataset.areaOption);option.querySelector('input').checked=checked;option.hidden=option.dataset.active!=='true'&&!checked;}cards($('#panel-orders'));$('#area-details').open=areas;open($('#area-editor'));}
 for(const c of cells){const color=c.querySelector('.area-bands i');if(color)c.style.setProperty('--area-color',color.style.background);const count=c.querySelectorAll('.booking-chip').length;c.dataset.count=count+' zlec.';c.classList.toggle('has-orders',count>0);}
 document.addEventListener('click',e=>{
  const day=e.target.closest('[data-select-day],[data-week-day]');if(day){choose(day.dataset.selectDay||day.dataset.weekDay);return;}
  const order=e.target.closest('[data-order]');if(order){$('#area-editor').close();$('#order-content').replaceChildren($('#order-'+order.dataset.order).content.cloneNode(true));open($('#order-editor'));return;}
  const cell=e.target.closest('[data-calendar-day]');if(cell){choose(cell.dataset.calendarDay);return;}
  if(e.target.closest('.add-booking')){$('#area-editor').close();$('#booking-form [name=day]').value=selected;open($('#booking-editor'));return;}
  if(e.target.closest('[data-close]'))e.target.closest('dialog').close();
 });
 for(const dialog of document.querySelectorAll('dialog')){dialog.addEventListener('close',()=>opener?.focus({preventScroll:true}));dialog.addEventListener('click',e=>{if(e.target!==dialog)return;const b=dialog.getBoundingClientRect();if(e.clientX<b.left||e.clientX>b.right||e.clientY<b.top||e.clientY>b.bottom)dialog.close();});}
 $('#mobile-areas').addEventListener('click',()=>showDay(true));
 $('#toggle-month').addEventListener('click',()=>{const on=main.classList.toggle('month-open');$('#toggle-month').setAttribute('aria-expanded',String(on));});
 $('#jump-month').addEventListener('change',e=>{if(e.target.validity.valid&&e.target.value)location.href='/calendar?month='+e.target.value;});
 for(const [id,delta] of [['previous-week',-7],['next-week',7]])$('#'+id).addEventListener('click',()=>{const d=parsed(selected);d.setDate(d.getDate()+delta);const day=iso(d);if(day<'2000-01-01'||day>'2100-12-31')return;choose(day,false);});
 $('#clear-areas').addEventListener('click',()=>document.querySelectorAll('[data-area-option] input').forEach(i=>i.checked=false));
 document.addEventListener('submit',async e=>{
  const f=e.target;if(!f.matches('#booking-form,#day-area-form,.order-update')||e.defaultPrevented)return;e.preventDefault();if(f.dataset.saving)return;
  const error=f.querySelector('.save-error,#customer-search-status')||$('#calendar-status');error.textContent='';const body=new URLSearchParams(new FormData(f));body.set('calendar','1');
  f.dataset.saving='1';const button=f.querySelector('button[type=submit],button:not([type]),#save-areas');if(button)button.disabled=true;
  try{const response=await fetch(f.action,{method:'POST',body,headers:{Accept:'application/json'}});if(!response.ok||!response.headers.get('content-type')?.includes('application/json')){let message='Nie udało się zapisać. Sprawdź połączenie i zalogowanie, a potem spróbuj ponownie.';try{message=(await response.json()).message||message;}catch(_){}throw Error(message);}const day=f.querySelector('[name=day]')?.value||selected;location.href='/calendar?day='+encodeURIComponent(day)+'&saved=1';}
  catch(ex){error.textContent=ex.message;delete f.dataset.saving;if(button)button.disabled=false;}
 });
 select(selected,false);if(main.dataset.open==='true'&&!mobile())showDay();
})();
