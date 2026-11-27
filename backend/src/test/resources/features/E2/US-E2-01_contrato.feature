# language: pt
@REQ_SCRUM-25 @US-E2-01 @E2 @BR9 @BR10 @BR11 @BR15 @wip
Funcionalidade: Contrato base: tarifário, caução e metas de SLA
  Como administradora
  Quero registar o contrato com o tarifário, a caução e as metas de SLA
  Para que os encargos e os prazos usem os valores acordados e não constantes do código

  Cenário: O operador consulta o contrato v1
    Dado o Rui autenticado como "operador"
    Quando consulto "/api/contract"
    Então a resposta tem o estado 200
    E o contrato apresentado tem:
      | desbloqueio             | 1,00 €  |
      | preço por minuto        | 0,05 €  |
      | caução                  | 25,00 € |
      | sobretaxa por minuto    | 0,10 €  |
      | tolerância da sobretaxa | 5 min   |
      | limite da sobretaxa     | 25,00 € |
      | prazo crítico           | 4 h     |
      | prazo alto              | 24 h    |
      | prazo normal            | 72 h    |

  Cenário: A versão 2 não altera o encargo de uma viagem cobrada com a versão 1
    Dado a Sofia autenticada como "administradora"
    E uma viagem de 53 minutos cobrada com a versão 1 do contrato
    Quando a Sofia publica a versão 2 do contrato com "0,07 €" por minuto a partir de "2030-01-01T00:00:00Z"
    Então a resposta tem o estado 201
    E a versão 2 do contrato tem "0,07 €" por minuto
    E a versão 1 do contrato tem "0,05 €" por minuto
    E o encargo dessa viagem continua a ser "3,65 €"

  Cenário: A gestora do cliente não pode alterar o tarifário
    Dado a Helena autenticada como "gestora do cliente"
    Quando a Helena publica a versão 2 do contrato com "0,07 €" por minuto a partir de "2030-01-01T00:00:00Z"
    Então a resposta tem o estado 403
