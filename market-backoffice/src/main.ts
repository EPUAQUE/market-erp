import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router'
import { useThemeStore } from './stores/theme.store'
import './styles/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// Confirma el atributo data-theme en <html> con el store como fuente de
// verdad — index.html ya lo adelantó de forma síncrona (antes de este
// bundle) solo para evitar el parpadeo en la recarga de un usuario que
// eligió oscuro.
useThemeStore().aplicar()

app.mount('#app')
