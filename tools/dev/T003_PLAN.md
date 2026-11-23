# Tokito Preview t0.0.3 — working plan (authoritative; re-read after any context compaction)

Repo: /home/claude/zalithlauncher2 (branch `preview`, applicationId com.tokito.preview, version t0.0.3 / code 3)
Scratchpad: tools/dev  (S0 below)

## Standing rules
- User (pt-BR, phone only): NOTHING goes to the Workflow/CI until he says "pode terminar". We only commit locally (main agent commits; forks DO NOT commit/push/stash/checkout).
- Commit trailers: `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>` and `Claude-Session: https://claude.ai/code/session_01K2kKHFQA89BwvTXFZpENEZ`.
- Cannot compile locally (CI only). Be extra careful: imports, API existence at compileSdk 37.2 / Kotlin 2.4.21 / Compose BOM 2026.09 / Material3 1.5.0-beta02, no new Gradle dependencies unless unavoidable.
- Strings: only through `S0/s0114/part_<feature>.py` files (format `S=[(name, en, pt), ...]`, values XML-escaped: `&amp;`, `\'`, `\n`, `%1$s`), then run `flock /tmp/strings.lock python3 S0/s0114/driver.py` (idempotent; regenerates one block from ALL part_*.py). Never edit strings.xml by hand.
- Local checks (run from repo root): `python3 S0/balance.py`, `python3 S0/imports.py` (known false positive: BuildKeys), `python3 S0/checkres.py`.
- Files are shared with other agents working in parallel: re-read a file right before editing it, make minimal Edit calls, never rewrite a whole file that you do not own.
- All user-facing text in Portuguese (pt-BR) + English; app is "Tokito Preview"; style = Liquid Glass / Minecraft, red-pink-purple palette, `tokitoPanel` / `LauncherStyle` helpers in ui/theme/TokitoStyle.kt.
- License header (GPL-3.0 Tokito header, copy from any file in tokito/) on every new Kotlin file.

## User requests for t0.0.3 (translated)
1. Home: PLAY button + character + background -> bottom-right corner, small (~490 px), background under the character customizable (photo/video); Mini Wallpaper = one still customizable photo near the middle where PLAY was; "Principais coisas" on top; remove the pull-down arrow; Skins category from SkinMC/LabyMod (see + apply), beautiful tab.
2. Benchmark: coloured background under the FPS: 1+ Nether, 60+ Overworld, 100+ End, 200+ Aether.
3. BawMC theme: logo, heads of famous players, layout; BawMC panorama.
4. Panoramas: real Minecraft title-screen style (camera inside a world of blocks, rotating) with BSL-shader look; Sakura + Minecraft (Cave, Cherry Blossom [still default], Snow, Pale Garden) + BawMC; remove other wallpapers.
5. Flower button in game: left categories of the full menu swipe horizontally.
6. Bugs: CurseForge no mods, launcher laggy/freezes, 35 s loading crashes game, chunks too heavy (crash in PVP).
7. (21:05) CHAT: add friends + talk to them, on the home screen AND inside the game.
8. (21:05) TokiAuth as "Original" but paid R$ 3; player OPedzGoat (the user, admin) free.
9. Pending from t0.0.2: Liquid Glass polish + press animations, 60 Hz lock, mods/textures tabs with photos + horizontal drag, Bedrock-style controls, Discord gate (soft, done), CurseForge key (done).

## Contracts between workstreams

### TokiAuthLicense (main agent writes; chat agent consumes) — tokito/TokiAuthLicense.kt
```kotlin
object TokiAuthLicense {
    const val PRICE_BRL = 3
    fun isLicensed(name: String): Boolean          // a valid saved license exists for this player name
    fun tokenFor(name: String): String?            // the saved token (to send in chat so others can verify)
    fun verify(name: String, token: String?): Boolean   // signature check only (ECDSA P-256), name-bound
}
```
Licence = `TKA1.<base64url(payload json {n,k,i})>.<base64url(ECDSA-SHA256 DER signature)>`; name-bound (case-insensitive). Admin issues via GitHub workflow `tokiauth-license.yml` (secret TOKIAUTH_SIGNING_KEY). OPedzGoat gets an `admin` licence (free).
Honest wording: TokiAuth "Original" = verified Tokito account (NOT a Mojang/Microsoft licence).

### Panorama assets (pano agent writes; main agent integrates) — S0/pano/final/
`pano_{cherry,sakura,cave,snow,pale,bawmc}.webp` 4096x704 cylindrical 360° strips, seamless in x (column 0 continues column 4095), vertical coverage given in manifest; `thumb_<name>.webp` 480x270; `manifest.json`; `preview_<name>.png` 2048x352.

### Chat (chat agent) — package com.movtery.zalithlauncher.tokito.chat + ui/components/chat
```kotlin
object TokitoChat { fun start(context: Context); fun stop(); val unread: StateFlow<Int>; val connection: StateFlow<ChatConnection> }
@Composable fun TokitoChatPanel(modifier: Modifier = Modifier, compact: Boolean = false, onClose: (() -> Unit)? = null)
@Composable fun TokitoChatDialog(onDismiss: () -> Unit)      // used from the home screen (main agent wires it)
@Composable fun rememberTokitoChatUnread(): State<Int>      // badge counters (home tile, rail, game ball)
```
The chat agent itself wires the IN-GAME side (TokitoGameSheet chat tab, GameBall unread badge, start/stop in VMActivity); main agent wires the HOME side.

### SkinMC gallery (skin agent) — tokito/TokitoSkinMc.kt + ui/components/TokitoSkinsUi.kt (+ TokitoSkins.kt bug fixes)
Gallery with tag chips + sort (latest/popular/random/today/week/month), paged grid of renders, preview dialog (3D/2D), "Usar" applies through the existing skin library to the Offline/TokiAuth account. Beautiful.
