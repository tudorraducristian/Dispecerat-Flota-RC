<template>
  <svg :viewBox="`0 0 ${LATIME} ${INALTIME}`" class="grafic" role="img" aria-label="Latența medie, ultimele 60 de secunde">
    <g v-for="g in grila" :key="g.valoare">
      <line :x1="STANGA" :x2="LATIME" :y1="g.y" :y2="g.y" class="grila" />
      <text :x="STANGA - 6" :y="g.y + 4" class="eticheta" text-anchor="end">{{ g.valoare }}</text>
    </g>
    <polyline :points="linie" class="linie" />
  </svg>
</template>

<script>
const LATIME = 600
const INALTIME = 160
const STANGA = 44
const SUS = 10
const JOS = 10

/**
 * Graficul de latență, desenat de mână în SVG, fără bibliotecă (spec §7). Ultima valoare stă la dreapta;
 * axa verticală se rotunjește în sus la sute de milisecunde.
 */
export default {
  name: 'GraficLatenta',
  props: {
    valori: { type: Array, default: () => [] },
    puncte: { type: Number, default: 60 },
  },
  data() {
    return { LATIME, INALTIME, STANGA }
  },
  computed: {
    plafon() {
      const maxim = Math.max(100, ...this.valori.filter((v) => v != null))
      return Math.ceil(maxim / 100) * 100
    },
    grila() {
      return [0, 0.5, 1].map((f) => ({ valoare: Math.round(this.plafon * f), y: this.y(this.plafon * f) }))
    },
    linie() {
      const pas = (LATIME - STANGA) / (this.puncte - 1)
      const decalaj = this.puncte - this.valori.length
      return this.valori
        .map((v, i) => (v == null ? null : `${STANGA + (decalaj + i) * pas},${this.y(v)}`))
        .filter(Boolean)
        .join(' ')
    },
  },
  methods: {
    y(valoare) {
      return SUS + (INALTIME - SUS - JOS) * (1 - valoare / this.plafon)
    },
  },
}
</script>

<style scoped>
.grafic {
  width: 100%;
  height: 160px;
}
.grila {
  stroke: rgba(var(--v-theme-on-surface), 0.12);
}
.eticheta {
  font-size: 11px;
  fill: rgba(var(--v-theme-on-surface), 0.6);
}
.linie {
  fill: none;
  stroke: rgb(var(--v-theme-primary));
  stroke-width: 2;
}
</style>
