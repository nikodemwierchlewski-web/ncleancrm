/* HTML DEMO transport only. The actual CRM uses authenticated Spring endpoints. */
(() => {
 const waiting=new Map();let sequence=0;
 function query(path,fields){const id=++sequence;return new Promise(resolve=>{waiting.set(id,resolve);parent.postMessage({preview:true,kind:'workspace-query',data:{id,path,fields}},'*');});}
 addEventListener('message',e=>{if(e.source!==parent||!e.data?.workspaceReply)return;const resolve=waiting.get(e.data.id);if(resolve){waiting.delete(e.data.id);resolve(e.data);}});
 const previous=window.fetch;
 window.fetch=async(url,options={})=>{const u=new URL(String(url),'https://preview.invalid');if(u.pathname==='/notes/reminders'||/^\/notes\/reminders\/\d+$/.test(u.pathname)){
  const result=await query(u.pathname,options.body?[...options.body]:null);return new Response(JSON.stringify(result.data),{status:result.status||200,headers:{'content-type':'application/json'}});
 }if(!options.method&&u.pathname==='/today'){const result=await query('/preview/today-html',null);return new Response(result.data,{headers:{'content-type':'text/html'}});}return previous(url,options);};
 document.addEventListener('submit',async e=>{const f=e.target;if(!f.matches('#fuel-purchase-form,.preview-fuel-action'))return;e.preventDefault();if(f.dataset.saving)return;f.dataset.saving='true';const body=[...new FormData(f)].filter(([k,v])=>k!=='_csrf'&&typeof v==='string');f.querySelectorAll('button').forEach(b=>b.disabled=true);let error=f.querySelector('.form-error');if(!error){error=document.createElement('p');error.className='form-error';error.setAttribute('role','alert');f.append(error);}
  try{const result=await query(f.matches('.preview-fuel-action')?'/preview/fuel-action':'/preview/fuel-save',body);if(result.status>=400)throw Error(result.data.message);window.previewNavigate(result.data.url);}catch(err){error.textContent=err.message;}finally{delete f.dataset.saving;f.querySelectorAll('button').forEach(b=>b.disabled=false);}
 });
})();
