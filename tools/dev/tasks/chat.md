# Task CHAT — friends + chat on the home screen and inside the game
Worktree: /home/claude/wt/chat (branch wt/chat). String/setting prefix: `chat_` / `TokitoChatSettings`. Read COMMON.md first.

User request (pt-BR): "adicione o Chat para adicionar amigos e poder conversar com eles tanto dentro do jogo tanto na tela inicial".

There is no backend and none can be run, and raw TCP is blocked in your sandbox: you CANNOT test networking, so write defensive, simple, well-reasoned code.

## Architecture (implement this; refine details if you find a better way but keep the spirit)
Serverless chat over a PUBLIC MQTT 3.1.1 broker with TLS, using a small hand-written MQTT client (CONNECT/CONNACK, SUBSCRIBE/SUBACK, PUBLISH QoS0 and QoS1 + PUBACK, PINGREQ/PINGRESP, DISCONNECT, last-will + retained presence) on javax.net.ssl SSLSocket, reader coroutine/thread, no new Gradle dependencies. Default broker list, tried in order (user-editable in Settings): broker.emqx.io:8883, broker.hivemq.com:8883.
- Identity: a random secret FRIEND CODE generated once per install (shown in the UI with copy + share buttons, e.g. TK-7F3K-92QX, >= 60 bits of randomness). Display name = the current launcher account name (AccountsManager current account; fall back to "Player"). Verified badge ✔ when a sender attaches a TokiAuth Original license token (tokito/TokiAuthLicense.kt: tokenFor(name) to attach, verify(name, token) to check; already written, read it).
- Topics (prefix `tokito/v1/`): inbox `u/<id>` with id = first 20 hex of SHA-256("tokito-chat-v1|"+code); presence `p/<id>` (retained, JSON {o,n,t}; LWT sets o=0). Everything published to an inbox is `base64(JSON envelope {v,f(senderId),k(req|acc|rej|msg|del),c(base64 AES-GCM nonce12||ciphertext)})`.
- Add friend = type their code; a `req` goes to their inbox, encrypted with a key derived from the recipient's code (SHA-256("req|"+code)); it carries my name, my code, license. They accept/decline (`acc`/`rej`). After acceptance both derive a pair key SHA-256("pair|"+min(codeA,codeB)+"|"+max(codeA,codeB)) and every `msg` is AES-256-GCM end-to-end (random 12-byte nonce, sender id as AAD) so the public broker and strangers only see ciphertext. Messages/requests from non-friends are dropped silently (requests only appear in the inbox UI). Block / remove friend. Max 300 chars, ~1 msg/s rate limit, dedupe by message id, local timestamps.
- Reliability: reconnect with exponential backoff (cap 60 s), keep-alive pings, offline outbox with retry, QoS1 + persistent session (stable clientId, cleanSession=false) so friends get messages sent while they were briefly offline; show connection state (Offline / Connecting / Online). Presence dot via the retained presence topic.
- Persistence: friends + history in a private JSON file (atomic temp+rename writes, cap history per friend ~300 messages), loaded lazily; never block the main thread.
- Process model: the game runs in another process (VMActivity, ':game'). Connect in only ONE process at a time: main process while the launcher UI is in the foreground, game process while the game runs. Expose `TokitoChat.start(context)` / `stop()` (idempotent) and call them from MainActivity (onStart/onStop; minimal edit) and VMActivity (onCreate/onDestroy). Since two processes may read/write the same JSON, re-read from disk when opening the panel and write only when changed.

## Public contract (other agents code against these exact names)
```kotlin
package com.movtery.zalithlauncher.tokito.chat
enum class ChatConnection { Offline, Connecting, Online }
object TokitoChat { fun start(context: Context); fun stop(); val unread: StateFlow<Int>; val connection: StateFlow<ChatConnection> }
package com.movtery.zalithlauncher.ui.components.chat
@Composable fun TokitoChatPanel(modifier: Modifier = Modifier, compact: Boolean = false, onClose: (() -> Unit)? = null)
@Composable fun TokitoChatDialog(onDismiss: () -> Unit)      // large glass dialog wrapping the panel; used by the home screen
@Composable fun rememberTokitoChatUnread(): State<Int>        // for badges (home tile, side rail, game ball)
```
## UI
Liquid Glass / Minecraft style like the rest of the launcher: friends list with online dots + unread counters, request inbox (accept/decline), conversation screen with bubbles (mine right / theirs left, ✔ for verified), input with send, empty states that explain how to add a friend (share your code), "my code" card, settings (enable chat, broker list, regenerate code with confirmation), clear error/offline states. `compact = true` is the in-game variant (fits the bottom sheet, touch friendly, no big margins). Landscape phone first.
## In-game integration (you own it)
Add a "Chat" tab to the bottom sheet (ui/screens/game/... TokitoGameSheet.kt) using TokitoChatPanel(compact = true) and an unread badge on the flower ball (GameBall) — minimal edits, those two files are only touched by you. The home-screen side (tile, rail entry, TokitoChatDialog placement) is wired by the home agent; do not edit home files.
## Files you own
tokito/chat/**, ui/components/chat/**, TokitoGameSheet.kt + GameBall.kt integration lines, tiny hooks in MainActivity.kt / VMActivity.kt, part_chat.py, TokitoChatSettings.
Honest limits to report: public broker reliability/availability, no server-side moderation, messages only between mutual friends, friend code must be shared out-of-band (Discord/WhatsApp).
