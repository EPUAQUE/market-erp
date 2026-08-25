import { defineStore } from 'pinia';
export const useUserStore = defineStore('user', {
    state: () => ({
        username: null,
    }),
    actions: {
        setUsername(username) {
            this.username = username;
        },
        clear() {
            this.username = null;
        },
    },
});
