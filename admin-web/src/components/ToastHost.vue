<script setup lang="ts">
import { toasts } from "../lib/toast";
</script>

<template>
  <div class="pointer-events-none fixed right-4 top-4 z-50 flex w-[360px] flex-col gap-2">
    <transition-group name="toast" tag="div" class="flex flex-col gap-2">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="pointer-events-auto glass rounded-2xl px-4 py-3 shadow-soft"
      >
        <div class="flex items-start justify-between gap-3">
          <div>
            <div class="font-semibold text-slate-900">
              <span v-if="t.kind === 'ok'" class="mr-2 inline-block h-2 w-2 rounded-full bg-emerald-500" />
              <span v-else class="mr-2 inline-block h-2 w-2 rounded-full bg-red-500" />
              {{ t.title }}
            </div>
            <div v-if="t.detail" class="mt-1 text-sm text-slate-600">{{ t.detail }}</div>
          </div>
        </div>
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: opacity 160ms ease, transform 160ms ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>

