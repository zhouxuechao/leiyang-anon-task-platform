<script setup lang="ts">
import { BadgeCheck, Check, Eye, RefreshCw, X } from "lucide-vue-next";
import { onMounted, ref } from "vue";
import AppCard from "../components/AppCard.vue";
import AppDrawer from "../components/AppDrawer.vue";
import AppModal from "../components/AppModal.vue";
import TagPill from "../components/TagPill.vue";
import { useAuthFailureHandler } from "../lib/auth";
import { api, API_BASE_URL } from "../lib/api";
import { fmtIso, fmtMoney } from "../lib/format";
import { toastErr, toastOk } from "../lib/toast";

type SubmittedOrder = { orderNo: string; taskNo: string; taskTitle: string; userOpenId: string; submitTime: string | null };
type PageResp<T> = { page: number; size: number; total: number; items: T[] };
type OrderDetail = {
  orderNo: string;
  orderStatus: string;
  auditReason: string | null;
  acceptUserOpenId: string;
  submitTime: string | null;
  taskNo: string;
  taskTitle: string;
  taskAmount: number;
  proofs: { type: string; url: string; remark?: string | null; createdAt: string }[];
};

const onAuthFail = useAuthFailureHandler();

const loading = ref(false);
const items = ref<SubmittedOrder[]>([]);
const page = ref(1);
const size = ref(20);
const total = ref(0);

const drawerOpen = ref(false);
const detail = ref<OrderDetail | null>(null);
const detailLoading = ref(false);

const modalOpen = ref(false);
const modalMode = ref<"REJECT_RESUBMIT" | "REJECT_CLOSE">("REJECT_RESUBMIT");
const modalReason = ref("");

function absUrl(u: string) {
  if (!u) return u;
  if (u.startsWith("http://") || u.startsWith("https://")) return u;
  return API_BASE_URL + u;
}

async function load() {
  try {
    loading.value = true;
    const data = await api<PageResp<SubmittedOrder>>(`/api/admin/orders/submitted?page=${page.value}&size=${size.value}`);
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

async function openDetail(orderNo: string) {
  drawerOpen.value = true;
  detail.value = null;
  try {
    detailLoading.value = true;
    detail.value = await api<OrderDetail>(`/api/admin/orders/${encodeURIComponent(orderNo)}`);
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("加载失败", e?.message || "");
  } finally {
    detailLoading.value = false;
  }
}

async function approve(orderNo: string) {
  try {
    await api<void>(`/api/admin/orders/${encodeURIComponent(orderNo)}/audit`, { method: "POST", body: { result: "APPROVE" } });
    toastOk("已通过并发奖");
    drawerOpen.value = false;
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("操作失败", e?.message || "");
  }
}

function openReject(mode: "REJECT_RESUBMIT" | "REJECT_CLOSE") {
  modalMode.value = mode;
  modalReason.value = "";
  modalOpen.value = true;
}

async function doReject() {
  if (!detail.value) return;
  try {
    await api<void>(`/api/admin/orders/${encodeURIComponent(detail.value.orderNo)}/audit`, {
      method: "POST",
      body: { result: modalMode.value, reason: modalReason.value },
    });
    toastOk(modalMode.value === "REJECT_CLOSE" ? "已驳回并关闭" : "已驳回可重提");
    modalOpen.value = false;
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
        <div class="text-sm font-semibold text-slate-600">凭证审核</div>
        <div class="mt-1 text-lg font-semibold text-slate-900">待审核列表</div>
      </div>
      <div class="flex items-center gap-2">
        <TagPill tone="warn" :text="'待审 ' + total" />
        <button class="btn btn-ghost" :disabled="loading" @click="load">
          <RefreshCw class="h-4 w-4" /> 刷新
        </button>
      </div>
    </div>

    <div class="mt-4 overflow-x-auto rounded-2xl border border-slate-200/70 bg-white/60">
      <table class="w-full text-sm">
        <thead class="bg-white/70 text-left text-xs text-slate-600">
          <tr>
            <th class="px-4 py-3 font-semibold">订单</th>
            <th class="px-4 py-3 font-semibold">接单人</th>
            <th class="px-4 py-3 font-semibold">提交时间</th>
            <th class="px-4 py-3 font-semibold">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!items.length">
            <td colspan="4" class="px-4 py-10 text-center text-slate-500">
              <span v-if="loading">加载中...</span>
              <span v-else>暂无待审凭证</span>
            </td>
          </tr>
          <tr v-for="o in items" :key="o.orderNo" class="border-t border-slate-200/60">
            <td class="px-4 py-3">
              <div class="font-semibold text-slate-900">{{ o.taskTitle }}</div>
              <div class="mt-0.5 text-xs text-slate-500">#{{ o.orderNo }} · {{ o.taskNo }}</div>
            </td>
            <td class="px-4 py-3 text-slate-800">{{ o.userOpenId }}</td>
            <td class="px-4 py-3 text-slate-700">{{ fmtIso(o.submitTime || "") }}</td>
            <td class="px-4 py-3">
              <button class="btn btn-ghost" @click="openDetail(o.orderNo)"><Eye class="h-4 w-4" /> 查看</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="mt-4 flex items-center gap-2 text-xs text-slate-500">
      <BadgeCheck class="h-4 w-4" />
      “通过”会立即发奖入余额（MVP 逻辑：平台兜底审核）
    </div>
    <div class="mt-4 flex items-center justify-between text-sm text-slate-600">
      <span>第 {{ page }} 页 / 共 {{ Math.max(1, Math.ceil(total / size)) }} 页</span>
      <div class="flex gap-2"><button class="btn btn-ghost" :disabled="page <= 1" @click="prev">上一页</button><button class="btn btn-ghost" :disabled="page >= Math.max(1, Math.ceil(total / size))" @click="next">下一页</button></div>
    </div>
  </AppCard>

  <AppDrawer :open="drawerOpen" title="订单详情与审核" @close="drawerOpen = false">
    <div v-if="detailLoading" class="rounded-2xl border border-slate-200/70 bg-white/60 px-4 py-6 text-sm text-slate-600">
      加载中...
    </div>
    <div v-else-if="detail" class="grid gap-4">
      <div class="rounded-2xl border border-slate-200/70 bg-white/60 p-4">
        <div class="text-xs font-semibold text-slate-600">订单信息</div>
        <div class="mt-2 text-sm text-slate-800">
          <div class="flex flex-wrap items-center justify-between gap-2">
            <div class="font-semibold text-slate-900">#{{ detail.orderNo }}</div>
            <TagPill tone="info" :text="detail.orderStatus" />
          </div>
          <div class="mt-2 text-slate-600">
            任务：<span class="font-semibold text-slate-900">{{ detail.taskTitle }}</span>
            · 金额：<span class="font-semibold text-slate-900">{{ fmtMoney(detail.taskAmount) }}</span>
          </div>
          <div class="mt-1 text-slate-600">接单人：{{ detail.acceptUserOpenId }}</div>
          <div class="mt-1 text-slate-600">提交时间：{{ fmtIso(detail.submitTime || "") }}</div>
        </div>
      </div>

      <div class="rounded-2xl border border-slate-200/70 bg-white/60 p-4">
        <div class="text-xs font-semibold text-slate-600">凭证</div>
        <div v-if="!detail.proofs.length" class="mt-3 text-sm text-slate-500">暂无凭证</div>
        <div v-else class="mt-3 grid grid-cols-2 gap-3">
          <div v-for="(p, idx) in detail.proofs" :key="idx" class="overflow-hidden rounded-2xl border border-slate-200 bg-white">
            <img v-if="p.type === 'IMAGE'" :src="absUrl(p.url)" class="h-40 w-full object-cover" />
            <div class="p-3">
              <div class="text-xs font-semibold text-slate-600">{{ p.type }}</div>
              <div v-if="p.remark" class="mt-1 text-sm text-slate-700">{{ p.remark }}</div>
              <div class="mt-2 text-xs text-slate-500">{{ fmtIso(p.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="rounded-2xl border border-slate-200/70 bg-white/60 p-4">
        <div class="text-xs font-semibold text-slate-600">审核操作</div>
        <div class="mt-3 flex flex-wrap gap-2">
          <button class="btn btn-ghost" @click="openReject('REJECT_RESUBMIT')"><X class="h-4 w-4" /> 驳回可重提</button>
          <button class="btn btn-ghost" @click="openReject('REJECT_CLOSE')"><X class="h-4 w-4" /> 驳回并关闭</button>
          <button class="btn btn-success" @click="approve(detail.orderNo)"><Check class="h-4 w-4" /> 通过并发奖</button>
        </div>
      </div>
    </div>
  </AppDrawer>

  <AppModal :open="modalOpen" :title="modalMode === 'REJECT_CLOSE' ? '驳回并关闭' : '驳回可重提'" @close="modalOpen = false">
    <div class="text-sm text-slate-700">订单：<span class="font-semibold text-slate-900">{{ detail?.orderNo }}</span></div>
    <div class="mt-3">
      <div class="text-xs font-semibold text-slate-600">原因（可选）</div>
      <textarea v-model="modalReason" class="field mt-2 h-28 resize-none" placeholder="例如：图片不清晰 / 数量不足 / 与任务不符" />
    </div>
    <div class="mt-4 flex justify-end gap-2">
      <button class="btn btn-ghost" @click="modalOpen = false">取消</button>
      <button class="btn btn-primary" @click="doReject">确认</button>
    </div>
  </AppModal>
</template>
