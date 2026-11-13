/* Shared, optional presentation effects. Business operations never wait for animation. */
(() => {
  'use strict';
  const root=document.documentElement,media=matchMedia('(prefers-reduced-motion: reduce)');
  const running=new Set(),channels=new WeakMap(),progressValues=new Map();
  const allowed=new Set(['booked','updated','completed','day','step']);
  let feedback=null,feedbackTimer=null,pendingScene=null;
  const off=()=>media.matches||document.hidden||root.classList.contains('tycoon-motion-off')||root.dataset.uiTheme==='classic';
  const get=key=>{try{return JSON.parse(sessionStorage.getItem(key));}catch{return null;}};
  const put=(key,value)=>{try{sessionStorage.setItem(key,JSON.stringify(value));}catch{}};
  const take=key=>{const value=get(key);try{sessionStorage.removeItem(key);}catch{}return value&&Date.now()-value.at<10000?value:null;};

  function animate(node,frames,duration=180,channel='effect') {
    if(!node)return;
    let map=channels.get(node);if(!map){map=new Map();channels.set(node,map);}
    map.get(channel)?.cancel();
    if(off()||!node.animate)return;
    const effect=node.animate(frames,{duration,easing:'ease-out'});
    map.set(channel,effect);running.add(effect);
    const cleanup=()=>{running.delete(effect);if(map.get(channel)===effect)map.delete(channel);};
    effect.addEventListener('finish',cleanup,{once:true});effect.addEventListener('cancel',cleanup,{once:true});
    return effect;
  }
  const pulse=node=>animate(node,[{boxShadow:'inset 0 0 0 2px #1689b8',backgroundColor:'#d9f0ef'},{boxShadow:'inset 0 0 0 0 transparent'}],220);
  function removeFeedback(){clearTimeout(feedbackTimer);feedbackTimer=null;feedback?.remove();feedback=null;}
  function suspend(){if(off()){for(const effect of running)effect.cancel();pendingScene=null;removeFeedback();}}
  function playScene(svg,kind){
    if(!svg?.isConnected||off()||svg.classList.contains('pixel-paused'))return;
    const worker=svg.querySelector('.pixel-worker');
    if(kind==='travel')animate(svg.querySelector('.pixel-van'),[{transform:'translate(0,0)'},{transform:'translate(7px,-3px)',offset:.45},{transform:'translate(0,0)'}],500,'scene');
    if(kind==='prepare')animate(worker,[{transform:'translateY(0)'},{transform:'translateY(-3px)',offset:.45},{transform:'translateY(0)'}],350,'scene');
    if(kind==='completed'){
      animate(worker,[{transform:'translateX(0)'},{transform:'translateX(-5px)',offset:.35},{transform:'translateX(2px)',offset:.7},{transform:'translateX(0)'}],650,'scene');
      animate(svg.querySelector('.pixel-nozzle'),[{transform:'translateX(0)'},{transform:'translateX(-7px)',offset:.4},{transform:'translateX(0)'}],650,'scene');
      animate(svg.querySelector('.pixel-bubbles'),[{opacity:0,transform:'translateY(3px)'},{opacity:1,offset:.45},{opacity:0,transform:'translateY(-9px)'}],650,'scene');
    }
    if(kind==='booked')animate(svg.querySelector('.pixel-booked'),[{opacity:0,transform:'translateY(4px)'},{opacity:1,transform:'translateY(0)',offset:.25},{opacity:1,offset:.7},{opacity:0,transform:'translateY(-3px)'}],650,'scene');
    if(kind==='day'||kind==='completed')animate(svg.querySelector('.pixel-sparkle'),[{opacity:.2},{opacity:1,offset:.5},{opacity:.2}],650,'scene');
  }
  function scene(kind,announce=false){
    if(off())return;
    const svg=[...document.querySelectorAll('svg.tycoon-world')].find(s=>!s.closest('.tycoon-action-feedback')&&!s.classList.contains('pixel-paused'));
    if(svg){playScene(svg,kind);return;}
    if(document.querySelector('.tycoon-base:not(.tycoon-action-feedback) img')){pendingScene=kind;return;}
    if(!announce||document.querySelector('dialog[open]'))return;
    pendingScene=kind;removeFeedback();
    feedback=document.createElement('aside');feedback.className='tycoon-action-feedback tycoon-base';feedback.setAttribute('role','status');
    const text=document.createElement('strong');text.textContent=kind==='day'?'Dzień sprawdzony ✓':kind==='completed'?'Zapisano wykonanie ✓':'Zapisano zlecenie ✓';
    const image=document.createElement('img');image.src=window.NcleanMotion.artwork||'/pixel/base.svg';image.alt='';image.width=140;image.height=62;
    feedback.append(text,image);document.body.append(feedback);window.NcleanMotion.hydrate?.(image);
    feedbackTimer=setTimeout(removeFeedback,2200);
  }
  function apply(signal,source){
    if(!allowed.has(signal?.kind))return;
    if(signal.kind==='step'){const id=Number(signal.entry),card=Number.isSafeInteger(id)?document.querySelector('[data-note-id=\"'+id+'\"]'):null;const input=card&&[...card.querySelectorAll('input[name=step]')].find(i=>i.value===signal.id);pulse(input?.closest('label,li,form')||card||source);return;}
    const id=Number(signal.id);
    if(Number.isSafeInteger(id)&&id>0){for(const card of document.querySelectorAll('[data-order="'+id+'"]'))pulse(card);}
    if(signal.kind==='updated'){pulse(source||document.querySelector('#agenda-orders,main h1'));return;}
    scene(signal.kind,true);
  }
  function confirmed(signal,carry=false,source){
    // Decoration must never turn a successful save into a client-side error.
    try{if(!allowed.has(signal?.kind))return;if(carry){const id=Number(signal.id);put('nclean.motion.saved',{kind:signal.kind,id:Number.isSafeInteger(id)?id:0,at:Date.now()});}else apply(signal,source);}catch{}
  }
  function progress(node,key){
    if(!node||!node.max)return;
    const value=Math.max(0,Math.min(1,node.value/node.max)),old=progressValues.get(key)??value;
    progressValues.set(key,value);if(progressValues.size>20)progressValues.delete(progressValues.keys().next().value);
    const track=document.createElement('span'),fill=document.createElement('i');track.className='tycoon-progress-track';track.setAttribute('aria-hidden','true');fill.style.transform='scaleX('+value+')';track.append(fill);node.classList.add('tycoon-native-progress');node.after(track);
    if(old!==value)animate(fill,[{transform:'scaleX('+old+')'},{transform:'scaleX('+value+')'}],200,'progress');
  }
  window.NcleanMotion={confirmed,progress,suspend,pause:svg=>{for(const effect of running)if(svg.contains(effect.effect?.target))effect.cancel();},world:svg=>{if(pendingScene){const kind=pendingScene;pendingScene=null;playScene(svg,kind);}},activeCount:()=>running.size};

  document.addEventListener('crm:day',event=>{
    const {day,previous}=event.detail;if(day===previous)return;
    pulse(document.querySelector('[data-calendar-day="'+day+'"]'));
    const direction=day>previous?1:-1;
    animate(document.querySelector('#agenda-orders'),[{opacity:.35,transform:'translateX('+direction*6+'px)'},{opacity:1,transform:'translateX(0)'}],160,'day');
    scene('travel');
  });
  document.addEventListener('click',event=>{
    const button=event.target.closest('button,.button');
    if(button&&!button.disabled&&!button.closest('dialog:not([open])'))animate(button,[{transform:'translateY(2px)'},{transform:'translateY(0)'}],100,'press');
    const card=event.target.closest('.booking-chip,.equipment-card,.settings-links .card');if(card)pulse(card);
    const category=event.target.closest('[data-category-choice]');if(category)animate(document.querySelector('.service-picker,.service-list'),[{opacity:.5},{opacity:1}],150,'category');
    const month=event.target.closest('.month-navigation a');
    if(month){const target=new URL(month.href,location.href).searchParams.get('month');put('nclean.motion.month',{target,direction:month.getAttribute('aria-label')?.includes('Poprzedni')?-1:1,at:Date.now()});}
    const nav=event.target.closest('.record-nav a,.period-tabs a,.mode-tabs a,.notes-tabs a,.cost-tabs a');
    if(nav){const container=nav.parentElement,items=[...container.querySelectorAll('a')];put('nclean.motion.nav',{type:container.className.replace(' tycoon-marked-nav',''),index:items.findIndex(a=>a.matches('[aria-current=page],.active,.selected')),at:Date.now()});}
    queueMicrotask(()=>document.querySelectorAll('.category-tabs').forEach(updateMarker));
  });
  const initialTotal=document.querySelector('#booking-total');if(initialTotal)initialTotal.dataset.lastTotal=initialTotal.textContent;
  const quantities=new Map();document.querySelectorAll('.service-line input[name^=qty_]').forEach(input=>quantities.set(input.name,Number(input.value||0)));
  document.addEventListener('booking:change',()=>{
    let changed=false,added=false;
    for(const input of document.querySelectorAll('.service-line input[name^=qty_]')){const value=Number(input.value||0),old=quantities.get(input.name)||0;if(value!==old){changed=true;added ||= value>old;pulse(input.closest('.service-line'));quantities.set(input.name,value);}}
    if(changed){pulse(document.querySelector('#booking-selected-count'));scene(added?'prepare':'travel');}
    const amount=document.querySelector('#booking-total');if(amount&&amount.dataset.lastTotal!==amount.textContent){if(amount.dataset.lastTotal)pulse(amount);amount.dataset.lastTotal=amount.textContent;}
  },true);

  const markerState=new WeakMap();let previousNav=null;
  const resize=typeof ResizeObserver==='function'?new ResizeObserver(entries=>entries.forEach(e=>updateMarker(e.target))):null;
  function updateMarker(container){
    const target=container.querySelector('[aria-current=page],[aria-pressed=true],.active,.selected');if(!target)return;
    let state=markerState.get(container);
    if(!state){const marker=document.createElement('i');marker.className='tycoon-nav-marker';marker.setAttribute('aria-hidden','true');container.append(marker);container.classList.add('tycoon-marked-nav');state={marker,left:0,top:0,width:0};markerState.set(container,state);resize?.observe(container);}
    const rect=target.getBoundingClientRect(),base=container.getBoundingClientRect(),left=rect.left-base.left+container.scrollLeft;
    if(rect.width===0)return;
    let from=state.left;
    if(!state.width&&previousNav?.type===container.className.replace(' tycoon-marked-nav','')){const item=container.querySelectorAll('a')[previousNav.index];if(item)from=item.getBoundingClientRect().left-base.left;}
    const top=rect.bottom-base.top+container.scrollTop-3;
    state.marker.style.width=rect.width+'px';state.marker.style.transform='translate('+left+'px,'+top+'px)';
    if(from!==left||state.top!==top)animate(state.marker,[{transform:'translate('+from+'px,'+(state.width?state.top:top)+'px)'},{transform:'translate('+left+'px,'+top+'px)'}],180,'marker');
    state.left=left;state.top=top;state.width=rect.width;
  }
  document.addEventListener('submit',event=>{const form=event.target;if(!event.defaultPrevented&&form.method?.toLowerCase()==='post'){form.dataset.motionNativePending='true';form.setAttribute('aria-busy','true');}});
  const observer=new MutationObserver(records=>{
    for(const record of records){const node=record.target;if(record.attributeName==='open'&&node.open)removeFeedback();}
  });
  observer.observe(document.body,{subtree:true,attributes:true,attributeFilter:['open']});
  media.addEventListener('change',suspend);document.addEventListener('visibilitychange',suspend);
  addEventListener('pageshow',event=>{if(event.persisted)document.querySelectorAll('form[data-motion-native-pending]').forEach(form=>{delete form.dataset.motionNativePending;form.removeAttribute('aria-busy');});});
  addEventListener('pagehide',()=>{suspend();for(const effect of running)effect.cancel();removeFeedback();});
  document.addEventListener('DOMContentLoaded',()=>{
    previousNav=take('nclean.motion.nav');
    document.querySelectorAll('.record-nav,.category-tabs,.period-tabs,.mode-tabs,.notes-tabs,.cost-tabs').forEach(updateMarker);
    const month=take('nclean.motion.month');if(month&&month.target===document.querySelector('.daily')?.dataset.month)animate(document.querySelector('.calendar-workspace'),[{opacity:.5,transform:'translateX('+month.direction*8+'px)'},{opacity:1,transform:'translateX(0)'}],180,'month');
    const flash=document.querySelector('meta[name=crm-confirmed]'),saved=take('nclean.motion.saved');
    if(flash)apply({kind:flash.dataset.kind,id:flash.dataset.order});else if(saved)apply(saved);
  });
})();
