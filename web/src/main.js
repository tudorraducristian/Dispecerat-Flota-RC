import { createApp } from 'vue'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import 'leaflet/dist/leaflet.css'
import App from './App.vue'
import { router } from './router.js'

// Toate componentele Vuetify, fără plugin de Vite: aplicația e mică, iar setarea rămâne simplă.
createApp(App).use(router).use(createVuetify({ components, directives })).mount('#app')
