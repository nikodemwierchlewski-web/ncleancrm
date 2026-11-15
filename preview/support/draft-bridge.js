/* Clear the demo transport's generic draft after an explicit save/discard. */
for(const type of ['nclean:form-saved','nclean:draft-discarded'])document.addEventListener(type,event=>{
 const id=event.target.getAttribute('id');if(window.previewWorkingDraft?.id===id)window.previewWorkingDraft=null;
 parent.postMessage({preview:true,kind:'draft-cleared',data:{id}},'*');
});
