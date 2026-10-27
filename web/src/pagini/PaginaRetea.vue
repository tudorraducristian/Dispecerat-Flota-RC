<template>
  <!-- Pe telefon (redesign §10.5): trei file, Comenzi, Flota și Jurnal; pagina se deschide mereu pe Comenzi. -->
  <div v-if="mobil" class="pagina-telefon">
    <div class="file" role="tablist" aria-label="Rețea">
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

    <template v-if="fila === 'comenzi'">
      <div class="stare-proxy scurt" :class="{ cazut: !flota.proxyDisponibil }">
        <i />{{ flota.proxyDisponibil ? 'Proxy disponibil' : 'Proxy indisponibil' }}
      </div>
      <PanouVreme class="umple" @trimis="adaugaEveniment" />
    </template>

    <div v-else-if="fila === 'flota'" class="flota-telefon">
      <CifreFlota />
      <div class="cutie">
        <div class="titlu-cutie">Latența flotei · ultimele 60 de secunde</div>
        <GraficLatenta :valori="flota.latentaFlotei" />
      </div>
      <div class="cutie randuri-telefon">
        <div v-for="r in randuri" :key="r.id" class="rand-telefon">
          <div class="sus">
            <b>{{ numeMasina(r.id) }}</b>
            <EtichetaVreme :descriere="flota.vreme[r.id] ?? null" />
          </div>
          <div class="jos cifre">
            {{ r.mesaje }} mesaje/s · {{ r.latenta }} · {{ r.recuperate }} recuperate ·
            <span :class="{ rau: r.pierdute > 0 }">{{ r.pierdute }} pierdute</span>
          </div>
        </div>
      </div>
    </div>

    <JurnalRetea v-else class="umple" :evenimente="evenimente" />
  </div>

  <div v-else class="pagina">
    <div class="antet">
      <h1 class="titlu-pagina">Rețea</h1>
      <div class="stare-proxy" :class="{ cazut: !flota.proxyDisponibil }">
        <i />{{ flota.proxyDisponibil ? 'Proxy disponibil · mașinile pe 1884, ordinele pe 1885' : 'Proxy indisponibil' }}
      </div>
    </div>

    <div class="grila">
      <PanouVreme class="stanga" @trimis="adaugaEveniment" />
      <div class="dreapta">
        <CifreFlota />
        <div class="cutie">
          <div class="titlu-cutie">Latența flotei · ultimele 60 de secunde</div>
          <GraficLatenta :valori="flota.latentaFlotei" />
        </div>
        <div class="cutie tabel">
          <table>
            <thead>
              <tr>
                <th>Mașina</th>
                <th>Rețeaua</th>
                <th>Mesaje pe secundă</th>
                <th>Latența</th>
                <th>Recuperate</th>
                <th>Pierdute</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in randuri" :key="r.id">
                <td class="nume">{{ numeMasina(r.id) }}</td>
                <td><EtichetaVreme :descriere="flota.vreme[r.id] ?? null" /></td>
                <td class="cifre">{{ r.mesaje }}</td>
                <td class="cifre">{{ r.latenta }}</td>
                <td class="cifre">{{ r.recuperate }}</td>
                <td class="cifre" :class="{ rau: r.pierdute > 0 }">{{ r.pierdute }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <JurnalRetea :evenimente="evenimente" />
      </div>
    </div>
  </div>
</template>

<script>
import { api } from '../api.js'
import CifreFlota from '../components/CifreFlota.vue'
import EtichetaVreme from '../components/EtichetaVreme.vue'
import GraficLatenta from '../components/GraficLatenta.vue'
import JurnalRetea from '../components/JurnalRetea.vue'
import PanouVreme from '../components/PanouVreme.vue'
import { flota, asculta } from '../live.js'
import { numar, numeMasina } from '../text.js'

const FILE = [
  { id: 'comenzi', titlu: 'Comenzi' },
  { id: 'flota', titlu: 'Flota' },
  { id: 'jurnal', titlu: 'Jurnal' },
]

/** Pagina Rețea (redesign §4.6, varianta 1; pe telefon §10.5, varianta B). */
export default {
  name: 'PaginaRetea',
  components: { CifreFlota, EtichetaVreme, GraficLatenta, JurnalRetea, PanouVreme },
  data() {
    return { flota, FILE, fila: 'comenzi', evenimente: [] }
  },
  computed: {
    mobil() {
      return this.$vuetify.display.mobile
    },
    /** Un rând pe mașină, din lista flotei, cu statisticile potrivite după masinaId. */
    randuri() {
      const dupaId = Object.fromEntries(flota.statistici.map((s) => [s.masinaId, s]))
      return Object.keys(flota.masini)
        .sort((a, b) => Number(a.split('-')[1]) - Number(b.split('-')[1]))
        .map((id) => {
          const s = dupaId[id]
          return {
            id,
            mesaje: s ? numar(s.mesajePeSecunda, 1) : '—',
            latenta: s?.latentaMedieMs == null ? '—' : `${Math.round(s.latentaMedieMs)} ms`,
            recuperate: s?.recuperate ?? flota.masini[id].recuperate,
            pierdute: s?.pierdute ?? flota.masini[id].pierdute,
          }
        })
    },
  },
  async mounted() {
    // Ascultarea stă în pagină, nu în jurnal: pe telefon, intrările nu se pierd cât ești pe altă filă.
    this.opresteAscultarea = asculta((tip, date) => {
      if (tip === 'EVENIMENT_RETEA') this.adaugaEveniment(date)
    })
    const vechi = await api.evenimente().catch(() => [])
    const noi = this.evenimente
    this.evenimente = [...noi, ...vechi.filter((e) => !noi.some((n) => n.id === e.id))]
  },
  beforeUnmount() {
    this.opresteAscultarea()
  },
  methods: {
    numeMasina,
    /** Același eveniment vine și din răspunsul cererii, și din canalul live: intră o singură dată. */
    adaugaEveniment(e) {
      if (!this.evenimente.some((x) => x.id === e.id)) this.evenimente.unshift(e)
    },
  },
}
</script>

<style scoped>
.pagina {
  padding: 20px 22px;
}
.antet {
  margin-bottom: 16px;
}
.stare-proxy {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  color: var(--text-2);
  font-size: 13px;
}
.stare-proxy i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--verde);
}
.stare-proxy.cazut {
  color: var(--rosu-text);
}
.stare-proxy.cazut i {
  background: var(--rosu);
}
.grila {
  display: grid;
  grid-template-columns: 270px minmax(0, 1fr);
  align-items: start;
  gap: 12px;
}
.dreapta {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}
.cutie {
  border: 1px solid var(--margine);
  border-radius: var(--colt-card);
  background: var(--suprafata);
}
.titlu-cutie {
  padding: 12px 14px 0;
  font-size: 13px;
  font-weight: 600;
}
.cutie :deep(.grafic) {
  display: block;
  padding: 6px 14px 10px 0;
}
.tabel {
  overflow-x: auto;
}
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
}
th {
  padding: 9px 12px;
  border-bottom: 1px solid var(--margine);
  color: var(--text-2);
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-align: left;
  text-transform: uppercase;
  white-space: nowrap;
}
td {
  padding: 8px 12px;
  border-bottom: 1px solid var(--fond);
}
tr:last-child td {
  border-bottom: 0;
}
.nume {
  font-weight: 600;
  white-space: nowrap;
}
.rau {
  color: var(--rosu-text);
  font-weight: 600;
}

/* Telefon */
.pagina-telefon {
  display: flex;
  flex-direction: column;
  height: calc(100dvh - var(--v-layout-top, 52px));
}
.file {
  display: flex;
  flex-shrink: 0;
  margin: 10px 16px 0;
  padding: 3px;
  border-radius: 10px;
  background: var(--margine);
}
.fila {
  flex: 1;
  min-height: var(--atingere);
  border-radius: var(--colt-control);
  color: var(--text-2);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.fila.activa {
  background: var(--suprafata);
  color: var(--text);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.12);
}
.fila:focus-visible {
  outline: 2px solid var(--primar);
  outline-offset: 2px;
}
.stare-proxy.scurt {
  flex-shrink: 0;
  margin: 10px 16px 0;
}
.umple {
  flex: 1;
  min-height: 0;
}
.flota-telefon {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
  padding: 12px 16px 16px;
  overflow-y: auto;
}
.rand-telefon {
  padding: 10px 14px;
  border-bottom: 1px solid var(--fond);
}
.rand-telefon:last-child {
  border-bottom: 0;
}
.rand-telefon .sus {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 14px;
}
.rand-telefon .jos {
  margin-top: 4px;
  color: var(--text-2);
  font-size: 12px;
}
.pagina-telefon > :deep(.jurnal) {
  margin-top: 10px;
}
</style>
