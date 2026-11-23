"""Shared helpers for the local check scripts: repo root (works in git worktrees) and the files changed vs the t0.0.3 base."""
import os, subprocess

DEFAULT_ROOT = '/home/claude/zalithlauncher2'
BASE = os.environ.get('TOKITO_BASE', 'e028ebc')


def root():
    r = os.environ.get('TOKITO_REPO')
    if r:
        return r
    try:
        out = subprocess.run(['git', 'rev-parse', '--show-toplevel'], capture_output=True, text=True)
        if out.returncode == 0 and out.stdout.strip():
            return out.stdout.strip()
    except Exception:
        pass
    return DEFAULT_ROOT


def changed(repo):
    """relative paths of files that exist and changed vs BASE (committed or not) or are untracked"""
    names = set()
    st = subprocess.run(['git', 'status', '--short', '--untracked-files=all'], cwd=repo, capture_output=True, text=True).stdout.splitlines()
    for l in st:
        if 'D' not in l[:2]:
            names.add(l[3:].strip().strip('"'))
    df = subprocess.run(['git', 'diff', '--name-only', '--diff-filter=d', BASE], cwd=repo, capture_output=True, text=True).stdout.split()
    names |= set(df)
    return sorted(n for n in names if os.path.exists(os.path.join(repo, n)))
