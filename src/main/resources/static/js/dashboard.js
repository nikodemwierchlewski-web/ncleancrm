(() => {
  const arcs=[...document.querySelectorAll('.cost-arc')];
  const circumference=2*Math.PI*80;let offset=0;
  const sum=arcs.reduce((total,arc)=>total+Math.max(0,Number(arc.dataset.percent)||0),0);
  for(const arc of arcs){const proportion=sum?Math.max(0,Number(arc.dataset.percent)||0)/sum:0;const length=proportion*circumference;arc.setAttribute('stroke-dasharray',`${length} ${circumference-length}`);arc.setAttribute('stroke-dashoffset',String(-offset));arc.style.opacity=length?1:0;offset+=length;}
  const rows=document.querySelector('#cost-rows'),add=document.querySelector('#add-cost');
  let next=rows?rows.children.length:0;
  add?.addEventListener('click',()=>{
    if(next>=50){add.disabled=true;return;}
    const row=rows.firstElementChild.cloneNode(true);
    row.querySelectorAll('input,select').forEach(input=>{input.name=input.name.replace(/_\d+$/,'_'+next);input.value=input.tagName==='SELECT'?'OTHER':'';});
    rows.append(row);next++;row.querySelector('input').focus();
  });
  rows?.addEventListener('click',event=>{if(event.target.closest('.remove-cost')){const row=event.target.closest('.cost-row');row.querySelectorAll('input').forEach(input=>input.value='');row.hidden=true;}});
  document.querySelectorAll('#month-cost-form,#fuel-cost-form').forEach(form=>form.addEventListener('submit',()=>{form.querySelector('[type=submit]').disabled=true;}));
})();
