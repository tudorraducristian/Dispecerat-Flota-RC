<template>
  <div ref="harta" class="harta"></div>
</template>

<script>
import L from 'leaflet'
import { flota } from '../live.js'

const CULORI = { ONLINE: '#2e7d32', OFFLINE: '#9e9e9e' }

export default {
  name: 'HartaFlotei',
  props: {
    zone: { type: Array, default: () => [] },
    selectata: { type: String, default: null },
  },
  emits: ['alege'],
  data() {
    return { flota }
  },
  watch: {
    'flota.masini': {
      deep: true,
      handler() {
        this.deseneazaMasini()
      },
    },
    selectata() {
      this.deseneazaMasini()
    },
    zone() {
      this.deseneazaZone()
    },
  },
  mounted() {
    // Obiectele Leaflet stau pe `this`, dar nu în data(): Vue nu are voie să le facă reactive.
    this.harta = L.map(this.$refs.harta).setView([45.9, 24.9], 7)
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(this.harta)
    this.stratZone = L.layerGroup().addTo(this.harta)
    this.deseneazaZone()
    this.markere = {}
    this.deseneazaMasini()
  },
  beforeUnmount() {
    this.harta.remove()
  },
  methods: {
    deseneazaMasini() {
      for (const m of Object.values(flota.masini)) {
        if (!m.pozitie) continue
        const loc = [m.pozitie.lat, m.pozitie.lon]
        let marker = this.markere[m.id]
        if (!marker) {
          marker = L.circleMarker(loc, { radius: 8, weight: 2, fillOpacity: 0.9 })
            .bindTooltip(m.id)
            .on('click', () => this.$emit('alege', m.id))
            .addTo(this.harta)
          this.markere[m.id] = marker
        }
        marker.setLatLng(loc)
        marker.setStyle({
          color: m.id === this.selectata ? '#000000' : '#ffffff',
          fillColor: this.culoare(m),
        })
      }
    },
    culoare(m) {
      return CULORI[m.stare] ?? CULORI.OFFLINE
    },
    deseneazaZone() {
      this.stratZone.clearLayers()
      const poligoane = this.zone.map((z) =>
        L.polygon(z.poligon, { color: '#1565c0', weight: 1, fillOpacity: 0.08 })
          .bindTooltip(`${z.nume}: maxim ${z.vitezaMaxima} km/h`)
          .addTo(this.stratZone),
      )
      if (poligoane.length > 0) {
        this.harta.fitBounds(L.featureGroup(poligoane).getBounds(), { padding: [20, 20] })
      }
    },
  },
}
</script>

<style scoped>
.harta {
  height: 100%;
  min-height: 400px;
}
</style>
