# language: pt
@REQ_SCRUM-27 @US-E1-03 @E1
Funcionalidade: Login do hóspede com o código pessoal
  Como hóspede
  Quero entrar com o código pessoal que recebi no check-in
  Para usar a aplicação durante a estadia

  @BR13
  Cenário: Entrada às 13h00, duas horas depois do check-out
    Dado uma estadia com check-out às 11h00 de 1 de dezembro e a hóspede Marta
    E o relógio marca 13h00 de 1 de dezembro
    Quando a Marta entra com o seu código
    Então a resposta do hóspede tem o estado 200
    E o campo "name" da resposta do hóspede é "Marta"
    E o campo "expiresAt" da resposta do hóspede é "2026-12-01T13:00:00Z"

  @BR13
  Cenário: Entrada um minuto depois de a janela de acesso fechar
    Dado uma estadia com check-out às 11h00 de 1 de dezembro e a hóspede Marta
    E o relógio marca 13h01 de 1 de dezembro
    Quando a Marta entra com o seu código
    Então a resposta do hóspede tem o estado 422
    E o campo "rule" da resposta do hóspede é "BR13"

  @BR13
  Cenário: Entrada antes de a estadia começar
    Dado uma estadia que começa amanhã e a hóspede Marta
    E o relógio marca 12h50 de 1 de dezembro
    Quando a Marta entra com o seu código
    Então a resposta do hóspede tem o estado 422
    E o campo "rule" da resposta do hóspede é "BR13"

  @BR13
  Cenário: A sessão termina quando a janela de acesso fecha
    Dado uma estadia com check-out às 11h00 de 1 de dezembro e a hóspede Marta
    E o relógio marca 12h50 de 1 de dezembro
    E a Marta entra com o seu código
    Quando o relógio marca 13h00 de 1 de dezembro e 1 s
    E a Marta consulta a sua sessão
    Então a resposta do hóspede tem o estado 401

  Cenário: Cinco códigos errados num minuto bloqueiam a origem
    Dado uma estadia com check-out às 11h00 de 1 de dezembro e a hóspede Marta
    E o relógio marca 12h50 de 1 de dezembro
    Quando alguém tenta 5 códigos errados
    E a Marta entra com o seu código
    Então a resposta do hóspede tem o estado 429
    E o campo "title" da resposta do hóspede é "Demasiadas tentativas"
