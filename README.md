# Syncthing KT

Reimplementação do app Android do Syncthing em Kotlin Multiplatform + Compose Multiplatform
(Android é o único target por enquanto), com a interface "Neon Grid" do redesign e um
resolvedor de conflitos no estilo do GitHub.

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
| `feature:folders` | Lista de pastas, ofertas de pastas, adicionar/aceitar pasta |
| `feature:devices` | Aparelhos, aprovação de pendentes, fluxo de 3 passos com QR |
| `feature:settings` | Ajustes (nome, código, iniciar/parar núcleo) |
| `feature:conflicts` | `domain` (diff Myers, modelo de merge), `data` (varredura e resolução no disco), `presentation` |
| `app` | NavHost, barra de abas, agregação dos módulos Koin |
| `androidApp` | Activity, serviço, gerenciador do processo do núcleo, scanner QR (ZXing) |
| `syncthing-native` | Task que compila o núcleo Go para Android |

## Resolução de conflitos

O Syncthing guarda a versão perdedora como `nome.sync-conflict-AAAAMMDD-HHMMSS-<aparelho>.ext`.
O app varre as pastas (de novo sempre que o índice de uma pasta muda), identifica quem fez cada versão
(`/rest/db/file` para a atual, o ID curto no nome para a cópia) e classifica:

- **Texto** (UTF-8, até 2 MB): diff por linhas; cada trecho diferente é resolvido com
  "este celular", "o outro aparelho" ou "as duas". Há visão de resultado final e **edição à mão**
  a partir do texto com marcadores `<<<<<<<`/`=======`/`>>>>>>>`, como no GitHub; só conclui sem marcadores.
- **Binário / imagem**: escolher uma versão ou manter as duas (`nome (cópia).ext`), com prévia para imagens.

Nada é apagado: as versões substituídas vão para `.stversions/` dentro da pasta, com o mesmo formato
de nome do versionamento do Syncthing (que nunca sincroniza esse diretório). Se o arquivo mudar no disco
enquanto o usuário decide, a gravação é recusada.

Sem ancestral comum (o Syncthing não guarda um), todo o merge é de duas vias.

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
conflitos são arquivos reais em `Android/data/<app>/files/Documentos`, então detecção e resolução rodam de verdade.

```bash
adb shell run-as com.adamfoerster.syncthing.debug touch files/demo
```

## Testes

```bash
./gradlew testAndroidHostTest
```

## Licenças

Fontes Chakra Petch e IBM Plex sob SIL OFL 1.1 (`licenses/`). O processo de build do núcleo segue o
`build-syncthing.py` do syncthing-android.
