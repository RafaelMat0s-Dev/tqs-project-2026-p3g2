# language: pt
@REQ_SCRUM-29 @US-E5-01 @E5 @SLO3
Funcionalidade: Telemetria das bicicletas
  Como coordenador de operações
  Quero que o estado das bicicletas se atualize a partir das leituras de telemetria
  Para que as reservas e o motor de autonomia usem dados recentes

  @TEST_SCRUM-272
  Cenário: A leitura publicada aparece no estado da bicicleta em menos de 5 segundos
    Dado que estão a ser publicadas 10 leituras por segundo de outras bicicletas
    Quando é publicada uma leitura de 47 % para a bicicleta "B-17"
    Então em menos de 5 segundos o estado da bicicleta "B-17" mostra 47 %

  @TEST_SCRUM-273
  Cenário: Uma leitura mais antiga é descartada e o estado não muda
    Dado que a bicicleta "B-17" tem uma leitura de 47 % das "2026-11-20T14:31:07Z"
    Quando chega à bicicleta "B-17" uma leitura de 30 % das "2026-11-20T14:31:05Z"
    Então a leitura é descartada
    Quando consulto "/api/telemetry/vehicles/B-17"
    Então a resposta tem o estado 200
    E o campo "batteryPercentage" da resposta é "47"
    E o campo "lastReadingAt" da resposta é "2026-11-20T14:31:07Z"

  @TEST_SCRUM-274
  Cenário: Uma mensagem com JSON inválido é rejeitada e as leituras seguintes continuam a ser processadas
    Dado que o valor atual da métrica "marazul_telemetry_rejected_total" fica registado
    E uma mensagem com JSON inválido
    Quando chega ao adaptador de telemetria
    Então a mensagem é rejeitada
    E a métrica "marazul_telemetry_rejected_total" aumenta
    Quando é publicada uma leitura de 47 % para a bicicleta "B-17"
    Então em menos de 5 segundos o estado da bicicleta "B-17" mostra 47 %

  @TEST_SCRUM-275
  Cenário: O atraso entre a leitura e o estado atualizado está exposto nas métricas
    Dado que há leituras a ser processadas
    E o valor atual da métrica "marazul_telemetry_delay_seconds_count" fica registado
    Quando é publicada uma leitura de 47 % para a bicicleta "B-17"
    Então em menos de 5 segundos a métrica "marazul_telemetry_delay_seconds_count" aumenta
    Quando consulto "/actuator/prometheus"
    Então a resposta tem o estado 200
    E a métrica "marazul_telemetry_delay_seconds" está exposta
