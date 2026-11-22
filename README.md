# Kanban de Reposição

Aplicativo mobile desenvolvido para digitalizar e organizar o processo de solicitação e reposição de insumos em ambientes industriais.

O sistema substitui a comunicação informal, como gritos, sinais luminosos e deslocamentos até o almoxarifado, por um quadro digital que permite acompanhar as solicitações de reposição em tempo real.

## 1. Objetivo do projeto

O objetivo do Kanban de Reposição é facilitar a comunicação entre os operadores de linha e a equipe de logística, proporcionando maior controle sobre os insumos que precisam ser repostos.

Por meio do aplicativo, o operador informa quando determinado insumo está acabando. A equipe de logística visualiza a solicitação em um quadro organizado por status e realiza o atendimento.

O sistema também registra os horários das etapas do atendimento, permitindo acompanhar o tempo de espera e o histórico das solicitações.

## 2. Perfis de usuário

O aplicativo possui dois perfis de acesso:

### Operador de linha

Responsável por:
- Acessar o aplicativo com sua conta.
- Selecionar o posto de trabalho em que está atuando.
- Consultar os insumos disponíveis para seu posto.
- Abrir cartões de reposição.
- Informar o nível de urgência da solicitação.
- Acompanhar o andamento dos cartões.
- Confirmar o recebimento do insumo para concluir o atendimento.

### Logística

Responsável por:
- Acessar o quadro geral de reposição.
- Visualizar os cartões de todos os postos.
- Identificar as solicitações abertas.
- Iniciar o atendimento de uma solicitação.
- Atualizar o status dos cartões.
- Acompanhar os tempos de atendimento.
- Gerenciar postos de trabalho e insumos.

Os novos usuários cadastrados pelo aplicativo recebem automaticamente o perfil de operador. As contas de logística são autorizadas manualmente no Firebase, de acordo com as regras de acesso do projeto.

## 3. Fluxo de funcionamento

O processo de reposição segue as seguintes etapas:

1. **Aberto:** o operador identifica a necessidade de reposição e abre um cartão.
2. **Em rota:** a equipe de logística inicia o atendimento e realiza o deslocamento para entregar o insumo.
3. **Entregue:** a logística registra que o insumo foi entregue ao posto.
4. **Concluído:** o operador confirma o recebimento do insumo, encerrando a solicitação.

O cartão permanece disponível no fluxo até que o operador confirme o recebimento.

## 4. Tecnologias utilizadas

| Tecnologia | Utilização |
|---|---|
| Java 11 | Linguagem de programação |
| Android Studio | Ambiente de desenvolvimento |
| Gradle Kotlin DSL | Automação de build |
| Android SDK | Desenvolvimento do aplicativo |
| Firebase Authentication | Autenticação de usuários |
| Cloud Firestore | Armazenamento e sincronização dos dados |
| Firebase BoM 35.1.0 | Gerenciamento das dependências Firebase |
| Google Services Plugin | Integração do projeto com Firebase |
| ZXing 4.3.0 | Leitura de QR Codes |
| AndroidX Lifecycle 2.6.2 | ViewModel e LiveData |
| RecyclerView 1.3.2 | Exibição de listas |
| Git e GitHub | Controle de versão e colaboração |

### Configurações Android

- **Application ID:** `com.example.kanban`
- **Namespace:** `com.example.kanban`
- **Compile SDK:** 36
- **Target SDK:** 36
- **Minimum SDK:** 24
- **Compatibilidade Java:** 11

## 5. Funcionalidades implementadas

- Cadastro e autenticação de usuários com Firebase Authentication.
- Persistência de sessão e encerramento de sessão.
- Controle de acesso conforme o perfil do usuário.
- Cadastro de postos de trabalho.
- Cadastro de insumos.
- Associação de insumos aos postos de trabalho.
- Abertura de cartões de reposição.
- Definição de urgência das solicitações.
- Prevenção de cartões duplicados para o mesmo posto e insumo.
- Leitura de QR Codes para identificação de postos.
- Quadro de logística dividido por status.
- Atualização dos cartões em tempo real.
- Confirmação de recebimento pelo operador.
- Registro de horários das etapas do atendimento.
- Acompanhamento do tempo de atendimento.
- Listagem com RecyclerView e indicação de estado vazio.
- Validação dos formulários.
- Preservação dos dados e do estado das telas durante a rotação do dispositivo.

## 6. Requisitos para execução

Para abrir e executar o projeto, são necessários:

- Android Studio;
- JDK compatível com Java 11;
- Git;
- Android SDK compatível com as configurações do projeto;
- Emulador Android ou dispositivo físico com Android 7.0 ou superior;
- Conexão com a internet;
- Projeto Firebase configurado.

## 7. Como executar o projeto

### 7.1 Clonar o repositório

No terminal, execute:

```bash
git clone https://github.com/oricoMiguel/Kanban-Mobile.git
```

Entre na pasta do projeto:

```bash
cd Kanban-Mobile-develop
```

Abra o projeto no Android Studio.

### 7.2 Configurar o Firebase

O aplicativo utiliza Firebase Authentication e Cloud Firestore.

Para configurar o ambiente:

1. Acesse o Firebase Console.
2. Crie ou selecione o projeto Firebase utilizado pela equipe.
3. Registre um aplicativo Android com o identificador `com.example.kanban`.
4. Baixe o arquivo `google-services.json`.
5. Coloque o arquivo dentro da pasta `app/` do projeto, localmente.
6. Habilite o provedor de autenticação por e-mail e senha.
7. Configure o Cloud Firestore.
8. Configure as regras de segurança e os índices necessários.

**Importante:** o arquivo `google-services.json` não deve ser enviado ao repositório público. Cada ambiente de desenvolvimento deve receber sua configuração Firebase de maneira segura. O arquivo deve permanecer localmente na pasta `app/`.

### 7.3 Executar o aplicativo

Após configurar o Firebase:

1. Aguarde a sincronização do Gradle.
2. Conecte um dispositivo Android ou inicie um emulador.
3. Selecione o dispositivo no Android Studio.
4. Clique em **Run** para executar o aplicativo.

### 7.4 Gerar o APK

Para gerar uma versão de depuração, utilize o terminal na pasta raiz do projeto.

**Windows:**

```bash
gradlew.bat assembleDebug
```

**Linux/macOS:**

```bash
./gradlew assembleDebug
```

O arquivo APK será gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 8. Estrutura do banco de dados

O aplicativo utiliza o Cloud Firestore para armazenar e sincronizar as informações.

As principais coleções são:

| Coleção | Descrição |
|---|---|
| `usuarios` | Dados dos usuários e seus perfis de acesso |
| `postos` | Cadastro dos postos de trabalho |
| `insumos` | Cadastro dos insumos |
| `cartoes` | Solicitações de reposição e seus status |
| `atendimentos` | Registros e informações dos atendimentos |

### 8.1 Usuários

Os documentos dos usuários são organizados pelo UID do Firebase Authentication:

```text
usuarios/{uid}
```

O campo `perfil` identifica o tipo de acesso do usuário:

- `operador`
- `logistica`

O perfil é definido conforme a autorização realizada pela equipe e não pode ser escolhido ou alterado livremente pelo próprio usuário.

### 8.2 Postos e insumos

Os postos utilizam identificadores no padrão:

```text
posto01
posto02
posto03
```

Os novos insumos recebem identificadores derivados de seus nomes, normalizados para facilitar a identificação.

Os insumos podem ser associados a um ou mais postos de trabalho.

### 8.3 Cartões de reposição

Cada cartão representa uma solicitação de reposição de determinado insumo em determinado posto.

O identificador do cartão segue o padrão:

```text
postoId_insumoId
```

Exemplo:

```text
posto07_parafuso-m6
```

Esse padrão permite identificar a combinação entre posto e insumo e evitar a criação de cartões duplicados para a mesma combinação.

### 8.4 Status dos cartões

Os cartões utilizam os seguintes estados:

| Status | Significado |
|---|---|
| `aberto` | Solicitação criada pelo operador |
| `em_rota` | Solicitação em atendimento pela logística |
| `entregue` | Insumo registrado como entregue pela logística |
| `concluido` | Recebimento confirmado pelo operador |

### 8.5 Registro de horários

O sistema registra os horários das etapas do atendimento, utilizando campos como:

- `abertoEm`: momento da abertura da solicitação.
- `emRotaEm`: momento em que a logística iniciou o atendimento.
- `entregueEm`: momento em que a logística registrou a entrega.
- `concluidoEm`: momento em que o operador confirmou o recebimento.
- `minutosTotal`: duração calculada do atendimento, conforme a implementação do sistema.

## 9. Regras de segurança

O projeto possui regras de segurança do Cloud Firestore no arquivo:

```text
firestore.rules
```

As regras controlam o acesso aos dados conforme a autenticação e o perfil do usuário.

O arquivo de índices do Firestore está em:

```text
firestore.indexes.json
```

A configuração do Firebase utilizada pelo projeto também possui o arquivo:

```text
firebase.json
```

Esses arquivos fazem parte da configuração do projeto e devem ser mantidos no repositório.

## 10. Engenharia de Software — ES1

### Histórias de usuário e critérios de aceitação

#### C1 — Cadastro de postos e insumos

**Objetivo:** permitir que a equipe de logística cadastre e organize os postos de trabalho e os insumos utilizados na produção.

**Critérios de aceitação:**

- **Dado** que um usuário da logística esteja autenticado, **quando** acessar a área de gerenciamento, **então** deverá visualizar os postos cadastrados.
- **Dado** que a logística queira cadastrar um posto, **quando** preencher as informações obrigatórias e confirmar, **então** o posto deverá ser salvo no Firestore.
- **Dado** que a logística queira cadastrar um insumo, **quando** informar seus dados e selecionar os postos relacionados, **então** o insumo deverá ser cadastrado e associado aos postos selecionados.
- **Dado** que existam postos e insumos cadastrados, **quando** o operador acessar seu posto, **então** deverá visualizar os insumos associados a ele.

#### C2 — Abrir cartão de reposição

**Objetivo:** permitir que o operador solicite a reposição de um insumo.

**Critérios de aceitação:**

- **Dado** que o operador esteja autenticado e tenha selecionado um posto, **quando** visualizar os insumos disponíveis, **então** poderá selecionar o insumo que precisa de reposição.
- **Dado** que o operador selecione um insumo, **quando** informar a urgência e confirmar a solicitação, **então** um cartão deverá ser criado com as informações do posto, do insumo e da urgência.
- **Dado** que o cartão seja criado, **então** deverá receber o status inicial `aberto` e registrar o horário de abertura.
- **Dado** que a solicitação tenha sido criada, **então** ela deverá ficar disponível para a equipe de logística.

#### C3 — Impedir cartões duplicados

**Objetivo:** evitar que existam solicitações duplicadas para o mesmo insumo no mesmo posto.

**Critérios de aceitação:**

- **Dado** que já exista um cartão para determinada combinação de posto e insumo, **quando** o operador tentar abrir outra solicitação para a mesma combinação, **então** o sistema deverá impedir a criação de um cartão duplicado.
- **Dado** que o identificador do cartão seja composto pelo posto e pelo insumo, **então** a mesma combinação deverá utilizar o mesmo identificador.
- **Dado** que uma combinação de posto e insumo ainda não possua cartão, **quando** o operador abrir uma solicitação, **então** o sistema poderá criar o cartão correspondente.

#### C4 — Quadro e movimentação dos cartões

**Objetivo:** permitir que a equipe de logística acompanhe e atualize as solicitações de reposição.

**Critérios de aceitação:**

- **Dado** que um usuário da logística esteja autenticado, **quando** acessar o quadro, **então** deverá visualizar as solicitações organizadas por status.
- **Dado** que existam cartões abertos, **então** eles deverão aparecer na coluna `Aberto`.
- **Dado** que a logística inicie o atendimento de um cartão, **quando** atualizar seu status, **então** ele deverá passar para `Em rota`.
- **Dado** que a logística registre a entrega, **quando** atualizar o status, **então** o cartão deverá passar para `Entregue`.
- **Dado** que os dados sejam atualizados no Firestore, **então** o quadro deverá refletir as alterações por meio da sincronização em tempo real.

#### C5 — Confirmação de recebimento

**Objetivo:** permitir que o operador confirme que recebeu o insumo solicitado.

**Critérios de aceitação:**

- **Dado** que um cartão esteja no status `Entregue`, **quando** o operador acessar sua solicitação, **então** deverá ter a possibilidade de confirmar o recebimento.
- **Dado** que o operador confirme o recebimento, **quando** a ação for concluída, **então** o cartão deverá passar para o status `Concluído`.
- **Dado** que o recebimento seja confirmado, **então** o sistema deverá registrar o horário da conclusão.
- **Dado** que o cartão tenha sido concluído, **então** ele não deverá permanecer como uma solicitação pendente de recebimento.

#### C6 — Registro de horários e tempo de atendimento

**Objetivo:** registrar os horários das etapas de reposição e permitir o acompanhamento da duração dos atendimentos.

**Critérios de aceitação:**

- **Dado** que um cartão seja criado, **quando** a solicitação for registrada, **então** o sistema deverá armazenar o horário de abertura.
- **Dado** que a logística inicie o atendimento, **quando** o status passar para `Em rota`, **então** o horário correspondente deverá ser registrado.
- **Dado** que a logística registre a entrega, **então** o sistema deverá armazenar o horário de entrega.
- **Dado** que o operador confirme o recebimento, **então** o sistema deverá registrar o horário de conclusão.
- **Dado** que existam os horários necessários, **então** o sistema deverá permitir calcular e acompanhar a duração do atendimento.

## 11. Engenharia de Software — ES2

### Backlog do projeto

O acompanhamento das atividades foi realizado por meio do GitHub Projects.

**Quadro do projeto:**  
https://github.com/users/oricoMiguel/projects/1

O backlog foi organizado em três etapas:

- **A fazer:** atividades ainda não iniciadas.
- **Fazendo:** atividades em desenvolvimento.
- **Feito:** atividades concluídas.

### Issues e responsáveis

| Issue | História | Responsáveis |
|---|---|---|
| #1 | C1: Cadastro de postos e insumos | Matheus e Quézia |
| #2 | C2: Abrir cartão de reposição | Akemi e Quézia |
| #3 | C3: Sem cartões duplicados | Akemi e Quézia |
| #4 | C4: Quadro e movimentação dos cartões | Miguel |
| #5 | C5: Confirmação de recebimento | Miguel |
| #6 | C6: Horários e tempo em aberto | Akemi |

## 12. Requisitos não funcionais e validações

Além das funcionalidades principais, o aplicativo contempla os seguintes requisitos:

- **NC1 — Autenticação:** cadastro e login com Firebase Authentication, persistência de sessão e logout.
- **NC2 — Persistência e sincronização:** utilização do Cloud Firestore e listeners para atualização dos dados em tempo real.
- **NC3 — Segurança:** regras do Firestore para controle de acesso conforme autenticação e perfil.
- **NC4 — Controle de perfil:** perfil armazenado em `usuarios/{uid}.perfil`, sem escolha livre pelo usuário durante o cadastro.
- **NC5 — Listagens:** utilização de RecyclerView e tratamento de listas vazias.
- **NC6 — Validação:** validação dos formulários de cadastro e das informações obrigatórias.
- **NC7 — Rotação de tela:** preservação dos dados e do estado das telas durante a mudança de orientação do dispositivo.
- **NC8 — Documentação:** documentação do projeto, tecnologias, configuração, execução, funcionalidades, histórias de usuário, equipe e evidências.

## 13. Evidências do aplicativo

Esta seção deve apresentar capturas de tela das principais funcionalidades do aplicativo.

Adicionar as imagens na pasta `docs/screenshots/` e inserir os arquivos abaixo no README:

| Tela | Arquivo |
|---|---|
| Tela de login | `docs/screenshots/login.jpeg` |
| Cadastro de usuário | `docs/screenshots/cadastro.jpeg` |
| Tela do operador | `docs/screenshots/operador.jpeg` |
| Cadastro de insumo | `docs/screenshots/novo-insumo.jpeg` |
| Quadro da logística | `docs/screenshots/quadro-logistica.jpeg` |
| Cartões em atendimento | `docs/screenshots/cartoes.jpeg` |

Exemplo de inclusão de uma imagem:

```markdown
![Quadro da logística](docs/screenshots/quadro-logistica.png)
```

As capturas devem mostrar as telas reais do aplicativo, sem expor senhas, dados pessoais ou informações privadas.

## 14. Contas para teste

Para facilitar a avaliação do aplicativo, disponibilizar contas de teste para os dois perfis.

| Perfil | E-mail de teste | Senha |
|---|---|---|
| Operador | PREENCHER COM E-MAIL DE TESTE | Disponibilizar separadamente |
| Logística | PREENCHER COM E-MAIL DE TESTE | Disponibilizar separadamente |

**Importante:** as contas de teste devem estar previamente cadastradas no Firebase Authentication. A conta de logística também deve possuir o perfil autorizado no documento correspondente do Firestore.

Não inserir senhas reais ou credenciais privadas em um repositório público. Caso necessário, disponibilizar os dados de acesso ao avaliador por um canal privado.

## 15. Estrutura principal do repositório

A estrutura principal do projeto está organizada da seguinte forma:

```text
Kanban-Mobile/
├── app/
│   └── src/
├── docs/
│   └── screenshots/
├── gradle/
├── tools/
├── .gitignore
├── build.gradle.kts
├── firebase.json
├── firestore.indexes.json
├── firestore.rules
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
└── README.md
```

O arquivo `google-services.json` é necessário localmente para a integração com o Firebase, mas não deve ser incluído no repositório público.

## 16. Equipe

| Integrante | Participação |
|---|---|
| Miguel | Desenvolvimento do quadro de logística, movimentação dos cartões e confirmação de recebimento |
| Akemi | Desenvolvimento das funcionalidades relacionadas às histórias C2, C3 e C6 |
| Matheus | Desenvolvimento das funcionalidades de cadastro de postos e insumos |
| Quézia | Desenvolvimento das funcionalidades de cadastro e abertura de cartões |

As responsabilidades detalhadas por história estão descritas na seção de backlog do projeto.

---

**Projeto:** Kanban de Reposição  
**Plataforma:** Android  
**Linguagem:** Java  
**Banco de dados:** Firebase Cloud Firestore  
**Autenticação:** Firebase Authentication  
**Controle de versão:** Git e GitHub
