<template>
  <v-card v-if="masina" variant="outlined" class="ma-2">
    <v-card-title class="d-flex align-center">
      {{ masina.id }}
      <v-spacer />
      <v-chip :color="masina.stare === 'ONLINE' ? 'success' : 'grey'" size="small" variant="flat">
        {{ masina.stare === 'ONLINE' ? 'online' : 'offline' }}
      </v-chip>
    </v-card-title>
    <v-card-subtitle>zona {{ masina.zonaId }}</v-card-subtitle>
    <v-card-text>
      <v-table density="compact">
        <tbody>
          <tr><td>Viteza</td><td>{{ valoare('viteza', 1) }} km/h</td></tr>
          <tr><td>Temperatura</td><td>{{ valoare('temperatura', 1) }} °C</td></tr>
          <tr><td>Bateria</td><td>{{ valoare('baterie', 0) }}%</td></tr>
          <tr><td>Recuperate</td><td>{{ masina.recuperate }}</td></tr>
          <tr><td>Pierdute</td><td>{{ masina.pierdute }}</td></tr>
          <tr><td>Văzută ultima dată</td><td>{{ ora(masina.ultimaVedere) }}</td></tr>
        </tbody>
      </v-table>
      <div class="text-caption mt-2">Linie plină: live · punctată: recuperat din buffer · lipsă: pierdut</div>
    </v-card-text>
    <v-card-actions>
      <v-btn variant="text" @click="$emit('inchide')">Închide</v-btn>
    </v-card-actions>
  </v-card>
</template>

<script>
import { actualizeazaContoare } from '../live.js'

export default {
  name: 'PanouMasina',
  props: {
    masina: { type: Object, default: null },
  },
  emits: ['inchide'],
  computed: {
    idMasina() {
      return this.masina ? this.masina.id : null
    },
  },
  watch: {
    idMasina: {
      immediate: true,
      handler(id) {
        clearInterval(this.cronometru)
        if (id) {
          this.cronometru = setInterval(() => actualizeazaContoare(id).catch(() => {}), 2000)
        }
      },
    },
  },
  beforeUnmount() {
    clearInterval(this.cronometru)
  },
  methods: {
    valoare(camp, zecimale) {
      const p = this.masina.pozitie
      return p ? p[camp].toFixed(zecimale) : '—'
    },
    ora(iso) {
      return iso ? new Date(iso).toLocaleTimeString('ro-RO') : '—'
    },
  },
}
</script>
