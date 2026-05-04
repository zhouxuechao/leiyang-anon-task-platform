<script setup lang="ts">
import { MessageSquareText, RefreshCw, Search } from "lucide-vue-next";
import { onMounted, ref } from "vue";
import AppCard from "../components/AppCard.vue";
import TagPill from "../components/TagPill.vue";
import { useAuthFailureHandler } from "../lib/auth";
import { api } from "../lib/api";
import { fmtIso } from "../lib/format";
import { toastErr } from "../lib/toast";

type PageResp<T> = { page: number; size: number; total: number; items: T[] };
type FeedbackItem = {
  id: number;
  userId: number;
  openId: string;
  nickname: string;
  content: string;
  contact: string;
  status: string;
  createdAt: string;
};

const onAuthFail = useAuthFailureHandler();
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const q = ref("");
const items = ref<FeedbackItem[]>([]);

async function load() {
  try {
    loading.value = true;
    const data = await api<PageResp<FeedbackItem>>(
      `/api/admin/feedbacks?page=${page.value}&size=${size.value}&q=${encodeURIComponent(q.value)}`
    );
    items.value = data.items || [];
    total.value = Number(data.total || 0);
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

onMounted(load);
</script>

<template>
  <AppCard>
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <div class="text-sm font-semibold text-slate-600">用户反馈建议</div>
        <div class="mt-1 text-lg font-semibold text-slate-900">提交列表</div>
      </div>
      <div class="flex items-center gap-2">
        <TagPill tone="info" :text="'总数 ' + total" />
        <button class="btn btn-ghost" :disabled="loading" @click="load"><RefreshCw class="h-4 w-4" /> 刷新</button>
      </div>
    </div>

    <div class="mt-4 flex flex-wrap items-end gap-3">
      <div class="min-w-[280px] flex-1">
        <div class="text-xs font-semibold text-slate-600">搜索</div>
        <input v-model="q" class="field mt-2" placeholder="内容 / 联系方式 / openId / 昵称 / 用户ID" @keydown.enter="applyFilter" />
      </div>
      <button class="btn btn-ghost" @click="applyFilter"><Search class="h-4 w-4" /> 查询</button>
    </div>

    <div class="mt-4 overflow-hidden rounded-2xl border border-slate-200/70 bg-white/60">
      <table class="w-full text-sm">
        <thead class="bg-white/70 text-left text-xs text-slate-600">
          <tr>
            <th class="px-4 py-3 font-semibold">用户</th>
            <th class="px-4 py-3 font-semibold">反馈内容</th>
            <th class="px-4 py-3 font-semibold">联系方式</th>
            <th class="px-4 py-3 font-semibold">状态</th>
            <th class="px-4 py-3 font-semibold">提交时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!items.length">
            <td colspan="5" class="px-4 py-10 text-center text-slate-500">
              <span v-if="loading">加载中...</span>
              <span v-else>暂无反馈</span>
            </td>
          </tr>
          <tr v-for="item in items" :key="item.id" class="border-t border-slate-200/60">
            <td class="px-4 py-3">
              <div class="font-semibold text-slate-900">{{ item.nickname || "-" }}</div>
              <div class="mt-1 text-xs text-slate-500">#{{ item.userId }} · {{ item.openId }}</div>
            </td>
            <td class="px-4 py-3 text-slate-800">
              <div class="flex items-start gap-2">
                <MessageSquareText class="mt-0.5 h-4 w-4 shrink-0 text-slate-400" />
                <div class="whitespace-pre-wrap break-all">{{ item.content || "-" }}</div>
              </div>
            </td>
            <td class="px-4 py-3 text-slate-700">{{ item.contact || "-" }}</td>
            <td class="px-4 py-3">
              <TagPill tone="warn" :text="item.status || 'NEW'" />
            </td>
            <td class="px-4 py-3 text-slate-700">{{ fmtIso(item.createdAt) }}</td>
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
