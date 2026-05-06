<script setup lang="ts">
import { Check, RefreshCw, Wallet, X } from "lucide-vue-next";
import { onMounted, ref } from "vue";
import AppCard from "../components/AppCard.vue";
import AppModal from "../components/AppModal.vue";
import TagPill from "../components/TagPill.vue";
import { useAuthFailureHandler } from "../lib/auth";
import { API_BASE_URL, api } from "../lib/api";
import { fmtIso, fmtMoney } from "../lib/format";
import { toastErr, toastOk } from "../lib/toast";
import { getToken } from "../state/auth";

type PendingWithdraw = {
  applyNo: string;
  userOpenId: string;
  amount: string;
  channel: string;
  status: string;
  createdAt: string;
  qrCodeUrl: string;
};
type WithdrawStats = {
  pendingCount: number;
  pendingAmount: string;
  paidCount: number;
  paidAmount: string;
  todayPaidCount: number;
  todayPaidAmount: string;
};
type WithdrawRecord = {
  applyNo: string;
  userOpenId: string;
  amount: string;
  channel: string;
  status: string;
  createdAt: string;
  qrCodeUrl: string;
  paidProofUrl: string;
  payRemark: string;
  paidAt: string;
};
type PageResp<T> = { page: number; size: number; total: number; items: T[] };

const onAuthFail = useAuthFailureHandler();

const loading = ref(false);
const items = ref<PendingWithdraw[]>([]);
const stats = ref<WithdrawStats | null>(null);
const recLoading = ref(false);
const records = ref<WithdrawRecord[]>([]);
const recPage = ref(1);
const recSize = ref(20);
const recTotal = ref(0);
const recStatus = ref("ALL");
const recQ = ref("");
const detailOpen = ref(false);
const detailItem = ref<WithdrawRecord | null>(null);
const payOpen = ref(false);
const payItem = ref<PendingWithdraw | null>(null);
const payRemark = ref("");
const payProofUrl = ref("");
const paying = ref(false);

const rejectOpen = ref(false);
const rejectNo = ref("");
const rejectReason = ref("");
const autoPayLoading = ref<string | null>(null);

async function load() {
  try {
    loading.value = true;
    items.value = await api<PendingWithdraw[]>("/api/admin/withdraws/pending");
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("加载失败", e?.message || "");
  } finally {
    loading.value = false;
  }
}

async function refreshAll() {
  await Promise.all([load(), loadStats(), loadRecords(true)]);
}

async function loadStats() {
  try {
    stats.value = await api<WithdrawStats>("/api/admin/withdraws/stats");
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("统计加载失败", e?.message || "");
  }
}

async function loadRecords(reset = false) {
  if (reset) recPage.value = 1;
  try {
    recLoading.value = true;
    const p = await api<PageResp<WithdrawRecord>>(
      `/api/admin/withdraws/records?page=${recPage.value}&size=${recSize.value}&status=${encodeURIComponent(recStatus.value)}&q=${encodeURIComponent(recQ.value)}`
    );
    records.value = p.items || [];
    recTotal.value = Number(p.total || 0);
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("记录加载失败", e?.message || "");
  } finally {
    recLoading.value = false;
  }
}

function prevPage() {
  if (recPage.value <= 1) return;
  recPage.value -= 1;
  loadRecords(false);
}

function nextPage() {
  const maxPage = Math.max(1, Math.ceil(recTotal.value / recSize.value));
  if (recPage.value >= maxPage) return;
  recPage.value += 1;
  loadRecords(false);
}

function openDetail(item: WithdrawRecord) {
  detailItem.value = item;
  detailOpen.value = true;
}

function fileUrl(raw?: string) {
  const v = String(raw || "").trim();
  if (!v) return "";
  if (/^https?:\/\//i.test(v)) return v;
  if (v.startsWith("/")) return API_BASE_URL.replace(/\/$/, "") + v;
  return API_BASE_URL.replace(/\/$/, "") + "/" + v;
}

function openPay(item: PendingWithdraw) {
  payItem.value = item;
  payRemark.value = "";
  payProofUrl.value = "";
  payOpen.value = true;
}

async function onPickProof(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  try {
    const token = getToken();
    const fd = new FormData();
    fd.append("file", file);
    const res = await fetch(API_BASE_URL + "/api/common/upload", {
      method: "POST",
      headers: {
        ...(token ? { Authorization: "Bearer " + token } : {}),
      },
      body: fd,
    });
    const body = await res.json();
    if (!res.ok || !body?.ok) throw new Error(body?.message || `HTTP ${res.status}`);
    payProofUrl.value = String(body?.data?.url || "");
    toastOk("凭证上传成功");
  } catch (e: any) {
    toastErr("上传失败", e?.message || "");
  } finally {
    input.value = "";
  }
}

async function doPayComplete() {
  if (!payItem.value) return;
  if (!payProofUrl.value) {
    toastErr("请先上传付款凭证");
    return;
  }
  try {
    paying.value = true;
    await api<void>(`/api/admin/withdraws/${encodeURIComponent(payItem.value.applyNo)}/pay-complete`, {
      method: "POST",
      body: { paidProofUrl: payProofUrl.value, payRemark: payRemark.value },
    });
    toastOk("打款完成，已闭环");
    payOpen.value = false;
    payItem.value = null;
    await load();
    await loadStats();
    await loadRecords(true);
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("操作失败", e?.message || "");
  } finally {
    paying.value = false;
  }
}

async function doAutoPay(item: PendingWithdraw) {
  if (!confirm(`确认通过微信商家转账向用户打款 ${fmtMoney(item.amount)}？此操作将直接发起转账，请谨慎确认。`)) return;
  try {
    autoPayLoading.value = item.applyNo;
    await api<void>(`/api/admin/withdraws/${encodeURIComponent(item.applyNo)}/auto-pay`, { method: "POST" });
    toastOk("微信转账已发起，已闭环");
    await Promise.all([load(), loadStats(), loadRecords(true)]);
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("微信打款失败", e?.message || "");
  } finally {
    autoPayLoading.value = null;
  }
}

function openReject(applyNo: string) {
  rejectNo.value = applyNo;
  rejectReason.value = "";
  rejectOpen.value = true;
}

async function doReject() {
  try {
    await api<void>(`/api/admin/withdraws/${encodeURIComponent(rejectNo.value)}/audit`, {
      method: "POST",
      body: { result: "REJECT", reason: rejectReason.value },
    });
    toastOk("已驳回提现");
    rejectOpen.value = false;
    await load();
    await loadStats();
    await loadRecords(true);
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("操作失败", e?.message || "");
  }
}

onMounted(refreshAll);
</script>

<template>
  <AppCard>
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <div class="text-sm font-semibold text-slate-600">提现审核</div>
        <div class="mt-1 text-lg font-semibold text-slate-900">待打款列表</div>
      </div>
      <div class="flex items-center gap-2">
        <TagPill tone="warn" text="PENDING" />
        <button class="btn btn-ghost" :disabled="loading" @click="refreshAll"><RefreshCw class="h-4 w-4" /> 刷新</button>
      </div>
    </div>

    <div class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-3">
      <div class="rounded-2xl border border-slate-200/70 bg-white/70 p-4">
        <div class="text-xs font-semibold text-slate-600">待处理提现</div>
        <div class="mt-2 text-2xl font-semibold text-slate-900">{{ stats?.pendingCount ?? 0 }}</div>
        <div class="mt-1 text-xs text-slate-500">金额 {{ fmtMoney(stats?.pendingAmount || "0") }}</div>
      </div>
      <div class="rounded-2xl border border-slate-200/70 bg-white/70 p-4">
        <div class="text-xs font-semibold text-slate-600">累计成功打款</div>
        <div class="mt-2 text-2xl font-semibold text-slate-900">{{ stats?.paidCount ?? 0 }}</div>
        <div class="mt-1 text-xs text-slate-500">金额 {{ fmtMoney(stats?.paidAmount || "0") }}</div>
      </div>
      <div class="rounded-2xl border border-slate-200/70 bg-white/70 p-4">
        <div class="text-xs font-semibold text-slate-600">今日成功打款</div>
        <div class="mt-2 text-2xl font-semibold text-slate-900">{{ stats?.todayPaidCount ?? 0 }}</div>
        <div class="mt-1 text-xs text-slate-500">金额 {{ fmtMoney(stats?.todayPaidAmount || "0") }}</div>
      </div>
    </div>

    <div class="mt-4 overflow-x-auto rounded-2xl border border-slate-200/70 bg-white/60">
      <table class="w-full text-sm">
        <thead class="bg-white/70 text-left text-xs text-slate-600">
          <tr>
            <th class="px-4 py-3 font-semibold">申请号</th>
            <th class="px-4 py-3 font-semibold">用户</th>
            <th class="px-4 py-3 font-semibold">金额</th>
            <th class="px-4 py-3 font-semibold">渠道</th>
            <th class="px-4 py-3 font-semibold">时间</th>
            <th class="px-4 py-3 font-semibold">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!items.length">
            <td colspan="6" class="px-4 py-10 text-center text-slate-500">
              <span v-if="loading">加载中...</span>
              <span v-else>暂无待审提现</span>
            </td>
          </tr>
          <tr v-for="w in items" :key="w.applyNo" class="border-t border-slate-200/60">
            <td class="px-4 py-3 font-semibold text-slate-900">#{{ w.applyNo }}</td>
            <td class="px-4 py-3 text-slate-800">{{ w.userOpenId }}</td>
            <td class="px-4 py-3 font-semibold text-slate-900">{{ fmtMoney(w.amount) }}</td>
            <td class="px-4 py-3 text-slate-700">{{ w.channel }}</td>
            <td class="px-4 py-3 text-slate-700">{{ fmtIso(w.createdAt) }}</td>
            <td class="px-4 py-3">
              <div class="flex items-center gap-2">
                <button class="btn btn-ghost" @click="openReject(w.applyNo)"><X class="h-4 w-4" /> 驳回</button>
                <button class="btn btn-success" @click="openPay(w)"><Check class="h-4 w-4" /> 打款</button>
                <button class="btn btn-primary" :disabled="autoPayLoading === w.applyNo" @click="doAutoPay(w)">
                  <Wallet class="h-4 w-4" /> {{ autoPayLoading === w.applyNo ? '转账中...' : '微信打款' }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="mt-4 flex items-center gap-2 text-xs text-slate-500">
      <Wallet class="h-4 w-4" />
      一期建议：人工打款后在后台标记结果，避免自动微信零钱发放的合规复杂度
    </div>
  </AppCard>

  <AppCard class="mt-5">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <div class="text-sm font-semibold text-slate-600">提现记录</div>
        <div class="mt-1 text-lg font-semibold text-slate-900">可查看每笔提现详情与凭证</div>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <select v-model="recStatus" class="field !w-auto" @change="loadRecords(true)">
          <option value="ALL">全部</option>
          <option value="PENDING">待处理</option>
          <option value="PAID">已打款</option>
          <option value="REJECTED">已驳回</option>
        </select>
        <input v-model="recQ" class="field !w-52" placeholder="申请号/用户OpenId" @keyup.enter="loadRecords(true)" />
        <button class="btn btn-ghost" :disabled="recLoading" @click="loadRecords(true)">查询</button>
      </div>
    </div>

    <div class="mt-4 overflow-x-auto rounded-2xl border border-slate-200/70 bg-white/60">
      <table class="w-full text-sm">
        <thead class="bg-white/70 text-left text-xs text-slate-600">
          <tr>
            <th class="px-4 py-3 font-semibold">申请号</th>
            <th class="px-4 py-3 font-semibold">用户</th>
            <th class="px-4 py-3 font-semibold">金额</th>
            <th class="px-4 py-3 font-semibold">状态</th>
            <th class="px-4 py-3 font-semibold">申请时间</th>
            <th class="px-4 py-3 font-semibold">打款时间</th>
            <th class="px-4 py-3 font-semibold">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!records.length">
            <td colspan="7" class="px-4 py-10 text-center text-slate-500">
              <span v-if="recLoading">加载中...</span>
              <span v-else>暂无记录</span>
            </td>
          </tr>
          <tr v-for="w in records" :key="w.applyNo" class="border-t border-slate-200/60">
            <td class="px-4 py-3 font-semibold text-slate-900">#{{ w.applyNo }}</td>
            <td class="px-4 py-3 text-slate-800">{{ w.userOpenId }}</td>
            <td class="px-4 py-3 font-semibold text-slate-900">{{ fmtMoney(w.amount) }}</td>
            <td class="px-4 py-3 text-slate-700">{{ w.status }}</td>
            <td class="px-4 py-3 text-slate-700">{{ fmtIso(w.createdAt) }}</td>
            <td class="px-4 py-3 text-slate-700">{{ w.paidAt ? fmtIso(w.paidAt) : "-" }}</td>
            <td class="px-4 py-3">
              <button class="btn btn-ghost" @click="openDetail(w)">详情</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="mt-3 flex items-center justify-end gap-2 text-sm">
      <button class="btn btn-ghost" :disabled="recPage <= 1" @click="prevPage">上一页</button>
      <div class="px-2 text-slate-600">第 {{ recPage }} 页 / 共 {{ Math.max(1, Math.ceil(recTotal / recSize)) }} 页</div>
      <button class="btn btn-ghost" :disabled="recPage >= Math.max(1, Math.ceil(recTotal / recSize))" @click="nextPage">下一页</button>
    </div>
  </AppCard>

  <AppModal :open="rejectOpen" title="驳回提现申请" @close="rejectOpen = false">
    <div class="text-sm text-slate-700">申请号：<span class="font-semibold text-slate-900">{{ rejectNo }}</span></div>
    <div class="mt-3">
      <div class="text-xs font-semibold text-slate-600">原因（可选）</div>
      <textarea v-model="rejectReason" class="field mt-2 h-28 resize-none" placeholder="例如：收款信息不完整 / 异常频繁" />
    </div>
    <div class="mt-4 flex justify-end gap-2">
      <button class="btn btn-ghost" @click="rejectOpen = false">取消</button>
      <button class="btn btn-primary" @click="doReject">确认驳回</button>
    </div>
  </AppModal>

  <AppModal :open="payOpen" title="人工打款闭环" @close="payOpen = false">
    <div v-if="payItem" class="max-h-[52vh] space-y-3 overflow-y-auto pr-1 text-sm text-slate-700">
      <div>申请号：<span class="font-semibold text-slate-900">{{ payItem.applyNo }}</span></div>
      <div>用户：<span class="font-semibold text-slate-900">{{ payItem.userOpenId }}</span></div>
      <div>金额：<span class="font-semibold text-slate-900">{{ fmtMoney(payItem.amount) }}</span></div>

      <div>
        <div class="text-xs font-semibold text-slate-600">用户微信收款码</div>
        <div class="mt-2">
          <img
            v-if="payItem.qrCodeUrl"
            :src="fileUrl(payItem.qrCodeUrl)"
            class="h-48 w-48 rounded-xl border border-slate-200 object-cover"
            alt="qr"
          />
          <div v-else class="rounded-xl border border-dashed border-slate-300 p-4 text-xs text-slate-500">用户未上传收款码</div>
        </div>
      </div>

      <div>
        <div class="text-xs font-semibold text-slate-600">上传付款凭证</div>
        <div class="mt-2 flex items-center gap-2">
          <input type="file" accept="image/*" @change="onPickProof" />
        </div>
        <div v-if="payProofUrl" class="mt-2">
          <img :src="fileUrl(payProofUrl)" class="h-28 w-28 rounded-lg border border-slate-200 object-cover" alt="proof" />
        </div>
      </div>

      <div>
        <div class="text-xs font-semibold text-slate-600">备注（可选）</div>
        <textarea v-model="payRemark" class="field mt-2 h-20 resize-none" placeholder="例如：已人工转账，尾号1234" />
      </div>
    </div>

    <div class="mt-4 sticky bottom-0 flex justify-end gap-2 bg-white/95 pt-2">
      <button class="btn btn-ghost" @click="payOpen = false">取消</button>
      <button class="btn btn-primary" :disabled="paying" @click="doPayComplete">
        {{ paying ? "提交中..." : "完成闭环" }}
      </button>
    </div>
  </AppModal>

  <AppModal :open="detailOpen" title="提现详情" @close="detailOpen = false">
    <div v-if="detailItem" class="space-y-2 text-sm text-slate-700">
      <div>申请号：<span class="font-semibold text-slate-900">{{ detailItem.applyNo }}</span></div>
      <div>用户：<span class="font-semibold text-slate-900">{{ detailItem.userOpenId }}</span></div>
      <div>金额：<span class="font-semibold text-slate-900">{{ fmtMoney(detailItem.amount) }}</span></div>
      <div>状态：<span class="font-semibold text-slate-900">{{ detailItem.status }}</span></div>
      <div>申请时间：<span class="font-semibold text-slate-900">{{ fmtIso(detailItem.createdAt) }}</span></div>
      <div>打款时间：<span class="font-semibold text-slate-900">{{ detailItem.paidAt ? fmtIso(detailItem.paidAt) : "-" }}</span></div>
      <div>备注：<span class="font-semibold text-slate-900">{{ detailItem.payRemark || "-" }}</span></div>
      <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
        <div>
          <div class="mb-1 text-xs font-semibold text-slate-600">用户收款码</div>
          <img v-if="detailItem.qrCodeUrl" :src="fileUrl(detailItem.qrCodeUrl)" class="h-40 w-40 rounded-lg border border-slate-200 object-cover" alt="qr" />
          <div v-else class="text-xs text-slate-400">无</div>
        </div>
        <div>
          <div class="mb-1 text-xs font-semibold text-slate-600">付款凭证</div>
          <img v-if="detailItem.paidProofUrl" :src="fileUrl(detailItem.paidProofUrl)" class="h-40 w-40 rounded-lg border border-slate-200 object-cover" alt="proof" />
          <div v-else class="text-xs text-slate-400">无</div>
        </div>
      </div>
    </div>
    <div class="mt-4 flex justify-end gap-2">
      <button class="btn btn-primary" @click="detailOpen = false">关闭</button>
    </div>
  </AppModal>
</template>
