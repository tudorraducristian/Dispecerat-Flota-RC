// Cele trei baze ale flotei (D54). Un singur loc: harta le desenează de aici, iar unelte/trasee.mjs pornește
// turele tot de aici. Fiecare tură începe și se termină în baza ei, unde mașina oprește la „Întoarce la bază”.

/** loc e [lat, lon]; masini spune ale cui e baza, pentru eticheta de pe hartă. */
export const BAZE = [
  { id: 'manastur', nume: 'Mănăștur', loc: [46.7555, 23.548], masini: '1–3' },
  { id: 'marasti', nume: 'Mărăști', loc: [46.7785, 23.617], masini: '4–6' },
  { id: 'buna_ziua', nume: 'Bună Ziua', loc: [46.748, 23.592], masini: '7–10' },
]

/** Casa din Lucide, albă, pe pătratul roșu al bazei. */
export const SVG_BAZA = '<svg viewBox="0 0 24 24" fill="none" stroke="#FFFFFF" stroke-width="2.2" stroke-linecap="round"'
  + ' stroke-linejoin="round" aria-hidden="true">'
  + '<path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"/>'
  + '<path d="M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>'
  + '</svg>'
