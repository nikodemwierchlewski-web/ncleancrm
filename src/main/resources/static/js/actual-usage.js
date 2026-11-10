(() => {
 'use strict';
 document.addEventListener('click',event=>{
  const button=event.target.closest('[data-actual-zero]');if(!button)return;
  for(const input of button.closest('form').querySelectorAll('input[type=number]'))input.value='0';
 });
 document.addEventListener('submit',event=>{
  const form=event.target;if(!form.matches('.actual-usage-form'))return;event.preventDefault();
  const status=form.querySelector('.save-error');status.classList.remove('save-success');
  window.NcleanForms.submit(form,status,()=>{
   form.elements.namedItem('version').value=String(Number(form.elements.namedItem('version').value)+1);
   const recorded=[...form.querySelectorAll('input[type=number]')].some(input=>input.value!=='');
   form.querySelector('.actual-state').textContent=recorded?'Zapisane rzeczywiste ilości':'Nie uzupełniono rzeczywistego zużycia';
   status.textContent=recorded?'Zapisano rzeczywiste zużycie.':'Usunięto pomiary. Puste pola nie są próbkami.';
   status.classList.add('save-success');delete form.dataset.saving;form.removeAttribute('aria-busy');
   form.querySelector('button[type=submit]').disabled=false;
   // Keep the open panel and the other service drafts; reopening must use fresh versions.
   for(const input of form.querySelectorAll('input'))input.defaultValue=input.value;
   const content=form.closest('#order-content'),template=document.querySelector('#order-'+form.dataset.bookingId);
   if(content&&template)template.content.replaceChildren(...[...content.children].map(child=>child.cloneNode(true)));
  });
 });
})();
