# language: pt
@REQ_SCRUM-23 @US-E6-01 @E6 @BR1 @wip
Funcionalidade: Pesquisa de bicicletas por hub e tipo
  Como hóspede
  Quero ver as bicicletas disponíveis num hub
  Para escolher uma que possa reservar

  Contexto:
    Dado que a Marta está autenticada como hóspede
    E que o mínimo de bateria para reservar vem do contrato em vigor

  Cenário: Das bicicletas de 24 % e de 25 % só a de 25 % é mostrada
    Dado o hub "Hotel Alvina Mar" com bicicletas Disponíveis de 24 % e de 25 %
    Quando a Marta pesquisa as bicicletas do hub "Hotel Alvina Mar"
    Então a resposta tem o estado 200
    E só vê a bicicleta de 25 %

  Esquema do Cenário: A bateria mínima do contrato decide se a bicicleta aparece
    Dado o hub "Hotel Alvina Mar" com uma bicicleta Disponível com <bateria> % de bateria
    Quando a Marta pesquisa as bicicletas do hub "Hotel Alvina Mar"
    Então a resposta tem o estado 200
    E a lista de bicicletas tem <quantidade> bicicleta(s)

    Exemplos:
      | bateria | quantidade |
      | 24      | 0          |
      | 25      | 1          |

  Cenário: Uma bicicleta em Manutenção não aparece
    Dado o hub "Hotel Alvina Mar" com a bicicleta "B-17" em "Manutenção"
    E uma bicicleta Disponível de 80 % no mesmo hub
    Quando a Marta pesquisa as bicicletas do hub "Hotel Alvina Mar"
    Então a resposta tem o estado 200
    E a bicicleta "B-17" não aparece na lista

  Cenário: A Marta filtra por tipo e só vê o tipo escolhido
    Dado o hub "Hotel Alvina Mar" com bicicletas Disponíveis de dois tipos
    Quando a Marta pesquisa as bicicletas do hub "Hotel Alvina Mar" do tipo "ELECTRIC"
    Então a resposta tem o estado 200
    E só vê bicicletas do tipo "ELECTRIC"
