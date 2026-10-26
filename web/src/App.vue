<template>
  <v-app>
    <!-- Pe /login, doar pagina, fără bară. -->
    <v-main v-if="$route.meta.public">
      <router-view />
    </v-main>
    <template v-else>
      <v-app-bar color="primary" density="compact">
        <v-app-bar-title>Dispecerat flotă</v-app-bar-title>
        <v-btn to="/" exact>Harta</v-btn>
        <v-btn to="/retea">Rețea</v-btn>
        <v-btn to="/alarme">Alarme</v-btn>
        <template #append>
          <v-chip :color="flota.conectat ? 'success' : 'error'" variant="flat" size="small" class="mr-2">
            {{ flota.conectat ? 'live' : 'fără legătură cu serverul' }}
          </v-chip>
          <v-btn variant="text" @click="iesire">Ieșire</v-btn>
        </template>
      </v-app-bar>
      <v-main>
        <router-view />
      </v-main>
    </template>
    <v-snackbar v-model="notificare.vizibila" :timeout="6000" color="warning" location="top right">
      <strong>{{ notificare.masina }}</strong>: {{ notificare.tip }}<br />{{ notificare.detalii }}
      <template #actions>
        <v-btn variant="text" to="/alarme" @click="notificare.vizibila = false">Vezi</v-btn>
      </template>
    </v-snackbar>
  </v-app>
</template>

<script>
import { api } from './api.js'
import { flota, asculta, pornesteLive, opresteLive, pornesteVremea, opresteVremea } from './live.js'
import { sesiune, uita, asiguraUtilizatorul } from './sesiune.js'

export default {
  name: 'App',
  data() {
    return { flota, sesiune, notificare: { vizibila: false, masina: '', tip: '', detalii: '' } }
  },
  watch: {
    /** Canalul live și vremea merg doar cu token: pornesc la login și se opresc la ieșire. */
    'sesiune.token': {
      immediate: true,
      handler(token) {
        if (token) {
          pornesteLive()
          pornesteVremea()
          asiguraUtilizatorul().catch(() => {}) // la 401, api.js trimite deja la /login
        } else {
          opresteLive()
          opresteVremea()
        }
      },
    },
  },
  mounted() {
    // Alarmele noi apar ca notificare pe orice pagină (spec §7).
    asculta((tip, date) => {
      if (tip === 'ALARMA') {
        this.notificare = { vizibila: true, masina: date.masinaId, tip: date.tip, detalii: date.detalii }
      }
    })
  },
  methods: {
    /** Întâi canalul, ca închiderea lui de la server, cu 4001, să nu pară o cădere. */
    async iesire() {
      opresteLive()
      try {
        await api.logout()
      } catch {
        // Token-ul era deja expirat: oricum ieșim.
      }
      uita()
      this.$router.push('/login')
    },
  },
}
</script>
