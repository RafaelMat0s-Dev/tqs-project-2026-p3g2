import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

function App() {
  return (
    <main className="shell">
      <p className="eyebrow">Mar Azul Mobility</p>
      <h1>Mobilidade leve, sem complicações.</h1>
      <p>O esqueleto inicial está ativo. A pesquisa e a reserva imediata serão o primeiro fluxo do hóspede.</p>
      <section className="card" aria-labelledby="slice-title">
        <h2 id="slice-title">Primeiro fluxo</h2>
        <ul>
          <li>Pesquisar bicicletas disponíveis por hub</li>
          <li>Validar bateria mínima de 25%</li>
          <li>Registar uma reserva sem sobreposição</li>
        </ul>
      </section>
    </main>
  );
}

createRoot(document.getElementById("root")!).render(
  <StrictMode><App /></StrictMode>,
);
