import { sesiune, uita } from './sesiune.js'

const LOGIN = '/api/auth/login'

/**
 * Ce face UI-ul când serverul spune 401: îl setează main.js, cu trimiterea la /login. Astfel api.js nu importă
 * router-ul, iar router-ul, care importă paginile, nu ajunge să se importe singur prin api.js.
 */
let laNeautorizat = () => {}

export function seteazaLaNeautorizat(f) {
  laNeautorizat = f
}

/** Toate cererile REST trec pe aici. Vite le trimite mai departe la serverul de pe 8080. */
async function cere(cale, optiuni = {}) {
  const headers = { 'Content-Type': 'application/json', ...(optiuni.headers ?? {}) }
  if (sesiune.token) headers.Authorization = `Bearer ${sesiune.token}`
  const raspuns = await fetch(cale, { ...optiuni, headers })
  const corp = raspuns.status === 204 ? null : await raspuns.json().catch(() => null)
  // Un 401 în afara login-ului înseamnă token expirat sau anulat: omul e scos din aplicație.
  if (raspuns.status === 401 && cale !== LOGIN) {
    uita()
    laNeautorizat()
  }
  if (!raspuns.ok) {
    const eroare = new Error(corp?.mesaj ?? `serverul a răspuns ${raspuns.status}`)
    eroare.status = raspuns.status
    throw eroare
  }
  return corp
}

export const api = {
  login: (nume, parola) => cere(LOGIN, { method: 'POST', body: JSON.stringify({ nume, parola }) }),
  logout: () => cere('/api/auth/logout', { method: 'POST' }),
  eu: () => cere('/api/auth/eu'),
  schimbaParola: (veche, noua) =>
    cere('/api/auth/parola', { method: 'PUT', body: JSON.stringify({ parolaVeche: veche, parolaNoua: noua }) }),
  masini: () => cere('/api/masini'),
  masina: (id) => cere(`/api/masini/${id}`),
  traseu: (id, de, pana) => {
    const parametri = new URLSearchParams()
    if (de) parametri.set('de', de)
    if (pana) parametri.set('pana', pana)
    return cere(`/api/masini/${id}/traseu?${parametri}`)
  },
  zone: () => cere('/api/zone'),
  stareRetea: () => cere('/api/retea/stare'),
  vreme: (cerere) => cere('/api/retea/vreme', { method: 'POST', body: JSON.stringify(cerere) }),
  evenimente: () => cere('/api/retea/evenimente'),
  alarme: (vazute, tip) => {
    const parametri = new URLSearchParams()
    if (vazute != null) parametri.set('vazute', vazute)
    if (tip != null) parametri.set('tip', tip)
    return cere(`/api/alarme?${parametri}`)
  },
  numarAlarme: () => cere('/api/alarme/numar'),
  marcheazaVazuta: (id) => cere(`/api/alarme/${id}`, { method: 'PATCH' }),
  marcheazaToateVazute: (tip) =>
    cere('/api/alarme/vazute' + (tip == null ? '' : `?tip=${encodeURIComponent(tip)}`), { method: 'POST' }),
  trimiteComanda: (id, cerere) => cere(`/api/masini/${id}/comenzi`, { method: 'POST', body: JSON.stringify(cerere) }),
  comenzi: (id) => cere(`/api/masini/${id}/comenzi`),
  utilizatori: () => cere('/api/utilizatori'),
  creeazaCont: (cerere) => cere('/api/utilizatori', { method: 'POST', body: JSON.stringify(cerere) }),
  stergeCont: (id) => cere(`/api/utilizatori/${id}`, { method: 'DELETE' }),
  reseteazaParola: (id, noua) =>
    cere(`/api/utilizatori/${id}/parola`, { method: 'PUT', body: JSON.stringify({ parolaNoua: noua }) }),
  setari: () => cere('/api/setari'),
  salveazaSetari: (s) => cere('/api/setari', { method: 'PUT', body: JSON.stringify(s) }),
}
