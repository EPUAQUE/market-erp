<script setup lang="ts">
defineProps<{ name: string }>()
</script>

<template>
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="2"
    stroke-linecap="round"
    stroke-linejoin="round"
  >
    <template v-if="name === 'edit'">
      <path d="M12 20h9" />
      <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" />
    </template>
    <template v-else-if="name === 'power'">
      <path d="M12 3v7" />
      <path d="M18.4 6.6a8 8 0 1 1-12.8 0" />
    </template>
    <template v-else-if="name === 'eye'">
      <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7Z" />
      <circle cx="12" cy="12" r="3" />
    </template>
    <template v-else-if="name === 'x'">
      <path d="M3 6h18M8 6V4h8v2m-1 0v14a1 1 0 0 1-1 1H10a1 1 0 0 1-1-1V6" />
    </template>
    <template v-else-if="name === 'check'">
      <path d="m20 6-11 11-5-5" />
    </template>
    <template v-else-if="name === 'refresh'">
      <path d="M21 12a9 9 0 1 1-3-6.7" />
      <path d="M21 3v6h-6" />
    </template>
    <template v-else-if="name === 'cash'">
      <rect x="2" y="6" width="20" height="12" rx="2" />
      <circle cx="12" cy="12" r="3" />
      <path d="M6 10v0M18 14v0" />
    </template>
    <template v-else-if="name === 'store'">
      <path d="M4 10V21h16V10" />
      <path d="M2.5 10 4 4h16l1.5 6z" />
      <path d="M9 21v-6h6v6" />
    </template>
    <template v-else-if="name === 'key'">
      <circle cx="8" cy="15" r="4" />
      <path d="m10.8 12.2 8.7-8.7M16 7l2 2M19 4l2 2" />
    </template>
  </svg>
</template>
