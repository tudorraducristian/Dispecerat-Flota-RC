<template>
  <div class="panou-vreme" :class="{ mobil }">
    <div class="sus">
      <h2 v-if="!mobil" class="titlu">Strică rețeaua</h2>

      <div class="eticheta-mica">Pentru</div>
      <v-select
        v-model="tinta"
        :items="tinte"
        item-title="titlu"
        item-value="valoare"
        density="compact"
        variant="outlined"
        hide-details
        aria-label="Pentru"
      />

      <div class="placi" role="radiogroup" aria-label="Ce strici">
        <button
          v-for="p in PLACI"
          :key="p.comanda"
          type="button"
          role="radio"
          class="placa"
          :class="[`ton-${p.ton}`, { aleasa: ales === p.comanda }]"
          :aria-checked="ales === p.comanda"
          @click="ales = p.comanda"
        >
          <span class="iconita"><component :is="p.iconita" :size="16" /></span>
          <b>{{ p.nume }}</b>
          <small v-if="!mobil">{{ p.explicatie }}</small>
        </button>
      </div>
      <p v-if="mobil" class="explicatie">{{ placa.nume }}: {{ placa.explicatie.toLowerCase() }}.</p>

      <div class="campuri">
        <label v-if="ales === 'TUNEL' || ales === 'GAURA'" class="camp">
          <span class="eticheta-camp">Secunde, între 1 și 600</span>
          <span class="intrare"><input v-model.number="secunde" type="number" min="1" max="600" inputmode="numeric" class="cifre" /><small>s</small></span>
        </label>
        <template v-else-if="ales === 'INTARZIERE'">
          <v-slider v-model="ms" :min="0" :max="3000" :step="50" label="Întârziere, ms" thumb-label color="warning" hide-details />
          <v-slider v-model="variatie" :min="0" :max="1000" :step="50" label="Variație, ms" thumb-label color="warning" hide-details />
        </template>
        <label v-else class="camp">
          <span class="eticheta-camp">Banda, între 1 și 100.000 kbps</span>
          <span class="intrare"><input v-model.number="kbps" type="number" min="1" max="100000" inputmode="numeric" class="cifre" /><small>kbps</small></span>
        </label>
      </div>
    </div>

    <!-- Pe telefon, partea asta stă jos, la degetul mare, iar cea de sus se derulează (redesign §10.5). -->
    <div class="jos">
      <div v-if="raspuns" class="raspuns" :class="raspuns.ok ? 'ok' : 'eroare'" role="status">{{ raspuns.text }}</div>
      <button type="button" class="buton-placa" :class="`ton-${placa.ton}`" :disabled="seTrimite" @click="trimite(ales, parametri)">
        {{ placa.buton }}
      </button>
      <button type="button" class="normal" :disabled="seTrimite" @click="trimite('NORMAL', {}, 'TOATE')">
        <CircleCheck :size="15" />Rețea normală pentru toată flota
      </button>
    </div>
  </div>
</template>

<script>
import { Ban, CircleCheck, Mountain, SignalLow, Timer } from 'lucide-vue-next'
import { api } from '../api.js'
import { flota } from '../live.js'
import { numeTinta } from '../text.js'

const PLACI = [
  { comanda: 'TUNEL', nume: 'Tunel', explicatie: 'Conexiunea cade, mașina ține mesajele', ton: 'rosu', iconita: Mountain, buton: 'Pornește tunelul' },
  { comanda: 'GAURA', nume: 'Gaură', explicatie: 'Mesajele se pierd pe drum', ton: 'bleumarin', iconita: Ban, buton: 'Deschide gaura' },
  { comanda: 'INTARZIERE', nume: 'Întârziere', explicatie: 'Fiecare mesaj stă pe loc', ton: 'ambra', iconita: Timer, buton: 'Aplică întârzierea' },
  { comanda: 'BANDA', nume: 'Bandă îngustă', explicatie: 'Puțini kilobiți pe secundă', ton: 'ambra', iconita: SignalLow, buton: 'Îngustează banda' },
]

/** Panoul „Strică rețeaua” de pe Rețea (redesign §4.6, varianta 1; pe telefon §10.5, fila Comenzi). */
export default {
  name: 'PanouVreme',
  components: { CircleCheck },
  emits: ['trimis'],
  data() {
    return { PLACI, tinta: 'TOATE', ales: 'TUNEL', secunde: 15, ms: 500, variatie: 200, kbps: 64, raspuns: null, seTrimite: false }
  },
  computed: {
    mobil() {
      return this.$vuetify.display.mobile
    },
    tinte() {
      const masini = Object.keys(flota.masini).sort((a, b) => Number(a.split('-')[1]) - Number(b.split('-')[1]))
      return ['TOATE', ...masini].map((valoare) => ({ valoare, titlu: numeTinta(valoare) }))
    },
    placa() {
      return PLACI.find((p) => p.comanda === this.ales)
    },
    parametri() {
      if (this.ales === 'INTARZIERE') return { ms: this.ms, variatie: this.variatie }
      if (this.ales === 'BANDA') return { kbps: this.kbps }
      return { secunde: this.secunde }
    },
  },
  methods: {
    /**
     * OK sau ERR vin de la proxy; o eroare HTTP înseamnă proxy indisponibil. „Rețea normală” merge mereu la
     * toată flota, oricare ar fi ținta aleasă sus (redesign §4.6).
     */
    async trimite(comanda, parametri, tinta = this.tinta) {
      this.raspuns = null
      this.seTrimite = true
      try {
        const e = await api.vreme({ comanda, tinta, parametri })
        this.raspuns = { ok: e.raspuns === 'OK', text: `${e.linie} → ${e.raspuns}` }
        this.$emit('trimis', e)
      } catch (err) {
        this.raspuns = { ok: false, text: err.message }
      } finally {
        this.seTrimite = false
      }
    },
  },
}
</script>

<style scoped>
.panou-vreme {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--margine);
  border-radius: var(--colt-card);
  background: var(--suprafata);
}
.sus {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
/* v-input are flex: 1 1 auto; într-o coloană ar crește pe verticală cât e loc. */
.sus > *,
.sus :deep(.v-input) {
  flex: none;
}
/* density compact dă 40 px; pe telefon, „Pentru” are zona de atingere întreagă. */
.mobil :deep(.v-field__input) {
  min-height: var(--atingere);
}
.titlu {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}
.eticheta-mica {
  margin-bottom: -4px;
}
.placi {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.placa {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 10px;
  border: 1px solid var(--margine);
  border-radius: 10px;
  color: var(--text);
  font-size: 12.5px;
  text-align: left;
  cursor: pointer;
}
.placa small {
  color: var(--text-2);
  font-size: 11px;
  line-height: 1.3;
}
.iconita {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: var(--colt-control);
}
.ton-rosu .iconita {
  background: var(--rosu-slab);
  color: var(--rosu-text);
}
.ton-bleumarin .iconita {
  background: var(--margine);
  color: var(--bleumarin-2);
}
.ton-ambra .iconita {
  background: var(--ambra-slab);
  color: var(--ambra-text);
}
.placa.aleasa.ton-rosu {
  border-color: var(--rosu);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--rosu) 12%, transparent);
}
.placa.aleasa.ton-bleumarin {
  border-color: var(--bleumarin);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--bleumarin) 12%, transparent);
}
.placa.aleasa.ton-ambra {
  border-color: var(--ambra);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ambra) 14%, transparent);
}
.explicatie {
  margin: 0;
  color: var(--text-2);
  font-size: 13px;
}
.campuri {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.camp {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.eticheta-camp {
  color: var(--text-2);
  font-size: 12px;
  font-weight: 600;
}
.intrare {
  display: flex;
  align-items: center;
  padding: 0 10px;
  border: 1px solid var(--text-3);
  border-radius: var(--colt-control);
}
.intrare:focus-within {
  border-color: var(--primar);
}
.intrare input {
  flex: 1;
  min-width: 0;
  padding: 7px 0;
  border: 0;
  outline: 0;
  background: none;
  font: inherit;
  font-size: 13px;
}
.intrare small {
  color: var(--text-2);
}
.jos {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.raspuns {
  font-family: var(--font-mono);
  font-size: 12px;
  word-break: break-word;
}
.raspuns.ok {
  color: var(--verde-text);
}
.raspuns.eroare {
  color: var(--rosu-text);
}
.buton-placa {
  padding: 9px 12px;
  border-radius: var(--colt-control);
  color: var(--suprafata);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.buton-placa.ton-rosu {
  background: var(--rosu);
}
.buton-placa.ton-bleumarin {
  background: var(--bleumarin);
}
.buton-placa.ton-ambra {
  background: var(--ambra);
}
.normal {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px;
  border: 1.5px solid var(--verde);
  border-radius: var(--colt-control);
  color: var(--verde-text);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}
.buton-placa:disabled,
.normal:disabled {
  opacity: 0.6;
  cursor: default;
}
.placa:focus-visible,
.buton-placa:focus-visible,
.normal:focus-visible {
  outline: 2px solid var(--primar);
  outline-offset: 2px;
}
/*
 * Pe telefon: o coloană pe toată înălțimea primită. Sus se derulează; jos, răspunsul și cele două butoane
 * rămân lipite, și deasupra tastaturii, fiindcă înălțimea vine din dvh.
 */
.mobil {
  height: 100%;
  min-height: 0;
  padding: 12px 16px;
  border: 0;
  border-radius: 0;
  background: transparent;
}
.mobil .sus {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.mobil .placa {
  flex-direction: row;
  align-items: center;
  min-height: var(--atingere);
  font-size: 13.5px;
}
.mobil .buton-placa,
.mobil .normal {
  min-height: var(--atingere);
  font-size: 14px;
}
</style>
