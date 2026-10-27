<template>
  <div ref="harta" class="harta"></div>
</template>

<script>
import L from 'leaflet'
import { flota } from '../live.js'
import { segmente } from '../traseu.js'
import { directie } from '../directie.js'
import { ALBASTRU_HARTA, CULORI_STARE, svgMasina } from '../masinaSvg.js'
import { numeMasina } from '../text.js'

export default {
  name: 'HartaFlotei',
  props: {
    zone: { type: Array, default: () => [] },
    selectata: { type: String, default: null },
    traseu: { type: Array, default: () => [] },
  },
  emits: ['alege'],
  data() {
    return { flota }
  },
  computed: {
    mobil() {
      return this.$vuetify.display.mobile
    },
  },
  watch: {
    'flota.vreme': {
      deep: true,
      handler() {
        this.deseneazaMasini()
      },
    },
    'flota.masini': {
      deep: true,
      handler() {
        this.deseneazaMasini()
      },
    },
    selectata() {
      this.deseneazaMasini()
    },
    // Peste prag se schimbă zona de atingere și tooltip-ul, deci markerele se fac din nou.
    mobil() {
      for (const marker of Object.values(this.markere)) marker.remove()
      this.markere = {}
      this.deseneazaMasini()
    },
    zone() {
      this.deseneazaZone()
    },
    traseu: {
      deep: true,
      handler() {
        this.deseneazaTraseu()
      },
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
    this.stratTraseu = L.layerGroup().addTo(this.harta)
    this.markere = {}
    // Locul de unde s-a calculat ultima direcție și direcția, pe mașină. Tot în afara lui data().
    this.ultimulLoc = {}
    this.directii = {}
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
          marker = L.marker(loc, { icon: this.iconita(), keyboard: false })
            .on('click', () => this.$emit('alege', m.id))
            .addTo(this.harta)
          // Pe telefon nu există hover: atingerea deschide mașina, iar numele apare în foaie (redesign §10.4).
          if (!this.mobil) marker.bindTooltip(numeMasina(m.id), { direction: 'top', offset: [0, -16] })
          this.markere[m.id] = marker
        }
        marker.setLatLng(loc)
        this.actualizeazaDirectia(m)
        this.deseneazaMasina(marker, m)
      }
    },
    /**
     * Pătratul markerului cuprinde mașina rotită în orice sens. Pe telefon e de 44 px, fiindcă un deget nu
     * nimerește desenul de 22 px; centrul pătratului e mereu poziția mașinii.
     */
    iconita() {
      const latura = this.mobil ? 44 : 36
      return L.divIcon({
        className: 'marker-masina',
        html: '<div class="masina"></div>',
        iconSize: [latura, latura],
        iconAnchor: [latura / 2, latura / 2],
      })
    },
    /**
     * Direcția se schimbă doar după cel puțin 1 m de mers, față de locul ultimei direcții. Altfel, o mașină
     * oprită s-ar roti după zgomotul poziției, iar una care merge încet n-ar mai avea niciodată o direcție.
     */
    actualizeazaDirectia(m) {
      const anterior = this.ultimulLoc[m.id]
      const acum = { lat: m.pozitie.lat, lon: m.pozitie.lon }
      if (!anterior) {
        this.ultimulLoc[m.id] = acum
        return
      }
      const d = directie(anterior, acum)
      if (d === null) return
      // Unghiul nu se readuce în [0, 360): de la 350° la 10°, mașina se întoarce cu 20°, nu cu 340° înapoi.
      const vechi = this.directii[m.id]
      this.directii[m.id] = vechi === undefined ? d : vechi + ((((d - vechi) % 360) + 540) % 360) - 180
      this.ultimulLoc[m.id] = acum
    },
    /** Rotirea stă pe div-ul interior: pe elementul markerului, Leaflet își pune propriul transform. */
    deseneazaMasina(marker, m) {
      const el = marker.getElement()?.querySelector('.masina')
      if (!el) return
      const culoare = this.culoare(m)
      if (el.dataset.culoare !== culoare) {
        el.innerHTML = svgMasina(culoare)
        el.dataset.culoare = culoare
      }
      el.style.transform = `rotate(${this.directii[m.id] ?? 0}deg)`
      el.classList.toggle('selectata', m.id === this.selectata)
      marker.setZIndexOffset(m.id === this.selectata ? 1000 : 0)
    },
    /** Verde online, gri offline, portocaliu dacă e online, dar are vreme setată (spec §7). */
    culoare(m) {
      const vreme = flota.vreme[m.id]
      if (m.stare === 'ONLINE' && vreme && vreme !== 'NORMAL') return CULORI_STARE.VREME
      return CULORI_STARE[m.stare] ?? CULORI_STARE.OFFLINE
    },
    deseneazaZone() {
      this.stratZone.clearLayers()
      const poligoane = this.zone.map((z) =>
        L.polygon(z.poligon, { color: ALBASTRU_HARTA, weight: 1.5, fillOpacity: 0.06 })
          .bindTooltip(`${z.nume}: maxim ${z.vitezaMaxima} km/h`)
          .addTo(this.stratZone),
      )
      if (poligoane.length > 0) {
        this.harta.fitBounds(L.featureGroup(poligoane).getBounds(), { padding: [20, 20] })
      }
    },
    /** Linie plină pentru live, punctată pentru ce s-a recuperat din buffer, nimic unde lipsesc mesaje. */
    deseneazaTraseu() {
      this.stratTraseu.clearLayers()
      for (const s of segmente(this.traseu)) {
        const stil = { color: ALBASTRU_HARTA, weight: 4, opacity: 0.85, lineCap: 'round' }
        // Aceleași puncte ca în legenda din fila Traseu.
        if (s.fel === 'recuperat') stil.dashArray = '1 7'
        L.polyline(s.puncte, stil).addTo(this.stratTraseu)
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
/* Markerele le face Leaflet în afara componentei, deci stilul lor e global. */
:global(.marker-masina) {
  display: grid;
  place-items: center;
  background: none;
  border: 0;
  cursor: pointer;
}
:global(.marker-masina .masina) {
  position: relative;
  width: 22px;
  height: 36px;
  transition: transform 300ms ease-out;
}
:global(.marker-masina .masina svg) {
  display: block;
  width: 22px;
  height: 36px;
  filter: drop-shadow(0 2px 3px rgba(15, 23, 42, 0.35));
}
/* Inelul mașinii alese, ca în machetă. */
:global(.marker-masina .masina.selectata::after) {
  content: '';
  position: absolute;
  inset: -9px -13px;
  z-index: -1;
  border: 2.5px solid var(--primar);
  border-radius: 50%;
  background: color-mix(in srgb, var(--primar) 12%, transparent);
}
@media (prefers-reduced-motion: reduce) {
  :global(.marker-masina .masina) {
    transition: none;
  }
}
</style>
