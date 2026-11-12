(() => {
  const rows=document.querySelector('#cost-rows'),add=document.querySelector('#add-cost');
  let next=rows?rows.children.length:0;
  add?.addEventListener('click',()=>{
    const reusable=[...rows.children].find(row=>row.hidden);
    if(reusable){reusable.hidden=false;reusable.querySelector('input').focus();return;}
    if(next>=50){add.disabled=true;return;}
    const row=rows.firstElementChild.cloneNode(true);
    row.hidden=false;
    row.querySelectorAll('input,select').forEach(input=>{input.name=input.name.replace(/_\d+$/,'_'+next);input.value=input.tagName==='SELECT'?'OTHER':'';});
    rows.append(row);next++;row.querySelector('input').focus();
  });
  rows?.addEventListener('click',event=>{if(event.target.closest('.remove-cost')){const row=event.target.closest('.cost-row');row.querySelectorAll('input').forEach(input=>input.value='');row.querySelector('select').value='OTHER';row.hidden=true;if(add)add.disabled=false;}});
})();
