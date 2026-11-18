# NEXUS ROOT V1 — projeto Android Kotlin

## Estado desta entrega
Este é um projeto-fonte inicial avançado, não um APK já compilado. O ambiente desta conversa não possui Android SDK/Gradle para validar a compilação. O workflow do GitHub Actions incluído pode compilar o APK debug em um repositório GitHub.

## Implementado no código
- Lobby em paisagem com tema escuro/azul.
- Lista de aplicativos com atividade Launcher e botão PLAY.
- Sessão iniciada somente pelo PLAY.
- Serviço em primeiro plano durante a sessão.
- Monitoramento por UsageStats somente durante a sessão.
- Overlay lateral básico com painel.
- Verificação de acesso ROOT via `su -c`.
- Presets de resolução via `wm size`, com snapshot e tentativa de restauração ao encerrar sessão.

## Ainda limitado / não implementado
- Pixel Graphics real: não há API universal para pixelizar a renderização interna de jogos.
- Troca forçada de OpenGL/Vulkan por jogo: não é oferecida porque o Android não fornece um controle universal seguro.
- Fast Charge: depende do kernel/controlador; não há alteração de corrente/tensão nesta versão.
- Métricas avançadas de FPS/temperatura: pendentes.
- A lista atual inclui apps com ícone Launcher, não classifica automaticamente somente jogos.
- A detecção de foreground depende da permissão "Acesso ao uso" e pode variar entre ROMs.

## Compilar no GitHub
1. Crie um repositório e envie todos os arquivos desta pasta.
2. Abra Actions e execute `Build NEXUS ROOT APK` (ou faça push na branch main).
3. Baixe o artefato `nexus-root-debug-apk`.
4. O APK debug estará em `app-debug.apk`.

Também é possível abrir a pasta no Android Studio com JDK 17 e SDK Android 35 e executar `assembleDebug`.

## Permissões no aparelho
- Conceder acesso ao uso nas configurações Android.
- Conceder permissão de sobreposição.
- Autorizar a solicitação ROOT do Magisk quando o app executar `su`.

## Atenção sobre resolução
`wm size` altera a resolução lógica do display do Android, não necessariamente a escala de renderização interna de cada jogo. A alteração pode afetar toda a interface. Teste primeiro com cuidado e mantenha um método de restauração. O app usa presets allowlisted e tenta restaurar o snapshot, mas diferenças de ROM podem exigir ajustes.

## Compatibilidade-alvo
- minSdk 26 (Android 8.0+)
- target/compile SDK 35
- JDK 17
- Magisk/root para comandos de resolução
