import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../App";
import { readSession, saveSession } from "../auth/session";

const SESSION = { token: "t1", guestId: "g1", name: "Marta", expiresAt: "2999-01-01T00:00:00Z" };

function renderAt(path: string) {
  render(
    <MemoryRouter initialEntries={[path]}>
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

async function submitCode(code: string) {
  await userEvent.type(screen.getByLabelText("Código pessoal"), code);
  await userEvent.click(screen.getByRole("button", { name: "Entrar" }));
}

beforeEach(() => sessionStorage.clear());
afterEach(() => vi.unstubAllGlobals());

describe("LoginPage", () => {
  it("guarda a sessão, vai para a página inicial e mostra o nome na barra", async () => {
    const fetchMock = vi.fn().mockImplementation(async () => Response.json(SESSION));
    vi.stubGlobal("fetch", fetchMock);
    renderAt("/login");

    await submitCode("MARTA-2026");

    expect(await screen.findByText("Olá, Marta")).toBeInTheDocument();
    expect(screen.queryByLabelText("Código pessoal")).not.toBeInTheDocument();
    expect(readSession()).toEqual(SESSION);
    const [path, init] = fetchMock.mock.calls.find(([url]) => url !== "/api/auth/csrf") as [
      string,
      RequestInit,
    ];
    expect(path).toBe("/api/auth/guest");
    expect(JSON.parse(init.body as string)).toEqual({ code: "MARTA-2026" });
  });

  it("desativa o botão durante o pedido", async () => {
    vi.stubGlobal("fetch", vi.fn().mockReturnValue(new Promise(() => undefined)));
    renderAt("/login");

    await submitCode("X");

    expect(await screen.findByRole("button", { name: "A entrar…" })).toBeDisabled();
  });

  it.each([
    [401, {}, "Código incorreto. Confirme o código que recebeu no check-in."],
    [
      422,
      { rule: "BR13" },
      "Este código não é válido neste momento. Vale do check-in até 2 horas depois do check-out.",
    ],
    [429, {}, "Demasiadas tentativas. Aguarde um minuto e tente novamente."],
    [400, {}, "Escreva o seu código pessoal."],
    [500, {}, "Não foi possível entrar. Tente novamente dentro de momentos."],
  ])("mostra a mensagem do estado %i", async (status, body, message) => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(async () => problem(status, body)),
    );
    renderAt("/login");

    await submitCode("ERRADO");

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(message);
    const input = screen.getByLabelText("Código pessoal");
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(input).toHaveAttribute("aria-describedby", alert.id);
  });

  it("uma falha de rede mostra a mensagem genérica", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("rede")));
    renderAt("/login");

    await submitCode("X");

    expect(await screen.findByRole("alert")).toHaveTextContent("Não foi possível entrar.");
  });

  it("um 429 não apaga a sessão já guardada", async () => {
    saveSession(SESSION);
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(async () => problem(429, {})),
    );
    renderAt("/login");

    await submitCode("X");

    expect(await screen.findByRole("alert")).toHaveTextContent("Demasiadas tentativas.");
    expect(readSession()).toEqual(SESSION);
  });
});

describe("barra de topo e sessão", () => {
  it("sem sessão mostra a ligação Entrar", () => {
    renderAt("/");

    expect(screen.getByRole("link", { name: "Entrar" })).toHaveAttribute("href", "/login");
  });

  it("Sair limpa a sessão", async () => {
    saveSession(SESSION);
    renderAt("/");

    await userEvent.click(screen.getByRole("button", { name: "Sair" }));

    expect(readSession()).toBeNull();
    expect(screen.getByRole("link", { name: "Entrar" })).toBeInTheDocument();
  });

  it("uma sessão expirada leva a /login com a mensagem de sessão terminada", async () => {
    saveSession({ ...SESSION, expiresAt: "2000-01-01T00:00:00Z" });
    renderAt("/");

    expect(await screen.findByRole("status")).toHaveTextContent("A sua sessão terminou. Entre novamente.");
    expect(screen.getByLabelText("Código pessoal")).toBeInTheDocument();
    expect(sessionStorage.length).toBe(0);
  });
});
