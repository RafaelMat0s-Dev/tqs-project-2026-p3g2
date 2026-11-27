import http from "k6/http";
import { check } from "k6";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.4.0/index.js";

const BASE_URL = __ENV.BASE_URL || "http://localhost:5173";
const VEHICLE_IDS = (__ENV.VEHICLE_IDS || "").split(",").filter(Boolean);

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 499 }));

export const options = {
  scenarios: {
    bookings: { executor: "constant-vus", vus: 50, duration: __ENV.DURATION || "3m" },
  },
  thresholds: {
    "http_req_duration{name:POST /api/bookings}": ["p(95)<500"],
    http_req_failed: ["rate<0.01"],
  },
};

let csrf;

function csrfHeaders() {
  if (csrf === undefined) {
    const response = http.get(`${BASE_URL}/api/auth/csrf`, { tags: { name: "GET /api/auth/csrf" } });
    csrf = { [response.json("headerName")]: response.json("token") };
  }
  return csrf;
}

export default function () {
  const vehicleId = VEHICLE_IDS.length > 0 ? VEHICLE_IDS[Math.floor(Math.random() * VEHICLE_IDS.length)] : uuidv4();
  const start = new Date(Date.now() + 24 * 3600 * 1000);
  const body = JSON.stringify({
    vehicleId,
    guestId: `k6-${__VU}-${__ITER}`,
    startsAt: start.toISOString(),
    endsAt: new Date(start.getTime() + 3600 * 1000).toISOString(),
  });

  const response = http.post(`${BASE_URL}/api/bookings`, body, {
    headers: { "Content-Type": "application/json", ...csrfHeaders() },
    tags: { name: "POST /api/bookings" },
  });
  check(response, { "answer is 201, 409 or 422": (r) => [201, 409, 422].includes(r.status) });
}
