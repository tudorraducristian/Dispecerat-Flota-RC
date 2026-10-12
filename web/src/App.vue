<template>
  <v-app>
    <v-app-bar color="primary" density="compact">
      <v-app-bar-title>Dispecerat flotă</v-app-bar-title>
    </v-app-bar>
    <v-main>
      <v-container>
        <v-alert v-if="eroare" type="error">Serverul nu răspunde: {{ eroare }}</v-alert>
        <p v-else>{{ masini.length }} mașini în registru.</p>
      </v-container>
    </v-main>
  </v-app>
</template>

<script>
import { api } from './api.js'

export default {
  name: 'App',
  data() {
    return { masini: [], eroare: null }
  },
  async mounted() {
    try {
      this.masini = await api.masini()
    } catch (e) {
      this.eroare = e.message
    }
  },
}
</script>
