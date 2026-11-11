(() => {
  document.querySelectorAll('.fuel-day-form,#fuel-purchase-form,#parameters form').forEach(form=>form.addEventListener('submit',()=>{form.querySelector('[type=submit]').disabled=true;}));
  document.querySelectorAll('.fuel-day-form').forEach(form=>{
    const field=form.querySelector('[name=odometer]'),message=form.querySelector('.reading-impact');
    field.addEventListener('input',()=>{const value=Number(field.value),old=Number(form.dataset.original);if(!form.dataset.original||field.value===''||value===old){message.textContent='';return;}const parts=[];if(form.dataset.previous)parts.push('od poprzedniego odczytu: '+(old-Number(form.dataset.previous)).toFixed(1)+' → '+(value-Number(form.dataset.previous)).toFixed(1)+' km');if(form.dataset.next)parts.push('do następnego odczytu: '+(Number(form.dataset.next)-old).toFixed(1)+' → '+(Number(form.dataset.next)-value).toFixed(1)+' km');message.textContent='Wpływ korekty: '+parts.join('; ')+'. Luki w dniach pozostaną bez automatycznego podziału.';});
  });
  const form=document.querySelector('#fuel-purchase-form');if(!form)return;
  const status=document.querySelector('#receipt-status'),image=document.querySelector('#receipt-preview'),receipt=document.querySelector('#receipt-id'),retry=document.querySelector('#upload-retry');
  const csrf=form.querySelector('[name=_csrf]').value;let pending=null,uploading=false;
  form.addEventListener('submit',event=>{if(uploading){event.preventDefault();status.textContent='Poczekaj na zakończenie przesyłania zdjęcia.';}});
  async function upload(){
    if(!pending||uploading)return;uploading=true;const submit=form.querySelector('[type=submit]');submit.disabled=true;document.querySelectorAll('[data-receipt-file]').forEach(input=>input.disabled=true);retry.hidden=true;status.textContent='Przesyłanie zdjęcia…';
    try{const body=new FormData();body.append('image',pending);body.append('_csrf',csrf);const response=await fetch('/fuel/receipts',{method:'POST',body});let data;try{data=await response.json();}catch{throw new Error('Nie udało się przesłać zdjęcia. Zapisane pola pozostają; spróbuj ponownie.');}if(!response.ok)throw new Error(data.error||'Błąd przesyłania zdjęcia.');const old=receipt.value;receipt.value=data.id;image.src=data.url;image.hidden=false;pending=null;status.textContent='Zdjęcie gotowe. Zapisz tankowanie, aby je przypisać.';if(old&&old!==data.id)discard(old);}
    catch(error){status.textContent=error.message;retry.hidden=false;}finally{uploading=false;submit.disabled=false;document.querySelectorAll('[data-receipt-file]').forEach(input=>input.disabled=false);}
  }
  async function discard(id){try{await fetch('/fuel/receipts/'+id+'/discard',{method:'POST',body:new URLSearchParams({_csrf:csrf})});}catch{/* Unlinked uploads expire after 24 hours on the next upload. */}}
  document.querySelectorAll('[data-receipt-file]').forEach(input=>input.addEventListener('change',()=>{pending=input.files[0];if(pending)upload();}));retry.addEventListener('click',upload);
  document.querySelector('#remove-receipt').addEventListener('click',()=>{if(uploading){status.textContent='Poczekaj na zakończenie przesyłania.';return;}const old=receipt.value;receipt.value='';pending=null;image.removeAttribute('src');image.hidden=true;retry.hidden=true;document.querySelectorAll('[data-receipt-file]').forEach(input=>input.value='');status.textContent='Usunięto z formularza. Zapisz tankowanie, aby potwierdzić zmianę.';if(old)discard(old);});
  for(const key of ['amount','litres'])form.querySelector('[name='+key+']').addEventListener('input',()=>{const amount=Number(form.querySelector('[name=amount]').value),litres=Number(form.querySelector('[name=litres]').value);document.querySelector('#calculated-price').textContent=litres>0?'Cena z kwoty i litrów: '+(amount/litres).toFixed(3)+' zł/l.':'Podaj litry, aby wyliczyć cenę litra.';});
})();
