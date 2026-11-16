# CLAUDE.md

MeshSync: reimplementação do Syncthing Android em Kotlin Multiplatform + Compose Multiplatform. Android, Windows e macOS
Apple Silicon (`desktopApp`) são os targets; o código vai em `commonMain` sempre que não precisar de API do Android.
A interface é traduzida para pt, en, es, it, de, fr e el (inglês é o padrão para idiomas não suportados). Visão geral e instruções de build: [README.md](README.md).

## Regras obrigatórias a cada alteração

Toda alteração (feature, correção, refatoração com efeito visível) só está pronta quando cumpre os cinco itens:

1. **Testes**: crie ou atualize testes que cubram o comportamento alterado (`commonTest` do módulo;
   regra de domínio → teste de domínio, ViewModel → teste de ViewModel, acesso a arquivos → `FakeFileSystem`).
   Corrigiu um bug? Primeiro um teste que falha por causa dele. Rode `./gradlew testAndroidHostTest`:
   tudo verde antes de concluir.
2. **Versão**: aumente `app.version` em `gradle.properties` seguindo SemVer
   (PATCH = correção, MINOR = funcionalidade nova compatível, MAJOR = quebra de compatibilidade:
   dados, configuração ou comportamento que o usuário perde). O `versionCode` é derivado dela.
3. **CHANGELOG.md**: adicione a entrada `## [X.Y.Z] - AAAA-MM-DD` no topo (abaixo de `[Unreleased]`),
   com seções `Adicionado` / `Alterado` / `Corrigido` / `Removido`, escrita para quem usa o app.
4. **Documentação**: atualize o README.md (e o `README.en.md`, a tradução em inglês) (e este arquivo, se mudar arquitetura, módulos, comandos ou regras)
   e os KDocs do código tocado.
5. **Notas da versão**: toda versão nova ganha uma entrada em `ReleaseNotes.entries` (`core:presentation`), no topo, com a
   versão igual a `app.version`. É um resumo curto para o usuário (só novidades, mudanças e correções, sem detalhes de
   implementação, 1 a 5 itens), com strings `release_notes_<versão com _>_<tipo>_<assunto>` traduzidas nos 7 idiomas. Sem
   entrada, o app não mostra a mensagem de novidades daquela versão. Ela abre sozinha uma vez por versão
   (`ReleaseNotesPolicy`) e pode ser revista em Configurações, ao lado da versão.

**Git**: nunca faça commit nem ofereça para comitar. Deixe tudo preparado (testes verdes, versão, CHANGELOG,
documentação) e termine sugerindo uma mensagem de commit; a revisão e o commit são feitos manualmente pelo usuário.

`./gradlew verifyVersion` falha se `app.version` não for a versão mais recente do CHANGELOG, e roda antes
de todo build do `androidApp`. Não contorne essa verificação.

Uma alteração = um bump. Vários ajustes na mesma tarefa entram numa única versão.

## Comandos

```bash
./gradlew testAndroidHostTest          # todos os testes (JVM, sem aparelho)
./gradlew :androidApp:assembleDebug    # APK debug
./gradlew :desktopApp:test            # testes do app Windows
./gradlew verifyVersion                # versão x CHANGELOG
./gradlew :syncthing-native:buildWindows   # syncthing.exe (precisa de Go); depois :desktopApp:run / packageMsi / packageExe
./gradlew :syncthing-native:buildMacos     # syncthing macOS arm64 (precisa de Go); depois :desktopApp:run / packageDmg (só num Mac; o JDK de empacotamento é escolhido sozinho, ou -Pmeshsync.packagingJdk)
./gradlew :syncthing-native:buildNative -Pabis=arm64-v8a,x86_64   # núcleo Go (precisa de Go + NDK)
```

Modo demo (debug, sem núcleo): `adb shell run-as com.adamfoerster.meshsync.debug touch files/demo`
e reabra o app. Use o emulador para testes manuais, não o aparelho físico do usuário.

## Arquitetura

- Clean Architecture por feature: `feature:<nome>:{domain,data,presentation}`; código usado por mais de uma
  feature vai para `core:{domain,data,presentation,design-system}`. Features nunca dependem umas das outras;
  navegação entre elas é ligada no módulo `app`.
- Convention plugins em `build-logic` (`syncthing.kmp.library`, `.compose`, `.feature`); versões só no
  `gradle/libs.versions.toml`.
- Apresentação em MVI: `State` (data class), `Action`, `Event` (Channel), `ViewModel`; composables
  `<Tela>Root` (ViewModel, navegação) e `<Tela>Screen` (só estado + `onAction`, com `@Preview`).
  Escolhas que importam sobrevivem a morte de processo via `SavedStateHandle`.
- Erros como `Result<T, DataError>`; textos de erro via `UiText` e `toUiText()`.
- DI com Koin: cada módulo expõe seu `module`, agregados em `app/di/AppModules.kt`; o `androidApp`
  fornece `SyncthingCore` e `SyncthingEndpoint`.
- O núcleo Go roda como processo (`AndroidSyncthingCore`); o app só fala com ele pela REST API
  (`core:data/SyncthingApi`). Não leia nem escreva o `config.xml` diretamente.
- Arquivos ignorados: regras do `.stignore` lidas e gravadas por `SyncthingRepository.ignorePatterns`/`setIgnorePatterns`
  (`/rest/db/ignores`); a lógica pura (`IgnoreRules`, `IgnorePreset`) fica em `core:domain` e a tela em `feature:folders`.
  Modo simples e avançado editam o mesmo texto; o Syncthing usa glob, não regex.
- Preferências do app (`AppPreferences` em `core:domain`, SharedPreferences no `androidApp`): aceite automático de
  ofertas de pastas já compartilhadas (`knownFolderOffers` + `FolderOfferAutoAccepter`), `ThemeMode` (sistema/claro/escuro; `SyncthingTheme(darkTheme)` escolhe `NgColors.Light` ou `NgColors.Dark`, resolvido no `MainActivity`) e idioma que sobrescreve o do
  aparelho (aplicado em `MainActivity.attachBaseContext`).
- Versão do app: `AppInfo` (`core:domain`) é fornecida por cada host (`androidApp` via `versionName`; `desktopApp` via `-Dmeshsync.version`, ambos de `app.version`) e aparece em Configurações junto da versão do núcleo.
- Notas da versão: `ReleaseNotes` + `ReleaseNotesDialog` em `core:presentation`; `App.kt` abre a mensagem uma vez por versão
  (`AppPreferences.lastSeenReleaseNotes` + `ReleaseNotesPolicy` em `core:domain`; instalação nova só grava a versão) e
  Configurações reabre pelo botão ao lado da versão.
- Onboarding (`feature:onboarding`): `OnboardingRoute` é o destino inicial até `AppPreferences.onboardingCompleted`; ao terminar
  navega para Home e, conforme a escolha, abre `AddFolderRoute` ou `AddDeviceRoute`.
- Gerenciador de arquivos (`feature:files:{domain,data,presentation}`): operações de arquivo (Okio) em `OkioFileManager`,
  aberto pelo botão da aba Pastas (`FilesRoute`). Pastas sincronizadas usam `NgFolderIcon(shared = true)`; **Sincronizar**
  abre `AddFolderRoute(path)`. Compartilhar usa `PlatformActions.shareFiles` (FileProvider no `androidApp`). Colar nunca sobrescreve. Receber do menu Compartilhar do Android: `MainActivity` põe o conteúdo no `IncomingShareInbox` (`feature:files:domain`), `App` abre `ShareTargetRoute` e `ShareImporter` (`AndroidShareImporter` no `androidApp`) copia para a pasta escolhida, sem sobrescrever.
- Desktop (`desktopApp`, JVM puro, Windows e macOS): reutiliza `:app` e fornece `AppPreferences`, `SyncthingEndpoint`, `SyncthingCore` (`DesktopSyncthingCore`:
  processo filho `syncthing(.exe)`, reinício no exit 3, limpeza de órfão por PID), `PlatformActions`, bandeja (`Tray`), instância única e
  autostart (`Autostart.forCurrentOs`: HKCU Run no Windows, LaunchAgent no macOS). O SO vem de `HostOs` (passe-o, não leia `os.name`).
  Todo código específico de SO fica ali; os módulos KMP ganham só o target `jvm("desktop")` e seus `actual`s.
  Dados em `%LOCALAPPDATA%\MeshSync` / `~/Library/Application Support/MeshSync`. O `upgradeUuid` do instalador nunca muda.
- Conflitos: detecção e resolução são operações de arquivo (Okio) em `feature:conflicts:data`; nada é apagado,
  versões substituídas vão para `.stversions/`. Preserve essa garantia.

## Interface

- O app tem tema claro e escuro (`NgColors.Light`/`Dark`): só use cores via `NgTheme.colors`, nunca fixas, e confira as duas paletas.
- Use os componentes de `core:design-system` (`NgPanel`, `NgButton`, `NgBadge`, `NgScreen`, `NgTopBar`…) e os
  tokens de `NgTheme`; não use cores, fontes ou formas soltas. Ícones vêm de `NgIcons`.
- Todo texto visível em `composeResources/values/strings.xml` do módulo (plurais com `<plurals>`). `values/` é o inglês
  (padrão); toda chave nova precisa de tradução em `values-pt`, `-es`, `-it`, `-de`, `-fr` e `-el` (o `TranslationsTest`
  confere chaves e placeholders). Não escape apóstrofos: use ’ (U+2019). Datas/números/ordem de argumentos mudam por idioma,
  então use `%1$s` posicional e nada de texto montado por concatenação.
- Status nunca depende só de cor: use o glifo de forma (`NgStatusLabel`) junto.
- Ao mudar uma tela, confira no emulador (modo demo serve) e compare com o mockup do redesign.

## Estilo

- Siga o código ao redor: nomes, densidade de comentários (KDoc curto explicando o porquê), idioma.
- Código e comentários em inglês; textos para o usuário em recursos traduzidos (nunca literais no código).
- Nada de `!!` em código de produção; trate `null` explicitamente.
