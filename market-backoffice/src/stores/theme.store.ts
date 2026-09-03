import { defineStore } from 'pinia'

export type Tema = 'claro' | 'oscuro'

const STORAGE_KEY = 'inven365-tema'

/**
 * Nunca sigue `prefers-color-scheme` del sistema operativo a propósito — el
 * cliente pidió que el backoffice siempre abra en claro por defecto, y que
 * cada usuario elija oscuro si quiere (guardado en su propio navegador).
 */
function temaGuardado(): Tema {
  if (typeof window === 'undefined') return 'claro'
  return window.localStorage.getItem(STORAGE_KEY) === 'oscuro' ? 'oscuro' : 'claro'
}

export const useThemeStore = defineStore('theme', {
  state: () => ({
    tema: temaGuardado() as Tema,
  }),
  actions: {
    alternar() {
      this.tema = this.tema === 'claro' ? 'oscuro' : 'claro'
      this.aplicar()
    },
    aplicar() {
      document.documentElement.setAttribute('data-theme', this.tema === 'oscuro' ? 'dark' : 'light')
      window.localStorage.setItem(STORAGE_KEY, this.tema)
    },
  },
})
