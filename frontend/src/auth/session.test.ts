import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearSession, hasStoredSession, readSession, saveSession } from "./session";

const valid = { token: "t", guestId: "g", name: "Marta", expiresAt: "2999-01-01T00:00:00Z" };

beforeEach(() => sessionStorage.clear());
afterEach(() => vi.restoreAllMocks());

describe("session", () => {
  it("guarda e lê a sessão", () => {
    saveSession(valid);
    expect(readSession()).toEqual(valid);
    expect(hasStoredSession()).toBe(true);
  });

  it("devolve null sem sessão", () => {
    expect(readSession()).toBeNull();
    expect(hasStoredSession()).toBe(false);
  });

  it("ignora uma sessão expirada", () => {
    saveSession({ ...valid, expiresAt: "2000-01-01T00:00:00Z" });
    expect(readSession()).toBeNull();
    expect(hasStoredSession()).toBe(true);
  });

  it("ignora JSON inválido e sessões sem token", () => {
    sessionStorage.setItem("marazul.session", "{estragado");
    expect(readSession()).toBeNull();
    sessionStorage.setItem("marazul.session", JSON.stringify({ expiresAt: valid.expiresAt }));
    expect(readSession()).toBeNull();
  });

  it("clearSession remove a sessão", () => {
    saveSession(valid);
    clearSession();
    expect(readSession()).toBeNull();
  });

  it("não lança quando o sessionStorage falha", () => {
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("bloqueado");
    });
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("bloqueado");
    });
    vi.spyOn(Storage.prototype, "removeItem").mockImplementation(() => {
      throw new Error("bloqueado");
    });
    expect(() => saveSession(valid)).not.toThrow();
    expect(() => clearSession()).not.toThrow();
    expect(readSession()).toBeNull();
    expect(hasStoredSession()).toBe(false);
  });
});
