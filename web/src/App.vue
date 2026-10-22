<template>
  <v-app>
    <v-app-bar color="primary" density="compact">
      <v-app-bar-title>Dispecerat flotă</v-app-bar-title>
      <v-btn to="/" exact>Harta</v-btn>
      <v-btn to="/retea">Rețea</v-btn>
      <template #append>
        <v-chip :color="flota.conectat ? 'success' : 'error'" variant="flat" size="small" class="mr-2">
          {{ flota.conectat ? 'live' : 'fără legătură cu serverul' }}
        </v-chip>
      </template>
    </v-app-bar>
    <v-main>
      <router-view />
    </v-main>
  </v-app>
</template>

<script>
import { flota, pornesteLive, pornesteVremea } from './live.js'

export default {
  name: 'App',
  data() {
    return { flota }
  },
  mounted() {
    pornesteLive()
    pornesteVremea()
  },
}
</script>
