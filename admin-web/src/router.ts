import { createRouter, createWebHistory } from "vue-router";
import { getToken } from "./state/auth";

const routes = [
  { path: "/", redirect: "/dashboard" },
  {
    path: "/login",
    component: () => import("./views/LoginView.vue"),
    meta: { public: true },
  },
  {
    path: "/",
    component: () => import("./views/ShellView.vue"),
    children: [
      { path: "dashboard", component: () => import("./views/DashboardView.vue") },
      { path: "stats", component: () => import("./views/StatsView.vue") },
      { path: "tasks", component: () => import("./views/TasksAuditView.vue") },
      { path: "task-manage", component: () => import("./views/TaskManageView.vue") },
      { path: "orders", component: () => import("./views/OrdersAuditView.vue") },
      { path: "withdraws", component: () => import("./views/WithdrawsView.vue") },
      { path: "reports", component: () => import("./views/ReportsView.vue") },
      { path: "feedbacks", component: () => import("./views/FeedbacksView.vue") },
      { path: "sensitive-words", component: () => import("./views/SensitiveWordsView.vue") },
      { path: "users", component: () => import("./views/UsersView.vue") },
      { path: "op-logs", component: () => import("./views/OpLogsView.vue") },
      { path: "notices", component: () => import("./views/NoticesView.vue") },
      { path: "configs", component: () => import("./views/ConfigsView.vue") },
      { path: "plaza-config", component: () => import("./views/PlazaConfigView.vue") },
      { path: "plaza-posts", component: () => import("./views/PlazaPostsView.vue") },
      { path: "ai-comment-jobs", component: () => import("./views/AiCommentJobsView.vue") },
      { path: "ai-task-drafts", component: () => import("./views/AiTaskDraftsView.vue") },
    ],
  },
];

export const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  if (to.meta.public) return true;
  if (to.path === "/login") return true;
  const token = getToken();
  if (!token) return { path: "/login", query: { next: to.fullPath } };
  return true;
});
