import { type FormEvent, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { loginGuest } from "../api/auth";
import { ApiError } from "../api/client";
import { saveSession } from "../auth/session";
import "./LoginPage.css";

type LoginState = { kind: "idle" } | { kind: "loading" } | { kind: "failed"; message: string };

export function loginErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) {
      return "Código incorreto. Confirme o código que recebeu no check-in.";
    }
    if (error.status === 422 && error.rule === "BR13") {
      return "Este código não é válido neste momento. Vale do check-in até 2 horas depois do check-out.";
    }
    if (error.status === 429) {
      return "Demasiadas tentativas. Aguarde um minuto e tente novamente.";
    }
    if (error.status === 400) {
      return "Escreva o seu código pessoal.";
    }
  }
  return "Não foi possível entrar. Tente novamente dentro de momentos.";
}

export function LoginPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [code, setCode] = useState("");
  const [state, setState] = useState<LoginState>({ kind: "idle" });

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setState({ kind: "loading" });
    try {
      saveSession(await loginGuest(code));
      void navigate("/");
    } catch (error) {
      setState({ kind: "failed", message: loginErrorMessage(error) });
    }
  }

  return (
    <>
      <h1>Entrar</h1>
      {params.get("sessao") === "terminou" && <p role="status">A sua sessão terminou. Entre novamente.</p>}
      <form className="card login-form" onSubmit={(event) => void handleSubmit(event)}>
        <label htmlFor="guest-code">Código pessoal</label>
        <input
          id="guest-code"
          name="code"
          autoComplete="off"
          autoCapitalize="characters"
          aria-invalid={state.kind === "failed" ? "true" : undefined}
          aria-describedby={state.kind === "failed" ? "login-error" : undefined}
          value={code}
          onChange={(event) => setCode(event.target.value)}
        />
        <button type="submit" disabled={state.kind === "loading"}>
          {state.kind === "loading" ? "A entrar…" : "Entrar"}
        </button>
        {state.kind === "failed" && (
          <p id="login-error" role="alert" className="login-error">
            {state.message}
          </p>
        )}
      </form>
    </>
  );
}
