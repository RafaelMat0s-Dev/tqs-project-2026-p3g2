# language: pt
@smoke
Funcionalidade: Saúde da aplicação e contrato de erros
  Para saber que o MarAzul está pronto a receber pedidos
  Como equipa de desenvolvimento
  Queremos que a API responda às sondas de saúde e devolva o ID da regra violada

  Cenário: A API está pronta a receber pedidos
    Quando consulto "/actuator/health/readiness"
    Então a resposta tem o estado 200
    E o campo "status" da resposta é "UP"

  Cenário: Uma regra de negócio violada devolve o identificador da regra
    Quando envio para "/api/bookings" o pedido:
      """
      {"vehicleId":"00000000-0000-0000-0000-000000000000","guestId":"marta","startsAt":"2099-01-01T10:00:00Z","endsAt":"2099-01-01T11:00:00Z"}
      """
    Então a resposta tem o estado 422
    E o campo "rule" da resposta é "BR1"
