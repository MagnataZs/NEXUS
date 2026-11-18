# NEXUS ROOT V2 — interface reconstruída

Projeto Android Kotlin para Android 8+ (minSdk 26), compileSdk 35, JDK 17.

## O que esta versão contém
- Lobby paisagem com identidade azul/preta neon e biblioteca de apps.
- Seletor persistente Lobby 1 / Lobby 2 em Configurações.
- Overlay de sessão com menu esquerdo, indicadores superiores, painel rápido à direita e barra inferior, seguindo as referências visuais do usuário.
- Pixel Graphics com cinco níveis configuráveis.
- Detecção de app em primeiro plano somente durante sessão iniciada pelo PLAY.
- Verificação ROOT e comandos de resolução por presets.
- Restauração temporária solicitada no encerramento da sessão.

## Limites técnicos importantes
- As duas opções de lobby têm a mesma estrutura funcional nesta base; o Lobby 2 é uma variação visual simples, ainda não uma reprodução gráfica independente pixel a pixel.
- Pixel Graphics usa presets de resolução lógica do display como aproximação. Isso afeta o display inteiro, não aplica blur/filtro apenas à imagem do jogo.
- FPS/GPU/temperatura reais ainda não estão ligados a uma fonte de telemetria; os rótulos mostram indisponível/placeholder. Não confiar em números fictícios.
- OpenGL/Vulkan não podem ser forçados universalmente por app externo; o jogo/driver seleciona.
- CPU/GPU governor e fast charge não são alterados nesta versão.
- A detecção depende da permissão Acesso ao uso e de como a ROM reporta UsageEvents.

## Compilar
Envie a pasta para um repositório GitHub, abra Actions e execute Build NEXUS ROOT APK. Baixe o artefato `nexus-root-v2-debug`. Também pode abrir no Android Studio com JDK 17 e SDK 35 e executar `assembleDebug`.

## Permissões
Conceda Acesso ao uso e Sobrepor a outros apps. Ao chamar comandos `su`, autorize o NEXUS ROOT no Magisk.

## Aviso de resolução
Mudanças em `wm size` são globais e podem afetar toda a interface. Teste com cautela. A restauração automática depende do serviço permanecer ativo; após crash/reboot pode ser necessário restaurar manualmente via menu ou ADB/ROOT.
