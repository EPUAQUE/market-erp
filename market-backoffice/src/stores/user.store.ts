import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    username: null as string | null,
  }),
  actions: {
    setUsername(username: string) {
      this.username = username
    },
    clear() {
      this.username = null
    },
  },
})
