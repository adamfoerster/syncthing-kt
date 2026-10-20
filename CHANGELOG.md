# Changelog

Todas as mudanças relevantes deste projeto ficam aqui. O formato segue
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o versionamento segue
[SemVer](https://semver.org/lang/pt-BR/): `app.version` em `gradle.properties` é a fonte única da versão,
e `./gradlew verifyVersion` (executado antes de todo build Android) exige que ela seja a versão mais
recente listada aqui.

## [Unreleased]

## [1.3.0] - 2026-10-19

### Adicionado
- Pastas conhecidas: quando um aparelho que você já adicionou oferece uma pasta que você já sincroniza com outro
  aparelho, ele entra nessa pasta sem pedir de novo, e um aviso rápido conta o que aconteceu. Ofertas de pastas novas,
  de aparelhos desconhecidos ou de pastas Somente enviar/receber continuam pedindo sua resposta. Dá para desligar em
  **Ajustes → Pastas conhecidas** (vem ligado).
- Idioma do app: em **Ajustes → Idioma** você escolhe um dos sete idiomas do app, independente do idioma do celular, ou
  volta para **Idioma do aparelho**.

## [1.2.0] - 2026-10-19

### Adicionado
- Arquivos ignorados: cada pasta da aba Pastas ganhou o botão **Arquivos ignorados**, que escolhe o que a pasta não
  deve sincronizar. No modo simples dá para ligar escolhas rápidas (arquivos temporários, sobras do Windows e do Mac,
  miniaturas e lixeira do Android, pastas de programação), digitar o nome de um arquivo, de uma pasta ou de um tipo
  (como `*.mp3`) e remover qualquer item da lista. O **modo avançado** abre o texto das regras (uma por linha, como no
  `.stignore`) com um guia da sintaxe: curingas, exceções com `!`, `(?i)`, `(?d)` e comentários. O Syncthing usa
  padrões com curingas, não expressões regulares. Os arquivos que já foram sincronizados continuam onde estão, e as
  regras valem só neste aparelho.
- Texto da tela de ignorados traduzido para os sete idiomas do app.

## [1.1.1] - 2026-10-19

### Adicionado
- README em inglês (`README.en.md`), com link a partir do README em português.
- Screenshots em inglês para a Play Store em `store-screenshots/en/` (1080×2160), geradas no modo demo.

### Alterado
- Os dados do modo demo (debug) agora estão em inglês: aparelhos, pastas e arquivos de conflito.

## [1.1.0] - 2026-10-19

### Adicionado
- O app agora está traduzido para português, inglês, espanhol, italiano, alemão, francês e grego. Ele usa o idioma do
  aparelho; se o idioma não for um desses, o app aparece em inglês. No Android 13 ou mais novo, também dá para escolher
  o idioma só do app nas configurações do sistema.

### Alterado
- Tamanhos de arquivo usam o separador decimal do idioma (4,2 GB em português, 4.2 GB em inglês) e datas antigas
  seguem a ordem do idioma (dia/mês ou mês/dia).
- O inglês passou a ser o idioma padrão dos textos; o português do Brasil continua completo.

## [1.0.2] - 2026-10-19

### Corrigido
- Na resolução de conflitos de texto, os botões de escolha agora têm a cor do trecho que representam: verde para
  a primeira versão e azul para a segunda. Antes, quando os dois lados vinham de aparelhos desconhecidos, os
  botões tinham o mesmo nome e a mesma cor e não dava para saber qual escolhia qual trecho.

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
