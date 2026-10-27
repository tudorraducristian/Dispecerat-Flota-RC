<template>
  <span class="iconita-alarma" :class="`ton-${ton}`" :style="{ width: `${marime}px`, height: `${marime}px` }" aria-hidden="true">
    <component :is="iconita" :size="Math.round(marime * 0.53)" />
  </span>
</template>

<script>
import { Bell, Gauge, MapPinOff, OctagonAlert, Thermometer, WifiOff } from 'lucide-vue-next'
import { TIPURI_ALARMA } from '../text.js'

const ICONITE = { Gauge, MapPinOff, OctagonAlert, Thermometer, WifiOff }

/** Pătratul cu iconița tipului, pe fondul culorii lui (redesign §4.1). Un tip necunoscut primește clopoțelul, pe gri. */
export default {
  name: 'IconitaAlarma',
  props: {
    tip: { type: String, required: true },
    marime: { type: Number, default: 36 },
  },
  computed: {
    ton() {
      return TIPURI_ALARMA[this.tip]?.ton ?? 'gri'
    },
    iconita() {
      return ICONITE[TIPURI_ALARMA[this.tip]?.iconita] ?? Bell
    },
  },
}
</script>

<style scoped>
.iconita-alarma {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  border-radius: 10px;
}
.ton-rosu {
  background: var(--rosu-slab);
  color: var(--rosu-text);
}
.ton-ambra {
  background: var(--ambra-slab);
  color: var(--ambra-text);
}
.ton-gri {
  background: var(--margine);
  color: var(--text-2);
}
</style>
