import { ref } from "vue";

export type Toast = { id: string; kind: "ok" | "err"; title: string; detail?: string };

export const toasts = ref<Toast[]>([]);

function id() {
  return Math.random().toString(16).slice(2) + Date.now().toString(16);
}

export function toastOk(title: string, detail?: string) {
  push({ kind: "ok", title, detail });
}

export function toastErr(title: string, detail?: string) {
  push({ kind: "err", title, detail });
}

function push(t: Omit<Toast, "id">) {
  const item: Toast = { ...t, id: id() };
  toasts.value = [item, ...toasts.value].slice(0, 4);
  window.setTimeout(() => {
    toasts.value = toasts.value.filter((x) => x.id !== item.id);
  }, 2600);
}

