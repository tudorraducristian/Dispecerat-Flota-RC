<template>
  <div class="pagina">
    <HartaFlotei class="harta" :selectata="selectata" @alege="selectata = $event" />
  </div>
</template>

<script>
import HartaFlotei from '../components/HartaFlotei.vue'

export default {
  name: 'PaginaHarta',
  components: { HartaFlotei },
  data() {
    return { selectata: null }
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
