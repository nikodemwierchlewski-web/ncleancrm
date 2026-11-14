// No dependencies: verify context paths and Cache Storage ownership in the actual scripts.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const root = path.join(__dirname, '../src/main/resources/static');
(async () => {
  const templates=path.join(__dirname,'../src/main/resources/templates');
  function walk(directory){return fs.readdirSync(directory,{withFileTypes:true}).flatMap(entry=>entry.isDirectory()?walk(path.join(directory,entry.name)):[path.join(directory,entry.name)]);}
  for(const file of walk(templates).filter(file=>file.endsWith('.html'))){
    for(const tag of fs.readFileSync(file,'utf8').match(/<[a-zA-Z][^>]*>/g)||[]){
      for(const attribute of ['href','src','action']){
        if(new RegExp('(?<![\\w:-])'+attribute+'="/(?!/)[^"]*"').test(tag))
          assert(new RegExp('\\bth:'+attribute+'=').test(tag),'Missing context-aware '+attribute+' in '+file);
      }
    }
  }
  for(const file of walk(path.join(root,'css')).filter(file=>file.endsWith('.css')))
    assert(!/url\([\x27\x22]?\//.test(fs.readFileSync(file,'utf8')),'Root CSS asset in '+file);
  for (const base of ['/', '/crm/', '/nested/crm/']) {
    const context = {URL, document:{querySelector:()=>({content:base})}, location:{origin:'https://example.test',pathname:base+'calendar'},window:{}};
    vm.runInNewContext(fs.readFileSync(path.join(root,'js/routes.js'),'utf8'),context);
    const api=context.window.Nclean;
    assert.equal(api.url('/calendar?day=2026-01-01'),base+'calendar?day=2026-01-01');
    assert.equal(api.url(base+'calendar'),base+'calendar');
    assert.equal(api.path('https://example.test'+base+'work/7'),'/work/7');
    for (const external of ['https://other.test/a','//other.test/a','tel:+48500000000','#photos']) assert.equal(api.url(external),external);
    const events={},scope='https://example.test'+base;
    const namespace='nclean-crm-offline:'+base+':';
    const names=new Set(['cms-assets','public-site-fixture-sentinel',namespace+'v2','nclean-crm-offline:/other/:v2']);
    const deleted=[],added=[];
    const worker={URL,Response,self:{registration:{scope},addEventListener:(name,callback)=>events[name]=callback,skipWaiting:async()=>{},clients:{claim:async()=>{}}},caches:{keys:async()=>[...names],delete:async key=>{deleted.push(key);names.delete(key);},open:async()=>({add:async url=>added.push(url)})}};
    vm.runInNewContext(fs.readFileSync(path.join(root,'sw.js'),'utf8'),worker);
    let pending;events.install({waitUntil:p=>pending=p});await pending;
    assert.equal(added[0],scope+'offline.html');
    events.activate({waitUntil:p=>pending=p});await pending;
    assert.deepEqual(deleted,[namespace+'v2']);assert(names.has('cms-assets'));assert(names.has('public-site-fixture-sentinel'));
    let intercepted=false;events.fetch({request:{mode:'navigate',url:'https://external.test/a'},respondWith:()=>intercepted=true});assert.equal(intercepted,false);
  }
  console.log('PASS: root/nested URL routing, external links unchanged, worker cache ownership and scope.');
})().catch(error=>{console.error(error);process.exitCode=1;});
