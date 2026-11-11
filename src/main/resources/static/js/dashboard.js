(() => {
  const donut=document.querySelector('.donut');
  if(donut && donut.dataset.empty!=='true'){
    let start=0;const segments=[];
    document.querySelectorAll('[data-percent]').forEach(row=>{const end=start+Number(row.dataset.percent);segments.push(`${row.dataset.color} ${start}% ${end}%`);start=end;});
    donut.style.background=`conic-gradient(${segments.join(',')})`;
  }
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
