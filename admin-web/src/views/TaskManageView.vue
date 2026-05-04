<script setup lang="ts">
import { Eye, RefreshCw, X } from "lucide-vue-next";
import { onMounted, ref } from "vue";
import AppCard from "../components/AppCard.vue";
import AppDrawer from "../components/AppDrawer.vue";
import AppModal from "../components/AppModal.vue";
import TagPill from "../components/TagPill.vue";
import { useAuthFailureHandler } from "../lib/auth";
import { api, API_BASE_URL } from "../lib/api";
import { fmtIso, fmtMoney } from "../lib/format";
import { toastErr, toastOk } from "../lib/toast";

type PageResp<T> = { page: number; size: number; total: number; items: T[] };
type TaskItem = {
  taskNo: string;
  status: string;
  title: string;
  amount: number;
  totalSlots: number;
  acceptedSlots: number;
  deadlineAt: string;
  publisherOpenId: string;
  createdAt: string;
};
type TaskDetail = {
  taskNo: string;
  status: string;
  title: string;
  content: string;
  locationText: string | null;
  amount: number;
  totalSlots: number;
  acceptedSlots: number;
  deadlineAt: string;
  proofRequirements: string | null;
  rejectReason: string | null;
  publisherOpenId: string;
  createdAt: string;
};
type TaskSubmissionProof = {
  type: string;
  url: string;
  remark?: string | null;
  createdAt: string;
};
type TaskSubmissionDetail = {
  orderNo: string;
  orderStatus: string;
  auditReason?: string | null;
  acceptUserOpenId: string;
  acceptUserName: string;
  acceptTime: string;
  submitTime: string;
  settledTime: string;
  settledAmount: string;
  proofs: TaskSubmissionProof[];
};

const onAuthFail = useAuthFailureHandler();
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const items = ref<TaskItem[]>([]);

const q = ref("");
const status = ref("");

const drawerOpen = ref(false);
const detail = ref<TaskDetail | null>(null);
const submissions = ref<TaskSubmissionDetail[]>([]);
const detailLoading = ref(false);

const closeOpen = ref(false);
const closeReason = ref("违规/作废");

function tone(st: string) {
  if (st === "PUBLISHED") return "ok";
  if (st === "PENDING_AUDIT") return "warn";
  if (st === "REJECTED") return "err";
  if (st === "CLOSED") return "err";
  if (st === "EXPIRED") return "warn";
  return "info";
}

function orderTone(st: string) {
  if (st === "SETTLED" || st === "APPROVED") return "ok";
  if (st === "SUBMITTED") return "warn";
  if (st === "REJECTED_RESUBMIT" || st === "REJECTED_CLOSE" || st === "TIMEOUT") return "err";
  return "info";
}

function fileUrl(raw?: string | null) {
  const v = String(raw || "").trim();
  if (!v || v === "TEXT_CONTENT") return "";
  if (/^https?:\/\//i.test(v)) return v;
  if (v.startsWith("/")) return API_BASE_URL.replace(/\/$/, "") + v;
  return API_BASE_URL.replace(/\/$/, "") + "/" + v;
}

async function load() {
  try {
    loading.value = true;
    const data = await api<PageResp<TaskItem>>(
      `/api/admin/task-manage/tasks?page=${page.value}&size=${size.value}&status=${encodeURIComponent(status.value)}&q=${encodeURIComponent(q.value)}`
    );
    items.value = data.items;
    total.value = data.total;
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("加载失败", e?.message || "");
  } finally {
    loading.value = false;
  }
}

async function openDetail(taskNo: string) {
  drawerOpen.value = true;
  detail.value = null;
  submissions.value = [];
  try {
    detailLoading.value = true;
    const [task, list] = await Promise.all([
      api<TaskDetail>(`/api/admin/task-manage/tasks/${encodeURIComponent(taskNo)}`),
      api<TaskSubmissionDetail[]>(`/api/admin/task-manage/tasks/${encodeURIComponent(taskNo)}/submissions`),
    ]);
    detail.value = task;
    submissions.value = list || [];
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("加载失败", e?.message || "");
  } finally {
    detailLoading.value = false;
  }
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

function applyFilter() {
  page.value = 1;
  load();
}

function openClose() {
  closeReason.value = "违规/作废";
  closeOpen.value = true;
}

async function doClose() {
  if (!detail.value) return;
  try {
    await api<void>(`/api/admin/task-manage/tasks/${encodeURIComponent(detail.value.taskNo)}/close`, {
      method: "POST",
      body: { reason: closeReason.value },
    });
    toastOk("已关闭任务");
    closeOpen.value = false;
    drawerOpen.value = false;
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
        <div class="text-sm font-semibold text-slate-600">任务列表</div>
        <div class="mt-1 text-lg font-semibold text-slate-900">全量任务（分页）</div>
      </div>
      <div class="flex items-center gap-2">
        <TagPill tone="info" :text="String(total) + ' 条'" />
        <button class="btn btn-ghost" :disabled="loading" @click="load"><RefreshCw class="h-4 w-4" /> 刷新</button>
      </div>
    </div>

    <div class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-3">
      <div class="md:col-span-2">
        <div class="text-xs font-semibold text-slate-600">搜索</div>
        <input v-model="q" class="field mt-2" placeholder="标题/内容关键字" @keydown.enter="applyFilter" />
      </div>
      <div>
        <div class="text-xs font-semibold text-slate-600">状态</div>
        <select v-model="status" class="field mt-2" @change="applyFilter">
          <option value="">全部</option>
          <option value="PENDING_AUDIT">PENDING_AUDIT</option>
          <option value="PUBLISHED">PUBLISHED</option>
          <option value="REJECTED">REJECTED</option>
          <option value="EXPIRED">EXPIRED</option>
          <option value="CLOSED">CLOSED</option>
        </select>
      </div>
    </div>

    <div class="mt-4 overflow-hidden rounded-2xl border border-slate-200/70 bg-white/60">
      <table class="w-full text-sm">
        <thead class="bg-white/70 text-left text-xs text-slate-600">
          <tr>
            <th class="px-4 py-3 font-semibold">任务</th>
            <th class="px-4 py-3 font-semibold">状态</th>
            <th class="px-4 py-3 font-semibold">金额</th>
            <th class="px-4 py-3 font-semibold">名额</th>
            <th class="px-4 py-3 font-semibold">截止</th>
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
          <tr v-for="t in items" :key="t.taskNo" class="border-t border-slate-200/60">
            <td class="px-4 py-3">
              <div class="font-semibold text-slate-900">{{ t.title }}</div>
              <div class="mt-0.5 text-xs text-slate-500">#{{ t.taskNo }} · {{ t.publisherOpenId }}</div>
            </td>
            <td class="px-4 py-3">
              <TagPill :tone="tone(t.status) as any" :text="t.status" />
            </td>
            <td class="px-4 py-3 font-semibold text-slate-900">{{ fmtMoney(t.amount) }}</td>
            <td class="px-4 py-3 text-slate-700">{{ t.acceptedSlots }} / {{ t.totalSlots }}</td>
            <td class="px-4 py-3 text-slate-700">{{ fmtIso(t.deadlineAt) }}</td>
            <td class="px-4 py-3">
              <button class="btn btn-ghost" @click="openDetail(t.taskNo)"><Eye class="h-4 w-4" /> 查看</button>
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

  <AppDrawer :open="drawerOpen" title="任务详情" @close="drawerOpen = false">
    <div v-if="detailLoading" class="rounded-2xl border border-slate-200/70 bg-white/60 px-4 py-6 text-sm text-slate-600">加载中...</div>
    <div v-else-if="detail" class="grid gap-4">
      <div class="rounded-2xl border border-slate-200/70 bg-white/60 p-4">
        <div class="flex items-start justify-between gap-3">
          <div>
            <div class="text-xs font-semibold text-slate-600">#{{ detail.taskNo }}</div>
            <div class="mt-1 text-lg font-semibold text-slate-900">{{ detail.title }}</div>
            <div class="mt-1 text-xs text-slate-500">{{ detail.publisherOpenId }} · {{ fmtIso(detail.createdAt) }}</div>
          </div>
          <TagPill :tone="tone(detail.status) as any" :text="detail.status" />
        </div>
        <div class="mt-3 text-sm text-slate-800">
          <div>金额：<span class="font-semibold">{{ fmtMoney(detail.amount) }}</span></div>
          <div class="mt-1">名额：{{ detail.acceptedSlots }} / {{ detail.totalSlots }}</div>
          <div class="mt-1">地点：{{ detail.locationText || "-" }}</div>
          <div class="mt-1">截止：{{ fmtIso(detail.deadlineAt) }}</div>
          <div class="mt-1">凭证：{{ detail.proofRequirements || "-" }}</div>
          <div v-if="detail.rejectReason" class="mt-2 text-sm text-rose-700">
            备注：{{ detail.rejectReason }}
          </div>
        </div>
      </div>

      <div class="rounded-2xl border border-slate-200/70 bg-white/60 p-4">
        <div class="text-xs font-semibold text-slate-600">描述</div>
        <div class="mt-2 whitespace-pre-wrap text-sm text-slate-800">{{ detail.content }}</div>
      </div>

      <div class="rounded-2xl border border-slate-200/70 bg-white/60 p-4">
        <div class="flex items-center justify-between gap-3">
          <div class="text-sm font-semibold text-slate-700">用户完成明细（{{ submissions.length }}）</div>
          <TagPill tone="info" :text="'已接单 ' + detail.acceptedSlots + ' / ' + detail.totalSlots" />
        </div>
        <div v-if="!submissions.length" class="mt-3 text-sm text-slate-500">暂无用户提交凭证</div>
        <div v-for="s in submissions" :key="s.orderNo" class="mt-3 rounded-xl border border-slate-200/70 bg-white/70 p-3">
          <div class="flex flex-wrap items-start justify-between gap-2">
            <div>
              <div class="text-sm font-semibold text-slate-900">{{ s.acceptUserName }}</div>
              <div class="mt-1 text-xs text-slate-500">
                {{ s.acceptUserOpenId }} · {{ s.orderNo }} · 提交 {{ fmtIso(s.submitTime) }}
              </div>
              <div v-if="s.auditReason" class="mt-1 text-xs text-rose-600">备注：{{ s.auditReason }}</div>
            </div>
            <div class="flex flex-wrap items-center justify-end gap-2">
              <TagPill :tone="orderTone(s.orderStatus) as any" :text="s.orderStatus" />
              <TagPill v-if="s.settledAmount" tone="ok" :text="'结算 ' + fmtMoney(Number(s.settledAmount))" />
            </div>
          </div>
          <div v-if="s.proofs.length" class="mt-3 grid gap-2">
            <div v-for="(p, idx) in s.proofs" :key="idx" class="rounded-lg border border-slate-200 bg-white p-2">
              <div class="mb-1 text-xs font-semibold text-slate-500">
                {{ p.type }} · {{ fmtIso(p.createdAt) }}
              </div>
              <a v-if="p.type === 'IMAGE' && fileUrl(p.url)" :href="fileUrl(p.url)" target="_blank" class="block">
                <img :src="fileUrl(p.url)" class="h-28 w-28 rounded-lg border border-slate-200 object-cover" />
              </a>
              <div v-else class="whitespace-pre-wrap text-sm text-slate-800">{{ p.remark || p.url || "-" }}</div>
            </div>
          </div>
          <div v-else class="mt-2 text-sm text-slate-500">暂无凭证内容</div>
        </div>
      </div>

      <div class="rounded-2xl border border-slate-200/70 bg-white/60 p-4">
        <div class="text-xs font-semibold text-slate-600">管理操作</div>
        <div class="mt-3 flex flex-wrap gap-2">
          <button class="btn btn-ghost" @click="openClose"><X class="h-4 w-4" /> 关闭任务</button>
        </div>
      </div>
    </div>
  </AppDrawer>

  <AppModal :open="closeOpen" title="关闭任务" @close="closeOpen = false">
    <div class="text-sm text-slate-700">任务：<span class="font-semibold text-slate-900">{{ detail?.taskNo }}</span></div>
    <div class="mt-3">
      <div class="text-xs font-semibold text-slate-600">原因</div>
      <textarea v-model="closeReason" class="field mt-2 h-28 resize-none" placeholder="违规/作废原因" />
    </div>
    <div class="mt-4 flex justify-end gap-2">
      <button class="btn btn-ghost" @click="closeOpen = false">取消</button>
      <button class="btn btn-primary" @click="doClose">确认关闭</button>
    </div>
  </AppModal>
</template>
