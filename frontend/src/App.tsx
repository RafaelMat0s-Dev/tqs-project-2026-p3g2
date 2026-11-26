import { useEffect, useState } from "react";
import { Link, Route, Routes, useLocation, useNavigate } from "react-router";
import { clearSession, hasStoredSession, readSession } from "./auth/session";
import "./App.css";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { UnlockPage } from "./pages/UnlockPage";

export function App() {
  const location = useLocation();
  const navigate = useNavigate();
  const [, setVersion] = useState(0);
  const session = readSession();

  useEffect(() => {
    if (readSession() === null && hasStoredSession()) {
      clearSession();
      void navigate("/entrar?sessao=terminou", { replace: true });
    }
  }, [location.pathname, navigate]);

  function handleLogout() {
    clearSession();
    setVersion((version) => version + 1);
    void navigate("/");
  }

  return (
    <>
      <a className="skip-link" href="#conteudo">
        Saltar para o conteúdo
      </a>
      <header className="topbar">
        <span className="brand">MarAzul</span>
        <div className="topbar-session">
          {session === null ? (
            <Link to="/entrar">Entrar</Link>
          ) : (
            <>
              <span>Olá, {session.name}</span>
              <button type="button" onClick={handleLogout}>
                Sair
              </button>
            </>
          )}
        </div>
      </header>
      <main id="conteudo" className="shell">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/entrar" element={<LoginPage />} />
          <Route path="/reservas/:reservationId/desbloquear" element={<UnlockPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
    </>
  );
}
