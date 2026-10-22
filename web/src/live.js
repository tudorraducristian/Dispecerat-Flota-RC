import { reactive } from 'vue'
import { api } from './api.js'

/** Tabla dispecerului: ce știe UI-ul acum despre flotă și despre rețea. O citesc toate componentele. */
export const flota = reactive({
  conectat: false,
  masini: {}, // id → { id, zonaId, stare, ultimaVedere, recuperate, pierdute, pozitie }
  vreme: {}, // id → descrierea de la proxy, de exemplu „TUNEL 12”
  proxyDisponibil: true,
  statistici: [], // ultima listă primită pe STATISTICI
  latentaFlotei: [], // media flotei, una pe secundă, ultimele 60
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

/** Vremea fiecărei mașini, din STARE-ul proxy-ului. Dacă proxy-ul nu răspunde, o spune (spec §6.5). */
export async function actualizeazaVremea() {
  try {
    const vreme = {}
    for (const r of await api.stareRetea()) vreme[r.masina] = r.descriere
    flota.vreme = vreme
    flota.proxyDisponibil = true
  } catch {
    flota.proxyDisponibil = false
  }
}

/** STARE la 3 secunde: tunelurile și găurile se termină singure, fără vreun eveniment (D26). */
export function pornesteVremea() {
  actualizeazaVremea()
  setInterval(actualizeazaVremea, 3000)
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

function aplica(tip, date) {
  switch (tip) {
    case 'POZITIE':
      aplicaPozitie(date)
      break
    case 'STARE':
      aplicaStare(date)
      break
    case 'EVENIMENT_RETEA':
      actualizeazaVremea()
      break
    case 'STATISTICI':
      aplicaStatistici(date)
      break
  }
}

/** Orele vin ca text ISO; se compară ca momente, nu ca text: „03Z” și „03.120Z” s-ar încurca. */
function maiNoua(a, b) {
  return !b || Date.parse(a) >= Date.parse(b)
}

function aplicaPozitie(date) {
  const m = flota.masini[date.masinaId]
  if (!m) return
  // O poziție recuperată din buffer e mai veche; markerul rămâne pe cea mai nouă.
  if (!m.pozitie || maiNoua(date.oraMasinii, m.pozitie.oraMasinii)) m.pozitie = date
  m.ultimaVedere = date.oraSosirii
}

function aplicaStare(date) {
  const m = flota.masini[date.masinaId]
  if (!m) return
  m.stare = date.stare
  m.ultimaVedere = date.ultimaVedere
}

const SECUNDE_GRAFIC = 60

/** O dată pe secundă: contoarele fiecărei mașini și un punct nou în graficul de latență. */
function aplicaStatistici(lista) {
  flota.statistici = lista
  for (const s of lista) {
    const m = flota.masini[s.masinaId]
    if (m) {
      m.recuperate = s.recuperate
      m.pierdute = s.pierdute
    }
  }
  const latente = lista.map((s) => s.latentaMedieMs).filter((v) => v != null)
  flota.latentaFlotei.push(latente.length ? latente.reduce((a, b) => a + b, 0) / latente.length : null)
  if (flota.latentaFlotei.length > SECUNDE_GRAFIC) flota.latentaFlotei.shift()
}
