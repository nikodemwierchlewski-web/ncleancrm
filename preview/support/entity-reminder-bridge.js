/* Synthetic transport for the exported server views. Never connects to PostgreSQL. */
(() => {
 const previous=window.fetch,pending=new Map();let sequence=0;
 const ask=(path,method,fields)=>new Promise(resolve=>{const id=++sequence;pending.set(id,resolve);parent.postMessage({preview:true,kind:'entity-query',data:{id,path,method,fields}},'*');});
 addEventListener('message',e=>{if(e.source!==parent||!e.data?.entityReply)return;const resolve=pending.get(e.data.id);if(resolve){pending.delete(e.data.id);resolve(e.data);}});
 const reply=result=>new Response(JSON.stringify(result.data),{status:result.status||200,headers:{'content-type':'application/json'}});
 function parse(f){const reminders=[];for(let i=0;i<10;i++)if(f.has('entity_id_'+i)){const id=f.get('entity_id_'+i);if(f.get('entity_mode_'+i)==='AT')reminders.push({id,offsetMinutes:null,absoluteAt:window.previewWarsawInstant(f.get('entity_at_'+i))});else{const factor={minutes:1,hours:60,days:1440}[f.get('entity_unit_'+i)],n=Number(f.get('entity_amount_'+i));if(!Number.isSafeInteger(n)||n<0||!factor||n*factor>5256000)throw Error('Sprawdź wyprzedzenie.');reminders.push({id,offsetMinutes:n*factor,absoluteAt:null});}}return {version:Number(f.get('entity_version')||0)+1,reminders};}
 window.fetch=async(target,options={})=>{
  const u=new URL(String(target),'https://preview.invalid'),method=options.method||'GET';
  if(u.pathname.startsWith('/api/v1/'))return reply(await ask(u.pathname+u.search,method,options.body?[...options.body]:null));
  if(u.pathname==='/notes/editor'&&method==='GET'){
   const response=await previous(target,options);if(!response.ok)return response;const d=new DOMParser().parseFromString(await response.text(),'text/html'),id=Number(u.searchParams.get('id')),n=localNotes.find(n=>n.id===id),section=d.querySelector('[data-reminder-source]');if(section)section.dataset.reminderId=n?.id||'';if(n)d.querySelector('[name=due_time]').value=n.dueTime||'';for(const i of d.querySelectorAll('input:not([type=file])'))if(i.type!=='checkbox'&&i.type!=='radio')i.setAttribute('value',i.value);return new Response(d.documentElement.outerHTML,{headers:{'content-type':'text/html'}});
  }
  if(u.pathname==='/notes'&&method==='POST'){
   const f=new URLSearchParams(options.body);let plan;try{plan=f.has('entity_reminders_present')?parse(f):null;if(plan?.reminders.some(s=>s.offsetMinutes!==null)&&(!f.get('due')||!f.get('due_time')))throw Error('Podaj termin i godzinę albo wybierz konkretną datę przypomnienia.');}catch(e){return reply({status:400,data:{message:e.message}});}
   const response=await previous(target,options);if(response.ok){const result=await response.clone().json();if(result.saved){const n=localNotes.find(n=>n.token===f.get('token'));if(n){n.dueTime=f.get('due_time')||null;if(plan)n.entityPlan=plan;parent.postMessage({preview:true,kind:'notes-update',data:localNotes},'*');}}}return response;
  }
  return previous(target,options);
 };
 window.previewWarsawInstant=local=>{if(!local)throw Error('Wpisz datę i godzinę.');let date=new Date(local+'Z');for(let i=0;i<2;i++){const parts=new Intl.DateTimeFormat('sv-SE',{timeZone:'Europe/Warsaw',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit',hourCycle:'h23'}).format(date).replace(' ','T');date=new Date(date.getTime()-(Date.parse(parts+'Z')-Date.parse(local+'Z')));}return date.toISOString();};
})();
