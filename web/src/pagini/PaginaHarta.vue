<template>
  <div class="pagina">
    <HartaFlotei class="harta" :zone="zone" :traseu="traseu" :selectata="selectata" @alege="alege" />
    <div class="lateral">
      <PanouMasina :masina="masinaSelectata" @inchide="alege(null)" />
      <PanouComenzi v-if="masinaSelectata" :masina="masinaSelectata" />
      <ListaMasini :selectata="selectata" @alege="alege" />
    </div>
  </div>
</template>

<script>
import HartaFlotei from '../components/HartaFlotei.vue'
import ListaMasini from '../components/ListaMasini.vue'
import PanouComenzi from '../components/PanouComenzi.vue'
import PanouMasina from '../components/PanouMasina.vue'
import { api } from '../api.js'
import { flota, asculta } from '../live.js'

export default {
  name: 'PaginaHarta',
  components: { HartaFlotei, ListaMasini, PanouComenzi, PanouMasina },
  data() {
    return { zone: [], selectata: null, traseu: [] }
  },
  computed: {
    masinaSelectata() {
      return this.selectata ? flota.masini[this.selectata] : null
    },
  },
  async mounted() {
    this.opresteAscultarea = asculta((tip, date) => {
      if (tip === 'POZITIE' && date.masinaId === this.selectata) this.traseu.push(date)
    })
    this.zone = await api.zone()
  },
  beforeUnmount() {
    this.opresteAscultarea()
  },
  methods: {
    /** Traseul vine din REST, ultimele 5 minute; după aceea crește din canalul live. */
    async alege(id) {
      this.selectata = id
      this.traseu = []
      if (!id) return
      const traseu = await api.traseu(id)
      if (this.selectata === id) this.traseu = traseu
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
