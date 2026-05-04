export function fmtMoney(n: string | number | undefined | null): string {
  if (n === undefined || n === null) return "-";
  const v = typeof n === "string" ? Number(n) : n;
  if (!Number.isFinite(v)) return String(n);
  return v.toFixed(2);
}

export function fmtIso(iso?: string | null): string {
  if (!iso) return "-";
  return iso.replace("T", " ").replace("Z", "Z");
}

