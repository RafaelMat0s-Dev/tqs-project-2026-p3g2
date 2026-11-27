# MarAzul

Plataforma de gestão de bicicletas elétricas partilhadas, entregue como serviço pela MarAzul Mobility ao Grupo Costa Alvina (três hotéis fictícios na Ilha de Santa Clara). Os hóspedes entram com um código pessoal recebido no check-in, sem criar conta. O diferenciador é o **motor de autonomia**: só propõe bicicletas com carga para chegar ao destino e, se o destino não tiver hub, também para voltar.

Projeto da unidade curricular Teste e Qualidade de Software (45426), Universidade de Aveiro.

## Equipa

| Pessoa | Papel |
| :- | :- |
| Rafael Matos | Team Coordinator |
| Henrique Reveles | Product Owner |
| Tiago Vieira | DevOps Master |
| Geovana Lourenço | QA Engineer |
| Henrique Madeira | UI/UX Engineer |

## Arrancar

Só é preciso Docker.

```bash
docker compose up --build
```

| Serviço | Endereço |
| :- | :- |
| Aplicação web (nginx, que encaminha a API) | http://localhost:5173 |
| API e estado | http://localhost:5173/actuator/health |
| Documentação da API (Swagger UI) | http://localhost:5173/swagger-ui.html |
| Métricas Prometheus | http://localhost:5173/actuator/prometheus |
| Simulador de telemetria | http://localhost:8090/health |
| PostgreSQL / Mosquitto (MQTT) | `localhost:5432` / `localhost:1883` |

Com a análise de qualidade local (SonarQube com o quality gate do projeto):

```bash
docker compose --profile quality up --build   # aplicação + SonarQube em http://localhost:9000
./scripts/quality.sh                           # testes, cobertura e análise do backend e do frontend
```

As portas e as palavras-passe locais podem ser mudadas num ficheiro `.env` (ver [.env.example](.env.example)).

## Estrutura

| Pasta | Conteúdo |
| :- | :- |
| `backend/` | API Spring Boot 4.1 (Java 21), monólito modular com portos e adaptadores: um pacote por épico (`identity`, `contract`, `network`, `fleet`, `telemetry`, `booking`, `autonomy`, `trip`, `payment`, `maintenance`, `metrics`, `gbfs`) mais `shared` |
| `frontend/` | Interface web React 19 + TypeScript (Vite), servida por nginx |
| `simulator/` | Simulador da telemetria da frota (contentor próprio, MQTT) |
| `perf/` | Testes de carga k6 com os limiares dos SLO |
| `infra/` | Configuração do Mosquitto e do SonarQube |
| `docs/` | Especificação do produto, Manual de Qualidade e apresentação |

## Testes e qualidade

| Camada | Ferramenta | Comando |
| :- | :- | :- |
| Unitários (`*Test`) e arquitetura | JUnit 6, AssertJ, Mockito, ArchUnit | `cd backend && mvn test` |
| Integração (`*IT`) | Testcontainers (PostgreSQL, Mosquitto), REST Assured, WireMock, Awaitility | `cd backend && mvn verify` |
| Aceitação (BDD) | Cucumber, cenários em português em `backend/src/test/resources/features` | `cd backend && mvn verify` |
| Cobertura | JaCoCo: ≥ 80 % no total e ≥ 90 % em cada pacote `domain` (o build falha abaixo disso) | `cd backend && mvn verify` |
| Frontend | ESLint, Prettier, TypeScript, Vitest + Testing Library (≥ 80 %) | `cd frontend && npm run lint && npm run test:coverage` |
| Ponta a ponta | Playwright + axe (WCAG 2.1 AA), desktop e 360 px | `cd frontend && npm run e2e` (com a aplicação a correr) |
| Acessibilidade | Lighthouse CI, pontuação ≥ 90 (SLO6) | `cd frontend && npm run build && npm run lighthouse` |
| Carga | k6 com limiares de SLO1, SLO2 e SLO4 | `k6 run -e BASE_URL=http://localhost:5173 perf/smoke.js` |
| Análise estática | SonarQube com o quality gate «MarAzul» | `./scripts/quality.sh` |
| Dependências | Trivy e Dependabot | job `dependencias` do CI |

O pipeline (`.github/workflows/ci.yml`) corre em cada push e PR: `backend`, `frontend`, `simulator`, `dependencias`, `compose` (sobe a aplicação com `docker compose up` e faz um teste de fumo) e `quality-gate` (SonarQube em contentor, bloqueia o merge se o gate falhar). Os workflows `E2E` e `Performance` correm à noite ou a pedido.

As regras para contribuir estão em [CONTRIBUTING.md](CONTRIBUTING.md); as instruções para agentes de IA em [AGENTS.md](AGENTS.md).

## Ligações

| O quê | Onde |
| :- | :- |
| Backlog Jira | https://ua-team-aqweupa3.atlassian.net/jira/software/projects/SCRUM/boards/1 |
| Documentação da API | `/swagger-ui.html` no ambiente implantado |
| Análise estática | SonarQube local (`docker compose --profile quality up`) e resumo em cada execução do job `quality-gate` |
| Observabilidade | a acrescentar (Prometheus e Grafana, SCRUM-237) |
| Aplicação implantada | a acrescentar (entrega contínua na VM, SCRUM-232) |
