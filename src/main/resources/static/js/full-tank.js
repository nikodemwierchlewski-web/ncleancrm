(() => {
  for(const form of document.querySelectorAll('#fuel-purchase-form[data-entry-mode=full],#fuel-baseline-form')){
    const baseline=form.id==='fuel-baseline-form',input=form.elements.meter,label=form.querySelector('[data-meter-label]');
    const drafts=new Map();let selected=form.querySelector('[name=target]:checked')?.value;
    const snapshot=()=>({meter:input.value,time:form.elements.started_at?.value,version:form.elements.version.value});
    const apply=()=>{const car=form.querySelector('[name=target]:checked')?.value==='CAR';label.textContent=baseline?(car?'Początkowy przebieg auta (km)':'Początkowy licznik agregatu (motogodziny)'):(car?'Aktualny przebieg auta (km)':'Łączny licznik agregatu (motogodziny)');input.pattern=car?'[0-9]+([,.][0-9])?':'[0-9]+([,.][0-9]{1,2})?';if(car)input.value=input.value.replace(/([.,]\d*?)0+$/,'$1').replace(/[.,]$/,'');};
    form.querySelectorAll('[name=target]').forEach(radio=>radio.addEventListener('change',()=>{
      if(selected)drafts.set(selected,snapshot());selected=radio.value;
      const value=drafts.get(selected);input.value=value?.meter??(baseline?radio.dataset.reading||'':'');
      if(baseline){form.elements.version.value=value?.version??radio.dataset.version??'-1';form.elements.started_at.value=value?.time??radio.dataset.time??form.elements.started_at.value;}
      apply();
    }));apply();
  }
  document.addEventListener('click',event=>{const link=event.target.closest('a[href="#parameters"]');if(!link)return;const section=document.querySelector('#parameters');if(section)section.open=true;});
  const form=document.querySelector('#fuel-purchase-form[data-entry-mode=full]');if(!form)return;
  const scaled=(value,digits)=>{const text=value.trim().replace(',','.');if(!/^\d+(?:\.\d+)?$/.test(text))return null;const [whole,fraction='']=text.split('.');return fraction.length>digits?null:BigInt(whole)*10n**BigInt(digits)+BigInt(fraction.padEnd(digits,'0'));};
  const money=value=>new Intl.NumberFormat('pl-PL',{style:'currency',currency:'PLN'}).format(Number(value)/100);
  const calculate=()=>{
    const amount=scaled(form.elements.amount.value,2),litres=scaled(form.elements.litres.value,3);
    document.querySelector('#fuel-total').textContent=amount===null?'—':money(amount);
    const price=amount!==null&&litres!==null&&litres>0n?(amount*10000n+litres/2n)/litres:null;
    document.querySelector('#full-unit-price').textContent=price===null?'—':String(price/1000n)+','+String(price%1000n).padStart(3,'0')+' zł/l';
  };
  for(const input of [form.elements.amount,form.elements.litres])input.addEventListener('input',calculate);calculate();
})();
