import { getToken } from "../state/auth";

export type ApiResult<T> = { ok: boolean; message?: string | null; data?: T };

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? (import.meta.env.DEV ? "http://localhost:8080" : "");

async function readError(res: Response): Promise<string> {
  try {
    const body = (await res.json()) as ApiResult<unknown>;
    return body?.message || `HTTP ${res.status}`;
  } catch {
    return `HTTP ${res.status}`;
  }
}

export async function api<T>(path: string, opts?: { method?: string; body?: unknown }): Promise<T> {
  const token = getToken();
  const res = await fetch(API_BASE_URL + path, {
    method: opts?.method || "GET",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: "Bearer " + token } : {}),
    },
    body: opts?.body ? JSON.stringify(opts.body) : undefined,
  });

  if (res.status === 401 || res.status === 403) {
    throw new Error("AUTH");
  }
  if (!res.ok) {
    throw new Error(await readError(res));
  }

  const body = (await res.json()) as ApiResult<T>;
  if (!body.ok) {
    throw new Error(body.message || "Request failed");
  }
  return body.data as T;
}

export async function upload(file: File): Promise<string> {
  const token = getToken();
  const fd = new FormData();
  fd.append("file", file);
  const res = await fetch(API_BASE_URL + "/api/common/upload", {
    method: "POST",
    headers: token ? { Authorization: "Bearer " + token } : {},
    body: fd,
  });
  if (res.status === 401 || res.status === 403) throw new Error("AUTH");
  if (!res.ok) throw new Error(await readError(res));
  const body = (await res.json()) as ApiResult<{ url: string }>;
  if (!body.ok) throw new Error(body.message || "Upload failed");
  return body.data?.url || "";
}
