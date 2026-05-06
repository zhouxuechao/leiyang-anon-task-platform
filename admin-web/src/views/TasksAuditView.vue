<script setup lang="ts">
import { Check, FileSearch, RefreshCw, X } from "lucide-vue-next";
import { onMounted, ref } from "vue";
import AppCard from "../components/AppCard.vue";
import AppModal from "../components/AppModal.vue";
import TagPill from "../components/TagPill.vue";
import { useAuthFailureHandler } from "../lib/auth";
import { api } from "../lib/api";
import { fmtIso, fmtMoney } from "../lib/format";
import { toastErr, toastOk } from "../lib/toast";

type PendingTask = { taskNo: string; title: string; amount: string; deadlineAt: string; publisherOpenId: string };
type PageResp<T> = { page: number; size: number; total: number; items: T[] };

const onAuthFail = useAuthFailureHandler();

const loading = ref(false);
const items = ref<PendingTask[]>([]);
const page = ref(1);
const size = ref(20);
const total = ref(0);

const rejectOpen = ref(false);
const rejectTaskNo = ref("");
const rejectReason = ref("");

async function load() {
  try {
    loading.value = true;
    const data = await api<PageResp<PendingTask>>(`/api/admin/tasks/pending?page=${page.value}&size=${size.value}`);
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

async function approve(taskNo: string) {
  try {
    await api<void>(`/api/admin/tasks/${encodeURIComponent(taskNo)}/audit`, { method: "POST", body: { result: "APPROVE" } });
    toastOk("已通过任务");
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("操作失败", e?.message || "");
  }
}

function openReject(taskNo: string) {
  rejectTaskNo.value = taskNo;
  rejectReason.value = "";
  rejectOpen.value = true;
}

async function doReject() {
  try {
    await api<void>(`/api/admin/tasks/${encodeURIComponent(rejectTaskNo.value)}/audit`, {
      method: "POST",
      body: { result: "REJECT", reason: rejectReason.value },
    });
    toastOk("已驳回任务");
    rejectOpen.value = false;
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
        <div class="text-sm font-semibold text-slate-600">任务发布审核</div>
        <div class="mt-1 text-lg font-semibold text-slate-900">待审核列表</div>
      </div>
      <div class="flex items-center gap-2">
        <TagPill tone="info" :text="'待审 ' + total" />
        <button class="btn btn-ghost" :disabled="loading" @click="load">
          <RefreshCw class="h-4 w-4" /> 刷新
        </button>
      </div>
    </div>

    <div class="mt-4 overflow-x-auto rounded-2xl border border-slate-200/70 bg-white/60">
      <table class="w-full text-sm">
        <thead class="bg-white/70 text-left text-xs text-slate-600">
          <tr>
            <th class="px-4 py-3 font-semibold">任务</th>
            <th class="px-4 py-3 font-semibold">金额</th>
            <th class="px-4 py-3 font-semibold">截止</th>
            <th class="px-4 py-3 font-semibold">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!items.length">
            <td colspan="4" class="px-4 py-10 text-center text-slate-500">
              <span v-if="loading">加载中...</span>
              <span v-else>暂无待审任务</span>
            </td>
          </tr>
          <tr v-for="t in items" :key="t.taskNo" class="border-t border-slate-200/60">
            <td class="px-4 py-3">
              <div class="font-semibold text-slate-900">{{ t.title }}</div>
              <div class="mt-0.5 text-xs text-slate-500">#{{ t.taskNo }} · {{ t.publisherOpenId }}</div>
            </td>
            <td class="px-4 py-3 font-semibold text-slate-900">{{ fmtMoney(t.amount) }}</td>
            <td class="px-4 py-3 text-slate-700">{{ fmtIso(t.deadlineAt) }}</td>
            <td class="px-4 py-3">
              <div class="flex items-center gap-2">
                <button class="btn btn-ghost" @click="openReject(t.taskNo)"><X class="h-4 w-4" /> 驳回</button>
                <button class="btn btn-success" @click="approve(t.taskNo)"><Check class="h-4 w-4" /> 通过</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="mt-4 flex items-center gap-2 text-xs text-slate-500">
      <FileSearch class="h-4 w-4" />
      建议结合敏感词、预算异常、导流词等策略做人工复核
    </div>
    <div class="mt-4 flex items-center justify-between text-sm text-slate-600">
      <span>第 {{ page }} 页 / 共 {{ Math.max(1, Math.ceil(total / size)) }} 页</span>
      <div class="flex gap-2"><button class="btn btn-ghost" :disabled="page <= 1" @click="prev">上一页</button><button class="btn btn-ghost" :disabled="page >= Math.max(1, Math.ceil(total / size))" @click="next">下一页</button></div>
    </div>
  </AppCard>

  <AppModal :open="rejectOpen" title="驳回任务" @close="rejectOpen = false">
    <div class="text-sm text-slate-700">任务号：<span class="font-semibold text-slate-900">{{ rejectTaskNo }}</span></div>
    <div class="mt-3">
      <div class="text-xs font-semibold text-slate-600">原因（可选）</div>
      <textarea v-model="rejectReason" class="field mt-2 h-28 resize-none" placeholder="例如：包含导流信息 / 描述不清 / 风险任务" />
    </div>
    <div class="mt-4 flex justify-end gap-2">
      <button class="btn btn-ghost" @click="rejectOpen = false">取消</button>
      <button class="btn btn-primary" @click="doReject">确认驳回</button>
    </div>
  </AppModal>
</template>
