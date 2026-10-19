# Changelog

Todas as mudanças relevantes deste projeto ficam aqui. O formato segue
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o versionamento segue
[SemVer](https://semver.org/lang/pt-BR/): `app.version` em `gradle.properties` é a fonte única da versão,
e `./gradlew verifyVersion` (executado antes de todo build Android) exige que ela seja a versão mais
recente listada aqui.

## [Unreleased]

## [1.0.1] - 2026-10-19

### Corrigido
- O Syncthing não iniciava no Android 13 e anteriores ("O Syncthing não respondeu a tempo"): o Android bloqueava
  o HTTP sem criptografia para a API local do núcleo. Agora o texto puro é liberado só para `127.0.0.1` e `localhost`.
- Falhas na checagem de saúde do núcleo passam a aparecer no log em vez de serem descartadas.

## [1.0.0] - 2026-10-19

Primeira versão: reimplementação do Syncthing Android em Kotlin Multiplatform + Compose Multiplatform
(Android como único target), com a interface "Neon Grid" do redesign.

### Adicionado
- Núcleo do Syncthing v2.1.6 (Go) rodando como processo filho, supervisionado por um foreground service
  `specialUse`; a REST API local usa porta e chave geradas pelo app.
- Task `:syncthing-native:buildNative` que compila o núcleo para Android com Go + NDK.
- Telas Início (status e mapa de aparelhos), Compartilhamento, Pastas, Aparelhos e Ajustes.
- Aceitar pastas oferecidas, adicionar pasta e escolher com quais aparelhos compartilhar.
- Fluxo de 3 passos para adicionar aparelho: mostrar QR, aprovar (QR via câmera ou código digitado),
  escolher pastas; aprovação de aparelhos pendentes.
- Resolução de conflitos:
  - texto: diff por linhas (Myers) com destaque dentro da linha; cada trecho fica com uma versão ou as duas,
    visão do resultado final e edição à mão a partir de marcadores `<<<<<<<`/`=======`/`>>>>>>>`, como no GitHub;
  - binários e imagens: manter uma versão ou as duas (`nome (cópia).ext`), com prévia das imagens;
  - versões substituídas vão para `.stversions/`; a gravação é recusada se o arquivo mudou no disco.
- Modo demo no build debug (REST simulada, conflitos reais no disco) para testar a interface sem o núcleo.
- O nome do aparelho começa como o modelo do celular.
- Reinício pedido pelo próprio núcleo relança o processo em vez de mostrar erro.
- Testes do parser de nomes de conflito, diff/merge, repositório de conflitos (Okio FakeFileSystem)
  e do ViewModel do resolvedor de texto.
