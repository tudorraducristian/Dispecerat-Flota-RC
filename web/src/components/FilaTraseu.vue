<template>
  <div class="traseu" :class="{ mobil }">
    <div class="rapide">
      <button type="button" class="rapid" :class="{ activ: activ === 'live' }" :aria-pressed="activ === 'live'" @click="live">
        <i v-if="activ === 'live'" class="punct-live" />Live · {{ minute }} min
      </button>
      <button
        v-for="r in RAPIDE"
        :key="r.id"
        type="button"
        class="rapid"
        :class="{ activ: activ === r.id }"
        :aria-pressed="activ === r.id"
        @click="ultimele(r)"
      >
        {{ r.titlu }}
      </button>
      <button type="button" class="rapid" :class="{ activ: activ === 'alt' }" :aria-pressed="activ === 'alt'" @click="activ = 'alt'">
        Alt interval…
      </button>
    </div>

    <div v-if="activ === 'alt'" class="interval">
      <label class="camp">
        <span>De la</span>
        <input v-model="de" type="datetime-local" />
      </label>
      <label class="camp">
        <span>Până la</span>
        <input v-model="pana" type="datetime-local" />
      </label>
      <button type="button" class="arata" :disabled="!de || !pana" @click="arata">Arată</button>
    </div>

    <div v-if="eroare" class="eroare" role="alert">{{ eroare }}</div>

    <div class="legenda">
      <div>
        <svg width="34" height="6" aria-hidden="true"><line x1="2" y1="3" x2="32" y2="3" class="linie" /></svg>
        live, a sosit la timp
      </div>
      <div>
        <svg width="34" height="6" aria-hidden="true"><line x1="2" y1="3" x2="32" y2="3" class="linie" stroke-dasharray="1 7" /></svg>
        recuperat din buffer, după tunel
      </div>
      <div>
        <svg width="34" height="6" aria-hidden="true">
          <line x1="2" y1="3" x2="10" y2="3" class="linie" /><line x1="24" y1="3" x2="32" y2="3" class="linie" />
        </svg>
        gol: mesaje pierdute în gaură
      </div>
    </div>
  </div>
</template>

<script>
const RAPIDE = [
  { id: '15', titlu: '15 min', minute: 15 },
  { id: '60', titlu: '1 oră', minute: 60 },
]

/**
 * Fila Traseu din panoul mașinii (redesign §4.5), pe bleumarin (D52). „15 min” și „1 oră” sînt intervale
 * istorice, cu „până la” acum, fără poziții live adăugate.
 */
export default {
  name: 'FilaTraseu',
  /** minute vine din setări, traseu_minute (spec §16.3); eroare e mesajul de la încărcarea traseului. */
  props: {
    minute: { type: Number, default: 5 },
    eroare: { type: String, default: null },
  },
  emits: ['interval', 'live'],
  data() {
    return { RAPIDE, activ: 'live', de: '', pana: '' }
  },
  computed: {
    mobil() {
      return this.$vuetify.display.mobile
    },
  },
  methods: {
    live() {
      this.activ = 'live'
      this.$emit('live')
    },
    ultimele(r) {
      this.activ = r.id
      const acum = Date.now()
      this.$emit('interval', { de: new Date(acum - r.minute * 60_000).toISOString(), pana: new Date(acum).toISOString() })
    },
    /** datetime-local dă ora locală, fără fus; serverul vrea UTC. */
    arata() {
      this.$emit('interval', { de: new Date(this.de).toISOString(), pana: new Date(this.pana).toISOString() })
    },
  },
}
</script>

<style scoped>
.rapide {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}
.rapid {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid var(--pe-bleumarin-linie);
  border-radius: 999px;
  color: var(--pe-bleumarin-text);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
/* Pe alb, butonul activ era bleumarin; pe coloana bleumarin se inversează. */
.rapid.activ {
  border-color: var(--suprafata);
  background: var(--suprafata);
  color: var(--bleumarin);
}
.punct-live {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--verde);
}
.interval {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}
.camp {
  display: flex;
  flex: 1 1 130px;
  flex-direction: column;
  gap: 4px;
  color: var(--text-3);
  font-size: 11.5px;
  font-weight: 600;
}
.camp input {
  min-width: 0;
  padding: 7px 8px;
  border: 1px solid var(--pe-bleumarin-linie);
  border-radius: var(--colt-control);
  outline: 0;
  background: var(--bleumarin-2);
  color: var(--suprafata);
  color-scheme: dark;
  font: inherit;
  font-size: 12.5px;
}
.camp input:focus {
  border-color: var(--pe-bleumarin-activ);
}
.arata {
  padding: 8px 14px;
  border-radius: var(--colt-control);
  background: var(--primar);
  color: var(--suprafata);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}
.arata:disabled {
  opacity: 0.5;
  cursor: default;
}
.rapid:focus-visible,
.arata:focus-visible {
  outline: 2px solid var(--pe-bleumarin-activ);
  outline-offset: 2px;
}
.eroare {
  margin-bottom: 12px;
  color: var(--rosu-deschis);
  font-size: 12px;
}
.legenda {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--text-3);
  font-size: 12px;
}
.legenda div {
  display: flex;
  align-items: center;
  gap: 10px;
}
.linie {
  stroke: var(--primar);
  stroke-width: 4;
  stroke-linecap: round;
}
/* Pe telefon: butoanele câte două pe rând, câmpurile unul sub altul, pe toată lățimea (redesign §10.4). */
.mobil .rapide {
  display: grid;
  grid-template-columns: 1fr 1fr;
}
.mobil .rapid {
  min-height: var(--atingere);
}
.mobil .interval {
  flex-direction: column;
  align-items: stretch;
}
.mobil .camp {
  flex-basis: auto;
}
.mobil .camp input,
.mobil .arata {
  min-height: var(--atingere);
}
</style>
