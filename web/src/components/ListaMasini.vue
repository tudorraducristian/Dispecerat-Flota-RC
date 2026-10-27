<template>
  <div class="lista">
    <div class="eticheta-mica antet">Flota · {{ online }} din {{ masini.length }} online</div>
    <button
      v-for="m in masini"
      :key="m.id"
      type="button"
      class="rand"
      :class="{ selectat: m.id === selectata }"
      :aria-pressed="m.id === selectata"
      @click="$emit('alege', m.id)"
    >
      <span class="punct" :class="`stare-${stare(m)}`" />
      <span class="nume">{{ numeMasina(m.id) }}</span>
      <span class="viteza cifre">{{ viteza(m) }}</span>
    </button>
  </div>
</template>

<script>
import { flota } from '../live.js'
import { numar, numeMasina } from '../text.js'

/** Lista flotei (redesign §4.5), pe bleumarin, ca toată coloana din dreapta a Hărții (D52, cerut de Tudor). */
export default {
  name: 'ListaMasini',
  props: {
    selectata: { type: String, default: null },
  },
  emits: ['alege'],
  computed: {
    masini() {
      return Object.values(flota.masini).sort((a, b) => ordine(a.id) - ordine(b.id))
    },
    online() {
      return this.masini.filter((m) => m.stare === 'ONLINE').length
    },
  },
  methods: {
    numeMasina,
    /** Aceeași regulă ca pe hartă: portocaliu dacă e online, dar are vreme setată. */
    stare(m) {
      const vreme = flota.vreme[m.id]
      if (m.stare === 'ONLINE' && vreme && vreme !== 'NORMAL') return 'vreme'
      return m.stare === 'ONLINE' ? 'online' : 'offline'
    },
    viteza(m) {
      if (m.stare === 'OFFLINE') return 'offline'
      return m.pozitie ? `${numar(m.pozitie.viteza, 0)} km/h` : 'fără poziție'
    },
  },
}

/** masina-10 vine după masina-9, nu după masina-1; un id în alt format merge la coadă. */
function ordine(id) {
  const n = Number(id.split('-')[1])
  return Number.isNaN(n) ? Infinity : n
}
</script>

<style scoped>
.lista {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 10px 12px;
  color: var(--pe-bleumarin-text);
}
.antet {
  padding: 4px 12px 8px;
}
.rand {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: var(--atingere);
  padding: 8px 12px;
  border-radius: var(--colt-control);
  text-align: left;
  cursor: pointer;
}
.rand:hover,
.rand:focus-visible {
  background: var(--bleumarin-2);
}
.rand.selectat {
  background: var(--bleumarin-2);
  color: var(--suprafata);
}
.rand.selectat::before {
  content: '';
  position: absolute;
  left: -10px;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--primar);
}
.punct {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.stare-online {
  background: var(--verde);
}
.stare-vreme {
  background: var(--portocaliu);
}
.stare-offline {
  background: var(--gri-offline);
}
.nume {
  flex: 1;
  font-size: 13.5px;
  font-weight: 500;
}
.viteza {
  font-size: 12.5px;
  color: var(--text-3);
}
</style>
