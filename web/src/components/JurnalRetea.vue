<template>
  <div class="jurnal">
    <div class="titlu"><Terminal :size="15" />Jurnalul rețelei</div>
    <div class="randuri">
      <div v-for="e in evenimente" :key="e.id" class="rand">
        <span class="ora cifre">{{ ora(e.oraStart) }}</span>
        <span class="linie">{{ e.linie }}</span>
        <span class="raspuns" :class="e.raspuns === 'OK' ? 'ok' : 'eroare'">{{ e.raspuns }}</span>
      </div>
      <div v-if="!evenimente.length" class="gol">Nicio comandă trimisă proxy-ului încă.</div>
    </div>
  </div>
</template>

<script>
import { Terminal } from 'lucide-vue-next'

/**
 * Jurnalul rețelei (redesign §4.6): liniile rămân brute, cu masina-3, fiindcă așa pleacă pe portul 1885.
 * Ascultarea stă în pagină, nu aici, ca pe telefon să nu se piardă intrări cât fila Jurnal e ascunsă.
 */
export default {
  name: 'JurnalRetea',
  components: { Terminal },
  props: {
    evenimente: { type: Array, required: true },
  },
  methods: {
    ora(iso) {
      return new Date(iso).toLocaleTimeString('ro-RO')
    },
  },
}
</script>

<style scoped>
.jurnal {
  display: flex;
  flex-direction: column;
  padding: 10px 14px;
  border-radius: var(--colt-card);
  background: var(--bleumarin);
  color: var(--pe-bleumarin-text);
}
.titlu {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  color: var(--fond);
  font-size: 13px;
  font-weight: 600;
}
.titlu svg {
  color: var(--pe-bleumarin-activ);
}
.randuri {
  max-height: 300px;
  overflow-y: auto;
  font-family: var(--font-mono);
  font-size: 11.5px;
  line-height: 1.75;
}
.rand {
  display: flex;
  gap: 10px;
}
.ora {
  color: var(--text-2);
}
.linie {
  flex: 1;
  min-width: 0;
  color: var(--fond);
  word-break: break-word;
}
.raspuns.ok {
  color: var(--live-text);
}
.raspuns.eroare {
  color: var(--rosu-deschis);
}
.gol {
  color: var(--text-2);
}
/* Pe telefon: toată înălțimea filei Jurnal; fiecare intrare pe două rânduri, răspunsul mutat spre dreapta. */
@media (max-width: 999px) {
  .jurnal {
    height: 100%;
    min-height: 0;
    border-radius: 0;
  }
  .randuri {
    flex: 1;
    max-height: none;
  }
  .rand {
    flex-wrap: wrap;
    column-gap: 10px;
    padding: 4px 0;
    border-bottom: 1px solid var(--bleumarin-2);
  }
  .raspuns {
    flex-basis: 100%;
    padding-left: 24px;
  }
}
</style>
