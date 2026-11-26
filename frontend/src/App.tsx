import { Route, Routes } from "react-router";
import { HomePage } from "./pages/HomePage";
import { NotFoundPage } from "./pages/NotFoundPage";

export function App() {
  return (
    <>
      <a className="skip-link" href="#conteudo">
        Saltar para o conteúdo
      </a>
      <header className="topbar">
        <span className="brand">MarAzul</span>
      </header>
      <main id="conteudo" className="shell">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
    </>
  );
}
