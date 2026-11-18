# NEXUS ROOT — V4 Integrated (Kotlin + Jetpack Compose)

Projeto Android em paisagem, com lobby selecionável, biblioteca de aplicativos, monitoramento básico, sessão de jogo iniciada pelo PLAY, overlay flutuante e gerenciador central de resolução ROOT.

## Integrado nesta versão

- Duas opções de lobby selecionáveis e persistidas.
- Biblioteca: lista apps instalados com atividade de inicialização, ícone/nome reais, pesquisa, adicionar/remover e persistência.
- PLAY: inicia o serviço de sessão e abre o pacote escolhido.
- Sessão sob demanda: o serviço só é iniciado pelo PLAY. Com Acesso a dados de uso, acompanha eventos de primeiro plano e encerra overlay/serviço quando o jogo deixa de estar em primeiro plano por mais de 25 segundos. Também há encerramento manual.
- Overlay flutuante real via `TYPE_APPLICATION_OVERLAY`, arrastável pelo cabeçalho, com minimizar, resolução, Pixel Graphics, renderizador e encerrar sessão.
- Monitor: CPU estimada por `/proc/stat`, RAM via `ActivityManager`, bateria/temperatura na tela principal. No overlay há leitura periódica de CPU/RAM. FPS/GPU ficam como N/D, pois não existe API universal confiável para qualquer jogo.
- `RootResolutionManager` centraliza resolução global e temporária. A temporária salva o override anterior e tenta restaurá-lo ao terminar a sessão ou ao destruir o serviço.
- Resolução global disponível em Configurações; resolução temporária disponível no overlay.
- Pixel Graphics agora possui botão de configuração em cada card de jogo (ícone de ajustes), com ativação individual e intensidade de 1 a 5 persistida por pacote. O overlay da sessão mostra o estado e nível do perfil daquele jogo. O perfil nunca altera a resolução global nem é aplicado a outros apps. **A aplicação visual de pixelização somente dentro do jogo ainda depende de integração específica com o jogo/driver**; Android não oferece uma API pública universal para interceptar e pixelizar a superfície de qualquer jogo. Portanto, esta versão entrega a configuração por jogo, mas não finge que o efeito gráfico já está aplicado. Os antigos botões que reduziam a resolução do display foram removidos do Pixel Graphics.
- Renderizador é identificado como controlado pelo jogo; o app não força OpenGL/Vulkan em jogos de terceiros.

## Permissões necessárias

1. ROOT concedido pelo Magisk para aplicar resolução.
2. Permitir sobreposição para exibir o painel por cima dos jogos.
3. Acesso a dados de uso para detectar quando o jogo fecha/é deixado em segundo plano.
4. Notificações podem ser solicitadas conforme a versão do Android.

O app direciona o usuário às telas de permissões quando necessário. Depois de conceder, toque em PLAY novamente.

## Segurança e restauração

- Os comandos ROOT são limitados a `wm size` e `wm density`, com tamanhos definidos no código; não há execução de comandos arbitrários pela interface.
- A resolução temporária e a global guardam snapshots separados. Evite alternar manualmente a resolução do Android enquanto uma sessão temporária estiver ativa.
- Se o Android encerrar o processo à força ou ocorrer falha do sistema antes da restauração, pode ser necessário abrir o app e usar Restaurar resolução global/temporária ou executar `wm size reset` e `wm density reset` no terminal ROOT.

## Build

Requer Android Studio recente, JDK 17, Android SDK 35 e Gradle 8.9. Execute na pasta do projeto:

```bash
gradle assembleDebug
```

O workflow em `.github/workflows/build.yml` compila o APK Debug e publica o artefato no GitHub Actions.

## Estado de validação

O código-fonte foi integrado nesta entrega, mas não foi possível executar Gradle/Android SDK neste ambiente. Portanto, este ZIP **não é um APK compilado nem uma build validada em aparelho**. É necessário compilar e testar no Redmi Note 11 antes de confiar em alterações ROOT.
