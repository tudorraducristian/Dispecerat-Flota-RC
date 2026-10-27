<template>
  <v-navigation-drawer
    :model-value="deschis"
    :permanent="!mobil"
    :temporary="mobil"
    :width="mobil ? latimeSertar : 232"
    disable-resize-watcher
    touchless
    class="meniu"
    @update:model-value="(v) => !v && inchide()"
  >
    <div class="interior">
      <div v-if="mobil" class="sus">
        <span class="logo"><Route :size="16" /></span>
        <div>
          <div class="t1">Dispecerat flotă</div>
          <div class="t2">{{ numarMasini }} mașini</div>
        </div>
      </div>

      <nav aria-label="Paginile">
        <template v-for="grup in grupuri" :key="grup.nume">
          <div class="grup">{{ grup.nume }}</div>
          <router-link
            v-for="p in grup.pagini"
            :key="p.cale"
            v-slot="{ href, navigate, isExactActive }"
            :to="p.cale"
            custom
          >
            <a
              :href="href"
              class="element"
              :class="{ activ: isExactActive }"
              :aria-current="isExactActive ? 'page' : undefined"
              @click="mergi($event, navigate, p.cale)"
            >
              <component :is="p.iconita" :size="18" class="iconita" />
              <span>{{ p.titlu }}</span>
              <span v-if="p.cale === '/alarme' && cifra" class="cifra cifre">{{ cifra }}</span>
            </a>
          </router-link>
        </template>
      </nav>

      <div v-if="mobil" class="jos utilizator">
        <span class="avatar">{{ initiala(sesiune.nume) }}</span>
        <div class="nume">
          <div class="u1">{{ sesiune.nume }}</div>
          <div class="u2">{{ numeRol(sesiune.rol) }}</div>
        </div>
        <button type="button" class="iesire" aria-label="Ieșire" @click="iesi">
          <LogOut :size="20" />
        </button>
      </div>
      <div v-else class="jos semnatura">Proiect RC · UTCN 2026</div>
    </div>
  </v-navigation-drawer>
</template>

<script>
import { Activity, Bell, LogOut, Map as Harta, Route, Settings, Users } from 'lucide-vue-next'
import { flota } from '../live.js'
import { sesiune, esteAdmin } from '../sesiune.js'
import { numarAlarme } from '../numarAlarme.js'
import { initiala, numeRol, textCifra } from '../text.js'

/**
 * Meniul (redesign §4.3, varianta 1): pe laptop stă lângă pagină, iar ☰ îl ascunde. Pe telefon (§10.3) e un
 * sertar peste pagină, iar butonul Înapoi al telefonului îl închide, ca la orice aplicație de Android.
 */
export default {
  name: 'MeniuLateral',
  components: { LogOut, Route },
  props: {
    deschis: { type: Boolean, required: true },
  },
  emits: ['inchide', 'iesire'],
  data() {
    return { flota, sesiune, numarAlarme, intrareIstoric: false }
  },
  computed: {
    mobil() {
      return this.$vuetify.display.mobile
    },
    latimeSertar() {
      return Math.min(Math.round(this.$vuetify.display.width * 0.78), 300)
    },
    numarMasini() {
      return Object.keys(flota.masini).length
    },
    cifra() {
      return textCifra(numarAlarme.nevazute)
    },
    grupuri() {
      const administrare = [{ cale: '/setari', titlu: 'Setări', iconita: Settings }]
      if (esteAdmin()) administrare.push({ cale: '/conturi', titlu: 'Conturi', iconita: Users })
      return [
        {
          nume: 'Operare',
          pagini: [
            { cale: '/', titlu: 'Harta', iconita: Harta },
            { cale: '/retea', titlu: 'Rețea', iconita: Activity },
            { cale: '/alarme', titlu: 'Alarme', iconita: Bell },
          ],
        },
        { nume: 'Administrare', pagini: administrare },
      ]
    },
  },
  watch: {
    // Sertarul deschis primește o intrare în istoric, ca Înapoi să-l închidă pe el, nu să plece de pe pagină.
    deschis(acum) {
      if (acum && this.mobil && !this.intrareIstoric) {
        history.pushState({ ...history.state, sertar: true }, '')
        this.intrareIstoric = true
      } else if (!acum && this.intrareIstoric) {
        this.scoateIntrarea()
      }
    },
    mobil(acum) {
      if (!acum && this.intrareIstoric) this.scoateIntrarea()
    },
  },
  mounted() {
    window.addEventListener('popstate', this.laInapoi)
  },
  beforeUnmount() {
    window.removeEventListener('popstate', this.laInapoi)
  },
  methods: {
    initiala,
    numeRol,
    laInapoi() {
      if (!this.intrareIstoric) return
      this.intrareIstoric = false
      this.$emit('inchide')
    },
    /** Scoate intrarea sertarului din istoric; promisiunea se împlinește când browserul a făcut pasul înapoi. */
    scoateIntrarea() {
      this.intrareIstoric = false
      return new Promise((gata) => {
        window.addEventListener('popstate', () => gata(), { once: true })
        history.back()
      })
    },
    inchide() {
      if (this.deschis) this.$emit('inchide')
    },
    /**
     * Pe telefon, intrarea sertarului iese din istoric înainte de schimbarea paginii: altfel, pasul înapoi făcut
     * după ar anula chiar pagina aleasă.
     */
    async mergi(e, navigate, cale) {
      if (!this.mobil) return navigate(e)
      e.preventDefault()
      if (this.intrareIstoric) await this.scoateIntrarea()
      this.inchide()
      if (this.$route.path !== cale) this.$router.push(cale)
    },
    async iesi() {
      if (this.intrareIstoric) await this.scoateIntrarea()
      this.inchide()
      this.$emit('iesire')
    },
  },
}
</script>

<style scoped>
.meniu {
  background: var(--bleumarin) !important;
  color: var(--pe-bleumarin-text) !important;
  border-right: 0 !important;
}
.interior {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  padding: 12px 10px;
}
.sus {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px 14px;
  margin-bottom: 6px;
  border-bottom: 1px solid var(--bleumarin-2);
  color: var(--fond);
}
.logo {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: var(--colt-control);
  background: var(--primar);
  color: var(--suprafata);
}
.t1 {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.1;
}
.t2 {
  font-size: 11px;
  color: var(--text-3);
}
nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.grup {
  padding: 10px 12px 6px;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-2);
}
.element {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 12px;
  border-radius: var(--colt-control);
  color: inherit;
  font-size: 13.5px;
  font-weight: 500;
  text-decoration: none;
}
.element:hover,
.element:focus-visible {
  background: var(--bleumarin-2);
}
.element.activ {
  background: var(--bleumarin-2);
  color: var(--suprafata);
}
.element.activ::before {
  content: '';
  position: absolute;
  left: -10px;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--primar);
}
.element.activ .iconita {
  color: var(--pe-bleumarin-activ);
}
.cifra {
  margin-left: auto;
  padding: 1px 7px;
  border-radius: 999px;
  background: var(--rosu);
  color: var(--suprafata);
  font-size: 11px;
  font-weight: 700;
}
.jos {
  margin-top: auto;
  border-top: 1px solid var(--bleumarin-2);
}
.semnatura {
  padding: 10px 12px;
  font-size: 11px;
  line-height: 1.4;
  color: var(--text-2);
}
.utilizator {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 4px 0 8px;
}
.avatar {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--pe-bleumarin-linie);
  color: var(--margine);
  font-size: 13px;
  font-weight: 600;
}
.nume {
  flex: 1;
  min-width: 0;
}
.u1 {
  font-size: 13px;
  font-weight: 600;
  color: var(--fond);
  overflow: hidden;
  text-overflow: ellipsis;
}
.u2 {
  font-size: 11px;
  color: var(--text-3);
}
.iesire {
  display: grid;
  place-items: center;
  width: var(--atingere);
  height: var(--atingere);
  border-radius: var(--colt-control);
  color: var(--pe-bleumarin-text);
  cursor: pointer;
}
/* Fundalul de sub sertar: Vuetify îl face negru la 0,2, prea slab pe o pagină albă; macheta are bleumarin la 55%. */
:global(.v-navigation-drawer__scrim) {
  background: var(--bleumarin);
  opacity: 0.55;
}
/* Pe telefon, fiecare rând se apasă cu degetul: cel puțin 44 px. */
@media (max-width: 999px) {
  .element {
    min-height: var(--atingere);
    font-size: 14px;
  }
}
</style>
