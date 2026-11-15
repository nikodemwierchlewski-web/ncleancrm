/* Server-calculated values; chart drawing never recalculates business costs. */
(() => {
 'use strict';
 const chart=document.querySelector('[data-finance-chart]');if(!chart)return;
 const select=chart.querySelector('[data-chart-select]'),svg=chart.querySelector('svg'),caption=chart.querySelector('[data-chart-value]');
 const points=[...select.options].map(o=>({date:o.dataset.date,revenue:Number(o.dataset.revenue),profit:Number(o.dataset.profit)}));
 if(!points.length)return;
 const format=new Intl.NumberFormat('pl-PL',{style:'currency',currency:'PLN'});
 if(points.every(p=>p.revenue===0&&p.profit===0)){chart.classList.add('is-empty');const empty=document.createElement('p');empty.className='chart-empty';empty.textContent='Brak wykonanych usług lub zapisanych kosztów w tym okresie.';svg.before(empty);}
 const NS='http://www.w3.org/2000/svg';const el=(tag,attrs,text)=>{const n=document.createElementNS(NS,tag);for(const [k,v]of Object.entries(attrs))n.setAttribute(k,v);if(text!=null)n.textContent=text;return n;};
 const values=points.flatMap(p=>[p.revenue,p.profit]),minimum=Math.min(0,...values),maximum=Math.max(1,...values),height=132,width=688,top=14,left=42;
 const y=value=>Math.round((top+(maximum-value)/(maximum-minimum)*height)/2)*2,zero=y(0),slot=width/points.length,bar=Math.max(3,Math.floor(slot*.34));
 svg.append(el('path',{d:`M${left} ${zero}H${left+width}`,stroke:'#829f9d','stroke-width':2}));
 for(const value of [maximum,minimum])svg.append(el('text',{x:left-6,y:y(value)+4,'text-anchor':'end',fill:'#355b68','font-size':11},new Intl.NumberFormat('pl-PL',{notation:'compact',maximumFractionDigits:1}).format(value)));
 points.forEach((p,i)=>{const g=el('g',{'data-chart-index':i}),x=left+i*slot+slot/2;
  for(const [key,offset,color]of [['revenue',-bar-1,'#197eaa'],['profit',1,p.profit<0?'#ad463e':'#357862']]){const value=p[key],end=y(value),size=value===0?0:Math.max(2,Math.abs(zero-end));const rect=el('rect',{x:Math.round(x+offset),y:Math.min(zero,end),width:bar,height:size,fill:color});rect.append(el('title',{},p.date+' · '+(key==='revenue'?'Przychód: ':'Zysk: ')+format.format(value)));g.append(rect);}
  g.append(el('rect',{x:Math.round(left+i*slot),y:top,width:slot,height,'fill':'transparent','pointer-events':'all'}));
  if(points.length<=7||i===0||i===points.length-1||(i+1)%5===0)svg.append(el('text',{x:Math.round(x),y:168,'text-anchor':'middle',fill:'#355b68','font-size':12},p.date.slice(-2)));
  svg.append(g);
 });
 const marker=el('path',{stroke:'#355b68','stroke-width':2,'stroke-dasharray':'3 4','pointer-events':'none'});svg.append(marker);
 function pick(index){if(!points[index])return;select.value=String(index);const p=points[index];caption.textContent=p.date.split('-').reverse().join('.')+' · przychód '+format.format(p.revenue)+' · zysk '+format.format(p.profit);const x=Math.round(left+index*slot+slot/2);marker.setAttribute('d',`M${x} ${top}V${top+height}`);}
 svg.addEventListener('click',e=>{const g=e.target.closest('[data-chart-index]');if(g)pick(Number(g.dataset.chartIndex));});select.addEventListener('change',()=>pick(Number(select.value)));
 const now=document.querySelector('.today-date')?.textContent.split('.').reverse().join('-');pick(Math.max(0,points.findIndex(p=>p.date===now)));
})();
