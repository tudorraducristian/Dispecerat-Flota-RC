/**
 * Stiva de alerte de sub bară (redesign §4.4). Capturile arată patru alarme în trei secunde, iar după o
 * reconectare pot veni zeci: rămân doar cele mai noi, una sub alta, ca pagina să se poată folosi în continuare.
 * O alarmă cu același id, venită din nou, nu se mai pune.
 */
export function inStiva(lista, noua, max = 3) {
  if (lista.some((a) => a.id === noua.id)) return lista
  return [noua, ...lista].slice(0, max)
}
