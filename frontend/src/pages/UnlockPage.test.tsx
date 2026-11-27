import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "../App";

const RESERVATION = "11111111-1111-1111-1111-111111111111";

function renderUnlock() {
  render(
    <MemoryRouter initialEntries={[`/reservas/${RESERVATION}/desbloquear`]}>
      <App />
    </MemoryRouter>,
  );
}

function problem(status: number, body: object) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/problem+json" },
  });
}

async function unlockAs(guest: string) {
  await userEvent.type(screen.getByLabelText("Identificação do hóspede (temporário)"), guest);
  await userEvent.click(screen.getByRole("button", { name: "Desbloquear bicicleta" }));
}

afterEach(() => vi.unstubAllGlobals());

describe("UnlockPage", () => {
  it("envia a reserva e o hóspede e mostra a hora de início da viagem", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { id: "t1", reservationId: RESERVATION, vehicleId: "v1", startedAt: "2026-12-01T15:17:00Z" },
          { status: 201 },
        ),
      );
    vi.stubGlobal("fetch", fetchMock);
    renderUnlock();

    await unlockAs("marta");

    expect(await screen.findByRole("status")).toHaveTextContent("A sua viagem começou às 15:17.");
    const [path, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(path).toBe("/api/trips");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string)).toEqual({ reservationId: RESERVATION, guestId: "marta" });
  });

  it.each([
    [
      422,
      { rule: "BR6" },
      "A sua reserva expirou porque passaram mais de 15 minutos. Faça uma nova reserva.",
    ],
    [403, {}, "Esta reserva pertence a outro hóspede."],
    [404, {}, "Não encontrámos esta reserva, ou ela já foi desbloqueada."],
    [400, {}, "O pedido não é válido. Confirme a identificação do hóspede."],
    [500, {}, "Não foi possível desbloquear a bicicleta. Tente novamente dentro de momentos."],
  ])("mostra a mensagem certa para a resposta %i", async (status, body, message) => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(problem(status, body)));
    renderUnlock();

    await unlockAs("marta");

    expect(await screen.findByRole("alert")).toHaveTextContent(message);
  });

  it("mostra uma mensagem genérica quando não há ligação", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));
    renderUnlock();

    await unlockAs("marta");

    expect(await screen.findByRole("alert")).toHaveTextContent("Não foi possível desbloquear a bicicleta.");
  });

  it("desativa o botão enquanto o pedido está a decorrer", async () => {
    vi.stubGlobal("fetch", vi.fn().mockReturnValue(new Promise(() => {})));
    renderUnlock();

    await unlockAs("marta");

    expect(screen.getByRole("button", { name: "A desbloquear…" })).toBeDisabled();
  });
});
