# Changelog

Todas as mudanças relevantes deste projeto ficam aqui. O formato segue
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o versionamento segue
[SemVer](https://semver.org/lang/pt-BR/): `app.version` em `gradle.properties` é a fonte única da versão,
e `./gradlew verifyVersion` (executado antes de todo build Android) exige que ela seja a versão mais
recente listada aqui.

## [Unreleased]

## [1.13.0] - 2026-11-10

### Adicionado
- **MeshSync para Windows**: um instalador (MSI ou EXE) instala o app para o seu usuário, sem pedir administrador. O Syncthing roda em segundo plano, com um **ícone ao lado do relógio** que mostra o estado (sincronizando, iniciando, pausado, com problema). Fechar a janela só a esconde; o serviço continua rodando.
- Menu do ícone: **Abrir MeshSync**, **Pausar/Retomar sincronização**, **Iniciar com o Windows** e **Sair** (que também encerra o Syncthing).
- O MeshSync inicia junto com o Windows, já na bandeja, e isso pode ser desligado no menu do ícone.
- Abrir o app uma segunda vez apenas mostra a janela que já está aberta.
- No Windows, **Escolher ou criar pasta** usa o seletor de pastas do sistema e **Compartilhar** abre o arquivo no Explorer.

### Alterado
- Quem usa o app no computador não vê a leitura de QR code: o código do aparelho é digitado ou colado.

## [1.12.3] - 2026-11-05

### Corrigido
- Arquivos criados ou editados em outro aparelho (por exemplo o computador) apareciam como **conflito** sem ninguém ter mexido neles no celular. O Android grava todos os arquivos com as mesmas permissões, o que fazia o celular anunciar uma "nova versão" de cada arquivo recebido e colidir com a próxima edição. Agora as pastas são criadas com a opção de **ignorar permissões**, e as pastas que você já tinha são corrigidas automaticamente na próxima vez que o app abrir. Conflitos que já existem continuam na lista até você resolvê-los.

## [1.12.2] - 2026-11-05

### Corrigido
- Em **Ajustes**, a versão mostrada junto ao estado do núcleo aparecia como "MeshSync …", mas é a versão do núcleo Syncthing. Agora aparece como "Syncthing …".

## [1.12.1] - 2026-11-03

### Corrigido
- Ao terminar a introdução logo após instalar, as telas *Adicionar pasta* e *Adicionar aparelho* falhavam com "Syncthing is not running yet", porque o núcleo ainda estava iniciando. Agora elas mostram o estado do núcleo (iniciando, sem permissão, parado) com o botão para resolver, e só liberam **Salvar** e **Confirmar** quando ele está rodando. A mensagem enganosa "Adicione um aparelho primeiro" também não aparece mais enquanto o núcleo não respondeu.

## [1.12.0] - 2026-11-03

### Adicionado
- Na aba **Pastas**, abaixo das pastas compartilhadas, o botão **Explorar arquivos** abre um gerenciador de arquivos com o armazenamento do aparelho. Toque numa pasta para abri-la; toque e segure (ou toque em um arquivo) para selecionar e então **copiar**, **mover**, **colar aqui**, **renomear**, **remover** ou **compartilhar** pelo diálogo do sistema. O botão **+** cria uma pasta nova. Colar nunca sobrescreve: se o nome já existe, o item chega como `nome (1)`.
- No gerenciador, as pastas que o MeshSync sincroniza aparecem com a marca do app dentro do ícone da pasta.
- Ao selecionar uma pasta ainda não sincronizada, a ação **Sincronizar** abre o fluxo de adicionar pasta com o caminho já preenchido (e o nome da pasta como nome do compartilhamento).
- Mensagens novas para nome inválido, nome que já existe e pasta copiada para dentro dela mesma.

## [1.11.0] - 2026-11-03

### Adicionado
- Ao adicionar uma pasta, o botão **Escolher ou criar pasta** abre o seletor de pastas do sistema, onde você também pode criar uma pasta nova. O local escolhido preenche o campo automaticamente, e o nome da pasta vira o nome do compartilhamento se você ainda não digitou um.

## [1.10.0] - 2026-11-03

### Adicionado
- A introdução agora pede o acesso a todos os arquivos antes da escolha final. Antes, quem escolhia conectar a outro aparelho via a falha e só então descobria na tela inicial que faltava essa permissão.

## [1.9.3] - 2026-11-01

### Alterado
- O app publicado agora é ofuscado e reduzido (R8), ficando menor e mais difícil de descompilar.
- O build de release pode ser assinado com a chave de publicação para Play Store e Amazon Appstore.

## [1.9.2] - 2026-11-01

### Alterado
- O identificador do app passou a ser `com.adamfoerster.meshsync` (antes `com.adamfoerster.syncthing`). Por ser um novo ID, o Android o trata como um app diferente: instale-o como novo e reconfigure pastas e dispositivos.

## [1.9.1] - 2026-10-31

### Corrigido
- Em aparelhos com páginas de memória de 16 KB (Android 15+), o app não mostra mais o aviso de
  incompatibilidade: o núcleo de sincronização e a biblioteca gráfica agora são compilados com alinhamento de 16 KB.

## [1.9.0] - 2026-10-31

### Adicionado
- Introdução na primeira abertura do app: três telas explicam em linguagem simples como o MeshSync funciona
  e uma última pergunta se você quer compartilhar uma pasta deste aparelho, pegar uma pasta de outro aparelho
  ou decidir depois. Dá para pular a qualquer momento e ela só aparece uma vez.

## [1.8.1] - 2026-10-31

### Alterado
- O app ganhou o novo logotipo do MeshSync como ícone (inclusive o ícone monocromático do Android 13+).

## [1.8.0] - 2026-10-30

### Adicionado
- Na aba Pastas, a nova seção **Ofertas recusadas** lista as pastas que você recusou, com o nome do aparelho
  que as ofereceu e um botão **Aceitar** para compartilhá-las depois.

### Alterado
- **Recusar** uma oferta de pasta agora é definitivo: o Syncthing guarda a recusa e a oferta não volta a
  aparecer sozinha quando o aparelho reconecta. Para tê-la de volta, aceite-a em Ofertas recusadas.

## [1.7.0] - 2026-10-28

### Adicionado
- Quando um conflito de texto não tem nenhum trecho diferente, a lista e a tela de resolução agora dizem o
  motivo em vez de "mesmo conteúdo": só as quebras de linha diferem (Windows x Unix), só a quebra de linha
  final difere, ou os dois casos juntos.

## [1.6.0] - 2026-10-28

### Adicionado
- Conflitos com conteúdo idêntico (os que mostravam "Same content in both versions", comuns depois de mover
  uma pasta) agora são resolvidos sozinhos: o app mantém o arquivo mais antigo e arquiva o outro em
  `.stversions/`. Só conflitos cujos bytes são exatamente iguais são resolvidos assim; qualquer diferença,
  mesmo de quebra de linha, continua na lista para você decidir.

## [1.5.2] - 2026-10-28

### Alterado
- Na tela de resolução de conflitos de texto, os botões "Tudo de …" agora mostram também a data e a hora
  da última modificação de cada versão, para você saber qual é a mais recente antes de aceitar tudo.

## [1.5.1] - 2026-10-27

### Alterado
- O app agora se chama **MeshSync**: o nome aparece no ícone, na notificação, na tela de configurações e nas
  mensagens de inicialização e de permissão, em todos os idiomas. O núcleo continua sendo o Syncthing, e
  os identificadores internos (pacote do app) não mudaram, então a atualização preserva seus dados.

## [1.5.0] - 2026-10-20

### Adicionado
- Renomear aparelho: ao tocar em um aparelho já adicionado na aba Aparelhos, o menu ganhou **Renomear**. O novo nome
  vale só neste celular (o outro aparelho mantém o próprio nome) e não pode ficar em branco.

## [1.4.0] - 2026-10-20

### Adicionado
- Modo claro: o app ganhou uma paleta clara, com os mesmos acentos neon escurecidos para ficarem legíveis sobre fundo
  branco. Por padrão ele segue o modo claro ou escuro do celular e muda na hora quando o sistema muda.
- Tema em **Ajustes → Tema**: escolha **Claro** ou **Escuro** para ignorar o modo do celular, ou **Aparelho** para
  voltar a segui-lo. As barras de status e de navegação acompanham a escolha.

### Alterado
- Quem usa o celular no modo claro passa a ver o app claro (antes ele era sempre escuro). Para manter o visual
  anterior, escolha **Escuro** em Ajustes → Tema.

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
