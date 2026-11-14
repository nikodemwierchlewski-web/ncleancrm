"""Repeatable Chromium checks against the synthetic, self-contained HTML preview."""
import argparse
import json
import re
import shutil
from pathlib import Path
from urllib.parse import urlparse
from playwright.sync_api import sync_playwright, expect

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--url',required=True)
parser.add_argument('--html',type=Path,required=True)
parser.add_argument('--output',type=Path,default=Path('/tmp/nclean-removal-preview-check'))
args=parser.parse_args()
if urlparse(args.url).hostname not in ('127.0.0.1','localhost'):parser.error('Use a locally served synthetic preview.')
args.output.mkdir(parents=True,exist_ok=True)
errors=[];results=[]
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path=shutil.which('chromium'),headless=True,args=['--no-sandbox'])
 for width in (360,390,768,1440):
  page=browser.new_page(viewport={'width':width,'height':844});page.on('pageerror',lambda e:errors.append(str(e)));page.goto(args.url);f=page.frame_locator('#screen');expect(page.locator('#status')).to_contain_text('DEMO')
  def navigate(route):page.frame(name='screen').evaluate('(r)=>window.previewNavigate(r)',route)
  def confirm():f.locator('#removal-confirm').get_by_role('button',name='Usuń',exact=True).click();expect(f.locator('#removal-confirm')).not_to_be_visible()
  def remove(form):form.locator('button').click();expect(f.locator('#removal-confirm')).to_be_visible();confirm()
  def undo():f.get_by_role('button',name='Cofnij usunięcie',exact=True).click();f.locator('#removal-confirm').get_by_role('button',name='Przywróć',exact=True).click();expect(f.locator('#removal-confirm')).not_to_be_visible()
  f.locator('#agenda-orders [data-order]').first.wait_for();count=f.locator('#agenda-orders [data-order]').count();f.locator('#agenda-orders [data-order]').first.click();form=f.locator('#order-content .removal-form');form.locator('button').click();f.locator('#removal-confirm').get_by_role('button',name='Zostaw',exact=True).click();expect(f.locator('#agenda-orders [data-order]')).to_have_count(count);remove(form);expect(f.locator('#agenda-orders [data-order]')).to_have_count(count-1);undo();expect(f.locator('#agenda-orders [data-order]')).to_have_count(count)
  navigate('/settings/services');group=f.locator('.service-category').first;group.locator('>summary').click();service=group.locator('.service-card').first;sid=service.get_attribute('data-service-id');remove(service.locator('>.removal-form'));expect(f.locator('[data-service-id="'+sid+'"]').first).to_have_count(0);undo();expect(f.locator('[data-service-id="'+sid+'"]')).to_have_count(1)
  group=f.locator('.service-category').first;cid=group.get_attribute('data-category-id');group.locator('>summary').click();remove(group.locator('>.removal-form'));expect(f.locator('[data-category-id="'+cid+'"]')).to_have_count(0);navigate('/calendar');f.locator('.add-booking').click();expect(f.locator('[data-category-choice="'+cid+'"]').first).to_have_count(0);page.keyboard.press('Escape');undo();navigate('/settings/services');expect(f.locator('[data-category-id="'+cid+'"]')).to_have_count(1)
  navigate('/costs');row=f.locator('[data-cost-id^="EXPENSE-"]').first;identifier=row.get_attribute('data-cost-id');remove(row.locator('.removal-form'));expect(f.locator('[data-cost-id="'+identifier+'"]').first).to_have_count(0);undo();expect(f.locator('[data-cost-id="'+identifier+'"]')).to_have_count(1)
  navigate('/calendar');f.locator('.add-booking').click();form=f.locator('#booking-form');form.locator('[data-category-choice="1"]').click();form.locator('[name=qty_2]').fill('1');form.locator('[name=name]').fill('Test usuwania nowego wpisu');form.locator('[name=phone]').fill('500000099');form.locator('[name=address]').fill('Adres testowy');form.get_by_role('button',name='Zapisz zlecenie',exact=True).click();expect(f.locator('#booking-editor')).not_to_be_visible();entry=f.locator('#agenda-orders [data-order]').filter(has_text='Test usuwania nowego wpisu');expect(entry).to_have_count(1);entry.click();remove(f.locator('#order-content .removal-form'));expect(f.locator('#agenda-orders')).not_to_contain_text('Test usuwania nowego wpisu');undo();expect(f.locator('#agenda-orders')).to_contain_text('Test usuwania nowego wpisu')
  page.frame(name='screen').evaluate("window.NcleanMotion.confirmed({kind:'completed',id:7001})");expect(f.locator('.nclean-stars svg')).to_have_count(8);assert page.frame(name='screen').evaluate("getComputedStyle(document.querySelector('.nclean-stars')).pointerEvents==='none'");page.screenshot(path=str(args.output/('clean-stars-'+str(width)+'.png')));page.wait_for_timeout(1000);expect(f.locator('.nclean-stars')).to_have_count(0);page.emulate_media(reduced_motion='reduce');page.frame(name='screen').evaluate("window.NcleanMotion.confirmed({kind:'completed',id:7001})");expect(f.locator('.nclean-stars')).to_have_count(0);assert page.frame(name='screen').evaluate('document.documentElement.scrollWidth<=innerWidth');results.append({'width':width,'delete_cancel_undo_booking_service_category_cost_and_new_booking':True,'category_hidden_from_booking':True,'stars_bounded_and_reduced_motion':True});page.close()
 page=browser.new_page(viewport={'width':390,'height':844});page.on('pageerror',lambda e:errors.append(str(e)));page.set_content('<iframe id="chat" name="chat" sandbox="allow-scripts" style="width:100%;height:810px;border:0"></iframe>');page.locator('#chat').evaluate('(e,s)=>e.srcdoc=s',args.html.read_text());f=page.frame_locator('#chat').frame_locator('#screen');f.locator('#agenda-orders [data-order]').first.click();f.locator('#order-content .removal-form button').click();f.locator('#removal-confirm').get_by_role('button',name='Usuń',exact=True).click();expect(f.get_by_role('button',name='Cofnij usunięcie',exact=True)).to_be_visible();assert not errors,errors;browser.close();(args.output/'removal-preview-results.json').write_text(json.dumps({'viewports':results,'nested_opaque_sandbox':True,'javascript_errors':errors},indent=2));print('PASS removal preview: four widths, cancellation/delete/undo, catalogue, costs, new booking, stars/reduced motion, nested sandbox; no JS errors.')
