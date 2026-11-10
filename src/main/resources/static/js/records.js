(() => {
 document.querySelector('#go-back')?.addEventListener('click',()=>history.back());
 const form=document.querySelector('#booking-form');if(!form)return;
 const category=document.querySelector('#booking-category');
 const lines=[...document.querySelectorAll('.service-line')];
 function total(){let cents=0;for(const line of lines){const input=line.querySelector('input');if(!input.disabled)cents+=Math.round(Number(line.dataset.price)*Number(input.value||0)*100);}document.querySelector('#booking-total').textContent=new Intl.NumberFormat('pl-PL',{style:'currency',currency:'PLN'}).format(cents/100);}
 function filter(){for(const line of lines){line.hidden=line.dataset.category!==category.value;line.querySelector('input').disabled=line.hidden;}total();}
 category.addEventListener('change',filter);form.addEventListener('input',total);filter();
 const search=document.querySelector('#customer-search'),select=document.querySelector('#booking-customer'),status=document.querySelector('#customer-search-status');
 let timer,sequence=0,chosenCustomer='';
 search.addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();search.dispatchEvent(new Event('input'));}});
 search.addEventListener('input',()=>{clearTimeout(timer);const current=++sequence;timer=setTimeout(async()=>{try{
 const body=new URLSearchParams({query:search.value,_csrf:form.querySelector('[name=_csrf]').value});
 const response=await fetch('/customers/lookup',{method:'POST',body});if(!response.ok)throw Error();const customers=await response.json();if(current!==sequence)return;
 select.replaceChildren(new Option('Nowy klient — wpisz dane poniżej',''));for(const customer of customers){const option=new Option(customer.name+' · '+customer.phone,customer.id);option.dataset.address=customer.address;option.dataset.name=customer.name;option.dataset.phone=customer.phone;select.append(option);}status.textContent=customers.length?'Wybierz klienta z listy.':'Brak dopasowania. Możesz dodać nowego klienta.';
 select.dispatchEvent(new Event('change'));
 }catch(_){status.textContent='Nie udało się wyszukać. Spróbuj ponownie lub dodaj nowego klienta.';}},250);});
 select.addEventListener('change',()=>{const existing=!!select.value,option=select.selectedOptions[0];document.querySelector('#new-customer').hidden=false;document.querySelectorAll('#new-customer input').forEach(input=>{input.required=true;input.disabled=false;});if(existing){form.elements.name.value=option.dataset.name;form.elements.phone.value=option.dataset.phone;document.querySelector('#booking-address').value=option.dataset.address||'';}else if(chosenCustomer){form.elements.name.value='';form.elements.phone.value='';}chosenCustomer=select.value;});
 for(const line of lines){const input=line.querySelector('input'),wrap=document.createElement('span');wrap.className='stepper';input.before(wrap);for(const [label,delta] of [['−',-1],['+',1]]){const button=document.createElement('button');button.type='button';button.textContent=label;button.setAttribute('aria-label',(delta>0?'Zwiększ ilość: ':'Zmniejsz ilość: ')+line.querySelector('strong').textContent);button.addEventListener('click',()=>{input.value=String(Math.max(0,Math.min(10000,Number(input.value||0)+delta)));input.dispatchEvent(new Event('input',{bubbles:true}));});wrap.append(button);if(delta<0)wrap.append(input);}}
 form.addEventListener('submit',event=>{if(!lines.some(line=>!line.querySelector('input').disabled&&Number(line.querySelector('input').value)>0)){event.preventDefault();status.textContent='Dodaj przynajmniej jedną usługę.';}});
})();
