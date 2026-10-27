import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  numeMasina, numeTinta, numeRol, sursaAlarmei, propozitieAlarma, numeTipAlarma, virgula, numar,
  stariVreme, textCifra, timpScurs, grupaTimp, dataCreare, dataScurta,
} from './text.js'

// Datele se fac cu new Date(an, luna, zi, ...), în ora locală, ca testele să nu depindă de fusul calculatorului.

test('numeMasina', () => {
  assert.equal(numeMasina('masina-7'), 'Mașina 7')
  assert.equal(numeMasina('masina-10'), 'Mașina 10')
  assert.equal(numeMasina('camion-x'), 'camion-x')            // Review Focus 4
})
test('numeTinta', () => { assert.equal(numeTinta('TOATE'), 'Toată flota'); assert.equal(numeTinta('masina-3'), 'Mașina 3') })
test('numeRol', () => { assert.equal(numeRol('ADMIN'), 'Administrator'); assert.equal(numeRol('DISPECER'), 'Dispecer'); assert.equal(numeRol('ALTUL'), 'ALTUL') })
test('sursaAlarmei', () => { assert.equal(sursaAlarmei('MASINA'), 'de la mașină'); assert.equal(sursaAlarmei('SERVER'), 'de la server') })
test('propozitieAlarma', () => {
  const p = (tip, detalii = '') => propozitieAlarma({ masinaId: 'masina-6', tip, detalii })
  assert.equal(p('SUPRAINCALZIRE'), 'Mașina 6 s-a supraîncălzit')
  assert.equal(p('FRANARE_BRUSCA'), 'Mașina 6 a frânat brusc')
  assert.equal(p('VITEZA_PESTE_LIMITA'), 'Mașina 6 a depășit limita de viteză')
  assert.equal(p('IESIRE_DIN_ZONA', 'a ieșit din zona Centru'), 'Mașina 6 a ieșit din zona Centru')
  assert.equal(p('IESIRE_DIN_ZONA', ''), 'Mașina 6 a ieșit din zonă')
  assert.equal(p('OFFLINE_PRELUNGIT'), 'Mașina 6 nu mai trimite date')
  assert.equal(p('CEVA_NOU'), 'Mașina 6: CEVA_NOU')         // Review Focus 4
})
test('numeTipAlarma', () => { assert.equal(numeTipAlarma('OFFLINE_PRELUNGIT'), 'Fără semnal'); assert.equal(numeTipAlarma('CEVA_NOU'), 'CEVA_NOU') })
test('virgula', () => {
  assert.equal(virgula('53.7 km/h; limita zonei Centru e 50 km/h'), '53,7 km/h; limita zonei Centru e 50 km/h')
  assert.equal(virgula('temperatura a ajuns la 110.8 °C'), 'temperatura a ajuns la 110,8 °C')
})
test('numar', () => { assert.equal(numar(92.44, 1), '92,4'); assert.equal(numar(9.8, 1), '9,8'); assert.equal(numar(58.4, 0), '58') })
test('stariVreme', () => {
  assert.deepEqual(stariVreme('NORMAL'), [{ text: 'Normală', ton: 'verde' }])
  assert.deepEqual(stariVreme('TUNEL 12'), [{ text: 'Tunel · încă 12 s', ton: 'rosu' }])
  assert.deepEqual(stariVreme('GAURA 4'), [{ text: 'Gaură · încă 4 s', ton: 'bleumarin' }])
  assert.deepEqual(stariVreme('INTARZIERE 500 200'), [{ text: 'Întârziere 500 ± 200 ms', ton: 'ambra' }])
  assert.deepEqual(stariVreme('INTARZIERE 500 0'), [{ text: 'Întârziere 500 ms', ton: 'ambra' }])
  assert.deepEqual(stariVreme('INTARZIERE 500 200 BANDA 64'),
    [{ text: 'Întârziere 500 ± 200 ms', ton: 'ambra' }, { text: 'Bandă 64 kbps', ton: 'ambra' }])
  assert.deepEqual(stariVreme('CEVA 5'), [{ text: 'CEVA 5', ton: 'gri' }])   // Review Focus 4
  assert.deepEqual(stariVreme(null), [])
})
test('textCifra', () => { assert.equal(textCifra(0), ''); assert.equal(textCifra(12), '12'); assert.equal(textCifra(100), '99+') })
test('timpScurs', () => {
  const acum = new Date(2026, 9, 27, 13, 0, 0).getTime()
  assert.equal(timpScurs(new Date(acum - 12_000).toISOString(), acum), 'acum 12 s')
  assert.equal(timpScurs(new Date(acum - 3 * 60_000).toISOString(), acum), 'acum 3 min')
  assert.equal(timpScurs(new Date(acum - 2 * 3_600_000).toISOString(), acum), 'acum 2 h')
})
test('grupaTimp', () => {
  const acum = new Date(2026, 9, 27, 13, 0, 0).getTime()
  assert.equal(grupaTimp(new Date(2026, 9, 27, 12, 57).toISOString(), acum), 'Ultimele 5 minute')
  assert.equal(grupaTimp(new Date(2026, 9, 27, 0, 30).toISOString(), acum), 'Mai devreme azi')
  assert.equal(grupaTimp(new Date(2026, 9, 26, 23, 30).toISOString(), acum), 'Ieri')
  assert.equal(grupaTimp(new Date(2026, 9, 20, 10, 0).toISOString(), acum), '20 oct. 2026')
})
test('dataCreare', () => { assert.equal(dataCreare(new Date(2026, 9, 26, 13, 57).toISOString()), 'creat pe 26 oct. 2026, 13:57') })
test('dataScurta', () => { assert.equal(dataScurta(new Date(2026, 9, 26, 13, 57).toISOString()), '26 oct. 2026') })
