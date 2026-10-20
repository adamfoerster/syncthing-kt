# Syncthing KT

🇬🇧 [Read in English](README.en.md)

Reimplementação do app Android do Syncthing em Kotlin Multiplatform + Compose Multiplatform
(Android é o único target por enquanto), com a interface "Neon Grid" do redesign e um
resolvedor de conflitos no estilo do GitHub.

## Destaques

Dois recursos que fazem falta no `syncthing-android` e que são o foco deste projeto:

- **Resolução de conflitos dentro do app**: compare as duas versões, escolha trecho a trecho ou edite à mão,
  sem abrir um editor nem apagar arquivos às cegas. Veja [Resolução de conflitos](#resolução-de-conflitos).
- **Arquivos ignorados para leigos**: escolha o que uma pasta não deve sincronizar com um toque (temporários,
  restos de sistema, miniaturas) ou digitando um nome ou tipo, e abra o **modo avançado** só se quiser editar o
  `.stignore` direto. Veja [Arquivos ignorados](#arquivos-ignorados).

## Como funciona

Igual ao `syncthing-android`: o núcleo oficial do Syncthing (Go) roda como processo filho e o app
conversa com ele pela [REST API](https://docs.syncthing.net/dev/rest.html) em `127.0.0.1`.
O app gera a chave da API e a porta e as passa ao núcleo (`--gui-address`, `--gui-apikey`), então
nunca precisa ler o `config.xml`. Um foreground service (`specialUse`) mantém o processo vivo.

## Módulos

| Módulo | Conteúdo |
|---|---|
| `build-logic` | Convention plugins (`syncthing.kmp.library`, `.compose`, `.feature`) |
| `core:domain` | Modelos, `Result`/`DataError`, contratos `SyncthingRepository`, `SyncthingCore`, `ConflictSummary` |
| `core:data` | Cliente Ktor da REST API, DTOs, repositório com long-poll em `/rest/events` |
| `core:design-system` | Tokens, fontes (Chakra Petch, IBM Plex), cantos chanfrados, ícones e componentes |
| `core:presentation` | `UiText`, formatadores, `ObserveAsEvents`, `PlatformActions`, aviso de estado do núcleo |
| `feature:home` | Início: status e mapa de aparelhos |
| `feature:sharing` | Quem compartilha o quê (grafo aparelhos ↔ pastas) |
| `feature:folders` | Lista de pastas, ofertas de pastas, adicionar/aceitar pasta, arquivos ignorados |
| `feature:devices` | Aparelhos, aprovação de pendentes, fluxo de 3 passos com QR |
| `feature:settings` | Ajustes (nome, código, iniciar/parar núcleo, aceite automático de pastas, idioma) |
| `feature:conflicts` | `domain` (diff Myers, modelo de merge), `data` (varredura e resolução no disco), `presentation` |
| `app` | NavHost, barra de abas, agregação dos módulos Koin |
| `androidApp` | Activity, serviço, gerenciador do processo do núcleo, scanner QR (ZXing) |
| `syncthing-native` | Task que compila o núcleo Go para Android |

## Resolução de conflitos

O Syncthing guarda a versão perdedora como `nome.sync-conflict-AAAAMMDD-HHMMSS-<aparelho>.ext`.
O app varre as pastas (de novo sempre que o índice de uma pasta muda), identifica quem fez cada versão
(`/rest/db/file` para a atual, o ID curto no nome para a cópia) e classifica:

- **Texto** (UTF-8, até 2 MB): diff por linhas; cada trecho diferente é resolvido com
  "este celular", "o outro aparelho" ou "as duas"; cada botão tem a cor do trecho que escolhe
  (verde para a primeira versão, azul para a segunda). Há visão de resultado final e **edição à mão**
  a partir do texto com marcadores `<<<<<<<`/`=======`/`>>>>>>>`, como no GitHub; só conclui sem marcadores.
- **Binário / imagem**: escolher uma versão ou manter as duas (`nome (cópia).ext`), com prévia para imagens.

Nada é apagado: as versões substituídas vão para `.stversions/` dentro da pasta, com o mesmo formato
de nome do versionamento do Syncthing (que nunca sincroniza esse diretório). Se o arquivo mudar no disco
enquanto o usuário decide, a gravação é recusada.

Sem ancestral comum (o Syncthing não guarda um), todo o merge é de duas vias.

## Arquivos ignorados

Cada cartão da aba **Pastas** tem o botão **Arquivos ignorados**, que abre o editor das regras do `.stignore`
daquela pasta. O app lê e grava as regras pela REST API (`GET`/`POST /rest/db/ignores`), sem tocar no arquivo;
o núcleo reescaneia sozinho depois de salvar. As regras valem só no aparelho onde foram salvas, e arquivos que
já foram sincronizados não são apagados: só deixam de ser sincronizados.

- **Modo simples** (padrão): escolhas rápidas liga/desliga (arquivos temporários, sobras de Windows/Mac,
  miniaturas e lixeira do Android, pastas de programação), um campo para ignorar um arquivo, uma pasta ou um
  tipo (`*.mp3`), e a lista do que está ignorado, com um botão para remover cada item.
- **Modo avançado** (botão no fim da tela): editor de texto com uma regra por linha, igual ao `.stignore`, e um
  guia rápido da sintaxe: `*`, `**`, `?`, `{a,b}`, `/` na raiz, `!` para exceções, `(?i)` para ignorar maiúsculas,
  `(?d)` para permitir apagar a pasta e `//` para comentários. O Syncthing usa padrões com curingas (glob), **não
  expressões regulares**.

Os dois modos editam o mesmo texto, então alternar entre eles não perde nada; regras que o modo simples não
entende (exceções, `(?i)`, curingas, comentários) aparecem como "regra avançada" e são mantidas como estão.
A lógica está em `core:domain` (`IgnoreRules`, `IgnorePreset`) e a tela em `feature:folders` (pacote `ignore`).

## Pastas conhecidas

Quando um aparelho que você já adicionou oferece uma pasta que você já compartilha com outro aparelho (mesmo ID,
tipo Enviar e receber), o app o inclui na pasta sem perguntar e mostra um aviso rápido. Ofertas de pastas novas, de
aparelhos desconhecidos ou de pastas Somente enviar/receber continuam pedindo sua resposta. A opção fica em
**Ajustes → Pastas conhecidas** (ligada por padrão; desligada, tudo volta a perguntar). A regra pura é
`knownFolderOffers` em `core:domain`; quem a aplica é o `FolderOfferAutoAccepter` em `core:data`, iniciado pelo
`SyncthingApplication`. As preferências ficam em `AppPreferences` (`core:domain`), guardadas em SharedPreferences.

## Idiomas

O app está traduzido para português (`pt`), inglês (`en`), espanhol (`es`), italiano (`it`), alemão (`de`),
francês (`fr`) e grego (`el`). Segue o idioma do aparelho; qualquer outro cai no **inglês**, que é o padrão. Em **Ajustes → Idioma** dá para
sobrescrever essa escolha só no app (**Idioma do aparelho** volta a seguir o sistema); o `MainActivity` aplica a
escolha em `attachBaseContext` e se recria ao mudar.
Os textos ficam em `composeResources/values/strings.xml` de cada módulo (inglês, o padrão) e em
`values-<idioma>/strings.xml` (as traduções); o `androidApp` tem o mesmo esquema em `res/` (notificação, scanner QR)
e declara os idiomas em `res/xml/locales_config.xml` (seletor de idioma por app do Android 13+).
`TranslationsTest` falha se uma tradução faltar, sobrar ou tiver placeholders (`%1$s`) diferentes do inglês.
Para adicionar um idioma: crie o `values-<idioma>` em todos os módulos, inclua-o em `locales_config.xml`,
em `localeFilters` (`androidApp/build.gradle.kts`) e na lista de `TranslationsTest`.

## Screenshots da Play Store

`store-screenshots/<idioma>/` guarda as imagens (1080×2160, proporção 2:1). Para refazê-las: emulador com
`wm size 1080x2160`, instale o debug, ative o modo demo, defina o idioma com
`adb shell cmd locale set-app-locales com.adamfoerster.syncthing.debug --locales en-US` e capture com
`adb exec-out screencap -p`. Hoje só existe `en/`.

## Compilar

```bash
./gradlew :androidApp:assembleDebug
```

O APK só sincroniza de verdade com o núcleo embutido. Compile-o uma vez (precisa de git, Go e Android NDK):

```bash
./gradlew :syncthing-native:buildNative -Pabis=arm64-v8a,x86_64
```

Isso clona o Syncthing `v2.1.6` em `syncthing-native/go/` e grava `androidApp/src/main/jniLibs/<abi>/libsyncthing.so`
(ambos fora do git). Sem o binário, o app mostra "Núcleo do Syncthing ausente".

### Modo demo (só debug)

Para ver a interface sem o núcleo: crie o arquivo de flag e reabra o app. A REST é simulada, mas os
conflitos são arquivos reais em `Android/data/<app>/files/Documents`, então detecção e resolução rodam de verdade.

```bash
adb shell run-as com.adamfoerster.syncthing.debug touch files/demo
```

## Testes

```bash
./gradlew testAndroidHostTest
```

## Versionamento e contribuição

Versão atual: **1.2.0** (`app.version` em `gradle.properties`, SemVer). Histórico em [CHANGELOG.md](CHANGELOG.md).

Cada alteração precisa de testes, um bump de versão, uma entrada no CHANGELOG e a documentação atualizada;
as regras completas estão em [CLAUDE.md](CLAUDE.md). `./gradlew verifyVersion` (que roda antes de todo
build do app) falha se a versão e o CHANGELOG não baterem.

## Licenças

Fontes Chakra Petch e IBM Plex sob SIL OFL 1.1 (`licenses/`). O processo de build do núcleo segue o
`build-syncthing.py` do syncthing-android.
