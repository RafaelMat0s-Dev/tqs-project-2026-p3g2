import { beforeEach, describe, expect, it, vi } from "vitest";
import { saveSession } from "../auth/session";
import { ApiError, apiFetch } from "./client";

function respond(status: number, body: string | null, contentType = "application/json") {
  const fetchMock = vi
    .fn()
    .mockImplementation(async () => new Response(body, { status, headers: { "Content-Type": contentType } }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function csrfResponse() {
  return Response.json({ headerName: "X-XSRF-TOKEN", token: "abc123" });
}

function respondAfterCsrf(...responses: Response[]) {
  const fetchMock = vi.fn().mockResolvedValueOnce(csrfResponse());
  for (const response of responses) {
    fetchMock.mockResolvedValueOnce(response);
  }
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function headersOf(fetchMock: ReturnType<typeof vi.fn>, call: number) {
  return (fetchMock.mock.calls[call][1] as RequestInit).headers as Headers;
}

beforeEach(() => sessionStorage.clear());

describe("apiFetch", () => {
  it("acrescenta o cabeçalho Authorization quando há sessão", async () => {
    saveSession({ token: "abc", guestId: "g", name: "Marta", expiresAt: "2999-01-01T00:00:00Z" });
    const fetchMock = respond(200, "{}");

    await apiFetch("/api/x");

    expect((fetchMock.mock.calls[0][1].headers as Headers).get("Authorization")).toBe("Bearer abc");
  });

  it("não envia o token para um URL absoluto", async () => {
    saveSession({ token: "abc", guestId: "g", name: "Marta", expiresAt: "2999-01-01T00:00:00Z" });
    for (const url of ["https://outro.example/api/x", "//outro.example/api/x"]) {
      const fetchMock = respond(200, "{}");
      await apiFetch(url);
      expect((fetchMock.mock.calls[0][1].headers as Headers).has("Authorization")).toBe(false);
    }
  });

  it("não substitui um Authorization já definido nem o envia sem sessão", async () => {
    saveSession({ token: "abc", guestId: "g", name: "Marta", expiresAt: "2999-01-01T00:00:00Z" });
    const fetchMock = respond(200, "{}");
    await apiFetch("/api/x", { headers: { Authorization: "Bearer outro" } });
    expect((fetchMock.mock.calls[0][1].headers as Headers).get("Authorization")).toBe("Bearer outro");

    sessionStorage.clear();
    const second = respond(200, "{}");
    await apiFetch("/api/x");
    expect((second.mock.calls[0][1].headers as Headers).has("Authorization")).toBe(false);
  });

  it("devolve o corpo JSON de uma resposta com sucesso", async () => {
    respond(200, '{"status":"UP"}');

    await expect(apiFetch<{ status: string }>("/actuator/health")).resolves.toEqual({ status: "UP" });
  });

  it("envia JSON e o token CSRF quando altera dados", async () => {
    const fetchMock = respondAfterCsrf(Response.json({ id: "1" }, { status: 201 }));

    await apiFetch("/api/bookings", { method: "post", body: JSON.stringify({ guestId: "marta" }) });

    expect(fetchMock.mock.calls[0][0]).toBe("/api/auth/csrf");
    expect(fetchMock.mock.calls[1][0]).toBe("/api/bookings");
    expect(headersOf(fetchMock, 1).get("Content-Type")).toBe("application/json");
    expect(headersOf(fetchMock, 1).get("X-XSRF-TOKEN")).toBe("abc123");
  });

  it("pede o token CSRF uma só vez", async () => {
    const fetchMock = respondAfterCsrf(
      Response.json({ id: "1" }, { status: 201 }),
      Response.json({ id: "2" }, { status: 201 }),
    );

    await apiFetch("/api/bookings", { method: "POST", body: "{}" });
    await apiFetch("/api/bookings", { method: "POST", body: "{}" });

    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(headersOf(fetchMock, 2).get("X-XSRF-TOKEN")).toBe("abc123");
  });

  it("não pede o token CSRF nos pedidos de leitura", async () => {
    const fetchMock = respond(200, "[]");

    await apiFetch("/api/vehicles");

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(headersOf(fetchMock, 0).has("X-XSRF-TOKEN")).toBe(false);
  });

  it("volta a pedir o token CSRF depois de um 403", async () => {
    const fetchMock = respondAfterCsrf(
      Response.json({ status: 403 }, { status: 403 }),
      csrfResponse(),
      Response.json({ id: "1" }, { status: 201 }),
    );

    await expect(apiFetch("/api/auth/staff", { method: "POST", body: "{}" })).rejects.toMatchObject({
      status: 403,
    });
    await apiFetch("/api/auth/staff", { method: "POST", body: "{}" });

    expect(fetchMock.mock.calls.map(([path]) => path)).toEqual([
      "/api/auth/csrf",
      "/api/auth/staff",
      "/api/auth/csrf",
      "/api/auth/staff",
    ]);
  });

  it("envia o pedido sem token quando o token CSRF não está disponível", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response("Bad Gateway", { status: 502 }))
      .mockResolvedValueOnce(Response.json({ id: "1" }, { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);

    await apiFetch("/api/bookings", { method: "POST", body: "{}" });

    expect(headersOf(fetchMock, 1).has("X-XSRF-TOKEN")).toBe(false);
  });

  it("envia o pedido sem token quando o pedido do token CSRF falha na rede", async () => {
    const fetchMock = vi
      .fn()
      .mockRejectedValueOnce(new TypeError("Failed to fetch"))
      .mockResolvedValueOnce(Response.json({ id: "1" }, { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);

    await apiFetch("/api/bookings", { method: "POST", body: "{}" });

    expect(headersOf(fetchMock, 1).has("X-XSRF-TOKEN")).toBe(false);
  });

  it("devolve undefined quando a resposta não tem corpo", async () => {
    respondAfterCsrf(new Response(null, { status: 204 }));

    await expect(apiFetch("/api/bookings/1", { method: "DELETE" })).resolves.toBeUndefined();
  });

  it("lança ApiError com o ID da regra violada", async () => {
    respond(
      422,
      '{"status":422,"title":"Regra de negócio violada","detail":"O hóspede já tem uma reserva ativa.","rule":"BR8"}',
      "application/problem+json",
    );

    const error = await apiFetch("/api/bookings").catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 422, rule: "BR8", message: "O hóspede já tem uma reserva ativa." });
  });

  it("lança ApiError com uma mensagem genérica quando o erro não é JSON", async () => {
    respond(502, "Bad Gateway", "text/plain");

    await expect(apiFetch("/api/bookings")).rejects.toMatchObject({
      status: 502,
      rule: undefined,
      message: "Pedido falhou com o estado 502",
    });
  });
});
