import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { saveSession } from "../auth/session";
import { fetchSession, loginGuest } from "./auth";

beforeEach(() => sessionStorage.clear());
afterEach(() => vi.unstubAllGlobals());

describe("auth api", () => {
  it("loginGuest envia o código por POST", async () => {
    const session = { token: "t", guestId: "g", name: "Marta", expiresAt: "2999-01-01T00:00:00Z" };
    const fetchMock = vi.fn().mockResolvedValue(Response.json(session));
    vi.stubGlobal("fetch", fetchMock);

    await expect(loginGuest("MARTA-2026")).resolves.toEqual(session);

    const [path, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(path).toBe("/api/auth/guest");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string)).toEqual({ code: "MARTA-2026" });
  });

  it("fetchSession usa o token guardado", async () => {
    saveSession({ token: "abc", guestId: "g", name: "Marta", expiresAt: "2999-01-01T00:00:00Z" });
    const fetchMock = vi
      .fn()
      .mockResolvedValue(Response.json({ guestId: "g", name: "Marta", expiresAt: "x" }));
    vi.stubGlobal("fetch", fetchMock);

    await fetchSession();

    const [path, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(path).toBe("/api/auth/guest/session");
    expect((init.headers as Headers).get("Authorization")).toBe("Bearer abc");
  });
});
