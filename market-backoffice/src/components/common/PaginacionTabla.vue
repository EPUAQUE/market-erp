<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  pagina: number
  totalPaginas: number
}>()

const emit = defineEmits<{ (e: 'update:pagina', value: number): void }>()

function ir(destino: number) {
  if (destino < 1 || destino > props.totalPaginas || destino === props.pagina) return
  emit('update:pagina', destino)
}

/**
 * Igual que la propuesta de diseño: números de página en vez de solo
 * "Anterior/Siguiente" — con muchas páginas no se listan todas, se muestra
 * la primera, la última, una ventana alrededor de la actual, y "…" donde
 * hay huecos.
 */
const slots = computed<(number | '…')[]>(() => {
  const total = props.totalPaginas
  const actual = props.pagina
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const paginas = new Set<number>([1, total, actual - 1, actual, actual + 1])
  const ordenadas = [...paginas].filter((p) => p >= 1 && p <= total).sort((a, b) => a - b)
  const resultado: (number | '…')[] = []
  ordenadas.forEach((p, i) => {
    if (i > 0 && p - ordenadas[i - 1] > 1) resultado.push('…')
    resultado.push(p)
  })
  return resultado
})
</script>

<template>
  <div class="flex items-center gap-1">
    <button type="button" class="mk-pager-btn" title="Primera página" :disabled="pagina <= 1" @click="ir(1)">
      «
    </button>
    <button
      type="button"
      class="mk-pager-btn"
      title="Anterior"
      :disabled="pagina <= 1"
      @click="ir(pagina - 1)"
    >
      ‹
    </button>
    <template v-for="(slot, i) in slots" :key="i">
      <span v-if="slot === '…'" class="px-1 text-mk-text-muted">…</span>
      <button
        v-else
        type="button"
        class="mk-pager-btn"
        :class="{ 'mk-pager-btn-activo': slot === pagina }"
        @click="ir(slot)"
      >
        {{ slot }}
      </button>
    </template>
    <button
      type="button"
      class="mk-pager-btn"
      title="Siguiente"
      :disabled="pagina >= totalPaginas"
      @click="ir(pagina + 1)"
    >
      ›
    </button>
    <button
      type="button"
      class="mk-pager-btn"
      title="Última página"
      :disabled="pagina >= totalPaginas"
      @click="ir(totalPaginas)"
    >
      »
    </button>
  </div>
</template>
