import { test } from 'node:test'
import assert from 'node:assert/strict'
import { numarAlarme, recalculeaza } from './numarAlarme.js'

test('recalculeaza pune numerele primite', async () => {
  await recalculeaza(async () => ({ nevazute: 12, azi: 148 }))
  assert.deepEqual({ ...numarAlarme }, { nevazute: 12, azi: 148 })
})
test('o eroare lasă ultimele numere știute și nu aruncă', async (t) => {   // Review Focus 5
  t.mock.method(console, 'warn', () => {})
  await recalculeaza(async () => ({ nevazute: 5, azi: 9 }))
  await recalculeaza(async () => { throw new Error('serverul a răspuns 503') })
  assert.deepEqual({ ...numarAlarme }, { nevazute: 5, azi: 9 })
})
