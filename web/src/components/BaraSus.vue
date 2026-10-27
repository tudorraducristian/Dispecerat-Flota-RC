<template>
  <v-app-bar :height="mobil ? 52 : 56" flat class="bara">
    <div class="continut" :class="{ mobil }">
      <button
        type="button"
        class="buton-iconita meniu"
        :aria-label="mobil && arePunct ? 'Meniu, alarme nevăzute' : 'Meniu'"
        @click="$emit('comuta-meniul')"
      >
        <Menu :size="20" />
        <span v-if="mobil && arePunct" class="punct-rosu" />
      </button>

      <template v-if="mobil">
        <span class="titlu-telefon">{{ $route.meta.titlu }}</span>
      </template>
      <div v-else class="titlu">
        <span class="logo"><Route :size="18" /></span>
        <div>
          <div class="t1">Dispecerat flotă</div>
          <div class="t2">{{ subtitlu }}</div>
        </div>
      </div>

      <span class="spatiu" />

      <span class="live" :class="{ cazut: !flota.conectat }" role="status">
        <span class="punct-live" />{{ flota.conectat ? 'LIVE' : 'FĂRĂ LEGĂTURĂ' }}
      </span>

      <template v-if="!mobil">
        <span class="separator" />
        <div v-if="sesiune.nume" class="utilizator">
          <span class="avatar">{{ initiala(sesiune.nume) }}</span>
          <div>
            <div class="u1">{{ sesiune.nume }}</div>
            <div class="u2">{{ numeRol(sesiune.rol) }}</div>
          </div>
        </div>
        <button type="button" class="buton-iconita iesire" aria-label="Ieșire" @click="$emit('iesire')">
          <LogOut :size="20" />
          <v-tooltip activator="parent" location="bottom">Ieșire</v-tooltip>
        </button>
      </template>
    </div>
  </v-app-bar>
</template>

<script>
import { LogOut, Menu, Route } from 'lucide-vue-next'
import { flota } from '../live.js'
import { sesiune } from '../sesiune.js'
import { numarAlarme } from '../numarAlarme.js'
import { initiala, numeRol } from '../text.js'

/** Bara de sus (redesign §4.2, varianta A). Pe telefon (§10.3) rămân doar ☰, titlul paginii și LIVE. */
export default {
  name: 'BaraSus',
  components: { LogOut, Menu, Route },
  emits: ['comuta-meniul', 'iesire'],
  data() {
    return { flota, sesiune, numarAlarme }
  },
  computed: {
    mobil() {
      return this.$vuetify.display.mobile
    },
    subtitlu() {
      const n = Object.keys(flota.masini).length
      return n ? `Cluj-Napoca · ${n} mașini` : 'Cluj-Napoca'
    },
    /** Pe telefon cifra din meniu se vede doar cu sertarul deschis; punctul spune că există ceva de văzut. */
    arePunct() {
      return numarAlarme.nevazute > 0
    },
  },
  methods: { initiala, numeRol },
}
</script>

<style scoped>
.bara {
  background: var(--bleumarin) !important;
  color: var(--fond) !important;
  border-bottom: 1px solid var(--bleumarin-2);
}
.continut {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  height: 100%;
  padding: 0 16px;
}
.continut.mobil {
  gap: 8px;
  padding: 0 8px 0 4px;
}
.buton-iconita {
  position: relative;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: var(--colt-control);
  color: var(--pe-bleumarin-text);
  cursor: pointer;
}
.buton-iconita:hover,
.buton-iconita:focus-visible {
  background: var(--bleumarin-2);
}
.mobil .buton-iconita {
  width: var(--atingere);
  height: var(--atingere);
}
.punct-rosu {
  position: absolute;
  top: 9px;
  right: 9px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--rosu);
  box-shadow: 0 0 0 2px var(--bleumarin);
}
.titlu {
  display: flex;
  align-items: center;
  gap: 10px;
}
.logo {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: var(--colt-control);
  background: var(--primar);
  color: var(--suprafata);
}
.t1 {
  font-size: 17px;
  font-weight: 600;
  line-height: 1.1;
  letter-spacing: -0.01em;
}
.t2 {
  margin-top: 2px;
  font-size: 11.5px;
  line-height: 1.2;
  color: var(--text-3);
}
.titlu-telefon {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.spatiu {
  flex: 1;
}
.live {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 5px 11px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--live) 25%, transparent);
  background: color-mix(in srgb, var(--live) 12%, transparent);
  color: var(--live-text);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.09em;
  white-space: nowrap;
}
.mobil .live {
  gap: 5px;
  padding: 4px 8px;
  font-size: 10px;
  letter-spacing: 0.08em;
}
.punct-live {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--live);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--live) 20%, transparent);
  animation: puls 1.6s ease-in-out infinite;
}
.live.cazut {
  border-color: color-mix(in srgb, var(--rosu) 40%, transparent);
  background: color-mix(in srgb, var(--rosu) 16%, transparent);
  color: var(--rosu-slab);
}
.live.cazut .punct-live {
  background: var(--rosu);
  box-shadow: none;
  animation: none;
}
@keyframes puls {
  50% {
    box-shadow: 0 0 0 6px transparent;
  }
}
@media (prefers-reduced-motion: reduce) {
  .punct-live {
    animation: none;
  }
}
.separator {
  width: 1px;
  height: 26px;
  background: var(--pe-bleumarin-linie);
}
.utilizator {
  display: flex;
  align-items: center;
  gap: 9px;
}
.avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--pe-bleumarin-linie);
  color: var(--margine);
  font-size: 13px;
  font-weight: 600;
}
.u1 {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.15;
}
.u2 {
  font-size: 11px;
  line-height: 1.2;
  color: var(--text-3);
}
</style>
