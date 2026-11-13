(() => {
 'use strict';
 const main=document.querySelector('main');if(main&&!main.id)main.id='main-content';document.querySelectorAll('.skip-link').forEach((link,index)=>{if(index)link.remove();else if(main){link.href='#'+main.id;main.tabIndex=-1;}});
 const width=matchMedia('(min-width:768px)');const queues=()=>document.querySelectorAll('.review-queue').forEach(q=>q.open=width.matches);width.addEventListener('change',queues);queues();

})();
