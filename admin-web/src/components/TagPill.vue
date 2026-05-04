<script setup lang="ts">
defineProps<{
  tone?: "ok" | "warn" | "err" | "info";
  text: string;
}>();
</script>

<template>
  <span
    class="inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold"
    :class="{
      'border-emerald-200 bg-emerald-50 text-emerald-700': tone === 'ok',
      'border-amber-200 bg-amber-50 text-amber-700': tone === 'warn',
      'border-red-200 bg-red-50 text-red-700': tone === 'err',
      'border-sky-200 bg-sky-50 text-sky-700': !tone || tone === 'info',
    }"
    >{{ text }}</span
  >
</template>

