"""Build a single-file, synthetic fuel DEMO from a verified real-app snapshot."""
import argparse
import base64
import json
from pathlib import Path
from urllib.parse import urlparse
from lxml import html

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--snapshot',type=Path,required=True)
parser.add_argument('--state',type=Path,required=True)
parser.add_argument('--output',type=Path,required=True)
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/static'
doc=html.document_fromstring(args.snapshot.read_text())
css=[]
for link in doc.xpath('//link[@rel="stylesheet"]'):
    name=urlparse(link.get('href')).path
    if not name.startswith('/css/'):raise ValueError('Only project CSS is allowed')
    path=(assets/name.lstrip('/')).resolve();path.relative_to(assets.resolve());css.append(path.read_text())
for node in doc.xpath('//script|//link|/html/body/header|//*[contains(concat(" ",normalize-space(@class)," ")," ncleanek-dock ")]|//*[contains(concat(" ",normalize-space(@class)," ")," removal-notice ")]|//*[contains(concat(" ",normalize-space(@class)," ")," ncleanek-help ")]'):
    if node.getparent() is not None:node.getparent().remove(node)
for node in doc.xpath('//input[@name="_csrf"]'):node.set('value','demo-only')
for button in doc.xpath('//form//button[@type="submit" or not(@type)]'):
    button.set('type','button');button.set('data-demo-submit','')
for image in doc.xpath('//img[@src]'):
    path=assets/urlparse(image.get('src')).path.lstrip('/')
    if path.is_file() and path.resolve().is_relative_to(assets.resolve()):image.set('src','data:image/svg+xml;base64,'+base64.b64encode(path.read_bytes()).decode())
    else:image.attrib.pop('src',None);image.set('hidden','')
for link in doc.xpath('//a[@href]'):
    if link.get('href').startswith('/today'):link.set('href','#demo-results');link.text='Wynik firmy ↓'
    elif link.get('href').startswith('/fuel/legacy'):link.set('href','#demo-results');link.text='Starsze dane są dostępne w pełnym CRM'
styles='\n'.join(css)
for font in (assets/'font').iterdir():
    if font.is_file() and font.suffix in ('.ttf','.woff','.woff2'):
        mime='font/ttf' if font.suffix=='.ttf' else 'font/'+font.suffix[1:]
        styles=styles.replace('/font/'+font.name,'data:'+mime+';base64,'+base64.b64encode(font.read_bytes()).decode())
style=html.Element('style');style.text=styles+'''
.demo-banner{max-width:1120px;margin:16px auto;padding:14px 20px;background:#244754;color:#fff;font-family:Arial,sans-serif;line-height:1.6;border-bottom:5px solid #3298be}.demo-banner strong{color:#9fe1e1}.demo-banner small{display:block}.demo-finance{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin:14px 0 20px}.demo-finance>div{border:2px solid #b4cdd3;box-shadow:4px 4px 0 #c1d9d8;background:#fffef7;padding:14px}.demo-finance span{display:block;font-size:12px;color:#506978;margin-bottom:8px}.demo-finance strong{font-size:23px;overflow-wrap:anywhere}.demo-finance .demo-profit{background:#e5f3e7}.demo-profit.negative{background:#fff0e5}.demo-message{position:fixed;top:8px;left:50%;transform:translateX(-50%);max-width:calc(100% - 20px);padding:12px 18px;background:#244754;color:#fff;z-index:1000;border:2px solid #8fd4d1;box-shadow:4px 4px 0 #799da6}.demo-message:empty{display:none}.full-tank-page .quick-fuel-footer{bottom:0!important}body{padding-bottom:10px!important}.demo-finance-label{grid-column:1/-1;font-size:12px;color:#506978;margin:0}@media(max-width:600px){.demo-finance{gap:8px;grid-template-columns:1fr 1fr}.demo-finance>div{padding:12px}.demo-finance strong{font-size:21px}.demo-profit{grid-column:1/-1}.demo-banner{margin:0;padding:10px 16px}}
'''
doc.find('head').append(style)
doc.find('head').find('title').text='ncLEAN — proste tankowania · DEMO'
banner=html.fragment_fromstring('<header class="demo-banner"><strong>ncLEAN · Tankowania · DEMO</strong><small>Próbny widok z aplikacji. Dane są syntetyczne i znikają po odświeżeniu. Prawdziwy CRM zapisuje do PostgreSQL.</small></header>')
doc.find('body').insert(0,banner)
main=doc.xpath('//*[@id="main-content"]')[0]
panel=html.fragment_fromstring('<section id="demo-results" class="demo-finance" aria-label="Wpływ paliwa na wynik firmy"><p class="demo-finance-label">Przykładowy wynik firmy — <span data-demo-month></span></p><div><span>Przychód</span><strong data-demo-income></strong></div><div><span>Zakupy paliwa</span><strong data-demo-fuel></strong></div><div class="demo-profit"><span>Zysk operacyjny</span><strong data-demo-profit></strong></div><p class="demo-finance-label">Próbne przychody i pozostałe koszty są stałe. Dodawaj, poprawiaj i usuwaj tankowania, aby zobaczyć ich wpływ na wybrany miesiąc.</p></section>')
main.insert(1,panel)
for node in doc.xpath('//*[contains(concat(" ",normalize-space(@class)," ")," notice ")]'):
    if node.getparent() is not None:node.getparent().remove(node)
message=html.Element('p',{'class':'demo-message','role':'status','id':'demo-message'});doc.find('body').append(message)
state=json.loads(args.state.read_text())
seed=html.Element('script');seed.text='window.FUEL_DEMO_SEED='+json.dumps(state,ensure_ascii=False).replace('<','\\u003c')+';';doc.find('body').append(seed)
for source in [assets/'js/full-tank.js',root/'preview/support/full-tank-demo.js']:
    script=html.Element('script');script.text=source.read_text();doc.find('body').append(script)
args.output.parent.mkdir(parents=True,exist_ok=True)
args.output.write_text('<!doctype html>\n'+html.tostring(doc,encoding='unicode'))
print('Standalone synthetic HTML:',args.output)
