import { reactive } from 'vue'
import { api } from './api.js'

/** Tabla dispecerului: ce știe UI-ul acum despre fiecare mașină. O citesc toate componentele. */
export const flota = reactive({
  conectat: false,
  masini: {}, // id → { id, zonaId, stare, ultimaVedere, recuperate, pierdute, pozitie }
})

const ascultatori = new Set()

/** f(tip, date) se cheamă la fiecare mesaj live. Întoarce funcția care oprește ascultarea. */
export function asculta(f) {
  ascultatori.add(f)
  return () => ascultatori.delete(f)
}

export async function incarcaMasini() {
  for (const m of await api.masini()) {
    flota.masini[m.id] = { ...m, pozitie: m.ultimaPozitie }
  }
}

/** Contoarele unei mașini, din REST. Până la faza 5, panoul le cere la 2 secunde (D25). */
export async function actualizeazaContoare(id) {
  const m = await api.masina(id)
  const tabla = flota.masini[id]
  if (tabla) {
    tabla.recuperate = m.recuperate
    tabla.pierdute = m.pierdute
  }
}

/** Deschide canalul live. Dacă se închide, inclusiv când serverul e oprit, reîncearcă la 2 secunde. */
export function pornesteLive() {
  const ws = new WebSocket(`ws://${location.host}/ws`)
  ws.onopen = () => {
    flota.conectat = true
    // Ce s-a întâmplat cât canalul a fost închis se ia din REST.
    incarcaMasini().catch((e) => console.warn('nu pot încărca mașinile:', e.message))
  }
  ws.onclose = () => {
    flota.conectat = false
    setTimeout(pornesteLive, 2000)
  }
  ws.onmessage = (e) => {
    const { tip, date } = JSON.parse(e.data)
    aplica(tip, date)
    ascultatori.forEach((f) => f(tip, date))
  }
}

/** Orele vin ca text ISO; se compară ca momente, nu ca text: „03Z” și „03.120Z” s-ar încurca. */
function maiNoua(a, b) {
  return !b || Date.parse(a) >= Date.parse(b)
}

function aplica(tip, date) {
  const m = flota.masini[date.masinaId]
  if (!m) return
  if (tip === 'POZITIE') {
    // O poziție recuperată din buffer e mai veche; markerul rămâne pe cea mai nouă.
    if (!m.pozitie || maiNoua(date.oraMasinii, m.pozitie.oraMasinii)) m.pozitie = date
    m.ultimaVedere = date.oraSosirii
  } else if (tip === 'STARE') {
    m.stare = date.stare
    m.ultimaVedere = date.ultimaVedere
  }
}
