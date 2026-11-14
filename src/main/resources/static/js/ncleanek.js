(() => {
 'use strict';
 const launch=document.querySelector('.ncleanek-launch'),catalog=window.NcleanekContent;
 if(!launch||!catalog)return;
 const root=document.documentElement,key='nclean.guide.preferences.v1';
 let prefs={hidden:false,seen:[],muted:''};
 try{const p=JSON.parse(localStorage.getItem(key));if(p){prefs.hidden=p.hidden===true;prefs.seen=Array.isArray(p.seen)?p.seen.filter(v=>Object.hasOwn(catalog.tours,v)).slice(0,25):[];prefs.muted=/^\d{4}-\d{2}-\d{2}$/.test(p.muted)?p.muted:'';}}catch{}
 const el=(tag,text,cls)=>{const n=document.createElement(tag);if(text!=null)n.textContent=text;if(cls)n.className=cls;return n;};
 const button=(text,action)=>{const n=el('button',text,'button secondary');n.type='button';n.addEventListener('click',action);return n;};
 const panel=el('section',null,'ncleanek-panel');panel.id='ncleanek-panel';panel.hidden=true;panel.setAttribute('role','dialog');panel.setAttribute('aria-labelledby','ncleanek-title');
 if(typeof panel.showPopover==='function')panel.setAttribute('popover','manual');
 const header=el('div',null,'ncleanek-panel-heading'),portrait=el('img');portrait.src='/pixel/ncleanek.svg';portrait.width=32;portrait.height=40;portrait.alt='';
 const options=button('Opcje',()=>home(opener||launch));options.classList.add('ncleanek-options');
 const close=button('×',()=>hide());close.setAttribute('aria-label','Zamknij pomoc ncLEANKA');header.append(portrait,el('strong','ncLEANEK'),options,close);
 const title=el('h2');title.id='ncleanek-title';title.tabIndex=-1;const content=el('div');panel.append(header,title,content);document.body.append(panel);
 const status=el('span',null,'sr-only');status.setAttribute('role','status');launch.append(status);
 const reminderChip=el('div',null,'ncleanek-reminder-chip');reminderChip.setAttribute('role','group');reminderChip.hidden=true;reminderChip.setAttribute('aria-label','Przypomnienie ncLEANKA');
 const reminderLink=el('a'),reminderHeading=el('strong','Dokończ dzień'),reminderDetail=el('span');reminderLink.append(reminderHeading,reminderDetail);
 const postpone=button('×',()=>snoozeReminder());postpone.setAttribute('aria-label','Odłóż przypomnienie na 20 minut');postpone.title='Później — za 20 minut';
 reminderChip.append(reminderLink,postpone);launch.closest('.ncleanek-dock')?.append(reminderChip);
 function snoozeReminder(){snooze=Date.now()+20*60*1000;try{sessionStorage.setItem('nclean.guide.snooze-until',String(snooze));}catch{}badge();}

 let restoring=false;
 let mode='home',opener=null,target=null,pinned=false,hoverTimer=null,snooze=0,data=null,sequence=0,requestTimer=null,cheerTimer=null,tour=[],step=0,tourKey='';
 try{snooze=Number(sessionStorage.getItem('nclean.guide.snooze-until'))||0;}catch{}
 const today=()=>data?.today||new Intl.DateTimeFormat('en-CA',{timeZone:'Europe/Warsaw'}).format(new Date());
 const savePrefs=()=>{try{localStorage.setItem(key,JSON.stringify(prefs));}catch{}preferences();};
 function preferences(){root.classList.toggle('ncleanek-off',prefs.hidden);document.querySelectorAll('[data-ncleanek-enabled]').forEach(n=>n.checked=!prefs.hidden);badge();}
 function badge(){const pending=Boolean(data?.reminder)&&prefs.muted!==today()&&Date.now()>=snooze&&!prefs.hidden;reminderChip.hidden=!pending;if(pending){reminderLink.href=/^\/day-closeout\?/.test(data.reminder.url)?data.reminder.url:'/day-closeout';reminderLink.title=data.reminder.message;reminderDetail.textContent=data.reminder.missing>0?'Do uzupełnienia: '+data.reminder.missing+' · '+data.reminder.date:'Dane uzupełnione · zamknij dzień';}document.dispatchEvent(new Event('ncleanek:reminder-layout'));launch.querySelector('.ncleanek-dot').hidden=!pending;launch.setAttribute('aria-label',pending?'ncLEANEK — pomoc, przypomnienie o dokumentacji':'ncLEANEK — pomoc');if(!document.hidden&&!document.activeElement?.matches('input,textarea,select')&&!document.querySelector('dialog[open]')){const text=pending?'ncLEANEK: jest przypomnienie o dokumentacji.':'';if(status.textContent!==text)status.textContent=text;}}
 function clearTarget(){target?.classList.remove('ncleanek-target');target=null;}
 function hide(restore=true){clearTimeout(hoverTimer);clearTarget();if(panel.hidePopover&&panel.matches(':popover-open'))panel.hidePopover();panel.hidden=true;launch.setAttribute('aria-expanded','false');document.querySelectorAll('.ncleanek-help[aria-expanded=true]').forEach(n=>{n.setAttribute('aria-expanded','false');n.removeAttribute('aria-describedby');});if(restore&&opener?.isConnected&&opener.getClientRects().length){restoring=true;opener.focus({preventScroll:true});restoring=false;}mode='home';}
 function place(){
  if(panel.hidden)return;panel.style.top='';panel.style.bottom='';panel.style.maxHeight='';
  if(!target?.isConnected)return;const p=panel.getBoundingClientRect(),t=target.getBoundingClientRect();
  if(t.bottom<=p.top||t.top>=p.bottom||t.right<=p.left||t.left>=p.right)return;
  const above=Math.max(0,t.top-86),below=Math.max(0,innerHeight-98-t.bottom);
  const space=Math.max(above,below);panel.style.maxHeight=Math.min(p.height,Math.max(88,space))+'px';panel.style.bottom='auto';
  const height=panel.getBoundingClientRect().height,top=above>=below?Math.max(12,t.top-height-12):Math.min(innerHeight-height-12,t.bottom+12);panel.style.top=top+'px';const offset=panel.getBoundingClientRect().top-top;if(Math.abs(offset)>1)panel.style.top=(top-offset)+'px';
 }
 function show(heading,source,focus=true){const transient=mode==='tip'&&!pinned;panel.classList.toggle('ncleanek-hover',transient);panel.setAttribute('role',transient?'tooltip':'dialog');if(transient&&source?.matches('.ncleanek-help'))source.setAttribute('aria-describedby',panel.id);const parent=document.querySelector('dialog[open]')||document.body;if(panel.parentElement!==parent)parent.append(panel);title.textContent=heading;panel.hidden=false;if(panel.showPopover){try{panel.showPopover();}catch{}}launch.setAttribute('aria-expanded','true');if(source)opener=source;if(focus)title.focus({preventScroll:true});place();}
 const visible=selector=>[...document.querySelectorAll(selector)].find(n=>n.getClientRects().length&&!n.closest('[hidden],.ncleanek-panel'));
 function screen(){if(document.querySelector('#booking-editor[open]'))return 'booking';if(document.querySelector('#cost-editor[open]'))return 'costForm';if(document.querySelector('#note-editor[open]'))return 'noteForm';const path=new URL(window.previewRoute||location.href,'https://preview.invalid').pathname;
  if(path.startsWith('/work/'))return 'work';if(path==='/day-closeout')return 'closeout';if(path==='/settings/areas')return 'areas';if(path==='/settings/services')return 'services';if(/^\/customers\/\d+$/.test(path))return 'customer';return ({'/calendar':'calendar','/today':'today','/customers':'customers','/dashboard':'dashboard','/costs':'costs','/fuel':'fuel','/notes':'notes','/settings':'settings','/settings/costs':'costs','/more':'more','/search':'search','/callbacks':'callbacks','/orders':'booking'})[path]||'fallback';}
 function home(source=launch){hide(false);mode='home';pinned=true;content.replaceChildren();show('Pomogę Ci znaleźć właściwe miejsce',source);content.append(el('p','Wyjaśnię opcje i wskażę braki. Wszystkie zapisy wykonujesz samodzielnie.'));
  if(data?.focused){const p=el('p',data.focused.message);content.append(p);}
  if(data?.reminder&&prefs.muted!==today()&&Date.now()>=snooze){const box=el('div',null,'ncleanek-reminder');box.append(el('p',data.reminder.message));const link=el('a','Przejdź do dokumentacji →','button primary');link.href=/^\/day-closeout\?/.test(data.reminder.url)?data.reminder.url:'/day-closeout';box.append(link,button('Później',()=>{snoozeReminder();home(opener);}),button('Wycisz na dziś',()=>{prefs.muted=today();savePrefs();home(opener);}));content.append(box);}
  else if(!data)content.append(el('p','Przypomnienia pojawią się po odczycie danych. Samouczek działa niezależnie od połączenia.'));
  const k=screen();content.append(button(prefs.seen.includes(k)?'Przypomnij mi ten ekran':'Pokaż mi ten ekran',()=>startTour()));content.append(button('Przywróć położenie',()=>document.dispatchEvent(new Event('ncleanek:position-reset'))),button('Przenieś na drugą stronę',()=>document.dispatchEvent(new Event('ncleanek:position-side'))));content.append(button('Ukryj ncLEANKA',()=>{prefs.hidden=true;savePrefs();hide();}));content.append(el('p','Pomoc przywrócisz w Ustawienia → Wygląd i aplikacja.','field-help'));place();
 }
 function startTour(){tourKey=screen();tour=(catalog.tours[tourKey]||catalog.tours.fallback).filter(s=>visible(s[0]));step=0;mode='tour';pinned=true;renderStep();}
 function renderStep(direction=1){clearTarget();while(step>=0&&step<tour.length&&!visible(tour[step][0]))step+=direction;
  if(step<0){step=0;return home(opener);}if(step>=tour.length){if(!prefs.seen.includes(tourKey))prefs.seen.push(tourKey);savePrefs();home(opener);title.textContent='Wszystko jasne — możesz działać';return;}
  const [selector,heading,text]=tour[step];target=visible(selector);target.classList.add('ncleanek-target');target.scrollIntoView({block:'center',behavior:'instant'});content.replaceChildren(el('p',`${step+1} z ${tour.length}`,'ncleanek-step'),el('p',text));const nav=el('nav');if(step>0)nav.append(button('Wstecz',()=>{step--;renderStep(-1);}));nav.append(button(step===tour.length-1?'Gotowe':'Dalej',()=>{step++;renderStep();}),button('Zakończ samouczek',()=>hide()));content.append(nav);show(heading,null);place();
 }
 function tip(def,source,pin=false){hide(false);mode='tip';pinned=pin;target=source;content.replaceChildren(el('p',def[2]),button('Pokaż mi ten ekran',()=>startTour()));show(def[1],source,pin);source.setAttribute('aria-expanded','true');}
 function bindTips(){for(const def of catalog.tips)for(const n of document.querySelectorAll(def[0])){if(n.dataset.ncleanekBound)continue;n.dataset.ncleanekBound='true';const help=button('?',()=>{});help.className='ncleanek-help';help.setAttribute('aria-label','Pomoc: '+def[1]);help.setAttribute('aria-haspopup','dialog');help.setAttribute('aria-controls',panel.id);help.addEventListener('click',e=>{e.preventDefault();e.stopPropagation();tip(def,help,true);});help.addEventListener('focus',()=>{if(!restoring&&mode!=='tour')tip(def,help,false);});help.addEventListener('pointerenter',e=>{if(e.pointerType==='mouse'&&mode!=='tour'){clearTimeout(hoverTimer);hoverTimer=setTimeout(()=>tip(def,help,false),350);}});help.addEventListener('pointerleave',()=>{clearTimeout(hoverTimer);if(mode==='tip'&&!pinned)hoverTimer=setTimeout(()=>hide(false),180);});help.addEventListener('blur',()=>{if(mode==='tip'&&!pinned)hoverTimer=setTimeout(()=>hide(false),180);});const anchor=n.matches('input,select')?(n.closest('label')||n.parentElement):n;if(anchor.matches('button,a,summary'))anchor.after(help);else{if(anchor.tagName==='LABEL')anchor.classList.add('ncleanek-field-help');anchor.append(help);}}}
 panel.addEventListener('toggle',e=>{if(e.newState==='closed'&&!panel.hidden)hide(false);});
 panel.addEventListener('pointerenter',()=>clearTimeout(hoverTimer));panel.addEventListener('pointerleave',()=>{if(mode==='tip'&&!pinned)hide(false);});
 function currentHelp(){if(!panel.hidden){hide();return;}hide(false);opener=launch;startTour();}
 launch.addEventListener('click',currentHelp);
 document.addEventListener('ncleanek:activate',currentHelp);
 document.addEventListener('ncleanek:dragstart',()=>hide(false));
 document.addEventListener('ncleanek:options',()=>home(launch));
 document.addEventListener('keydown',e=>{if(e.key==='Escape'&&!panel.hidden){e.preventDefault();e.stopImmediatePropagation();hide();}},true);
 document.addEventListener('close',e=>{if(e.target.contains?.(panel)&&e.target!==panel)hide(false);},true);
 document.addEventListener('click',e=>{const open=e.target.closest('[data-ncleanek-open]'),reset=e.target.closest('[data-ncleanek-reset]');if(open)home(open);if(reset){prefs.seen=[];prefs.muted='';snooze=0;try{sessionStorage.removeItem('nclean.guide.snooze-until');}catch{}savePrefs();document.querySelector('[data-ncleanek-preference-status]').textContent='Propozycje samouczka przywrócono. Otwórz pomoc na wybranym ekranie.';}});
 document.addEventListener('change',e=>{if(e.target.matches('[data-ncleanek-enabled]')){prefs.hidden=!e.target.checked;savePrefs();if(prefs.hidden)hide(false);}});
 addEventListener('storage',e=>{if(e.key!==key)return;try{const p=JSON.parse(e.newValue);prefs.hidden=p?.hidden===true;prefs.muted=/^\d{4}-\d{2}-\d{2}$/.test(p?.muted)?p.muted:'';preferences();if(prefs.hidden)hide(false);}catch{}});
 async function refresh(){if(document.hidden)return;const ticket=++sequence;try{const url=new URL(window.previewRoute||location.href,'https://preview.invalid'),q=new URLSearchParams(),day=document.querySelector('.daily')?.dataset.selected||url.searchParams.get('day')||url.searchParams.get('date');if(day&&/^\d{4}-\d{2}-\d{2}$/.test(day))q.set('date',day);const order=url.pathname.match(/^\/work\/(\d+)$/)?.[1]||(url.pathname==='/day-closeout'?url.searchParams.get('order'):null);if(order)q.set('order',order);const r=await fetch('/help/context?'+q,{cache:'no-store',headers:{Accept:'application/json'}});if(!r.ok||r.redirected)throw Error();const next=await r.json();if(ticket!==sequence||typeof next.today!=='string'||!Number.isInteger(next.total))return;data=next;badge();if(!panel.hidden&&mode==='home'&&(!panel.contains(document.activeElement)||document.activeElement===title||document.activeElement===close))home(opener);}catch{/* Help never blocks a form or claims that missing data is complete. */}}
 const refreshSoon=()=>{clearTimeout(requestTimer);requestTimer=setTimeout(refresh,180);};
 document.addEventListener('nclean:committed',()=>{clearTimeout(cheerTimer);launch.classList.remove('ncleanek-cheer');requestAnimationFrame(()=>launch.classList.add('ncleanek-cheer'));cheerTimer=setTimeout(()=>launch.classList.remove('ncleanek-cheer'),400);refreshSoon();});
 const calendar=document.querySelector('.daily');if(calendar)new MutationObserver(refreshSoon).observe(calendar,{attributes:true,attributeFilter:['data-selected']});
 let bindQueued=false;new MutationObserver(()=>{if(bindQueued)return;bindQueued=true;queueMicrotask(()=>{bindQueued=false;const modal=document.querySelector('dialog[open]');if(modal&&!modal.contains(panel)&&!panel.hidden)hide(false);bindTips();});}).observe(document.body,{childList:true,subtree:true,attributes:true,attributeFilter:['open']});
 document.addEventListener('visibilitychange',()=>{if(!document.hidden)refreshSoon();});setInterval(()=>{if(!document.hidden)refresh();},120000);addEventListener('resize',place);document.addEventListener('scroll',e=>{if(!panel.contains(e.target))place();},true);preferences();bindTips();refresh();
})();
