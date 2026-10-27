import { reactive } from 'vue'
import { api } from './api.js'
import { asculta } from './live.js'

/**
 * Cifra din meniu și din antetul paginii Alarme (redesign §5). Vine de la server, fiindcă lista e tăiată la 200;
 * între două recalculări, fiecare ALARMA live o crește cu 1.
 */
export const numarAlarme = reactive({ nevazute: 0, azi: 0 })

/** Cu serverul oprit sau cu token-ul expirat, cifra rămâne ultima știută; la 401, api.js duce deja la login. */
export async function recalculeaza(cere = api.numarAlarme) {
  try {
    const { nevazute, azi } = await cere()
    numarAlarme.nevazute = nevazute
    numarAlarme.azi = azi
  } catch (e) {
    console.warn('nu pot număra alarmele:', e.message)
  }
}

let interval = null
let oprireAscultare = null

/** Recalcularea la 30 s prinde ce au marcat alți dispeceri; nu există mesaj live pentru asta. */
export function pornesteNumararea() {
  opresteNumararea()
  recalculeaza()
  interval = setInterval(() => recalculeaza(), 30_000)
  oprireAscultare = asculta((tip) => {
    if (tip === 'ALARMA') {
      numarAlarme.nevazute++
      numarAlarme.azi++
    }
  })
}

export function opresteNumararea() {
  clearInterval(interval)
  interval = null
  oprireAscultare?.()
  oprireAscultare = null
}
