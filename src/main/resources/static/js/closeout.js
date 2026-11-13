(() => {
  const update = () => {
    const form=document.querySelector('#complete-form.day-review-form');
    if(!form)return;
    const outcome=form.querySelector('[name=outcome]:checked')?.value;
    form.querySelectorAll('[data-outcome]').forEach(section=>{
      const active=section.dataset.outcome===outcome;section.hidden=!active;
      section.querySelectorAll('input,select,textarea').forEach(input=>input.disabled=!active);
    });
    const reason=form.querySelector('[data-reason]');reason.hidden=!['MOVED','CANCELLED'].includes(outcome);reason.querySelector('input').disabled=reason.hidden;
    form.querySelector('[name=final_total]').required=outcome==='DONE';
    form.querySelectorAll('[name=new_day],[name=new_starts]').forEach(input=>input.required=outcome==='MOVED');
    form.querySelectorAll('[data-chem-mode]').forEach(select=>{
      const fields=select.closest('article').querySelector('.measured-fields');fields.hidden=select.value!=='MEASURED';fields.querySelectorAll('input').forEach(input=>input.disabled=outcome!=='DONE'||fields.hidden);
    });
  };
  update();document.addEventListener('change',event=>{if(event.target.matches('[name=outcome],[data-chem-mode]'))update();});
  document.addEventListener('submit',async event=>{
    const form=event.target;if(!form.matches('.day-review-form'))return;event.preventDefault();if(form.dataset.saving)return;
    const button=form.querySelector('[type=submit]'),error=form.querySelector('.review-error');
    form.dataset.saving='true';button.disabled=true;form.setAttribute('aria-busy','true');error.textContent='Zapisywanie…';
    try{const response=await fetch(form.getAttribute('action'),{method:'POST',body:new URLSearchParams(new FormData(form)),headers:{Accept:'application/json'}});let result;
      try{result=await response.json();}catch{throw Error('Sprawdź połączenie i zalogowanie. Wpisane dane pozostają tutaj.');}
      if(!response.ok)throw Error(result.message||'Nie udało się zapisać. Spróbuj ponownie.');
      if(result.url){window.NcleanMotion?.confirmed({kind:result.motion||'updated',id:result.id},true);location.assign(result.url);}else error.textContent='Zapis demonstracyjny. Dane nie trafiły do bazy.';
    }catch(exception){error.textContent=exception.message;error.scrollIntoView({block:'nearest'});if(form.getAttribute('action')==='/day-closeout/close'){let link=form.querySelector('.missing-link');if(!link){const first=document.querySelector('.review-row[data-incomplete="true"]');if(first){link=document.createElement('a');link.className='text-link missing-link';link.href=first.getAttribute('href');link.textContent='Przejdź do brakującego zlecenia →';error.after(link);}}}}
    finally{delete form.dataset.saving;button.disabled=false;form.removeAttribute('aria-busy');}
  });
})();
