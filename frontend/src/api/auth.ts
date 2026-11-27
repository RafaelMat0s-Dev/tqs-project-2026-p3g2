import type { GuestSession } from "../auth/session";
import { apiFetch } from "./client";

export type { GuestSession };

export function loginGuest(code: string): Promise<GuestSession> {
  return apiFetch<GuestSession>("/api/auth/guest", {
    method: "POST",
    body: JSON.stringify({ code }),
  });
}

export function fetchSession(): Promise<Omit<GuestSession, "token">> {
  return apiFetch<Omit<GuestSession, "token">>("/api/auth/guest/session");
}
