<template>
  <v-app>
    <v-app-bar color="primary" density="compact">
      <v-app-bar-title>Dispecerat flotă</v-app-bar-title>
      <v-btn to="/" exact>Harta</v-btn>
      <v-btn to="/retea">Rețea</v-btn>
      <v-btn to="/alarme">Alarme</v-btn>
      <template #append>
        <v-chip :color="flota.conectat ? 'success' : 'error'" variant="flat" size="small" class="mr-2">
          {{ flota.conectat ? 'live' : 'fără legătură cu serverul' }}
        </v-chip>
      </template>
    </v-app-bar>
    <v-main>
      <router-view />
    </v-main>
    <v-snackbar v-model="notificare.vizibila" :timeout="6000" color="warning" location="top right">
      <strong>{{ notificare.masina }}</strong>: {{ notificare.tip }}<br />{{ notificare.detalii }}
      <template #actions>
        <v-btn variant="text" to="/alarme" @click="notificare.vizibila = false">Vezi</v-btn>
      </template>
    </v-snackbar>
  </v-app>
</template>

<script>
import { flota, asculta, pornesteLive, pornesteVremea } from './live.js'

export default {
  name: 'App',
  data() {
    return { flota, notificare: { vizibila: false, masina: '', tip: '', detalii: '' } }
  },
  mounted() {
    pornesteLive()
    pornesteVremea()
    // Alarmele noi apar ca notificare pe orice pagină (spec §7).
    asculta((tip, date) => {
      if (tip === 'ALARMA') {
        this.notificare = { vizibila: true, masina: date.masinaId, tip: date.tip, detalii: date.detalii }
      }
    })
  },
}
</script>
