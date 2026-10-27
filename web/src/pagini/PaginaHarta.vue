<template>
  <div class="pagina" :class="{ mobil }">
    <HartaFlotei ref="harta" class="harta" :zone="zone" :traseu="traseu" :selectata="selectata" @alege="alege" />
    <!--
      Ori lista flotei, ori panoul mașinii alese, niciodată amândouă, pe bleumarin (D52). Pe laptop stau în
      coloana din dreapta; pe telefon, în foaia care urcă de jos (redesign §10.4).
    -->
    <component :is="mobil ? 'FoaieJos' : 'div'" v-bind="mobil ? { stare: stareFoaie } : { class: 'lateral' }" @update:stare="(s) => (stareFoaie = s)">
      <PanouMasina v-if="masinaSelectata" :masina="masinaSelectata" :zone="zone" @inchide="alege(null)">
        <template #comenzi>
          <PanouComenzi :masina="masinaSelectata" />
        </template>
        <template #traseu>
          <FilaTraseu
            :key="selectata"
            :minute="traseuMinute"
            :eroare="eroareTraseu"
            @interval="arataInterval"
            @live="incarcaTraseu(selectata)"
          />
        </template>
      </PanouMasina>
      <ListaMasini v-else :selectata="selectata" @alege="alege" />
    </component>
  </div>
</template>

<script>
import FilaTraseu from '../components/FilaTraseu.vue'
import FoaieJos from '../components/FoaieJos.vue'
import HartaFlotei from '../components/HartaFlotei.vue'
import ListaMasini from '../components/ListaMasini.vue'
import PanouComenzi from '../components/PanouComenzi.vue'
import PanouMasina from '../components/PanouMasina.vue'
import { api } from '../api.js'
import { flota, asculta } from '../live.js'
import { inaltimi } from '../foaie.js'

export default {
  name: 'PaginaHarta',
  components: { FilaTraseu, FoaieJos, HartaFlotei, ListaMasini, PanouComenzi, PanouMasina },
  data() {
    return {
      zone: [],
      selectata: null,
      traseu: [],
      istoric: false,
      eroareTraseu: null,
      traseuMinute: 5,
      stareFoaie: 'stransa',
    }
  },
  computed: {
    masinaSelectata() {
      return this.selectata ? flota.masini[this.selectata] : null
    },
    conectat() {
      return flota.conectat
    },
    idMasini() {
      return Object.keys(flota.masini).join(',')
    },
    mobil() {
      return this.$vuetify.display.mobile
    },
  },
  watch: {
    /** Când revine legătura cu serverul, se ia din nou ce n-a mers cât a lipsit (registrul fazei 2). */
    conectat(acum) {
      if (!acum) return
      if (!this.zone.length) this.incarcaZone()
      if (this.selectata && !this.istoric && this.eroareTraseu) this.incarcaTraseu(this.selectata)
    },
    // Peste prag, mașina aleasă rămâne aleasă; pe telefon, foaia se deschide pe ea.
    mobil(acum) {
      this.stareFoaie = acum && this.selectata ? 'jumatate' : 'stransa'
    },
    // /?masina=<id> vine din „Vezi pe hartă” sau e scrisă de mână (redesign §4.4).
    '$route.query.masina': { immediate: true, handler: 'dinAdresa' },
    // La F5, lista mașinilor vine după montare, deci adresa se citește din nou când apare.
    idMasini: 'dinAdresa',
  },
  mounted() {
    this.opresteAscultarea = asculta((tip, date) => {
      if (tip === 'POZITIE' && !this.istoric && date.masinaId === this.selectata) this.traseu.push(date)
    })
    this.incarcaZone()
    // Setările se citesc o dată; fără ele rămâne implicitul de 5 minute (spec §16.3).
    api.setari().then((s) => (this.traseuMinute = s.traseuMinute)).catch(() => {})
  },
  beforeUnmount() {
    this.opresteAscultarea()
  },
  methods: {
    /** Cu serverul oprit nu se poate; atunci reîncearcă watch-ul de mai sus, la revenirea legăturii. */
    async incarcaZone() {
      try {
        this.zone = await api.zone()
      } catch (e) {
        console.warn('nu pot încărca zonele:', e.message)
      }
    },
    /** Un id pe care flota nu-l are, de exemplu masina-99, e ignorat: rămâne lista. */
    dinAdresa() {
      const id = this.$route.query.masina
      if (id && flota.masini[id] && this.selectata !== id) this.alege(id)
    },
    alege(id) {
      // Adresa urmează mașina aleasă, ca un „Vezi pe hartă” spre aceeași mașină să mai facă ceva și ca F5 să n-o redeschidă pe cea închisă.
      const dinUrl = this.$route.query.masina
      if (dinUrl && dinUrl !== id) this.$router.replace({ path: '/', query: id ? { masina: id } : {} })
      this.selectata = id
      if (this.mobil) {
        // Pe jumătate, iar harta aduce mașina deasupra foii, nu sub ea (redesign §10.4).
        this.stareFoaie = id ? 'jumatate' : 'stransa'
        if (id) this.$nextTick(() => this.$refs.harta.adu(id, inaltimi(innerHeight - 52).jumatate))
      }
      this.incarcaTraseu(id)
    },
    /** Ultimele traseuMinute minute din REST, după setări, apoi traseul crește din canalul live. */
    async incarcaTraseu(id) {
      this.istoric = false
      this.eroareTraseu = null
      this.traseu = []
      if (!id) return
      try {
        const de = new Date(Date.now() - this.traseuMinute * 60_000).toISOString()
        const traseu = await api.traseu(id, de)
        if (this.selectata === id && !this.istoric) this.traseu = traseu
      } catch (e) {
        if (this.selectata === id) this.eroareTraseu = `nu pot încărca traseul: ${e.message}`
      }
    },
    /** Modul istoric: doar intervalul cerut, fără poziții live adăugate. */
    async arataInterval({ de, pana }) {
      const id = this.selectata
      this.istoric = true
      this.eroareTraseu = null
      try {
        const traseu = await api.traseu(id, de, pana)
        if (this.selectata === id) this.traseu = traseu
      } catch (e) {
        this.eroareTraseu = e.message
      }
    },
  },
}
</script>

<style scoped>
.pagina {
  position: relative;
  display: flex;
  /* --v-layout-top e înălțimea barei, pusă de Vuetify pe v-main: 56 px pe laptop, 52 pe telefon. */
  height: calc(100vh - var(--v-layout-top, 56px));
  /* Pe telefon, 100vh trece și pe sub bara de adrese a Chrome; dvh e doar partea care se vede. */
  height: calc(100dvh - var(--v-layout-top, 56px));
}
.harta {
  flex: 1;
}
.lateral {
  width: 340px;
  overflow-y: auto;
  background: var(--bleumarin);
  border-left: 1px solid var(--bleumarin-2);
}
</style>
