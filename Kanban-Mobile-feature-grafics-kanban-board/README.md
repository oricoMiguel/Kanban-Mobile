# Kanban de Reposição

App mobile que substitui o grito, a luz e a ida pessoal ao almoxarifado. Quando um insumo está acabando no posto de montagem, o operador abre um cartão de reposição e a logística atende pelo quadro, vendo o que falta, onde e há quanto tempo.

## Perfis

- **Operador de linha:** sinaliza a falta abrindo um cartão e confirma o recebimento.
- **Logística:** cadastra postos e insumos, vê o quadro e move os cartões.

## Fluxo do cartão

**Aberto → Em rota → Entregue → Concluído**

A logística move o cartão até **Entregue**. Somente o operador conclui, ao confirmar o recebimento.

---

## ES1: Histórias de usuário

Cada requisito CORE virou uma história, com critérios de aceite em Dado / Quando / Então. Cada história tem uma issue correspondente no repositório.

| Requisito | História | Issue |
|---|---|---|
| C1 | Cadastro de postos e insumos | #1 |
| C2 | Abrir cartão de reposição | #2 |
| C3 | Sem cartões duplicados | #3 |
| C4 | Quadro e movimentação dos cartões | #4 |
| C5 | Confirmação de recebimento | #5 |
| C6 | Horários e tempo em aberto | #6 |

### C1: Cadastro de postos e insumos (#1)

**Como** logística, **quero** cadastrar postos de trabalho e os insumos usados em cada posto, **para** que o operador veja apenas o que realmente usa.

**Critérios de aceite**

- **CA1.** **Dado** que estou na lista de postos, **quando** informo nome e setor e salvo, **então** o posto aparece na lista.
- **CA2.** **Dado** um posto cadastrado, **quando** adiciono um insumo a ele, **então** o insumo aparece na lista de insumos daquele posto e fica disponível para o operador desse posto.
- **CA3.** **Dado** que o insumo já está no posto, **quando** tento adicioná-lo de novo, **então** o app avisa e não cria duplicata.

### C2: Abrir cartão de reposição (#2)

**Como** operador de linha, **quero** abrir um cartão escolhendo o insumo e a urgência, **para** que a logística saiba o que falta e com que pressa.

**Critérios de aceite**

- **CA1.** **Dado** que estou na tela do meu posto, **quando** escolho um insumo e uma urgência e toco em "Pedir reposição", **então** um cartão aparece na coluna Aberto do quadro.
- **CA2.** **Dado** que não escolhi nenhum insumo, **quando** toco em "Pedir reposição", **então** o app mostra um erro e não cria o cartão.
- **CA3.** **Dado** que abro a tela de pedir, **quando** a lista de insumos carrega, **então** vejo apenas os insumos do meu posto.

### C3: Sem cartões duplicados (#3)

**Como** operador de linha, **quero** que o app impeça dois cartões abertos para o mesmo insumo no mesmo posto, **para** não gerar pedidos repetidos para a logística.

**Critérios de aceite**

- **CA1.** **Dado** que existe um cartão aberto de Parafuso M6 no Posto 07, **quando** abro a tela de pedir nesse posto, **então** Parafuso M6 aparece indisponível, com "Já pedido há X min".
- **CA2.** **Dado** que o cartão com id `postoId_insumoId` já existe, **quando** outro pedido tenta criar o mesmo id, **então** o app recusa e não cria um segundo documento.
- **CA3.** **Dado** que o cartão anterior foi concluído, **quando** o operador pede o mesmo insumo de novo, **então** um novo cartão é criado normalmente.

### C4: Quadro e movimentação dos cartões (#4)

**Como** logística, **quero** ver o quadro com as colunas Aberto, Em rota e Entregue em tempo real e mover os cartões, **para** atender os pedidos na ordem certa.

**Critérios de aceite**

- **CA1.** **Dado** que há cartões abertos, **quando** abro o quadro, **então** vejo as três colunas com a contagem de cada uma e os mais antigos primeiro.
- **CA2.** **Dado** um cartão em Aberto, **quando** toco em "Pegar e sair para entrega", **então** ele vai para Em rota e some de Aberto.
- **CA3.** **Dado** um cartão em Em rota, **quando** toco em "Marcar como entregue", **então** ele vai para Entregue com o aviso "Aguardando confirmação".
- **CA4.** **Dado** que o quadro está aberto, **quando** um operador abre um novo cartão, **então** ele aparece no quadro sem eu recarregar a tela.

### C5: Confirmação de recebimento (#5)

**Como** operador de linha, **quero** confirmar o recebimento do insumo, **para** que o cartão só seja concluído quando o material chegou de fato.

**Critérios de aceite**

- **CA1.** **Dado** um cartão do meu posto em Entregue, **quando** toco em "Confirmar recebimento", **então** o cartão é concluído e sai do quadro.
- **CA2.** **Dado** um cartão em Aberto ou Em rota, **quando** abro o detalhe, **então** não existe o botão de confirmar.
- **CA3.** **Dado** que a logística marcou Entregue, **quando** o operador ainda não confirmou, **então** o cartão continua visível como "Aguardando confirmação".

### C6: Horários e tempo em aberto (#6)

**Como** logística, **quero** ver há quantos minutos cada cartão está aberto e o horário de cada mudança de status, **para** priorizar os pedidos mais antigos e medir o atendimento.

**Critérios de aceite**

- **CA1.** **Dado** um cartão aberto, **quando** olho o quadro, **então** vejo "há X min" contado desde a abertura, e o valor se atualiza com o tempo.
- **CA2.** **Dado** que o cartão mudou de status, **quando** abro o detalhe, **então** a linha do tempo mostra o horário de cada mudança (Aberto, Em rota, Entregue e Concluído).
- **CA3.** **Dado** um cartão concluído, **quando** abro o detalhe, **então** vejo o tempo total de atendimento.

---

## ES2: Backlog

O backlog fica no [GitHub Projects](https://github.com/users/oricoMiguel/projects/1), com as colunas **A fazer**, **Fazendo** e **Feito**.

- Cada história é uma issue, com um responsável definido.
- Os critérios de aceite estão como checklist no corpo de cada issue.
- Uma issue só vai para **Feito** quando todos os critérios de aceite funcionam no app.

| Issue | Título | Responsável |
|---|---|---|
| #1 | C1: Cadastro de postos e insumos | a definir |
| #2 | C2: Abrir cartão de reposição | a definir |
| #3 | C3: Sem cartões duplicados | a definir |
| #4 | C4: Quadro e movimentação dos cartões | a definir |
| #5 | C5: Confirmação de recebimento | a definir |
| #6 | C6: Horários e tempo em aberto | a definir |
