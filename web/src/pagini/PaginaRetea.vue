<template>
  <v-container fluid>
    <v-row>
      <v-col cols="12" md="5">
        <v-card variant="outlined">
          <v-card-title>Vremea din rețea</v-card-title>
          <v-card-text>
            <v-select v-model="tinta" :items="tinte" label="Pentru" density="compact" />

            <div class="d-flex ga-2 align-center">
              <v-text-field v-model.number="secunde" type="number" label="Secunde (1–600)" density="compact" hide-details />
              <v-btn color="error" @click="trimite('TUNEL', { secunde })">Tunel</v-btn>
              <v-btn color="warning" @click="trimite('GAURA', { secunde })">Gaură</v-btn>
            </div>

            <v-slider v-model="ms" :min="0" :max="3000" :step="50" label="Întârziere, ms" thumb-label class="mt-4" />
            <!-- se taie întâi (spec §8): variația -->
            <v-slider v-model="variatie" :min="0" :max="1000" :step="50" label="Variație, ms" thumb-label />
            <v-btn @click="trimite('INTARZIERE', { ms, variatie })">Aplică întârzierea</v-btn>

            <!-- se taie întâi (spec §8): banda -->
            <div class="d-flex ga-2 align-center mt-4">
              <v-text-field v-model.number="kbps" type="number" label="Bandă, kbps (1–100.000)" density="compact" hide-details />
              <v-btn @click="trimite('BANDA', { kbps })">Aplică banda</v-btn>
            </div>

            <v-btn color="success" class="mt-4" block @click="trimite('NORMAL', {})">Normal</v-btn>
            <v-alert v-if="raspuns" :type="raspuns.ok ? 'success' : 'error'" density="compact" class="mt-4">
              {{ raspuns.text }}
            </v-alert>
          </v-card-text>
        </v-card>
        <v-card variant="outlined" class="mt-4">
          <v-card-title>Latența medie a flotei, ultimele 60 de secunde</v-card-title>
          <v-card-text>
            <GraficLatenta :valori="flota.latentaFlotei" />
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="7">
        <v-card variant="outlined">
          <v-card-title class="d-flex align-center">
            Starea proxy-ului
            <v-spacer />
            <v-chip :color="flota.proxyDisponibil ? 'success' : 'error'" size="small" variant="flat">
              {{ flota.proxyDisponibil ? 'proxy disponibil' : 'proxy indisponibil' }}
            </v-chip>
          </v-card-title>
          <v-table density="compact">
            <thead>
              <tr><th>Mașina</th><th>Vremea</th></tr>
            </thead>
            <tbody>
              <tr v-for="(descriere, masina) in flota.vreme" :key="masina">
                <td>{{ masina }}</td>
                <td>{{ descriere }}</td>
              </tr>
            </tbody>
          </v-table>
        </v-card>

        <v-card variant="outlined" class="mt-4">
          <v-card-title>Live, pe mașină</v-card-title>
          <v-table density="compact">
            <thead>
              <tr><th>Mașina</th><th>Mesaje/s</th><th>Latența, ms</th><th>Recuperate</th><th>Pierdute</th></tr>
            </thead>
            <tbody>
              <tr v-for="s in flota.statistici" :key="s.masinaId">
                <td>{{ s.masinaId }}</td>
                <td>{{ s.mesajePeSecunda.toFixed(1) }}</td>
                <td>{{ s.latentaMedieMs == null ? '—' : Math.round(s.latentaMedieMs) }}</td>
                <td>{{ s.recuperate }}</td>
                <td>{{ s.pierdute }}</td>
              </tr>
            </tbody>
          </v-table>
        </v-card>
        <v-card variant="outlined" class="mt-4">
          <v-card-title>Jurnalul rețelei</v-card-title>
          <v-table density="compact">
            <thead>
              <tr><th>Ora</th><th>Linia trimisă</th><th>Răspunsul</th></tr>
            </thead>
            <tbody>
              <tr v-for="e in evenimente" :key="e.id">
                <td>{{ ora(e.oraStart) }}</td>
                <td><code>{{ e.linie }}</code></td>
                <td>{{ e.raspuns }}</td>
              </tr>
            </tbody>
          </v-table>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import { api } from '../api.js'
import GraficLatenta from '../components/GraficLatenta.vue'
import { flota, asculta } from '../live.js'

export default {
  name: 'PaginaRetea',
  components: { GraficLatenta },
  data() {
    return { flota, tinta: 'TOATE', secunde: 15, ms: 500, variatie: 200, kbps: 64, raspuns: null, evenimente: [] }
  },
  computed: {
    tinte() {
      const masini = Object.keys(flota.masini).sort((a, b) => Number(a.split('-')[1]) - Number(b.split('-')[1]))
      return ['TOATE', ...masini]
    },
  },
  async mounted() {
    this.opresteAscultarea = asculta((tip, date) => {
      if (tip === 'EVENIMENT_RETEA') this.evenimente.unshift(date)
    })
    this.evenimente = await api.evenimente().catch(() => [])
  },
  beforeUnmount() {
    this.opresteAscultarea()
  },
  methods: {
    /** Trimite o cerere de vreme. OK sau ERR vin de la proxy; o eroare HTTP înseamnă proxy indisponibil. */
    async trimite(comanda, parametri) {
      this.raspuns = null
      try {
        const e = await api.vreme({ comanda, tinta: this.tinta, parametri })
        this.raspuns = { ok: e.raspuns === 'OK', text: `${e.linie} → ${e.raspuns}` }
      } catch (err) {
        this.raspuns = { ok: false, text: err.message }
      }
    },
    ora(iso) {
      return new Date(iso).toLocaleTimeString('ro-RO')
    },
  },
}
</script>
