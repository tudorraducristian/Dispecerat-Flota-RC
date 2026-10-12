<template>
  <v-app>
    <v-app-bar color="primary" density="compact">
      <v-app-bar-title>Dispecerat flotă</v-app-bar-title>
      <template #append>
        <v-chip :color="flota.conectat ? 'success' : 'error'" variant="flat" size="small" class="mr-2">
          {{ flota.conectat ? 'live' : 'fără legătură cu serverul' }}
        </v-chip>
      </template>
    </v-app-bar>
    <v-main>
      <PaginaHarta />
    </v-main>
  </v-app>
</template>

<script>
import PaginaHarta from './pagini/PaginaHarta.vue'
import { flota, pornesteLive } from './live.js'

export default {
  name: 'App',
  components: { PaginaHarta },
  data() {
    return { flota }
  },
  mounted() {
    pornesteLive()
  },
}
</script>
