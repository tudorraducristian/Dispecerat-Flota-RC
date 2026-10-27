const R = 6_371_000 // raza Pământului, în metri
const rad = (g) => (g * Math.PI) / 180

/** Distanța pe sferă, în metri (haversine). */
function distanta(a, b) {
  const dLat = rad(b.lat - a.lat)
  const dLon = rad(b.lon - a.lon)
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLon / 2) ** 2
  return 2 * R * Math.asin(Math.sqrt(h))
}

/**
 * Încotro merge mașina, în grade de la nord, în sensul acelor de ceasornic (azimutul inițial pe sferă).
 * Sub 1 m nu are direcție: GPS-ul simulat al unei mașini oprite ar roti-o aiurea pe hartă.
 */
export function directie(a, b) {
  if (distanta(a, b) < 1) return null
  const f1 = rad(a.lat)
  const f2 = rad(b.lat)
  const dLon = rad(b.lon - a.lon)
  const y = Math.sin(dLon) * Math.cos(f2)
  const x = Math.cos(f1) * Math.sin(f2) - Math.sin(f1) * Math.cos(f2) * Math.cos(dLon)
  return ((Math.atan2(y, x) * 180) / Math.PI + 360) % 360
}
