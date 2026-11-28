# language: pt
@REQ_SCRUM-22 @US-E3-01 @E3 @BR12 @wip
Funcionalidade: Hubs e pontos de interesse
  Como operador
  Quero registar hubs e pontos de interesse com a sua localização
  Para que o sistema saiba onde se levantam e devolvem bicicletas

  Cenário: O Rui cria um hub com 4 docas
    Dado que o Rui está autenticado como operador
    Quando envio para "/api/hubs" o pedido:
      """
      {"name":"São Brás","docks":4,"latitude":38.7223,"longitude":-9.1393}
      """
    Então a resposta tem o estado 201
    E o campo "docks" da resposta é "4"

  Esquema do Cenário: O número de docas de um hub respeita as fronteiras da BR12
    Dado que o Rui está autenticado como operador
    Quando o Rui cria um hub com <docas> docas
    Então a resposta tem o estado <estado>

    Exemplos:
      | docas | estado |
      | 4     | 201    |
      | 40    | 201    |

  Esquema do Cenário: Um hub fora do intervalo de docas é recusado com a BR12
    Dado que o Rui está autenticado como operador
    Quando o Rui cria um hub com <docas> docas
    Então a resposta tem o estado 422
    E o campo "rule" da resposta é "BR12"

    Exemplos:
      | docas |
      | 3     |
      | 41    |

  Cenário: A base de dados também recusa um hub com 41 docas
    Quando é inserido na base de dados um hub com 41 docas
    Então a inserção é recusada pela restrição de 4 a 40 docas

  Cenário: O Miradouro da Costa Norte aparece como destino e não como local de devolução
    Dado o Miradouro da Costa Norte registado como ponto de interesse
    E que o Hotel Alvina Mar está registado como hub
    Quando um hóspede autenticado consulta "/api/locations"
    Então a resposta tem o estado 200
    E o local "Miradouro da Costa Norte" é um destino
    E o local "Miradouro da Costa Norte" não é um local de devolução

  Cenário: Um hóspede vê os hubs com as docas livres e os pontos de interesse
    Dado que o Hotel Alvina Mar está registado como hub com 12 docas e 5 bicicletas
    E o Miradouro da Costa Norte registado como ponto de interesse
    Quando um hóspede autenticado consulta "/api/locations"
    Então a resposta tem o estado 200
    E o hub "Hotel Alvina Mar" tem 7 docas livres
    E o local "Miradouro da Costa Norte" está na lista

  Cenário: A lista de locais não traz dados de outros hóspedes
    Dado que o Hotel Alvina Mar está registado como hub
    Quando um hóspede autenticado consulta "/api/locations"
    Então a resposta tem o estado 200
    E a resposta não contém dados de outros hóspedes
