<template>
  <div class="pagina" :class="{ telefon: mobil }">
    <!-- Pe telefon titlul stă în bară, iar „Cont nou” plutește jos, la degetul mare (redesign §10.6). -->
    <div v-if="mobil" class="sub sus cifre">{{ textConturi }}</div>
    <div v-else class="antet">
      <div>
        <h1 class="titlu-pagina">Conturi</h1>
        <div class="sub cifre">{{ textConturi }}</div>
      </div>
      <div class="spatiu" />
      <button type="button" class="buton-primar" @click="deschideContNou"><Plus :size="15" />Cont nou</button>
    </div>

    <div v-if="eroare" class="mesaj eroare" role="alert">
      <span class="spatiu">{{ eroare }}</span>
      <button type="button" class="inchide" aria-label="Închide mesajul" @click="eroare = null"><X :size="15" /></button>
    </div>
    <div v-if="mesaj" class="mesaj ok" role="status">
      <CircleCheck :size="15" /><span class="spatiu">{{ mesaj }}</span>
      <button type="button" class="inchide" aria-label="Închide mesajul" @click="mesaj = null"><X :size="15" /></button>
    </div>

    <div class="lista">
      <div v-for="u in conturi" :key="u.id" class="rand">
        <span class="avatar" :class="{ admin: u.rol === 'ADMIN' }">{{ initiala(u.nume) }}</span>

        <template v-if="mobil">
          <div class="text">
            <div class="nume">{{ u.nume }}<span v-if="eu(u)" class="eu">tu</span></div>
            <div class="jos cifre"><span class="rol" :class="{ admin: u.rol === 'ADMIN' }">{{ numeRol(u.rol) }}</span> · {{ dataScurta(u.creatLa) }}</div>
          </div>
          <!-- Pe rândul propriu ⋮ lipsește: parola ta se schimbă din Setări, iar contul nu ți-l poți șterge. -->
          <v-menu v-if="!eu(u)" location="bottom end">
            <template #activator="{ props }">
              <button type="button" class="puncte" v-bind="props" :aria-label="`Acțiuni pentru ${u.nume}`">
                <EllipsisVertical :size="18" />
              </button>
            </template>
            <div class="meniu-cont">
              <button type="button" @click="deschideResetare(u)"><KeyRound :size="16" />Resetează parola</button>
              <button type="button" class="rosu" @click="deschideStergere(u)"><Trash2 :size="16" />Șterge contul</button>
            </div>
          </v-menu>
        </template>

        <template v-else>
          <div class="nume">{{ u.nume }}<span v-if="eu(u)" class="eu">tu</span></div>
          <span class="rol" :class="{ admin: u.rol === 'ADMIN' }">{{ numeRol(u.rol) }}</span>
          <span class="spatiu" />
          <span class="creat cifre">{{ dataCreare(u.creatLa) }}</span>
          <!-- Pe rândul propriu butoanele lipsesc: parola ta se schimbă din Setări, iar contul nu ți-l poți șterge. -->
          <div class="actiuni">
            <template v-if="!eu(u)">
              <button type="button" class="buton-mic" @click="deschideResetare(u)"><KeyRound :size="14" />Resetează parola</button>
              <button type="button" class="buton-mic rosu" @click="deschideStergere(u)"><Trash2 :size="14" />Șterge</button>
            </template>
          </div>
        </template>
      </div>
    </div>

    <button v-if="mobil" type="button" class="buton-primar plutitor" @click="deschideContNou"><Plus :size="18" />Cont nou</button>

    <v-dialog v-model="contNou.deschis" :fullscreen="mobil" :max-width="mobil ? undefined : 440">
      <form class="fereastra" :class="{ plina: mobil }" @submit.prevent="creeaza">
        <div v-if="mobil" class="cap">
          <button type="button" class="x" aria-label="Închide" @click="contNou.deschis = false"><X :size="20" /></button>
          <span>Cont nou</span>
        </div>
        <template v-else>
          <h3>Cont nou</h3>
        </template>
        <div :class="mobil ? 'avertisment' : 'descriere'">Rolul nu se mai poate schimba după creare.</div>
        <CampFormular v-model="contNou.nume" eticheta="Nume" ajutor="3–20 caractere: litere mici, cifre, punct, _ sau -." />
        <CampFormular v-model="contNou.parola" eticheta="Parolă" tip="parola" autocomplete="new-password" ajutor="Cel puțin 8 caractere." />
        <div>
          <div class="eticheta-camp">Rol</div>
          <div class="roluri" role="group" aria-label="Rol">
            <button
              v-for="r in ROLURI"
              :key="r.cod"
              type="button"
              class="placa"
              :class="{ activa: contNou.rol === r.cod }"
              :aria-pressed="contNou.rol === r.cod"
              @click="contNou.rol = r.cod"
            >
              <b>{{ numeRol(r.cod) }}</b>{{ r.text }}
            </button>
          </div>
        </div>
        <div v-if="contNou.eroare" class="mesaj eroare" role="alert">{{ contNou.eroare }}</div>
        <div class="fund">
          <button type="button" class="buton-secundar" @click="contNou.deschis = false">Renunță</button>
          <button type="submit" class="buton-primar">Creează contul</button>
        </div>
      </form>
    </v-dialog>

    <v-dialog v-model="resetare.deschis" v-bind="fereastraMica">
      <form class="fereastra" @submit.prevent="reseteaza">
        <h3>Parolă nouă pentru {{ resetare.cont?.nume }}</h3>
        <div class="descriere">Omul e scos din aplicație și intră din nou cu parola nouă.</div>
        <CampFormular v-model="resetare.parola" eticheta="Parola nouă" tip="parola" autocomplete="new-password" ajutor="Cel puțin 8 caractere." />
        <div v-if="resetare.eroare" class="mesaj eroare" role="alert">{{ resetare.eroare }}</div>
        <div class="fund">
          <button type="button" class="buton-secundar" @click="resetare.deschis = false">Renunță</button>
          <button type="submit" class="buton-primar">Resetează</button>
        </div>
      </form>
    </v-dialog>

    <v-dialog v-model="stergere.deschis" v-bind="fereastraMica">
      <div class="fereastra">
        <h3>Ștergi contul {{ stergere.cont?.nume }}?</h3>
        <div class="descriere">Omul e scos din aplicație, iar contul nu se mai poate recupera.</div>
        <div v-if="stergere.eroare" class="mesaj eroare" role="alert">{{ stergere.eroare }}</div>
        <div class="fund">
          <button type="button" class="buton-secundar" @click="stergere.deschis = false">Renunță</button>
          <button type="button" class="buton-pericol" @click="sterge">Șterge</button>
        </div>
      </div>
    </v-dialog>
  </div>
</template>

<script>
import { CircleCheck, EllipsisVertical, KeyRound, Plus, Trash2, X } from 'lucide-vue-next'
import CampFormular from '../components/CampFormular.vue'
import { api } from '../api.js'
import { sesiune, asiguraUtilizatorul } from '../sesiune.js'
import { dataCreare, dataScurta, initiala, numeRol } from '../text.js'

const ROLURI = [
  { cod: 'DISPECER', text: 'Harta, rețeaua, alarmele și comenzile.' },
  { cod: 'ADMIN', text: 'În plus, conturile și setările flotei.' },
]

/** Pagina Conturi (redesign §4.9; pe telefon §10.6). */
export default {
  name: 'PaginaConturi',
  components: { CampFormular, CircleCheck, EllipsisVertical, KeyRound, Plus, Trash2, X },
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
  computed: {
    mobil() {
      return this.$vuetify.display.mobile
    },
    /** În română, de la 20 în sus vine „de”: 3 conturi, dar 20 de conturi. */
    textConturi() {
      const n = this.conturi.length
      if (n === 1) return '1 cont'
      const rest = n % 100
      return (rest === 0 && n > 0) || rest >= 20 ?`${n} de conturi` : `${n} conturi`
    },
    /** Resetarea și ștergerea rămân ferestre mici; pe telefon, pe lățimea ecranului, cu 16 px de margine. */
    fereastraMica() {
      return this.mobil
        ? { width: 'calc(100% - 32px)', maxWidth: 'calc(100% - 32px)', contentClass: 'fereastra-telefon' }
        : { maxWidth: 420 }
    },
  },
  created() {
    this.ROLURI = ROLURI
  },
  async mounted() {
    await asiguraUtilizatorul().catch(() => {})
    await this.incarca()
  },
  methods: {
    dataCreare,
    dataScurta,
    initiala,
    numeRol,
    eu(cont) {
      return cont.nume === sesiune.nume
    },
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
  },
}
</script>

<style scoped>
.pagina {
  padding: 20px 22px;
}
.antet {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  margin-bottom: 16px;
}
.spatiu {
  flex: 1;
}
.sub {
  margin-top: 4px;
  color: var(--text-2);
  font-size: 13px;
}
.mesaj {
  margin-bottom: 12px;
}
.inchide {
  display: grid;
  place-items: center;
  color: inherit;
  cursor: pointer;
}
.lista {
  overflow: hidden;
  border: 1px solid var(--margine);
  border-radius: var(--colt-card);
  background: var(--suprafata);
}
.rand {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--margine);
  font-size: 13px;
}
.rand:last-child {
  border-bottom: 0;
}
.avatar {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--margine);
  color: var(--text-2);
  font-size: 13px;
  font-weight: 600;
}
.avatar.admin {
  background: var(--primar-slab);
  color: var(--primar-text);
}
.nume {
  display: flex;
  flex: 0 1 170px;
  align-items: center;
  min-width: 0;
  font-weight: 600;
}
.eu {
  margin-left: 6px;
  padding: 1px 6px;
  border-radius: 5px;
  background: var(--margine);
  color: var(--text-2);
  font-size: 11px;
  font-weight: 600;
}
.rol {
  display: inline-flex;
  padding: 3px 9px;
  border-radius: 999px;
  background: var(--margine);
  color: var(--text);
  font-size: 11.5px;
  font-weight: 600;
  white-space: nowrap;
}
.rol.admin {
  background: var(--primar-slab);
  color: var(--primar-text);
}
.creat {
  flex: 0 1 190px;
  min-width: 0;
  color: var(--text-2);
  font-size: 12.5px;
}
.actiuni {
  display: flex;
  flex: 0 0 236px;
  justify-content: flex-end;
  gap: 6px;
}
.buton-mic {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 11px;
  border: 1px solid var(--margine);
  border-radius: var(--colt-control);
  background: var(--suprafata);
  color: var(--text);
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
}
.buton-mic.rosu {
  border-color: var(--rosu-slab);
  color: var(--rosu-text);
}
.buton-mic:focus-visible,
.puncte:focus-visible,
.placa:focus-visible,
.x:focus-visible,
.meniu-cont button:focus-visible {
  outline: 2px solid var(--primar);
  outline-offset: 2px;
}

/* Ferestrele */
.fereastra {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px;
  border-radius: 14px;
  background: var(--suprafata);
  box-shadow: var(--umbra-plutitor);
  color: var(--text);
}
.fereastra h3 {
  margin: 0 0 -10px;
  font-size: 17px;
}
.descriere {
  color: var(--text-2);
  font-size: 12.5px;
}
.eticheta-camp {
  margin-bottom: 6px;
  font-size: 12.5px;
  font-weight: 600;
}
.roluri {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.placa {
  padding: 10px 12px;
  border: 1.5px solid var(--margine);
  border-radius: 10px;
  color: var(--text-2);
  font-size: 11.5px;
  line-height: 1.35;
  text-align: left;
  cursor: pointer;
}
.placa b {
  display: block;
  margin-bottom: 3px;
  color: var(--text);
  font-size: 13px;
}
.placa.activa {
  border-color: var(--primar);
  background: var(--primar-slab);
}
.fund {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 4px;
}

/* Telefon */
.pagina.telefon {
  /* Loc liber sub ultimul rând, ca butonul care plutește să nu-l acopere. */
  padding: 12px 16px 88px;
}
.sub.sus {
  margin: 0 0 10px;
  font-size: 12px;
}
.telefon .inchide {
  width: var(--atingere);
  height: var(--atingere);
  margin: -12px -10px -12px 0;
}
.telefon .lista {
  padding: 0 12px;
}
.telefon .rand {
  gap: 10px;
  padding: 11px 0;
}
.telefon .text {
  flex: 1;
  min-width: 0;
}
.telefon .nume {
  font-size: 14px;
}
.telefon .eu {
  background: var(--bleumarin);
  color: var(--suprafata);
  font-size: 10px;
}
.telefon .jos {
  margin-top: 2px;
  color: var(--text-2);
  font-size: 11.5px;
}
.telefon .rol {
  padding: 2px 8px;
  font-size: 10.5px;
}
.puncte {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: var(--atingere);
  height: var(--atingere);
  margin-right: -6px;
  border-radius: var(--colt-control);
  color: var(--text-2);
  cursor: pointer;
}
.meniu-cont {
  display: flex;
  flex-direction: column;
  width: 200px;
  padding: 6px;
  border-radius: 10px;
  background: var(--suprafata);
  box-shadow: var(--umbra-plutitor);
}
.meniu-cont button {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: var(--atingere);
  padding: 0 10px;
  border-radius: 7px;
  color: var(--text);
  font-size: 14px;
  text-align: left;
  cursor: pointer;
}
.meniu-cont button.rosu {
  color: var(--rosu-text);
}
.plutitor {
  position: fixed;
  right: 16px;
  bottom: 16px;
  z-index: 5;
  height: 50px;
  padding: 0 18px;
  border-radius: 14px;
  box-shadow: var(--umbra-plutitor);
  font-size: 14.5px;
}
.fereastra.plina {
  height: 100%;
  gap: 12px;
  padding: 0 16px 16px;
  border-radius: 0;
  box-shadow: none;
}
.cap {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 52px;
  flex-shrink: 0;
  margin: 0 -16px;
  padding: 0 16px 0 4px;
  border-bottom: 1px solid var(--margine);
  font-size: 16px;
  font-weight: 600;
}
.x {
  display: grid;
  place-items: center;
  width: var(--atingere);
  height: var(--atingere);
  color: var(--text);
  cursor: pointer;
}
.avertisment {
  padding: 8px 10px;
  border-radius: var(--colt-control);
  background: var(--ambra-slab);
  color: var(--ambra-text);
  font-size: 12.5px;
}
.plina .fund {
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  margin-top: auto;
}
.plina .fund > * {
  min-height: var(--atingere);
  font-size: 14.5px;
}
</style>

<style>
/* Fereastra mică de pe telefon: Vuetify îi dă 24 px de margine, iar spec-ul cere 16 (§10.6). */
.fereastra-telefon {
  margin: 16px;
}
.fereastra-telefon .fund > * {
  flex: 1;
  min-height: var(--atingere);
}
</style>
