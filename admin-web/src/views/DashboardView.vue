<script setup lang="ts">
import { BadgeCheck, FileSearch, Flag, ShieldAlert, Wallet } from "lucide-vue-next";
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import AppCard from "../components/AppCard.vue";
import TagPill from "../components/TagPill.vue";
import { api } from "../lib/api";
import { fmtIso, fmtMoney } from "../lib/format";
import { useAuthFailureHandler } from "../lib/auth";
import { toastErr, toastOk } from "../lib/toast";

type PendingTask = { taskNo: string; title: string; amount: string; deadlineAt: string; publisherOpenId: string };
type SubmittedOrder = { orderNo: string; taskNo: string; taskTitle: string; userOpenId: string; submitTime: string | null };
type PendingWithdraw = { applyNo: string; userOpenId: string; amount: string; channel: string; status: string; createdAt: string };
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

const router = useRouter();
const onAuthFail = useAuthFailureHandler();

const loading = ref(false);
const tasks = ref<PendingTask[]>([]);
const orders = ref<SubmittedOrder[]>([]);
const withdraws = ref<PendingWithdraw[]>([]);
const reports = ref<PendingReport[]>([]);

async function load() {
  try {
    loading.value = true;
    const [t, o, w, r] = await Promise.all([
      api<PageResp<PendingTask>>("/api/admin/tasks/pending?page=1&size=20"),
      api<PageResp<SubmittedOrder>>("/api/admin/orders/submitted?page=1&size=20"),
      api<PendingWithdraw[]>("/api/admin/withdraws/pending"),
      api<PageResp<PendingReport>>("/api/admin/reports/pending?page=1&size=20"),
    ]);
    tasks.value = t.items;
    orders.value = o.items;
    withdraws.value = w;
    reports.value = r.items;
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("加载失败", e?.message || "请检查后端是否启动");
  } finally {
    loading.value = false;
  }
}

async function quickResolveReport(reportNo: string, result: "RESOLVE" | "REJECT") {
  try {
    await api<void>(`/api/admin/reports/${encodeURIComponent(reportNo)}/resolve`, { method: "POST", body: { result } });
    toastOk("已处理举报");
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("操作失败", e?.message || "");
  }
}

onMounted(load);
</script>

<template>
  <div class="grid gap-5">
    <div class="grid grid-cols-1 gap-4 md:grid-cols-4">
      <button class="glass rounded-3xl p-4 text-left shadow-soft transition hover:-translate-y-0.5 hover:shadow-lift" @click="router.push('/tasks')">
        <div class="flex items-center justify-between">
          <div class="text-sm font-semibold text-slate-600">待审任务</div>
          <FileSearch class="h-4 w-4 text-slate-500" />
        </div>
        <div class="mt-2 text-3xl font-[760] text-slate-900" style="font-family: 'Space Grotesk', sans-serif">
          {{ tasks.length }}
        </div>
        <div class="mt-1 text-xs text-slate-500">发布审核</div>
      </button>

      <button class="glass rounded-3xl p-4 text-left shadow-soft transition hover:-translate-y-0.5 hover:shadow-lift" @click="router.push('/orders')">
        <div class="flex items-center justify-between">
          <div class="text-sm font-semibold text-slate-600">待审凭证</div>
          <BadgeCheck class="h-4 w-4 text-slate-500" />
        </div>
        <div class="mt-2 text-3xl font-[760] text-slate-900" style="font-family: 'Space Grotesk', sans-serif">
          {{ orders.length }}
        </div>
        <div class="mt-1 text-xs text-slate-500">提交审核</div>
      </button>

      <button class="glass rounded-3xl p-4 text-left shadow-soft transition hover:-translate-y-0.5 hover:shadow-lift" @click="router.push('/withdraws')">
        <div class="flex items-center justify-between">
          <div class="text-sm font-semibold text-slate-600">待审提现</div>
          <Wallet class="h-4 w-4 text-slate-500" />
        </div>
        <div class="mt-2 text-3xl font-[760] text-slate-900" style="font-family: 'Space Grotesk', sans-serif">
          {{ withdraws.length }}
        </div>
        <div class="mt-1 text-xs text-slate-500">人工打款</div>
      </button>

      <button class="glass rounded-3xl p-4 text-left shadow-soft transition hover:-translate-y-0.5 hover:shadow-lift" @click="router.push('/reports')">
        <div class="flex items-center justify-between">
          <div class="text-sm font-semibold text-slate-600">待处理举报</div>
          <Flag class="h-4 w-4 text-slate-500" />
        </div>
        <div class="mt-2 text-3xl font-[760] text-slate-900" style="font-family: 'Space Grotesk', sans-serif">
          {{ reports.length }}
        </div>
        <div class="mt-1 text-xs text-slate-500">平台治理</div>
      </button>
    </div>

    <div class="grid grid-cols-1 gap-5 lg:grid-cols-2">
      <AppCard>
        <div class="flex items-center justify-between">
          <div>
            <div class="text-sm font-semibold text-slate-600">任务发布审核</div>
            <div class="mt-1 text-lg font-semibold text-slate-900">最新待审</div>
          </div>
          <TagPill tone="info" text="PENDING_AUDIT" />
        </div>

        <div class="mt-4 overflow-x-auto rounded-2xl border border-slate-200/70 bg-white/60">
          <table class="w-full text-sm">
            <thead class="bg-white/70 text-left text-xs text-slate-600">
              <tr>
                <th class="px-4 py-3 font-semibold">标题</th>
                <th class="px-4 py-3 font-semibold">金额</th>
                <th class="px-4 py-3 font-semibold">截止</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!tasks.length">
                <td colspan="3" class="px-4 py-6 text-center text-slate-500">
                  <span v-if="loading">加载中...</span>
                  <span v-else>暂无待审任务</span>
                </td>
              </tr>
              <tr v-for="t in tasks.slice(0, 6)" :key="t.taskNo" class="border-t border-slate-200/60">
                <td class="px-4 py-3">
                  <div class="font-semibold text-slate-900">{{ t.title }}</div>
                  <div class="mt-0.5 text-xs text-slate-500">#{{ t.taskNo }} · {{ t.publisherOpenId }}</div>
                </td>
                <td class="px-4 py-3 font-semibold text-slate-900">{{ fmtMoney(t.amount) }}</td>
                <td class="px-4 py-3 text-slate-700">{{ fmtIso(t.deadlineAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mt-4 flex items-center justify-end">
          <button class="btn btn-ghost" @click="router.push('/tasks')">
            <FileSearch class="h-4 w-4" /> 去审核
          </button>
        </div>
      </AppCard>

      <AppCard>
        <div class="flex items-center justify-between">
          <div>
            <div class="text-sm font-semibold text-slate-600">举报处理</div>
            <div class="mt-1 text-lg font-semibold text-slate-900">最新待处理</div>
          </div>
          <TagPill tone="warn" text="PENDING" />
        </div>

        <div class="mt-4 grid gap-3">
          <div v-if="loading" class="rounded-2xl border border-slate-200/70 bg-white/60 px-4 py-5 text-sm text-slate-600">
            加载中...
          </div>
          <div
            v-for="r in reports.slice(0, 4)"
            :key="r.reportNo"
            class="rounded-2xl border border-slate-200/70 bg-white/60 p-4"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <div class="text-xs font-semibold text-slate-600">#{{ r.reportNo }} · {{ r.reporterOpenId }}</div>
                <div class="mt-1 font-semibold text-slate-900">
                  {{ r.targetType }}: {{ r.targetId }}
                </div>
                <div class="mt-2 text-sm text-slate-700">{{ r.reason }}</div>
              </div>
              <div class="flex shrink-0 gap-2">
                <button class="btn btn-ghost" @click="quickResolveReport(r.reportNo, 'REJECT')">驳回</button>
                <button class="btn btn-success" @click="quickResolveReport(r.reportNo, 'RESOLVE')">处理</button>
              </div>
            </div>
          </div>

          <div v-if="!loading && !reports.length" class="rounded-2xl border border-slate-200/70 bg-white/60 px-4 py-6 text-center text-sm text-slate-500">
            暂无待处理举报
          </div>
        </div>

        <div class="mt-4 flex items-center justify-between">
          <div class="inline-flex items-center gap-2 text-xs text-slate-500">
            <ShieldAlert class="h-4 w-4" />
            审核/处理结果建议留痕
          </div>
          <button class="btn btn-ghost" @click="router.push('/reports')">
            <Flag class="h-4 w-4" /> 查看全部
          </button>
        </div>
      </AppCard>
    </div>
  </div>
</template>
