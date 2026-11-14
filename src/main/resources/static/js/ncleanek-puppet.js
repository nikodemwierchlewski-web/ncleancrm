/* Pointer interaction and presentation only. No business requests or form values. */
(() => {
 'use strict';
 const dock=document.querySelector('.ncleanek-dock'),actor=dock?.querySelector('.ncleanek-launch');
 if(!dock||!actor)return;
 const key='nclean.puppet.position.v1';
 let preferred=null,position={x:0,y:0},gesture=null,frame=0,layoutTimer=0,landingTimer=0,waveTimer=0,suppressClickUntil=0;
 const valid=p=>p&&Number.isFinite(p.rx)&&Number.isFinite(p.ry)&&p.rx>=0&&p.rx<=1&&p.ry>=0&&p.ry<=1;
 try{const p=JSON.parse(localStorage.getItem(key));if(valid(p))preferred=p;}catch{}
 if(typeof dock.showPopover==='function')dock.setAttribute('popover','manual');
 document.body.append(dock);actor.setAttribute('aria-keyshortcuts','ArrowLeft ArrowRight Home');
 const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
 function bounds(){
  const v=window.visualViewport,left=(v?.offsetLeft||0)+10,top=(v?.offsetTop||0)+10;
  const right=left+(v?.width||innerWidth)-20;
  let bottom=top+(v?.height||innerHeight)-20;
  const nav=document.querySelector('.record-nav')?.getBoundingClientRect();
  if(nav&&nav.height&&nav.top>top&&nav.bottom>=bottom-12)bottom=Math.min(bottom,nav.top-8);
  return {left,top,right,bottom};
 }
 function size(){return {w:dock.offsetWidth||44,h:dock.offsetHeight||60};}
 function constrained(p,b=bounds()){const {w,h}=size();return {x:clamp(p.x,b.left,Math.max(b.left,b.right-w)),y:Math.max(b.top,b.bottom-h)};}
 function paint(p){position=constrained(p);dock.style.left=position.x+'px';dock.style.top=position.y+'px';dock.style.right='auto';dock.style.bottom='auto';const b=bounds(),chipWidth=Math.min(230,b.right-b.left);dock.style.setProperty('--ncleanek-chip-left',(clamp(position.x+44-chipWidth,b.left,b.right-chipWidth)-position.x)+'px');}
 function portal(){
  const parent=document.querySelector('dialog[open]')||document.body;
  if(dock.parentElement!==parent)parent.append(dock);
  if(dock.showPopover&&!dock.matches(':popover-open')){try{dock.showPopover();}catch{}}
 }
 const overlaps=(a,b)=>a.left<b.right&&a.right>b.left&&a.top<b.bottom&&a.bottom>b.top;
 // Yield to typing, save actions and the help panel without moving the user's anchor.
 function yieldToContent(){
  if(gesture)return;
  const focused=document.activeElement;
  const editing=focused?.matches('input:not([type=hidden]),textarea,select,[contenteditable=true]');
  dock.classList.toggle('is-busy',Boolean(editing||document.querySelector('dialog[open],.ncleanek-panel:not([hidden])')));
  const r=dock.getBoundingClientRect();
  const protectedNodes=[...document.querySelectorAll('.booking-submit button,.review-save button,.completion-submit button,.dialog-footer button,.ncleanek-panel:not([hidden])')];
  if(editing)protectedNodes.push(focused,focused.closest('label')||focused);
  const yielding=protectedNodes.some(n=>n.getClientRects().length&&overlaps(r,n.getBoundingClientRect()));
  dock.classList.toggle('is-yielding',yielding);
  // Never leave an invisible keyboard stop over a field or a save action.
  actor.tabIndex=yielding?-1:0;
 }
 function desired(){const b=bounds(),{w,h}=size();return preferred?{x:b.left+preferred.rx*Math.max(0,b.right-b.left-w),y:b.bottom-h}:{x:b.right-w,y:b.bottom-h};}
 function resolve(wanted=null){
  if(gesture)return;
  portal();paint(wanted||desired());yieldToContent();
 }
 function persist(){const b=bounds(),{w,h}=size();preferred={rx:Number(clamp((position.x-b.left)/Math.max(1,b.right-b.left-w),0,1).toFixed(4)),ry:1};try{localStorage.setItem(key,JSON.stringify(preferred));}catch{}}
 function reset(){preferred=null;try{localStorage.removeItem(key);}catch{}resolve();}
 function side(){const b=bounds(),{w}=size();resolve({x:position.x<(b.left+b.right-w)/2?b.right-w:b.left,y:position.y});persist();}
 function schedule(){
  if(gesture)return;clearTimeout(layoutTimer);layoutTimer=setTimeout(()=>resolve(),60);
 }
 function finish(event,cancelled=false){
  if(!gesture||event.pointerId!==gesture.id)return;
  const g=gesture;gesture=null;if(frame){cancelAnimationFrame(frame);frame=0;}
  dock.classList.remove('is-dragging');dock.style.removeProperty('--ncleanek-tilt');
  if(g.moved){
   suppressClickUntil=performance.now()+600;
   if(cancelled)resolve(g.origin);
   else{persist();resolve(position);dock.classList.add('is-landing');clearTimeout(landingTimer);landingTimer=setTimeout(()=>dock.classList.remove('is-landing'),280);}
  }else if(cancelled)suppressClickUntil=performance.now()+600;
  if(actor.hasPointerCapture?.(g.id))actor.releasePointerCapture(g.id);
 }
 actor.addEventListener('pointerdown',event=>{
  if(event.button!==0||event.isPrimary===false||gesture)return;
  suppressClickUntil=0;
  clearTimeout(layoutTimer);
  gesture={id:event.pointerId,startX:event.clientX,startY:event.clientY,lastX:event.clientX,lastY:event.clientY,origin:{...position},moved:false,x:event.clientX,y:event.clientY,tilt:0};
  actor.setPointerCapture?.(event.pointerId);
 });
 actor.addEventListener('pointermove',event=>{
  const g=gesture;if(!g||g.id!==event.pointerId)return;
  if(!g.moved&&Math.hypot(event.clientX-g.startX,event.clientY-g.startY)<8)return;
  event.preventDefault();
  if(!g.moved){g.moved=true;document.dispatchEvent(new Event('ncleanek:dragstart'));dock.classList.remove('is-landing','is-waving','is-yielding');dock.classList.add('is-dragging');}
  g.tilt=clamp(g.tilt*.55+(event.clientX-g.lastX)*.45,-18,18);g.lastX=event.clientX;g.lastY=event.clientY;g.x=event.clientX;g.y=event.clientY;
  if(!frame)frame=requestAnimationFrame(()=>{frame=0;if(gesture!==g)return;paint({x:g.origin.x+g.x-g.startX,y:g.origin.y+g.y-g.startY});dock.style.setProperty('--ncleanek-tilt',g.tilt.toFixed(1)+'deg');});
 });
 actor.addEventListener('pointerup',event=>{
  if(!gesture||gesture.id!==event.pointerId)return;
  const tap=gesture?.id===event.pointerId&&!gesture.moved&&['touch','pen'].includes(event.pointerType);
  if(gesture.moved){const g=gesture;paint({x:g.origin.x+event.clientX-g.startX,y:g.origin.y+event.clientY-g.startY});}
  finish(event);
  if(tap){suppressClickUntil=performance.now()+600;document.dispatchEvent(new Event('ncleanek:activate'));}
 });
 actor.addEventListener('pointercancel',event=>finish(event,true));
 actor.addEventListener('lostpointercapture',event=>finish(event,true));
 actor.addEventListener('click',event=>{if(event.detail>0&&performance.now()<suppressClickUntil){suppressClickUntil=0;event.preventDefault();event.stopImmediatePropagation();}},true);
 actor.addEventListener('contextmenu',event=>{event.preventDefault();if(!gesture?.moved)document.dispatchEvent(new Event('ncleanek:options'));});
 actor.addEventListener('dragstart',event=>event.preventDefault());
 actor.addEventListener('keydown',event=>{
  const delta={ArrowLeft:[-24,0],ArrowRight:[24,0]}[event.key];
  if(event.key==='Escape'&&gesture){event.preventDefault();event.stopPropagation();finish({pointerId:gesture.id},true);return;}
  if(event.key==='Home'){event.preventDefault();reset();return;}
  if(!delta)return;event.preventDefault();event.stopPropagation();const factor=event.shiftKey?2:1;resolve({x:position.x+delta[0]*factor,y:position.y+delta[1]*factor});persist();
 });
 document.addEventListener('click',event=>{if(event.target.closest('[data-ncleanek-position-reset]'))reset();if(event.target.closest('[data-ncleanek-position-side]'))side();});
 document.addEventListener('ncleanek:position-reset',reset);document.addEventListener('ncleanek:position-side',side);
 document.addEventListener('nclean:committed',()=>{clearTimeout(waveTimer);dock.classList.remove('is-waving');requestAnimationFrame(()=>dock.classList.add('is-waving'));waveTimer=setTimeout(()=>dock.classList.remove('is-waving'),550);});
 document.addEventListener('visibilitychange',()=>{if(document.hidden&&gesture)finish({pointerId:gesture.id},true);if(!document.hidden)schedule();});
 addEventListener('blur',()=>{if(gesture)finish({pointerId:gesture.id},true);});
 addEventListener('resize',()=>schedule());window.visualViewport?.addEventListener('resize',schedule);window.visualViewport?.addEventListener('scroll',schedule);
 document.addEventListener('scroll',yieldToContent,true);
 document.addEventListener('ncleanek:reminder-layout',yieldToContent);
 document.addEventListener('focusin',yieldToContent);document.addEventListener('focusout',()=>queueMicrotask(yieldToContent));
 new MutationObserver(records=>{if(gesture||!records.some(r=>!dock.contains(r.target)))return;if(records.some(r=>r.type==='attributes'&&r.attributeName==='open')){portal();paint(position);}yieldToContent();}).observe(document.body,{childList:true,subtree:true,attributes:true,attributeFilter:['open','hidden']});
 addEventListener('storage',event=>{if(event.key!==key)return;try{const p=JSON.parse(event.newValue);preferred=valid(p)?p:null;schedule();}catch{}});
 resolve();
})();
