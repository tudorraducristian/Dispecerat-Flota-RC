/** Toate cererile REST trec pe aici. Vite le trimite mai departe la serverul de pe 8080. */
async function cere(cale, optiuni = {}) {
  const raspuns = await fetch(cale, {
    ...optiuni,
    headers: { 'Content-Type': 'application/json', ...(optiuni.headers ?? {}) },
  })
  const corp = raspuns.status === 204 ? null : await raspuns.json().catch(() => null)
  if (!raspuns.ok) {
    throw new Error(corp?.mesaj ?? `serverul a răspuns ${raspuns.status}`)
  }
  return corp
}

export const api = {
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
  alarme: (vazute) => cere('/api/alarme' + (vazute == null ? '' : `?vazute=${vazute}`)),
  marcheazaVazuta: (id) => cere(`/api/alarme/${id}`, { method: 'PATCH' }),
}
