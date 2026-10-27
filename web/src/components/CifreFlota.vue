<template>
  <div class="cifre-flota">
    <div class="placa">
      <div class="eticheta-mica">Mesaje pe secundă</div>
      <div class="valoare cifre">{{ numar(mesaje, 1) }}</div>
    </div>
    <div class="placa">
      <div class="eticheta-mica">Latența medie</div>
      <div class="valoare cifre">
        <template v-if="latenta !== null">{{ latenta }}<small>ms</small></template>
        <template v-else>—</template>
      </div>
    </div>
    <div class="placa">
      <div class="eticheta-mica">Recuperate</div>
      <div class="valoare cifre">{{ suma('recuperate') }}</div>
    </div>
    <div class="placa">
      <div class="eticheta-mica">Pierdute</div>
      <div class="valoare cifre" :class="{ rau: suma('pierdute') > 0 }">{{ suma('pierdute') }}</div>
    </div>
  </div>
</template>

<script>
import { flota } from '../live.js'
import { numar } from '../text.js'

/** Cele patru cifre mari ale flotei, pe Rețea (redesign §4.6). Pe telefon stau două câte două. */
export default {
  name: 'CifreFlota',
  computed: {
    mesaje() {
      return flota.statistici.reduce((s, x) => s + x.mesajePeSecunda, 0)
    },
    /** Ultima secundă care are o valoare: cât canalul tace, nu se arată 0 în loc de „nu știm”. */
    latenta() {
      const v = [...flota.latentaFlotei].reverse().find((x) => x != null)
      return v == null ? null : Math.round(v)
    },
  },
  methods: {
    numar,
    suma(camp) {
      return flota.statistici.reduce((s, x) => s + x[camp], 0)
    },
  },
}
</script>

<style scoped>
.cifre-flota {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.placa {
  padding: 11px 14px;
  border: 1px solid var(--margine);
  border-radius: var(--colt-card);
  background: var(--suprafata);
}
.valoare {
  margin-top: 2px;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.valoare small {
  margin-left: 2px;
  color: var(--text-2);
  font-size: 12px;
  font-weight: 500;
}
.rau {
  color: var(--rosu-text);
}
@media (max-width: 999px) {
  .cifre-flota {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }
  .valoare {
    font-size: 20px;
  }
}
</style>
