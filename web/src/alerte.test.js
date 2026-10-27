import { test } from 'node:test'
import assert from 'node:assert/strict'
import { inStiva } from './alerte.js'

// Review Focus 1: avalanșa de alarme, de exemplu patru în trei secunde sau zeci după o reconectare.

test('cel mult 3, cea mai nouă prima', () => {
  let s = []
  for (const id of [1, 2, 3, 4]) s = inStiva(s, { id })
  assert.deepEqual(s.map((a) => a.id), [4, 3, 2])
})
test('pe telefon, cu max 1, rămâne doar cea mai nouă', () => {
  let s = []
  for (const id of [1, 2, 3]) s = inStiva(s, { id }, 1)
  assert.deepEqual(s.map((a) => a.id), [3])
})
test('aceeași alarmă nu intră de două ori', () => {
  const s = inStiva(inStiva([], { id: 7 }), { id: 7 })
  assert.equal(s.length, 1)
})
test('nu schimbă lista primită', () => {
  const s = [{ id: 1 }]
  inStiva(s, { id: 2 })
  assert.equal(s.length, 1)
})
