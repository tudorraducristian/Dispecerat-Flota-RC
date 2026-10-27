<template>
  <div class="pagina" :class="{ telefon: mobil }">
    <!-- Pe telefon titlul stă în bară, iar „Le-am văzut pe toate” urcă sus, departe de deget (redesign §10.6). -->
    <div v-if="mobil" class="rand-sus">
      <div class="sub cifre">{{ subtitlu }}</div>
      <button type="button" class="link" :disabled="!sePotMarcaToate" @click="marcheazaToate">Le-am văzut pe toate</button>
    </div>
    <div v-else class="antet">
      <div>
        <h1 class="titlu-pagina">Alarme</h1>
        <div class="sub cifre">{{ subtitlu }}</div>
      </div>
      <div class="spatiu" />
      <button type="button" class="buton-contur" :disabled="!sePotMarcaToate" @click="marcheazaToate">
        <CheckCheck :size="15" />Le-am văzut pe toate
      </button>
      <div class="segment" role="group" aria-label="Ce alarme arăt">
        <button
          v-for="f in FILE"
          :key="f.id"
          type="button"
          :class="{ activ: filtru === f.id }"
          :aria-pressed="filtru === f.id"
          @click="alegeFiltru(f.id)"
        >
          {{ f.titlu }}<span v-if="f.id === 'nevazute' && cifraNevazute" class="numar cifre">{{ cifraNevazute }}</span>
        </button>
      </div>
    </div>

    <div v-if="mobil" class="segment lat" role="group" aria-label="Ce alarme arăt">
      <button
        v-for="f in FILE"
        :key="f.id"
        type="button"
        :class="{ activ: filtru === f.id }"
        :aria-pressed="filtru === f.id"
        @click="alegeFiltru(f.id)"
      >
        {{ f.titlu }}<span v-if="f.id === 'nevazute' && cifraNevazute" class="numar cifre">{{ cifraNevazute }}</span>
      </button>
    </div>

    <div class="tipuri" role="group" aria-label="Tipul alarmei">
      <button type="button" class="tip" :class="{ activ: tip == null }" :aria-pressed="tip == null" @click="alegeTip(null)">
        Toate tipurile
      </button>
      <button
        v-for="(t, cod) in TIPURI_ALARMA"
        :key="cod"
        type="button"
        class="tip"
        :class="{ activ: tip === cod }"
        :aria-pressed="tip === cod"
        @click="alegeTip(cod)"
      >
        <i :class="`ton-${t.ton}`" />{{ t.nume }}
      </button>
    </div>

    <v-alert v-if="eroare" type="error" density="compact" class="mb-3">{{ eroare }}</v-alert>

    <template v-for="g in grupe" :key="g.titlu">
      <div class="eticheta-mica grupa">{{ g.titlu }}</div>
      <div class="carduri">
        <CardAlarma v-for="a in g.alarme" :key="a.id" :alarma="a" :acum="acum" @vazuta="vazuta" />
      </div>
    </template>
    <div v-if="alarme.length === 0 && !eroare" class="gol">Nicio alarmă aici.</div>
  </div>
</template>

<script>
import { CheckCheck } from 'lucide-vue-next'
import CardAlarma from '../components/CardAlarma.vue'
import { api } from '../api.js'
import { asculta } from '../live.js'
import { numarAlarme, recalculeaza } from '../numarAlarme.js'
import { TIPURI_ALARMA, grupaTimp, textCifra } from '../text.js'

const FILTRE = { nevazute: false, vazute: true, toate: null }
const FILE = [
  { id: 'nevazute', titlu: 'Nevăzute' },
  { id: 'vazute', titlu: 'Văzute' },
  { id: 'toate', titlu: 'Toate' },
]

/** Pagina Alarme (redesign §4.7, varianta 2, fluxul de carduri; pe telefon §10.6). */
export default {
  name: 'PaginaAlarme',
  components: { CardAlarma, CheckCheck },
  data() {
    return { filtru: 'nevazute', tip: null, alarme: [], eroare: null, acum: Date.now() }
  },
  computed: {
    mobil() {
      return this.$vuetify.display.mobile
    },
    subtitlu() {
      return `${numarAlarme.nevazute} nevăzute · ${numarAlarme.azi} azi`
    },
    /** Numerele sînt ale întregii flote; lângă un tip ales, cifra ar părea că numără doar tipul acela (§4.7). */
    cifraNevazute() {
      return this.tip == null ? textCifra(numarAlarme.nevazute) : ''
    },
    sePotMarcaToate() {
      return this.filtru !== 'vazute' && this.alarme.some((a) => !a.vazuta)
    },
    /** Grupele urmează ora sosirii, ca sortarea serverului: o alarmă întârziată de un tunel apare sus, ca nouă. */
    grupe() {
      const grupe = []
      for (const a of this.alarme) {
        const titlu = grupaTimp(a.oraSosirii, this.acum)
        const ultima = grupe[grupe.length - 1]
        if (ultima?.titlu === titlu) ultima.alarme.push(a)
        else grupe.push({ titlu, alarme: [a] })
      }
      return grupe
    },
  },
  created() {
    this.FILE = FILE
    this.TIPURI_ALARMA = TIPURI_ALARMA
  },
  async mounted() {
    this.opresteAscultarea = asculta((fel, date) => {
      if (fel !== 'ALARMA' || this.filtru === 'vazute') return
      if (this.tip != null && date.tip !== this.tip) return
      if (!this.alarme.some((a) => a.id === date.id)) this.alarme.unshift(date)
    })
    this.ceas = setInterval(() => {
      this.acum = Date.now()
    }, 10_000)
    await this.incarca()
  },
  beforeUnmount() {
    this.opresteAscultarea()
    clearInterval(this.ceas)
  },
  methods: {
    /** Două filtre alese repede pleacă în două cereri; răspunsul care vine ultimul nu e mereu al filtrului ales. */
    async incarca() {
      const cerere = (this.cerere = (this.cerere ?? 0) + 1)
      try {
        const alarme = await api.alarme(FILTRE[this.filtru], this.tip)
        if (cerere !== this.cerere) return
        this.alarme = alarme
        this.acum = Date.now()
        this.eroare = null
      } catch (e) {
        if (cerere === this.cerere) this.eroare = e.message
      }
    },
    alegeFiltru(filtru) {
      if (filtru === this.filtru) return
      this.filtru = filtru
      this.incarca()
    },
    alegeTip(tip) {
      if (tip === this.tip) return
      this.tip = tip
      this.incarca()
    },
    async marcheazaToate() {
      try {
        await api.marcheazaToateVazute(this.tip)
      } catch (e) {
        this.eroare = e.message
        return
      }
      await this.incarca()
      recalculeaza()
    },
    async vazuta(alarma) {
      const noua = await api.marcheazaVazuta(alarma.id)
      if (this.filtru === 'nevazute') {
        this.alarme = this.alarme.filter((a) => a.id !== alarma.id)
      } else {
        Object.assign(alarma, noua)
      }
      recalculeaza()
    },
  },
}
</script>

<style scoped>
.pagina {
  padding: 20px 22px;
}
.antet {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 16px;
}
.spatiu {
  flex: 1;
}
.sub {
  margin-top: 4px;
  color: var(--text-2);
  font-size: 13px;
}
.buton-contur {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border: 1px solid var(--margine);
  border-radius: var(--colt-control);
  background: var(--suprafata);
  color: var(--text);
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
}
.segment {
  display: flex;
  gap: 2px;
  padding: 3px;
  border-radius: 9px;
  background: var(--margine);
}
.segment button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 7px;
  color: var(--text-2);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}
.segment button.activ {
  background: var(--suprafata);
  color: var(--text);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.12);
}
.segment .numar {
  color: var(--text-2);
  font-size: 11px;
}
.tipuri {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 14px;
}
.tip {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border: 1px solid var(--margine);
  border-radius: 999px;
  background: var(--suprafata);
  color: var(--text-2);
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
}
.tip.activ {
  border-color: var(--bleumarin);
  background: var(--bleumarin);
  color: var(--suprafata);
}
.tip i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}
.ton-rosu {
  background: var(--rosu);
}
.ton-ambra {
  background: var(--ambra);
}
.ton-gri {
  background: var(--text-3);
}
.grupa {
  margin: 4px 0 8px;
}
.carduri {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}
.gol {
  padding: 16px;
  border: 1px solid var(--margine);
  border-radius: var(--colt-card);
  background: var(--suprafata);
  color: var(--text-2);
  font-size: 14px;
}
button:disabled {
  opacity: 0.5;
  cursor: default;
}
button:focus-visible {
  outline: 2px solid var(--primar);
  outline-offset: 2px;
}

/* Telefon */
.pagina.telefon {
  padding: 6px 16px 16px;
}
.rand-sus {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
}
.telefon .sub {
  margin-top: 0;
  font-size: 12px;
}
.link {
  min-height: var(--atingere);
  color: var(--primar-text);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}
.segment.lat {
  margin-bottom: 6px;
}
.segment.lat button {
  flex: 1;
  min-height: var(--atingere);
  padding: 0;
}
.telefon .segment .numar {
  padding: 0 6px;
  border-radius: 999px;
  background: var(--rosu);
  color: var(--suprafata);
  font-size: 10px;
  font-weight: 700;
}
/*
 * Rândul de tipuri se derulează singur, până la marginea ecranului; pagina nu (redesign §10.6). Cei 4 px de sus și
 * de jos țin zona de atingere a etichetelor, pe care derularea altfel ar tăia-o.
 */
.telefon .tipuri {
  flex-wrap: nowrap;
  overflow-x: auto;
  margin: 0 -16px 8px;
  padding: 4px 16px;
  scrollbar-width: none;
}
.telefon .tip {
  min-height: 36px;
  padding: 0 11px;
}
.telefon .tip::after {
  content: '';
  position: absolute;
  inset: calc((36px - var(--atingere)) / 2) 0;
}
</style>
