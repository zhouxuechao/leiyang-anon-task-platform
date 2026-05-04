<script setup lang="ts">
import { LockKeyhole, User } from "lucide-vue-next";
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppCard from "../components/AppCard.vue";
import ToastHost from "../components/ToastHost.vue";
import { api } from "../lib/api";
import { toastErr, toastOk } from "../lib/toast";
import { setToken } from "../state/auth";

const route = useRoute();
const router = useRouter();

const username = ref("");
const password = ref("");
const loading = ref(false);

async function login() {
  try {
    loading.value = true;
    const data = await api<{ token: string }>("/api/admin/auth/login", {
      method: "POST",
      body: { username: username.value, password: password.value },
    });
    setToken(data.token);
    toastOk("登录成功");
    const next = typeof route.query.next === "string" ? route.query.next : "/dashboard";
    await router.replace(next);
  } catch (e: any) {
    toastErr("登录失败", e?.message || "请检查账号密码");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div>
    <ToastHost />
    <div class="min-h-full">
      <div class="mx-auto flex min-h-full w-full max-w-5xl items-center px-6 py-14">
        <div class="grid w-full grid-cols-1 gap-8 md:grid-cols-2">
          <div class="px-2">
            <div class="text-sm font-semibold text-slate-600">Leiyang Ops Console</div>
            <div class="mt-3 text-4xl font-[750] leading-tight text-slate-900" style="font-family: 'Space Grotesk', sans-serif">
              审核、结算、治理
            </div>
            <div class="mt-4 text-sm text-slate-600">
              任务审核、凭证审核、提现审核、举报处理、敏感词维护与配置管理。
            </div>
            <div class="mt-8 flex items-center gap-3 text-xs text-slate-500">
              <span class="inline-block h-2 w-2 rounded-full bg-cyan-500"></span>
              <span>生产环境请使用独立管理员账号，并定期更换密码。</span>
            </div>
          </div>

          <AppCard>
            <div class="flex items-center justify-between">
              <div class="text-lg font-semibold text-slate-900">管理员登录</div>
              <div class="chip">JWT</div>
            </div>

            <div class="mt-5 grid gap-3">
              <label class="text-xs font-semibold text-slate-600">用户名</label>
              <div class="relative">
                <User class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input v-model="username" class="field pl-9" placeholder="admin" autocomplete="username" />
              </div>

              <label class="mt-2 text-xs font-semibold text-slate-600">密码</label>
              <div class="relative">
                <LockKeyhole class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  v-model="password"
                  class="field pl-9"
                  placeholder="请输入管理员密码"
                  type="password"
                  autocomplete="current-password"
                  @keydown.enter="login"
                />
              </div>

              <button class="btn btn-primary mt-3 w-full" :disabled="loading" @click="login">
                <span v-if="loading">登录中...</span>
                <span v-else>进入控制台</span>
              </button>

              <div class="mt-2 text-xs text-slate-500">请勿在公共设备保存管理员登录状态。</div>
            </div>
          </AppCard>
        </div>
      </div>
    </div>
  </div>
</template>
