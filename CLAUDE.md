# CLAUDE.md

Reimplementação do Syncthing Android em Kotlin Multiplatform + Compose Multiplatform. Android é o único
target por enquanto, mas o código vai em `commonMain` sempre que não precisar de API do Android.
A interface é traduzida para pt, en, es, it, de, fr e el (inglês é o padrão para idiomas não suportados). Visão geral e instruções de build: [README.md](README.md).

## Regras obrigatórias a cada alteração

Toda alteração (feature, correção, refatoração com efeito visível) só está pronta quando cumpre os quatro itens:

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

`./gradlew verifyVersion` falha se `app.version` não for a versão mais recente do CHANGELOG, e roda antes
de todo build do `androidApp`. Não contorne essa verificação.

Uma alteração = um bump. Vários ajustes na mesma tarefa entram numa única versão.

## Comandos

```bash
./gradlew testAndroidHostTest          # todos os testes (JVM, sem aparelho)
./gradlew :androidApp:assembleDebug    # APK debug
./gradlew verifyVersion                # versão x CHANGELOG
./gradlew :syncthing-native:buildNative -Pabis=arm64-v8a,x86_64   # núcleo Go (precisa de Go + NDK)
```

Modo demo (debug, sem núcleo): `adb shell run-as com.adamfoerster.syncthing.debug touch files/demo`
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
