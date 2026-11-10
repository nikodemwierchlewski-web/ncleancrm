(() => {
  'use strict';
  const form=document.querySelector('#booking-form');if(!form)return;
  const section=form.querySelector('#chemistry-forecast'),container=form.querySelector('#forecast-items');
  const materialRows=[...form.querySelector('#forecast-materials').content.children];
  const materials=new Map(materialRows.map(row=>[row.dataset.id,{id:row.dataset.id,name:row.dataset.name,unit:row.dataset.unit,cost:Number(row.dataset.cost)}]));
  const recipes=new Map([...form.querySelector('#forecast-recipes').content.children].map(row=>[row.dataset.service,{defined:row.dataset.defined==='true',quantities:new Map([...row.children].map(c=>[c.dataset.chemical,Number(c.dataset.quantity)]))}]));
  const states=new Map();let selected=[];
  const el=(tag,text)=>{const node=document.createElement(tag);if(text!==undefined)node.textContent=text;return node;};
  const money=n=>new Intl.NumberFormat('pl-PL',{style:'currency',currency:'PLN'}).format(n);
  const rounded=(base,qty)=>Math.round(base*qty*1000)/1000;
  function costs(){
    let total=0,unknown=0;
    for(const service of selected){const state=states.get(service.id);let cents=0;
      for(const row of state.rows.values()){row.input.disabled=!state.defined;const amount=Math.round(row.quantity*row.material.cost*100);cents+=amount;row.costLabel.textContent=state.defined?'Koszt: '+money(amount/100):'Nie określono';}
      state.mode.value=state.defined?'defined':'unknown';state.check.checked=state.defined;
      state.cost.textContent=state.defined?'Przewidywany koszt: '+money(cents/100):'Nie określono prognozy.';
      if(state.defined)total+=cents;else unknown++;
    }
    form.querySelector('#forecast-total').textContent=!selected.length?'Nie określono':unknown?(total?money(total/100)+' + nieokreślone pozycje':'Nie określono'):money(total/100);
  }
  function rowFor(state,id,quantity,manual){
    const material=materials.get(id);if(!material)return;
    const node=el('div');node.className='forecast-usage';
    const label=el('label',material.name+' ('+material.unit+')');const costLabel=el('small');costLabel.className='field-help';label.append(costLabel);
    const input=el('input');Object.assign(input,{type:'number',min:'0',max:'99999999999.999',step:'0.001',value:String(quantity),name:`chem_${state.id}_${id}`,required:true});input.setAttribute('aria-label',`Prognoza: ${state.name} — ${material.name}`);
    const remove=el('button','×');remove.type='button';remove.setAttribute('aria-label','Usuń z prognozy: '+material.name);
    const row={material,quantity,manual,input,node,costLabel};state.rows.set(id,row);
    input.addEventListener('input',()=>{row.quantity=Number(input.value||0);row.manual=true;state.defined=true;costs();});
    remove.addEventListener('click',()=>{state.rows.delete(id);state.removed.add(id);node.remove();state.defined=true;costs();});
    label.htmlFor=`forecast-${state.id}-${id}`;input.id=label.htmlFor;node.append(label,input,remove);state.list.append(node);
  }
  function restore(state){
    state.rows.clear();state.removed.clear();state.list.replaceChildren();state.defined=state.recipe.defined;
    for(const [id,q] of state.recipe.quantities)rowFor(state,id,rounded(q,state.quantity),false);
    costs();
  }
  function create(service){
    const recipe=recipes.get(service.id)||{defined:false,quantities:new Map()};
    const card=el('section');card.className='forecast-service';card.dataset.forecastService=service.id;
    const heading=el('h4',service.name),mode=el('input');Object.assign(mode,{type:'hidden',name:'forecast_'+service.id});
    const checkbox=el('label');checkbox.className='check';const check=el('input');check.type='checkbox';checkbox.append(check,document.createTextNode('Prognoza uzupełniona (również zużycie 0)'));
    const list=el('div'),add=el('select');add.className='forecast-add';add.setAttribute('aria-label','Dodaj środek do prognozy: '+service.name);add.append(new Option('+ Dodaj środek',''));
    for(const material of materials.values())add.append(new Option(material.name+' · '+material.unit,material.id));
    const reset=el('button','Przywróć standardowe zużycie');reset.type='button';reset.className='text-button';
    const cost=el('p');const state={id:service.id,name:service.name,quantity:service.quantity,defined:recipe.defined,recipe,rows:new Map(),removed:new Set(),card,list,mode,check,cost};
    check.addEventListener('input',()=>{state.defined=check.checked;costs();});
    add.addEventListener('change',()=>{if(add.value&&!state.rows.has(add.value)){state.removed.delete(add.value);rowFor(state,add.value,0,true);state.defined=true;costs();}add.value='';});
    reset.addEventListener('click',()=>restore(state));card.append(heading,mode,checkbox,list,add,reset,cost);
    for(const [id,q] of recipe.quantities)rowFor(state,id,rounded(q,service.quantity),false);
    return state;
  }
  function update(services){
    selected=services;
    const wanted=new Set(services.map(service=>service.id));
    for(const [id,state] of states)if(!wanted.has(id))state.card.remove();
    for(const service of services){
      if(!states.has(service.id))states.set(service.id,create(service));
      const state=states.get(service.id);state.quantity=service.quantity;
      for(const [id,row] of state.rows)if(!row.manual){row.quantity=rounded(state.recipe.quantities.get(id)||0,service.quantity);if(document.activeElement!==row.input)row.input.value=String(row.quantity);}
      if(!state.card.isConnected)container.append(state.card);
    }
    costs();
  }
  form.addEventListener('booking:change',event=>update(event.detail));
  form.addEventListener('invalid',event=>{if(event.target.closest('.forecast-section'))section.open=true;},true);
  update([]);
})();
