"""Exercise the synthetic Today HTML. The real backend is verified separately."""
import argparse,json,shutil
from pathlib import Path
from urllib.parse import urlparse
from playwright.sync_api import sync_playwright,expect
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--url',required=True);parser.add_argument('--html',type=Path,required=True);parser.add_argument('--output',type=Path,default=Path('/tmp/nclean-today-preview'));args=parser.parse_args()
if urlparse(args.url).hostname not in ('127.0.0.1','localhost'):parser.error('Use a locally served synthetic preview.')
args.output.mkdir(parents=True,exist_ok=True);expect.set_options(timeout=15000);errors=[];results=[]
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path=shutil.which('chromium'),headless=True,args=['--no-sandbox'])
 for width in (360,390,768,1440):
  page=browser.new_page(viewport={'width':width,'height':844});page.on('pageerror',lambda e:errors.append(str(e)));page.goto(args.url);f=page.frame_locator('#screen')
  def nav(route):page.frame(name='screen').evaluate('(r)=>window.previewNavigate(r)',route)
  goal=f.locator('.main-goal-card');expect(goal.locator('.goal-count')).to_have_text('1 z 2 kroków');expect(f.locator('[data-chart-select] option')).to_have_count(30);expect(f.locator('.today-order')).not_to_have_count(0)
  assert page.frame(name='screen').evaluate('document.documentElement.scrollWidth<=innerWidth');page.screenshot(path=str(args.output/('today-'+str(width)+'.png')))
  f.get_by_role('link',name='Tydzień',exact=True).click();expect(f.locator('[data-chart-select] option')).to_have_count(7)
  nav('/calendar');expect(f.locator('#month-overview')).to_be_visible();expect(f.locator('#week-strip')).to_have_count(0)
  assert page.frame(name='screen').evaluate('getComputedStyle(document.querySelector(".calendar-grid")).gridTemplateColumns.split(" ").length')==7
  f.locator('.day-select').first.click()
  if width<=900:expect(f.locator('#agenda-sheet')).to_be_visible();f.locator('#agenda-sheet [data-close]').click()
  nav('/fuel');form=f.locator('#fuel-purchase-form');form.locator('[name=odometer]').fill('150000');form.locator('[name=litres]').fill('32,5');form.locator('[name=litre_price]').fill('6,29');expect(f.locator('#fuel-total')).to_contain_text('204,43');form.get_by_role('button',name='Zapisz tankowanie',exact=True).click();expect(f.locator('#purchases')).to_contain_text('204.43')
  row=f.locator('#purchases .fuel-history-row').filter(has_text='204.43');row.get_by_role('link',name='Edytuj',exact=True).click();form=f.locator('#fuel-purchase-form');expect(form.locator('[name=odometer]')).to_have_value('150000');form.locator('[name=litres]').fill('10');expect(f.locator('#fuel-total')).to_contain_text('62,90');form.get_by_role('button',name='Zapisz tankowanie',exact=True).click();expect(f.locator('#purchases')).to_contain_text('62.90')
  row=f.locator('#purchases .fuel-history-row').filter(has_text='62.90');row.get_by_role('button',name='Anuluj',exact=True).click();expect(row).to_contain_text('Anulowane');row.get_by_role('button',name='Przywróć',exact=True).click();expect(row).not_to_contain_text('Anulowane')
  nav('/notes');f.locator('[data-note-new]').click();form=f.locator('#note-form');form.locator('[name=title]').fill('Notatka z przypomnieniem DEMO');form.locator('[name=todayPinned]').check();form.locator('.reminder-editor>summary').click();form.locator('[name=reminder_frequency]').select_option('DAILY');form.locator('[name=reminder_time]').fill('00:00');form.get_by_role('button',name='Zapisz',exact=True).click();expect(f.locator('#note-editor')).not_to_be_visible();nav('/today');reminder=f.locator('.today-reminders .reminder-card').filter(has_text='Notatka z przypomnieniem DEMO');expect(reminder).to_be_visible();reminder.get_by_role('button',name='Zrobione na dziś',exact=True).click();expect(reminder).to_have_count(0);expect(f.locator('.today-notes')).to_contain_text('Notatka z przypomnieniem DEMO')
  page.emulate_media(reduced_motion='reduce');nav('/today');goal=f.locator('.main-goal-card');goal.locator('.main-goal-action button').click();expect(goal.locator('.goal-count')).to_have_text('2 z 2 kroków');expect(f.locator('.nclean-stars')).to_have_count(0)
  assert page.frame(name='screen').evaluate('document.documentElement.scrollWidth<=innerWidth');results.append({'width':width,'today_month_week':True,'monthly_calendar':True,'quick_fuel_create_edit_cancel_restore':True,'note_pin_recurrence_done':True,'goal_and_reduced_motion':True});page.close()
 # HTTP host, two opaque sandbox frames, matching a chat file preview.
 page=browser.new_page(viewport={'width':390,'height':844});page.on('pageerror',lambda e:errors.append(str(e)));page.goto(args.url);page.set_content('<iframe id="chat" name="chat" sandbox="allow-scripts" style="width:100%;height:810px;border:0"></iframe>');page.locator('#chat').evaluate('(e,s)=>e.srcdoc=s',args.html.read_text());f=page.frame_locator('#chat').frame_locator('#screen');expect(f.locator('.today-order')).not_to_have_count(0);page.frame(name='screen').evaluate("window.previewNavigate('/fuel')");form=f.locator('#fuel-purchase-form');form.locator('[name=odometer]').fill('150100');form.locator('[name=litres]').fill('10');form.locator('[name=litre_price]').fill('6.50');form.get_by_role('button',name='Zapisz tankowanie',exact=True).click();expect(f.locator('#purchases')).to_contain_text('65.00');assert not errors,errors;browser.close()
(args.output/'results.json').write_text(json.dumps({'viewports':results,'nested_opaque_sandbox':True,'javascript_errors':errors},indent=2))
print('PASS Today HTML: four widths, month/week, month calendar/day sheet, fuel create/edit/cancel/restore, pinned daily note/done, goal, reduced motion and nested sandbox; no JS errors.')
