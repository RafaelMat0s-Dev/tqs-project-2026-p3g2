import http from "k6/http";
import { check } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:5173";
const HUB_ID = __ENV.HUB_ID || "hotel-alvina-mar";
const DESTINATION_ID = __ENV.DESTINATION_ID || "praia-da-ribeira";

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 499 }));

export const options = {
  scenarios: {
    search: { executor: "constant-vus", vus: 50, duration: __ENV.DURATION || "3m" },
  },
  thresholds: {
    "http_req_duration{name:GET /api/hubs/{id}/vehicles}": ["p(95)<800"],
    http_req_failed: ["rate<0.01"],
  },
};

export default function () {
  const response = http.get(`${BASE_URL}/api/hubs/${HUB_ID}/vehicles?destino=${DESTINATION_ID}`, {
    tags: { name: "GET /api/hubs/{id}/vehicles" },
  });
  check(response, { "search answers 200": (r) => r.status === 200 });
}
