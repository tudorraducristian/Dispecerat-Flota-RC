<template>
  <v-container class="fill-height" fluid>
    <v-row justify="center" align="center">
      <v-col cols="12" sm="8" md="4">
        <v-card>
          <v-card-title class="text-h5">Dispecerat flotă</v-card-title>
          <v-card-text>
            <v-form @submit.prevent="intra">
              <v-text-field v-model="nume" label="Nume" autocomplete="username" autofocus />
              <v-text-field v-model="parola" label="Parolă" type="password" autocomplete="current-password" />
              <v-alert v-if="eroare" type="error" density="compact" class="mb-4">{{ eroare }}</v-alert>
              <v-btn type="submit" color="primary" block :loading="seIncarca">Intră</v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import { api } from '../api.js'
import { salveaza } from '../sesiune.js'

export default {
  name: 'PaginaLogin',
  data() {
    return { nume: '', parola: '', eroare: null, seIncarca: false }
  },
  methods: {
    /** Butonul „Intră” e de tip submit, deci merge și cu Enter. */
    async intra() {
      this.eroare = null
      this.seIncarca = true
      try {
        salveaza(await api.login(this.nume, this.parola))
        this.$router.replace(this.$route.query.inapoi || '/')
      } catch (e) {
        this.eroare = e.message
      } finally {
        this.seIncarca = false
      }
    },
  },
}
</script>
