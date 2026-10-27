<template>
  <span class="etichete" :class="{ inchis }">
    <span v-for="(e, i) in etichete" :key="i" class="eticheta" :class="`ton-${e.ton}`">{{ e.text }}</span>
  </span>
</template>

<script>
import { stariVreme } from '../text.js'

/**
 * Starea vremii unei mașini, în cuvinte (redesign §4.1): una sau două etichete. inchis e pentru coloana
 * bleumarin a Hărții (D52), unde tonurile de pe alb nu s-ar citi.
 */
export default {
  name: 'EtichetaVreme',
  props: {
    descriere: { type: String, default: null },
    inchis: { type: Boolean, default: false },
  },
  computed: {
    etichete() {
      return stariVreme(this.descriere)
    },
  },
}
</script>

<style scoped>
.etichete {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px;
}
.eticheta {
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
  white-space: nowrap;
}
.ton-verde {
  background: var(--verde-slab);
  color: var(--verde-text);
}
.ton-rosu {
  background: var(--rosu-slab);
  color: var(--rosu-text);
}
.ton-ambra {
  background: var(--ambra-slab);
  color: var(--ambra-text);
}
.ton-bleumarin {
  background: var(--bleumarin);
  color: var(--suprafata);
}
.ton-gri {
  background: var(--margine);
  color: var(--text-2);
}
.inchis .ton-verde {
  background: color-mix(in srgb, var(--verde) 20%, transparent);
  color: var(--live-text);
}
.inchis .ton-rosu {
  background: color-mix(in srgb, var(--rosu) 22%, transparent);
  color: var(--rosu-deschis);
}
.inchis .ton-ambra {
  background: color-mix(in srgb, var(--ambra) 22%, transparent);
  color: var(--ambra-deschis);
}
.inchis .ton-bleumarin {
  background: var(--pe-bleumarin-linie);
  color: var(--suprafata);
}
.inchis .ton-gri {
  background: var(--bleumarin-2);
  color: var(--pe-bleumarin-text);
}
</style>
