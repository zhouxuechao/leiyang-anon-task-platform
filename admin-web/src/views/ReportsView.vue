<script setup lang="ts">
import { Flag, RefreshCw, X } from "lucide-vue-next";
import { onMounted, ref } from "vue";
import AppCard from "../components/AppCard.vue";
import TagPill from "../components/TagPill.vue";
import { useAuthFailureHandler } from "../lib/auth";
import { api } from "../lib/api";
import { fmtIso } from "../lib/format";
import { toastErr, toastOk } from "../lib/toast";

type PendingReport = {
  reportNo: string;
  reporterOpenId: string;
  targetType: string;
  targetId: number;
  reason: string;
  status: string;
  createdAt: string;
};
type PageResp<T> = { page: number; size: number; total: number; items: T[] };

const onAuthFail = useAuthFailureHandler();
const loading = ref(false);
const items = ref<PendingReport[]>([]);
const page = ref(1);
const size = ref(20);
const total = ref(0);

async function load() {
  try {
    loading.value = true;
    const data = await api<PageResp<PendingReport>>(`/api/admin/reports/pending?page=${page.value}&size=${size.value}`);
    items.value = data.items || [];
    total.value = Number(data.total || 0);
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("加载失败", e?.message || "");
  } finally {
    loading.value = false;
  }
}

function prev() { if (page.value > 1) { page.value -= 1; void load(); } }
function next() {
  const max = Math.max(1, Math.ceil(total.value / size.value));
  if (page.value < max) { page.value += 1; void load(); }
}

async function resolve(reportNo: string, result: "RESOLVE" | "REJECT") {
  try {
    await api<void>(`/api/admin/reports/${encodeURIComponent(reportNo)}/resolve`, { method: "POST", body: { result } });
    toastOk("已处理");
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("操作失败", e?.message || "");
  }
}

onMounted(load);
</script>

<template>
  <AppCard>
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <div class="text-sm font-semibold text-slate-600">举报处理</div>
        <div class="mt-1 text-lg font-semibold text-slate-900">待处理列表</div>
      </div>
      <div class="flex items-center gap-2">
        <TagPill tone="warn" :text="'待处理 ' + total" />
        <button class="btn btn-ghost" :disabled="loading" @click="load"><RefreshCw class="h-4 w-4" /> 刷新</button>
      </div>
    </div>

    <div class="mt-4 grid gap-3">
      <div v-if="!items.length" class="rounded-2xl border border-slate-200/70 bg-white/60 px-4 py-10 text-center text-sm text-slate-500">
        <span v-if="loading">加载中...</span>
        <span v-else>暂无待处理举报</span>
      </div>

      <div v-for="r in items" :key="r.reportNo" class="rounded-2xl border border-slate-200/70 bg-white/60 p-4">
        <div class="flex flex-wrap items-start justify-between gap-3">
          <div>
            <div class="text-xs font-semibold text-slate-600">#{{ r.reportNo }} · {{ r.reporterOpenId }}</div>
            <div class="mt-1 font-semibold text-slate-900">{{ r.targetType }}: {{ r.targetId }}</div>
            <div class="mt-2 text-sm text-slate-700">{{ r.reason }}</div>
            <div class="mt-2 text-xs text-slate-500">{{ fmtIso(r.createdAt) }}</div>
          </div>
          <div class="flex gap-2">
            <button class="btn btn-ghost" @click="resolve(r.reportNo, 'REJECT')"><X class="h-4 w-4" /> 驳回</button>
            <button class="btn btn-success" @click="resolve(r.reportNo, 'RESOLVE')">
              <Flag class="h-4 w-4" /> 处理
            </button>
          </div>
        </div>
      </div>
    </div>
    <div class="mt-4 flex items-center justify-between text-sm text-slate-600">
      <span>第 {{ page }} 页 / 共 {{ Math.max(1, Math.ceil(total / size)) }} 页</span>
      <div class="flex gap-2"><button class="btn btn-ghost" :disabled="page <= 1" @click="prev">上一页</button><button class="btn btn-ghost" :disabled="page >= Math.max(1, Math.ceil(total / size))" @click="next">下一页</button></div>
    </div>
  </AppCard>
</template>
