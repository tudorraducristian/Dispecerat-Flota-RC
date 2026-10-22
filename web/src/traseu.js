/**
 * Împarte traseul unei mașini în bucăți de desenat (spec §7):
 * - „live”: mesaje venite pe loc, desenate cu linie plină;
 * - „recuperat”: mesaje cu dinBuffer, venite după o întrerupere, desenate cu linie punctată;
 * - între două poziții cu numere de ordine care nu se urmează nu se trage nimic: acolo lipsesc mesaje,
 *   sau simulatorul a repornit.
 * O bucată leagă două poziții vecine, iar felul ei îl dă a doua poziție.
 */
export function segmente(pozitii) {
  const rezultat = []
  let curent = null
  for (let i = 1; i < pozitii.length; i++) {
    const a = pozitii[i - 1]
    const b = pozitii[i]
    if (b.seq !== a.seq + 1) {
      curent = null // gol
      continue
    }
    const fel = b.dinBuffer ? 'recuperat' : 'live'
    if (!curent || curent.fel !== fel) {
      curent = { fel, puncte: [[a.lat, a.lon]] }
      rezultat.push(curent)
    }
    curent.puncte.push([b.lat, b.lon])
  }
  return rezultat
}
