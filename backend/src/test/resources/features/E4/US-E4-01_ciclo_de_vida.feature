# language: pt
@REQ_SCRUM-20 @US-E4-01 @E4 @wip
Funcionalidade: Registo e ciclo de vida do veículo
  Como coordenador de operações
  Quero registar bicicletas e gerir o seu estado
  Para que só as bicicletas em condições circulem

  Cenário: Uma bicicleta acabada de registar está Inativa e fica Disponível depois de ativada
    Dado estou autenticado como coordenador
    E a bicicleta "B-17" acabada de registar no hub "sao-bras"
    Quando o Rui consulta a bicicleta "B-17"
    Então a resposta tem o estado 200
    E o campo "status" da resposta é "DISABLED"
    Quando o Rui ativa a bicicleta "B-17"
    E o Rui consulta a bicicleta "B-17"
    Então o campo "status" da resposta é "AVAILABLE"

  Cenário: Uma bicicleta Abatida não pode ser ativada
    Dado estou autenticado como coordenador
    E a bicicleta "B-17" está "Abatida"
    Quando o Rui ativa a bicicleta "B-17"
    Então a resposta tem o estado 422
    E o campo "rule" da resposta é "VEHICLE_LIFECYCLE"
    E a mensagem da resposta contém "Abatida → Disponível"

  Cenário: Uma bicicleta Reservada não pode ser desativada
    Dado estou autenticado como coordenador
    E a bicicleta "B-17" está "Reservada"
    Quando o Rui desativa a bicicleta "B-17"
    Então a resposta tem o estado 422
    E o campo "rule" da resposta é "VEHICLE_LIFECYCLE"
    E a mensagem da resposta contém "Reservada → Inativa"

  @BR1
  Cenário: Uma bicicleta em Manutenção não pode ser reservada
    Dado a bicicleta "B-17" está "Manutenção" no hub "hotel-alvina-mar"
    E estou autenticado como hóspede
    Quando a Marta tenta reservar a bicicleta "B-17"
    Então a resposta tem o estado 422
    E o campo "rule" da resposta é "BR1"
