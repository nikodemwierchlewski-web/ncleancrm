"""Exercise the real PowerShell updater with a fake Docker CLI, never a real installation."""
import argparse,json,os,pathlib,shutil,subprocess,uuid
parser=argparse.ArgumentParser();parser.add_argument('--pwsh',default='pwsh');parser.add_argument('--work-dir',type=pathlib.Path,required=True);args=parser.parse_args()
root=pathlib.Path(__file__).resolve().parents[1];work=args.work_dir.resolve()/('pc-update-'+uuid.uuid4().hex);work.mkdir(parents=True)
cli=work/'bin';cli.mkdir();docker=cli/'docker'
docker.write_text('''#!/usr/bin/env python3
import sys,os,json,pathlib
args=sys.argv[1:];scenario=os.environ['QA_SCENARIO']
with open(os.environ['QA_LOG'],'a') as out:out.write(json.dumps(args)+'\\n')
if args[:1]==['inspect']:
 print(json.dumps([{'Destination':'/run/secrets/config','Type':'bind','Name':''},{'Destination':'/var/lib/postgresql/data','Type':'volume','Name':'other-data' if scenario=='wrong-mount' else 'ncleancrm-pc-data'}]));sys.exit(0)
if args[:1]==['compose']:
 cmd=args[3:]
 if cmd==['ps','-a','-q','postgres']:print('qa-database');sys.exit(0)
 if cmd==['ps','-q','app']:
  if scenario!='stopped-backup-fail':print('qa-old-app')
  sys.exit(0)
 if cmd[:1]==['build'] and scenario=='build-fail':sys.exit(23)
 if cmd[-1:] == ['backup'] and scenario in ['backup-fail','stopped-backup-fail']:sys.exit(24)
 if cmd[:1]==['up'] and scenario=='up-fail':sys.exit(25)
print('QA docker')
''');docker.chmod(0o700)
results=[]
for scenario in ['success','wrong-mount','build-fail','backup-fail','stopped-backup-fail','up-fail']:
    home=work/scenario;(home/'scripts').mkdir(parents=True);(home/'.local').mkdir()
    shutil.copy(root/'scripts/CRM-PC.ps1',home/'scripts/CRM-PC.ps1')
    for name in ['application-local.properties','postgres-password','postgres-admin-password']:(home/'.local'/name).write_text('synthetic-qa-only')
    log=home/'calls.jsonl';env={**os.environ,'PATH':str(cli)+os.pathsep+os.environ['PATH'],'QA_SCENARIO':scenario,'QA_LOG':str(log),'POWERSHELL_TELEMETRY_OPTOUT':'1'}
    run=subprocess.run([args.pwsh,'-NoLogo','-NoProfile','-File',str(home/'scripts/CRM-PC.ps1'),'-Action','Update'],env=env,capture_output=True,text=True)
    calls=[json.loads(x) for x in log.read_text().splitlines()];commands=[x[3:] for x in calls if x[0]=='compose']
    stopped=['stop','app'] in commands;resumed=['start','qa-old-app'] in calls;up=any(x[:1]==['up'] for x in commands);built=['build','app','postgres'] in commands
    assert (run.returncode==0)==(scenario=='success'),(scenario,run.stderr)
    assert resumed==(scenario=='backup-fail'),(scenario,calls)
    assert stopped==(scenario in ['success','backup-fail','stopped-backup-fail','up-fail']),(scenario,calls)
    assert up==(scenario in ['success','up-fail']),(scenario,calls)
    assert built==(scenario!='wrong-mount'),(scenario,calls)
    if stopped:assert commands.index(['build','app','postgres'])<commands.index(['stop','app'])
    results.append({'scenario':scenario,'pass':True,'oldContainerResumed':resumed,'migrationStartAttempted':up})
print(json.dumps(results,indent=2))
