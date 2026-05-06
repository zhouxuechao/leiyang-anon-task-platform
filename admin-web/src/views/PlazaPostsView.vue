<script setup lang="ts">
import { Eye, Flame, RefreshCw, Trash2 } from "lucide-vue-next";
import { computed, onMounted, ref } from "vue";
import AppCard from "../components/AppCard.vue";
import AppDrawer from "../components/AppDrawer.vue";
import TagPill from "../components/TagPill.vue";
import { useAuthFailureHandler } from "../lib/auth";
import { api, API_BASE_URL } from "../lib/api";
import { fmtIso } from "../lib/format";
import { toastErr, toastOk } from "../lib/toast";

type PageResp<T> = { page: number; size: number; total: number; items: T[] };
type PostItem = {
  id: number;
  authorId: number;
  authorName: string;
  category: string;
  categoryName: string;
  content: string;
  likeCount: number;
  commentCount: number;
  hot: boolean;
  createdAt: string;
};
type PostComment = {
  id: number;
  userId: number;
  userName: string;
  userAvatar: string;
  content: string;
  createdAt: string;
};
type PostDetail = {
  id: number;
  authorId: number;
  authorName: string;
  authorAvatar: string;
  category: string;
  categoryName: string;
  content: string;
  images: string[];
  likeCount: number;
  commentCount: number;
  hot: boolean;
  createdAt: string;
  comments: PostComment[];
};

const onAuthFail = useAuthFailureHandler();
const loading = ref(false);
const page = ref(1);
const size = ref(20);
const total = ref(0);
const items = ref<PostItem[]>([]);
const q = ref("");
const category = ref("");
const drawerOpen = ref(false);
const detailLoading = ref(false);
const detail = ref<PostDetail | null>(null);
const commentQ = ref("");

const filteredComments = computed(() => {
  const list = detail.value?.comments || [];
  const qv = commentQ.value.trim().toLowerCase();
  if (!qv) return list;
  return list.filter((c) => {
    return (
      String(c.userId).includes(qv) ||
      (c.userName || "").toLowerCase().includes(qv) ||
      (c.content || "").toLowerCase().includes(qv)
    );
  });
});

function fileUrl(raw?: string) {
  const v = String(raw || "").trim();
  if (!v) return "";
  if (/^https?:\/\//i.test(v)) return v;
  if (v.startsWith("/")) return API_BASE_URL.replace(/\/$/, "") + v;
  return API_BASE_URL.replace(/\/$/, "") + "/" + v;
}

async function load() {
  try {
    loading.value = true;
    const data = await api<PageResp<PostItem>>(
      `/api/admin/plaza/posts?page=${page.value}&size=${size.value}&q=${encodeURIComponent(q.value)}&category=${encodeURIComponent(category.value)}`
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

async function delPost(id: number) {
  if (!window.confirm(`确认删除帖子 #${id} ?`)) return;
  try {
    await api<void>(`/api/admin/plaza/posts/${id}/delete`, { method: "POST" });
    toastOk("删除成功");
    if (items.value.length === 1 && page.value > 1) page.value -= 1;
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("删除失败", e?.message || "");
  }
}

async function setHot(post: PostItem | PostDetail, hot: boolean) {
  try {
    await api<void>(`/api/admin/plaza/posts/${post.id}/hot`, { method: "POST", body: { hot } });
    post.hot = hot;
    if (detail.value?.id === post.id) detail.value.hot = hot;
    const item = items.value.find((p) => p.id === post.id);
    if (item) item.hot = hot;
    toastOk(hot ? "已设为热门" : "已取消热门");
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("设置失败", e?.message || "");
  }
}

async function openDetail(id: number) {
  drawerOpen.value = true;
  detail.value = null;
  commentQ.value = "";
  try {
    detailLoading.value = true;
    detail.value = await api<PostDetail>(`/api/admin/plaza/posts/${id}`);
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("加载详情失败", e?.message || "");
  } finally {
    detailLoading.value = false;
  }
}

async function delComment(commentId: number) {
  if (!detail.value) return;
  if (!window.confirm(`确认删除评论 #${commentId} ?`)) return;
  try {
    await api<void>(`/api/admin/plaza/posts/comments/${commentId}/delete`, { method: "POST" });
    toastOk("评论已删除");
    detail.value = await api<PostDetail>(`/api/admin/plaza/posts/${detail.value.id}`);
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("删除评论失败", e?.message || "");
  }
}

onMounted(load);
</script>

<template>
  <AppCard>
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <div class="text-sm font-semibold text-slate-600">广场帖子管理</div>
        <div class="mt-1 text-lg font-semibold text-slate-900">内容治理与清理</div>
      </div>
      <div class="flex items-center gap-2">
        <TagPill tone="info" :text="String(total) + ' 条'" />
        <button class="btn btn-ghost" :disabled="loading" @click="load"><RefreshCw class="h-4 w-4" /> 刷新</button>
      </div>
    </div>

    <div class="mt-4 grid grid-cols-1 gap-3 md:grid-cols-3">
      <div class="md:col-span-2">
        <div class="text-xs font-semibold text-slate-600">搜索内容</div>
        <input v-model="q" class="field mt-2" placeholder="模糊搜索：内容 / 昵称 / openId" @keydown.enter="applyFilter" />
      </div>
      <div>
        <div class="text-xs font-semibold text-slate-600">分类代码</div>
        <input v-model="category" class="field mt-2" placeholder="例如 FEMALE / PET / OTHER" @keydown.enter="applyFilter" />
      </div>
    </div>

    <div class="mt-3">
      <button class="btn btn-ghost" @click="applyFilter">应用筛选</button>
    </div>

    <div class="mt-4 overflow-x-auto rounded-2xl border border-slate-200/70 bg-white/60">
      <table class="w-full text-sm">
        <thead class="bg-white/70 text-left text-xs text-slate-600">
          <tr>
            <th class="px-4 py-3 font-semibold">作者/分类</th>
            <th class="px-4 py-3 font-semibold">内容</th>
            <th class="px-4 py-3 font-semibold">互动</th>
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
          <tr v-for="p in items" :key="p.id" class="border-t border-slate-200/60">
            <td class="px-4 py-3">
              <div class="font-semibold text-slate-900">{{ p.authorName }}</div>
              <div class="mt-1 text-xs text-slate-500">UID {{ p.authorId }} · {{ p.categoryName || p.category }}</div>
            </td>
            <td class="px-4 py-3">
              <div class="max-w-[520px] truncate text-slate-800">{{ p.content }}</div>
            </td>
            <td class="px-4 py-3 text-slate-700">赞 {{ p.likeCount }} · 评 {{ p.commentCount }}</td>
            <td class="px-4 py-3 text-slate-700">{{ fmtIso(p.createdAt) }}</td>
            <td class="px-4 py-3">
              <div class="flex gap-2">
                <button class="btn btn-ghost" @click="openDetail(p.id)"><Eye class="h-4 w-4" /> 详情</button>
                <button class="btn btn-ghost" :class="p.hot ? 'text-orange-600' : ''" @click="setHot(p, !p.hot)">
                  <Flame class="h-4 w-4" /> {{ p.hot ? "取消热门" : "设热门" }}
                </button>
                <button class="btn btn-ghost text-rose-600" @click="delPost(p.id)"><Trash2 class="h-4 w-4" /> 删除</button>
              </div>
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

  <AppDrawer :open="drawerOpen" title="帖子详情" @close="drawerOpen = false">
    <div v-if="detailLoading" class="rounded-2xl border border-slate-200/70 bg-white/60 px-4 py-6 text-sm text-slate-600">
      加载中...
    </div>
    <div v-else-if="detail" class="grid gap-4">
      <div class="rounded-2xl border border-slate-200/70 bg-white/60 p-4">
        <div class="flex items-center justify-between gap-3">
          <div>
            <div class="text-lg font-semibold text-slate-900">{{ detail.authorName }}</div>
            <div class="mt-1 text-xs text-slate-500">UID {{ detail.authorId }} · {{ detail.categoryName || detail.category }}</div>
          </div>
          <div class="flex items-center gap-2">
            <TagPill :tone="detail.hot ? 'warn' : 'info'" :text="(detail.hot ? '热门 · ' : '') + '赞 ' + detail.likeCount + ' · 评 ' + detail.commentCount" />
            <button class="btn btn-ghost h-8 px-2" :class="detail.hot ? 'text-orange-600' : ''" @click="setHot(detail, !detail.hot)">
              <Flame class="h-4 w-4" /> {{ detail.hot ? "取消热门" : "设热门" }}
            </button>
          </div>
        </div>
        <div class="mt-3 whitespace-pre-wrap text-sm text-slate-800">{{ detail.content }}</div>
        <div v-if="detail.images?.length" class="mt-3 grid grid-cols-2 gap-2">
          <a v-for="img in detail.images" :key="img" :href="fileUrl(img)" target="_blank" class="block">
            <img :src="fileUrl(img)" class="h-28 w-full rounded-xl object-cover" />
          </a>
        </div>
        <div class="mt-2 text-xs text-slate-500">{{ fmtIso(detail.createdAt) }}</div>
      </div>

      <div class="rounded-2xl border border-slate-200/70 bg-white/60 p-4">
        <div class="flex items-center justify-between gap-3">
          <div class="text-sm font-semibold text-slate-700">评论列表（{{ detail.comments?.length || 0 }}）</div>
          <input v-model="commentQ" class="field h-9 w-[260px] py-1 text-sm" placeholder="模糊搜索评论：UID/昵称/内容" />
        </div>
        <div v-if="!filteredComments.length" class="mt-3 text-sm text-slate-500">暂无评论</div>
        <div v-for="c in filteredComments" :key="c.id" class="mt-3 rounded-xl border border-slate-200/70 bg-white/70 p-3">
          <div class="flex items-center justify-between gap-2">
            <div class="text-sm font-semibold text-slate-900">{{ c.userName }}</div>
            <div class="flex items-center gap-2">
              <div class="text-xs text-slate-500">{{ fmtIso(c.createdAt) }}</div>
              <button class="btn btn-ghost h-7 px-2 text-rose-600" @click="delComment(c.id)">
                <Trash2 class="h-4 w-4" /> 删除
              </button>
            </div>
          </div>
          <div class="mt-1 text-sm text-slate-700">UID {{ c.userId }}</div>
          <div class="mt-2 whitespace-pre-wrap text-sm text-slate-800">{{ c.content }}</div>
        </div>
      </div>
    </div>
  </AppDrawer>
</template>
