<template>
  <!-- Fără focus și fără rol de dialog: alerta anunță, nu întrerupe ce face dispecerul. -->
  <div class="stiva" :class="{ mobil }" :style="{ '--dreapta': `${mobil ? 0 : dreapta}px` }" aria-live="polite">
    <TransitionGroup name="intrare">
      <div
        v-for="a in alerte"
        :key="a.id"
        class="card"
        :class="[`ton-${ton(a)}`, { oprit: oprite.has(a.id) }]"
        @mouseenter="!mobil && opreste(a.id)"
        @mouseleave="!mobil && reia(a.id)"
        @pointerdown="mobil && apasa(a.id)"
      >
        <div class="interior">
          <IconitaAlarma :tip="a.tip" :marime="mobil ? 32 : 36" />
          <div class="text">
            <div class="titlu">{{ propozitieAlarma(a) }}</div>
            <div class="sub">{{ numeTipAlarma(a.tip) }} · {{ ora(a.ora) }}</div>
          </div>
          <button type="button" class="vezi" @click="vezi(a)">{{ mobil ? 'Vezi' : 'Vezi pe hartă' }}</button>
          <button type="button" class="x" aria-label="Închide alerta" @click="$emit('scoate', a.id)">
            <X :size="16" />
          </button>
        </div>
        <!-- Linia se golește în 6 s; când se termină animația, cardul pleacă. Oprită, stă pe loc de unde a rămas. -->
        <div class="timp"><div class="rest" @animationend="$emit('scoate', a.id)" /></div>
      </div>
    </TransitionGroup>
  </div>
</template>

<script>
import { X } from 'lucide-vue-next'
import IconitaAlarma from './IconitaAlarma.vue'
import { TIPURI_ALARMA, numeTipAlarma, ora, propozitieAlarma } from '../text.js'

/**
 * Alertele de sub bară (redesign §4.4, varianta 1; pe telefon §10.4). Stă în v-main, deci ia de la Vuetify,
 * prin --v-layout-*, locul barei și al meniului; dreapta e lățimea panoului de pe Hartă, ca să nu-l acopere.
 */
export default {
  name: 'StivaAlerte',
  components: { IconitaAlarma, X },
  props: {
    alerte: { type: Array, required: true },
    dreapta: { type: Number, default: 0 },
  },
  emits: ['scoate'],
  data() {
    return { oprite: new Set() }
  },
  computed: {
    mobil() {
      return this.$vuetify.display.mobile
    },
  },
  methods: {
    numeTipAlarma,
    ora,
    propozitieAlarma,
    ton(a) {
      return TIPURI_ALARMA[a.tip]?.ton ?? 'gri'
    },
    opreste(id) {
      this.oprite = new Set(this.oprite).add(id)
    },
    reia(id) {
      const s = new Set(this.oprite)
      s.delete(id)
      this.oprite = s
    },
    /** Pe telefon, timpul stă cât ține degetul pe card; ridicarea poate fi și în afara lui. */
    apasa(id) {
      this.opreste(id)
      const ridica = () => {
        window.removeEventListener('pointerup', ridica)
        window.removeEventListener('pointercancel', ridica)
        this.reia(id)
      }
      window.addEventListener('pointerup', ridica)
      window.addEventListener('pointercancel', ridica)
    },
    vezi(a) {
      this.$router.push({ path: '/', query: { masina: a.masinaId } })
      this.$emit('scoate', a.id)
    },
  },
}
</script>

<style scoped>
.stiva {
  position: fixed;
  /* Peste controalele Leaflet (1000), sub fundalul sertarului de pe telefon (1003) și sub dialoguri. */
  z-index: 1002;
  top: calc(var(--v-layout-top, 56px) + 12px);
  left: var(--v-layout-left, 0px);
  right: calc(var(--v-layout-right, 0px) + var(--dreapta));
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  pointer-events: none;
}
.stiva.mobil {
  top: calc(var(--v-layout-top, 52px) + 8px);
  left: 0;
  right: 0;
  padding: 0 8px;
}
.card {
  width: 100%;
  max-width: 430px;
  overflow: hidden;
  border-radius: var(--colt-card);
  background: var(--suprafata);
  box-shadow: var(--umbra-plutitor);
  pointer-events: auto;
}
.mobil .card {
  max-width: none;
}
.interior {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 12px 12px 14px;
}
.mobil .interior {
  gap: 10px;
  padding: 8px 4px 8px 10px;
}
.text {
  flex: 1;
  min-width: 0;
}
.titlu {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--text);
}
.sub {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.35;
  color: var(--text-2);
}
.vezi {
  padding: 7px 12px;
  border-radius: var(--colt-control);
  background: var(--primar-slab);
  color: var(--primar-text);
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
}
.mobil .vezi {
  min-height: var(--atingere);
}
.x {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 7px;
  color: var(--text-3);
  cursor: pointer;
}
.mobil .x {
  width: var(--atingere);
  height: var(--atingere);
}
.vezi:focus-visible,
.x:focus-visible {
  outline: 2px solid var(--primar);
  outline-offset: 2px;
}
.timp {
  height: 3px;
  background: var(--fond);
}
.rest {
  height: 100%;
  transform-origin: left;
  animation: goleste 6s linear forwards;
}
.oprit .rest {
  animation-play-state: paused;
}
.ton-rosu .rest {
  background: var(--rosu);
}
.ton-ambra .rest {
  background: var(--ambra);
}
.ton-gri .rest {
  background: var(--gri-offline);
}
@keyframes goleste {
  from {
    transform: scaleX(1);
  }
  to {
    transform: scaleX(0);
  }
}
.intrare-enter-active {
  transition: opacity 200ms ease-out, transform 200ms ease-out;
}
.intrare-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}
@media (prefers-reduced-motion: reduce) {
  .intrare-enter-active {
    transition: none;
  }
}
</style>
