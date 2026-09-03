<script setup lang="ts">
withDefaults(
  defineProps<{
    modelValue: boolean
    title: string
    /** Clase Tailwind de ancho máximo — formularios con líneas (Ventas, Compras, Traslados) necesitan más espacio. */
    maxWidth?: string
  }>(),
  { maxWidth: 'max-w-lg' },
)

const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()

function cerrar() {
  emit('update:modelValue', false)
}
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4">
      <div class="mk-card flex max-h-[90vh] w-full flex-col overflow-hidden bg-mk-surface" :class="maxWidth">
        <div class="flex items-center justify-between border-b border-mk-border px-5 py-4">
          <h2 class="text-base font-bold text-mk-text">{{ title }}</h2>
          <button
            type="button"
            class="rounded-md p-1 text-mk-text-muted hover:bg-mk-surface-2 hover:text-mk-text"
            aria-label="Cerrar"
            @click="cerrar"
          >
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="m6 6 12 12M18 6 6 18" stroke-linecap="round" />
            </svg>
          </button>
        </div>
        <div class="overflow-y-auto p-5">
          <slot />
        </div>
      </div>
    </div>
  </Teleport>
</template>
