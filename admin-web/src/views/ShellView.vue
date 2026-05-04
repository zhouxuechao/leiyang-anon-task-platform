<script setup lang="ts">
import {
  BadgeCheck,
  Bell,
  Bot,
  FileSearch,
  Flag,
  LayoutDashboard,
  LogOut,
  PieChart,
  ListFilter,
  ShieldAlert,
  Settings,
  ScrollText,
  Users,
  Wallet,
  MessageSquare,
} from "lucide-vue-next";
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import ToastHost from "../components/ToastHost.vue";
import { clearToken } from "../state/auth";

const route = useRoute();
const router = useRouter();

const groups = [
  { title: "总览", items: [
    { to: "/dashboard", label: "仪表盘", icon: LayoutDashboard },
    { to: "/stats", label: "报表", icon: PieChart },
  ] },
  { title: "审核", items: [
    { to: "/tasks", label: "任务审核", icon: FileSearch },
    { to: "/orders", label: "凭证审核", icon: BadgeCheck },
    { to: "/withdraws", label: "提现审核", icon: Wallet },
    { to: "/reports", label: "举报处理", icon: Flag },
    { to: "/feedbacks", label: "反馈建议", icon: MessageSquare },
  ] },
  { title: "内容", items: [
    { to: "/task-manage", label: "任务列表", icon: ListFilter },
    { to: "/plaza-posts", label: "广场帖子", icon: MessageSquare },
    { to: "/plaza-config", label: "广场配置", icon: Bot },
    { to: "/notices", label: "公告", icon: Bell },
  ] },
  { title: "用户与资金", items: [
    { to: "/users", label: "用户", icon: Users },
  ] },
  { title: "AI", items: [
    { to: "/ai-comment-jobs", label: "评论队列", icon: MessageSquare },
    { to: "/ai-task-drafts", label: "AI任务生成", icon: Bot },
  ] },
  { title: "系统", items: [
    { to: "/sensitive-words", label: "敏感词", icon: ShieldAlert },
    { to: "/configs", label: "配置中心", icon: Settings },
    { to: "/op-logs", label: "日志", icon: ScrollText },
  ] },
];

const nav = groups.flatMap((g) => g.items);

const title = computed(() => {
  const hit = nav.find((n) => route.path.startsWith(n.to));
  return hit?.label || "控制台";
});

async function logout() {
  clearToken();
  await router.replace("/login");
}
</script>

<template>
  <div>
    <ToastHost />
    <div class="min-h-full">
      <div class="mx-auto flex w-full max-w-7xl gap-5 px-5 py-5">
        <aside class="hidden w-[280px] shrink-0 md:block">
          <div class="glass sticky top-5 rounded-3xl p-4 shadow-soft">
            <div class="px-2 py-1">
              <div class="text-xs font-semibold text-slate-600">Leiyang Ops</div>
              <div class="mt-1 text-xl font-[750] text-slate-900" style="font-family: 'Space Grotesk', sans-serif">
                匿名任务平台
              </div>
              <div class="mt-2 text-xs text-slate-500">审核闭环优先，治理先行</div>
            </div>

            <div class="mt-4 grid gap-3">
              <div v-for="g in groups" :key="g.title">
                <div class="px-3 pb-1 text-[11px] font-bold uppercase tracking-wide text-slate-400">{{ g.title }}</div>
                <div class="grid gap-1">
                  <router-link
                    v-for="n in g.items"
                    :key="n.to"
                    :to="n.to"
                    class="flex items-center gap-3 rounded-2xl px-3 py-2 text-sm font-semibold transition"
                    :class="
                      route.path.startsWith(n.to)
                        ? 'bg-slate-900 text-white shadow-soft'
                        : 'text-slate-700 hover:bg-white/70'
                    "
                  >
                    <component :is="n.icon" class="h-4 w-4" />
                    <span>{{ n.label }}</span>
                  </router-link>
                </div>
              </div>
            </div>

            <div class="mt-4 border-t border-slate-200/70 pt-3">
              <button class="btn btn-ghost w-full justify-between" @click="logout">
                <span class="inline-flex items-center gap-2">
                  <LogOut class="h-4 w-4" />
                  退出登录
                </span>
                <span class="text-xs text-slate-500">JWT</span>
              </button>
            </div>
          </div>
        </aside>

        <main class="min-w-0 flex-1">
          <div class="glass rounded-3xl px-5 py-4 shadow-soft">
            <div class="flex items-center justify-between gap-4">
              <div>
                <div class="text-sm font-semibold text-slate-600">管理后台</div>
                <div class="mt-1 text-2xl font-[750] text-slate-900" style="font-family: 'Space Grotesk', sans-serif">
                  {{ title }}
                </div>
              </div>
              <div class="hidden items-center gap-2 md:flex">
                <div class="chip">Spring Boot</div>
                <div class="chip">Flyway</div>
                <div class="chip">Audit First</div>
              </div>
            </div>
          </div>

          <div class="mt-5">
            <router-view />
          </div>
        </main>
      </div>
    </div>
  </div>
</template>
