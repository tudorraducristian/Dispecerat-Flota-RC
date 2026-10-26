<template>
  <div class="pagina">
    <HartaFlotei class="harta" :zone="zone" :traseu="traseu" :selectata="selectata" @alege="alege" />
    <div class="lateral">
      <PanouMasina :masina="masinaSelectata" @inchide="alege(null)" />
      <PanouComenzi v-if="masinaSelectata" :masina="masinaSelectata" />
      <AlegereInterval v-if="masinaSelectata" @interval="arataInterval" @live="alege(selectata)" />
      <v-alert v-if="eroareTraseu" type="error" density="compact" class="ma-2">{{ eroareTraseu }}</v-alert>
      <ListaMasini :selectata="selectata" @alege="alege" />
    </div>
  </div>
</template>

<script>
import AlegereInterval from '../components/AlegereInterval.vue'
import HartaFlotei from '../components/HartaFlotei.vue'
import ListaMasini from '../components/ListaMasini.vue'
import PanouComenzi from '../components/PanouComenzi.vue'
import PanouMasina from '../components/PanouMasina.vue'
import { api } from '../api.js'
import { flota, asculta } from '../live.js'

export default {
  name: 'PaginaHarta',
  components: { AlegereInterval, HartaFlotei, ListaMasini, PanouComenzi, PanouMasina },
  data() {
    return { zone: [], selectata: null, traseu: [], istoric: false, eroareTraseu: null }
  },
  computed: {
    masinaSelectata() {
      return this.selectata ? flota.masini[this.selectata] : null
    },
    conectat() {
      return flota.conectat
    },
  },
  watch: {
    /** Când revine legătura cu serverul, se ia din nou ce n-a mers cât a lipsit (registrul fazei 2). */
    conectat(acum) {
      if (!acum) return
      if (!this.zone.length) this.incarcaZone()
      if (this.selectata && !this.istoric && this.eroareTraseu) this.alege(this.selectata)
    },
  },
  mounted() {
    this.opresteAscultarea = asculta((tip, date) => {
      if (tip === 'POZITIE' && !this.istoric && date.masinaId === this.selectata) this.traseu.push(date)
    })
    this.incarcaZone()
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
    /** Ultimele 5 minute din REST, apoi traseul crește din canalul live. */
    async alege(id) {
      this.selectata = id
      this.istoric = false
      this.eroareTraseu = null
      this.traseu = []
      if (!id) return
      try {
        const traseu = await api.traseu(id)
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
  display: flex;
  height: calc(100vh - 48px);
}
.harta {
  flex: 1;
}
.lateral {
  width: 340px;
  overflow-y: auto;
  border-left: 1px solid rgba(0, 0, 0, 0.12);
}
</style>
