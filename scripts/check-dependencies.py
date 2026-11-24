"""Check exact resolved runtime versions against OSV; fail closed on network/error/new advisories."""
import datetime,json,pathlib,re,sys,urllib.request
root=pathlib.Path(__file__).resolve().parents[1]
queries=[]
for line in pathlib.Path(sys.argv[1]).read_text().splitlines():
    m=re.match(r'\s+([^: ]+):([^: ]+):jar:([^: ]+):(compile|runtime)',line)
    if m: queries.append({'package':{'ecosystem':'Maven','name':m[1]+':'+m[2]},'version':m[3]})
if not queries:raise SystemExit('No resolved runtime dependencies found')
# No compatible Spring 6.2 patches are published for these two feature-specific advisories.
exceptions=json.loads((root/'config/dependency-applicability.json').read_text())
main=root/'src/main'
for marker in exceptions['forbiddenFeatures']:
    if any(marker in p.read_text(errors='replace') for p in main.rglob('*') if p.is_file() and p.suffix in ('.java','.yml','.xml','.html','.properties')):
        raise SystemExit('Dependency applicability must be re-evaluated: '+marker)
req=urllib.request.Request('https://api.osv.dev/v1/querybatch',data=json.dumps({'queries':queries}).encode(),headers={'Content-Type':'application/json'})
result=json.load(urllib.request.urlopen(req,timeout=45))
if len(result['results'])!=len(queries):raise SystemExit('Incomplete OSV response')
today=datetime.date.today();unresolved=[];accepted=[]
for q,r in zip(queries,result['results']):
    for vuln in r.get('vulns',[]):
        entry=next((e for e in exceptions['exceptions'] if e['id']==vuln['id'] and e['package']==q['package']['name'] and e['version']==q['version']),None)
        item={**q,'advisory':vuln['id']}
        if entry and today<=datetime.date.fromisoformat(entry['reviewBy']):accepted.append(item)
        else:unresolved.append(item)
evidence={'checkedOn':str(today),'dependencies':len(queries),'unresolved':unresolved,'documentedNotApplicable':accepted}
out=root/'target/dependency-security.json';out.parent.mkdir(exist_ok=True);out.write_text(json.dumps(evidence,indent=2)+'\n')
print(json.dumps(evidence,indent=2))
if unresolved:raise SystemExit('Unresolved vulnerability advisories; see target/dependency-security.json')
