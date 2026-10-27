// Face date/trasee.json: zece ture prin Cluj-Napoca, câte una pe mașină, pe străzile adevărate (D54).
// Fiecare tură pleacă dintr-o bază, trece prin locurile de mai jos și se întoarce în bază. Drumul pe străzi
// îl dă OSRM, pe datele OpenStreetMap; simulatorul citește doar fișierul, deci internetul trebuie doar aici.
//   node unelte\trasee.mjs
import { writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { BAZE as LISTA_BAZE } from '../web/src/baze.js'

// Bazele vin din același fișier ca semnele lor de pe hartă. [lat, lon]
const BAZE = Object.fromEntries(LISTA_BAZE.map((b) => [b.id, b.loc]))

const LOCURI = {
  unirii: [46.7695, 23.5897],
  mihai_viteazu: [46.774, 23.5925],
  avram_iancu: [46.7703, 23.5965],
  cipariu: [46.7684, 23.602],
  parcul_central: [46.7686, 23.5784],
  cluj_arena: [46.7686, 23.5722],
  gara: [46.7845, 23.5865],
  gruia: [46.782, 23.578],
  grigorescu: [46.77, 23.555],
  flora: [46.756, 23.5545],
  zorilor: [46.758, 23.583],
  iulius: [46.7718, 23.6262],
  intre_lacuri: [46.781, 23.626],
  iris: [46.793, 23.61],
  someseni: [46.777, 23.645],
  sopor: [46.744, 23.571],
  // în afara zonei „Cluj-Napoca”: aici apare, rar, alarma „a ieșit din zonă”
  floresti: [46.7475, 23.491],
  aeroport: [46.785, 23.686],
}

// Mașinile 1–3 sînt ale bazei din Mănăștur, 4–6 ale celei din Mărăști, 7–10 ale celei din Bună Ziua.
const TURE = [
  { id: 't1', baza: 'manastur', prin: ['parcul_central', 'unirii', 'mihai_viteazu', 'gruia', 'grigorescu'] },
  { id: 't2', baza: 'manastur', prin: ['floresti', 'grigorescu', 'cluj_arena'] },
  { id: 't3', baza: 'manastur', prin: ['zorilor', 'avram_iancu', 'cipariu', 'parcul_central'] },
  { id: 't4', baza: 'marasti', prin: ['gara', 'gruia', 'mihai_viteazu'] },
  { id: 't5', baza: 'marasti', prin: ['iris', 'aeroport', 'someseni'] },
  { id: 't6', baza: 'marasti', prin: ['cipariu', 'unirii', 'avram_iancu', 'iulius'] },
  { id: 't7', baza: 'buna_ziua', prin: ['zorilor', 'cluj_arena', 'grigorescu', 'flora'] },
  { id: 't8', baza: 'buna_ziua', prin: ['cipariu', 'iulius', 'intre_lacuri', 'avram_iancu'] },
  { id: 't9', baza: 'buna_ziua', prin: ['unirii', 'gara', 'mihai_viteazu'] },
  { id: 't10', baza: 'buna_ziua', prin: ['sopor', 'flora', 'parcul_central', 'unirii'] },
]

const OSRM = 'https://router.project-osrm.org/route/v1/driving/'

async function drum(puncte) {
  const coordonate = puncte.map(([lat, lon]) => `${lon},${lat}`).join(';')
  const raspuns = await fetch(`${OSRM}${coordonate}?overview=full&geometries=geojson`)
  const corp = await raspuns.json()
  if (corp.code !== 'Ok') throw new Error(`OSRM: ${corp.code} ${corp.message ?? ''}`)
  return corp.routes[0]
}

const rotunjit = (x) => Math.round(x * 1e6) / 1e6

const trasee = []
for (const tura of TURE) {
  const baza = BAZE[tura.baza]
  const ruta = await drum([baza, ...tura.prin.map((nume) => LOCURI[nume]), baza])
  const linie = ruta.geometry.coordinates.map(([lon, lat]) => [rotunjit(lon), rotunjit(lat)])
  const nume = [tura.baza, ...tura.prin, tura.baza].join(' → ')
  trasee.push({ type: 'Feature', properties: { id: tura.id, nume }, geometry: { type: 'LineString', coordinates: linie } })
  console.log(`${tura.id}: ${(ruta.distance / 1000).toFixed(1)} km, ${linie.length} puncte — ${nume}`)
  // Serviciul public cere cel mult o cerere pe secundă.
  await new Promise((r) => setTimeout(r, 1100))
}

// O linie pe traseu, cu coordonatele una după alta: fișierul rămâne mic și ușor de citit în diff.
const text = '{\n  "type": "FeatureCollection",\n  "features": [\n'
  + trasee.map((t) => '    ' + JSON.stringify(t)).join(',\n')
  + '\n  ]\n}\n'
const fisier = fileURLToPath(new URL('../date/trasee.json', import.meta.url))
writeFileSync(fisier, text)
console.log(`scris ${fisier}`)
