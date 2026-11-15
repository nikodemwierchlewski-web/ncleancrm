;
// Explicit transfer of synthetic exercise records; no production connection or private photo backup.
(() => {
 const panel=document.getElementById('preview-options-panel');
 const description=document.createElement('p');description.textContent='Eksport próby obejmuje dodane zlecenia, notatki, oznaczenia dni, koszty, tankowania i stan przypomnień. Zdjęcia oraz szkice formularzy nie są objęte eksportem. To nie jest kopia bazy CRM.';
 const save=document.createElement('button');save.id='demo-export';save.textContent='Eksport próby JSON';
 const load=document.createElement('button');load.id='demo-import';load.textContent='Wczytaj próbę JSON';
 const input=document.createElement('input');input.type='file';input.accept='application/json,.json';input.hidden=true;
 const status=document.createElement('p');status.id='demo-transfer-status';status.setAttribute('role','status');panel.append(description,save,load,input,status);
 save.onclick=()=>{
  const state={workspace:{fuel:demoFuel,reminders:[...demoReminderEvents]},format:'nclean-demo-1',seed:META.date,bookings:demoBookings,updates:[...demoUpdates],areas:[...demoAreas],notes:noteModel,costs:window.costPreview.snapshot(),removals:[...removalModel]};
  const blob=new Blob([JSON.stringify(state)],{type:'application/json'}),url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download='ncLEAN-proba-'+META.date+'.json';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);status.textContent='Wyeksportowano dane próbne. Zdjęcia i szkice nie są w tym pliku.';
 };
 load.onclick=()=>input.click();
 input.onchange=async()=>{
  let rollback=null;
  try{
   const file=input.files[0];if(!file)return;if(file.size>2*1024*1024)throw Error('Plik jest za duży. Limit 2 MB.');
   const state=JSON.parse(await file.text(),(key,value)=>{if(['__proto__','prototype','constructor'].includes(key))throw Error('Niedozwolona struktura pliku.');return value;});
   const bounded=(value,depth=0)=>{if(depth>10)throw Error('Nieprawidłowy plik.');if(typeof value==='string'&&value.length>10000)throw Error('Za długi tekst.');if(Array.isArray(value)&&value.length>1000)throw Error('Za dużo wpisów.');if(value&&typeof value==='object')Object.values(value).forEach(v=>bounded(v,depth+1));};bounded(state);
   if(state.format!=='nclean-demo-1'||state.seed!==META.date)throw Error('To nie jest eksport tej wersji demonstracji.');
   for(const key of ['bookings','updates','areas','notes','removals'])if(!Array.isArray(state[key]))throw Error('Niepełny eksport.');
   if(!state.costs||!Array.isArray(state.costs.entries)||!Array.isArray(state.costs.assets)||!Number.isInteger(state.costs.counter))throw Error('Nieprawidłowe koszty.');
   for(const b of state.bookings)if(typeof b.id!=='string'||!/^\d{4}-\d{2}-\d{2}$/.test(b.day)||!Array.isArray(b.items)||!Number.isFinite(b.total)||!['PLANNED','DONE','CANCELLED'].includes(b.status))throw Error('Nieprawidłowe zlecenie.');
   for(const n of state.notes)if(!['NOTE','TASK','GOAL'].includes(n.kind)||!Array.isArray(n.steps)||typeof n.title!=='string')throw Error('Nieprawidłowa notatka.');
   for(const key of ['updates','areas','removals'])for(const row of state[key])if(!Array.isArray(row)||row.length!==2||typeof row[0]!=='string')throw Error('Nieprawidłowa mapa danych.');
   if(state.workspace){
    if(!Array.isArray(state.workspace.fuel)||!Array.isArray(state.workspace.reminders))throw Error('Nieprawidłowy zapis pulpitu.');
    for(const fuel of state.workspace.fuel)if(!Number.isSafeInteger(fuel.id)||fuel.id<1||!Number.isSafeInteger(fuel.version)||fuel.version<0||!Number.isFinite(fuel.amount)||fuel.amount<0||!['CAR','GENERATOR','SHARED'].includes(fuel.target)||typeof fuel.purchased_at!=='string'||!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2})?$/.test(fuel.purchased_at))throw Error('Nieprawidłowe tankowanie.');
    for(const row of state.workspace.reminders)if(!Array.isArray(row)||row.length!==2||typeof row[0]!=='string'||!row[1]||typeof row[1]!=='object')throw Error('Nieprawidłowy stan przypomnienia.');
   }
   const before={workspace:{fuel:structuredClone(demoFuel),reminders:[...demoReminderEvents]},bookings:structuredClone(demoBookings),updates:[...demoUpdates],areas:[...demoAreas],notes:structuredClone(noteModel),removals:[...removalModel],costs:structuredClone(window.costPreview.snapshot()),sequence:demoSequence,route:current};
   rollback=()=>{demoFuel=before.workspace.fuel;demoReminderEvents.clear();before.workspace.reminders.forEach(([key,value])=>demoReminderEvents.set(key,value));demoBookings.splice(0,demoBookings.length,...before.bookings);for(const [map,rows]of [[demoUpdates,before.updates],[demoAreas,before.areas],[removalModel,before.removals]]){map.clear();rows.forEach(([k,v])=>map.set(k,v));}noteModel=before.notes;demoSequence=before.sequence;window.costPreview.restore(before.costs);navigate(before.route);};
   state.costs.entries.forEach(e=>{if(e.receipt_id&&!IMAGES['/fuel/receipts/'+e.receipt_id])e.receipt_id='';});
   demoBookings.splice(0,demoBookings.length,...state.bookings);for(const [map,rows]of [[demoUpdates,state.updates],[demoAreas,state.areas],[removalModel,state.removals]]){map.clear();rows.forEach(([k,v])=>map.set(k,v));}
   demoSequence=Math.max(7000,...demoBookings.map(b=>Number(b.id)||0));noteModel=state.notes;if(state.workspace){if(!Array.isArray(state.workspace.fuel)||!Array.isArray(state.workspace.reminders))throw Error('Nieprawidłowy zapis pulpitu.');demoFuel=state.workspace.fuel;demoReminderEvents.clear();state.workspace.reminders.forEach(([key,value])=>demoReminderEvents.set(key,value));}window.costPreview.restore(state.costs);
   navigate(current||'/calendar?day='+META.date);rollback=null;status.textContent='Wczytano dane próbne. Brak połączenia z bazą prawdziwego CRM.';
  }catch(error){if(rollback)rollback();status.textContent='Nie wczytano: '+error.message;}finally{input.value='';}
 };
})();
