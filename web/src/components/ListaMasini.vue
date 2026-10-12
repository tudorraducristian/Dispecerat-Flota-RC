<template>
  <v-list density="compact" nav>
    <v-list-subheader>Flota: {{ online }} din {{ masini.length }} online</v-list-subheader>
    <v-list-item
      v-for="m in masini"
      :key="m.id"
      :active="m.id === selectata"
      @click="$emit('alege', m.id)"
    >
      <template #prepend>
        <v-icon :color="m.stare === 'ONLINE' ? 'success' : 'grey'" icon="mdi-car" />
      </template>
      <v-list-item-title>{{ m.id }}</v-list-item-title>
      <v-list-item-subtitle>{{ m.pozitie ? m.pozitie.viteza.toFixed(0) + ' km/h' : 'fără poziție' }}</v-list-item-subtitle>
    </v-list-item>
  </v-list>
</template>

<script>
import { flota } from '../live.js'

export default {
  name: 'ListaMasini',
  props: {
    selectata: { type: String, default: null },
  },
  emits: ['alege'],
  computed: {
    masini() {
      return Object.values(flota.masini).sort((a, b) => numar(a.id) - numar(b.id))
    },
    online() {
      return this.masini.filter((m) => m.stare === 'ONLINE').length
    },
  },
}

/** masina-10 vine după masina-9, nu după masina-1. */
function numar(id) {
  return Number(id.split('-')[1])
}
</script>
