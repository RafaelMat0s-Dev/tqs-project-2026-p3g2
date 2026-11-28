# language: pt
@REQ_SCRUM-26 @US-E3-02 @E3 @BR2 @wip
Funcionalidade: Troços com distância e desnível
  Como operador
  Quero registar os troços entre locais com a distância e o desnível
  Para que o motor de autonomia estime o consumo

  Contexto:
    Dado que o Rui está autenticado como operador
    E que o Hotel Alvina Mar e a Praia da Ribeira estão registados como locais

  Cenário: O troço Hotel Alvina Mar para Praia da Ribeira consome 14,1 pp
    Quando o Rui regista o troço do "Hotel Alvina Mar" para a "Praia da Ribeira" com 7 km, 60 m de subida e 10 m de descida
    Então a resposta tem o estado 201
    E o consumo calculado do troço é de 14,1 pp

  Cenário: O troço inverso tem a subida e a descida trocadas e consome 11,1 pp
    Dado o troço do "Hotel Alvina Mar" para a "Praia da Ribeira" com 7 km, 60 m de subida e 10 m de descida
    Quando se pede o troço inverso
    Então a resposta tem o estado 200
    E o troço tem 7 km, 10 m de subida e 60 m de descida
    E o consumo calculado do troço é de 11,1 pp

  Cenário: O Rui não regista duas vezes o mesmo troço
    Dado o troço do "Hotel Alvina Mar" para a "Praia da Ribeira" com 7 km, 60 m de subida e 10 m de descida
    Quando o Rui regista o troço do "Hotel Alvina Mar" para a "Praia da Ribeira" com 7 km, 60 m de subida e 10 m de descida
    Então a resposta tem o estado 409
    E continua a existir um só troço do "Hotel Alvina Mar" para a "Praia da Ribeira"
