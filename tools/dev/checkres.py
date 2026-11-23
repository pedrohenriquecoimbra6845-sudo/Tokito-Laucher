import re, subprocess, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import chg
ROOT=chg.root()
RES=ROOT+'/ZalithLauncher/src/main/res'
files=[f for f in chg.changed(ROOT) if f.endswith(('.kt','.java'))]
names=set()
for d in ('values','values-pt-rBR'):
    names|=set(re.findall(r'<string name="([^"]+)"',open(f'{RES}/{d}/strings.xml',encoding='utf-8').read()))
en=set(re.findall(r'<string name="([^"]+)"',open(f'{RES}/values/strings.xml',encoding='utf-8').read()))
draw=set()
for d in os.listdir(RES):
    if d.startswith(('drawable','mipmap','raw')):
        for f in os.listdir(f'{RES}/{d}'):
            draw.add(f.rsplit('.',1)[0])
bad=0
for f in files:
    t=open(f'{ROOT}/{f}',encoding='utf-8').read()
    for n in set(re.findall(r'R\.string\.([A-Za-z0-9_]+)',t)):
        if n not in en:
            print('MISSING string',n,'in',f); bad+=1
    for n in set(re.findall(r'R\.(?:drawable|raw|mipmap)\.([A-Za-z0-9_]+)',t)):
        if n not in draw:
            print('MISSING drawable/raw',n,'in',f); bad+=1
print('checked',len(files),'files; problems:',bad)
