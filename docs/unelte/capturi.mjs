// Capturile din aplicație pentru articol și manual (faza 6, Task 10 și 13), cu Edge fără fereastră, prin
// protocolul DevTools. Fără pachete npm: Node 24 are fetch și WebSocket.
//
//   node docs\unelte\capturi.mjs <lista.json> <dosar-iesire>
//
// lista.json: [{ "fisier": "harta.png", "ruta": "/?masina=masina-3", "cont": "admin" | "dispecer" | null,
//                "asteapta": 6000, "latime": 1600, "inaltime": 1000 }]
// Token-urile vin din FLOTA_TOKEN_ADMIN și FLOTA_TOKEN_DISPECER, nu din fișier, ca să nu ajungă în git.
// Adresa UI-ului: FLOTA_UI, implicit http://127.0.0.1:5173.
import { spawn } from 'node:child_process'
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'

const [lista, dosar] = process.argv.slice(2)
if (!lista || !dosar) {
  console.error('folosire: node capturi.mjs <lista.json> <dosar-iesire>')
  process.exit(2)
}
const UI = process.env.FLOTA_UI ?? 'http://127.0.0.1:5173'
const tokenuri = { admin: process.env.FLOTA_TOKEN_ADMIN, dispecer: process.env.FLOTA_TOKEN_DISPECER }
const capturi = JSON.parse(readFileSync(lista, 'utf8'))
mkdirSync(dosar, { recursive: true })

const EDGE = [
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
].find(existsSync)
if (!EDGE) throw new Error('Edge nu e instalat')

const dormi = (ms) => new Promise((r) => setTimeout(r, ms))
const PORT = 9333
const profil = mkdtempSync(join(tmpdir(), 'capturi-edge-'))
const edge = spawn(EDGE, ['--headless=new', '--disable-gpu', '--hide-scrollbars', `--remote-debugging-port=${PORT}`,
  `--user-data-dir=${profil}`, '--window-size=1600,1000', 'about:blank'], { stdio: 'ignore' })

/** O legătură DevTools cu o pagină: trimite comenzi și așteaptă răspunsul cu același id. */
class Pagina {
  constructor(ws) {
    this.ws = ws
    this.urmatorul = 1
    this.asteptari = new Map()
    ws.addEventListener('message', (e) => {
      const m = JSON.parse(e.data)
      const a = m.id && this.asteptari.get(m.id)
      if (!a) return
      this.asteptari.delete(m.id)
      m.error ? a.respinge(new Error(`${m.error.message}`)) : a.rezolva(m.result)
    })
  }
  trimite(metoda, parametri = {}) {
    const id = this.urmatorul++
    this.ws.send(JSON.stringify({ id, method: metoda, params: parametri }))
    return new Promise((rezolva, respinge) => this.asteptari.set(id, { rezolva, respinge }))
  }
  async evalueaza(cod) {
    const r = await this.trimite('Runtime.evaluate', { expression: cod, awaitPromise: true, returnByValue: true })
    return r.result?.value
  }
}

async function deschide() {
  for (let i = 0; i < 50; i++) {
    try {
      const tinte = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json()
      const t = tinte.find((x) => x.type === 'page')
      if (t) {
        const ws = new WebSocket(t.webSocketDebuggerUrl)
        await new Promise((r, j) => { ws.onopen = r; ws.onerror = j })
        return new Pagina(ws)
      }
    } catch {
      // Edge încă pornește.
    }
    await dormi(200)
  }
  throw new Error('Edge nu răspunde pe portul DevTools')
}

async function mergiLa(pagina, url) {
  await pagina.trimite('Page.navigate', { url })
  for (let i = 0; i < 100; i++) {
    if ((await pagina.evalueaza('document.readyState')) === 'complete') return
    await dormi(100)
  }
}

try {
  const pagina = await deschide()
  await pagina.trimite('Page.enable')
  await pagina.trimite('Runtime.enable')
  // Fără ele, Edge fără fereastră poate socoti pagina ascunsă și nu mai desenează nimic.
  await pagina.trimite('Page.bringToFront')
  await pagina.trimite('Emulation.setFocusEmulationEnabled', { enabled: true })
  for (const c of capturi) {
    const latime = c.latime ?? 1600
    const inaltime = c.inaltime ?? 1000
    await pagina.trimite('Emulation.setDeviceMetricsOverride', { width: latime, height: inaltime, deviceScaleFactor: 1, mobile: false })
    // Token-ul se pune în localStorage-ul originii UI-ului, înainte de pagina cerută.
    await mergiLa(pagina, `${UI}/login`)
    const token = c.cont ? tokenuri[c.cont] : null
    if (c.cont && !token) throw new Error(`lipsește token-ul pentru contul ${c.cont}`)
    await pagina.evalueaza(token
      ? `localStorage.setItem('flota.token', ${JSON.stringify(token)})`
      : `localStorage.removeItem('flota.token')`)
    await mergiLa(pagina, 'about:blank')
    await mergiLa(pagina, UI + c.ruta)
    await dormi(c.asteapta ?? 3000)
    // "ascunde": un selector CSS ascuns înainte de captură, de exemplu alertele care acoperă pagina.
    if (c.ascunde) {
      await pagina.evalueaza(`(() => { const s = document.createElement('style'); s.textContent = ${JSON.stringify(c.ascunde + '{visibility:hidden !important}')}; document.head.appendChild(s) })()`)
      await dormi(200)
    }
    const unde = await pagina.evalueaza('location.href + " · " + document.visibilityState + " · " + document.body.innerText.length + " caractere"')
    console.log(`${c.fisier}: ${unde}`)
    const { data } = await pagina.trimite('Page.captureScreenshot', { format: 'png' })
    const cale = resolve(dosar, c.fisier)
    writeFileSync(cale, Buffer.from(data, 'base64'))
    console.log(`scris: ${cale}`)
  }
  // Pagina rămâne fără token, ca profilul temporar să nu țină nimic.
  await mergiLa(pagina, `${UI}/login`)
  await pagina.evalueaza(`localStorage.removeItem('flota.token')`)
} finally {
  edge.kill()
  await dormi(500)
  rmSync(profil, { recursive: true, force: true, maxRetries: 5, retryDelay: 200 })
}
