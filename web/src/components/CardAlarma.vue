<template>
  <div class="card-alarma" :class="{ nevazuta: !alarma.vazuta, vazuta: alarma.vazuta, telefon: mobil }">
    <IconitaAlarma :tip="alarma.tip" :marime="mobil ? 32 : 38" />

    <div class="text">
      <!-- Pe telefon ora urcă deasupra titlului: în dreapta se tăia (redesign §10.6, cerut de Tudor). -->
      <div v-if="mobil" class="ora-sus cifre">{{ ora(alarma.ora) }} · {{ timpScurs(alarma.ora, acum) }}</div>
      <div class="titlu">{{ propozitieAlarma(alarma) }}</div>
      <div class="detalii">{{ virgula(alarma.detalii) }} · {{ sursaAlarmei(alarma.sursa) }}</div>
      <button v-if="mobil" type="button" class="vezi" @click="vezi">Vezi pe hartă</button>
    </div>

    <div v-if="!mobil" class="ora-dreapta cifre">
      {{ ora(alarma.ora) }}<br>{{ timpScurs(alarma.ora, acum) }}
    </div>
    <!-- O alarmă văzută pierde doar bifa; „Vezi pe hartă” rămâne (redesign §4.7). -->
    <div v-if="!mobil || !alarma.vazuta" class="actiuni">
      <button v-if="!mobil" type="button" class="vezi" @click="vezi">Vezi pe hartă</button>
      <button v-if="!alarma.vazuta" type="button" class="bifa" aria-label="Am văzut" title="Am văzut" @click="$emit('vazuta', alarma)">
        <Check :size="mobil ? 18 : 15" :stroke-width="2.2" />
      </button>
    </div>
  </div>
</template>

<script>
import { Check } from 'lucide-vue-next'
import IconitaAlarma from './IconitaAlarma.vue'
import { ora, propozitieAlarma, sursaAlarmei, timpScurs, virgula } from '../text.js'

/**
 * O alarmă din fluxul paginii Alarme (redesign §4.7, varianta 2; pe telefon §10.6). Ora și „acum …” sînt ale
 * mașinii; grupa și locul în listă le hotărăște pagina, după ora sosirii.
 */
export default {
  name: 'CardAlarma',
  components: { Check, IconitaAlarma },
  props: {
    alarma: { type: Object, required: true },
    acum: { type: Number, required: true },
  },
  emits: ['vazuta'],
  computed: {
    mobil() {
      return this.$vuetify.display.mobile
    },
  },
  methods: {
    ora,
    propozitieAlarma,
    sursaAlarmei,
    timpScurs,
    virgula,
    vezi() {
      this.$router.push({ path: '/', query: { masina: this.alarma.masinaId } })
    },
  },
}
</script>

<style scoped>
.card-alarma {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border: 1px solid var(--margine);
  border-radius: var(--colt-card);
  background: var(--suprafata);
}
/* Linia albastră e o umbră interioară: urmează colțul rotunjit și nu mută conținutul. */
.card-alarma.nevazuta {
  box-shadow: inset 3px 0 0 var(--primar);
}
.card-alarma.vazuta {
  opacity: 0.7;
}
.text {
  flex: 1;
  min-width: 0;
}
.titlu {
  font-size: 14px;
  font-weight: 600;
}
.detalii {
  margin-top: 2px;
  color: var(--text-2);
  font-size: 12.5px;
}
.ora-dreapta {
  color: var(--text-2);
  font-size: 12px;
  text-align: right;
  white-space: nowrap;
}
.actiuni {
  display: flex;
  align-items: center;
  gap: 6px;
}
.vezi {
  padding: 6px 10px;
  border-radius: 7px;
  background: var(--primar-slab);
  color: var(--primar-text);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
}
.bifa {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border: 1px solid var(--margine);
  border-radius: var(--colt-control);
  background: var(--suprafata);
  color: var(--text-2);
  cursor: pointer;
}
.vezi:focus-visible,
.bifa:focus-visible {
  outline: 2px solid var(--primar);
  outline-offset: 2px;
}

/* Telefon */
.card-alarma.telefon {
  align-items: stretch;
  gap: 10px;
  padding: 10px 10px 10px 12px;
}
.telefon .ora-sus {
  margin-bottom: 3px;
  color: var(--text-2);
  font-size: 11px;
  font-weight: 500;
}
.telefon .titlu {
  font-size: 13px;
  line-height: 1.25;
}
.telefon .detalii {
  font-size: 11px;
}
.telefon .vezi {
  position: relative;
  margin-top: 7px;
  padding: 0;
  background: none;
  font-size: 12.5px;
}
.telefon .actiuni {
  align-self: center;
}
.telefon .bifa {
  position: relative;
  width: 36px;
  height: 36px;
  border-width: 1.5px;
  color: var(--text);
}
/* Desenul rămâne mic, dar degetul are --atingere pe --atingere (redesign §10, Constrângeri). */
.telefon .vezi::after,
.telefon .bifa::after {
  content: '';
  position: absolute;
  inset: calc((36px - var(--atingere)) / 2);
}
.telefon .vezi::after {
  inset: -14px -8px;
}
</style>
