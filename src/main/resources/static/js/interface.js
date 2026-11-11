(() => {
  const openHash=()=>{const target=document.querySelector(location.hash||'#nonexistent');if(target?.tagName==='DETAILS')target.open=true;};
  try{openHash();addEventListener('hashchange',openHash);}catch{}
  // Keep error-bearing disclosures visible without adding another screen.
  document.querySelectorAll('details').forEach(detail=>{if(detail.querySelector('[role=alert]'))detail.open=true;});
})();
