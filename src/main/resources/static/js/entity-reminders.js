/* The same entity editor is used by visits, notes and goals. No native permission prompt. */
(() => {
 'use strict';
 const url=p=>window.Nclean?.url(p)||p;
 const csrf=()=>document.querySelector('input[name="_csrf"]')?.value||document.querySelector('meta[name="_csrf"]')?.content;
 const token=()=>crypto.randomUUID();
 const node=(tag,attrs={},text)=>{const e=document.createElement(tag);for(const[k,v]of Object.entries(attrs))e.setAttribute(k,v);if(text!==undefined)e.textContent=text;return e;};
 const input=(name,value,type='hidden')=>node('input',{name,value,type});
 const label=(text,child)=>{const e=node('label',{},text);e.append(child);return e;};
 const error=(section,text)=>{section.querySelector('[data-reminder-error]').textContent=text;if(text&&!text.startsWith('Zapisano'))section.querySelector('.reminder-disclosure').open=true;};
 async function json(path,options){const r=await fetch(url(path),{...options,headers:{Accept:'application/json',...options?.headers}});if(!r.ok||r.redirected){let message;try{message=(await r.json()).message;}catch{}throw Error(message||'Nie udało się pobrać przypomnień. Sprawdź sesję i połączenie.');}return r.json();}
 function reindex(section){[...section.querySelectorAll('.entity-reminder-row')].forEach((row,i)=>row.querySelectorAll('[data-field]').forEach(n=>n.name='entity_'+n.dataset.field+'_'+i));const count=section.querySelectorAll('.entity-reminder-row').length;section.querySelector('[data-reminder-add]').disabled=count>=10;section.querySelector('[data-reminder-count]').textContent=count===0?'Brak':count===1?'1 przypomnienie':count<5?count+' przypomnienia':count+' przypomnień';}
 function modeState(row){const offset=row.querySelector('[data-field="mode"]').value==='OFFSET';for(const key of ['amount','unit']){const field=row.querySelector(`[data-field="${key}"]`);field.disabled=!offset;field.closest('label').hidden=!offset;}const at=row.querySelector('[data-field="at"]');at.disabled=offset;at.required=!offset;at.closest('label').hidden=offset;}
 function field(tag,key,attrs={}){return node(tag,{'data-field':key,...attrs});}
 function row(section,spec={id:token(),offsetMinutes:30}){
  const e=node('div',{class:'entity-reminder-row'});e.append(field('input','id',{type:'hidden',value:spec.id}));
  const mode=field('select','mode');for(const[value,text]of [['OFFSET','Przed terminem'],['AT','Konkretna data']])mode.append(node('option',{value},text));mode.value=spec.absoluteAt?'AT':'OFFSET';
  const minutes=spec.offsetMinutes??30,unit=minutes&&minutes%1440===0?'days':minutes&&minutes%60===0?'hours':'minutes',factor=unit==='days'?1440:unit==='hours'?60:1;
  const amount=field('input','amount',{type:'number',min:0,max:5256000,step:1,value:minutes/factor,required:'required',inputmode:'numeric'}),units=field('select','unit');for(const[value,text]of [['minutes','minut'],['hours','godzin'],['days','dni']])units.append(node('option',{value},text));units.value=unit;
  const local=spec.absoluteAt?new Intl.DateTimeFormat('sv-SE',{timeZone:'Europe/Warsaw',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hourCycle:'h23'}).format(new Date(spec.absoluteAt)).replace(' ','T'):'';
  const at=field('input','at',{type:'datetime-local',min:'2000-01-01T00:00',max:'2100-12-31T23:59',value:local});
  const remove=node('button',{type:'button',class:'icon-button','aria-label':'Usuń przypomnienie'},'×');remove.addEventListener('click',()=>{e.remove();reindex(section);window.NcleanDrafts?.changed(section.closest('form'));});
  e.append(label('Kiedy',mode),label('Ile wcześniej',amount),label('Jednostka',units),label('Data i godzina',at),remove);mode.addEventListener('change',()=>modeState(e));modeState(e);section.querySelector('[data-entity-reminder-rows]').append(e);reindex(section);
 }
 async function mount(root=document){for(const section of root.querySelectorAll('[data-reminder-source]:not([data-reminders-mounted])')){
  section.dataset.remindersMounted='true';const add=section.querySelector('[data-reminder-add]'),save=section.querySelector('[data-reminder-save]'),form=section.closest('#note-form');
  let loading=true;const submit=form?.querySelector('[type="submit"]');if(submit)submit.disabled=true;
  try{const id=section.dataset.reminderId;const plan=id?await json('/api/v1/reminders?sourceType='+section.dataset.reminderSource+'&sourceId='+id):{version:0,reminders:[]};section.append(input('entity_reminders_present','true'),input('entity_version',plan.version));plan.reminders.forEach(s=>row(section,s));error(section,'');reindex(section);if(save)save.disabled=false;
   add.addEventListener('click',()=>{row(section);window.NcleanDrafts?.changed(section.closest('form'));section.querySelector('.entity-reminder-row:last-child select').focus();});
   if(save)save.addEventListener('click',async()=>{if(loading)return;const invalid=[...section.querySelectorAll('input,select')].find(i=>!i.disabled&&!i.checkValidity());if(invalid){invalid.reportValidity();return;}loading=true;const fields=[...section.querySelectorAll('input,select,button')].map(n=>({n,disabled:n.disabled}));const body=new URLSearchParams();for(const n of section.querySelectorAll('input,select'))if(n.name&&!n.disabled)body.set(n.name,n.value);body.set('sourceType',section.dataset.reminderSource);body.set('sourceId',id);body.set('_csrf',csrf()||'');fields.forEach(({n})=>n.disabled=true);
    try{const p=await json('/api/v1/reminders',{method:'POST',body});section.querySelector('[name="entity_version"]').value=p.version;error(section,'Zapisano przypomnienia.');}catch(e){error(section,e.message);}finally{fields.forEach(({n,disabled})=>n.disabled=disabled);loading=false;reindex(section);}
   });
  }catch(e){section.querySelector('[data-reminder-count]').textContent='Nie odczytano';error(section,e.message+' Zamknij i otwórz wpis ponownie.');/* Never submit an unloaded reminder set. */}
  finally{loading=false;if(submit)submit.disabled=false;}
 }}
 window.NcleanReminders=Object.freeze({mount});mount();
 // Compact, dismissible in-app notice; configuration always stays with the record.
 let checking=false;
 async function poll(){if(document.hidden||checking||document.querySelector('.notification-toast')||document.querySelector('dialog[open]'))return;checking=true;try{const data=await json('/api/v1/notifications');const n=data.notifications.find(n=>!n.displayed)||data.legacyNotifications?.filter(n=>!n.seen).map(n=>({...n,legacy:true,href:'/notes?entry='+n.id}))[0];if(!n)return;const endpoint=n.legacy?'/notes/reminders/'+n.id:'/api/v1/notifications/'+n.id;const params=action=>n.legacy?new URLSearchParams({revision:n.revision,occurrence:n.occurrence,action:action==='DISPLAYED'?'seen':'done',_csrf:csrf()||''}):new URLSearchParams({version:n.version,action,_csrf:csrf()||''});const claimed=await json(endpoint,{method:'POST',body:params('DISPLAYED')});if(!claimed.claimed)return;
  const toast=node('aside',{class:'notification-toast','aria-label':'Przypomnienie',role:'status'});toast.append(node('strong',{},'Przypomnienie'),node('a',{href:url(n.href)},n.title));const dismiss=node('button',{type:'button',class:'icon-button','aria-label':'Ukryj przypomnienie'},'×');dismiss.addEventListener('click',()=>toast.remove());const read=node('button',{type:'button',class:'text-button'},'Odczytane');read.addEventListener('click',async()=>{read.disabled=true;try{await json(endpoint,{method:'POST',body:params('READ')});toast.remove();}catch(e){read.disabled=false;toast.append(node('p',{},e.message));}});toast.append(read,dismiss);document.body.append(toast);
 }catch{/* Next visible poll retries, never fabricate a notification. */}finally{checking=false;}}
 if(document.querySelector('nav')&&!document.querySelector('.login-card')){setTimeout(poll,2000);setInterval(poll,60000);document.addEventListener('visibilitychange',()=>{if(!document.hidden)poll();});}
})();
