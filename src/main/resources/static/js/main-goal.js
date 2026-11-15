/* One explicit, persisted goal. The server is authoritative; decoration is optional. */
(() => {
 'use strict';
 const cards=[...document.querySelectorAll('[data-main-goal]')];if(!cards.length)return;
 let goal=null,loading=false,saving=false,queued=false,lastShown=null,focusCard=null;
 const node=(tag,text,cls)=>{const n=document.createElement(tag);if(text!=null)n.textContent=text;if(cls)n.className=cls;return n;};
 const link=(text,path)=>{const n=node('a',text,'text-link');n.href=Nclean.url(path);return n;};
 function ring(g){
  const svg=document.createElementNS('http://www.w3.org/2000/svg','svg');svg.setAttribute('viewBox','0 0 132 140');svg.setAttribute('class','goal-pixel-ring');svg.setAttribute('role','img');svg.setAttribute('aria-label',g.total?g.done+' z '+g.total+' kroków, '+g.percent+' procent':'Cel bez kroków. Bez wyliczania procentu.');
  const points=[[4,0],[5,0],[6,0],[7,1],[8,2],[9,3],[10,4],[10,5],[10,6],[9,7],[8,8],[7,9],[6,10],[5,10],[4,10],[3,9],[2,8],[1,7],[0,6],[0,5],[0,4],[1,3],[2,2],[3,1]];
  points.forEach(([x,y],i)=>{for(const shadow of [true,false]){const tile=document.createElementNS(svg.namespaceURI,'rect');tile.setAttribute('x',x*11+7);tile.setAttribute('y',y*11+7+(shadow?4:0));tile.setAttribute('width',9);tile.setAttribute('height',9);tile.setAttribute('fill',shadow?'#9dbbb7':g.total&&i<Math.floor(points.length*g.done/g.total)?'#147d8b':'#dbe9dd');svg.append(tile);}});
  const text=document.createElementNS(svg.namespaceURI,'text');text.setAttribute('x',66);text.setAttribute('y',71);text.setAttribute('text-anchor','middle');text.textContent=g.total?g.percent+'%':g.completed?'✓':'—';svg.append(text);return svg;
 }
 function actionForm(g,action,label,step){
  const form=node('form',null,'main-goal-action');form.method='post';form.action=Nclean.url('/notes/'+g.id+'/action');
  const values={version:g.version,action};if(step){values.step=step.id;values.checked='true';}
  for(const [name,value]of Object.entries(values)){const input=node('input');input.type='hidden';input.name=name;input.value=value;form.append(input);}
  const button=node('button',label,step?'goal-next-step':'button secondary');button.type='submit';button.disabled=saving;form.append(button);return form;
 }
 function render(){for(const card of cards){const compact=card.dataset.compact==='true',content=card.querySelector('[data-goal-content]');content.replaceChildren();card.classList.toggle('is-achieved',Boolean(goal?.completed));
  const caption=card.querySelector('[data-goal-caption]');if(caption)caption.textContent=goal?'Cel · '+goal.title+(goal.total?' · '+goal.done+'/'+goal.total:''):'Wybierz główny cel';
  if(!goal){if(!compact){content.append(node('small','MAŁE KROKI, DUŻY EFEKT','goal-eyebrow'),node('h2','Co chcesz osiągnąć?'),node('p','Wybierz jeden główny cel. Będziesz widzieć jego prawdziwy postęp.'));}content.append(link('Wybierz w Notatkach i celach →','/notes'));continue;}
  const body=node('div',null,'goal-body');if(!compact){const graphic=node('div',null,'goal-art');graphic.append(ring(goal));const img=node('img',null,'goal-cleaner');img.src=Nclean.url('/pixel/goal-cleaner.svg');img.width=96;img.height=88;img.alt='';graphic.append(img);content.append(graphic);body.append(node('small',goal.completed?'CEL OSIĄGNIĘTY':'TWÓJ GŁÓWNY CEL','goal-eyebrow'),node('h2',goal.title));}
  const count=goal.total?goal.done+' z '+goal.total+' kroków':goal.completed?'Ukończony przez Ciebie.':'Cel bez kroków — ukończenie zaznaczasz samodzielnie.';body.append(node('p',count,'goal-count'));
  if(goal.total){const progress=node('progress');progress.max=goal.total;progress.value=goal.done;progress.setAttribute('aria-label','Postęp głównego celu');body.append(progress);window.NcleanMotion?.progress(progress,'main-goal:'+goal.id);}
  if(goal.due)body.append(node('small','Termin: '+goal.due.split('-').reverse().join('.'),'goal-due'));
  if(!goal.completed){const next=goal.steps.find(s=>!s.done);if(next)body.append(actionForm(goal,'step','□ '+next.text,next));else body.append(actionForm(goal,'done',goal.total?'✓ Potwierdź osiągnięcie celu':'✓ Cel osiągnięty'));}
  else body.append(actionForm(goal,'reopen','Przywróć do aktywnych'));
  const links=node('div',null,'goal-links');const edit=link('Edytuj cel',document.querySelector('#note-editor')?'/notes/editor?id='+goal.id:'/notes#memo-'+goal.id);if(document.querySelector('#note-editor'))edit.dataset.noteEdit='';links.append(edit);if(compact)links.append(link('Podsumowanie →','/dashboard'));else links.append(link('Zmień główny cel','/notes'));body.append(links);content.append(body);if(!compact&&lastShown?.id===goal.id&&lastShown.done!==goal.done){const art=content.querySelector('.goal-art');const rect=art.getBoundingClientRect();if(rect.bottom>0&&rect.top<innerHeight)window.NcleanMotion?.animate(art,[{opacity:.55,transform:'translateY(2px)'},{opacity:1,transform:'translateY(0)'}],240,'goal-progress');}
 }lastShown=goal?{id:goal.id,done:goal.done}:null;if(focusCard){focusCard.querySelector('.main-goal-action button,.goal-links a,a')?.focus({preventScroll:true});focusCard=null;}}
 async function refresh(){if(loading||saving){queued=true;return;}loading=true;try{const r=await fetch(Nclean.url('/notes/main-goal'),{headers:{Accept:'application/json'}});if(!r.ok||r.redirected)throw Error('Nie udało się odczytać celu. Sprawdź połączenie i sesję.');const data=await r.json();if(typeof data.selected!=='boolean'||data.selected&&!Array.isArray(data.goal?.steps))throw Error('Nie udało się odczytać celu.');goal=data.selected?data.goal:null;render();cards.forEach(c=>c.querySelector('.main-goal-error').textContent='');}catch(e){cards.forEach(c=>c.querySelector('.main-goal-error').textContent=e.message);}finally{loading=false;if(queued){queued=false;refresh();}}}
 document.addEventListener('submit',async event=>{const form=event.target;if(!form.matches('.main-goal-action'))return;event.preventDefault();if(saving)return;saving=true;form.setAttribute('aria-busy','true');cards.forEach(c=>c.querySelectorAll('.main-goal-action button').forEach(b=>b.disabled=true));const error=form.closest('[data-main-goal]').querySelector('.main-goal-error');error.textContent='';
  try{const r=await fetch(form.getAttribute('action'),{method:'POST',body:new URLSearchParams(new FormData(form)),headers:{Accept:'application/json'}});let result;try{result=await r.json();}catch{throw Error('Sprawdź połączenie i sesję. Możesz ponowić zapis.');}if(!r.ok||!result.saved)throw Error(result.message||'Nie udało się zapisać.');focusCard=form.closest('[data-main-goal]');window.NcleanMotion?.confirmed({kind:result.motion||'updated',entry:goal.id,id:form.elements.step?.value},false,form);document.dispatchEvent(new Event('nclean:notes-saved'));}
  catch(e){error.textContent=e.message;}
  finally{saving=false;form.removeAttribute('aria-busy');cards.forEach(c=>c.querySelectorAll('.main-goal-action button').forEach(b=>b.disabled=false));if(queued){queued=false;refresh();}}
 });
 document.addEventListener('nclean:notes-saved',refresh);addEventListener('pageshow',e=>{if(e.persisted)refresh();});refresh();
})();
