# "Principais coisas" panel, pop-ups, skins, news and credits (0.11.4)
REMOVE = ['tokito_news_title']

S = [
 # panel
 ('tokito_main_title', 'Main things', 'Principais coisas'),
 ('tokito_main_customize', 'Customize', 'Personalizar'),
 ('tokito_main_customize_title', 'Customize Main things', 'Personalizar Principais coisas'),
 ('tokito_main_customize_summary',
  'Choose what shows in this panel and in which order. The unofficial version notice always stays at the end.',
  'Escolha o que aparece neste painel e a ordem. O aviso de versão não oficial sempre fica no fim.'),
 ('tokito_main_customize_reset', 'Restore default', 'Restaurar o padrão'),
 ('tokito_main_item_library', 'Library (mods, textures, modpacks)', 'Biblioteca (mods, texturas, modpacks)'),
 ('tokito_main_item_modes', 'Optimization modes', 'Modos de otimização'),
 ('tokito_main_item_skins', 'Skins', 'Skins'),
 ('tokito_main_item_popups', 'Pop-ups with photos and videos', 'Pop-ups com fotos e vídeos'),
 ('tokito_main_item_news', 'News', 'Novidades'),
 ('tokito_open', 'Open', 'Abrir'),

 # modes card
 ('tokito_modes_card_title', 'Optimization modes', 'Modos de otimização'),
 ('tokito_modes_more', 'More options', 'Mais opções'),

 # skins
 ('tokito_skins_card_title', 'Skins', 'Skins'),
 ('tokito_skins_card_summary',
  'Keep your skins and use them on the Offline or TokiAuth account.',
  'Guarde suas skins e use na conta Offline ou TokiAuth.'),
 ('tokito_skins_title', 'Skins', 'Skins'),
 ('tokito_skins_summary',
  'Add 64x64 PNG skins (one PNG file or a ZIP with several). They work on Offline and TokiAuth accounts. Online accounts use the skin of their own service.',
  'Adicione skins PNG de 64x64 (um arquivo PNG ou um ZIP com várias). Elas valem para contas Offline e TokiAuth. Contas online usam a skin do próprio serviço.'),
 ('tokito_skins_account', 'Current account: %1$s', 'Conta atual: %1$s'),
 ('tokito_skins_account_online',
  'The current account is online. Use an Offline or TokiAuth account to change skins here.',
  'A conta atual é online. Use uma conta Offline ou TokiAuth para trocar de skin aqui.'),
 ('tokito_skins_no_account',
  'No account selected. Create or pick an Offline or TokiAuth account.',
  'Nenhuma conta selecionada. Crie ou escolha uma conta Offline ou TokiAuth.'),
 ('tokito_skins_add', 'Add skin (PNG or ZIP)', 'Adicionar skin (PNG ou ZIP)'),
 ('tokito_skins_save_current', 'Keep the current skin', 'Guardar a skin atual'),
 ('tokito_skins_use', 'Use', 'Usar'),
 ('tokito_skins_delete', 'Delete skin', 'Apagar skin'),
 ('tokito_skins_empty',
  'The library is empty. Add a skin or keep the current one.',
  'A biblioteca está vazia. Adicione uma skin ou guarde a skin atual.'),
 ('tokito_skins_added', '%1$d skin(s) added.', '%1$d skin(s) adicionada(s).'),
 ('tokito_skins_ignored',
  '%1$d file(s) skipped: old format (64x32), invalid or library full.',
  '%1$d arquivo(s) ignorado(s): formato antigo (64x32), inválido ou biblioteca cheia.'),
 ('tokito_skins_none_valid',
  'No valid skin found. Use 64x64 PNG images.',
  'Nenhuma skin válida encontrada. Use imagens PNG de 64x64.'),
 ('tokito_skins_applied', 'Skin applied to the account.', 'Skin aplicada à conta.'),
 ('tokito_skins_apply_failed', 'Could not apply the skin.', 'Não foi possível aplicar a skin.'),
 ('tokito_skins_saved', 'Skin kept in the library.', 'Skin guardada na biblioteca.'),
 ('tokito_skins_save_failed',
  'Could not keep the skin (the account may have no skin or the library is full).',
  'Não foi possível guardar a skin (a conta pode não ter skin ou a biblioteca está cheia).'),

 # pop-ups
 ('tokito_popups_card_title', 'Home screen pop-ups', 'Pop-ups da tela inicial'),
 ('tokito_popups_card_summary',
  'Floating photos and videos on the home screen. Drag to move.',
  'Fotos e vídeos flutuantes na tela inicial. Arraste para mover.'),
 ('tokito_popups_enabled', 'Show pop-ups', 'Mostrar pop-ups'),
 ('tokito_popups_card_count', '%1$d of %2$d pop-ups', '%1$d de %2$d pop-ups'),
 ('tokito_popups_manage', 'Manage', 'Gerenciar'),
 ('tokito_popups_title', 'Home screen pop-ups', 'Pop-ups da tela inicial'),
 ('tokito_popups_summary',
  'Photos and videos that float on the home screen. Drag a pop-up to move it, tap the X to close it until the app is opened again, and tap a video to pause or play it. Videos play muted, in a loop, only while the home screen is open (at most 2 at the same time).',
  'Fotos e vídeos que flutuam na tela inicial. Arraste um pop-up para mover, toque no X para fechar até abrir o app de novo e toque num vídeo para pausar ou tocar. Os vídeos tocam mudos, em loop e só enquanto a tela inicial está aberta (no máximo 2 ao mesmo tempo).'),
 ('tokito_popups_add_photo', 'Add photo', 'Adicionar foto'),
 ('tokito_popups_add_video', 'Add video', 'Adicionar vídeo'),
 ('tokito_popups_empty', 'No pop-ups yet.', 'Nenhum pop-up ainda.'),
 ('tokito_popups_photo', 'Photo', 'Foto'),
 ('tokito_popups_video', 'Video', 'Vídeo'),
 ('tokito_popups_size_small', 'Small', 'Pequeno'),
 ('tokito_popups_size_medium', 'Medium', 'Médio'),
 ('tokito_popups_size_large', 'Large', 'Grande'),
 ('tokito_popups_show_again', 'Show again', 'Mostrar de novo'),
 ('tokito_popups_remove', 'Remove pop-up', 'Remover pop-up'),
 ('tokito_popups_added', 'Pop-up added.', 'Pop-up adicionado.'),
 ('tokito_popups_full',
  'Limit of %1$d pop-ups reached. Remove one to add another.',
  'Limite de %1$d pop-ups atingido. Remova um para adicionar outro.'),
 ('tokito_popups_too_big', 'The video is too big (60 MB limit).', 'O vídeo é grande demais (limite de 60 MB).'),
 ('tokito_popups_failed', 'Could not add this file.', 'Não foi possível adicionar este arquivo.'),
 ('tokito_popup_close', 'Close', 'Fechar'),

 # news, announcement and share
 ('tokito_news_card_title', 'News', 'Novidades'),
 ('tokito_news_card_summary', 'What changed in version %1$s.', 'O que mudou na versão %1$s.'),
 ('tokito_news_card_button', 'See news', 'Ver novidades'),
 ('tokito_whatsnew_title', 'What is new in version %1$s', 'Novidades da versão %1$s'),
 ('tokito_whatsnew_close', 'Got it', 'Entendi'),
 ('tokito_whatsnew_share', 'Share', 'Compartilhar'),
 ('tokito_whatsnew_share_chooser', 'Tell other players', 'Avisar outros jogadores'),
 ('tokito_whatsnew_body',
  '• Tokito Preview t0.0.1 is the test version: it installs next to the stable Tokito Launcher and may have bugs.\\n'
  '• Optimization modes now work on the launcher, not inside the game: FPS limiter in steps of 10 that goes down when the phone is hot, device benchmark, screen at the same Hz as the FPS, nothing written in the options of the game.\\n'
  '• Survival: calibrates the FPS and drops to 10 FPS in the background. Brutal: everything plus light and fast chunks (JVM tuned against freezes when a chunk loads).\\n'
  '• Pull down the tab in the top right corner of the home screen: Main things and pop-ups.\\n'
  '• Skins: new Search category (find the skin of a player by name) and a filter in your library.\\n'
  '• TokiAuth now has a password (asked before the game opens) and a link to the TokiAuth site.\\n'
  '• Faster screen changes and a shorter opening animation.\\n'
  '• Creators: Claude, pedznb.',
  '• O Tokito Preview t0.0.1 é a versão de teste: instala ao lado do Tokito Launcher estável e pode ter bugs.\\n'
  '• Os modos de otimização agora trabalham no launcher, não dentro do jogo: limitador de FPS de 10 em 10 que desce quando o celular esquenta, benchmark do aparelho, tela no mesmo Hz do FPS, nada escrito nas opções do jogo.\\n'
  '• Survival: calibra o FPS e cai para 10 FPS em segundo plano. Brutal: tudo mais chunks leves e rápidas (JVM ajustada contra travadas quando uma chunk carrega).\\n'
  '• Puxe para baixo a aba no canto superior direito da tela inicial: Principais coisas e pop-ups.\\n'
  '• Skins: nova categoria Pesquisar (ache a skin de um jogador pelo nome) e filtro na sua biblioteca.\\n'
  '• O TokiAuth agora tem senha (pedida antes de abrir o jogo) e link para o site do TokiAuth.\\n'
  '• Troca de telas mais rápida e animação de abertura mais curta.\\n'
  '• Criadores: Claude, pedznb.'),
 ('tokito_announce_text',
  'Tokito Preview %1$s (test version) is out! What is new: optimization modes that work on the launcher (FPS in steps of 10, heat and benchmark), pull-down drawer with Main things and pop-ups, skin search and a password for TokiAuth. Creators: Claude, pedznb.',
  'O Tokito Preview %1$s (versão de teste) chegou! Novidades: modos de otimização que trabalham no launcher (FPS de 10 em 10, calor e benchmark), gaveta de puxar para baixo com Principais coisas e pop-ups, pesquisa de skins e senha no TokiAuth. Criadores: Claude, pedznb.'),
 ('tokito_notice_update_available', 'New version available: %1$s', 'Nova versão disponível: %1$s'),
 ('tokito_notice_default_title', 'Tokito Launcher notice', 'Aviso do Tokito Launcher'),
 ('tokito_notice_open', 'Open link', 'Abrir link'),

 # credits
 ('tokito_creators_text',
  'Creators of Tokito Launcher, an unofficial modified version of Zalith Launcher 2.',
  'Criadores do Tokito Launcher, uma versão não oficial modificada do Zalith Launcher 2.'),
 ('tokito_creators_line', 'Creators: Claude, pedznb', 'Criadores: Claude, pedznb'),
]
