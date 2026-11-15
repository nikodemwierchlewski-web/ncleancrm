/* Private drafts exist only in this page's memory. Only an explicit save writes the database. */
(() => {
 'use strict';
 const dirty=new Set();
 const changed=form=>{if(form&&!['/login','/logout'].includes(Nclean.path(Nclean.formTarget(form).url)))dirty.add(form);};
 document.addEventListener('input',e=>changed(e.target.form));
 document.addEventListener('change',e=>changed(e.target.form));
 const saved=form=>{dirty.delete(form);form?.dispatchEvent(new Event('nclean:form-saved',{bubbles:true}));};
 window.NcleanDrafts=Object.freeze({changed,isDirty:form=>dirty.has(form),saved,hasDirty:()=>dirty.size>0});
 window.NcleanEditorDrafts=class {
  constructor(root,extra=()=>null){this.root=root;this.extra=extra;this.key=null;this.cache=new Map();}
  async load(key,loader,reload){
   const current=this.root.querySelector('form');
   if(key===this.key&&dirty.has(current)){this.banner(reload);return {restored:true,extra:this.extra()};}
   const cached=this.cache.get(key),next=cached||await loader();
   if(current&&dirty.has(current))this.cache.set(this.key,{nodes:[...this.root.childNodes],extra:this.extra()});
   this.root.replaceChildren(...next.nodes);this.key=key;this.cache.delete(key);
   if(cached)this.banner(reload);
   return {restored:!!cached,extra:next.extra??null};
  }
  banner(reload){
   this.root.querySelector('[data-editor-draft]')?.remove();
   const notice=document.createElement('div');notice.className='notice editor-draft';notice.dataset.editorDraft='';notice.setAttribute('role','status');
   const text=document.createElement('p');text.textContent='Przywrócono szkic z tej karty. Zapisz, aby trafił do bazy.';
   const discard=document.createElement('button');discard.type='button';discard.className='text-button';discard.textContent='Odrzuć szkic';
   discard.addEventListener('click',()=>{const form=this.root.querySelector('form');if(form?.getAttribute('aria-busy')==='true')return;dirty.delete(form);form?.dispatchEvent(new Event('nclean:draft-discarded',{bubbles:true}));this.cache.delete(this.key);reload();});notice.append(text,discard);this.root.querySelector('form').prepend(notice);
  }
  saved(form){saved(form);if(form===this.root.querySelector('form')){this.cache.delete(this.key);this.root.querySelector('[data-editor-draft]')?.remove();}}
 };
})();
