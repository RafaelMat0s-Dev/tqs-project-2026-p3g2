# Mar Azul

O Mar Azul é um serviço de gestão de uma frota de mobilidade suave para o resort fictício Alvina Mar. Este repositório contém a API Spring Boot, o cliente web React, o simulador de telemetria, a documentação de qualidade e a estrutura de implementação do projeto TQS.

## Executar localmente

```bash
docker compose up --build
```

- Interface: http://localhost:5173
- Estado da API: http://localhost:8080/actuator/health
- Simulador de telemetria: http://localhost:8090

Para criar o primeiro administrador localmente, forneça as credenciais através de variáveis de ambiente ao iniciar a aplicação; estas não são armazenadas no repositório:

```bash
MARAZUL_SECURITY_BOOTSTRAP_ADMIN_EMAIL=sofia@marazul.test \
MARAZUL_SECURITY_BOOTSTRAP_ADMIN_PASSWORD='12-char-password' \
docker compose up --build
```

As contas dos funcionários utilizam palavras-passe encriptadas com Argon2id e sessões JWT num cookie HttpOnly, Secure e SameSite=Strict. O repositório temporário das contas é mantido em memória até ser introduzido o adaptador de persistência de identidades.

Consulte [docs/architecture.md](docs/architecture.md) para conhecer a arquitetura atual, os contratos implementados e os próximos limites do sistema.
