<script setup lang="ts">
export type EstadoBadgeVariant = 'success' | 'pending' | 'overdue' | 'danger' | 'info' | 'neutral'

defineProps<{
  variant: EstadoBadgeVariant
  label: string
}>()
</script>

<template>
  <span class="mk-badge" :class="`mk-badge-${variant}`">{{ label }}</span>
</template>
