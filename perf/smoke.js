import http from "k6/http";
import { check } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:5173";

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 499 }));

export const options = {
  vus: 10,
  duration: "30s",
  thresholds: {
    http_req_failed: ["rate<0.01"],
    "http_req_duration{name:readiness}": ["p(95)<300"],
    checks: ["rate>0.99"],
  },
};

export default function () {
  const response = http.get(`${BASE_URL}/actuator/health/readiness`, { tags: { name: "readiness" } });
  check(response, { "readiness is UP": (r) => r.status === 200 && r.json("status") === "UP" });
}
