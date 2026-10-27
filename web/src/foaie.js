// Foaia care urcă de jos pe Harta de telefon (redesign §10.4): trei înălțimi și regulile dintre ele.
// Pure, ca să se poată testa fără browser.

const ORDINE = ['stransa', 'jumatate', 'intinsa']

/** Prag de viteză, în px/ms: peste el, degetul a aruncat foaia; sub el, doar a lăsat-o. */
const ARUNCARE = 0.5

/**
 * ecranPx e înălțimea de sub bară. Strânsă arată antetul listei și primele rânduri; pe jumătate lasă harta
 * vizibilă deasupra; întinsă urcă aproape până sub bară.
 */
export function inaltimi(ecranPx) {
  return { stransa: 132, jumatate: Math.round(ecranPx * 0.48), intinsa: ecranPx - 24 }
}

/** O atingere pe mâner, fără tragere, duce la înălțimea următoare, iar din întinsă înapoi la strânsă. */
export function urmatoarea(stare) {
  return ORDINE[(ORDINE.indexOf(stare) + 1) % ORDINE.length]
}

/** Unde ajunge foaia lăsată la inaltime px, cu viteza degetului în px/ms, pozitivă în sus. */
export function dupaTragere(inaltime, viteza, h) {
  if (viteza > ARUNCARE) return ORDINE.find((s) => h[s] > inaltime) ?? 'intinsa'
  if (viteza < -ARUNCARE) return [...ORDINE].reverse().find((s) => h[s] < inaltime) ?? 'stransa'
  return ORDINE.reduce((a, b) => (Math.abs(h[b] - inaltime) < Math.abs(h[a] - inaltime) ? b : a))
}
