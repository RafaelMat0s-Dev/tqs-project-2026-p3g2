# Como contribuir para o MarAzul

Regras curtas para todos (prática Q5). O detalhe está no Manual de Qualidade, em `docs/`.

## 1. Fluxo de uma história

1. Pegar numa história da sprint ativa no Jira e passá-la a **In Progress**.
2. Criar o ramo a partir do `main`: `feature/SCRUM-<n>-descricao-curta` (por exemplo `feature/SCRUM-20-ciclo-vida-veiculo`).
3. A QA já publicou o `.feature` com `@wip`. Implementar em pequenos commits:
   - `test(<modulo>): SCRUM-<n> <o que se testa> (BRx)` antes de `feat(<modulo>): SCRUM-<n> <o que se faz> (BRx)` quando é TDD;
   - outros prefixos: `fix`, `refactor`, `docs`, `chore`, `ci`.
4. Correr localmente `cd backend && mvn verify` e `cd frontend && npm run test:coverage`; antes do PR, `./scripts/quality.sh`.
5. Abrir o PR com a chave no título (`SCRUM-<n> …`), o template preenchido e **uma** etiqueta `ia:nenhuma`, `ia:assistida` ou `ia:agente`. Passar a história a **In Review**.
6. Outra pessoa revê em menos de 4 horas úteis. Integrar só por **squash** e com o CI verde.
7. O PO aceita a história contra os critérios; só então passa a **Done**.

## 2. Estilo de código

| Onde | Regra |
| :- | :- |
| Java | Identificadores em inglês; injeção por construtor; `record` para DTO e objetos de valor; sem `System.out` (ArchUnit) |
| Domínio | Java puro em `domain/`; regras recebem um `Clock`; dinheiro em cêntimos (`long`) |
| Erros | `BusinessRuleViolation("BRx", "mensagem em português")` (422) ou `BusinessRuleConflict` (409) |
| Módulos | só se usa outro módulo pelo seu pacote `api`; nada de acesso às tabelas de outro módulo |
| Base de dados | só por migrações Flyway novas (`V<n>__<modulo>_<o_que>.sql`); nunca editar uma migração já integrada |
| TypeScript | ESLint e Prettier sem avisos; componentes funcionais; chamadas à API só por `apiFetch` |
| Interface | texto em português de Portugal; acessível (axe sem violações, Lighthouse ≥ 90); funciona a 360 px |
| Testes | nomes que descrevem o comportamento; fronteiras das regras parametrizadas; sem `Thread.sleep` |

`.editorconfig` define a indentação: 4 espaços em Java, 2 no resto.

## 3. Lista de verificação da revisão

- [ ] O título, o ramo e os commits têm a chave `SCRUM-<n>`; o PR tem uma etiqueta `ia:*`.
- [ ] Todos os critérios da história têm um cenário no `.feature`, sem `@wip`.
- [ ] As fronteiras de cada regra BR estão testadas (por exemplo 24 % e 25 %).
- [ ] Há `*IT` quando a alteração toca na base de dados, num adaptador ou num endpoint, incluindo 403, 409 e 422.
- [ ] O erro de uma regra violada devolve o ID da regra (`rule`).
- [ ] Nenhum `now()` no domínio; nenhum valor de regra fixo no código.
- [ ] Sem segredos nem dados pessoais; dados de teste sintéticos.
- [ ] CI verde, incluindo `quality-gate` e `dependencias`.
- [ ] A secção do manual de utilização foi escrita (quando a história tem interface).
- [ ] Os achados da revisão por IA estão registados no PR como aceites ou rejeitados, com o motivo.

## 4. Uso de IA

- Assistentes e agentes podem escrever código, testes e documentação, dentro da política do Manual de Qualidade.
- Um agente **nunca** aprova nem integra um PR; um humano é responsável por cada alteração e tem de a saber explicar.
- Não dar a serviços de IA segredos, credenciais nem dados pessoais.

## 5. Referências

- Manual de Qualidade e especificação do produto: `docs/`.
- Instruções para agentes: [AGENTS.md](AGENTS.md).
- Cenários e etiquetas do Cucumber: [backend/src/test/resources/features/README.md](backend/src/test/resources/features/README.md).
