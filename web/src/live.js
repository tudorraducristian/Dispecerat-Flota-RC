import { reactive } from 'vue'
import { api } from './api.js'
import { sesiune } from './sesiune.js'

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

/** Mașinile care au primit un STARE live de la ultima deschidere a canalului. */
const stariLive = new Set()

export async function incarcaMasini() {
  for (const m of await api.masini()) {
    flota.masini[m.id] = imbinaMasina(flota.masini[m.id], m, stariLive.has(m.id))
  }
}

/**
 * Instantaneul din REST pleacă după ce canalul live s-a deschis, deci până sosește pot veni mesaje live mai
 * noi decât el. Se păstrează poziția și ultimaVedere mai noi, iar starea live doar dacă a venit după
 * deschidere: una rămasă dinainte, de exemplu de dinaintea unei reporniri a serverului, nu mai e adevărată.
 */
export function imbinaMasina(existenta, dinRest, stareLive) {
  const noua = { ...dinRest, pozitie: dinRest.ultimaPozitie }
  if (!existenta) return noua
  if (existenta.pozitie && maiNoua(existenta.pozitie.oraMasinii, noua.pozitie?.oraMasinii)) {
    noua.pozitie = existenta.pozitie
  }
  if (existenta.ultimaVedere && maiNoua(existenta.ultimaVedere, noua.ultimaVedere)) {
    noua.ultimaVedere = existenta.ultimaVedere
  }
  if (stareLive) noua.stare = existenta.stare
  return noua
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

let intervalVreme = null

/** STARE la 3 secunde: tunelurile și găurile se termină singure, fără vreun eveniment (D26). */
export function pornesteVremea() {
  opresteVremea()
  actualizeazaVremea()
  intervalVreme = setInterval(actualizeazaVremea, 3000)
}

export function opresteVremea() {
  clearInterval(intervalVreme)
  intervalVreme = null
}

let ws = null
let oprireCeruta = false
let reconectare = null

/**
 * Deschide canalul live, cu token-ul în adresă: browserul nu poate pune header-e la WebSocket (spec §16.2).
 * Pornește abia după login, fiindcă fără token serverul refuză handshake-ul.
 */
export function pornesteLive() {
  oprireCeruta = false
  clearTimeout(reconectare)
  const canal = new WebSocket(`ws://${location.host}/ws?token=${encodeURIComponent(sesiune.token ?? '')}`)
  ws = canal
  canal.onopen = () => {
    flota.conectat = true
    stariLive.clear()
    // Ce s-a întâmplat cât canalul a fost închis se ia din REST.
    incarcaMasini().catch((e) => console.warn('nu pot încărca mașinile:', e.message))
  }
  canal.onclose = () => {
    if (canal !== ws) return // un canal vechi, înlocuit între timp
    flota.conectat = false
    if (!oprireCeruta) dupaInchidere()
  }
  canal.onmessage = (e) => {
    const { tip, date } = JSON.parse(e.data)
    aplica(tip, date)
    ascultatori.forEach((f) => f(tip, date))
  }
}

/**
 * Canalul s-a închis fără să fi cerut noi. Întreb serverul cine sînt: dacă răspunde, reîncerc la 2 secunde;
 * la 401, api.js a trimis deja la /login; la o eroare de rețea, cu serverul oprit, reîncerc tot la 2 secunde.
 */
function dupaInchidere() {
  api
    .eu()
    .then(programeazaReconectarea)
    .catch((e) => {
      if (e.status !== 401) programeazaReconectarea()
    })
}

function programeazaReconectarea() {
  clearTimeout(reconectare)
  reconectare = setTimeout(() => {
    if (!oprireCeruta && sesiune.token) pornesteLive()
  }, 2000)
}

/** La ieșire: închide canalul și nu mai încearcă să-l redeschidă. */
export function opresteLive() {
  oprireCeruta = true
  clearTimeout(reconectare)
  if (ws) ws.close()
  ws = null
  flota.conectat = false
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
  stariLive.add(date.masinaId)
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
