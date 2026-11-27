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

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json, application/problem+json");
  if (init.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, { credentials: "same-origin", ...init, headers });
  const body: unknown = await readBody(response);

  if (!response.ok) {
    throw new ApiError(response.status, isProblem(body) ? body : {});
  }
  return body as T;
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
