// Textele pentru oameni (redesign §4.1). Serverul și proxy-ul vorbesc în coduri, iar ecranul în română.
// Orice cod necunoscut se afișează brut: un tip nou de la server nu trebuie să lase o pagină goală.

const ROLURI = { ADMIN: 'Administrator', DISPECER: 'Dispecer' }
const SURSE = { MASINA: 'de la mașină', SERVER: 'de la server' }

/** ton e culoarea din token-uri (rosu, ambra, gri); iconita e numele componentei Lucide. */
export const TIPURI_ALARMA = {
  SUPRAINCALZIRE: { nume: 'Supraîncălzire', ton: 'rosu', iconita: 'Thermometer' },
  FRANARE_BRUSCA: { nume: 'Frânare bruscă', ton: 'rosu', iconita: 'OctagonAlert' },
  VITEZA_PESTE_LIMITA: { nume: 'Viteză peste limită', ton: 'ambra', iconita: 'Gauge' },
  IESIRE_DIN_ZONA: { nume: 'Ieșire din zonă', ton: 'ambra', iconita: 'MapPinOff' },
  OFFLINE_PRELUNGIT: { nume: 'Fără semnal', ton: 'gri', iconita: 'WifiOff' },
}

const FAPTE = {
  SUPRAINCALZIRE: 's-a supraîncălzit',
  FRANARE_BRUSCA: 'a frânat brusc',
  VITEZA_PESTE_LIMITA: 'a depășit limita de viteză',
  OFFLINE_PRELUNGIT: 'nu mai trimite date',
}

export function numeMasina(id) {
  const m = /^masina-(\d+)$/.exec(id ?? '')
  return m ? `Mașina ${m[1]}` : id
}

export function numeTinta(tinta) {
  return tinta === 'TOATE' ? 'Toată flota' : numeMasina(tinta)
}

export function numeRol(rol) {
  return ROLURI[rol] ?? rol
}

export function initiala(nume) {
  return (nume ?? '').charAt(0).toUpperCase() || '?'
}

export function sursaAlarmei(sursa) {
  return SURSE[sursa] ?? sursa
}

export function numeTipAlarma(tip) {
  return TIPURI_ALARMA[tip]?.nume ?? tip
}

export function propozitieAlarma({ masinaId, tip, detalii }) {
  const masina = numeMasina(masinaId)
  if (tip === 'IESIRE_DIN_ZONA') {
    // Serverul scrie „a ieșit din zona Centru”; numele zonei se ia de acolo, dacă se poate citi.
    const m = /^a ieșit din zona (.+)$/.exec(detalii ?? '')
    return m ? `${masina} a ieșit din zona ${m[1]}` : `${masina} a ieșit din zonă`
  }
  return FAPTE[tip] ? `${masina} ${FAPTE[tip]}` : `${masina}: ${tip}`
}

/** Serverul scrie detaliile cu punct zecimal; pe ecran, ca în ro-RO, cu virgulă. Doar punctul dintre două cifre. */
export function virgula(text) {
  return (text ?? '').replace(/(\d)\.(\d)/g, '$1,$2')
}

export function numar(valoare, zecimale) {
  return valoare.toLocaleString('ro-RO', { minimumFractionDigits: zecimale, maximumFractionDigits: zecimale })
}

// Câte numere ia fiecare cuvânt din răspunsul proxy-ului la STARE și ce scrie din ele.
const VREME = {
  NORMAL: { numere: 0, ton: 'verde', text: () => 'Normală' },
  TUNEL: { numere: 1, ton: 'rosu', text: ([s]) => `Tunel · încă ${s} s` },
  GAURA: { numere: 1, ton: 'bleumarin', text: ([s]) => `Gaură · încă ${s} s` },
  INTARZIERE: {
    numere: 2, ton: 'ambra',
    text: ([ms, variatie]) => (Number(variatie) === 0 ? `Întârziere ${ms} ms` : `Întârziere ${ms} ± ${variatie} ms`),
  },
  BANDA: { numere: 1, ton: 'ambra', text: ([kbps]) => `Bandă ${kbps} kbps` },
}

/** Etichetele stării vremii. La primul cuvânt pe care nu-l știe, restul textului rămâne brut, pe gri. */
export function stariVreme(descriere) {
  const cuvinte = (descriere ?? '').trim().split(/\s+/).filter(Boolean)
  const etichete = []
  let i = 0
  while (i < cuvinte.length) {
    const fel = VREME[cuvinte[i]]
    const numere = fel ? cuvinte.slice(i + 1, i + 1 + fel.numere) : []
    if (!fel || numere.length < fel.numere || !numere.every((n) => /^\d+$/.test(n))) {
      etichete.push({ text: cuvinte.slice(i).join(' '), ton: 'gri' })
      break
    }
    etichete.push({ text: fel.text(numere), ton: fel.ton })
    i += 1 + fel.numere
  }
  return etichete
}

/** Cifra roșie din meniu: nimic la 0, iar peste 99 nu mai încape. */
export function textCifra(n) {
  if (!n) return ''
  return n > 99 ? '99+' : String(n)
}

export function ora(iso) {
  return new Date(iso).toLocaleString('ro-RO', { hour: '2-digit', minute: '2-digit' })
}

export function timpScurs(iso, acumMs) {
  // Ceasul mașinii poate fi puțin înaintea celui al laptopului; „acum -2 s” n-ar însemna nimic.
  const secunde = Math.max(0, Math.round((acumMs - new Date(iso).getTime()) / 1000))
  if (secunde < 60) return `acum ${secunde} s`
  const minute = Math.floor(secunde / 60)
  if (minute < 60) return `acum ${minute} min`
  return `acum ${Math.floor(minute / 60)} h`
}

function aceeasiZi(a, b) {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
}

/** Grupele din pagina Alarme, după ziua calendaristică a laptopului, nu după 24 de ore. */
export function grupaTimp(iso, acumMs) {
  const data = new Date(iso)
  const acum = new Date(acumMs)
  if (acumMs - data.getTime() < 5 * 60_000) return 'Ultimele 5 minute'
  if (aceeasiZi(data, acum)) return 'Mai devreme azi'
  const ieri = new Date(acum.getFullYear(), acum.getMonth(), acum.getDate() - 1)
  if (aceeasiZi(data, ieri)) return 'Ieri'
  return dataScurta(iso)
}

export function dataScurta(iso) {
  return new Date(iso).toLocaleString('ro-RO', { day: 'numeric', month: 'short', year: 'numeric' })
}

export function dataCreare(iso) {
  return `creat pe ${dataScurta(iso)}, ${ora(iso)}`
}
