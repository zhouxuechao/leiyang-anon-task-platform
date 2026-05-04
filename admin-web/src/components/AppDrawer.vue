<script setup lang="ts">
defineProps<{
  open: boolean;
  title: string;
}>();

defineEmits<{
  (e: "close"): void;
}>();
</script>

<template>
  <teleport to="body">
    <transition name="backdrop">
      <div v-if="open" class="fixed inset-0 z-40 bg-black/30 backdrop-blur-sm" @click="$emit('close')" />
    </transition>
    <transition name="drawer">
      <div v-if="open" class="fixed inset-y-0 right-0 z-50 w-full max-w-xl">
        <div class="h-full glass border-l border-slate-200/80 p-5 shadow-lift">
          <div class="flex items-start justify-between gap-3">
            <div class="font-[650] text-slate-900">{{ title }}</div>
            <button class="btn btn-ghost px-3" @click="$emit('close')">关闭</button>
          </div>
          <div class="mt-4 h-[calc(100%-56px)] overflow-auto pr-1">
            <slot />
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<style scoped>
.backdrop-enter-active,
.backdrop-leave-active {
  transition: opacity 160ms ease;
}
.backdrop-enter-from,
.backdrop-leave-to {
  opacity: 0;
}
.drawer-enter-active,
.drawer-leave-active {
  transition: transform 200ms ease, opacity 200ms ease;
}
.drawer-enter-from,
.drawer-leave-to {
  transform: translateX(12px);
  opacity: 0;
}
</style>

