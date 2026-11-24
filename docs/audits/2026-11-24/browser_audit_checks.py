import json,time
from urllib.parse import urlsplit

def verify_audit(page,base,out,container):
    paths=['/calendar','/today','/orders','/customers','/dashboard','/dashboard?range=month','/day-closeout','/notes','/notes/editor','/equipment','/equipment/standards','/costs','/costs/editor','/fuel','/fuel/legacy','/settings','/settings/services','/settings/areas','/settings/costs','/more','/search?q=QA','/callbacks']
    checks=[];overflows=[];names=[]
    for path in paths:
        response=page.goto(base+path)
        assert response.status==200,(path,response.status)
        assert 'no-store' in response.headers.get('cache-control',''),path
        assert "frame-ancestors 'none'" in response.headers.get('content-security-policy',''),path
        for width in [360,390,768,1440]:
            page.set_viewport_size({'width':width,'height':844})
            page.wait_for_timeout(40)
            if not page.evaluate('document.documentElement.scrollWidth<=innerWidth'):
                overflows.append({'path':path,'width':width,'elements':page.locator('main').evaluate("e=>[...e.querySelectorAll('*')].filter(n=>n.getBoundingClientRect().right>innerWidth+1).slice(0,6).map(n=>({tag:n.tagName,cls:n.className,text:n.textContent.slice(0,60)}))")})
        checks.append(path)
    # Authentication barriers across every literal non-public GET controller route, with valid sample resource IDs.
    import re,pathlib
    routes=set()
    for f in (pathlib.Path(__file__).resolve().parents[3]/'src/main/java').rglob('*Controller.java'):
        source=f.read_text();prefix=re.search(r'@RequestMapping\("([^"]+)"\)\s*public class',source)
        for route in re.findall(r'@GetMapping\("([^"?]+)"\)',source):
            if prefix:route=prefix[1]+route
            if route in ['/login','/offline.html','/logout','/']:continue
            if '{' in route:route=re.sub(r'\{(?:id|photo)\}', '00000000-0000-0000-0000-000000000001' if any(s in route for s in ['photos','receipts']) else '1',route)
            routes.add(route)
    anonymous=page.context.browser.new_context()
    denied=[]
    for route in sorted(routes):
        r=anonymous.request.get(base+route,max_redirects=0)
        assert r.status in (302,401,403),(route,r.status)
        denied.append(route)
    anonymous.close()
    page.goto('about:blank')
    import subprocess,statistics
    def sql(statement):
        subprocess.run(['docker','exec',container,'psql','-U','qa','-d','qa','-v','ON_ERROR_STOP=1','-c',statement],check=True,stdout=subprocess.DEVNULL)
    timings=[]
    for total,add in [(0,0),(100,100),(1000,900)]:
        if add:
            sql("WITH added AS (INSERT INTO memo_entry(request_token,kind,title,archived) SELECT gen_random_uuid(),'TASK','Load Audit QA',TRUE FROM generate_series(1,"+str(add)+") RETURNING id) INSERT INTO reminder_owner(source_key,memo_id) SELECT 'MEMO:'||id,id FROM added")
            sql("INSERT INTO entity_reminder(id,source_key,absolute_at) SELECT gen_random_uuid(),source_key,now()-interval '1 day' FROM reminder_owner WHERE memo_id IN (SELECT id FROM memo_entry WHERE title='Load Audit QA') AND source_key NOT IN (SELECT source_key FROM entity_reminder)")
        samples=[]
        for _ in range(3):
            start=time.monotonic();r=page.request.get(base+'/api/v1/notifications');assert r.status==200;samples.append(round((time.monotonic()-start)*1000,1))
        timings.append({'archivedReminderOwners':total,'milliseconds':samples,'median':statistics.median(samples)})
    page.goto(base+'/orders')
    assert page.request.post(base+'/orders',form={'note':'missing csrf'}).status==403
    # Log out in a second tab and verify the old tab cannot fetch protected data or replay a write.
    csrf=page.locator('input[name=_csrf]').first.input_value()
    r=page.request.post(base+'/logout',form={'_csrf':csrf},max_redirects=0)
    assert r.status==302
    assert page.request.get(base+'/export/customers.csv',max_redirects=0).status==302
    assert page.request.post(base+'/orders',form={'_csrf':csrf,'note':'old session'}).status==403
    page.reload();assert '/login' in page.url
    result={'routes':checks,'responsiveViews':len(checks)*4,'overflows':overflows,'anonymousProtectedRoutes':denied,'csrfLogout':'PASS','notificationScaling':timings}
    (out/'audit-browser-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
    print('AUDIT_BROWSER '+json.dumps(result,ensure_ascii=False),flush=True)
