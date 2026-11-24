# Tokito Preview t0.0.3 — HANDOFF (read this first in any new chat)

## 1. Context
- User: Brazilian teenager (pt-BR), PHONE ONLY, FREE Claude plan (small 5-hour usage limit). He builds only through GitHub Actions and often just sends "Continue". Answer in short, honest Brazilian Portuguese.
- Project: **Tokito Launcher** = Android launcher for Minecraft Java (PojavLauncher engine), fork of Zalith Launcher 2 (GPL-3.0 + extra terms: different name, "Unofficial Modified Version" notice, keep headers and credits; creators credited "Claude, pedznb").
- Repo: https://github.com/pedrohenriquecoimbra6845-sudo/Tokito-Laucher
  - `main` = stable 0.11.4 (applicationId com.tokito.laucher)
  - `preview` = "Tokito Preview" (com.tokito.preview), last CI-green build = t0.0.2 (run 44969717602)
  - `wip/t003` = t0.0.3 work in progress, pushed with `[skip ci]` so NO build ran. Other `wt/*` branches = parallel workstreams (see section 4).
- Stack: Kotlin 2.4.21, AGP 9.3.0, compileSdk 37.2 (minSdk 26), Compose BOM 2026.09.00, Material3 1.5.0-beta02, Navigation 3, Coil 3, Media3, MMKV settings (`setting/AllSettings.kt` on `SettingsRegistry`: boolSetting/intSetting/stringSetting/enumSetting; `.state`, `.getValue()`, `.save(v)`). Package `com.movtery.zalithlauncher`.
- Nobody can compile locally: CI only (`.github/workflows/push_ci.yml` -> `build.yml`, runs on a push to ANY branch unless the head commit message has `[skip ci]`; 5 matrix variants; artifacts "debug apk (arm64)" etc.). `gh run list/view --repo pedrohenriquecoimbra6845-sudo/Tokito-Laucher` works. Avoid APIs that do not resolve at compileSdk 37.2 (`Build.VERSION.DEVICE_INITIAL_SDK_INT` broke a build once).
- The game runs in another process (`VMActivity`, `:game`); the launcher UI in the main process.

## 2. STANDING RULES from the user
- Do NOT run the Workflow/CI for t0.0.3 until the user says "pode terminar" (his words: "junte tudo que eu mandar e quando eu falar que já pode terminar, junte e jogue para o Workflow"). Until then: commit locally and push WIP only with `[skip ci]` in the first line of the commit message. When he says "pode terminar": merge everything into `preview`, push, poll Build CI with `gh`, fix compile errors until green.
- Version `t0.0.3` / code 3 (already in gradle.properties).
- Commit message trailers: `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>` and `Claude-Session: https://claude.ai/code/session_01K2kKHFQA89BwvTXFZpENEZ`.
- **USAGE LIMIT LESSON (important):** on 11-23 the free 5-hour limit was exhausted twice because the old chat had a ~300k-token context and every tool call re-reads it. Keep chats small: batch independent tool calls in ONE message, never Read whole big files (use offset/limit/Grep), delegate multi-turn work to FRESH sub-agents (general-purpose, small contexts, one git worktree each; never `fork` agents, they inherit the whole chat), and start a NEW chat when the context grows. If a 429 "5-hour limit" appears: stop, save state, tell the user the reset time.

## 3. DONE so far (in `wip/t003`, on top of preview 53cdeab)
- t0.0.2 parts 1+2 (already on `preview`, CI-green): modes + Hz sync option + FPS governor, Benchmark tab with live coloured FPS, flower game ball + bottom sheet menu, loading overlay + fast boot, light-chunk JVM flags.
- t0.0.3 base commit: version bump; **CurseForge** key field in Settings (`TokitoCurseForgeCard`, `UrlManager.curseForgeApiKey()`) + mirror fallback when no key; **Discord soft gate** (`TokitoDiscordGate`, link https://discord.gg/u9zxQQWpwn); **TokiAuth Original license** (see 5).
- Dev tooling in the repo: `tools/dev/` (= "S0" in the task files): `chg.py`, `balance.py`, `imports.py`, `checkres.py`, `s0114/driver.py` + `s0114/part_*.py` (ALL app strings live in these part files; never edit strings.xml by hand; run `python3 tools/dev/s0114/driver.py` from the repo root to regenerate), `tasks/*.md` (briefs), `pano/SPEC.md`, `T003_PLAN.md`.

## 4. PENDING, in recommended order (each has a brief in tools/dev/tasks/; run each as a fresh sub-agent in its own worktree or as its own small chat)
1. **GAME fixes** (`tasks/game.md`): 35 s boot/loading crash, heavy chunks/PVP crash, swipeable left categories in the in-game full menu, Bedrock-style controls.
1b. **MODS in-game** (`tasks/mods-ingame.md`, user request 11-24): enable/disable mods (rename .jar <-> .jar.disabled), download mods/textures/shaders from inside the game, "Aplicar e reiniciar" fast restart (heavy mods take longer; 35 s was only the first-launch goal).
2. **POLISH + perf** (`tasks/polish.md`, item 0 = the user's newest request "melhore tudo do Launcher, o mais detalhado e otimizado possível"): whole-launcher perf + detail pass, 60 Hz lock, global press animations + Liquid Glass polish, Benchmark dimension backdrops (1+ Nether, 60+ Overworld, 100+ End, 200+ Aether), mods/textures tabs layout.
3. **HOME layout** (`tasks/home.md`): "Principais coisas" strip on top, remove pull-down arrow, small PLAY+character card bottom-right (~490 px, slider), customizable photo/video backdrop, single still mini wallpaper with flower-bubble reveal.
4. **PANORAMAS** (`pano/SPEC.md`): six real voxel-world 360° strips (cherry [still default], sakura, cave, snow, pale, bawmc) with BSL-style lighting; engine NOT written yet (old flat art in the scratchpad was rejected by the user: "tem que ser que nem do Minecraft, parecendo que está em um mundo com bloco girando e coloca o Shaders BSL"). Then **integration + BawMC theme** (`tasks/panorama-bawmc.md`).
5. **CHAT** (`tasks/chat.md`): friends + chat on home and in game (public MQTT broker, E2E AES-GCM, friend codes).
6. **SKINS** (`tasks/skins.md`): SkinMC gallery tab + skin bug fixes.
7. Wrap-up: replace `tokito_whatsnew_body` (REPLACE) for t0.0.3 and update the README; ask the user for: BawMC famous player names, his Pix key / payment link (TokiAuth R$ 3), then "pode terminar" -> merge, push `preview`, CI loop.

## 5. TokiAuth Original (paid R$ 3; free for OPedzGoat = the user, admin)
- `tokito/TokiAuthLicense.kt`: license text `TKA1.<base64url payload {n:name lowercase,k:std|admin,i:ts}>.<base64url ECDSA-P256-SHA256 DER signature>`; public key embedded; name-bound; checked on the device (no server). Weak against someone patching the open-source app (honest limit); real enforcement would need a server.
- UI: `TokiAuthLicenseDialog` / `TokiAuthLicenseRow` (ui/components/TokiAuthUi.kt); gate in account creation (AccountElements) and at launch (`LaunchGameOperation.TokiAuthNoLicense` in LauncherElements); label "TokiAuth Original" in AccountUtils.
- Issuing keys: `tools/tokiauth-license.sh <name> [std|admin] <private-key.pem>` or the GitHub workflow "TokiAuth: emitir licença" (needs the repo secret TOKIAUTH_SIGNING_KEY = the private key PEM; the workflow file may live at tools/tokiauth-license.workflow.yml if the push token cannot create workflows; the user then adds it in the GitHub web UI as .github/workflows/tokiauth-license.yml).
- The PRIVATE KEY and OPedzGoat's admin license were sent to the user as files in chat (never commit them, never store them in memory). `PAY_URL` currently = the Discord invite; ask for his Pix key/link to build a real payment screen (Pix "copia e cola" can be generated offline).
- Wording is honest everywhere: TokiAuth Original = verified Tokito account, NOT a Mojang/Microsoft license.

## 6. User requests for t0.0.3 (pt-BR originals are in tools/dev/T003_PLAN.md and the tasks)
Home layout; mini wallpaper still photo; "Principais coisas" on top; remove pull-down arrow; skins from SkinMC/LabyMod; benchmark dimension backdrops; BawMC theme (logo, famous player heads, layout); real Minecraft-style panoramas with BSL shaders (Sakura, Cave, Cherry Blossom [still default], Snow, Pale Garden, BawMC); flower-button categories swipe; bugs: CurseForge no mods, launcher laggy, 35 s loading crash, heavy chunks/PVP crashes; chat with friends (home + in game); TokiAuth Original R$ 3 (OPedzGoat free); Liquid Glass polish + press animations; Bedrock-style controls; mods/textures tabs with photos; "melhore tudo do Launcher… o mais otimizado possível".

## 7. Technical notes worth keeping
- Home architecture today: `LauncherScreen.kt` Row { `TokitoHero` weight 6 | "Principais coisas" HomeGrid weight 4 | `TokitoPullDown` }; `TokitoHero.kt` (grid backdrop, `PetModelView` 3D skin, `TokitoLaunchBar`, `TokitoPlayerChip`, `TokitoModeChip`); `TokitoChrome.kt` = left rail. Wallpapers: `LauncherWallpaper` enum in `ui/theme/TokitoStyle.kt` (raw images + thumbs) copied by `BackgroundViewModel.importWallpaper`; `Background` composable in `LauncherElements.kt`. Launcher density scaling in `ui/theme/Theme.kt`.
- In-game full menu: `GameMenuSubscreen` = `DualMenuSubscreen` with left tabs + `HorizontalPager(userScrollEnabled=false)` -> enable swipe + keep tabs in sync.
- Press animation plan: custom `IndicationNodeFactory` (DrawModifierNode + Animatable scale ~0.95) via `LocalIndication` inside the MaterialTheme content. 60 Hz plan: `window.attributes.preferredRefreshRate`/preferred display mode for the launcher activity only.
- SkinMC (skinmc.net): `/api/v1/renders/skins/<uuid>/skin` (PNG), `/full` (2D render), `/api/v1/skins/uuid/<playerUUID>`; list HTML `/skins`, `/skins/tagged/<slug>?latest=1|random=1|today=1|this_week=1|this_month=1&cursor=`, skin links `/skins/<uuid>`. Existing `TokitoSkinSearch` only uses Mojang name lookup.
- BawMC: server IP `bawmc.net`; logo/wallpaper must be downloaded at RUNTIME (cache), generated fallback offline; famous player names unknown -> ask the user (OPedzGoat is the admin).
- CurseForge: key from build secret CURSEFORGE_API_KEY or the in-app field `tokitoCurseForgeKey`; MCIM mirror fallback (unverified).
- Panorama render design (cylindrical 360° strip 4096x704, DDA voxel ray caster in C, soft shadows, AO, god rays, bloom) is in `pano/SPEC.md`; in-app `PanoramaView` draws the strip twice with a slow horizontal scroll (~120-180 s/turn), still for cherry.
- Check scripts compare against base commit e028ebc (env TOKITO_BASE); worktree-friendly (env TOKITO_REPO).

## 8. Usage-limit log (why chats stall)
- 11-23 and 11-24: the free 5-hour limit was hit again on the first turn after each long pause. After a pause the chat context (~300k tokens) is re-read at FULL price, which alone uses most of a window; sub-agents then fail instantly with HTTP 429. THE OLD CHAT MUST NOT BE USED ANY MORE: the user should open a NEW chat/session on the repo (branch `wip/t003`) and say "Continue a t0.0.3, leia tools/dev/HANDOFF.md". In a small chat, steps cost ~10x less.
- Worktrees from the old container (/home/claude/wt/*) are gone in a new container; their branches were never pushed (no work was lost: the agents never started). Recreate with `git worktree add`.
- `gh` token (GH_TOKEN) was reported invalid on 11-23; `git push` still worked. If `gh run list` fails, re-attach the repo with push access (add_repo) or poll CI another way.
