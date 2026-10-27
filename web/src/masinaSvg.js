/** Culorile stărilor, ca valori: Leaflet desenează în afara CSS-ului paginii, deci nu poate citi variabile. */
export const CULORI_STARE = { ONLINE: '#16A34A', VREME: '#EA580C', OFFLINE: '#94A3B8' }

/** Albastrul primar, pentru zonele și traseul desenate de Leaflet; același cu --primar. */
export const ALBASTRU_HARTA = '#2563EB'

/**
 * Mașina văzută de sus (redesign §4.5, varianta 1, din masini-pe-harta.html): caroseria în culoarea stării,
 * parbrizul în față, luneta în spate. Fața e în sus, deci la 0° mașina merge spre nord.
 */
export function svgMasina(culoare, contur = '#FFFFFF') {
  const geamuri = culoare === CULORI_STARE.OFFLINE ? ['.4', '.3'] : ['.55', '.4']
  return `<svg viewBox="0 0 24 40" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">`
    + `<rect x="3" y="2" width="18" height="36" rx="7" fill="${culoare}" stroke="${contur}" stroke-width="2"/>`
    + `<path d="M6.5 13 Q12 9.5 17.5 13 L16.5 18 H7.5 Z" fill="#0F172A" opacity="${geamuri[0]}"/>`
    + `<path d="M7.5 29 H16.5 L17.5 33 Q12 35.5 6.5 33 Z" fill="#0F172A" opacity="${geamuri[1]}"/>`
    + `</svg>`
}
