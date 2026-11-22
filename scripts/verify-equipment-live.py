"""Live equipment checks on a NEW disposable PostgreSQL container only.
Requires built JAR, Docker, Python Playwright + bcrypt and Java 17.
Example: python scripts/verify-equipment-live.py --work-dir /tmp/nclean-equipment-qa --java /path/to/java
The runner never uses compose, .local, existing volumes or application credentials.
"""
import argparse
import shutil
import os,sys,uuid,json,subprocess,time,socket,urllib.request
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--work-dir',type=Path,required=True)
parser.add_argument('--java',default='java')
parser.add_argument('--chromium',default='/usr/bin/chromium')
parser.add_argument('--depreciation-only',action='store_true')
parser.add_argument('--job-defaults-only',action='store_true')
parser.add_argument('--customer-sources-only',action='store_true')
parser.add_argument('--customer-insights-only',action='store_true')
parser.add_argument('--context-path',default='/crm',choices=['','/crm'])
args=parser.parse_args()
sys.path.insert(0,str(Path(__file__).resolve().parent))
import bcrypt
from playwright.sync_api import sync_playwright
from browser_equipment_checks import verify_equipment
from browser_depreciation_checks import verify_depreciation
from browser_job_defaults_checks import verify_job_defaults
from browser_customer_sources_checks import verify_customer_sources
from browser_customer_insights_checks import verify_customer_insights
root=Path(__file__).resolve().parents[1];out=args.work_dir.resolve()
if not shutil.which(args.java):parser.error('Wskaż Java 17 przez --java.')
if not (root/'target/nclean-crm-0.0.1-SNAPSHOT.jar').is_file():parser.error('Najpierw zbuduj JAR przez ./mvnw verify.')
out.mkdir(parents=True,exist_ok=True)
owner='equipment-qa';password=uuid.uuid4().hex;pgpass=uuid.uuid4().hex
name='nclean-equipment-qa-'+uuid.uuid4().hex[:10]
envfile=out/'postgres.env';envfile.write_text('POSTGRES_PASSWORD='+pgpass+'\nPOSTGRES_USER=qa\nPOSTGRES_DB=qa\n');envfile.chmod(0o600)
subprocess.run(['docker','run','-d','--name',name,'--env-file',str(envfile),'-p','127.0.0.1::5432','--tmpfs','/var/lib/postgresql/data','postgres@sha256:31ba9c94a03ada9988f2bb493a22a75eef3a185884533e5cdbcd498ea835b08f'],check=True,stdout=subprocess.DEVNULL)
port=json.loads(subprocess.check_output(['docker','inspect',name]))[0]['NetworkSettings']['Ports']['5432/tcp'][0]['HostPort']
for _ in range(60):
 if subprocess.run(['docker','exec',name,'pg_isready','-U','qa','-d','qa'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0:break
 time.sleep(.5)
sock=socket.socket();sock.bind(('127.0.0.1',0));appport=sock.getsockname()[1];sock.close()
env={k:v for k,v in os.environ.items() if not k.startswith(('CRM_','SPRING_','SERVER_'))}
env.update(CRM_DB_URL='jdbc:postgresql://127.0.0.1:'+port+'/qa',CRM_DB_USERNAME='qa',CRM_DB_PASSWORD=pgpass,CRM_ADMIN_USERNAME=owner,CRM_ADMIN_PASSWORD_HASH=bcrypt.hashpw(password.encode(),bcrypt.gensalt()).decode())
log=(out/'app.log').open('w')
process=subprocess.Popen([args.java,'-jar',str(root/'target/nclean-crm-0.0.1-SNAPSHOT.jar'),'--spring.config.location=classpath:/application.yml','--spring.profiles.active=integration,tycoon','--server.port='+str(appport),'--server.servlet.context-path='+args.context_path],env=env,cwd=root,stdout=log,stderr=subprocess.STDOUT)
base=f'http://127.0.0.1:{appport}'+args.context_path
try:
 for _ in range(120):
  try:
   if urllib.request.urlopen(base+'/login',timeout=1).status==200:break
  except Exception:time.sleep(.5)
  if process.poll() is not None:raise RuntimeError('Application startup failed; see private QA app.log')
 with sync_playwright() as p:
  browser=p.chromium.launch(executable_path=args.chromium,args=['--no-sandbox']);context=browser.new_context();page=context.new_page();errors=[];page.on('pageerror',lambda e:errors.append(str(e)))
  page.goto(base+'/login');page.locator('[name=username]').fill(owner);page.locator('[name=password]').fill(password);page.locator('button[type=submit]').click();page.wait_for_url('**/calendar')
  if args.customer_insights_only:verify_customer_insights(page,base,out)
  elif args.customer_sources_only:verify_customer_sources(page,base,out)
  elif args.job_defaults_only:verify_job_defaults(page,base,out)
  elif args.depreciation_only:verify_depreciation(page,base,out)
  else:verify_equipment(page,base,out)
  assert not errors,errors
  print('PASS no JS errors; context path '+(args.context_path or '/')+'; Java 17; synthetic isolated PostgreSQL',flush=True);browser.close()
finally:
 process.terminate();process.wait(timeout=30);log.close();subprocess.run(['docker','stop',name],stdout=subprocess.DEVNULL,check=False)
 subprocess.run(['docker','rm',name],stdout=subprocess.DEVNULL,check=False)  # Only our own new tmpfs QA container; no volumes.
