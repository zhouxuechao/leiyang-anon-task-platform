<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ImagePlus, Plus, RefreshCw, Save, Trash2 } from "lucide-vue-next";
import AppCard from "../components/AppCard.vue";
import AppModal from "../components/AppModal.vue";
import { API_BASE_URL, api, upload } from "../lib/api";
import { useAuthFailureHandler } from "../lib/auth";
import { toastErr, toastOk } from "../lib/toast";

type Category = { id: number; code: string; name: string; keywords?: string; status: string; sortNo: number };
type Provider = {
  id: number;
  code: string;
  name: string;
  abbr: string;
  logoText?: string;
  logoUrl?: string | null;
  status: string;
  sortNo: number;
  baseUrl?: string | null;
  model?: string | null;
  apiKey?: string | null;
  timeoutMs?: number | null;
  temperature?: number | null;
};
type SortOpt = { id: number; code: string; name: string; status: string; sortNo: number };

const onAuthFail = useAuthFailureHandler();
const loading = ref(false);
const categories = ref<Category[]>([]);
const providers = ref<Provider[]>([]);
const sorts = ref<SortOpt[]>([]);
const providerSaving = ref<Record<string, boolean>>({});

type ProviderRuntime = {
  enabled: string;
  baseUrl: string;
  model: string;
  apiKey: string;
  timeoutMs: string;
  temperature: string;
};
const providerRuntime = ref<Record<string, ProviderRuntime>>({});

const catModal = ref(false);
const catForm = ref<{ id?: number; code: string; name: string; keywords: string; sortNo: number }>({
  code: "",
  name: "",
  keywords: "",
  sortNo: 0,
});

const providerModal = ref(false);
const providerForm = ref<{ id?: number; code: string; name: string; abbr: string; logoText: string; logoUrl: string; sortNo: number }>({
  code: "",
  name: "",
  abbr: "",
  logoText: "",
  logoUrl: "",
  sortNo: 0,
});
const runtimeModal = ref(false);
const runtimeTarget = ref<Provider | null>(null);

const sortModal = ref(false);
const sortForm = ref<{ id?: number; code: string; name: string; sortNo: number }>({
  code: "",
  name: "",
  sortNo: 0,
});

async function load() {
  try {
    loading.value = true;
    const [c, p, s] = await Promise.all([
      api<Category[]>("/api/admin/plaza/categories"),
      api<Provider[]>("/api/admin/plaza/ai-providers"),
      api<SortOpt[]>("/api/admin/plaza/sort-options"),
    ]);
    categories.value = c;
    providers.value = p;
    sorts.value = s;
    applyAiForm(p);
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("加载失败", e?.message || "");
  } finally {
    loading.value = false;
  }
}

function applyAiForm(rows: Provider[]) {
  const next: Record<string, ProviderRuntime> = {};
  for (const p of rows) {
    const code = p.code.toLowerCase();
    next[code] = {
      enabled: p.status === "ACTIVE" ? "true" : "false",
      baseUrl: p.baseUrl || "",
      model: p.model || "",
      apiKey: p.apiKey || "",
      timeoutMs: String(p.timeoutMs ?? 6000),
      temperature: String(p.temperature ?? 0),
    };
  }
  providerRuntime.value = next;
}

function runtimeByProvider(code: string): ProviderRuntime {
  const k = code.toLowerCase();
  if (!providerRuntime.value[k]) {
    providerRuntime.value[k] = {
      enabled: "false",
      baseUrl: "",
      model: "",
      apiKey: "",
      timeoutMs: "6000",
      temperature: "0",
    };
  }
  return providerRuntime.value[k];
}

function logoSrc(url?: string | null) {
  const v = String(url || "").trim();
  if (!v) return "";
  if (/^https?:\/\//i.test(v)) return v;
  return v.startsWith("/") ? API_BASE_URL + v : `${API_BASE_URL}/${v}`;
}

async function saveProviderRuntime(code: string) {
  const key = code.toLowerCase();
  const cfg = runtimeByProvider(key);
  const row = providers.value.find(v => v.code.toLowerCase() === key);
  if (!row) return;
  try {
    providerSaving.value[key] = true;
    await api<void>("/api/admin/plaza/ai-providers", {
      method: "POST",
      body: {
        id: row.id,
        code: row.code,
        name: row.name,
        abbr: row.abbr,
        logoText: row.logoText || "",
        logoUrl: row.logoUrl || "",
        sortNo: row.sortNo,
        baseUrl: cfg.baseUrl,
        model: cfg.model,
        apiKey: cfg.apiKey,
        timeoutMs: Number(cfg.timeoutMs || 6000),
        temperature: Number(cfg.temperature || 0),
      },
    });
    if ((cfg.enabled === "true") !== (row.status === "ACTIVE")) {
      await api<void>(`/api/admin/plaza/ai-providers/${row.id}/toggle`, { method: "POST" });
    }
    toastOk(`${key} 配置已保存`);
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("保存失败", e?.message || "");
  } finally {
    providerSaving.value[key] = false;
  }
}

function openRuntime(row: Provider) {
  runtimeTarget.value = row;
  runtimeByProvider(row.code);
  runtimeModal.value = true;
}

async function saveRuntimeModal() {
  if (!runtimeTarget.value) return;
  await saveProviderRuntime(runtimeTarget.value.code);
  runtimeModal.value = false;
}

function openCategory(row?: Category) {
  catForm.value = row
    ? { id: row.id, code: row.code, name: row.name, keywords: row.keywords || "", sortNo: row.sortNo }
    : { code: "", name: "", keywords: "", sortNo: 0 };
  catModal.value = true;
}

function openProvider(row?: Provider) {
  providerForm.value = row
    ? { id: row.id, code: row.code, name: row.name, abbr: row.abbr, logoText: row.logoText || "", logoUrl: row.logoUrl || "", sortNo: row.sortNo }
    : { code: "", name: "", abbr: "", logoText: "", logoUrl: "", sortNo: 0 };
  providerModal.value = true;
}

async function uploadProviderLogo(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  try {
    providerForm.value.logoUrl = await upload(file);
    toastOk("Logo已上传，请保存AI配置");
  } catch (err: any) {
    if (err?.message === "AUTH") return onAuthFail();
    toastErr("上传失败", err?.message || "");
  } finally {
    (e.target as HTMLInputElement).value = "";
  }
}

function openSort(row?: SortOpt) {
  sortForm.value = row
    ? { id: row.id, code: row.code, name: row.name, sortNo: row.sortNo }
    : { code: "", name: "", sortNo: 0 };
  sortModal.value = true;
}

async function saveCategory() {
  try {
    await api<void>("/api/admin/plaza/categories", { method: "POST", body: catForm.value });
    toastOk("分类已保存");
    catModal.value = false;
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("保存失败", e?.message || "");
  }
}

async function saveProvider() {
  try {
    const old = providerForm.value.id ? providers.value.find(v => v.id === providerForm.value.id) : null;
    await api<void>("/api/admin/plaza/ai-providers", {
      method: "POST",
      body: {
        ...providerForm.value,
        baseUrl: old?.baseUrl || "",
        model: old?.model || "",
        apiKey: old?.apiKey || "",
        timeoutMs: old?.timeoutMs ?? 6000,
        temperature: old?.temperature ?? 0,
      },
    });
    toastOk("AI配置已保存");
    providerModal.value = false;
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("保存失败", e?.message || "");
  }
}

async function saveSort() {
  try {
    await api<void>("/api/admin/plaza/sort-options", { method: "POST", body: sortForm.value });
    toastOk("排序项已保存");
    sortModal.value = false;
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("保存失败", e?.message || "");
  }
}

async function toggle(path: string) {
  try {
    await api<void>(path, { method: "POST" });
    await load();
  } catch (e: any) {
    if (e?.message === "AUTH") return onAuthFail();
    toastErr("操作失败", e?.message || "");
  }
}

async function del(path: string) {
  try {
    await api<void>(path, { method: "POST" });
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
  <div class="grid gap-4">
    <AppCard>
      <div class="flex items-center justify-between">
        <div>
          <div class="text-sm font-semibold text-slate-600">广场分类管理</div>
          <div class="mt-1 text-lg font-semibold text-slate-900">AI自动归类使用这份分类词库</div>
        </div>
        <div class="flex gap-2">
          <button class="btn btn-ghost" :disabled="loading" @click="load"><RefreshCw class="h-4 w-4" /> 刷新</button>
          <button class="btn btn-primary" @click="openCategory()"><Plus class="h-4 w-4" /> 新增分类</button>
        </div>
      </div>
      <div class="mt-4 overflow-hidden rounded-2xl border border-slate-200/70 bg-white/60">
        <table class="w-full text-sm">
          <thead class="bg-white/70 text-left text-xs text-slate-600">
            <tr>
              <th class="px-4 py-3 font-semibold">Code</th>
              <th class="px-4 py-3 font-semibold">名称</th>
              <th class="px-4 py-3 font-semibold">关键词</th>
              <th class="px-4 py-3 font-semibold">状态</th>
              <th class="px-4 py-3 font-semibold">排序</th>
              <th class="px-4 py-3 font-semibold">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in categories" :key="c.id" class="border-t border-slate-200/60">
              <td class="px-4 py-3 font-semibold text-slate-900">{{ c.code }}</td>
              <td class="px-4 py-3">{{ c.name }}</td>
              <td class="px-4 py-3 text-slate-600">{{ c.keywords || "-" }}</td>
              <td class="px-4 py-3">{{ c.status === "ACTIVE" ? "启用" : "停用" }}</td>
              <td class="px-4 py-3">{{ c.sortNo }}</td>
              <td class="px-4 py-3">
                <div class="flex gap-2">
                  <button class="btn btn-ghost" @click="openCategory(c)"><Save class="h-4 w-4" /> 编辑</button>
                  <button class="btn btn-ghost" @click="toggle(`/api/admin/plaza/categories/${c.id}/toggle`)">{{ c.status === "ACTIVE" ? "停用" : "启用" }}</button>
                  <button class="btn btn-ghost" @click="del(`/api/admin/plaza/categories/${c.id}/delete`)"><Trash2 class="h-4 w-4" /> 删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </AppCard>

    <AppCard>
      <div class="flex items-center justify-between">
        <div class="text-lg font-semibold text-slate-900">AI提供商统一管理（展示 + 运行参数）</div>
        <button class="btn btn-primary" @click="openProvider()"><Plus class="h-4 w-4" /> 新增AI</button>
      </div>
      <div class="mt-4 overflow-hidden rounded-2xl border border-slate-200/70 bg-white/60">
        <table class="w-full text-sm">
          <thead class="bg-white/70 text-left text-xs text-slate-600">
            <tr>
              <th class="px-4 py-3 font-semibold">Code</th>
              <th class="px-4 py-3 font-semibold">名称</th>
              <th class="px-4 py-3 font-semibold">缩写</th>
              <th class="px-4 py-3 font-semibold">Logo</th>
              <th class="px-4 py-3 font-semibold">状态</th>
              <th class="px-4 py-3 font-semibold">运行</th>
              <th class="px-4 py-3 font-semibold">模型</th>
              <th class="px-4 py-3 font-semibold">排序</th>
              <th class="px-4 py-3 font-semibold">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in providers" :key="p.id" class="border-t border-slate-200/60">
              <td class="px-4 py-3 font-semibold text-slate-900">{{ p.code }}</td>
              <td class="px-4 py-3">{{ p.name }}</td>
              <td class="px-4 py-3">{{ p.abbr }}</td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-2">
                  <img v-if="p.logoUrl" :src="logoSrc(p.logoUrl)" class="h-8 w-8 rounded-full object-cover" alt="" />
                  <span v-else class="inline-grid h-8 w-8 place-items-center rounded-full bg-indigo-100 text-xs font-black text-indigo-600">{{ p.logoText || p.abbr }}</span>
                  <span class="text-xs text-slate-500">{{ p.logoText || "-" }}</span>
                </div>
              </td>
              <td class="px-4 py-3">{{ p.status === "ACTIVE" ? "启用" : "停用" }}</td>
              <td class="px-4 py-3">{{ runtimeByProvider(p.code).enabled === "true" ? "已启用" : "已关闭" }}</td>
              <td class="px-4 py-3 text-slate-600">{{ runtimeByProvider(p.code).model || "-" }}</td>
              <td class="px-4 py-3">{{ p.sortNo }}</td>
              <td class="px-4 py-3">
                <div class="flex gap-2">
                  <button class="btn btn-ghost" @click="openRuntime(p)">AI配置</button>
                  <button class="btn btn-ghost" @click="openProvider(p)">编辑</button>
                  <button class="btn btn-ghost" @click="toggle(`/api/admin/plaza/ai-providers/${p.id}/toggle`)">{{ p.status === "ACTIVE" ? "停用" : "启用" }}</button>
                  <button class="btn btn-ghost" @click="del(`/api/admin/plaza/ai-providers/${p.id}/delete`)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </AppCard>

    <AppCard>
      <div class="flex items-center justify-between">
        <div class="text-lg font-semibold text-slate-900">广场排序项管理（热门/最新/关注）</div>
        <button class="btn btn-primary" @click="openSort()"><Plus class="h-4 w-4" /> 新增排序项</button>
      </div>
      <div class="mt-4 overflow-hidden rounded-2xl border border-slate-200/70 bg-white/60">
        <table class="w-full text-sm">
          <thead class="bg-white/70 text-left text-xs text-slate-600">
            <tr>
              <th class="px-4 py-3 font-semibold">Code</th>
              <th class="px-4 py-3 font-semibold">名称</th>
              <th class="px-4 py-3 font-semibold">状态</th>
              <th class="px-4 py-3 font-semibold">排序</th>
              <th class="px-4 py-3 font-semibold">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in sorts" :key="s.id" class="border-t border-slate-200/60">
              <td class="px-4 py-3 font-semibold text-slate-900">{{ s.code }}</td>
              <td class="px-4 py-3">{{ s.name }}</td>
              <td class="px-4 py-3">{{ s.status === "ACTIVE" ? "启用" : "停用" }}</td>
              <td class="px-4 py-3">{{ s.sortNo }}</td>
              <td class="px-4 py-3">
                <div class="flex gap-2">
                  <button class="btn btn-ghost" @click="openSort(s)">编辑</button>
                  <button class="btn btn-ghost" @click="toggle(`/api/admin/plaza/sort-options/${s.id}/toggle`)">{{ s.status === "ACTIVE" ? "停用" : "启用" }}</button>
                  <button class="btn btn-ghost" @click="del(`/api/admin/plaza/sort-options/${s.id}/delete`)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </AppCard>
  </div>

  <AppModal :open="catModal" title="分类配置" @close="catModal = false">
    <div class="grid gap-3">
      <input v-model="catForm.code" class="field" placeholder="Code，例如 TECH" />
      <input v-model="catForm.name" class="field" placeholder="分类名，例如 科技" />
      <input v-model="catForm.keywords" class="field" placeholder="关键词，用逗号分隔" />
      <input v-model.number="catForm.sortNo" class="field" type="number" placeholder="排序值" />
      <div class="mt-2 flex justify-end gap-2">
        <button class="btn btn-ghost" @click="catModal = false">取消</button>
        <button class="btn btn-primary" @click="saveCategory">保存</button>
      </div>
    </div>
  </AppModal>

  <AppModal :open="providerModal" title="AI提供商配置" @close="providerModal = false">
    <div class="grid gap-3">
      <input v-model="providerForm.code" class="field" placeholder="Code，例如 deepseek" />
      <input v-model="providerForm.name" class="field" placeholder="名称" />
      <input v-model="providerForm.abbr" class="field" placeholder="缩写，例如 DS" />
      <input v-model="providerForm.logoText" class="field" placeholder="Logo文字，例如 D" />
      <input v-model="providerForm.logoUrl" class="field" placeholder="Logo图片URL，或点击下方上传" />
      <label class="btn btn-ghost inline-flex cursor-pointer">
        <ImagePlus class="h-4 w-4" /> 上传Logo图片
        <input class="hidden" type="file" accept="image/*" @change="uploadProviderLogo" />
      </label>
      <input v-model.number="providerForm.sortNo" class="field" type="number" placeholder="排序值" />
      <div class="mt-2 flex justify-end gap-2">
        <button class="btn btn-ghost" @click="providerModal = false">取消</button>
        <button class="btn btn-primary" @click="saveProvider">保存</button>
      </div>
    </div>
  </AppModal>

  <AppModal
    :open="runtimeModal"
    :title="`AI运行配置 - ${runtimeTarget?.name || ''}`"
    @close="runtimeModal = false"
  >
    <div v-if="runtimeTarget" class="grid gap-3">
      <label class="grid gap-2">
        <span class="text-xs font-semibold text-slate-600">启用</span>
        <select v-model="runtimeByProvider(runtimeTarget.code).enabled" class="field">
          <option value="true">启用</option>
          <option value="false">关闭</option>
        </select>
      </label>
      <label class="grid gap-2">
        <span class="text-xs font-semibold text-slate-600">模型</span>
        <input v-model="runtimeByProvider(runtimeTarget.code).model" class="field" placeholder="例如 deepseek-chat" />
      </label>
      <label class="grid gap-2">
        <span class="text-xs font-semibold text-slate-600">Base URL</span>
        <input v-model="runtimeByProvider(runtimeTarget.code).baseUrl" class="field" placeholder="例如 https://api.deepseek.com/v1" />
      </label>
      <label class="grid gap-2">
        <span class="text-xs font-semibold text-slate-600">API Key</span>
        <input v-model="runtimeByProvider(runtimeTarget.code).apiKey" type="password" class="field" placeholder="sk-***" />
      </label>
      <label class="grid gap-2">
        <span class="text-xs font-semibold text-slate-600">超时(ms)</span>
        <input v-model="runtimeByProvider(runtimeTarget.code).timeoutMs" class="field" placeholder="6000" />
      </label>
      <label class="grid gap-2">
        <span class="text-xs font-semibold text-slate-600">温度</span>
        <input v-model="runtimeByProvider(runtimeTarget.code).temperature" class="field" placeholder="0" />
      </label>
      <div class="mt-2 flex justify-end gap-2">
        <button class="btn btn-ghost" @click="runtimeModal = false">取消</button>
        <button
          class="btn btn-primary"
          :disabled="providerSaving[(runtimeTarget.code || '').toLowerCase()]"
          @click="saveRuntimeModal"
        >
          保存
        </button>
      </div>
    </div>
  </AppModal>

  <AppModal :open="sortModal" title="排序项配置" @close="sortModal = false">
    <div class="grid gap-3">
      <input v-model="sortForm.code" class="field" placeholder="Code，例如 HOT" />
      <input v-model="sortForm.name" class="field" placeholder="名称，例如 热门" />
      <input v-model.number="sortForm.sortNo" class="field" type="number" placeholder="排序值" />
      <div class="mt-2 flex justify-end gap-2">
        <button class="btn btn-ghost" @click="sortModal = false">取消</button>
        <button class="btn btn-primary" @click="saveSort">保存</button>
      </div>
    </div>
  </AppModal>
</template>
