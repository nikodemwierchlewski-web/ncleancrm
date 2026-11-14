/* Pointer interaction and presentation only. No business requests or form values. */
(() => {
 'use strict';
 const dock=document.querySelector('.ncleanek-dock'),actor=dock?.querySelector('.ncleanek-launch');
 if(!dock||!actor)return;
 const key='nclean.puppet.position.v1',root=document.documentElement;
 let preferred=null,position={x:0,y:0},gesture=null,frame=0,layoutTimer=0,landingTimer=0,waveTimer=0,suppressClickUntil=0;
 const valid=p=>p&&Number.isFinite(p.rx)&&Number.isFinite(p.ry)&&p.rx>=0&&p.rx<=1&&p.ry>=0&&p.ry<=1;
 try{const p=JSON.parse(localStorage.getItem(key));if(valid(p))preferred=p;}catch{}
 if(typeof dock.showPopover==='function')dock.setAttribute('popover','manual');
 document.body.append(dock);actor.setAttribute('aria-keyshortcuts','ArrowUp ArrowDown ArrowLeft ArrowRight Home');
 const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
 function bounds(){
  const v=window.visualViewport,left=(v?.offsetLeft||0)+10,top=(v?.offsetTop||0)+10;
  const right=left+(v?.width||innerWidth)-20;
  let bottom=top+(v?.height||innerHeight)-20;
  const nav=document.querySelector('.record-nav')?.getBoundingClientRect();
  if(nav&&nav.height&&nav.top>top&&nav.bottom>=bottom-12)bottom=Math.min(bottom,nav.top-8);
  return {left,top,right,bottom};
 }
 function size(){return {w:dock.offsetWidth||76,h:dock.offsetHeight||112};}
 function constrained(p,b=bounds()){const {w,h}=size();return {x:clamp(p.x,b.left,Math.max(b.left,b.right-w)),y:clamp(p.y,b.top,Math.max(b.top,b.bottom-h))};}
 function paint(p){position=constrained(p);dock.style.left=position.x+'px';dock.style.top=position.y+'px';dock.style.right='auto';dock.style.bottom='auto';}
 function portal(){
  const parent=document.querySelector('dialog[open]')||document.body;
  if(dock.parentElement!==parent)parent.append(dock);
  if(dock.showPopover&&!dock.matches(':popover-open')){try{dock.showPopover();}catch{}}
 }
 const overlaps=(a,b)=>a.x<b.right+6&&a.x+a.w>b.left-6&&a.y<b.bottom+6&&a.y+a.h>b.top-6;
 function obstacles(){
  const scope=document.querySelector('dialog[open]')||document;
  const nodes=[...scope.querySelectorAll('input:not([type=hidden]),textarea,select,button,a[href],summary,label,h1,h2,[role=button]')];
  if(scope===document){const header=document.querySelector('.app-header');if(header)nodes.push(header);}
  const out=[];
  for(const n of nodes){if(n.closest('.ncleanek-dock,.ncleanek-panel')||!n.getClientRects().length)continue;let rects;if(n.matches('h1,h2')){const range=document.createRange();range.selectNodeContents(n);rects=[...range.getClientRects()];}else rects=[n.getBoundingClientRect()];for(const r of rects)if(r.width&&r.height&&r.bottom>0&&r.top<innerHeight)out.push(r);}
  const panel=document.querySelector('.ncleanek-panel:not([hidden])');if(panel?.getClientRects().length)out.push(panel.getBoundingClientRect());
  return out;
 }
 function freePosition(wanted,rects){
  const b=bounds(),{w,h}=size(),p=constrained(wanted,b);
  const safe=(x,y)=>!rects.some(r=>overlaps({x,y,w,h},r));
  if(safe(p.x,p.y))return p;
  const xs=new Set([b.left,Math.max(b.left,b.right-w),p.x]),ys=new Set([b.top,Math.max(b.top,b.bottom-h),p.y]);
  for(let x=b.left;x<=b.right-w;x+=28)xs.add(x);
  for(let y=b.top;y<=b.bottom-h;y+=28)ys.add(y);
  // Include exact obstacle boundaries so narrow safe gaps are not lost to grid sampling.
  for(const r of rects){xs.add(clamp(r.left-w-6,b.left,Math.max(b.left,b.right-w)));xs.add(clamp(r.right+6,b.left,Math.max(b.left,b.right-w)));ys.add(clamp(r.top-h-6,b.top,Math.max(b.top,b.bottom-h)));ys.add(clamp(r.bottom+6,b.top,Math.max(b.top,b.bottom-h)));}
  let best=null,score=Infinity;
  for(const x of xs)for(const y of ys){const d=(x-p.x)**2+(y-p.y)**2;if(d<score&&safe(x,y)){score=d;best={x,y};}}
  return best;
 }
 function desired(){const b=bounds(),{w,h}=size();return preferred?{x:b.left+preferred.rx*Math.max(0,b.right-b.left-w),y:b.top+preferred.ry*Math.max(0,b.bottom-b.top-h)}:{x:b.right-w,y:b.bottom-h};}
 function resolve(wanted=null){
  if(gesture)return;portal();dock.classList.remove('is-stowed','is-compact','is-yielding');actor.tabIndex=0;
  const point=wanted||desired(),rects=obstacles();let free=freePosition(point,rects);
  if(!free){dock.classList.add('is-compact');free=freePosition(wanted||desired(),rects);}
  if(free)paint(free);else{paint(point);dock.classList.add('is-stowed');actor.tabIndex=-1;}
 }
 function persist(){const b=bounds(),{w,h}=size();preferred={rx:Number(clamp((position.x-b.left)/Math.max(1,b.right-b.left-w),0,1).toFixed(4)),ry:Number(clamp((position.y-b.top)/Math.max(1,b.bottom-b.top-h),0,1).toFixed(4))};try{localStorage.setItem(key,JSON.stringify(preferred));}catch{}}
 function reset(){preferred=null;try{localStorage.removeItem(key);}catch{}resolve();}
 function side(){const b=bounds(),{w}=size();resolve({x:position.x<(b.left+b.right-w)/2?b.right-w:b.left,y:position.y});if(!dock.classList.contains('is-stowed'))persist();}
 function schedule(yielding=false){
  if(gesture)return;if(yielding)dock.classList.add('is-yielding');clearTimeout(layoutTimer);layoutTimer=setTimeout(()=>resolve(),100);
 }
 function finish(event,cancelled=false){
  if(!gesture||event.pointerId!==gesture.id)return;
  const g=gesture;gesture=null;if(frame){cancelAnimationFrame(frame);frame=0;}
  dock.classList.remove('is-dragging');dock.style.removeProperty('--ncleanek-tilt');
  if(g.moved){
   suppressClickUntil=performance.now()+600;
   if(cancelled)resolve(g.origin);
   else{const b=bounds(),{w}=size();let p={...position};if(p.x-b.left<28)p.x=b.left;if(b.right-(p.x+w)<28)p.x=b.right-w;resolve(p);if(!dock.classList.contains('is-stowed'))persist();dock.classList.add('is-landing');clearTimeout(landingTimer);landingTimer=setTimeout(()=>dock.classList.remove('is-landing'),280);}
  }else if(cancelled)suppressClickUntil=performance.now()+600;
  if(actor.hasPointerCapture?.(g.id))actor.releasePointerCapture(g.id);
 }
 actor.addEventListener('pointerdown',event=>{
  if(event.button!==0||event.isPrimary===false||gesture)return;
  suppressClickUntil=0;
  gesture={id:event.pointerId,startX:event.clientX,startY:event.clientY,lastX:event.clientX,lastY:event.clientY,origin:{...position},moved:false,x:event.clientX,y:event.clientY,tilt:0};
  actor.setPointerCapture?.(event.pointerId);
 });
 actor.addEventListener('pointermove',event=>{
  const g=gesture;if(!g||g.id!==event.pointerId)return;
  if(!g.moved&&Math.hypot(event.clientX-g.startX,event.clientY-g.startY)<8)return;
  event.preventDefault();
  if(!g.moved){g.moved=true;document.dispatchEvent(new Event('ncleanek:dragstart'));dock.classList.remove('is-landing','is-waving');dock.classList.add('is-dragging');}
  g.tilt=clamp(g.tilt*.55+(event.clientX-g.lastX)*.45,-18,18);g.lastX=event.clientX;g.lastY=event.clientY;g.x=event.clientX;g.y=event.clientY;
  if(!frame)frame=requestAnimationFrame(()=>{frame=0;if(gesture!==g)return;const {w,h}=size();paint({x:g.x-w*.75,y:g.y-h*.09});dock.style.setProperty('--ncleanek-tilt',g.tilt.toFixed(1)+'deg');});
 });
 actor.addEventListener('pointerup',event=>{
  if(!gesture||gesture.id!==event.pointerId)return;
  const tap=gesture?.id===event.pointerId&&!gesture.moved&&['touch','pen'].includes(event.pointerType);
  if(gesture?.moved){const {w,h}=size();paint({x:event.clientX-w*.75,y:event.clientY-h*.09});}
  finish(event);
  if(tap){suppressClickUntil=performance.now()+600;document.dispatchEvent(new Event('ncleanek:activate'));}
 });
 actor.addEventListener('pointercancel',event=>finish(event,true));
 actor.addEventListener('lostpointercapture',event=>finish(event,true));
 actor.addEventListener('click',event=>{if(event.detail>0&&performance.now()<suppressClickUntil){suppressClickUntil=0;event.preventDefault();event.stopImmediatePropagation();}},true);
 actor.addEventListener('contextmenu',event=>{event.preventDefault();if(!gesture?.moved)document.dispatchEvent(new Event('ncleanek:options'));});
 actor.addEventListener('dragstart',event=>event.preventDefault());
 actor.addEventListener('keydown',event=>{
  const delta={ArrowLeft:[-24,0],ArrowRight:[24,0],ArrowUp:[0,-24],ArrowDown:[0,24]}[event.key];
  if(event.key==='Escape'&&gesture){event.preventDefault();event.stopPropagation();finish({pointerId:gesture.id},true);return;}
  if(event.key==='Home'){event.preventDefault();reset();return;}
  if(!delta)return;event.preventDefault();event.stopPropagation();const factor=event.shiftKey?2:1;resolve({x:position.x+delta[0]*factor,y:position.y+delta[1]*factor});if(!dock.classList.contains('is-stowed'))persist();
 });
 document.addEventListener('click',event=>{if(event.target.closest('[data-ncleanek-position-reset]'))reset();if(event.target.closest('[data-ncleanek-position-side]'))side();});
 document.addEventListener('ncleanek:position-reset',reset);document.addEventListener('ncleanek:position-side',side);
 document.addEventListener('nclean:committed',()=>{clearTimeout(waveTimer);dock.classList.remove('is-waving');requestAnimationFrame(()=>dock.classList.add('is-waving'));waveTimer=setTimeout(()=>dock.classList.remove('is-waving'),550);});
 document.addEventListener('visibilitychange',()=>{if(document.hidden&&gesture)finish({pointerId:gesture.id},true);if(!document.hidden)schedule();});
 addEventListener('blur',()=>{if(gesture)finish({pointerId:gesture.id},true);});
 addEventListener('resize',()=>schedule());window.visualViewport?.addEventListener('resize',()=>schedule(true));window.visualViewport?.addEventListener('scroll',()=>schedule(true));
 document.addEventListener('scroll',event=>{if(!dock.contains(event.target))schedule(true);},true);
 new MutationObserver(records=>{if(records.some(r=>!dock.contains(r.target))){if(!gesture&&records.some(r=>r.type==='attributes'&&r.attributeName==='open'))portal();schedule();}}).observe(document.body,{childList:true,subtree:true,attributes:true,attributeFilter:['open','hidden']});
 addEventListener('storage',event=>{if(event.key!==key)return;try{const p=JSON.parse(event.newValue);preferred=valid(p)?p:null;schedule();}catch{}});
 resolve();
})();
