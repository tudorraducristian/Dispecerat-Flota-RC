import { reactive } from 'vue'
import { api } from './api.js'

/**
 * Singurul loc pentru token (spec §16.2). Stă în localStorage, ca omul să rămână logat după o reîncărcare a
 * paginii; o variabilă simplă s-ar pierde la fiecare F5. Numele și rolul se iau din nou de la server.
 */
const CHEIE = 'flota.token'

function citesteToken() {
  try {
    return localStorage.getItem(CHEIE)
  } catch {
    return null // de exemplu, stocarea blocată de browser
  }
}

export const sesiune = reactive({ token: citesteToken(), nume: null, rol: null })

export function salveaza({ token, nume, rol }) {
  sesiune.token = token
  sesiune.nume = nume
  sesiune.rol = rol
  try {
    localStorage.setItem(CHEIE, token)
  } catch {
    // Rămâne logat doar până la reîncărcare.
  }
}

export function uita() {
  sesiune.token = null
  sesiune.nume = null
  sesiune.rol = null
  try {
    localStorage.removeItem(CHEIE)
  } catch {
    // Nimic de șters.
  }
}

export function esteAdmin() {
  return sesiune.rol === 'ADMIN'
}

let cerereEu = null

/** După o reîncărcare știm doar token-ul: numele și rolul le cere o singură dată, chiar dacă întreabă mai mulți. */
export async function asiguraUtilizatorul() {
  if (!sesiune.token || sesiune.rol) return
  cerereEu ??= api
    .eu()
    .then(({ nume, rol }) => {
      sesiune.nume = nume
      sesiune.rol = rol
    })
    .finally(() => {
      cerereEu = null
    })
  return cerereEu
}
