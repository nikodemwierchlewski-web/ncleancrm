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
  const form=document.querySelector('#complete-form');
  if(form){
    const allocations=form.querySelector('.allocation-fields');
    if(allocations){
      const warning=document.createElement('div');warning.className='allocation-warning';warning.hidden=true;warning.setAttribute('role','alert');
      const message=document.createElement('p'),clear=document.createElement('button');clear.type='button';clear.className='button secondary';clear.textContent='Usuń bieżący podział — rozlicz całe zlecenie';warning.append(message,clear);form.querySelector('.final-price').after(warning);
      const fields=[...allocations.querySelectorAll('input[type=number]')];
      const mismatch=()=>{
        const check=(prefix,total,scale)=>{const inputs=fields.filter(n=>n.name.startsWith(prefix));if(!inputs.some(n=>n.value!==''))return false;return inputs.some(n=>n.value==='')||total===''||inputs.reduce((sum,n)=>sum+Math.round(Number(n.value)*scale),0)!==Math.round(Number(total)*scale);};
        return form.querySelector('[name=outcome]:checked')?.value==='DONE'&&(check('amount_',form.elements.final_total.value,100)||(form.dataset.timeSource!=='TIMER'&&check('time_',form.elements.minutes.value,1)));
      };
      const explain=()=>{warning.hidden=!mismatch();if(!warning.hidden)message.textContent='Kwota lub czas różni się od zapisanego podziału. Popraw pozycje poniżej albo świadomie usuń ich podział.';};
      form.addEventListener('input',explain);form.addEventListener('change',explain);
      clear.addEventListener('click',()=>{fields.forEach(n=>n.value='');explain();form.elements.final_total.focus();});
      form.addEventListener('submit',event=>{if(!mismatch())return;event.preventDefault();event.stopImmediatePropagation();explain();form.querySelector('.completion-extra').open=true;allocations.open=true;fields.find(n=>n.name.startsWith('amount_'))?.focus();warning.scrollIntoView({block:'nearest'});},true);
    }
  }
  update();document.addEventListener('change',event=>{if(event.target.matches('[name=outcome],[data-chem-mode]'))update();});
  document.addEventListener('submit',async event=>{
    const form=event.target;if(!form.matches('.day-review-form'))return;event.preventDefault();if(form.dataset.saving)return;
    const button=form.querySelector('[type=submit]'),error=form.querySelector('.review-error');
    form.dataset.saving='true';button.disabled=true;form.setAttribute('aria-busy','true');error.textContent='Zapisywanie…';
    try{const response=await fetch(form.getAttribute('action'),{method:'POST',body:new URLSearchParams(new FormData(form)),headers:{Accept:'application/json'}});let result;
      try{result=await response.json();}catch{throw Error('Sprawdź połączenie i zalogowanie. Wpisane dane pozostają tutaj.');}
      if(!response.ok)throw Error(result.message||'Nie udało się zapisać. Spróbuj ponownie.');
      window.NcleanDrafts?.saved(form);
      if(result.url){window.NcleanMotion?.confirmed({kind:result.motion||'updated',id:result.id},true);location.assign(result.url);}else error.textContent='Zapis demonstracyjny. Dane nie trafiły do bazy.';
    }catch(exception){error.textContent=exception.message;if(/podział|Czasy pozycji|kwoty wszystkich pozycji/i.test(exception.message)){for(const detail of form.querySelectorAll('.completion-extra,.allocation-fields'))detail.open=true;const warning=form.querySelector('.allocation-warning');if(warning){warning.hidden=false;warning.querySelector('p').textContent=exception.message;}form.querySelector('.allocation-fields input[type=number]')?.focus();}error.scrollIntoView({block:'nearest'});if(form.getAttribute('action')==='/day-closeout/close'){let link=form.querySelector('.missing-link');if(!link){const first=document.querySelector('.review-row[data-incomplete="true"]');if(first){link=document.createElement('a');link.className='text-link missing-link';link.href=first.getAttribute('href');link.textContent='Przejdź do brakującego zlecenia →';error.after(link);}}}}
    finally{delete form.dataset.saving;button.disabled=false;form.removeAttribute('aria-busy');}
  });
})();
