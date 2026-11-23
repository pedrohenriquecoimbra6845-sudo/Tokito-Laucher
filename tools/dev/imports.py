import re,subprocess,sys,os
root='ZalithLauncher/src/main/java'
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import chg
files=[p for p in chg.changed(os.getcwd()) if p.endswith(('.kt','.java'))]
allsrc=''
for d,_,fs in os.walk(root):
    for f in fs:
        if f.endswith(('.kt','.java')):
            allsrc+=open(os.path.join(d,f),errors='ignore').read()+'\n'
bad=0
for f in files:
    for m in re.finditer(r'^import (com\.movtery\.zalithlauncher\.[\w.]+)',open(f).read(),re.M):
        fq=m.group(1); name=fq.split('.')[-1]
        if name in('R','BuildConfig'): continue
        if not re.search(r'\b(class|object|interface|fun|val|var|typealias)\s+(<[^>]*>\s*)?([\w.]+\.)?%s\b'%name,allsrc):
            print(f,fq); bad+=1
print('unresolved',bad)
