export interface GuestSession {
  token: string;
  guestId: string;
  name: string;
  expiresAt: string;
}

const KEY = "marazul.session";

function readRaw(): string | null {
  try {
    return sessionStorage.getItem(KEY);
  } catch {
    return null;
  }
}

export function hasStoredSession(): boolean {
  return readRaw() !== null;
}

export function readSession(): GuestSession | null {
  const raw = readRaw();
  if (raw === null) {
    return null;
  }
  try {
    const session = JSON.parse(raw) as GuestSession;
    if (typeof session.token !== "string" || !(Date.parse(session.expiresAt) > Date.now())) {
      return null;
    }
    return session;
  } catch {
    return null;
  }
}

export function saveSession(session: GuestSession): void {
  try {
    sessionStorage.setItem(KEY, JSON.stringify(session));
  } catch {
    /* sem armazenamento disponível: a sessão só vale até ao fim da página */
  }
}

export function clearSession(): void {
  try {
    sessionStorage.removeItem(KEY);
  } catch {
    /* sem armazenamento disponível */
  }
}
