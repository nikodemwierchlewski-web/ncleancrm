(() => {
 document.querySelector('#go-back')?.addEventListener('click',()=>history.back());
 const form=document.querySelector('#booking-form');if(!form)return;
 const category=document.querySelector('#booking-category');
 const lines=[...document.querySelectorAll('.service-line')];
 function total(){let cents=0;for(const line of lines){const input=line.querySelector('input');if(!input.disabled)cents+=Math.round(Number(line.dataset.price)*Number(input.value||0)*100);}document.querySelector('#booking-total').textContent=new Intl.NumberFormat('pl-PL',{style:'currency',currency:'PLN'}).format(cents/100);}
 function filter(){for(const line of lines){line.hidden=line.dataset.category!==category.value;line.querySelector('input').disabled=line.hidden;}total();}
 category.addEventListener('change',filter);form.addEventListener('input',total);filter();
 const search=document.querySelector('#customer-search'),select=document.querySelector('#booking-customer'),status=document.querySelector('#customer-search-status');
 let timer,sequence=0;
 search.addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();search.dispatchEvent(new Event('input'));}});
 search.addEventListener('input',()=>{clearTimeout(timer);const current=++sequence;timer=setTimeout(async()=>{try{
 const body=new URLSearchParams({query:search.value,_csrf:form.querySelector('[name=_csrf]').value});
 const response=await fetch('/customers/lookup',{method:'POST',body});if(!response.ok)throw Error();const customers=await response.json();if(current!==sequence)return;
 select.replaceChildren(new Option('Nowy klient — wpisz dane poniżej',''));for(const customer of customers){const option=new Option(customer.name+' · '+customer.phone,customer.id);option.dataset.address=customer.address;select.append(option);}status.textContent=customers.length?'Wybierz klienta z listy.':'Brak dopasowania. Możesz dodać nowego klienta.';
 select.dispatchEvent(new Event('change'));
 }catch(_){status.textContent='Nie udało się wyszukać. Spróbuj ponownie lub dodaj nowego klienta.';}},250);});
 select.addEventListener('change',()=>{const existing=!!select.value;document.querySelector('#new-customer').hidden=existing;document.querySelectorAll('#new-customer input').forEach(input=>{input.required=!existing;input.disabled=existing;});if(existing)document.querySelector('#booking-address').value=select.selectedOptions[0].dataset.address||'';});
 form.addEventListener('submit',event=>{if(!lines.some(line=>!line.querySelector('input').disabled&&Number(line.querySelector('input').value)>0)){event.preventDefault();status.textContent='Dodaj przynajmniej jedną usługę.';}});
})();
