# language: pt
@REQ_SCRUM-24 @US-E1-02 @E1 @BR14 @wip
Funcionalidade: Importação de estadias e ocupantes
  Como administradora
  Quero importar as estadias e os ocupantes em JSON
  Para que cada hóspede receba o seu código pessoal sem criar conta

  Contexto:
    Dado que a Sofia está autenticada como administradora

  Cenário: Um ficheiro válido com 37 estadias dá um código pessoal único a cada ocupante
    Quando a Sofia importa um ficheiro válido com 37 estadias
    Então a resposta tem o estado 200
    E o campo "created" da resposta é "37"
    E o campo "updated" da resposta é "0"
    E cada ocupante tem um código pessoal único

  Cenário: Os códigos pessoais ficam guardados só como hash
    Quando a Sofia importa um ficheiro válido com 37 estadias
    Então a resposta tem o estado 200
    E nenhum código pessoal está guardado em claro

  Cenário: Importar de novo uma estadia com a mesma referência externa atualiza-a
    Dado uma estadia da Marta já importada com a referência externa "RES-2026-0142"
    Quando a Sofia importa a estadia "RES-2026-0142" com outro check-out
    Então a resposta tem o estado 200
    E o campo "created" da resposta é "0"
    E o campo "updated" da resposta é "1"
    E existe uma só estadia com a referência externa "RES-2026-0142"
    E essa estadia tem o novo check-out

  Cenário: Um ficheiro com o check-out antes do check-in não importa nada
    Quando a Sofia importa um ficheiro em que a estadia "RES-2026-0150" tem o check-out antes do check-in
    Então a resposta tem o estado 422
    E o erro indica a estadia "RES-2026-0150"
    E não fica nenhuma estadia importada

  Cenário: Uma estadia inválida no fim do ficheiro cancela a importação inteira
    Quando a Sofia importa um ficheiro com 36 estadias válidas e 1 inválida
    Então a resposta tem o estado 422
    E não fica nenhuma estadia importada

  Cenário: A autorização do Duarte, de 16 anos, registada pela Marta fica válida
    Dado o Duarte, de 16 anos, na estadia da Marta
    Quando a Marta regista a autorização para o Duarte reservar
    Então a resposta tem o estado 201
    E a autorização do Duarte fica válida

  Cenário: Um adulto de outra estadia não autoriza o Duarte
    Dado o Duarte, de 16 anos, na estadia da Marta
    E um adulto de outra estadia
    Quando esse adulto regista a autorização para o Duarte reservar
    Então a resposta tem o estado 422
    E o campo "rule" da resposta é "BR14"
    E a autorização do Duarte não fica registada
