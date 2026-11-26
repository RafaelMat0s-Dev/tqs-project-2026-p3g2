import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, it, vi } from "vitest";
import { App } from "./App";

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  );
}

function stubHealth(response: Promise<Response>) {
  vi.stubGlobal("fetch", vi.fn().mockReturnValue(response));
}

describe("App", () => {
  it("mostra a página inicial e a API disponível", async () => {
    stubHealth(Promise.resolve(Response.json({ status: "UP" })));

    renderAt("/");

    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("chegam ao seu destino");
    expect(await screen.findByText("API disponível")).toBeInTheDocument();
  });

  it("mostra a API indisponível quando o estado não é UP", async () => {
    stubHealth(Promise.resolve(Response.json({ status: "DOWN" })));

    renderAt("/");

    expect(await screen.findByText("API indisponível")).toBeInTheDocument();
  });

  it("mostra a API indisponível quando o pedido falha", async () => {
    stubHealth(Promise.reject(new Error("rede em baixo")));

    renderAt("/");

    expect(await screen.findByText("API indisponível")).toBeInTheDocument();
  });

  it("mostra a página não encontrada para rotas desconhecidas", () => {
    stubHealth(new Promise(() => {}));

    renderAt("/nao-existe");

    expect(screen.getByRole("heading", { name: "Página não encontrada" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Voltar ao início" })).toHaveAttribute("href", "/");
  });
});
