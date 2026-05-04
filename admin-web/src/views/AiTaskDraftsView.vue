<script setup lang="ts">
import { Bot, RefreshCw, Sparkles, Trash2 } from "lucide-vue-next";
import { computed, onMounted, ref } from "vue";
import AppCard from "../components/AppCard.vue";
import TagPill from "../components/TagPill.vue";
import { useAuthFailureHandler } from "../lib/auth";
import { api } from "../lib/api";
import { toastErr, toastOk } from "../lib/toast";

type ProviderItem = { code: string; name: string; abbr: string };
type ProviderHealth = { code: string; name: string; abbr: string; ready: boolean; ok: boolean; message: string };
type DraftItem = {
  id: number;
  providerCode: string;
  title: string;
  content: string;
  category: string;
  locationText: string;
  amount: string;
  totalSlots: number;
  deadlineAt: string;
  proofRequirements: string;
  status: string;
  publishedTaskNo: string;
  createdAt: string;
};

const onAuthFail = useAuthFailureHandler();
const loading = ref(false);
const generating = ref(false);
const providers = ref<ProviderItem[]>([]);
const providerChecks = ref<ProviderHealth[]>([]);
const drafts = ref<DraftItem[]>([]);
const pickedProvider = ref("");
const checkingProviders = ref(false);
const currentDraft = computed(() => (drafts.value || []).find((d) => d.status === "DRAFT") || null);

function toLocalInput(iso: string) {
  const d = new Date(iso || "");
  if (Number.isNaN(d.getTime())) return "";
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mi = String(d.getMinutes()).padStart(2, "0");
  return `${d.getFullYear()}-${mm}-${dd}T${hh}:${mi}`;
}

function fromLocalInput(v: string) {
  const d = new Date(v || "");
  if (Number.isNaN(d.getTime())) return "";
  return d.toISOString();
}

async function load() {
  try {
    loading.value = true;
    const [p, d] = await Promise.all([
      api<ProviderItem[]>("/api/admin/ai-task-drafts/providers"),
      api<DraftItem[]>("/api/admin/ai-task-drafts"),
    ]);
    providers.value = p || [];
    drafts.value = (d || []).map((x) => ({
      ...x,
      deadlineAt: x.deadlineAt ? toLocalInput(x.deadlineAt) : "",
    }));
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("加载失败", e?.message || "");
  } finally {
    loading.value = false;
  }
}

function providerLabel(p: { code: string; name?: string; abbr?: string }) {
  const c = String(p.code || "").toLowerCase();
  if (c === "gpt" || c === "openai") return "GPT";
  if (c === "doubao" || c === "ark") return "豆包 (Doubao)";
  if (c === "qwen" || c === "dashscope") return "千问 (Qwen)";
  if (c === "glm") return "智谱 GLM";
  return p.abbr || p.name || p.code;
}

async function checkProviders() {
  try {
    checkingProviders.value = true;
    providerChecks.value = await api<ProviderHealth[]>("/api/admin/ai-task-drafts/providers/check");
    toastOk("连通性检查完成");
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("检查失败", e?.message || "");
  } finally {
    checkingProviders.value = false;
  }
}

async function generateOne() {
  try {
    generating.value = true;
    await api<DraftItem>("/api/admin/ai-task-drafts/generate", {
      method: "POST",
      body: { providerCode: pickedProvider.value || "" },
    });
    toastOk("AI任务草稿已生成");
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("生成失败", e?.message || "");
  } finally {
    generating.value = false;
  }
}

async function persistDraft(d: DraftItem, showToast = false) {
  try {
    const saved = await api<DraftItem>(`/api/admin/ai-task-drafts/${d.id}/update`, {
      method: "POST",
      body: {
        title: d.title,
        content: d.content,
        category: d.category,
        locationText: d.locationText,
        amount: Number(d.amount),
        totalSlots: Number(d.totalSlots),
        deadlineAt: fromLocalInput(d.deadlineAt),
        proofRequirements: d.proofRequirements,
      },
    });
    Object.assign(d, {
      ...saved,
      deadlineAt: saved.deadlineAt ? toLocalInput(saved.deadlineAt) : "",
    });
    if (showToast) toastOk("已保存草稿");
    return true;
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("保存失败", e?.message || "");
    return false;
  }
}

async function publishDraft(d: DraftItem) {
  try {
    const ok = await persistDraft(d);
    if (!ok) return;
    const r = await api<{ taskNo: string }>(`/api/admin/ai-task-drafts/${d.id}/publish`, { method: "POST" });
    toastOk(`发布成功：${r.taskNo}`);
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("发布失败", e?.message || "");
  }
}

async function deleteDraft(id: number) {
  if (!window.confirm(`确认删除草稿 #${id} ?`)) return;
  try {
    await api<void>(`/api/admin/ai-task-drafts/${id}/delete`, { method: "POST" });
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
        <div class="text-sm font-semibold text-slate-600">AI任务生成</div>
        <div class="mt-1 text-lg font-semibold text-slate-900">AI提问人类，一键生成草稿后人工审查发布</div>
      </div>
      <div class="flex items-center gap-2">
        <button class="btn btn-ghost" :disabled="checkingProviders" @click="checkProviders">
          {{ checkingProviders ? "检查中..." : "检查AI连通性" }}
        </button>
        <button class="btn btn-ghost" :disabled="loading" @click="load"><RefreshCw class="h-4 w-4" /> 刷新</button>
      </div>
    </div>

    <div class="mt-4 rounded-2xl border border-slate-200/70 bg-white/70 p-4">
      <div class="grid grid-cols-1 gap-3 md:grid-cols-[260px_1fr]">
        <div>
          <div class="text-xs font-semibold text-slate-600">指定AI提供商（可选）</div>
          <select v-model="pickedProvider" class="field mt-2">
            <option value="">自动选择（优先 GPT）</option>
            <option v-for="p in providers" :key="p.code" :value="p.code">
              {{ providerLabel(p) }} ({{ p.code }})
            </option>
          </select>
        </div>
        <div class="flex items-end">
          <button class="btn btn-primary" :disabled="generating" @click="generateOne">
            <Sparkles class="h-4 w-4" />
            {{ generating ? "生成中..." : "一键生成任务草稿" }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="providerChecks.length" class="mt-4 rounded-2xl border border-slate-200/70 bg-white/70 p-4">
      <div class="text-sm font-semibold text-slate-700">AI连通性结果</div>
      <div class="mt-3 grid gap-2">
        <div
          v-for="p in providerChecks"
          :key="p.code"
          class="flex items-center justify-between rounded-xl border border-slate-200/70 bg-white px-3 py-2 text-sm"
        >
          <div class="font-semibold text-slate-800">{{ providerLabel(p) }} ({{ p.code }})</div>
          <div class="flex items-center gap-2">
            <TagPill :tone="p.ready ? 'info' : 'err'" :text="p.ready ? '已配置' : '未就绪'" />
            <TagPill :tone="p.ok ? 'ok' : 'err'" :text="p.ok ? '可调用' : '失败'" />
            <div class="text-xs text-slate-500">{{ p.message }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-4 grid gap-4">
      <div
        v-if="currentDraft"
        :key="currentDraft.id"
        class="rounded-2xl border border-slate-200/70 bg-white/70 p-4"
      >
        <div class="flex flex-wrap items-center justify-between gap-2">
          <div class="flex items-center gap-2">
            <TagPill tone="info" text="DRAFT" />
            <div class="text-xs text-slate-500">#{{ currentDraft.id }} · {{ currentDraft.providerCode }}</div>
          </div>
          <div class="flex gap-2">
            <button class="btn btn-success" @click="publishDraft(currentDraft)">
              <Bot class="h-4 w-4" /> 一键发布
            </button>
            <button class="btn btn-ghost text-rose-600" @click="deleteDraft(currentDraft.id)">
              <Trash2 class="h-4 w-4" /> 删除
            </button>
          </div>
        </div>

        <div class="mt-3 grid gap-3 md:grid-cols-2">
          <div class="md:col-span-2">
            <div class="text-xs font-semibold text-slate-600">标题</div>
            <input v-model="currentDraft.title" class="field mt-2" />
          </div>
          <div class="md:col-span-2">
            <div class="text-xs font-semibold text-slate-600">任务说明</div>
            <textarea v-model="currentDraft.content" class="field mt-2 h-28 resize-none" />
          </div>
          <div>
            <div class="text-xs font-semibold text-slate-600">分类</div>
            <input v-model="currentDraft.category" class="field mt-2" />
          </div>
          <div>
            <div class="text-xs font-semibold text-slate-600">地点</div>
            <input v-model="currentDraft.locationText" class="field mt-2" />
          </div>
          <div>
            <div class="text-xs font-semibold text-slate-600">金额</div>
            <input v-model="currentDraft.amount" type="number" class="field mt-2" />
          </div>
          <div>
            <div class="text-xs font-semibold text-slate-600">人数</div>
            <input v-model.number="currentDraft.totalSlots" type="number" class="field mt-2" />
          </div>
          <div>
            <div class="text-xs font-semibold text-slate-600">截止时间</div>
            <input v-model="currentDraft.deadlineAt" type="datetime-local" class="field mt-2" />
          </div>
          <div>
            <div class="text-xs font-semibold text-slate-600">凭证要求</div>
            <input v-model="currentDraft.proofRequirements" class="field mt-2" />
          </div>
        </div>
      </div>

      <div v-if="!currentDraft" class="rounded-2xl border border-slate-200/70 bg-white/70 px-4 py-10 text-center text-sm text-slate-500">
        {{ loading ? "加载中..." : "暂无可编辑草稿，点击上方“一键生成任务草稿”" }}
      </div>
    </div>
  </AppCard>
</template>
