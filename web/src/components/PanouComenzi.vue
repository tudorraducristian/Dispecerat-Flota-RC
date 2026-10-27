<template>
  <div class="comenzi" :class="{ mobil }">
    <div class="alege" role="radiogroup" aria-label="Comanda">
      <button
        v-for="t in TIPURI"
        :key="t.valoare"
        type="button"
        role="radio"
        class="placa"
        :class="{ aleasa: tip === t.valoare }"
        :aria-checked="tip === t.valoare"
        @click="tip = t.valoare"
      >
        <component :is="t.iconita" :size="17" />{{ t.titlu }}
      </button>
    </div>

    <div class="rand-camp">
      <label v-if="tip === 'LIMITEAZA_VITEZA'" class="camp">
        <span class="ascuns">Viteza maximă, între 10 și 130 km/h</span>
        <input v-model.number="kmh" type="number" min="10" max="130" inputmode="numeric" class="cifre" @keyup.enter="trimite" />
        <span class="unitate">km/h</span>
      </label>
      <label v-else-if="tip === 'MESAJ_SOFER'" class="camp">
        <span class="ascuns">Mesajul pentru șofer, cel mult 200 de caractere</span>
        <input v-model="text" type="text" maxlength="200" placeholder="Mesajul pentru șofer" @keyup.enter="trimite" />
        <span class="unitate cifre">{{ text.length }}/200</span>
      </label>
      <button type="button" class="trimite" :disabled="seTrimite" @click="trimite">
        {{ seTrimite ? 'Se trimite…' : 'Trimite' }}
      </button>
    </div>
    <div v-if="eroare" class="eroare" role="alert">{{ eroare }}</div>

    <div v-if="comenzi.length" class="ultimele">
      <div class="titlu-sectiune">Ultimele comenzi</div>
      <div v-for="c in comenzi" :key="c.id" class="comanda">
        <div class="text">
          <b :title="descriere(c)">{{ descriere(c) }}</b>
          <span class="cifre">{{ ora(c.oraTrimiterii) }}</span>
        </div>
        <span class="stare" :class="`ton-${stareAfisata(c).ton}`" :title="stareAfisata(c).text">
          <component :is="stareAfisata(c).iconita" v-if="stareAfisata(c).iconita" :size="12" :stroke-width="2.5" />
          <span class="text-stare">{{ stareAfisata(c).text }}</span>
        </span>
      </div>
    </div>
  </div>
</template>

<script>
import { Check, Clock, Gauge, House, MessageCircle, X } from 'lucide-vue-next'
import { api } from '../api.js'
import { asculta } from '../live.js'

const TIPURI = [
  { titlu: 'Limitează viteza', valoare: 'LIMITEAZA_VITEZA', iconita: Gauge },
  { titlu: 'Întoarce la bază', valoare: 'INTOARCE_LA_BAZA', iconita: House },
  { titlu: 'Mesaj pentru șofer', valoare: 'MESAJ_SOFER', iconita: MessageCircle },
]

/** Fila Comenzi din panoul mașinii (redesign §4.5), pe bleumarin (D52). Pe telefon, „Trimite” stă jos (§10.4). */
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
    mobil() {
      return this.$vuetify.display.mobile
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
      if (this.seTrimite) return
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
    /** „Trimisă” cât mașina e online; „Așteaptă mașina” cât e offline: broker-ul o păstrează (spec §7). */
    stareAfisata(c) {
      if (c.stare === 'CONFIRMATA') return { text: 'Confirmată', ton: 'verde', iconita: Check }
      if (c.stare === 'RESPINSA') return { text: `Respinsă: ${c.motiv}`, ton: 'rosu', iconita: X }
      return this.masina.stare === 'ONLINE'
        ? { text: 'Trimisă', ton: 'albastru', iconita: null }
        : { text: 'Așteaptă mașina', ton: 'ambra', iconita: Clock }
    },
    descriere(c) {
      if (c.tip === 'LIMITEAZA_VITEZA') return `Limitează la ${c.parametri.kmh} km/h`
      if (c.tip === 'MESAJ_SOFER') return `Mesaj: „${c.parametri.text}”`
      return 'Întoarce la bază'
    },
    ora(iso) {
      return iso ? new Date(iso).toLocaleTimeString('ro-RO') : ''
    },
  },
}
</script>

<style scoped>
.comenzi {
  display: flex;
  flex-direction: column;
}
.alege {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  margin-bottom: 10px;
}
.placa {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-height: 56px;
  padding: 8px 4px;
  border: 1px solid var(--pe-bleumarin-linie);
  border-radius: 9px;
  color: var(--pe-bleumarin-text);
  font-size: 11px;
  font-weight: 600;
  line-height: 1.2;
  text-align: center;
  cursor: pointer;
}
.placa svg {
  color: var(--text-3);
}
.placa.aleasa {
  border-color: var(--primar);
  background: color-mix(in srgb, var(--primar) 22%, transparent);
  color: var(--suprafata);
}
.placa.aleasa svg {
  color: var(--pe-bleumarin-activ);
}
.rand-camp {
  display: flex;
  gap: 8px;
}
.camp {
  flex: 1;
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 0 10px;
  border: 1px solid var(--pe-bleumarin-linie);
  border-radius: var(--colt-control);
  background: var(--bleumarin-2);
}
.camp:focus-within {
  border-color: var(--pe-bleumarin-activ);
}
.camp input {
  flex: 1;
  min-width: 0;
  padding: 8px 0;
  border: 0;
  outline: 0;
  background: none;
  color: var(--suprafata);
  font: inherit;
  font-size: 13px;
}
.camp input::placeholder {
  color: var(--text-2);
}
.unitate {
  margin-left: 6px;
  color: var(--text-3);
  font-size: 12px;
}
.trimite {
  padding: 8px 14px;
  border-radius: var(--colt-control);
  background: var(--primar);
  color: var(--suprafata);
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
}
.trimite:disabled {
  opacity: 0.6;
  cursor: default;
}
.placa:focus-visible,
.trimite:focus-visible {
  outline: 2px solid var(--pe-bleumarin-activ);
  outline-offset: 2px;
}
.eroare {
  margin-top: 8px;
  color: var(--rosu-deschis);
  font-size: 12px;
}
.ultimele {
  margin-top: 14px;
}
.titlu-sectiune {
  margin-bottom: 4px;
  color: var(--text-3);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
.comanda {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--bleumarin-2);
  font-size: 12.5px;
}
.comanda:last-child {
  border-bottom: 0;
}
.comanda .text {
  flex: 1;
  min-width: 0;
}
.comanda b {
  display: block;
  overflow: hidden;
  color: var(--suprafata);
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.comanda .text span {
  color: var(--text-3);
  font-size: 11px;
}
.stare {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 50%;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}
.text-stare {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.ton-verde {
  background: color-mix(in srgb, var(--verde) 20%, transparent);
  color: var(--live-text);
}
.ton-ambra {
  background: color-mix(in srgb, var(--ambra) 22%, transparent);
  color: var(--ambra-deschis);
}
.ton-rosu {
  background: color-mix(in srgb, var(--rosu) 22%, transparent);
  color: var(--rosu-deschis);
}
.ton-albastru {
  background: color-mix(in srgb, var(--primar) 22%, transparent);
  color: var(--pe-bleumarin-activ);
}
.ascuns {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
}
/* Pe telefon, „Trimite” e ultimul, pe toată lățimea, lipit de marginea de jos a foii cât derulezi. */
.mobil .rand-camp {
  display: contents;
}
.mobil .camp {
  order: 1;
  min-height: var(--atingere);
}
.mobil .eroare {
  order: 2;
}
.mobil .ultimele {
  order: 3;
}
.mobil .trimite {
  order: 4;
  position: sticky;
  bottom: 0;
  min-height: var(--atingere);
  margin-top: 12px;
  font-size: 14px;
  box-shadow: 0 -8px 12px var(--bleumarin);
}
.mobil .placa {
  min-height: 64px;
}
</style>
