# language: pt
@REQ_SCRUM-21 @US-E1-01 @E1 @wip
Funcionalidade: Contas de staff com papéis
  Como administradora
  Quero criar contas de staff com um papel
  Para que cada pessoa aceda só ao que o seu trabalho precisa

  Cenário: A Sofia cria a conta da Nídia com o papel técnico
    Dado que a Sofia está autenticada como administradora
    Quando envio para "/api/auth/staff" o pedido:
      """
      {"email":"nidia@marazul.test","password":"palavra-passe-da-nidia","role":"TECHNICIAN"}
      """
    Então a resposta tem o estado 201
    E o campo "role" da resposta é "TECHNICIAN"

  Cenário: A Nídia entra com a conta criada e só vê os bilhetes atribuídos a si
    Dado que a Sofia criou a conta da Nídia com o papel técnico
    Quando a Nídia entra com o email "nidia@marazul.test" e a palavra-passe certa
    Então a resposta tem o estado 200
    E a Nídia só vê os bilhetes atribuídos a si

  Cenário: Uma conta com o papel técnico não altera o contrato
    Dado uma conta com o papel técnico autenticada
    Quando envio um PUT para "/api/contract" com o pedido:
      """
      {}
      """
    Então a resposta tem o estado 403

  Esquema do Cenário: Só a administradora cria contas de staff
    Dado uma conta com o papel <papel> autenticada
    Quando envio para "/api/auth/staff" o pedido:
      """
      {"email":"novo@marazul.test","password":"palavra-passe-longa","role":"TECHNICIAN"}
      """
    Então a resposta tem o estado 403
    E não fica nenhuma conta nova com o email "novo@marazul.test"

    Exemplos:
      | papel              |
      | coordenador        |
      | técnico            |
      | gestora do cliente |

  Cenário: Sem autenticação a criação de contas é recusada
    Dado que ninguém está autenticado
    Quando envio para "/api/auth/staff" o pedido:
      """
      {"email":"novo@marazul.test","password":"palavra-passe-longa","role":"TECHNICIAN"}
      """
    Então a resposta tem o estado 401

  Cenário: Um email já registado não cria uma segunda conta
    Dado que a Sofia está autenticada como administradora
    E que já existe uma conta com o email "nidia@marazul.test"
    Quando envio para "/api/auth/staff" o pedido:
      """
      {"email":"nidia@marazul.test","password":"outra-palavra-passe","role":"COORDINATOR"}
      """
    Então a resposta tem o estado 409
    E continua a existir uma só conta com o email "nidia@marazul.test"

  Cenário: Uma conta desativada não entra com a palavra-passe certa
    Dado que existe a conta desativada da Nídia com o email "nidia@marazul.test"
    Quando a Nídia entra com o email "nidia@marazul.test" e a palavra-passe certa
    Então a resposta tem o estado 401

  Cenário: A sessão fica num cookie HttpOnly, Secure e SameSite Strict
    Dado que existe a conta ativa da Nídia com o email "nidia@marazul.test"
    Quando a Nídia entra com o email "nidia@marazul.test" e a palavra-passe certa
    Então a resposta tem o estado 200
    E o cookie de sessão é HttpOnly, Secure e SameSite "Strict"
