# Cenários de aceitação (Cucumber)

Um ficheiro `.feature` por história, numa pasta por épico, em português (`# language: pt`).

```
features/
├── smoke/          cenários @smoke do esqueleto
├── E1/US-E1-01_contas_staff.feature
├── E4/US-E4-01_ciclo_vida_veiculo.feature
└── …
```

## Etiquetas

| Etiqueta | Uso |
| :- | :- |
| `@REQ_SCRUM-<n>` | liga o cenário à história no Xray (chave Jira da história) |
| `@TEST_SCRUM-<n>` | liga o cenário ao seu Test no Xray (chave do Test); fica na linha acima do `Cenário` |
| `@US-Ex-yy` | identificador da história |
| `@Ex` | épico |
| `@BRx` | regra de negócio exercitada (uma por regra) |
| `@wip` | cenário escrito pela QA e ainda não implementado: **não corre** no CI |
| `@quarentena` | cenário instável à espera de correção: **não corre** no CI (abrir Bug) |
| `@smoke` | verificação rápida da aplicação |

## Fluxo

1. A QA publica o `.feature` com `@wip` um dia antes (subtarefa 1 da história).
2. Quem implementa escreve os passos numa classe em `pt.marazul.cucumber.steps` e reutiliza `ApiSteps`.
3. Tira o `@wip` no mesmo PR da história; o CI corre o cenário e o job `xray` envia o resultado.
4. Num cenário novo, o job `xray` cria o Test no primeiro merge para a `main`. No PR seguinte acrescenta `@TEST_SCRUM-<n>` com a chave criada: sem essa etiqueta o Xray não associa o resultado ao Test.

Correr só uma história: `mvn verify -Dcucumber.filter.tags="@US-E4-01"`.
