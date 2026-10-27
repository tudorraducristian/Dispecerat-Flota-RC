<template>
  <label class="camp">
    <span class="eticheta-camp">{{ eticheta }}</span>
    <span class="intrare" :class="{ citire }">
      <input
        :type="tipInput"
        :value="modelValue"
        :readonly="citire"
        :autocomplete="autocomplete"
        :placeholder="placeholder"
        :min="min"
        :max="max"
        :inputmode="tip === 'numar' ? 'numeric' : undefined"
        :class="{ cifre: tip === 'numar' }"
        @input="schimba"
      />
      <small v-if="sufix">{{ sufix }}</small>
      <button
        v-if="tip === 'parola'"
        type="button"
        class="ochi"
        :aria-label="vizibila ? 'Ascunde parola' : 'Arată parola'"
        @click="vizibila = !vizibila"
      >
        <EyeOff v-if="vizibila" :size="16" />
        <Eye v-else :size="16" />
      </button>
    </span>
    <span v-if="ajutor" class="ajutor">{{ ajutor }}</span>
  </label>
</template>

<script>
import { Eye, EyeOff } from 'lucide-vue-next'

/**
 * Un câmp de formular în stilul nou (redesign §4.8–4.10): eticheta deasupra, nu doar placeholder, ajutorul dedesubt,
 * sufixul în câmp și, la parole, ochiul care arată ce ai scris.
 */
export default {
  name: 'CampFormular',
  components: { Eye, EyeOff },
  props: {
    modelValue: { type: [String, Number], default: null },
    eticheta: { type: String, required: true },
    tip: { type: String, default: 'text', validator: (t) => ['text', 'parola', 'numar'].includes(t) },
    ajutor: { type: String, default: null },
    sufix: { type: String, default: null },
    citire: { type: Boolean, default: false },
    autocomplete: { type: String, default: 'off' },
    placeholder: { type: String, default: null },
    min: { type: Number, default: null },
    max: { type: Number, default: null },
  },
  emits: ['update:modelValue'],
  data() {
    return { vizibila: false }
  },
  computed: {
    tipInput() {
      if (this.tip === 'numar') return 'number'
      return this.tip === 'parola' && !this.vizibila ? 'password' : 'text'
    },
  },
  methods: {
    schimba(e) {
      const valoare = e.target.value
      // Un câmp numeric gol trimite null, nu 0: serverul spune singur că lipsește valoarea.
      if (this.tip === 'numar') this.$emit('update:modelValue', valoare === '' ? null : Number(valoare))
      else this.$emit('update:modelValue', valoare)
    },
  },
}
</script>

<style scoped>
.camp {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.eticheta-camp {
  color: var(--text);
  font-size: 12.5px;
  font-weight: 600;
}
.intrare {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 11px;
  border: 1px solid var(--text-3);
  border-radius: var(--colt-control);
  background: var(--suprafata);
}
.intrare:focus-within {
  border-color: var(--primar);
  box-shadow: 0 0 0 3px var(--primar-slab);
}
.intrare.citire {
  border-color: var(--margine);
  background: var(--fond);
  color: var(--text-2);
  box-shadow: none;
}
.intrare input {
  flex: 1;
  min-width: 0;
  padding: 9px 0;
  border: 0;
  outline: 0;
  background: none;
  color: inherit;
  font: inherit;
  font-size: 13.5px;
}
.intrare small {
  color: var(--text-2);
  font-size: 12.5px;
}
.ochi {
  display: grid;
  place-items: center;
  align-self: stretch;
  width: 32px;
  margin-right: -6px;
  color: var(--text-3);
  cursor: pointer;
}
.ochi:focus-visible {
  outline: 2px solid var(--primar);
  outline-offset: -2px;
  border-radius: 6px;
}
.ajutor {
  color: var(--text-2);
  font-size: 11.5px;
}

/* Pe telefon câmpul e cât zona de atingere, iar ochiul la fel de lat (redesign §10, Constrângeri). */
@media (max-width: 999px) {
  .intrare {
    min-height: var(--atingere);
  }
  .ochi {
    width: var(--atingere);
    margin-right: -11px;
  }
}
</style>
