import { type FormEvent, useState } from "react";
import { useParams } from "react-router";
import { ApiError } from "../api/client";
import { type Trip, unlockReservation } from "../api/trips";
import "./UnlockPage.css";

type UnlockState =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "unlocked"; trip: Trip }
  | { kind: "failed"; message: string };

const timeFormat = new Intl.DateTimeFormat("pt-PT", {
  hour: "2-digit",
  minute: "2-digit",
  timeZone: "Europe/Lisbon",
});

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 422 && error.rule === "BR6") {
      return "A sua reserva expirou porque passaram mais de 15 minutos. Faça uma nova reserva.";
    }
    if (error.status === 403) {
      return "Esta reserva pertence a outro hóspede.";
    }
    if (error.status === 404) {
      return "Não encontrámos esta reserva, ou ela já foi desbloqueada.";
    }
    if (error.status === 400) {
      return "O pedido não é válido. Confirme a identificação do hóspede.";
    }
  }
  return "Não foi possível desbloquear a bicicleta. Tente novamente dentro de momentos.";
}

export function UnlockPage() {
  const { reservationId = "" } = useParams();
  const [guestId, setGuestId] = useState("");
  const [state, setState] = useState<UnlockState>({ kind: "idle" });

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setState({ kind: "loading" });
    try {
      setState({ kind: "unlocked", trip: await unlockReservation(reservationId, guestId) });
    } catch (error) {
      setState({ kind: "failed", message: errorMessage(error) });
    }
  }

  if (state.kind === "unlocked") {
    return (
      <>
        <h1>Boa viagem!</h1>
        <p role="status">A sua viagem começou às {timeFormat.format(new Date(state.trip.startedAt))}.</p>
      </>
    );
  }

  return (
    <>
      <h1>Desbloquear bicicleta</h1>
      <p>Tem 15 minutos a partir do início da reserva para desbloquear a bicicleta.</p>
      <form className="card unlock-form" onSubmit={(event) => void handleSubmit(event)}>
        <label htmlFor="guest-id">Identificação do hóspede (temporário)</label>
        <input
          id="guest-id"
          name="guestId"
          required
          autoComplete="off"
          value={guestId}
          onChange={(event) => setGuestId(event.target.value)}
        />
        <button type="submit" disabled={state.kind === "loading"}>
          {state.kind === "loading" ? "A desbloquear…" : "Desbloquear bicicleta"}
        </button>
        {state.kind === "failed" && (
          <p role="alert" className="unlock-error">
            {state.message}
          </p>
        )}
      </form>
    </>
  );
}
