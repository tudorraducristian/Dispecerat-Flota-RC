<template>
  <div class="pagina">
    <HartaFlotei class="harta" :zone="zone" :selectata="selectata" @alege="selectata = $event" />
  </div>
</template>

<script>
import HartaFlotei from '../components/HartaFlotei.vue'
import { api } from '../api.js'

export default {
  name: 'PaginaHarta',
  components: { HartaFlotei },
  data() {
    return { zone: [], selectata: null }
  },
  async mounted() {
    this.zone = await api.zone()
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
</style>
