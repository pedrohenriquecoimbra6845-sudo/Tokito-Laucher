# Tokito Laucher

Launcher de **Minecraft: Java Edition para Android** (motor PojavLauncher), com visual próprio em vermelho, rosa e roxo, menu lateral com **mods, texturas e modpacks**, tela de entrada nova e atalho para o **Minecraft Rust Edition**.

Versão atual: **1.11.2**

> **Versão Modificada Não Oficial** do [Zalith Launcher 2](https://github.com/ZalithLauncher/ZalithLauncher2).
> Não é afiliada nem endossada pelos desenvolvedores do Zalith Launcher.

## O que muda em relação ao Zalith Launcher 2

- **Nome e identidade:** Tokito Laucher (`applicationId` próprio: `com.tokito.laucher`). Instala separado do Zalith e dos apps antigos (PolyJav / Prestigie).
- **Ícone:** flor recolorida em vermelho, rosa e roxo (ícone adaptativo, com versão monocromática para o tema do Android). O nome do app aparece **ao lado direito do ícone** na barra de cima, na tela de entrada e na tela de carregamento.
- **Layout novo:**
  - **Menu lateral** (ícone no topo): Início, Versões, Baixar, **Mods**, **Texturas**, **Modpacks**, Rust Edition e Ajustes.
  - **Barra de cima** com nome e versão do launcher, seletor de versão, seletor de conta, pasta do jogo e multiplayer.
  - **Início:** personagem 3D (sua skin) sobre um chão em grade, botão grande **JOGAR** com lista de versões e, à direita, o painel de **Novidades** com a Biblioteca (Mods, Texturas, Modpacks, Shaders, Mundos) e os cards.
  - O layout usa só a *ideia* de organização de launchers modernos; nenhuma arte, texto ou código de outro launcher foi copiado.
- **Tema Tokito Rosa (padrão):** fundo ameixa escuro com destaques rosa, roxo e vermelho, com variante clara. Em *Configurações > Launcher* dá para trocar para o Ciano, o OneClient e os temas do Zalith.
- **Nova forma de entrar:** toda vez que o launcher abre, o ícone gira, o nome aparece ao lado e:
  - com conta logada: "Bem-vindo de volta, <nome>" e o launcher abre sozinho (ou ao tocar);
  - sem conta: botões **Entrar com Microsoft**, **Outras formas de entrar** e **Entrar sem conta**.
- **Minecraft Rust Edition:** card e item no menu lateral. É um app **separado** (port nativo do Minecraft: Java Edition escrito em Rust, projeto Starling, antigo Horizon Android, licença MIT). Ele **não** vira parte do launcher: o Tokito detecta se está instalado, abre o app e, se não estiver, leva para a página de download. Você precisa da sua própria cópia do Minecraft: Java Edition.
- **Animação ao abrir o Minecraft:** tela cheia com anel girando, brilho pulsando e barra de progresso. A lista de tarefas e o botão de cancelar continuam por cima.
- **Aba principal:** card fixo com o aviso "Versão Modificada Não Oficial" e o crédito ao OneClient (Polyfrost).
- **Atualizações:** a checagem de atualização do Zalith foi desativada, para o app não oferecer o Zalith oficial como "atualização".

Arquivos novos: `ui/theme/TokitoTheme.kt`, `ui/screens/main/TokitoChrome.kt` (menu lateral e barra de cima), `ui/screens/content/home/TokitoHero.kt` (tela inicial), `ui/screens/content/home/TokitoHomeCards.kt` (Biblioteca e Rust Edition), `ui/components/TokitoEntryOverlay.kt` (tela de entrada) e `ui/components/PrestigieLaunchAnimation.kt`.

## Como gerar o APK (sem PC)

1. Suba este projeto para um repositório seu no GitHub.
2. O workflow **Build CI** roda sozinho a cada push (ou em *Actions > Build CI > Run workflow*).
3. Quando terminar, baixe o artefato **debug apk (arm64)** (a maioria dos celulares atuais) em *Actions*.

Para um APK de release assinado, cadastre os segredos `STORE_PASSWORD` e `KEY_PASSWORD` do seu keystore e publique uma *Release*.

## Créditos e licença

- **Zalith Launcher 2**, de MovTery e colaboradores, licença **GPL-3.0** com os termos adicionais da seção 7 (nome diferente, aviso de versão não oficial, avisos de copyright mantidos). Veja `LICENSE` e `README_EN_US.md`.
- **PojavLauncher** e as demais bibliotecas listadas no `README_EN_US.md`, cada uma com sua licença.
- **Minecraft Rust Edition / Starling** (Horizon Android), de Vasysik e JuanLuci, licença MIT: é um app separado; o Tokito só abre o app ou o link de download.
- **OneClient**, de Polyfrost (GPL-3.0): apenas os valores de cor do tema OneClient (opcional) foram usados como inspiração; nenhum código ou arte foi copiado.
- **Cryogenic Launcher** (RussoViller) não foi usado: o repositório dele não publica código-fonte e a identidade visual é protegida por licença própria.
- **Arte do ícone:** a flor foi feita a partir da imagem enviada pelo dono do projeto e recolorida. Se essa imagem veio de um banco de imagens ou de outro autor, confira a licença antes de publicar o app.

Este projeto como um todo é distribuído sob **GPL-3.0**. Ao distribuir o APK, disponibilize também o código-fonte correspondente.

Nota: o código Kotlin e os símbolos JNI continuam no pacote `com.movtery.zalithlauncher` de propósito; mudar isso exigiria alterar também o código nativo.
