# PolyJav Laucher

Launcher de **Minecraft: Java Edition para Android** (motor PojavLauncher), com visual inspirado no **OneClient** e uma animação ao abrir o Minecraft.

> **Versão Modificada Não Oficial** do [Zalith Launcher 2](https://github.com/ZalithLauncher/ZalithLauncher2).
> Não é afiliada nem endossada pelos desenvolvedores do Zalith Launcher.

## O que muda em relação ao Zalith Launcher 2

- **Nome e identidade:** PolyJav Laucher (`applicationId` próprio: `com.polyjav.laucher`).
- **Tema OneClient (padrão):** cores baseadas no tema "PolyGlass" do OneClient (escuro `#11171C` / `#1A2229`, texto `#D5DBFF`, destaque azul `#2B4BFF`), com variante clara. Em *Configurações > Launcher* ainda dá para trocar para os temas do Zalith.
- **Aba principal:** card fixo com o aviso "Versão Modificada Não Oficial" e o crédito ao OneClient (Polyfrost), com link.
- **Animação ao abrir o Minecraft:** tela cheia com anel girando, brilho pulsando e barra de progresso. A lista de tarefas e o botão de cancelar continuam por cima.
- **Atualizações:** a checagem de atualização do Zalith foi desativada, para o app não oferecer o Zalith oficial como "atualização".

Arquivos novos: `ui/theme/OneClientTheme.kt` e `ui/components/PrestigieLaunchAnimation.kt`.

## Como gerar o APK (sem PC)

1. Suba este projeto para um repositório seu no GitHub.
2. O workflow **Build CI** roda sozinho a cada push (ou em *Actions > Build CI > Run workflow*).
3. Quando terminar, baixe o artefato **debug apk (arm64)** (a maioria dos celulares atuais) em *Actions*.

Para um APK de release assinado, cadastre os segredos `STORE_PASSWORD` e `KEY_PASSWORD` do seu keystore e publique uma *Release*.

## Créditos e licença

- **Zalith Launcher 2**, de MovTery e colaboradores, licença **GPL-3.0** com os termos adicionais da seção 7 (nome diferente, aviso de versão não oficial, avisos de copyright mantidos). Veja `LICENSE` e `README_EN_US.md`.
- **PojavLauncher** e as demais bibliotecas listadas no `README_EN_US.md`, cada uma com sua licença.
- **OneClient**, de Polyfrost (GPL-3.0): apenas os valores de cor do tema foram usados como inspiração; nenhum código ou arte foi copiado.
- **Cryogenic Launcher** (RussoViller) não foi usado: o repositório dele não publica código-fonte e a identidade visual é protegida por licença própria.

Este projeto como um todo é distribuído sob **GPL-3.0**. Ao distribuir o APK, disponibilize também o código-fonte correspondente.

Nota: o código Kotlin e os símbolos JNI continuam no pacote `com.movtery.zalithlauncher` de propósito; mudar isso exigiria alterar também o código nativo.
