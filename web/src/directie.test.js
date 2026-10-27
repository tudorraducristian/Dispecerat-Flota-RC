import { test } from 'node:test'
import assert from 'node:assert/strict'
import { directie } from './directie.js'

// Review Focus 2: mașina care stă pe loc nu se rotește aiurea.

const C = { lat: 46.77, lon: 23.59 }
test('nord, est, sud, vest', () => {
  assert.ok(Math.abs(directie(C, { lat: 46.78, lon: 23.59 }) - 0) < 0.5)
  assert.ok(Math.abs(directie(C, { lat: 46.77, lon: 23.60 }) - 90) < 0.5)
  assert.ok(Math.abs(directie(C, { lat: 46.76, lon: 23.59 }) - 180) < 0.5)
  assert.ok(Math.abs(directie(C, { lat: 46.77, lon: 23.58 }) - 270) < 0.5)
})
test('pe loc sau sub 1 m nu are direcție', () => {
  assert.equal(directie(C, { ...C }), null)
  assert.equal(directie(C, { lat: 46.770004, lon: 23.59 }), null)   // ~0,45 m
})
