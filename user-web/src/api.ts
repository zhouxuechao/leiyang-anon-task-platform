export type ApiResult<T> = { ok: boolean; message?: string | null; data?: T };

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? (import.meta.env.DEV ? "http://127.0.0.1:8080" : "");
const TOKEN_KEY = "leiyang_user_token";

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || "";
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

async function readError(res: Response) {
  try {
    const body = (await res.json()) as ApiResult<unknown>;
    return normalizeApiMessage(body?.message || `HTTP ${res.status}`);
  } catch {
    return `HTTP ${res.status}`;
  }
}

function normalizeApiMessage(message?: string | null) {
  const raw = String(message || "").trim();
  const lower = raw.toLowerCase();
  if (lower.includes("call frequency is too high") || lower.includes("too many requests") || lower.includes("rate limit") || lower.includes("frequency")) {
    return "AI音乐生成服务调用过于频繁，请稍后再试";
  }
  if (lower.includes("current credits are insufficient") || lower.includes("please top up") || lower.includes("insufficient")) {
    return "AI音乐生成服务额度不足，请联系平台管理员充值后再试";
  }
  return raw;
}

export async function api<T>(path: string, opts?: { method?: string; body?: unknown; auth?: boolean }): Promise<T> {
  const token = getToken();
  const res = await fetch(API_BASE_URL + path, {
    method: opts?.method || "GET",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: "Bearer " + token } : {}),
    },
    body: opts?.body ? JSON.stringify(opts.body) : undefined,
  });
  if (res.status === 401 || res.status === 403) throw new Error("AUTH");
  if (!res.ok) throw new Error(await readError(res));
  const body = (await res.json()) as ApiResult<T>;
  if (!body.ok) throw new Error(normalizeApiMessage(body.message || "Request failed"));
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
  if (!res.ok) throw new Error(await readError(res));
  const body = (await res.json()) as ApiResult<{ url: string }>;
  if (!body.ok) throw new Error(normalizeApiMessage(body.message || "Upload failed"));
  return body.data?.url || "";
}

export function fileUrl(value?: string | null) {
  const v = String(value || "").trim();
  if (!v) return "";
  if (/^https?:\/\//i.test(v)) return v.replace(/^http:\/\/(?:127\.0\.0\.1|localhost):8080/i, API_BASE_URL);
  if (v.startsWith("/")) return API_BASE_URL + v;
  return API_BASE_URL + "/" + v;
}
