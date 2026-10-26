<template>
  <v-container>
    <div class="d-flex align-center mb-4">
      <h2 class="text-h6">Conturi</h2>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-account-plus" @click="deschideContNou">Cont nou</v-btn>
    </div>
    <v-alert v-if="eroare" type="error" class="mb-4" closable @click:close="eroare = null">{{ eroare }}</v-alert>
    <v-alert v-if="mesaj" type="success" class="mb-4" closable @click:close="mesaj = null">{{ mesaj }}</v-alert>
    <v-table density="compact">
      <thead>
        <tr><th>Nume</th><th>Rol</th><th>Creat la</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-for="u in conturi" :key="u.id">
          <td>{{ u.nume }}</td>
          <td>{{ u.rol }}</td>
          <td>{{ ora(u.creatLa) }}</td>
          <td class="text-right">
            <!-- Pe rândul propriu, butoanele lipsesc: parola ta se schimbă din Setări, iar contul nu ți-l poți șterge. -->
            <template v-if="u.nume !== sesiune.nume">
              <v-btn size="small" variant="text" @click="deschideResetare(u)">Resetează parola</v-btn>
              <v-btn size="small" variant="text" color="error" @click="deschideStergere(u)">Șterge</v-btn>
            </template>
          </td>
        </tr>
      </tbody>
    </v-table>

    <v-dialog v-model="contNou.deschis" max-width="420">
      <v-card title="Cont nou">
        <v-card-text>
          <v-text-field v-model="contNou.nume" label="Nume" hint="3–20 caractere: litere mici, cifre, punct, _ sau -" />
          <v-text-field v-model="contNou.parola" label="Parolă" type="password" autocomplete="new-password" hint="cel puțin 8 caractere" />
          <v-select v-model="contNou.rol" :items="['DISPECER', 'ADMIN']" label="Rol" />
          <v-alert v-if="contNou.eroare" type="error" density="compact">{{ contNou.eroare }}</v-alert>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="contNou.deschis = false">Renunță</v-btn>
          <v-btn color="primary" @click="creeaza">Creează</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="resetare.deschis" max-width="420">
      <v-card :title="`Parolă nouă pentru ${resetare.cont?.nume}`">
        <v-card-text>
          <v-text-field v-model="resetare.parola" label="Parola nouă" type="password" autocomplete="new-password" hint="cel puțin 8 caractere" />
          <p class="text-body-2">Omul e scos din aplicație și intră din nou cu parola nouă.</p>
          <v-alert v-if="resetare.eroare" type="error" density="compact">{{ resetare.eroare }}</v-alert>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="resetare.deschis = false">Renunță</v-btn>
          <v-btn color="primary" @click="reseteaza">Resetează</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="stergere.deschis" max-width="420">
      <v-card :title="`Ștergi contul ${stergere.cont?.nume}?`">
        <v-card-text>
          Omul e scos din aplicație, iar contul nu se mai poate recupera.
          <v-alert v-if="stergere.eroare" type="error" density="compact" class="mt-4">{{ stergere.eroare }}</v-alert>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="stergere.deschis = false">Renunță</v-btn>
          <v-btn color="error" @click="sterge">Șterge</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script>
import { api } from '../api.js'
import { sesiune, asiguraUtilizatorul } from '../sesiune.js'

export default {
  name: 'PaginaConturi',
  data() {
    return {
      sesiune,
      conturi: [],
      eroare: null,
      mesaj: null,
      contNou: { deschis: false, nume: '', parola: '', rol: 'DISPECER', eroare: null },
      resetare: { deschis: false, cont: null, parola: '', eroare: null },
      stergere: { deschis: false, cont: null, eroare: null },
    }
  },
  async mounted() {
    await asiguraUtilizatorul().catch(() => {})
    await this.incarca()
  },
  methods: {
    async incarca() {
      try {
        this.conturi = await api.utilizatori()
      } catch (e) {
        this.eroare = e.message
      }
    },
    deschideContNou() {
      this.contNou = { deschis: true, nume: '', parola: '', rol: 'DISPECER', eroare: null }
    },
    deschideResetare(cont) {
      this.resetare = { deschis: true, cont, parola: '', eroare: null }
    },
    deschideStergere(cont) {
      this.stergere = { deschis: true, cont, eroare: null }
    },
    /** Un 409, de exemplu numele luat, apare în dialog cu mesajul serverului. */
    async creeaza() {
      const c = this.contNou
      try {
        const nou = await api.creeazaCont({ nume: c.nume, parola: c.parola, rol: c.rol })
        c.deschis = false
        this.mesaj = `Contul ${nou.nume} e creat.`
        await this.incarca()
      } catch (e) {
        c.eroare = e.message
      }
    },
    async reseteaza() {
      const r = this.resetare
      try {
        await api.reseteazaParola(r.cont.id, r.parola)
        r.deschis = false
        this.mesaj = `Parola lui ${r.cont.nume} s-a schimbat.`
      } catch (e) {
        r.eroare = e.message
      }
    },
    async sterge() {
      const s = this.stergere
      try {
        await api.stergeCont(s.cont.id)
        s.deschis = false
        this.mesaj = `Contul ${s.cont.nume} e șters.`
        await this.incarca()
      } catch (e) {
        s.eroare = e.message
      }
    },
    ora(iso) {
      return new Date(iso).toLocaleString('ro-RO')
    },
  },
}
</script>
