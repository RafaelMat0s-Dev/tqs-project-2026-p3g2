import { apiFetch } from "./client";

export interface Trip {
  id: string;
  reservationId: string;
  vehicleId: string;
  startedAt: string;
}

export function unlockReservation(reservationId: string, guestId: string): Promise<Trip> {
  return apiFetch<Trip>("/api/trips", {
    method: "POST",
    body: JSON.stringify({ reservationId, guestId }),
  });
}
