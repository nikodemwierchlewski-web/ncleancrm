import json,pathlib
from urllib.parse import urlencode

def verify_a11y(page,base,out,container):
    page.set_viewport_size({'width':390,'height':844})
    reports=[]
    for route in ['/today','/calendar','/orders','/customers','/dashboard?range=month','/notes','/equipment','/costs','/fuel','/settings/services']:
        page.goto(base+route);page.evaluate((out/'axe-4.10.3.min.js').read_text())
        result=page.evaluate("async()=>{const r=await axe.run(document,{runOnly:{type:'tag',values:['wcag2a','wcag2aa','wcag21aa']}});return {violations:r.violations.map(v=>({id:v.id,impact:v.impact,description:v.description,helpUrl:v.helpUrl,nodes:v.nodes.map(n=>({target:n.target,summary:n.failureSummary})).slice(0,8)})),passes:r.passes.length,incomplete:r.incomplete.map(v=>v.id)}}")
        reports.append({'route':route,**result})
    page.goto(base+'/customers');token=page.locator('input[name=_csrf]').first.input_value();payload='<img src=x onerror=window.auditInjected=1>'
    r=page.request.post(base+'/customers',form={'_csrf':token,'name':payload,'phone':'500009999','address':'QA','notes':payload,'customer_type':'PRIVATE','source':'OTHER'})
    assert r.ok,r.status
    page.goto(base+'/customers?'+urlencode({'query':'500009999'}));assert page.locator('main').get_by_text(payload,exact=False).count()>0
    assert not page.locator('main img[src=x]').count();assert page.evaluate('window.auditInjected===undefined')
    r=page.request.get(base+'/export/customers.csv');assert r.ok
    # Formula-injection protection is covered separately by unit tests; HTML payload must stay literal CSV data.
    assert payload in r.text()
    (out/'accessibility-results.json').write_text(json.dumps({'tool':'axe-core 4.10.3','width':390,'pages':reports,'htmlEscaping':'PASS'},ensure_ascii=False,indent=2))
    print('AUDIT_A11Y '+json.dumps([{'route':r['route'],'violations':[(v['id'],v['impact'],len(v['nodes'])) for v in r['violations']]} for r in reports]),flush=True)
    print('PASS stored HTML escaped in client listing; no injected image/handler; authenticated CSV downloadable',flush=True)
