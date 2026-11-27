# language: pt
@REQ_SCRUM-30 @US-E8-01 @E8
Funcionalidade: Desbloqueio na janela de levantamento
  Como hóspede
  Quero desbloquear a bicicleta que reservei
  Para começar a viagem

  @TEST_SCRUM-267 @BR6
  Cenário: Desbloqueio no último segundo da janela de levantamento
    Dado uma reserva da Marta que começou às 15h02
    Quando a Marta desbloqueia às 15h17 e 0 s
    Então a viagem começa
    E a bicicleta passa a "Em uso"

  @TEST_SCRUM-265 @BR6
  Cenário: Desbloqueio um segundo depois de a janela terminar
    Dado uma reserva da Marta que começou às 15h02
    Quando a Marta desbloqueia às 15h17 e 1 s
    Então a resposta tem o estado 422
    E o campo "rule" da resposta é "BR6"
    E a reserva passa a "Expirada"

  @TEST_SCRUM-268 @BR6 @BR9 @wip
  Cenário: A caução é libertada quando a reserva expira no desbloqueio
    Dado uma reserva da Marta que começou às 15h02
    Quando a Marta desbloqueia às 15h17 e 1 s
    Então a caução da Marta é libertada

  @TEST_SCRUM-269 @BR6
  Cenário: Uma reserva que não é desbloqueada expira sozinha
    Dado uma reserva da Marta que começou às 15h02
    Quando passam 15 minutos e 1 s sobre o início
    Então a reserva passa a "Expirada"
    E a bicicleta passa a "Disponível"

  @TEST_SCRUM-266
  Cenário: Um hóspede não desbloqueia a reserva de outro
    Dado uma reserva do Pedro que começou às 15h02
    Quando a Marta desbloqueia às 15h05 e 0 s
    Então a resposta tem o estado 403
