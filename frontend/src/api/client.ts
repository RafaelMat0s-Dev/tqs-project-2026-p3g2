import { readSession } from "../auth/session";

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  rule?: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly rule?: string;
  readonly problem: ProblemDetail;

  constructor(status: number, problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Pedido falhou com o estado ${status}`);
    this.name = "ApiError";
    this.status = status;
    this.rule = problem.rule;
    this.problem = problem;
  }
}

const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

interface CsrfToken {
  headerName: string;
  token: string;
}

let csrfToken: CsrfToken | undefined;

export function forgetCsrfToken(): void {
  csrfToken = undefined;
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json, application/problem+json");
  if (init.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const changesData = !SAFE_METHODS.has((init.method ?? "GET").toUpperCase());
  if (changesData) {
    const csrf = await loadCsrfToken();
    if (csrf !== undefined) {
      headers.set(csrf.headerName, csrf.token);
    }
  }

  const session = readSession();
  if (session !== null && isSameOriginPath(path) && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${session.token}`);
  }

  const response = await fetch(path, { credentials: "same-origin", ...init, headers });
  const body: unknown = await readBody(response);

  if (!response.ok) {
    if (changesData && response.status === 403) {
      forgetCsrfToken();
    }
    throw new ApiError(response.status, isProblem(body) ? body : {});
  }
  return body as T;
}

async function loadCsrfToken(): Promise<CsrfToken | undefined> {
  if (csrfToken === undefined) {
    try {
      const response = await fetch("/api/auth/csrf", {
        credentials: "same-origin",
        headers: { Accept: "application/json" },
      });
      const body = await readBody(response);
      if (response.ok && isCsrfToken(body)) {
        csrfToken = body;
      }
    } catch {
      return undefined;
    }
  }
  return csrfToken;
}

function isCsrfToken(value: unknown): value is CsrfToken {
  return (
    typeof value === "object" &&
    value !== null &&
    typeof (value as CsrfToken).headerName === "string" &&
    typeof (value as CsrfToken).token === "string"
  );
}

async function readBody(response: Response): Promise<unknown> {
  const text = await response.text();
  if (text === "") {
    return undefined;
  }
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function isProblem(value: unknown): value is ProblemDetail {
  return typeof value === "object" && value !== null;
}

function isSameOriginPath(path: string): boolean {
  return path.startsWith("/") && !path.startsWith("//");
}
