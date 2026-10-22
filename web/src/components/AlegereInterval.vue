<template>
  <v-card variant="outlined" class="ma-2">
    <v-card-title>Istoric</v-card-title>
    <v-card-text>
      <v-text-field v-model="de" type="datetime-local" label="De la" density="compact" />
      <v-text-field v-model="pana" type="datetime-local" label="Până la" density="compact" />
      <div class="d-flex ga-2">
        <v-btn :disabled="!de || !pana" @click="arata">Arată</v-btn>
        <v-btn variant="text" @click="$emit('live')">Înapoi la live</v-btn>
      </div>
    </v-card-text>
  </v-card>
</template>

<script>
export default {
  name: 'AlegereInterval',
  emits: ['interval', 'live'],
  data() {
    return { de: '', pana: '' }
  },
  methods: {
    /** datetime-local dă ora locală, fără fus; serverul vrea UTC. */
    arata() {
      this.$emit('interval', { de: new Date(this.de).toISOString(), pana: new Date(this.pana).toISOString() })
    },
  },
}
</script>
