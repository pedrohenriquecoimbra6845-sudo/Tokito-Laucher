import re, sys, importlib.util, glob, os
import xml.etree.ElementTree as ET

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import chg
RES = chg.root() + '/ZalithLauncher/src/main/res'
HERE = os.path.dirname(os.path.abspath(__file__))
BEGIN = '    <!-- Tokito Launcher 0.11.4 begin -->'
END = '    <!-- Tokito Launcher 0.11.4 end -->'

S = []
REMOVE = []      # names that must disappear from every locale
REPLACE = {}     # name -> (en, pt)
for f in sorted(glob.glob(os.path.join(HERE, 'part_*.py'))):
    spec = importlib.util.spec_from_file_location(os.path.basename(f)[:-3], f)
    m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
    S += getattr(m, 'S', [])
    REMOVE += getattr(m, 'REMOVE', [])
    REPLACE.update(getattr(m, 'REPLACE', {}))

names = [r[0] for r in S]
assert len(names) == len(set(names)), [n for n in names if names.count(n) > 1]

def process(path, idx):
    text = open(path, encoding='utf-8').read()
    # drop the previous 0.11.4 block, if any
    text = re.sub(r'\n?' + re.escape(BEGIN) + r'.*?' + re.escape(END) + r'\n?', '\n', text, flags=re.S)
    for name in REMOVE:
        text = re.sub(r'[ \t]*<string name="%s"[^>]*>.*?</string>[ \t]*\n?' % re.escape(name), '', text, flags=re.S)
    for name, vals in REPLACE.items():
        pat = re.compile(r'(<string name="%s"[^>]*>)(.*?)(</string>)' % re.escape(name), re.S)
        if pat.search(text) and idx is not None:
            text = pat.sub(lambda m: m.group(1) + vals[idx] + m.group(3), text)
    if idx is not None:
        existing = set(re.findall(r'<string name="([A-Za-z0-9_]+)"', text))
        clash = [n for n in names if n in existing]
        if clash:
            raise SystemExit('already defined outside the block in %s: %s' % (path, clash))
        lines = ['    <string name="%s">%s</string>' % (r[0], r[1 + idx]) for r in S]
        block = BEGIN + '\n' + '\n'.join(lines) + '\n' + END + '\n'
        i = text.rindex('</resources>')
        text = text[:i].rstrip('\n') + '\n\n' + block + text[i:]
    open(path, 'w', encoding='utf-8').write(text)
    ET.parse(path)

import subprocess
locales = [d for d in sorted(os.listdir(RES)) if d.startswith('values') and os.path.exists(os.path.join(RES, d, 'strings.xml'))]
for d in locales:
    p = os.path.join(RES, d, 'strings.xml')
    if d == 'values':
        process(p, 0)
    elif d == 'values-pt-rBR':
        process(p, 1)
    else:
        process(p, None)   # only removes names
print('ok', len(S), 'strings;', 'removed:', REMOVE)
