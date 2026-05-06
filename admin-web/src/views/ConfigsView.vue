<script setup lang="ts">
import { ImagePlus, Pencil, Plus, RefreshCw, Trash2, ToggleLeft, ToggleRight } from "lucide-vue-next";
import { onMounted, ref } from "vue";
import AppCard from "../components/AppCard.vue";
import AppModal from "../components/AppModal.vue";
import TagPill from "../components/TagPill.vue";
import { useAuthFailureHandler } from "../lib/auth";
import { api, upload } from "../lib/api";
import { fmtIso } from "../lib/format";
import { toastErr, toastOk } from "../lib/toast";

type ConfigItem = { id: number; key: string; value: string; remark?: string | null; updatedAt: string };
type PageResp<T> = { page: number; size: number; total: number; items: T[] };
type WxPayStatus = { sdkEnabled: boolean; rechargeEnabled: boolean };

const onAuthFail = useAuthFailureHandler();
const loading = ref(false);
const wxPayStatus = ref<WxPayStatus | null>(null);
const wxPaySwitchLoading = ref(false);
const items = ref<ConfigItem[]>([]);
const page = ref(1);
const size = ref(20);
const total = ref(0);

const modalOpen = ref(false);
const editing = ref(false);
const cfgKey = ref("limits.maxOngoingOrders");
const cfgValue = ref("3");
const cfgRemark = ref("Max ongoing orders per user");

async function loadWxPayStatus() {
  try {
    wxPayStatus.value = await api<WxPayStatus>("/api/admin/configs/wxpay-status");
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
  }
}

async function toggleWxPayRecharge() {
  if (!wxPayStatus.value) return;
  const newVal = !wxPayStatus.value.rechargeEnabled;
  try {
    wxPaySwitchLoading.value = true;
    await api<void>("/api/admin/configs/wxpay-recharge-switch", { method: "POST", body: { enabled: newVal } });
    wxPayStatus.value = { ...wxPayStatus.value, rechargeEnabled: newVal };
    toastOk(newVal ? "微信支付充值已开启" : "微信支付充值已关闭");
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("操作失败", (e as any)?.message || "");
  } finally {
    wxPaySwitchLoading.value = false;
  }
}

async function load() {
  try {
    loading.value = true;
    const data = await api<PageResp<ConfigItem>>(`/api/admin/configs?page=${page.value}&size=${size.value}&excludeAiProvider=true`);
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

function openCreate() {
  editing.value = false;
  cfgKey.value = "limits.maxOngoingOrders";
  cfgValue.value = "3";
  cfgRemark.value = "";
  modalOpen.value = true;
}

function openEdit(row: ConfigItem) {
  editing.value = true;
  cfgKey.value = row.key;
  cfgValue.value = row.value || "";
  cfgRemark.value = row.remark || "";
  modalOpen.value = true;
}

async function upsert() {
  try {
    await api<void>("/api/admin/configs", {
      method: "POST",
      body: { key: cfgKey.value, value: cfgValue.value, remark: cfgRemark.value },
    });
    toastOk("已保存配置");
    modalOpen.value = false;
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("保存失败", e?.message || "");
  }
}

async function uploadRechargeQr(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  try {
    cfgKey.value = "recharge.wechat.qr_url";
    cfgValue.value = await upload(file);
    cfgRemark.value = "平台微信收款码图片URL";
    toastOk("收款码已上传，请保存配置");
  } catch (err: any) {
    if (err?.message === "AUTH") return onAuthFail();
    toastErr("上传失败", err?.message || "");
  } finally {
    (e.target as HTMLInputElement).value = "";
  }
}

async function del(id: number) {
  try {
    await api<void>(`/api/admin/configs/${id}/delete`, { method: "POST" });
    toastOk("已删除");
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("删除失败", e?.message || "");
  }
}

onMounted(() => { load(); loadWxPayStatus(); });
</script>

<template>
  <AppCard class="mb-5">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <div class="text-sm font-semibold text-slate-600">微信支付</div>
        <div class="mt-1 text-lg font-semibold text-slate-900">充值通道开关</div>
      </div>
      <button class="btn btn-ghost" @click="loadWxPayStatus"><RefreshCw class="h-4 w-4" /> 刷新</button>
    </div>
    <div class="mt-4 flex flex-wrap items-center gap-6">
      <div class="flex items-center gap-3">
        <span class="text-sm text-slate-700">SDK 状态（env WXPAY_ENABLED）</span>
        <TagPill :tone="wxPayStatus?.sdkEnabled ? 'ok' : 'warn'" :text="wxPayStatus?.sdkEnabled ? '已启用' : '未启用'" />
      </div>
      <div class="flex items-center gap-3">
        <span class="text-sm text-slate-700">Native 扫码充值开关</span>
        <TagPill :tone="wxPayStatus?.rechargeEnabled ? 'ok' : 'neutral'" :text="wxPayStatus?.rechargeEnabled ? '开启中' : '关闭中'" />
        <button
          class="btn"
          :class="wxPayStatus?.rechargeEnabled ? 'btn-ghost' : 'btn-primary'"
          :disabled="wxPaySwitchLoading || !wxPayStatus?.sdkEnabled"
          @click="toggleWxPayRecharge"
        >
          <component :is="wxPayStatus?.rechargeEnabled ? ToggleRight : ToggleLeft" class="h-4 w-4" />
          {{ wxPayStatus?.rechargeEnabled ? '关闭' : '开启' }}
        </button>
        <span v-if="!wxPayStatus?.sdkEnabled" class="text-xs text-slate-500">（需先在环境变量开启 SDK）</span>
      </div>
    </div>
  </AppCard>

  <AppCard>
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <div class="text-sm font-semibold text-slate-600">配置中心</div>
        <div class="mt-1 text-lg font-semibold text-slate-900">键值配置（开发版）</div>
      </div>
      <div class="flex items-center gap-2">
        <TagPill tone="info" :text="total + ' 项'" />
        <button class="btn btn-ghost" :disabled="loading" @click="load"><RefreshCw class="h-4 w-4" /> 刷新</button>
        <button class="btn btn-primary" @click="openCreate"><Plus class="h-4 w-4" /> 新增</button>
      </div>
    </div>

    <div class="mt-4 overflow-x-auto rounded-2xl border border-slate-200/70 bg-white/60">
      <table class="w-full text-sm">
        <thead class="bg-white/70 text-left text-xs text-slate-600">
          <tr>
            <th class="px-4 py-3 font-semibold">Key</th>
            <th class="px-4 py-3 font-semibold">Value</th>
            <th class="px-4 py-3 font-semibold">Remark</th>
            <th class="px-4 py-3 font-semibold">Updated</th>
            <th class="px-4 py-3 font-semibold">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!items.length">
            <td colspan="5" class="px-4 py-10 text-center text-slate-500">
              <span v-if="loading">加载中...</span>
              <span v-else>暂无数据</span>
            </td>
          </tr>
          <tr v-for="c in items" :key="c.id" class="border-t border-slate-200/60">
            <td class="px-4 py-3 font-semibold text-slate-900">{{ c.key }}</td>
            <td class="px-4 py-3 text-slate-800">{{ c.value }}</td>
            <td class="px-4 py-3 text-slate-700">{{ c.remark || "-" }}</td>
            <td class="px-4 py-3 text-slate-600">{{ fmtIso(c.updatedAt) }}</td>
            <td class="px-4 py-3">
              <div class="flex gap-2">
                <button class="btn btn-ghost" @click="openEdit(c)"><Pencil class="h-4 w-4" /> 编辑</button>
                <button class="btn btn-ghost" @click="del(c.id)"><Trash2 class="h-4 w-4" /> 删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="mt-4 flex items-center justify-between text-sm text-slate-600">
      <div>第 {{ page }} 页 / 共 {{ total }} 项</div>
      <div class="flex gap-2">
        <button class="btn btn-ghost" :disabled="page <= 1 || loading" @click="prev">上一页</button>
        <button class="btn btn-ghost" :disabled="page * size >= total || loading" @click="next">下一页</button>
      </div>
    </div>

    <div class="mt-4 text-xs text-slate-500">
      常用配置：
      `limits.maxOngoingOrders`，
      `ai.auto_comment.enabled`，
      `ai.auto_comment.max_providers`，
      `ai.auto_comment.batch_size`，
      `ai.auto_comment.max_retry`，
      `ai.auto_comment.retry_delay_sec`，
      `ai.auto_task.enabled`，
      `ai.auto_task.deadline_hours`，
      `ai.auto_task.total_slots`，
      `ai.auto_task.max_live_tasks`，
      `suno.api.enabled`，
      `suno.api.base_url`，
      `suno.api.key`，
      `suno.api.model`，
      `suno.api.callback_url`，
      `suno.api.timeout_ms`，
      `music.free_until`，
      `music.daily_free`，
      `music.paid_price`，
      `recharge.wechat.qr_url`，
      `recharge.wechat.name`，
      `ui.nav_hot_tabs`（用户端导航火热标记，值如 plaza,tasks,music）。
      AI供应商的 `api_key/base_url/model/timeout_ms/temperature` 已统一放到“广场配置”维护。
    </div>
  </AppCard>

  <AppModal :open="modalOpen" :title="editing ? '编辑配置' : '新增配置'" @close="modalOpen = false">
    <div class="grid gap-3">
      <div>
        <div class="text-xs font-semibold text-slate-600">Key</div>
        <input v-model="cfgKey" class="field mt-2" placeholder="limits.maxOngoingOrders" />
      </div>
      <div>
        <div class="text-xs font-semibold text-slate-600">Value</div>
        <input v-model="cfgValue" class="field mt-2" placeholder="3" />
        <label v-if="cfgKey === 'recharge.wechat.qr_url'" class="btn btn-ghost mt-2 inline-flex cursor-pointer">
          <ImagePlus class="h-4 w-4" /> 上传微信收款码
          <input class="hidden" type="file" accept="image/*" @change="uploadRechargeQr" />
        </label>
      </div>
      <div>
        <div class="text-xs font-semibold text-slate-600">Remark</div>
        <input v-model="cfgRemark" class="field mt-2" placeholder="说明" />
      </div>
      <div class="mt-2 flex justify-end gap-2">
        <button class="btn btn-ghost" @click="modalOpen = false">取消</button>
        <button class="btn btn-primary" @click="upsert">保存</button>
      </div>
    </div>
  </AppModal>
</template>
