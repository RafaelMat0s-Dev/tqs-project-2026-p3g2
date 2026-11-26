import { describe, expect, it, vi } from "vitest";
import { ApiError, apiFetch } from "./client";

function respond(status: number, body: string | null, contentType = "application/json") {
  const fetchMock = vi
    .fn()
    .mockResolvedValue(new Response(body, { status, headers: { "Content-Type": contentType } }));
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("apiFetch", () => {
  it("devolve o corpo JSON de uma resposta com sucesso", async () => {
    respond(200, '{"status":"UP"}');

    await expect(apiFetch<{ status: string }>("/actuator/health")).resolves.toEqual({ status: "UP" });
  });

  it("envia JSON quando há corpo", async () => {
    const fetchMock = respond(201, '{"id":"1"}');

    await apiFetch("/api/bookings", { method: "POST", body: JSON.stringify({ guestId: "marta" }) });

    const headers = fetchMock.mock.calls[0][1].headers as Headers;
    expect(headers.get("Content-Type")).toBe("application/json");
  });

  it("devolve undefined quando a resposta não tem corpo", async () => {
    respond(204, null);

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
