import http from "node:http";

const port = Number(process.env.PORT ?? 8090);
const server = http.createServer((_request, response) => {
  response.writeHead(200, { "content-type": "application/json" });
  response.end(JSON.stringify({ service: "marazul-telemetry-simulator", status: "ok", readings: [] }));
});

server.listen(port, () => console.log(`Telemetry simulator listening on ${port}`));
