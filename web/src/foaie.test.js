import { test } from 'node:test'
import assert from 'node:assert/strict'
import { inaltimi, urmatoarea, dupaTragere } from './foaie.js'

const H = { stransa: 132, jumatate: 300, intinsa: 600 }
test('inaltimi', () => {
  assert.deepEqual(inaltimi(548), { stransa: 132, jumatate: 263, intinsa: 524 })
})
test('urmatoarea, la atingerea mânerului', () => {
  assert.equal(urmatoarea('stransa'), 'jumatate')
  assert.equal(urmatoarea('jumatate'), 'intinsa')
  assert.equal(urmatoarea('intinsa'), 'stransa')
})
test('lăsată încet, foaia merge la cea mai apropiată înălțime', () => {
  assert.equal(dupaTragere(310, 0, H), 'jumatate')
  assert.equal(dupaTragere(200, 0, H), 'stransa')
  assert.equal(dupaTragere(460, 0, H), 'intinsa')
  assert.equal(dupaTragere(310, 0.3, H), 'jumatate')    // degetul care tremură nu e aruncare
})
test('aruncată în sus sau în jos, merge la înălțimea următoare în sensul acela', () => {
  assert.equal(dupaTragere(150, 1.2, H), 'jumatate')
  assert.equal(dupaTragere(310, 1.2, H), 'intinsa')
  assert.equal(dupaTragere(590, 1.2, H), 'intinsa')
  assert.equal(dupaTragere(590, -1.2, H), 'jumatate')
  assert.equal(dupaTragere(290, -1.2, H), 'stransa')
  assert.equal(dupaTragere(140, -1.2, H), 'stransa')
})
