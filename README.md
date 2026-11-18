# Tokito Launcher

Launcher de **Minecraft: Java Edition para Android** (motor PojavLauncher), com visual próprio em vermelho, rosa e roxo, vários estilos de launcher, **modos de otimização** para ganhar fluidez, **pop-ups com fotos e vídeos** na tela inicial, biblioteca de **skins** e a conta própria **TokiAuth**.

Versão atual: **0.11.4**

Criadores: **Claude, pedznb**

> **Versão Modificada Não Oficial** do [Zalith Launcher 2](https://github.com/ZalithLauncher/ZalithLauncher2).
> Não é afiliada nem endossada pelos desenvolvedores do Zalith Launcher.

## Novidades da 0.11.4

- **Modos de otimização:** Desligado, **PVP**, **Survival** e **Brutal**. Dá para escolher no card *Modos de otimização* da tela inicial, no atalho ao lado do botão JOGAR e em *Ajustes*.
  - **PVP:** trava o FPS na maior taxa de atualização da tela (v-sync desligado), pede desempenho sustentado ao celular, diminui um pouco o DPI do launcher e deixa o boneco menor.
  - **Survival:** chunks rápidas e leves (distância de renderização limitada a 10, menos detalhes de entidades e partículas) e cache de shaders maior, para jogar e explorar por horas.
  - **Brutal:** junta tudo dos outros modos **sem** limitar a distância de renderização, para manter muitas chunks jogáveis. O porém: o jogo é renderizado em 80% da resolução, as distâncias de simulação e de entidades e as partículas diminuem e as nuvens ficam desligadas (imagem mais simples e celular mais quente).
  - As opções do jogo que um modo muda voltam ao normal quando você o desliga.
  - **Importante:** quantos FPS cada modo ganha depende do celular. Não existe um número fixo garantido (nem "5 em 5 FPS", nem "nunca cair de 10 FPS"): o objetivo é dar mais fluidez e evitar quedas bruscas, e isso precisa ser testado no seu aparelho.
- **Chunks leves e rápidas:** chave própria (já faz parte do Survival e do Brutal). Chunks mais baratas de montar (sem iluminação suave, mistura de biomas pequena, gráficos rápidos, sem sombras de entidades), distância de simulação menor e chunks atualizadas em segundo plano, pensando em celulares de geração passada.
- **Personagem da tela de jogar** não fica mais cortado em cima e embaixo e ficou menor.
- **Painel *Principais coisas*** (antigo painel de Novidades, no canto direito da tela inicial): toque no lápis para **mostrar, esconder e reordenar** Biblioteca, Modos de otimização, Skins, Pop-ups e Novidades do jeito que quiser. O aviso de versão não oficial sempre fica no fim (a licença exige).
- **Pop-ups com fotos e vídeos** na tela inicial (como mini wallpapers flutuantes): até 6 pop-ups, arraste para mover, 3 tamanhos, X para fechar até abrir o app de novo. As fotos são reduzidas para no máximo 1280 px. Os vídeos (até 60 MB) tocam **mudos, em loop, só enquanto a tela inicial está aberta e no máximo 2 ao mesmo tempo**; os outros mostram uma imagem parada, e um toque pausa ou toca.
- **Skins:** biblioteca de skins PNG 64x64 (um PNG ou um ZIP com vários), com pré-visualização de frente. Dá para usar uma skin na conta **Offline** ou **TokiAuth** e guardar a skin atual da conta. Contas online usam a skin do próprio serviço.
- **TokiAuth:** conta própria do Tokito Launcher. Fica sempre ativa neste aparelho (sem login, sem internet e sem expirar). É uma conta do tipo offline: servidores que verificam o login online (premium) não a aceitam.
- **Aviso de atualização:** depois de atualizar, a janela **Novidades** abre uma vez (não abre numa instalação nova), com um botão para **compartilhar** um texto pronto avisando os outros jogadores. Veja *Como avisar todos os jogadores*.
- **JNA:** ajustes para melhorar a compatibilidade em celulares de outras marcas (veja *Notas*).
- **Removidos:** Minecraft Rust Edition e a opção de sincronização vertical (v-sync) do Zink.
- **Nome corrigido** para **Tokito Launcher**, com os criadores *Claude, pedznb* na aba Sobre e no card da tela inicial.

## O que o Tokito tem em relação ao Zalith Launcher 2

- **Nome e identidade:** Tokito Launcher (`applicationId` próprio: `com.tokito.laucher`). Instala separado do Zalith e dos apps antigos (PolyJav / Prestigie).
- **Ícone:** flor recolorida em vermelho, rosa e roxo (ícone adaptativo, com versão monocromática para o tema do Android). O nome do app aparece **ao lado direito do ícone** na barra de cima, na tela de entrada e na tela de carregamento.
- **Layout novo:**
  - **Menu lateral:** Início, Versões, Baixar, **Mods**, **Texturas**, **Modpacks** e Ajustes.
  - **Barra de cima** com nome e versão do launcher, seletor de versão, seletor de conta, pasta do jogo e multiplayer.
  - **Início:** personagem 3D (sua skin) sobre um chão em grade, botão grande **JOGAR** com lista de versões e, à direita, o painel **Principais coisas**.
  - O layout usa só a *ideia* de organização de launchers modernos; nenhuma arte, texto ou código de outro launcher foi copiado.
- **Estilos do launcher:** Tokito, Liquid Glass (modos Normal, Leve e Frio), Transparente, Neon, Minecraft e Zalith, com wallpaper combinando e controles do jogo no mesmo estilo.
- **Pacotes ZIP:** importe um ZIP com setas do mouse, wallpapers e HUD de controle.
- **Tema Tokito Rosa (padrão):** fundo ameixa escuro com destaques rosa, roxo e vermelho, com variante clara. Em *Ajustes* dá para trocar para o Ciano, o OneClient e os temas do Zalith.
- **Nova forma de entrar:** toda vez que o launcher abre, o ícone gira, o nome aparece ao lado e:
  - com conta logada: "Bem-vindo de volta, <nome>" e o launcher abre sozinho (ou ao tocar);
  - sem conta: botões **Entrar com Microsoft**, **Outras formas de entrar** e **Entrar sem conta**.
- **Animação ao abrir o Minecraft:** tela cheia com anel girando, brilho pulsando e barra de progresso. A lista de tarefas e o botão de cancelar continuam por cima.
- **Aba principal:** card fixo com o aviso "Versão Modificada Não Oficial", os criadores e o crédito ao OneClient (Polyfrost).
- **Atualizações:** a checagem de atualização do Zalith foi desativada, para o app não oferecer o Zalith oficial como "atualização".

Arquivos novos da 0.11.4: `tokito/TokitoPopups.kt`, `tokito/TokitoSkins.kt`, `tokito/TokitoNotice.kt`, `game/launch/OptimizationProfile.kt`, `game/launch/JnaNatives.kt`, `setting/enums/OptimizationMode.kt`, `ui/components/TokitoOptimizationUi.kt`, `ui/components/TokitoPopupLayer.kt`, `ui/components/TokitoSkinsUi.kt`, `ui/components/TokitoNoticeUi.kt`, `ui/screens/content/home/HomeMainItems.kt` e `ui/screens/content/home/TokitoPanelCards.kt`.

## Como avisar todos os jogadores

1. **Dentro do app:** quem atualiza para uma versão nova vê a janela **Novidades** uma vez. O mesmo texto fica sempre no card *Novidades* do painel, com o botão **Compartilhar** (abre o menu do celular com uma mensagem pronta).
2. **Aviso remoto (opcional):** o launcher lê o arquivo `tokito-update.json` deste repositório (no máximo a cada 6 horas e só com internet) e mostra o aviso uma vez para cada número de `id`. Para avisar todo mundo:
   - edite `tokito-update.json` no GitHub, troque `id` por um número **maior que o último usado** (1, 2, 3...) e escreva `title` e `message` (pode ser texto simples ou um objeto com `pt` e `en`);
   - se for aviso de versão nova, preencha também `versionCode` (o `launcher_version_code` do `gradle.properties`) e `versionName`; quem já está nessa versão ou numa mais nova não vê o aviso;
   - se quiser um botão de link, preencha `url` (precisa começar com `https://`);
   - faça o commit na branch `main`.
   - Limites: só chega em quem já tem a **0.11.4 ou mais nova** (as versões antigas não têm esse recurso) e só funciona com o repositório **público**.
3. **Texto pronto para colar** (Discord, WhatsApp...):

   > O Tokito Launcher 0.11.4 chegou! Novidades: modos de otimização (PVP, Survival e Brutal), chunks leves e rápidas, painel Principais coisas personalizável, pop-ups com fotos e vídeos, biblioteca de skins e a conta própria TokiAuth. Atualize o seu launcher! Criadores: Claude, pedznb.

## Notas

- **JNA em outras marcas de celular:** o launcher agora decide se o processo é 32 ou 64 bits pelo próprio processo (não pelo modelo do aparelho), escolhe a biblioteca nativa do JNA mais próxima da versão que o jogo pede, confere e repara os arquivos antes de abrir o jogo e define `jna.nosys=true` para não usar uma biblioteca do sistema. Não foi possível reproduzir o problema em aparelhos reais, então, se ele continuar numa marca específica, envie o log do launcher e o texto do erro (marca e modelo do celular ajudam).
- **Compilação:** o projeto só é compilado no GitHub Actions. Se o *Build CI* acusar erro, o texto do erro mostra o arquivo e a linha para corrigir.

## Como gerar o APK (sem PC)

1. Suba este projeto para um repositório seu no GitHub.
2. O workflow **Build CI** roda sozinho a cada push (ou em *Actions > Build CI > Run workflow*).
3. Quando terminar, baixe o artefato **debug apk (arm64)** (a maioria dos celulares atuais) em *Actions*.

Para um APK de release assinado, cadastre os segredos `STORE_PASSWORD` e `KEY_PASSWORD` do seu keystore e publique uma *Release*.

## Créditos e licença

- **Zalith Launcher 2**, de MovTery e colaboradores, licença **GPL-3.0** com os termos adicionais da seção 7 (nome diferente, aviso de versão não oficial, avisos de copyright mantidos). Veja `LICENSE` e `README_EN_US.md`.
- **PojavLauncher** e as demais bibliotecas listadas no `README_EN_US.md`, cada uma com sua licença.
- **OneClient**, de Polyfrost (GPL-3.0): apenas os valores de cor do tema OneClient (opcional) foram usados como inspiração; nenhum código ou arte foi copiado.
- **Cryogenic Launcher** (RussoViller) não foi usado: o repositório dele não publica código-fonte e a identidade visual é protegida por licença própria.
- **Arte do ícone:** a flor foi feita a partir da imagem enviada pelo dono do projeto e recolorida. Se essa imagem veio de um banco de imagens ou de outro autor, confira a licença antes de publicar o app.
- **Tokito Launcher:** criado por **Claude, pedznb**.

Este projeto como um todo é distribuído sob **GPL-3.0**. Ao distribuir o APK, disponibilize também o código-fonte correspondente.

Nota: o código Kotlin e os símbolos JNI continuam no pacote `com.movtery.zalithlauncher` de propósito; mudar isso exigiria alterar também o código nativo.
