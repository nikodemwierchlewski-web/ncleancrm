"""Click the synthetic HTML, including its sandbox. Requires local HTTP and Chromium."""
import argparse
import json
import shutil
from pathlib import Path
from urllib.parse import urlparse
from playwright.sync_api import expect, sync_playwright

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--url',required=True)
parser.add_argument('--html',type=Path,required=True)
parser.add_argument('--output',type=Path,default=Path('/tmp/nclean-pixel-preview'))
args=parser.parse_args()
if urlparse(args.url).hostname not in ('127.0.0.1','localhost'):parser.error('Use a local synthetic preview.')
args.output.mkdir(parents=True,exist_ok=True)
expect.set_options(timeout=15000)
errors=[];results=[]
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path=shutil.which('chromium'),headless=True,args=['--no-sandbox'])
 for width in (360,390,768,1440):
  page=browser.new_page(viewport={'width':width,'height':844});page.on('pageerror',lambda e:errors.append(str(e)));page.goto(args.url)
  f=page.frame_locator('#screen')
  def nav(path):page.frame(name='screen').evaluate('(p)=>window.previewNavigate(p)',path)
  goal=f.locator('.main-goal-card');expect(goal.locator('.goal-count')).to_have_text('1 z 2 kroków')
  expect(goal.locator('.goal-pixel-ring text')).to_have_text('50%')
  assert page.frame(name='screen').evaluate('document.documentElement.scrollWidth<=innerWidth')
  page.screenshot(path=str(args.output/('dashboard-'+str(width)+'.png')),full_page=True)
  goal.locator('.main-goal-action button').click();expect(goal.locator('.goal-count')).to_have_text('2 z 2 kroków')
  goal.get_by_role('button',name='✓ Potwierdź osiągnięcie celu',exact=True).click();expect(goal).to_have_class('main-goal-card is-achieved')
  nav('/calendar');compact=f.locator('.main-goal-compact');expect(compact.locator('summary')).to_contain_text('2/2');compact.locator('summary').click();expect(compact.get_by_role('button',name='Przywróć do aktywnych',exact=True)).to_be_visible()
  nav('/notes?state=done');card=f.locator('.memo-card').filter(has_text='Dopracować ofertę na zimę');card.get_by_role('button',name='Archiwizuj',exact=True).click();expect(card).to_have_count(0)
  nav('/dashboard');expect(goal).to_contain_text('Co chcesz osiągnąć?')
  nav('/notes');f.locator('[data-note-new]').click();form=f.locator('#note-form');form.locator('[name=kind]').select_option('GOAL');form.locator('[name=title]').fill('Mój nowy główny cel');form.locator('[name=mainGoal]').check();form.get_by_role('button',name='Zapisz',exact=True).click();expect(f.locator('#note-editor')).not_to_be_visible()
  nav('/dashboard');expect(goal.locator('h2')).to_have_text('Mój nowy główny cel');expect(goal.locator('.goal-count')).to_contain_text('bez kroków');expect(goal.locator('.goal-pixel-ring text')).to_have_text('—')
  page.emulate_media(reduced_motion='reduce');goal.get_by_role('button',name='✓ Cel osiągnięty',exact=True).click();expect(goal).to_have_class('main-goal-card is-achieved');expect(f.locator('.nclean-stars')).to_have_count(0)
  nav('/calendar');expect(f.locator('#agenda-orders [data-order]')).not_to_have_count(0)
  f.get_by_role('link',name='Klienci',exact=True).click();expect(f.locator('main h1')).to_contain_text('Klienci')
  nav('/settings');expect(f.locator('main h1')).to_have_text('Ustawienia')
  results.append({'width':width,'goal_step_complete_archive_new_select':True,'no_fake_percent':True,'calendar_customers_settings':True,'reduced_motion':True});page.close()
 page=browser.new_page(viewport={'width':390,'height':844});page.on('pageerror',lambda e:errors.append(str(e)));page.goto(args.url);page.set_content('<iframe id="chat" name="chat" sandbox="allow-scripts" style="width:100%;height:810px;border:0"></iframe>');page.locator('#chat').evaluate('(e,s)=>e.srcdoc=s',args.html.read_text());f=page.frame_locator('#chat').frame_locator('#screen');goal=f.locator('.main-goal-card');expect(goal.locator('.goal-count')).to_have_text('1 z 2 kroków');goal.locator('.main-goal-action button').click();expect(goal.locator('.goal-count')).to_have_text('2 z 2 kroków');assert not errors,errors
 browser.close()
(args.output/'results.json').write_text(json.dumps({'viewports':results,'nested_sandbox':True,'javascript_errors':errors},indent=2))
print('PASS pixel preview: four widths, persisted in-session goal, steps/completion/archive/new goal, calendar/customers/settings, reduced motion and nested sandbox; no JS errors.')
