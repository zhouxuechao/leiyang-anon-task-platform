<script setup lang="ts">
import { Bell, Plus, RefreshCw, Trash2 } from "lucide-vue-next";
import { onMounted, ref } from "vue";
import AppCard from "../components/AppCard.vue";
import AppModal from "../components/AppModal.vue";
import TagPill from "../components/TagPill.vue";
import { useAuthFailureHandler } from "../lib/auth";
import { api } from "../lib/api";
import { fmtIso } from "../lib/format";
import { toastErr, toastOk } from "../lib/toast";

type NoticeItem = { id: number; title: string; content: string; status: string; sortNo: number; createdAt: string };
type PageResp<T> = { page: number; size: number; total: number; items: T[] };

const onAuthFail = useAuthFailureHandler();
const loading = ref(false);
const items = ref<NoticeItem[]>([]);
const page = ref(1);
const size = ref(20);
const total = ref(0);

const modalOpen = ref(false);
const editId = ref<number | null>(null);
const title = ref("");
const content = ref("");
const status = ref("ACTIVE");
const sortNo = ref(10);

async function load() {
  try {
    loading.value = true;
    const data = await api<PageResp<NoticeItem>>(`/api/admin/notices?page=${page.value}&size=${size.value}`);
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
  editId.value = null;
  title.value = "";
  content.value = "";
  status.value = "ACTIVE";
  sortNo.value = 10;
  modalOpen.value = true;
}

function openEdit(n: NoticeItem) {
  editId.value = n.id;
  title.value = n.title;
  content.value = n.content;
  status.value = n.status;
  sortNo.value = n.sortNo;
  modalOpen.value = true;
}

async function upsert() {
  try {
    await api<void>("/api/admin/notices", {
      method: "POST",
      body: { id: editId.value, title: title.value, content: content.value, status: status.value, sortNo: sortNo.value },
    });
    toastOk("已保存公告");
    modalOpen.value = false;
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("保存失败", e?.message || "");
  }
}

async function del(id: number) {
  try {
    await api<void>(`/api/admin/notices/${id}/delete`, { method: "POST" });
    toastOk("已删除");
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("删除失败", e?.message || "");
  }
}

onMounted(load);
</script>

<template>
  <AppCard>
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <div class="text-sm font-semibold text-slate-600">公告</div>
        <div class="mt-1 text-lg font-semibold text-slate-900">小程序首页公告</div>
      </div>
      <div class="flex items-center gap-2">
        <TagPill tone="info" :text="total + ' 条'" />
        <button class="btn btn-ghost" :disabled="loading" @click="load"><RefreshCw class="h-4 w-4" /> 刷新</button>
        <button class="btn btn-primary" @click="openCreate"><Plus class="h-4 w-4" /> 新增</button>
      </div>
    </div>

    <div class="mt-4 overflow-x-auto rounded-2xl border border-slate-200/70 bg-white/60">
      <table class="w-full text-sm">
        <thead class="bg-white/70 text-left text-xs text-slate-600">
          <tr>
            <th class="px-4 py-3 font-semibold">标题</th>
            <th class="px-4 py-3 font-semibold">状态</th>
            <th class="px-4 py-3 font-semibold">排序</th>
            <th class="px-4 py-3 font-semibold">时间</th>
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
          <tr v-for="n in items" :key="n.id" class="border-t border-slate-200/60">
            <td class="px-4 py-3">
              <div class="font-semibold text-slate-900">{{ n.title }}</div>
              <div class="mt-0.5 text-xs text-slate-500 line-clamp-1">{{ n.content }}</div>
            </td>
            <td class="px-4 py-3">
              <TagPill :tone="n.status === 'ACTIVE' ? 'ok' : 'err'" :text="n.status" />
            </td>
            <td class="px-4 py-3 text-slate-700">{{ n.sortNo }}</td>
            <td class="px-4 py-3 text-slate-700">{{ fmtIso(n.createdAt) }}</td>
            <td class="px-4 py-3">
              <div class="flex gap-2">
                <button class="btn btn-ghost" @click="openEdit(n)">编辑</button>
                <button class="btn btn-ghost" @click="del(n.id)"><Trash2 class="h-4 w-4" /> 删除</button>
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

    <div class="mt-4 flex items-center gap-2 text-xs text-slate-500">
      <Bell class="h-4 w-4" />
      小程序可在首页展示公告（运营引导/规则说明/活动）
    </div>
  </AppCard>

  <AppModal :open="modalOpen" title="公告编辑" @close="modalOpen = false">
    <div class="grid gap-3">
      <div>
        <div class="text-xs font-semibold text-slate-600">标题</div>
        <input v-model="title" class="field mt-2" />
      </div>
      <div>
        <div class="text-xs font-semibold text-slate-600">内容</div>
        <textarea v-model="content" class="field mt-2 h-28 resize-none" />
      </div>
      <div class="grid grid-cols-2 gap-3">
        <div>
          <div class="text-xs font-semibold text-slate-600">状态</div>
          <select v-model="status" class="field mt-2">
            <option value="ACTIVE">ACTIVE</option>
            <option value="INACTIVE">INACTIVE</option>
          </select>
        </div>
        <div>
          <div class="text-xs font-semibold text-slate-600">排序</div>
          <input v-model.number="sortNo" type="number" class="field mt-2" />
        </div>
      </div>
      <div class="mt-2 flex justify-end gap-2">
        <button class="btn btn-ghost" @click="modalOpen = false">取消</button>
        <button class="btn btn-primary" @click="upsert">保存</button>
      </div>
    </div>
  </AppModal>
</template>
