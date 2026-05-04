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
    <transition name="modal">
      <div v-if="open" class="fixed inset-0 z-50 grid place-items-center overflow-y-auto p-4">
        <div class="glass w-full max-w-lg max-h-[90vh] rounded-3xl p-5 shadow-lift" @click.stop>
          <div class="flex items-start justify-between gap-3">
            <div class="font-[650] text-slate-900" style="font-family: var(--font-display, inherit)">
              {{ title }}
            </div>
            <button class="btn btn-ghost px-3" @click="$emit('close')">关闭</button>
          </div>
          <div class="mt-4 max-h-[70vh] overflow-y-auto pr-1">
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
.modal-enter-active,
.modal-leave-active {
  transition: opacity 170ms ease, transform 170ms ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.99);
}
</style>
