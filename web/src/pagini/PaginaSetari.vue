<template>
  <div class="pagina" :class="{ telefon: mobil }">
    <!-- Pe telefon titlul stă în bară (redesign §10.6). -->
    <div v-if="!mobil" class="antet">
      <h1 class="titlu-pagina">Setări</h1>
      <div class="sub">Contul tău și regulile flotei</div>
    </div>

    <div class="grila">
      <form class="cutie" @submit.prevent="schimbaParola">
        <div class="cine">
          <span class="avatar" :class="{ admin }">{{ initiala(sesiune.nume) }}</span>
          <div>
            <b>{{ sesiune.nume }}</b>
            <span class="rol" :class="{ admin }">{{ numeRol(sesiune.rol) }}</span>
          </div>
        </div>
        <h3>Schimbă parola</h3>
        <div class="descriere">După schimbare rămâi conectat.</div>
        <div class="campuri">
          <CampFormular v-model="parola.veche" eticheta="Parola de acum" tip="parola" autocomplete="current-password" />
          <CampFormular
            v-model="parola.noua"
            eticheta="Parola nouă"
            tip="parola"
            autocomplete="new-password"
            ajutor="Cel puțin 8 caractere."
          />
          <CampFormular v-model="parola.confirmare" eticheta="Parola nouă, încă o dată" tip="parola" autocomplete="new-password" />
        </div>
        <div v-if="parola.eroare" class="mesaj eroare" role="alert">{{ parola.eroare }}</div>
        <div v-if="parola.reusita" class="mesaj ok" role="status"><CircleCheck :size="15" />Parola s-a schimbat.</div>
        <button type="submit" class="buton-primar capat">Schimbă parola</button>
      </form>

      <form class="cutie" @submit.prevent="salveaza">
        <h3>Flota</h3>
        <div class="descriere">Se aplică pe loc, pentru toți dispecerii.</div>
        <div v-if="!admin" class="lacat"><Lock :size="14" />Doar administratorul le poate schimba.</div>
        <div class="campuri">
          <CampFormular
            v-model="flota.offlinePrelungitSecunde"
            eticheta="Alarmă când o mașină e offline de"
            tip="numar"
            sufix="secunde"
            :min="10"
            :max="3600"
            ajutor="Între 10 și 3600."
            :citire="!admin"
          />
          <CampFormular
            v-model="flota.traseuMinute"
            eticheta="Traseul din panoul mașinii arată ultimele"
            tip="numar"
            sufix="minute"
            :min="1"
            :max="360"
            ajutor="Între 1 și 360."
            :citire="!admin"
          />
        </div>
        <div v-if="flota.eroare" class="mesaj eroare" role="alert">{{ flota.eroare }}</div>
        <div v-if="flota.reusita" class="mesaj ok" role="status"><CircleCheck :size="15" />Setările s-au salvat.</div>
        <button v-if="admin" type="submit" class="buton-primar capat">Salvează</button>
      </form>
    </div>
  </div>
</template>

<script>
import { CircleCheck, Lock } from 'lucide-vue-next'
import CampFormular from '../components/CampFormular.vue'
import { api } from '../api.js'
import { sesiune, esteAdmin, asiguraUtilizatorul } from '../sesiune.js'
import { initiala, numeRol } from '../text.js'

/** Pagina Setări (redesign §4.8; pe telefon §10.6). */
export default {
  name: 'PaginaSetari',
  components: { CampFormular, CircleCheck, Lock },
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
    mobil() {
      return this.$vuetify.display.mobile
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
    initiala,
    numeRol,
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

<style scoped>
.pagina {
  padding: 20px 22px;
}
.antet {
  margin-bottom: 16px;
}
.sub {
  margin-top: 4px;
  color: var(--text-2);
  font-size: 13px;
}
.grila {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
  gap: 14px;
}
.cutie {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--margine);
  border-radius: var(--colt-card);
  background: var(--suprafata);
}
.cine {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--margine);
}
.cine b {
  display: block;
  margin-bottom: 3px;
  font-size: 15px;
}
.avatar {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--margine);
  color: var(--text-2);
  font-size: 15px;
  font-weight: 600;
}
.avatar.admin {
  background: var(--primar-slab);
  color: var(--primar-text);
}
.rol {
  display: inline-flex;
  padding: 3px 9px;
  border-radius: 999px;
  background: var(--margine);
  color: var(--text);
  font-size: 11.5px;
  font-weight: 600;
}
.rol.admin {
  background: var(--primar-slab);
  color: var(--primar-text);
}
h3 {
  margin: 0 0 -12px;
  font-size: 15px;
  font-weight: 600;
}
.descriere {
  color: var(--text-2);
  font-size: 12.5px;
}
.campuri {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.lacat {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--margine);
  border-radius: var(--colt-control);
  background: var(--fond);
  color: var(--text-2);
  font-size: 12px;
}
.capat {
  align-self: flex-start;
}

/* Telefon: cardurile unul sub altul, iar butonul fiecăruia pe toată lățimea, la capăt (redesign §10.6). */
.pagina.telefon {
  padding: 12px 16px 16px;
}
.telefon .grila {
  grid-template-columns: minmax(0, 1fr);
  gap: 12px;
}
.telefon .cutie {
  padding: 14px;
}
.telefon .capat {
  align-self: stretch;
  min-height: var(--atingere);
  font-size: 14.5px;
}
</style>
