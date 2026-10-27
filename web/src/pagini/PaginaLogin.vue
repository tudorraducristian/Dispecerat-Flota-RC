<template>
  <div class="login">
    <!-- Pe laptop, stânga; sub 1000 px, banda de sus, fără cele trei rânduri cu iconiță (redesign §10.6). -->
    <div class="prezentare">
      <div class="fond" aria-hidden="true" />
      <div class="titlu">
        <span class="logo"><Route :size="18" /></span>
        <span>Dispecerat flotă</span>
      </div>
      <p class="fraza">Zece mașini, o rețea care se strică la comandă și un dispecerat care rezistă.</p>
      <div class="fapte">
        <div class="fapt"><span><Radio :size="15" /></span>MQTT, cu QoS 0, 1 și 2</div>
        <div class="fapt"><span><Mountain :size="15" /></span>Proxy cu tunel, gaură, întârziere și bandă</div>
        <div class="fapt"><span><Harta :size="15" /></span>Hartă live a Clujului</div>
        <div class="subsol">Proiect la Rețele de Calculatoare · UTCN · 2026</div>
      </div>
    </div>

    <div class="dreapta">
      <form class="formular" @submit.prevent="intra">
        <div>
          <h1>Intră în cont</h1>
          <div class="descriere">Cu contul primit de la administrator.</div>
        </div>
        <CampFormular v-model="nume" eticheta="Nume" autocomplete="username" autofocus />
        <CampFormular v-model="parola" eticheta="Parolă" tip="parola" autocomplete="current-password" />
        <div v-if="eroare" class="mesaj eroare" role="alert">{{ eroare }}</div>
        <button type="submit" class="buton-primar intra" :disabled="seIncarca">Intră</button>
      </form>
      <div class="subsol">Proiect la Rețele de Calculatoare · UTCN · 2026</div>
    </div>
  </div>
</template>

<script>
import { Map as Harta, Mountain, Radio, Route } from 'lucide-vue-next'
import CampFormular from '../components/CampFormular.vue'
import { api } from '../api.js'
import { salveaza } from '../sesiune.js'

/** Login-ul, varianta 2: ecranul împărțit (redesign §4.10); pe telefon, banda sus și formularul dedesubt (§10.6). */
export default {
  name: 'PaginaLogin',
  components: { CampFormular, Harta, Mountain, Radio, Route },
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

<style scoped>
.login {
  display: flex;
  min-height: 100vh;
  min-height: 100dvh;
}
.prezentare {
  position: relative;
  display: flex;
  flex: 1.1;
  flex-direction: column;
  overflow: hidden;
  padding: 34px;
  background: var(--bleumarin);
  color: var(--fond);
}
/* Harta stilizată: două rânduri de străzi oblice, estompate. */
.fond {
  position: absolute;
  inset: 0;
  opacity: 0.55;
  background:
    repeating-linear-gradient(35deg, transparent 0 46px, var(--bleumarin-2) 46px 48px),
    repeating-linear-gradient(-55deg, transparent 0 64px, var(--bleumarin-2) 64px 66px);
}
.prezentare > :not(.fond) {
  position: relative;
}
.titlu {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 600;
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
.fraza {
  max-width: 340px;
  margin: auto 0 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.2;
}
.fapt {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  color: var(--pe-bleumarin-text);
  font-size: 13px;
}
.fapt span {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: var(--colt-control);
  background: var(--bleumarin-2);
  color: var(--pe-bleumarin-activ);
}
.subsol {
  margin-top: 24px;
  color: var(--text-2);
  font-size: 11.5px;
}
.dreapta {
  display: grid;
  flex: 1;
  place-items: center;
  background: var(--suprafata);
}
.dreapta > .subsol {
  display: none;
}
.formular {
  display: flex;
  flex-direction: column;
  gap: 14px;
  width: 300px;
}
h1 {
  margin: 0;
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.descriere {
  margin-top: 2px;
  color: var(--text-2);
  font-size: 13px;
}
.intra {
  width: 100%;
  margin-top: 4px;
  padding: 11px;
  font-size: 14px;
}

/* Sub 1000 px: o coloană, banda bleumarin sus, formularul pe alb dedesubt (redesign §10.6). */
@media (max-width: 999px) {
  .login {
    flex-direction: column;
  }
  .prezentare {
    flex: none;
    padding: 26px 20px 22px;
  }
  .fraza {
    margin-top: 16px;
    font-size: 17px;
    line-height: 1.3;
  }
  .fapte {
    display: none;
  }
  .dreapta {
    display: flex;
    flex-direction: column;
    place-items: stretch;
    padding: 22px 20px;
  }
  .formular {
    gap: 12px;
    width: 100%;
    max-width: 420px;
    align-self: center;
  }
  h1 {
    font-size: 20px;
  }
  .intra {
    min-height: var(--atingere);
    font-size: 14.5px;
  }
  .dreapta > .subsol {
    display: block;
    margin-top: auto;
    padding-top: 22px;
    color: var(--text-3);
    font-size: 10.5px;
    text-align: center;
  }
}
</style>
