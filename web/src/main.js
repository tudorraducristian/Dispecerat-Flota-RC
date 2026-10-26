import { createApp } from 'vue'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import 'leaflet/dist/leaflet.css'
import App from './App.vue'
import { router } from './router.js'
import { seteazaLaNeautorizat } from './api.js'

// La un 401, api.js a uitat deja token-ul; aici omul e dus la login, cu pagina în care era, ca să revină în ea.
seteazaLaNeautorizat(() => {
  const acum = router.currentRoute.value
  if (!acum.meta.public) router.push({ path: '/login', query: { inapoi: acum.fullPath } })
})

// Toate componentele Vuetify, fără plugin de Vite: aplicația e mică, iar setarea rămâne simplă.
createApp(App).use(router).use(createVuetify({ components, directives })).mount('#app')
