import { createApp } from 'vue'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import 'leaflet/dist/leaflet.css'
import '@fontsource/inter/400.css'
import '@fontsource/inter/500.css'
import '@fontsource/inter/600.css'
import '@fontsource/inter/700.css'
import '@fontsource/jetbrains-mono/400.css'
import './stil/token-uri.css'
import App from './App.vue'
import { router } from './router.js'
import { seteazaLaNeautorizat } from './api.js'
import { temaFlota } from './tema.js'

// La un 401, api.js a uitat deja token-ul; aici omul e dus la login, cu pagina în care era, ca să revină în ea.
seteazaLaNeautorizat(() => {
  const acum = router.currentRoute.value
  if (!acum.meta.public) router.push({ path: '/login', query: { inapoi: acum.fullPath } })
})

// Toate componentele Vuetify, fără plugin de Vite: aplicația e mică, iar setarea rămâne simplă.
const vuetify = createVuetify({
  components,
  directives,
  // Tema e setată explicit: altfel Vuetify urmează tema întunecată din Chrome, iar paginile ies pe jumătate negre.
  theme: { defaultTheme: 'flota', themes: { flota: temaFlota } },
  // Singurul prag de telefon din aplicație; CSS-ul folosește același, @media (max-width: 999px).
  display: { mobileBreakpoint: 1000 },
})

createApp(App).use(router).use(vuetify).mount('#app')
