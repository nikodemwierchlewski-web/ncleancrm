(() => {
  'use strict';
  const money=n=>new Intl.NumberFormat('pl-PL',{style:'currency',currency:'PLN'}).format(n);
  for(const form of document.querySelectorAll('.service-recipe form')) {
    const rows=[...form.querySelectorAll('.standard-row')];
    const update=()=>{
      let cents=0,hasQuantity=false;
      for(const row of rows){
        const quantity=Number(row.querySelector('input').value||0);hasQuantity ||= quantity>0;
        const amount=Math.round(quantity*Number(row.dataset.cost)*100);cents+=amount;
        row.querySelector('output').textContent='Koszt pozycji: '+money(amount/100);
      }
      const defined=form.querySelector('[name=defined]').checked||hasQuantity;
      form.querySelector('.standard-total').textContent=defined?'Przewidywany koszt: '+money(cents/100):'Nie określono standardu.';
    };
    form.addEventListener('input',update);update();
  }
})();
