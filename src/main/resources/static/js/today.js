/* Pixel ring shows the composition of revenue; profit stays a separate signed amount. */
(() => {
 'use strict';
 const form=document.querySelector('.finance-filters');
 if(form){const filter=form.querySelector('[data-finance-filter]'),custom=form.querySelector('[data-custom-range]');const state=()=>{custom.hidden=filter.value!=='custom';custom.querySelectorAll('input').forEach(n=>{n.disabled=custom.hidden;n.required=!custom.hidden;});};filter.addEventListener('change',state);state();}
 const chart=document.querySelector('[data-finance-chart]');if(!chart)return;
 const select=chart.querySelector('[data-chart-select]'),svg=chart.querySelector('svg'),caption=chart.querySelector('[data-chart-value]');
 const points=[...select.options].map(o=>({date:o.dataset.date,revenue:Number(o.dataset.revenue),profit:Number(o.dataset.profit)}));if(!points.length)return;
 const format=new Intl.NumberFormat('pl-PL',{style:'currency',currency:'PLN'}),total=points.reduce((n,p)=>n+p.revenue,0),colors=['#197eaa','#357862','#b87921','#7656a1','#397e93','#ad463e','#596fb3'];
 const NS='http://www.w3.org/2000/svg',el=(tag,attrs,text)=>{const n=document.createElementNS(NS,tag);for(const[k,v]of Object.entries(attrs))n.setAttribute(k,v);if(text!=null)n.textContent=text;return n;};
 const cells=[];for(let y=12;y<228;y+=8)for(let x=12;x<228;x+=8){const dx=x+4-120,dy=y+4-120,d=Math.hypot(dx,dy);if(d<70||d>108)continue;let angle=(Math.atan2(dy,dx)+Math.PI/2+Math.PI*2)%(Math.PI*2),part=angle/(Math.PI*2)*total,index=-1,cumulative=0;if(total>0){for(let i=0;i<points.length;i++){cumulative+=points[i].revenue;if(part<cumulative){index=i;break;}}}const n=el('rect',{x,y,width:7,height:7,fill:index<0?'#bfd5da':colors[index%colors.length],'data-chart-index':index});if(index>=0){n.append(el('title',{},points[index].date+' · '+format.format(points[index].revenue)));n.style.cursor='pointer';}svg.append(n);cells.push({n,index});}
 svg.append(el('text',{x:120,y:112,'text-anchor':'middle',class:'donut-label'},'PRZYCHÓD'));
 svg.append(el('text',{x:120,y:135,'text-anchor':'middle',class:'donut-total'},new Intl.NumberFormat('pl-PL',{maximumFractionDigits:2}).format(total)+' zł'));
 if(total===0){const empty=document.createElement('p');empty.className='chart-empty';empty.textContent='Brak przychodu z wykonanych usług w tym zakresie. Zapisane koszty nadal wpływają na wynik obok.';svg.after(empty);}
 function pick(index){const p=points[index];if(!p)return;select.value=String(index);caption.textContent=p.date.split('-').reverse().join('.')+' · przychód '+format.format(p.revenue)+' · wynik '+format.format(p.profit);for(const cell of cells)cell.n.setAttribute('opacity',cell.index<0||cell.index===index?1:.55);}
 svg.addEventListener('click',e=>{const g=e.target.closest('[data-chart-index]');if(g&&Number(g.dataset.chartIndex)>=0){chart.querySelector('.chart-days').open=true;pick(Number(g.dataset.chartIndex));}});select.addEventListener('change',()=>pick(Number(select.value)));pick(points.length-1);
})();
