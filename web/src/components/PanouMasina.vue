<template>
  <div v-if="masina" class="panou" :class="{ mobil }">
    <button v-if="!mobil" type="button" class="inapoi" @click="$emit('inchide')">
      <ChevronLeft :size="14" />Toată flota
    </button>

    <div class="antet">
      <span class="patrat" :class="`stare-${stare}`" v-html="svg" />
      <div class="titlu">
        <h2>
          {{ numeMasina(masina.id) }}
          <span class="pastila" :class="`stare-${stare}`">{{ masina.stare === 'ONLINE' ? 'Online' : 'Offline' }}</span>
        </h2>
        <div class="zona">{{ subtitlu }}</div>
      </div>
      <button type="button" class="x" aria-label="Închide panoul mașinii" @click="$emit('inchide')">
        <X :size="16" />
      </button>
    </div>

    <div class="placi">
      <div class="placa">
        <div class="eticheta">Viteza</div>
        <div class="valoare cifre">{{ cifra('viteza', 0) }}<small>km/h</small></div>
      </div>
      <div class="placa">
        <div class="eticheta">Temp.</div>
        <div class="valoare cifre">{{ cifra('temperatura', 1) }}<small>°C</small></div>
      </div>
      <div class="placa">
        <div class="eticheta">Bateria</div>
        <div class="valoare cifre">{{ cifra('baterie', 0) }}<small>%</small></div>
        <div v-if="masina.pozitie" class="baterie">
          <div :class="{ slaba: masina.pozitie.baterie <= 20 }" :style="{ width: `${Math.max(0, Math.min(100, masina.pozitie.baterie))}%` }" />
        </div>
      </div>
    </div>

    <div class="retea">
      <EtichetaVreme :descriere="flota.vreme[masina.id] ?? null" inchis />
      <span class="cifre">{{ masina.recuperate }} recuperate · {{ masina.pierdute }} pierdute</span>
    </div>

    <div class="file" role="tablist" aria-label="Panoul mașinii">
      <button
        v-for="f in FILE"
        :key="f.id"
        type="button"
        role="tab"
        class="fila"
        :class="{ activa: fila === f.id }"
        :aria-selected="fila === f.id"
        @click="fila = f.id"
      >
        {{ f.titlu }}
      </button>
    </div>
    <!-- v-show, nu v-if: fila ascunsă își păstrează starea, de exemplu intervalul ales în Traseu. -->
    <div v-show="fila === 'comenzi'" class="continut" role="tabpanel"><slot name="comenzi" /></div>
    <div v-show="fila === 'traseu'" class="continut" role="tabpanel"><slot name="traseu" /></div>
  </div>
</template>

<script>
import { ChevronLeft, X } from 'lucide-vue-next'
import EtichetaVreme from './EtichetaVreme.vue'
import { flota } from '../live.js'
import { culoareMasina, svgMasina } from '../masinaSvg.js'
import { numar, numeMasina, timpScurs } from '../text.js'

const FILE = [
  { id: 'comenzi', titlu: 'Comenzi' },
  { id: 'traseu', titlu: 'Traseu' },
]

/**
 * Panoul mașinii (redesign §4.5, varianta 1, cu file), pe bleumarin (D52). Comenzile și traseul vin prin
 * sloturi, de la PaginaHarta. Pe telefon (§10.4), Task 7 îl pune în foaia de jos, așa cum e.
 */
export default {
  name: 'PanouMasina',
  components: { ChevronLeft, EtichetaVreme, X },
  props: {
    masina: { type: Object, default: null },
    zone: { type: Array, default: () => [] },
  },
  emits: ['inchide'],
  data() {
    return { flota, FILE, fila: 'comenzi', acum: Date.now() }
  },
  computed: {
    mobil() {
      return this.$vuetify.display.mobile
    },
    culoare() {
      return culoareMasina(this.masina.stare, flota.vreme[this.masina.id])
    },
    stare() {
      if (this.masina.stare !== 'ONLINE') return 'offline'
      return this.culoare === culoareMasina('ONLINE', null) ? 'online' : 'vreme'
    },
    svg() {
      return svgMasina(this.culoare, 'transparent')
    },
    subtitlu() {
      const nume = this.zone.find((z) => z.id === this.masina.zonaId)?.nume ?? this.masina.zonaId
      const vazuta = this.masina.ultimaVedere ? ` · văzută ${timpScurs(this.masina.ultimaVedere, this.acum)}` : ''
      return `zona ${nume}${vazuta}`
    },
  },
  watch: {
    // Altă mașină se deschide pe Comenzi, ca la prima deschidere.
    'masina.id'() {
      this.fila = 'comenzi'
    },
  },
  mounted() {
    this.ceas = setInterval(() => (this.acum = Date.now()), 1000)
  },
  beforeUnmount() {
    clearInterval(this.ceas)
  },
  methods: {
    numeMasina,
    cifra(camp, zecimale) {
      const p = this.masina.pozitie
      return p ? numar(p[camp], zecimale) : '—'
    },
  },
}
</script>

<style scoped>
.panou {
  display: flex;
  flex-direction: column;
  color: var(--pe-bleumarin-text);
}
.inapoi {
  display: flex;
  align-items: center;
  gap: 6px;
  align-self: flex-start;
  padding: 12px 16px 0;
  color: var(--text-3);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.inapoi:hover {
  color: var(--suprafata);
}
.antet {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px 14px 16px;
  border-bottom: 1px solid var(--bleumarin-2);
}
.patrat {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border-radius: var(--colt-card);
}
.patrat :deep(svg) {
  width: 16px;
  height: 27px;
}
.patrat.stare-online {
  background: color-mix(in srgb, var(--verde) 20%, transparent);
}
.patrat.stare-vreme {
  background: color-mix(in srgb, var(--portocaliu) 22%, transparent);
}
.patrat.stare-offline {
  background: var(--bleumarin-2);
}
.titlu {
  flex: 1;
  min-width: 0;
}
h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: var(--suprafata);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.pastila {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}
.pastila.stare-online,
.pastila.stare-vreme {
  background: color-mix(in srgb, var(--verde) 20%, transparent);
  color: var(--live-text);
}
.pastila.stare-offline {
  background: var(--bleumarin-2);
  color: var(--text-3);
}
.zona {
  margin-top: 2px;
  color: var(--text-3);
  font-size: 12px;
}
.x {
  display: grid;
  place-items: center;
  align-self: flex-start;
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: var(--colt-control);
  color: var(--text-3);
  cursor: pointer;
}
.mobil .x {
  width: var(--atingere);
  height: var(--atingere);
  margin: -6px -4px 0 0;
}
.x:hover {
  background: var(--bleumarin-2);
  color: var(--suprafata);
}
.placi {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 14px 16px 10px;
}
.placa {
  padding: 9px 10px;
  border-radius: 10px;
  background: var(--bleumarin-2);
}
.placa .eticheta {
  color: var(--text-3);
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}
.valoare {
  margin-top: 2px;
  color: var(--suprafata);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.valoare small {
  margin-left: 2px;
  color: var(--text-3);
  font-size: 11px;
  font-weight: 500;
}
.baterie {
  height: 4px;
  margin-top: 6px;
  border-radius: 2px;
  background: var(--pe-bleumarin-linie);
}
.baterie div {
  height: 100%;
  border-radius: 2px;
  background: var(--verde);
}
.baterie div.slaba {
  background: var(--rosu);
}
.retea {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 0 16px 14px;
  border-bottom: 1px solid var(--bleumarin-2);
  color: var(--text-3);
  font-size: 12px;
}
.file {
  display: flex;
  margin: 14px 16px 12px;
  padding: 3px;
  border-radius: 9px;
  background: var(--bleumarin-2);
}
.fila {
  flex: 1;
  padding: 6px;
  border-radius: 7px;
  color: var(--text-3);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}
.mobil .fila {
  min-height: var(--atingere);
}
.fila.activa {
  background: var(--pe-bleumarin-linie);
  color: var(--suprafata);
}
.fila:focus-visible,
.inapoi:focus-visible,
.x:focus-visible {
  outline: 2px solid var(--pe-bleumarin-activ);
  outline-offset: 2px;
}
.continut {
  padding: 0 16px 14px;
}
</style>
