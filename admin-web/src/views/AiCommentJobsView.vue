<script setup lang="ts">
import { PlayCircle, RefreshCw, RotateCcw, Trash2 } from "lucide-vue-next";
import { onMounted, ref } from "vue";
import AppCard from "../components/AppCard.vue";
import TagPill from "../components/TagPill.vue";
import { useAuthFailureHandler } from "../lib/auth";
import { api } from "../lib/api";
import { fmtIso } from "../lib/format";
import { toastErr, toastOk } from "../lib/toast";

type PageResp<T> = { page: number; size: number; total: number; items: T[] };
type JobItem = {
  id: number;
  postId: number;
  providerCode: string;
  status: "PENDING" | "SUCCESS" | "FAILED" | string;
  attempts: number;
  nextRetryAt?: string;
  lastError?: string;
  commentId?: number | null;
  processedAt?: string;
  createdAt: string;
};
type Summary = { pending: number; success: number; failed: number };
type ConsumeResp = { processed: number };

const onAuthFail = useAuthFailureHandler();
const loading = ref(false);
const consuming = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const items = ref<JobItem[]>([]);
const status = ref("");
const q = ref("");
const summary = ref<Summary>({ pending: 0, success: 0, failed: 0 });

async function loadSummary() {
  summary.value = await api<Summary>("/api/admin/ai-comment-jobs/summary");
}

async function load() {
  try {
    loading.value = true;
    const data = await api<PageResp<JobItem>>(
      `/api/admin/ai-comment-jobs?page=${page.value}&size=${size.value}&status=${encodeURIComponent(status.value)}&q=${encodeURIComponent(q.value)}`
    );
    items.value = data.items;
    total.value = data.total;
    await loadSummary();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("加载失败", e?.message || "");
  } finally {
    loading.value = false;
  }
}

function applyFilter() {
  page.value = 1;
  load();
}

function prev() {
  if (page.value <= 1) return;
  page.value -= 1;
  load();
}

function next() {
  const max = Math.max(1, Math.ceil(total.value / size.value));
  if (page.value >= max) return;
  page.value += 1;
  load();
}

async function consumeOnce() {
  try {
    consuming.value = true;
    const r = await api<ConsumeResp>("/api/admin/ai-comment-jobs/consume-once", { method: "POST" });
    toastOk(`本次处理 ${r.processed} 条`);
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("执行失败", e?.message || "");
  } finally {
    consuming.value = false;
  }
}

async function retry(id: number) {
  try {
    await api<void>(`/api/admin/ai-comment-jobs/${id}/retry`, { method: "POST" });
    toastOk("已加入重试");
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("重试失败", e?.message || "");
  }
}

async function del(id: number) {
  if (!window.confirm(`确认删除任务 #${id} ?`)) return;
  try {
    await api<void>(`/api/admin/ai-comment-jobs/${id}/delete`, { method: "POST" });
    toastOk("已删除");
    if (items.value.length === 1 && page.value > 1) page.value -= 1;
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("删除失败", e?.message || "");
  }
}

onMounted(load);
</script>

<template>
  <AppCard>
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <div class="text-sm font-semibold text-slate-600">AI 评论任务队列</div>
        <div class="mt-1 text-lg font-semibold text-slate-900">待处理 / 失败监控与人工重试</div>
      </div>
      <div class="flex items-center gap-2">
        <TagPill tone="warn" :text="'待处理 ' + summary.pending" />
        <TagPill tone="ok" :text="'成功 ' + summary.success" />
        <TagPill tone="err" :text="'失败 ' + summary.failed" />
        <button class="btn btn-ghost" :disabled="loading" @click="load"><RefreshCw class="h-4 w-4" /> 刷新</button>
        <button class="btn btn-primary" :disabled="consuming" @click="consumeOnce"><PlayCircle class="h-4 w-4" /> 立即消费</button>
      </div>
    </div>

    <div class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-3">
      <div>
        <div class="text-xs font-semibold text-slate-600">状态</div>
        <select v-model="status" class="field mt-2">
          <option value="">全部</option>
          <option value="PENDING">PENDING</option>
          <option value="SUCCESS">SUCCESS</option>
          <option value="FAILED">FAILED</option>
        </select>
      </div>
      <div class="md:col-span-2">
        <div class="text-xs font-semibold text-slate-600">搜索</div>
        <input v-model="q" class="field mt-2" placeholder="provider / postId / 错误信息" @keydown.enter="applyFilter" />
      </div>
    </div>

    <div class="mt-3">
      <button class="btn btn-ghost" @click="applyFilter">应用筛选</button>
    </div>

    <div class="mt-4 overflow-hidden rounded-2xl border border-slate-200/70 bg-white/60">
      <table class="w-full text-sm">
        <thead class="bg-white/70 text-left text-xs text-slate-600">
          <tr>
            <th class="px-4 py-3 font-semibold">任务</th>
            <th class="px-4 py-3 font-semibold">状态</th>
            <th class="px-4 py-3 font-semibold">重试</th>
            <th class="px-4 py-3 font-semibold">错误</th>
            <th class="px-4 py-3 font-semibold">时间</th>
            <th class="px-4 py-3 font-semibold">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!items.length">
            <td colspan="6" class="px-4 py-10 text-center text-slate-500">
              <span v-if="loading">加载中...</span>
              <span v-else>暂无数据</span>
            </td>
          </tr>
          <tr v-for="j in items" :key="j.id" class="border-t border-slate-200/60">
            <td class="px-4 py-3">
              <div class="font-semibold text-slate-900">#{{ j.id }} · 帖子 {{ j.postId }}</div>
              <div class="mt-1 text-xs text-slate-500">{{ j.providerCode }}</div>
            </td>
            <td class="px-4 py-3">
              <TagPill
                :tone="j.status === 'SUCCESS' ? 'ok' : j.status === 'FAILED' ? 'err' : 'warn'"
                :text="j.status"
              />
            </td>
            <td class="px-4 py-3 text-slate-700">
              {{ j.attempts }}
              <div v-if="j.nextRetryAt" class="text-xs text-slate-500">下次：{{ fmtIso(j.nextRetryAt) }}</div>
            </td>
            <td class="px-4 py-3">
              <div class="max-w-[360px] truncate text-slate-700">{{ j.lastError || "-" }}</div>
            </td>
            <td class="px-4 py-3 text-slate-700">
              <div>创建：{{ fmtIso(j.createdAt) }}</div>
              <div v-if="j.processedAt" class="text-xs text-slate-500">处理：{{ fmtIso(j.processedAt) }}</div>
            </td>
            <td class="px-4 py-3">
              <div class="flex gap-2">
                <button class="btn btn-ghost" @click="retry(j.id)"><RotateCcw class="h-4 w-4" /> 重试</button>
                <button class="btn btn-ghost text-rose-600" @click="del(j.id)"><Trash2 class="h-4 w-4" /> 删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="mt-4 flex items-center justify-between gap-3">
      <div class="text-xs text-slate-500">第 {{ page }} 页 · 每页 {{ size }} 条</div>
      <div class="flex gap-2">
        <button class="btn btn-ghost" @click="prev">上一页</button>
        <button class="btn btn-ghost" @click="next">下一页</button>
      </div>
    </div>
  </AppCard>
</template>
