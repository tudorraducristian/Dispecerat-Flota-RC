<template>
  <v-app>
    <!-- Pe /login, doar pagina, fără bară. -->
    <v-main v-if="$route.meta.public">
      <router-view />
    </v-main>
    <template v-else>
      <BaraSus @comuta-meniul="comutaMeniul" @iesire="iesire" />
      <MeniuLateral :deschis="meniu" @inchide="meniu = false" @iesire="iesire" />
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
import { asculta, pornesteLive, opresteLive, pornesteVremea, opresteVremea } from './live.js'
import { sesiune, uita, asiguraUtilizatorul } from './sesiune.js'
import { pornesteNumararea, opresteNumararea } from './numarAlarme.js'
import BaraSus from './components/BaraSus.vue'
import MeniuLateral from './components/MeniuLateral.vue'

/** Starea meniului de laptop rămâne după F5 (redesign §4.3); pe telefon sertarul pornește mereu închis. */
const CHEIE_MENIU = 'flota.meniu'

function meniulDeLaptop() {
  try {
    return localStorage.getItem(CHEIE_MENIU) !== 'strans'
  } catch {
    return true // stocarea blocată de browser
  }
}

export default {
  name: 'App',
  components: { BaraSus, MeniuLateral },
  data() {
    return {
      sesiune,
      meniu: this.$vuetify.display.mobile ? false : meniulDeLaptop(),
      notificare: { vizibila: false, masina: '', tip: '', detalii: '' },
    }
  },
  watch: {
    /** Canalul live, vremea și cifra de alarme merg doar cu token: pornesc la login și se opresc la ieșire. */
    'sesiune.token': {
      immediate: true,
      handler(token) {
        if (token) {
          pornesteLive()
          pornesteVremea()
          pornesteNumararea()
          asiguraUtilizatorul().catch(() => {}) // la 401, api.js trimite deja la /login
        } else {
          opresteLive()
          opresteVremea()
          opresteNumararea()
        }
      },
    },
    /** Peste prag, în orice sens, meniul ia starea potrivită: închis pe telefon, cea ținută minte pe laptop. */
    '$vuetify.display.mobile'(mobil) {
      this.meniu = mobil ? false : meniulDeLaptop()
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
    comutaMeniul() {
      this.meniu = !this.meniu
      if (this.$vuetify.display.mobile) return
      try {
        localStorage.setItem(CHEIE_MENIU, this.meniu ? 'deschis' : 'strans')
      } catch {
        // Rămâne așa doar până la reîncărcare.
      }
    },
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
