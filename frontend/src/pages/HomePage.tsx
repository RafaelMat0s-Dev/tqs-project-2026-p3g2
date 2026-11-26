import { useEffect, useState } from "react";
import { apiFetch } from "../api/client";

type ApiState = "a verificar" | "disponível" | "indisponível";

export function HomePage() {
  const [apiState, setApiState] = useState<ApiState>("a verificar");

  useEffect(() => {
    let active = true;
    apiFetch<{ status: string }>("/actuator/health")
      .then((health) => active && setApiState(health.status === "UP" ? "disponível" : "indisponível"))
      .catch(() => active && setApiState("indisponível"));
    return () => {
      active = false;
    };
  }, []);

  return (
    <>
      <p className="eyebrow">MarAzul Mobility</p>
      <h1>Bicicletas elétricas que chegam ao seu destino.</h1>
      <p>
        O MarAzul só propõe bicicletas com carga para chegar ao destino e, se o destino não tiver hub, também
        para voltar.
      </p>
      <section className="card" aria-labelledby="estado-titulo">
        <h2 id="estado-titulo">Estado do serviço</h2>
        <p role="status">API {apiState}</p>
      </section>
    </>
  );
}
