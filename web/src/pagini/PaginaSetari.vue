<template>
  <v-container>
    <h2 class="text-h6 mb-4">Setări</h2>
    <v-row>
      <v-col cols="12" md="6">
        <v-card>
          <v-card-title>Contul meu</v-card-title>
          <v-card-subtitle>{{ sesiune.nume }} · {{ sesiune.rol }}</v-card-subtitle>
          <v-card-text>
            <v-form @submit.prevent="schimbaParola">
              <v-text-field v-model="parola.veche" label="Parola de acum" type="password" autocomplete="current-password" />
              <v-text-field v-model="parola.noua" label="Parola nouă" type="password" autocomplete="new-password" hint="cel puțin 8 caractere" />
              <v-text-field v-model="parola.confirmare" label="Parola nouă, încă o dată" type="password" autocomplete="new-password" />
              <v-alert v-if="parola.eroare" type="error" density="compact" class="mb-4">{{ parola.eroare }}</v-alert>
              <v-alert v-if="parola.reusita" type="success" density="compact" class="mb-4">Parola s-a schimbat.</v-alert>
              <v-btn type="submit" color="primary">Schimbă parola</v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="6">
        <v-card>
          <v-card-title>Flota</v-card-title>
          <v-card-subtitle v-if="!admin">Doar administratorul le poate schimba.</v-card-subtitle>
          <v-card-text>
            <v-form @submit.prevent="salveaza">
              <v-text-field v-model.number="flota.offlinePrelungitSecunde" label="Alarmă după câte secunde offline" type="number" min="10" max="3600" hint="între 10 și 3600" :readonly="!admin" />
              <v-text-field v-model.number="flota.traseuMinute" label="Traseul din panoul mașinii, în minute" type="number" min="1" max="360" hint="între 1 și 360" :readonly="!admin" />
              <v-alert v-if="flota.eroare" type="error" density="compact" class="mb-4">{{ flota.eroare }}</v-alert>
              <v-alert v-if="flota.reusita" type="success" density="compact" class="mb-4">Setările s-au salvat.</v-alert>
              <v-btn v-if="admin" type="submit" color="primary">Salvează</v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import { api } from '../api.js'
import { sesiune, esteAdmin, asiguraUtilizatorul } from '../sesiune.js'

export default {
  name: 'PaginaSetari',
  data() {
    return {
      sesiune,
      parola: { veche: '', noua: '', confirmare: '', eroare: null, reusita: false },
      flota: { offlinePrelungitSecunde: null, traseuMinute: null, eroare: null, reusita: false },
    }
  },
  computed: {
    admin() {
      return esteAdmin()
    },
  },
  async mounted() {
    asiguraUtilizatorul().catch(() => {})
    try {
      Object.assign(this.flota, await api.setari())
    } catch (e) {
      this.flota.eroare = e.message
    }
  },
  methods: {
    /** Cele două parole noi trebuie să fie egale înainte să plece cererea. */
    async schimbaParola() {
      const p = this.parola
      p.eroare = null
      p.reusita = false
      if (p.noua !== p.confirmare) {
        p.eroare = 'cele două parole noi nu sînt la fel'
        return
      }
      try {
        await api.schimbaParola(p.veche, p.noua)
        Object.assign(p, { veche: '', noua: '', confirmare: '', reusita: true })
      } catch (e) {
        p.eroare = e.message
      }
    },
    async salveaza() {
      const f = this.flota
      f.eroare = null
      f.reusita = false
      try {
        Object.assign(f, await api.salveazaSetari({ offlinePrelungitSecunde: f.offlinePrelungitSecunde, traseuMinute: f.traseuMinute }))
        f.reusita = true
      } catch (e) {
        f.eroare = e.message
      }
    },
  },
}
</script>
