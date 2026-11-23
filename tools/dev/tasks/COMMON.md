# Tokito Launcher — common brief for every agent (read fully before starting)

S0 = tools/dev   (shared scratchpad, outside the repo)

## Project
"Tokito Launcher" / "Tokito Preview" (version t0.0.3) = Android launcher for Minecraft Java (PojavLauncher engine), a fork of Zalith Launcher 2.
Kotlin 2.4, Jetpack Compose + Material3, Navigation 3, Coil 3, Media3, MMKV-based settings. Package `com.movtery.zalithlauncher`.
GPL-3.0: keep copyright headers; every NEW Kotlin file starts with the same license header as ZalithLauncher/src/main/java/com/movtery/zalithlauncher/tokito/TokiAuthLicense.kt.
Look: "Liquid Glass" + Minecraft pixel feel, red / pink / purple. Helpers: ui/theme/TokitoStyle.kt (`tokitoPanel`, `LauncherStyle`, ...).
The game runs in ANOTHER process (VMActivity, ':game'); the launcher UI runs in the main process.
The user is a Brazilian teenager building the app from a phone: ALL user-facing text must exist in pt-BR AND English.
Existing Tokito code lives in tokito/, ui/components/Tokito*.kt, ui/screens/content/home/, game/launch/ (DeviceProfile, FpsGovernor, TokitoBoot/FastBoot/LogHub...), ui/screens/game/ (TokitoGameSheet, GameBall, loading overlay). Read what you need; the repo is big, search with Grep/Glob.

## Hard constraints
- NOBODY can compile locally (CI only, much later). Write Kotlin like a compiler: check every import, nullability, `when` exhaustiveness, Compose rules (no side effects in composition, remember keys, stable lambdas), and that every API exists for compileSdk 37.2 / Compose BOM 2026.09 / Material3 1.5.0-beta02. If unsure an API exists, grep the repo for an existing usage or use a safer alternative. Never use `Build.VERSION.DEVICE_INITIAL_SDK_INT`. No new Gradle dependencies unless unavoidable (then say so in the report).
- Outbound network from the shell is blocked (no pip/npm/curl/raw TCP). WebSearch / WebFetch tools work for public pages.
- Work ONLY in YOUR git worktree (path in your task, own branch). Use absolute paths. Never touch /home/claude/zalithlauncher2 (main checkout) or other worktrees, never push, never switch branches, never stash/reset.
- Commit on your branch when done AND after each big step (WIP commits are fine): `git add -A && git commit -m "..."` with these trailer lines at the end of the message:
  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_01K2kKHFQA89BwvTXFZpENEZ
- Keep changes focused; do not reformat unrelated code; do not rename things others use.

## Strings
Never edit strings.xml by hand. Put your strings in S0/s0114/part_<prefix>.py:
    S = [('prefix_name', 'English text', 'Texto em português'), ...]     # values XML-escaped: &amp; &lt; \' \n  and %1$s / %1$d placeholders
    REPLACE = {'existing_name': ('en', 'pt')}   # optional, replaces the text of an existing string
    REMOVE = ['old_name']                        # optional, deletes strings that nothing uses any more
Names MUST start with your prefix (collisions between agents break the generator). Generate into your worktree to check resources:
    cd <worktree> && flock /tmp/strings.lock python3 S0/s0114/driver.py
(the driver rebuilds one block from ALL part_*.py files, so other agents' strings appear too — harmless).
BEFORE your final commit restore the generated files so branches merge cleanly (the coordinator regenerates them):
    git checkout -- ZalithLauncher/src/main/res/values/strings.xml ZalithLauncher/src/main/res/values-pt-rBR/strings.xml

## Settings
Do not edit setting/AllSettings.kt. Create your own `object <Name>Settings : SettingsRegistry()` (see setting/SettingsRegistry.kt; `boolSetting/intSetting/stringSetting/floatSetting/enumSetting` are protected helpers; look at how AllSettings declares and uses them: `.state`, `.getValue()`, `.save(v)`).

## Checks (run from the worktree root, BEFORE the final commit, fix everything reported)
    python3 S0/balance.py     # bracket balance of changed .kt files
    python3 S0/imports.py     # unresolved project imports (known false positives: BuildKeys, findUsing, navigateToLogView)
    python3 S0/checkres.py    # every R.string / R.drawable / R.raw used by changed files exists
They compare against the base commit e028ebc, so they also work after you committed. Then re-read your own diff (`git diff e028ebc --stat`, then the files) hunting compile errors.

## Final report (your last message, <= 250 words, English)
What you built; files created/changed; public APIs others must call; what still has to be wired by someone else; risks / honest limits; what the user should verify on the phone. Do not paste code.

## Commit message rule (added 11-23)
The FIRST LINE of every commit message must contain `[skip ci]` (then GitHub does not run the build when the branch is pushed). The user decides when CI runs.
Usage-limit rule: the user is on a small FREE usage limit. Work economically: batch independent tool calls in one message, do not re-read big files, commit finished items early. If you get an HTTP 429 / "5-hour limit" error, stop and report what is committed.
