<template>
  <v-container>
    <div class="d-flex align-center mb-4">
      <h2 class="text-h6">Alarme</h2>
      <v-spacer />
      <v-btn-toggle v-model="filtru" mandatory density="compact" @update:model-value="incarca">
        <v-btn value="nevazute">Nevăzute</v-btn>
        <v-btn value="vazute">Văzute</v-btn>
        <v-btn value="toate">Toate</v-btn>
      </v-btn-toggle>
    </div>
    <v-alert v-if="eroare" type="error" class="mb-4">{{ eroare }}</v-alert>
    <v-table density="compact">
      <thead>
        <tr><th>Ora mașinii</th><th>Mașina</th><th>Tip</th><th>Sursa</th><th>Detalii</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-for="a in alarme" :key="a.id">
          <td>{{ ora(a.ora) }}</td>
          <td>{{ a.masinaId }}</td>
          <td>{{ a.tip }}</td>
          <td>{{ a.sursa }}</td>
          <td>{{ a.detalii }}</td>
          <td><v-btn v-if="!a.vazuta" size="small" variant="text" @click="vazuta(a)">Am văzut</v-btn></td>
        </tr>
        <tr v-if="alarme.length === 0">
          <td colspan="6">Nicio alarmă.</td>
        </tr>
      </tbody>
    </v-table>
  </v-container>
</template>

<script>
import { api } from '../api.js'
import { asculta } from '../live.js'

const FILTRE = { nevazute: false, vazute: true, toate: null }

export default {
  name: 'PaginaAlarme',
  data() {
    return { filtru: 'nevazute', alarme: [], eroare: null }
  },
  async mounted() {
    this.opresteAscultarea = asculta((tip, date) => {
      if (tip === 'ALARMA' && this.filtru !== 'vazute') this.alarme.unshift(date)
    })
    await this.incarca()
  },
  beforeUnmount() {
    this.opresteAscultarea()
  },
  methods: {
    async incarca() {
      try {
        this.alarme = await api.alarme(FILTRE[this.filtru])
        this.eroare = null
      } catch (e) {
        this.eroare = e.message
      }
    },
    async vazuta(alarma) {
      const noua = await api.marcheazaVazuta(alarma.id)
      if (this.filtru === 'nevazute') {
        this.alarme = this.alarme.filter((a) => a.id !== alarma.id)
      } else {
        Object.assign(alarma, noua)
      }
    },
    ora(iso) {
      return new Date(iso).toLocaleString('ro-RO')
    },
  },
}
</script>
