/* The real CRM renders these filter indicators on the server; this is only DEMO transport. */
{
 const original=window.costPreview.render;
 window.costPreview.render=request=>{const u=new URL(request,'https://preview.invalid'),normalized=new URL(u);if(normalized.pathname==='/costs'&&normalized.searchParams.get('view')==='overview')normalized.searchParams.delete('view');const raw=original(normalized.pathname+normalized.search);if(raw==null)return raw;const d=new DOMParser().parseFromString(raw,'text/html'),q=u.searchParams.get('q')||'',cancelled=u.searchParams.get('cancelled')==='true',panel=d.querySelector('.cost-filter-disclosure');
  if(panel){panel.open=false;const summary=panel.querySelector('summary');summary.textContent='Filtry kosztów'+(q||cancelled?' · aktywne':'');d.querySelector('.active-cost-filters')?.remove();panel.querySelector('.text-link')?.remove();if(q||cancelled){const p=d.createElement('p');p.className='active-cost-filters field-help';p.textContent='Filtry: '+(q?'„'+q+'”':'')+(cancelled?' · z anulowanymi':'');panel.after(p);const clear=d.createElement('a');clear.className='text-link';clear.textContent='Wyczyść wyszukiwanie i anulowane';const clean=new URL(request,'https://preview.invalid');clean.searchParams.delete('q');clean.searchParams.delete('cancelled');clear.href=clean.pathname+clean.search;panel.append(clear);}}
  for(const a of d.querySelectorAll('.cost-tabs a,.cost-period-links a')){const target=new URL(a.getAttribute('href'),'https://preview.invalid');if(q)target.searchParams.set('q',q);if(cancelled)target.searchParams.set('cancelled','true');a.setAttribute('href',target.pathname+target.search);}
  return d.documentElement.outerHTML;
 };
}
