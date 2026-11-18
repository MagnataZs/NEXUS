# NEXUS ROOT V3 — Premium UI

Projeto Android em Kotlin/Jetpack Compose, Android 8+ (minSdk 26), compileSdk 35 e JDK 17. A V3 adiciona duas opções de lobby, biblioteca visual com capas locais, seletor persistente, layout paisagem responsivo e prévia do overlay.

## Estado funcional e compatibilidade

Esta atualização é prioritariamente visual. PLAY, métricas e overlay são demonstrações: a nova tela principal ainda não chama o monitor de sessão nem os comandos ROOT existentes no projeto. Os módulos anteriores (`GameSessionService`, `RootAccess` e `ResolutionManager`) foram mantidos no repositório, mas não estão conectados à interface V3. Não há janela flutuante real nem telemetria real de FPS/GPU/temperatura nesta interface.

O `applicationId` permanece `com.nexusroot`, para manter a identidade de instalação da V2. O código de versão foi elevado para 3.

## Compilar

Abra no Android Studio com JDK 17 e Android SDK 35 e execute `assembleDebug`. No GitHub Actions, rode `Build NEXUS ROOT Premium V3` e baixe o artefato `nexus-root-v3-debug`.

## Limitações

Pixel Graphics, mudança de resolução e detecção de sessão não são acionados pela interface V3. Presets de resolução, quando reativados futuramente, podem alterar o display inteiro. Não considerar os indicadores visuais como medições reais.
