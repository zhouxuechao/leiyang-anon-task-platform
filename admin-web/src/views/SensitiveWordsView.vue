<script setup lang="ts">
import { Plus, RefreshCw, ShieldAlert } from "lucide-vue-next";
import { onMounted, ref } from "vue";
import AppCard from "../components/AppCard.vue";
import TagPill from "../components/TagPill.vue";
import { useAuthFailureHandler } from "../lib/auth";
import { api } from "../lib/api";
import { toastErr, toastOk } from "../lib/toast";

type WordItem = { id: number; word: string; level: number; actionType: string; status: string };
type PageResp<T> = { page: number; size: number; total: number; items: T[] };

const onAuthFail = useAuthFailureHandler();
const loading = ref(false);
const items = ref<WordItem[]>([]);
const page = ref(1);
const size = ref(20);
const total = ref(0);

const word = ref("");
const level = ref(1);
const actionType = ref<"REJECT" | "MANUAL_REVIEW">("MANUAL_REVIEW");

async function load() {
  try {
    loading.value = true;
    const data = await api<PageResp<WordItem>>(`/api/admin/sensitive-words?page=${page.value}&size=${size.value}`);
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

async function create() {
  try {
    await api<void>("/api/admin/sensitive-words", { method: "POST", body: { word: word.value, level: level.value, actionType: actionType.value } });
    toastOk("已添加敏感词");
    word.value = "";
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("添加失败", e?.message || "");
  }
}

async function setStatus(id: number, status: "ACTIVE" | "INACTIVE") {
  try {
    await api<void>(`/api/admin/sensitive-words/${id}/status`, { method: "POST", body: { status } });
    toastOk("已更新状态");
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
    <AppCard>
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div class="text-sm font-semibold text-slate-600">敏感词</div>
          <div class="mt-1 text-lg font-semibold text-slate-900">规则维护</div>
        </div>
        <button class="btn btn-ghost" :disabled="loading" @click="load"><RefreshCw class="h-4 w-4" /> 刷新</button>
      </div>

      <div class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-4">
        <div class="md:col-span-2">
          <div class="text-xs font-semibold text-slate-600">词</div>
          <input v-model="word" class="field mt-2" placeholder="例如：加微信 / QQ / 返现 / 私聊" />
        </div>
        <div>
          <div class="text-xs font-semibold text-slate-600">等级</div>
          <input v-model.number="level" type="number" class="field mt-2" min="1" />
        </div>
        <div>
          <div class="text-xs font-semibold text-slate-600">动作</div>
          <select v-model="actionType" class="field mt-2">
            <option value="MANUAL_REVIEW">人工复核</option>
            <option value="REJECT">直接驳回</option>
          </select>
        </div>
      </div>

      <div class="mt-4 flex items-center justify-between gap-3">
        <div class="inline-flex items-center gap-2 text-xs text-slate-500">
          <ShieldAlert class="h-4 w-4" />
          建议把“导流绕平台”“隐私侵犯”“违法违规”设为更高等级
        </div>
        <button class="btn btn-primary" @click="create"><Plus class="h-4 w-4" /> 添加</button>
      </div>
    </AppCard>

    <AppCard>
      <div class="flex items-center justify-between">
        <div class="text-sm font-semibold text-slate-600">列表</div>
        <TagPill tone="info" :text="String(total) + ' 条'" />
      </div>

      <div class="mt-4 overflow-hidden rounded-2xl border border-slate-200/70 bg-white/60">
        <table class="w-full text-sm">
          <thead class="bg-white/70 text-left text-xs text-slate-600">
            <tr>
              <th class="px-4 py-3 font-semibold">词</th>
              <th class="px-4 py-3 font-semibold">等级</th>
              <th class="px-4 py-3 font-semibold">动作</th>
              <th class="px-4 py-3 font-semibold">状态</th>
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
            <tr v-for="w in items" :key="w.id" class="border-t border-slate-200/60">
              <td class="px-4 py-3 font-semibold text-slate-900">{{ w.word }}</td>
              <td class="px-4 py-3 text-slate-800">{{ w.level }}</td>
              <td class="px-4 py-3 text-slate-700">{{ w.actionType }}</td>
              <td class="px-4 py-3">
                <TagPill :tone="w.status === 'ACTIVE' ? 'ok' : 'err'" :text="w.status" />
              </td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-2">
                  <button v-if="w.status !== 'ACTIVE'" class="btn btn-ghost" @click="setStatus(w.id, 'ACTIVE')">启用</button>
                  <button v-else class="btn btn-ghost" @click="setStatus(w.id, 'INACTIVE')">停用</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="mt-4 flex items-center justify-between text-sm text-slate-600">
        <div>第 {{ page }} 页 / 共 {{ total }} 条</div>
        <div class="flex gap-2">
          <button class="btn btn-ghost" :disabled="page <= 1 || loading" @click="prev">上一页</button>
          <button class="btn btn-ghost" :disabled="page * size >= total || loading" @click="next">下一页</button>
        </div>
      </div>
    </AppCard>
  </div>
</template>
