(() => {
 'use strict';
 function reveal(hash){if(!hash||hash==='#')return;let node;try{node=document.getElementById(decodeURIComponent(hash.slice(1)));}catch{return;}if(!node)return;let current=node;while(current){if(current.tagName==='DETAILS')current.open=true;current=current.parentElement;}node.scrollIntoView({block:'nearest'});}
 document.addEventListener('click',e=>{const link=e.target.closest('a[href]');if(!link)return;const href=link.getAttribute('href');if(href?.startsWith('#'))queueMicrotask(()=>reveal(href));});
 addEventListener('hashchange',()=>reveal(location.hash));
 const width=matchMedia('(min-width:768px)');const queues=()=>document.querySelectorAll('.review-queue').forEach(q=>q.open=width.matches);width.addEventListener('change',queues);queues();
 reveal(location.hash);
})();
