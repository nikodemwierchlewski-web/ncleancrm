"""Click the standalone, in-memory fuel HTML; including an opaque chat sandbox."""
import argparse
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--html',type=Path,default=Path('preview/nclean-full-tank.html'))
parser.add_argument('--output',type=Path,default=Path('.local/full-tank-preview-results'))
args=parser.parse_args();args.output.mkdir(parents=True,exist_ok=True)
source=args.html.read_text()

def exercise(root):
    expect(root.locator('[data-demo-profit]')).to_contain_text('740,00')
    f=root.locator('#fuel-purchase-form');f.locator('[name=purchased_at]').fill('2026-10-15T12:00');f.locator('[name=meter]').fill('100800');f.locator('[name=litres]').fill('25');f.locator('[name=amount]').fill('150');expect(root.locator('#full-unit-price')).to_have_text('6,000 zł/l');f.get_by_role('button',name='Zapisz tankowanie',exact=True).click();expect(root.locator('[data-demo-profit]')).to_contain_text('590,00')
    row=root.locator('#purchases .fuel-history-row').first;row.get_by_role('button',name='Edytuj',exact=True).click();f.locator('[name=amount]').fill('180');f.get_by_role('button',name='Zapisz tankowanie',exact=True).click();expect(root.locator('[data-demo-profit]')).to_contain_text('560,00')
    def toggle(name,profit):
        root.locator('#purchases .fuel-history-row').first.get_by_role('button',name=name,exact=True).click();root.locator('#removal-confirm').get_by_role('button',name=name,exact=True).click();expect(root.locator('[data-demo-profit]')).to_contain_text(profit)
    toggle('Usuń','740,00');toggle('Przywróć','560,00');toggle('Usuń','740,00')
    period=root.locator('.fuel-date-selector');period.locator('[name=month]').fill('2026-11');period.get_by_role('button',name='Pokaż',exact=True).click();expect(root.locator('[data-demo-profit]')).to_contain_text('0,00')
    f.locator('[name=target][value=GENERATOR]').check();f.locator('[name=purchased_at]').fill('2026-11-02T12:00');f.locator('[name=meter]').fill('115');f.locator('[name=litres]').fill('5');f.locator('[name=amount]').fill('40');f.get_by_role('button',name='Zapisz tankowanie',exact=True).click();expect(root.locator('[data-demo-profit]')).to_contain_text('-40,00')
    period.locator('[name=month]').fill('2026-10');period.get_by_role('button',name='Pokaż',exact=True).click();expect(root.locator('[data-demo-profit]')).to_contain_text('740,00')
    f.locator('[name=purchased_at]').fill('2026-10-16T12:00');f.locator('[name=meter]').fill('99000');f.locator('[name=litres]').fill('5');f.locator('[name=amount]').fill('30');f.get_by_role('button',name='Zapisz tankowanie',exact=True).click();expect(f.locator('.form-error')).to_contain_text('stanem początkowym');expect(f.locator('[name=amount]')).to_have_value('30');expect(root.locator('[data-demo-profit]')).to_contain_text('740,00')

with sync_playwright() as p:
    browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox'])
    page=browser.new_page(viewport={'width':390,'height':844});errors=[];requests=[]
    page.on('pageerror',lambda e:errors.append(str(e)))
    page.on('request',lambda r:requests.append(r.url) if r.url.startswith(('http:','https:')) else None)
    page.set_content(source,wait_until='load');exercise(page)
    for width in (360,390,768,1440):
        page.set_viewport_size({'width':width,'height':844});assert page.evaluate('document.documentElement.scrollWidth<=innerWidth');page.screenshot(path=str(args.output/('fuel-demo-'+str(width)+'.png')),full_page=True)
    page.set_content('<style>body{margin:0}</style><iframe id="chat" sandbox="allow-scripts" style="width:100%;height:844px;border:0"></iframe>')
    page.locator('#chat').evaluate('(e,s)=>e.srcdoc=s',source);exercise(page.frame_locator('#chat'))
    assert not errors,errors
    assert not requests,requests
    browser.close()
print('PASS fuel HTML: add/edit/delete/restore, October/November profit, generator counter, validation preserves fields, four widths, opaque sandbox, no external requests or JS errors.')
