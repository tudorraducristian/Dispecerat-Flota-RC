<template>
  <div class="foaie" :class="{ trage: tragere }" :style="{ height: `${inaltime}px` }">
    <button
      type="button"
      class="maner"
      aria-label="Mărește sau micșorează panoul"
      @pointerdown="incepe"
      @pointermove="muta"
      @pointerup="lasa"
      @pointercancel="tragere = null"
      @click="tasta"
    >
      <span class="bara" />
    </button>
    <div class="continut">
      <slot />
    </div>
  </div>
</template>

<script>
import { dupaTragere, inaltimi, urmatoarea } from '../foaie.js'

const BARA = 52 // bara de telefon; foaia măsoară ecranul de sub ea
const PRAG_MISCARE = 6 // sub atât, degetul doar a atins mânerul

/**
 * Foaia care urcă de jos pe Harta de telefon (redesign §10.4, varianta A). Se trage doar de mâner, ca
 * derularea listei din ea și mutarea hărții să rămână ale degetului.
 */
export default {
  name: 'FoaieJos',
  props: {
    stare: { type: String, default: 'stransa' },
  },
  emits: ['update:stare'],
  data() {
    return { ecran: innerHeight - BARA, tragere: null, inaltimeTrasa: 0 }
  },
  computed: {
    h() {
      return inaltimi(this.ecran)
    },
    inaltime() {
      return this.tragere ? this.inaltimeTrasa : this.h[this.stare]
    },
  },
  mounted() {
    window.addEventListener('resize', this.masoara)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.masoara)
  },
  methods: {
    masoara() {
      this.ecran = innerHeight - BARA
    },
    incepe(e) {
      e.currentTarget.setPointerCapture(e.pointerId)
      this.inaltimeTrasa = this.inaltime
      this.tragere = { y0: e.clientY, h0: this.inaltime, mutat: false, puncte: [{ t: e.timeStamp, y: e.clientY }] }
    },
    muta(e) {
      const t = this.tragere
      if (!t) return
      const dy = t.y0 - e.clientY // în sus e pozitiv
      if (Math.abs(dy) > PRAG_MISCARE) t.mutat = true
      this.inaltimeTrasa = Math.min(this.ecran, Math.max(this.h.stransa / 2, t.h0 + dy))
      t.puncte.push({ t: e.timeStamp, y: e.clientY })
      // Viteza se ia din ultimele ~100 ms: o oprire lungă înainte de ridicare nu mai e aruncare.
      while (t.puncte.length > 2 && e.timeStamp - t.puncte[0].t > 100) t.puncte.shift()
    },
    lasa(e) {
      const t = this.tragere
      if (!t) return
      this.tragere = null
      if (!t.mutat) {
        this.$emit('update:stare', urmatoarea(this.stare))
        return
      }
      const primul = t.puncte[0]
      const durata = e.timeStamp - primul.t
      const viteza = durata > 0 ? (primul.y - e.clientY) / durata : 0
      this.$emit('update:stare', dupaTragere(this.inaltimeTrasa, viteza, this.h))
    },
    /** Din tastatură (Enter sau Space), butonul dă click fără pointer: aceeași regulă ca atingerea. */
    tasta(e) {
      if (e.detail === 0) this.$emit('update:stare', urmatoarea(this.stare))
    },
  },
}
</script>

<style scoped>
.foaie {
  position: absolute;
  z-index: 1001; /* peste hartă și controalele Leaflet (1000), sub alertă (1002) și sub sertar */
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  border-radius: 16px 16px 0 0;
  background: var(--bleumarin);
  box-shadow: 0 -4px 16px rgba(15, 23, 42, 0.25);
  transition: height 250ms ease-out;
}
.foaie.trage {
  transition: none;
}
.maner {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 100%;
  height: var(--atingere);
  touch-action: none;
  cursor: grab;
}
.bara {
  width: 36px;
  height: 4px;
  border-radius: 2px;
  background: var(--pe-bleumarin-linie);
}
.maner:focus-visible .bara {
  outline: 2px solid var(--pe-bleumarin-activ);
  outline-offset: 4px;
}
.continut {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
}
@media (prefers-reduced-motion: reduce) {
  .foaie {
    transition: none;
  }
}
</style>
