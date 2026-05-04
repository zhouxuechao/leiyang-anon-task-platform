<script setup lang="ts">
import { PieChart, RefreshCw } from "lucide-vue-next";
import { computed, onMounted, ref } from "vue";
import AppCard from "../components/AppCard.vue";
import TagPill from "../components/TagPill.vue";
import { useAuthFailureHandler } from "../lib/auth";
import { api } from "../lib/api";
import { toastErr } from "../lib/toast";

type Overview = {
  usersTotal: number;
  tasksTotal: number;
  tasksPendingAudit: number;
  ordersSubmitted: number;
  withdrawPending: number;
  reportsPending: number;
  todayNewUsers: number;
  todayTasksPublished: number;
  todayOrdersSettled: number;
  todayWithdrawApply: number;
};

type DailyItem = {
  date: string;
  newUsers: number;
  tasksPublished: number;
  ordersAccepted: number;
  ordersSubmitted: number;
  ordersSettled: number;
  withdrawApplied: number;
  reportsCreated: number;
};

type UserGrowth = {
  range: string;
  newUsers: number;
};

const onAuthFail = useAuthFailureHandler();
const loading = ref(false);
const overview = ref<Overview | null>(null);
const daily = ref<DailyItem[]>([]);
const userGrowth = ref<UserGrowth[]>([]);

const metric = ref<keyof DailyItem>("ordersSettled");

const metricLabel: Record<string, string> = {
  newUsers: "日增用户",
  tasksPublished: "上架任务",
  ordersAccepted: "接单",
  ordersSubmitted: "提交",
  ordersSettled: "结算(发奖)",
  withdrawApplied: "提现申请",
  reportsCreated: "举报",
};

const bars = computed(() => {
  const key = metric.value;
  const arr = daily.value.map((d) => ({ date: d.date, v: Number((d as any)[key] || 0) }));
  const max = Math.max(1, ...arr.map((x) => x.v));
  return arr.map((x) => ({ ...x, h: Math.round((x.v / max) * 100) }));
});

async function load() {
  try {
    loading.value = true;
    const [o, d, g] = await Promise.all([
      api<Overview>("/api/admin/stats/overview"),
      api<DailyItem[]>("/api/admin/stats/daily?days=14"),
      api<UserGrowth[]>("/api/admin/stats/user-growth"),
    ]);
    overview.value = o;
    daily.value = d;
    userGrowth.value = g;
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("加载失败", e?.message || "");
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="grid gap-5">
    <AppCard>
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div class="text-sm font-semibold text-slate-600">统计报表</div>
          <div class="mt-1 text-lg font-semibold text-slate-900">关键指标概览</div>
        </div>
        <div class="flex items-center gap-2">
          <TagPill tone="info" text="UTC" />
          <button class="btn btn-ghost" :disabled="loading" @click="load"><RefreshCw class="h-4 w-4" /> 刷新</button>
        </div>
      </div>

      <div class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-3">
        <div class="glass rounded-3xl p-4 shadow-soft">
          <div class="text-xs font-semibold text-slate-600">总用户</div>
          <div class="mt-2 text-3xl font-[760] text-slate-900" style="font-family: 'Space Grotesk', sans-serif">
            {{ overview?.usersTotal ?? "-" }}
          </div>
          <div class="mt-1 text-xs text-slate-500">全部注册用户</div>
        </div>
        <div class="glass rounded-3xl p-4 shadow-soft">
          <div class="text-xs font-semibold text-slate-600">总任务</div>
          <div class="mt-2 text-3xl font-[760] text-slate-900" style="font-family: 'Space Grotesk', sans-serif">
            {{ overview?.tasksTotal ?? "-" }}
          </div>
          <div class="mt-1 text-xs text-slate-500">发布累计</div>
        </div>
        <div class="glass rounded-3xl p-4 shadow-soft">
          <div class="text-xs font-semibold text-slate-600">待审任务</div>
          <div class="mt-2 text-3xl font-[760] text-slate-900" style="font-family: 'Space Grotesk', sans-serif">
            {{ overview?.tasksPendingAudit ?? "-" }}
          </div>
          <div class="mt-1 text-xs text-slate-500">PENDING_AUDIT</div>
        </div>
      </div>

      <div class="mt-3 grid grid-cols-1 gap-3 md:grid-cols-3">
        <div class="glass rounded-3xl p-4 shadow-soft">
          <div class="text-xs font-semibold text-slate-600">今日新增用户</div>
          <div class="mt-2 text-3xl font-[760] text-slate-900" style="font-family: 'Space Grotesk', sans-serif">
            {{ overview?.todayNewUsers ?? "-" }}
          </div>
          <div class="mt-1 text-xs text-slate-500">今日注册</div>
        </div>
        <div class="glass rounded-3xl p-4 shadow-soft">
          <div class="text-xs font-semibold text-slate-600">待审凭证</div>
          <div class="mt-2 text-3xl font-[760] text-slate-900" style="font-family: 'Space Grotesk', sans-serif">
            {{ overview?.ordersSubmitted ?? "-" }}
          </div>
          <div class="mt-1 text-xs text-slate-500">SUBMITTED</div>
        </div>
        <div class="glass rounded-3xl p-4 shadow-soft">
          <div class="text-xs font-semibold text-slate-600">待审提现</div>
          <div class="mt-2 text-3xl font-[760] text-slate-900" style="font-family: 'Space Grotesk', sans-serif">
            {{ overview?.withdrawPending ?? "-" }}
          </div>
          <div class="mt-1 text-xs text-slate-500">PENDING</div>
        </div>
        <div class="glass rounded-3xl p-4 shadow-soft">
          <div class="text-xs font-semibold text-slate-600">待处理举报</div>
          <div class="mt-2 text-3xl font-[760] text-slate-900" style="font-family: 'Space Grotesk', sans-serif">
            {{ overview?.reportsPending ?? "-" }}
          </div>
          <div class="mt-1 text-xs text-slate-500">PENDING</div>
        </div>
      </div>

      <div class="mt-4 flex items-center gap-2 text-xs text-slate-500">
        <PieChart class="h-4 w-4" />
        今日指标：新增用户 {{ overview?.todayNewUsers ?? 0 }} · 上架 {{ overview?.todayTasksPublished ?? 0 }} · 结算 {{ overview?.todayOrdersSettled ?? 0 }} · 提现申请
        {{ overview?.todayWithdrawApply ?? 0 }}
      </div>
    </AppCard>

    <AppCard>
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div class="text-sm font-semibold text-slate-600">用户增长</div>
          <div class="mt-1 text-lg font-semibold text-slate-900">日增用户报表</div>
        </div>
      </div>
      <div class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-3">
        <div v-for="g in userGrowth" :key="g.range" class="glass rounded-3xl p-4 shadow-soft">
          <div class="text-xs font-semibold text-slate-600">{{ g.range }}</div>
          <div class="mt-2 text-3xl font-[760] text-slate-900" style="font-family: 'Space Grotesk', sans-serif">
            {{ g.newUsers }}
          </div>
          <div class="mt-1 text-xs text-slate-500">新增注册用户</div>
        </div>
      </div>
    </AppCard>

    <AppCard>
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div class="text-sm font-semibold text-slate-600">趋势</div>
          <div class="mt-1 text-lg font-semibold text-slate-900">近 14 天</div>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <div class="text-xs font-semibold text-slate-600">指标</div>
          <select v-model="metric" class="field !w-auto">
            <option value="ordersSettled">结算(发奖)</option>
            <option value="newUsers">日增用户</option>
            <option value="ordersSubmitted">提交</option>
            <option value="ordersAccepted">接单</option>
            <option value="tasksPublished">上架任务</option>
            <option value="withdrawApplied">提现申请</option>
            <option value="reportsCreated">举报</option>
          </select>
        </div>
      </div>

      <div class="mt-4 rounded-3xl border border-slate-200/70 bg-white/60 p-4">
        <div class="mb-3 text-xs font-semibold text-slate-600">{{ metricLabel[metric] }}</div>
        <div class="flex items-end gap-2">
          <div
            v-for="b in bars"
            :key="b.date"
            class="group flex-1"
            :title="b.date + ' · ' + b.v"
          >
            <div
              class="w-full rounded-xl bg-gradient-to-b from-cyan-500 to-emerald-500 transition"
              :style="{ height: Math.max(6, b.h) + 'px' }"
            />
            <div class="mt-2 truncate text-center text-[10px] text-slate-500">
              {{ b.date.slice(5) }}
            </div>
          </div>
        </div>
      </div>
    </AppCard>
  </div>
</template>
