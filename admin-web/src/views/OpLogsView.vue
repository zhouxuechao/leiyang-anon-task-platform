<script setup lang="ts">
import { RefreshCw, ScrollText } from "lucide-vue-next";
import { onMounted, ref } from "vue";
import AppCard from "../components/AppCard.vue";
import TagPill from "../components/TagPill.vue";
import { useAuthFailureHandler } from "../lib/auth";
import { api } from "../lib/api";
import { fmtIso } from "../lib/format";
import { toastErr } from "../lib/toast";

type LogItem = {
  id: number;
  actorType: string;
  actorId: number;
  method: string;
  path: string;
  ip: string | null;
  userAgent: string | null;
  ok: boolean;
  error: string | null;
  createdAt: string;
};
type PageResp<T> = { page: number; size: number; total: number; items: T[] };

const onAuthFail = useAuthFailureHandler();
const loading = ref(false);
const items = ref<LogItem[]>([]);
const page = ref(1);
const size = ref(20);
const total = ref(0);

async function load() {
  try {
    loading.value = true;
    const data = await api<PageResp<LogItem>>(`/api/admin/op-logs?page=${page.value}&size=${size.value}`);
    items.value = data.items;
    total.value = data.total;
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("加载失败", e?.message || "");
  } finally {
    loading.value = false;
  }
}

async function prev() {
  if (page.value <= 1) return;
  page.value -= 1;
  await load();
}

async function next() {
  if (page.value * size.value >= total.value) return;
  page.value += 1;
  await load();
}

onMounted(load);
</script>

<template>
  <AppCard>
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <div class="text-sm font-semibold text-slate-600">操作日志</div>
        <div class="mt-1 text-lg font-semibold text-slate-900">操作审计记录</div>
      </div>
      <div class="flex items-center gap-2">
        <TagPill tone="info" :text="total + ' 条'" />
        <button class="btn btn-ghost" :disabled="loading" @click="load"><RefreshCw class="h-4 w-4" /> 刷新</button>
      </div>
    </div>

    <div class="mt-4 overflow-x-auto rounded-2xl border border-slate-200/70 bg-white/60">
      <table class="w-full text-sm">
        <thead class="bg-white/70 text-left text-xs text-slate-600">
          <tr>
            <th class="px-4 py-3 font-semibold">时间</th>
            <th class="px-4 py-3 font-semibold">Actor</th>
            <th class="px-4 py-3 font-semibold">请求</th>
            <th class="px-4 py-3 font-semibold">结果</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!items.length">
            <td colspan="4" class="px-4 py-10 text-center text-slate-500">
              <span v-if="loading">加载中...</span>
              <span v-else>暂无数据</span>
            </td>
          </tr>
          <tr v-for="l in items" :key="l.id" class="border-t border-slate-200/60">
            <td class="px-4 py-3 text-slate-700">{{ fmtIso(l.createdAt) }}</td>
            <td class="px-4 py-3 text-slate-800">
              <div class="font-semibold text-slate-900">{{ l.actorType }} #{{ l.actorId }}</div>
              <div class="mt-0.5 text-xs text-slate-500">{{ l.ip || "-" }}</div>
            </td>
            <td class="px-4 py-3 text-slate-800">
              <div class="font-semibold text-slate-900">{{ l.method }} {{ l.path }}</div>
              <div class="mt-0.5 truncate text-xs text-slate-500" style="max-width: 520px">{{ l.userAgent || "-" }}</div>
            </td>
            <td class="px-4 py-3">
              <TagPill :tone="l.ok ? 'ok' : 'err'" :text="l.ok ? 'OK' : 'ERR'" />
              <div v-if="!l.ok && l.error" class="mt-1 text-xs text-slate-500">{{ l.error }}</div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="mt-4 flex items-center justify-between text-sm text-slate-600">
      <div>第 {{ page }} 页 / 共 {{ total }} 条</div>
      <div class="flex gap-2">
        <button class="btn btn-ghost" :disabled="page <= 1 || loading" @click="prev">上一页</button>
        <button class="btn btn-ghost" :disabled="page * size >= total || loading" @click="next">下一页</button>
      </div>
    </div>

    <div class="mt-4 flex items-center gap-2 text-xs text-slate-500">
      <ScrollText class="h-4 w-4" />
      封禁/退款/提现/审核等敏感操作必须可追溯（后续可扩展为更完整审计日志）
    </div>
  </AppCard>
</template>
