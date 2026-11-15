addEventListener('message',event=>{
 if(event.source!==frame.contentWindow||event.data?.kind!=='draft-cleared')return;
 if(workingDrafts.get(current)?.id===event.data.data.id)workingDrafts.delete(current);
});
