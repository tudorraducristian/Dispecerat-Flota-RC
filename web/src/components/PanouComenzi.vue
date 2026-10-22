<template>
  <v-card variant="outlined" class="ma-2">
    <v-card-title>Comenzi</v-card-title>
    <v-card-text>
      <v-select v-model="tip" :items="TIPURI" item-title="titlu" item-value="valoare" label="Comanda" density="compact" />
      <v-text-field v-if="tip === 'LIMITEAZA_VITEZA'" v-model.number="kmh" type="number" label="km/h (10–130)" density="compact" />
      <v-text-field v-if="tip === 'MESAJ_SOFER'" v-model="text" label="Mesajul" counter="200" density="compact" />
      <v-btn color="primary" :loading="seTrimite" @click="trimite">Trimite</v-btn>
      <v-alert v-if="eroare" type="error" density="compact" class="mt-2">{{ eroare }}</v-alert>

      <v-list density="compact" class="mt-2">
        <v-list-item v-for="c in comenzi" :key="c.id" :title="descriere(c)" :subtitle="ora(c.oraTrimiterii)">
          <template #append>
            <v-chip :color="stareAfisata(c).culoare" size="small" variant="flat">{{ stareAfisata(c).text }}</v-chip>
          </template>
        </v-list-item>
      </v-list>
    </v-card-text>
  </v-card>
</template>

<script>
import { api } from '../api.js'
import { asculta } from '../live.js'

const TIPURI = [
  { titlu: 'Limitează viteza', valoare: 'LIMITEAZA_VITEZA' },
  { titlu: 'Întoarce la bază', valoare: 'INTOARCE_LA_BAZA' },
  { titlu: 'Mesaj pentru șofer', valoare: 'MESAJ_SOFER' },
]

export default {
  name: 'PanouComenzi',
  props: {
    masina: { type: Object, required: true },
  },
  data() {
    return { TIPURI, tip: 'LIMITEAZA_VITEZA', kmh: 50, text: '', comenzi: [], seTrimite: false, eroare: null }
  },
  computed: {
    idMasina() {
      return this.masina.id
    },
  },
  watch: {
    idMasina: {
      immediate: true,
      handler() {
        this.incarca()
      },
    },
  },
  mounted() {
    this.opresteAscultarea = asculta((tip, date) => {
      if (tip === 'CONFIRMARE' && date.masinaId === this.masina.id) this.inlocuieste(date)
    })
  },
  beforeUnmount() {
    this.opresteAscultarea()
  },
  methods: {
    async incarca() {
      const id = this.masina.id
      this.comenzi = []
      this.eroare = null
      const lista = await api.comenzi(id).catch(() => [])
      if (id === this.masina.id) this.comenzi = lista
    },
    async trimite() {
      this.eroare = null
      this.seTrimite = true
      try {
        const parametri =
          this.tip === 'LIMITEAZA_VITEZA' ? { kmh: this.kmh } : this.tip === 'MESAJ_SOFER' ? { text: this.text } : {}
        this.inlocuieste(await api.trimiteComanda(this.masina.id, { tip: this.tip, parametri }))
      } catch (e) {
        this.eroare = e.message
      } finally {
        this.seTrimite = false
      }
    },
    /**
     * Pune comanda în listă sau o înlocuiește pe cea cu același id. Confirmarea poate sosi înaintea
     * răspunsului la POST, așa că o comandă deja confirmată nu se mai întoarce la TRIMISA.
     */
    inlocuieste(comanda) {
      const i = this.comenzi.findIndex((c) => c.id === comanda.id)
      if (i < 0) this.comenzi.unshift(comanda)
      else if (this.comenzi[i].stare === 'TRIMISA') this.comenzi.splice(i, 1, comanda)
    },
    /** „trimisă” cât mașina e online; „așteaptă mașina” cât e offline: broker-ul o păstrează (spec §7). */
    stareAfisata(c) {
      if (c.stare === 'CONFIRMATA') return { text: 'confirmată', culoare: 'success' }
      if (c.stare === 'RESPINSA') return { text: `respinsă: ${c.motiv}`, culoare: 'error' }
      return this.masina.stare === 'ONLINE'
        ? { text: 'trimisă', culoare: 'info' }
        : { text: 'așteaptă mașina', culoare: 'warning' }
    },
    descriere(c) {
      if (c.tip === 'LIMITEAZA_VITEZA') return `Limitează la ${c.parametri.kmh} km/h`
      if (c.tip === 'MESAJ_SOFER') return `Mesaj: ${c.parametri.text}`
      return 'Întoarce la bază'
    },
    ora(iso) {
      return iso ? new Date(iso).toLocaleTimeString('ro-RO') : ''
    },
  },
}
</script>
