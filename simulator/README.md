# Simulador de telemetria da frota

Serviço Node.js autónomo que simula a frota de bicicletas do MarAzul. Publica leituras de telemetria no Mosquitto por MQTT e expõe uma pequena API HTTP para injetar cenários de teste.

## Configuração

| Variável      | Por omissão             | Descrição                                      |
|---------------|-------------------------|------------------------------------------------|
| `MQTT_URL`    | `mqtt://localhost:1883` | URL do broker MQTT                             |
| `PUBLISH_RATE`| `10`                    | Leituras publicadas por segundo (número > 0)   |
| `FLEET_SIZE`  | `60`                    | Número de bicicletas (`B-01`, `B-02`, ...)     |
| `PORT`        | `8090`                  | Porta HTTP                                     |

No `docker-compose.yml`, `PUBLISH_RATE` e `FLEET_SIZE` podem ser definidos no ambiente do anfitrião. Um valor inválido termina o processo com mensagem de erro.

## Mensagem publicada

- Tópico: `marazul/vehicles/{code}/telemetry`, QoS 1.
- Corpo JSON:

```json
{
  "vehicleId": "B-17",
  "originAt": "2026-11-26T10:15:30.123Z",
  "batteryPercentage": 78,
  "locked": true,
  "hubId": "sao-bras",
  "shock": false
}
```

- `vehicleId` é igual ao `{code}` do tópico.
- `originAt` é um instante ISO em UTC.
- `batteryPercentage` é um inteiro entre 0 e 100.
- `hubId` pode ser `null` (bicicleta fora de um hub).
- `shock` indica um choque detetado.

## Hubs

A frota distribui-se por seis hubs, por rotação determinística, com 10 bicicletas por hub (60 bicicletas): `hotel-alvina-mar`, `praia-da-ribeira`, `sao-bras`, `hub-4`, `hub-5`, `hub-6`. A `B-17` fica sempre em `sao-bras`.

## API HTTP

### `GET /health`

Devolve 200 com `service`, `status`, `brokerUrl` e `mqttConnected`.

### `POST /scenarios`

Publica uma leitura à medida. `vehicleId` é obrigatório e tem de existir na frota; os restantes campos (`originAt`, `batteryPercentage`, `locked`, `hubId`, `shock`) são opcionais e sobrepõem-se ao estado atual da bicicleta.

```bash
curl -X POST http://localhost:8090/scenarios \
  -H 'content-type: application/json' \
  -d '{"vehicleId":"B-17","batteryPercentage":15,"locked":false,"hubId":null}'
```

Com o campo `raw`, o texto é publicado tal como está, por exemplo para enviar JSON inválido:

```bash
curl -X POST http://localhost:8090/scenarios \
  -H 'content-type: application/json' \
  -d '{"vehicleId":"B-17","raw":"{isto não é json"}'
```

Respostas: `202` leitura publicada; `400` corpo mal formado, `vehicleId` em falta ou desconhecido, ou `batteryPercentage` fora de 0 a 100; `502` falha ao publicar no broker.

## Trocar de simulador

O backend só conhece o tópico e o formato acima. Para usar outro simulador, publique no mesmo tópico e com o mesmo formato. Para trocar a origem dos dados (por exemplo, para outro broker ou protocolo), substitua o adaptador que implementa `TelemetrySource` (hoje `MqttTelemetryAdapter`), sem mudar a lógica de negócio.

## Limitações conhecidas

- O backend liga-se com um client-id aleatório e `cleanSession`, por isso as leituras publicadas enquanto a API está em baixo perdem-se. É aceitável para telemetria, porque chega sempre uma leitura mais recente.
- O Mosquitto continua com `allow_anonymous true` até serem configuradas as ACL (30 de novembro).

## Desenvolvimento

```bash
npm ci
npm test
npm run test:coverage
```
