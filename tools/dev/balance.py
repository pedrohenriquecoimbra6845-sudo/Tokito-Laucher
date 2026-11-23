import sys, subprocess, os

def check(src):
    stack = []
    i, n = 0, len(src)
    line = 1
    pairs = {')': '(', '}': '{', ']': '['}
    # modes stack for templates: when inside "${ ... }", we push a marker on the stack so the matching } returns to string mode
    def skip_string(i, raw):
        # returns index after the string; handles ${...} by recursive code scanning
        nonlocal line
        while i < n:
            c = src[i]
            if c == '\n': line += 1
            if raw:
                if src.startswith('"""', i):
                    j = i
                    while j < n and src[j] == '"': j += 1
                    return j
            else:
                if c == '\\': i += 2; continue
                if c == '"': return i + 1
            if c == '$' and i + 1 < n and src[i + 1] == '{':
                i = scan_code(i + 2, template=True)
                continue
            i += 1
        return i
    def scan_code(i, template=False):
        nonlocal line
        depth = 0
        while i < n:
            c = src[i]
            if c == '\n': line += 1; i += 1; continue
            if src.startswith('//', i):
                while i < n and src[i] != '\n': i += 1
                continue
            if src.startswith('/*', i):
                d = 1; i += 2
                while i < n and d:
                    if src.startswith('/*', i): d += 1; i += 2
                    elif src.startswith('*/', i): d -= 1; i += 2
                    else:
                        if src[i] == '\n': line += 1
                        i += 1
                continue
            if src.startswith('"""', i):
                i = skip_string(i + 3, True); continue
            if c == '"':
                i = skip_string(i + 1, False); continue
            if c == "'":
                # char literal
                j = i + 1
                if j < n and src[j] == '\\': j += 2
                else: j += 1
                while j < n and src[j] != "'" and src[j] != '\n': j += 1
                i = j + 1; continue
            if template:
                if c == '{': depth += 1
                elif c == '}':
                    if depth == 0: return i + 1
                    depth -= 1
            if not template:
                if c in '({[': stack.append((c, line))
                elif c in ')}]':
                    if not stack or stack[-1][0] != pairs[c]:
                        return -line  # mismatch
                    stack.pop()
            else:
                if c in '([': depth_paren.append(c)
            i += 1
        return i
    depth_paren = []
    r = scan_code(0)
    if isinstance(r, int) and r < 0: return 'mismatch near line %d' % (-r)
    if stack: return 'unclosed %s opened at line %d' % stack[-1]
    return None

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import chg
files = [p for p in chg.changed(os.getcwd()) if p.endswith('.kt')]
bad = 0
for p in files:
    r = check(open(p, encoding='utf-8').read())
    if r:
        print('PROBLEM', p, r); bad += 1
print('checked', len(files), 'files, problems:', bad)
