<script setup lang="ts">
import { Ban, Check, Coins, Pencil, RefreshCw, Search, UserRound } from "lucide-vue-next";
import { onMounted, ref } from "vue";
import AppCard from "../components/AppCard.vue";
import AppModal from "../components/AppModal.vue";
import TagPill from "../components/TagPill.vue";
import { useAuthFailureHandler } from "../lib/auth";
import { api } from "../lib/api";
import { fmtIso, fmtMoney } from "../lib/format";
import { toastErr, toastOk } from "../lib/toast";

type UserItem = {
  id: number;
  openId: string;
  nickname: string | null;
  status: string;
  creditScore: number;
  gender?: string;
  balance: string;
  frozen: string;
  createdAt: string;
};
type PageResp<T> = { page: number; size: number; total: number; items: T[] };

const onAuthFail = useAuthFailureHandler();
const loading = ref(false);
const items = ref<UserItem[]>([]);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const filters = ref({
  q: "",
  id: "",
  status: "",
  gender: "",
  minCredit: "",
  maxCredit: "",
  balanceState: "",
});

const creditOpen = ref(false);
const creditUser = ref<UserItem | null>(null);
const creditValue = ref(100);

const topupOpen = ref(false);
const topupUser = ref<UserItem | null>(null);
const topupAmount = ref("10");

async function load() {
  try {
    loading.value = true;
    const params = new URLSearchParams();
    Object.entries(filters.value).forEach(([key, value]) => {
      if (String(value || "").trim()) params.set(key, String(value).trim());
    });
    params.set("page", String(page.value));
    params.set("size", String(size.value));
    const data = await api<PageResp<UserItem>>(`/api/admin/users?${params.toString()}`);
    items.value = data.items || [];
    total.value = Number(data.total || 0);
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("加载失败", e?.message || "");
  } finally {
    loading.value = false;
  }
}

function applySearch() {
  page.value = 1;
  void load();
}

function prev() {
  if (page.value <= 1) return;
  page.value -= 1;
  void load();
}

function next() {
  const max = Math.max(1, Math.ceil(total.value / size.value));
  if (page.value >= max) return;
  page.value += 1;
  void load();
}

function resetFilters() {
  filters.value = { q: "", id: "", status: "", gender: "", minCredit: "", maxCredit: "", balanceState: "" };
  page.value = 1;
  void load();
}

async function ban(u: UserItem) {
  try {
    await api<void>(`/api/admin/users/${u.id}/ban`, { method: "POST" });
    toastOk("已封禁用户");
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("操作失败", e?.message || "");
  }
}

async function unban(u: UserItem) {
  try {
    await api<void>(`/api/admin/users/${u.id}/unban`, { method: "POST" });
    toastOk("已解封用户");
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("操作失败", e?.message || "");
  }
}

function openCredit(u: UserItem) {
  creditUser.value = u;
  creditValue.value = u.creditScore;
  creditOpen.value = true;
}

async function saveCredit() {
  if (!creditUser.value) return;
  try {
    await api<void>(`/api/admin/users/${creditUser.value.id}/credit`, {
      method: "POST",
      body: { creditScore: creditValue.value },
    });
    toastOk("已更新信用分");
    creditOpen.value = false;
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("操作失败", e?.message || "");
  }
}

function openTopup(u: UserItem) {
  topupUser.value = u;
  topupAmount.value = "10";
  topupOpen.value = true;
}

async function doTopup() {
  if (!topupUser.value) return;
  try {
    await api<void>(`/api/admin/users/${topupUser.value.id}/wallet/credit`, {
      method: "POST",
      body: { amount: Number(topupAmount.value) },
    });
    toastOk("已调账入余额");
    topupOpen.value = false;
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
        <div class="text-sm font-semibold text-slate-600">用户管理</div>
        <div class="mt-1 text-lg font-semibold text-slate-900">用户查询</div>
      </div>
      <div class="flex items-center gap-2">
        <TagPill tone="info" :text="'总数 ' + total" />
        <button class="btn btn-ghost" :disabled="loading" @click="load"><RefreshCw class="h-4 w-4" /> 刷新</button>
      </div>
    </div>

    <div class="mt-4 grid grid-cols-1 gap-3 rounded-2xl border border-slate-200/70 bg-white/60 p-4 md:grid-cols-4">
      <input v-model="filters.q" class="field" placeholder="关键词：昵称 / openId / 签名" @keyup.enter="applySearch" />
      <input v-model="filters.id" class="field" type="number" placeholder="用户ID" @keyup.enter="applySearch" />
      <select v-model="filters.status" class="field">
        <option value="">全部状态</option>
        <option value="ACTIVE">ACTIVE</option>
        <option value="BANNED">BANNED</option>
      </select>
      <select v-model="filters.gender" class="field">
        <option value="">全部性别</option>
        <option value="MALE">男生</option>
        <option value="FEMALE">女生</option>
        <option value="UNKNOWN">未知</option>
      </select>
      <input v-model="filters.minCredit" class="field" type="number" placeholder="最低信用分" @keyup.enter="applySearch" />
      <input v-model="filters.maxCredit" class="field" type="number" placeholder="最高信用分" @keyup.enter="applySearch" />
      <select v-model="filters.balanceState" class="field">
        <option value="">全部余额</option>
        <option value="HAS_BALANCE">有可用余额</option>
        <option value="FROZEN">有冻结余额</option>
        <option value="ZERO">余额为零</option>
      </select>
      <div class="flex gap-2">
        <button class="btn btn-primary flex-1" :disabled="loading" @click="applySearch"><Search class="h-4 w-4" /> 查询</button>
        <button class="btn btn-ghost" @click="resetFilters">重置</button>
      </div>
    </div>

    <div class="mt-4 overflow-hidden rounded-2xl border border-slate-200/70 bg-white/60">
      <table class="w-full text-sm">
        <thead class="bg-white/70 text-left text-xs text-slate-600">
          <tr>
            <th class="px-4 py-3 font-semibold">用户</th>
            <th class="px-4 py-3 font-semibold">状态</th>
            <th class="px-4 py-3 font-semibold">信用分</th>
            <th class="px-4 py-3 font-semibold">余额/冻结</th>
            <th class="px-4 py-3 font-semibold">注册时间</th>
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
          <tr v-for="u in items" :key="u.id" class="border-t border-slate-200/60">
            <td class="px-4 py-3">
              <div class="flex items-center gap-3">
                <div class="grid h-9 w-9 place-items-center rounded-2xl bg-slate-900 text-white">
                  <UserRound class="h-4 w-4" />
                </div>
                <div>
                  <div class="font-semibold text-slate-900">{{ u.nickname || "-" }}</div>
                  <div class="mt-0.5 text-xs text-slate-500">#{{ u.id }} · {{ u.openId }} · {{ u.gender || "UNKNOWN" }}</div>
                </div>
              </div>
            </td>
            <td class="px-4 py-3">
              <TagPill :tone="u.status === 'ACTIVE' ? 'ok' : 'err'" :text="u.status" />
            </td>
            <td class="px-4 py-3 font-semibold text-slate-900">{{ u.creditScore }}</td>
            <td class="px-4 py-3 text-slate-800">
              <div class="font-semibold">{{ fmtMoney(u.balance) }}</div>
              <div class="text-xs text-slate-500">冻结 {{ fmtMoney(u.frozen) }}</div>
            </td>
            <td class="px-4 py-3 text-xs text-slate-600">{{ fmtIso(u.createdAt) }}</td>
            <td class="px-4 py-3">
              <div class="flex flex-wrap items-center gap-2">
                <button class="btn btn-ghost" @click="openCredit(u)"><Pencil class="h-4 w-4" /> 信用分</button>
                <button class="btn btn-ghost" @click="openTopup(u)"><Coins class="h-4 w-4" /> 入账</button>
                <button v-if="u.status === 'ACTIVE'" class="btn btn-ghost" @click="ban(u)"><Ban class="h-4 w-4" /> 封禁</button>
                <button v-else class="btn btn-success" @click="unban(u)"><Check class="h-4 w-4" /> 解封</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="mt-4 text-xs text-slate-500">
      “入账”用于开发演示（模拟奖励/充值）；生产环境建议走正规充值/支付与资金对账。
    </div>
    <div class="mt-4 flex items-center justify-between text-sm text-slate-600">
      <span>第 {{ page }} 页 / 共 {{ Math.max(1, Math.ceil(total / size)) }} 页</span>
      <div class="flex gap-2">
        <button class="btn btn-ghost" :disabled="page <= 1" @click="prev">上一页</button>
        <button class="btn btn-ghost" :disabled="page >= Math.max(1, Math.ceil(total / size))" @click="next">下一页</button>
      </div>
    </div>
  </AppCard>

  <AppModal :open="creditOpen" title="调整信用分" @close="creditOpen = false">
    <div class="text-sm text-slate-700">
      用户：<span class="font-semibold text-slate-900">{{ creditUser?.openId }}</span>
    </div>
    <div class="mt-3">
      <div class="text-xs font-semibold text-slate-600">信用分</div>
      <input v-model.number="creditValue" class="field mt-2" type="number" min="0" />
    </div>
    <div class="mt-4 flex justify-end gap-2">
      <button class="btn btn-ghost" @click="creditOpen = false">取消</button>
      <button class="btn btn-primary" @click="saveCredit">保存</button>
    </div>
  </AppModal>

  <AppModal :open="topupOpen" title="钱包入账（开发演示）" @close="topupOpen = false">
    <div class="text-sm text-slate-700">
      用户：<span class="font-semibold text-slate-900">{{ topupUser?.openId }}</span>
    </div>
    <div class="mt-3">
      <div class="text-xs font-semibold text-slate-600">金额</div>
      <input v-model="topupAmount" class="field mt-2" type="number" />
    </div>
    <div class="mt-4 flex justify-end gap-2">
      <button class="btn btn-ghost" @click="topupOpen = false">取消</button>
      <button class="btn btn-primary" @click="doTopup">确认入账</button>
    </div>
  </AppModal>
</template>
