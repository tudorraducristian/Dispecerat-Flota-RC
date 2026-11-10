// PowerPoint-ul proiectului (faza 6, Task 12; spec §16.6), cu pptxgenjs, din articol și scenariul prezentării.
//   cd docs\unelte; npm install; node prezentare.cjs
// Scrie docs\prezentare\flota-rc.pptx. PNG-urile și PDF-ul le face pptx-la-png.ps1, prin PowerPoint.
// Tema o scrie apply_theme.js din skill-ul pptx, dacă e găsit; fără el, culorile rămân cele scrise ca hex.
const path = require('path')
const fs = require('fs')
const pptxgen = require('pptxgenjs')

const RADACINA = path.resolve(__dirname, '..', '..')
const FIGURI = path.join(RADACINA, 'docs', 'articol', 'figuri')
const IESIRE = path.join(RADACINA, 'docs', 'prezentare', 'flota-rc.pptx')
const AUTOR = process.env.FLOTA_AUTOR || '<de completat: numele>'

// Asfaltul și semnul de drum: fundal închis, chihlimbar pentru ce se strică, verde pentru „online”.
const THEME = {
  name: 'Flota RC',
  headFontFace: 'Cambria',
  bodyFontFace: 'Calibri',
  colors: {
    dk1: '111827', lt1: 'FFFFFF', dk2: '1F2A37', lt2: 'F3F4F6',
    accent1: 'F59E0B', accent2: '16A34A', accent3: '6B7280', accent4: 'DC2626', accent5: '2563EB', accent6: '0EA5E9',
    hlink: '2563EB', folHlink: '7C3AED',
  },
}
const HEX = THEME.colors

const pres = new pptxgen()
pres.layout = 'LAYOUT_WIDE' // 13,33" × 7,5"
pres.author = AUTOR
pres.title = 'Dispecerat de flotă pe rețele nesigure'
pres.theme = { headFontFace: THEME.headFontFace, bodyFontFace: THEME.bodyFontFace }
const C = pres.SchemeColor
const L = 13.333
const M = 0.6 // marginea

pres.defineSlideMaster({
  title: 'Titlu',
  background: { color: HEX.dk1 },
  objects: [
    { placeholder: { options: { name: 'title', type: 'title', x: M, y: 2.2, w: L - 2 * M, h: 1.6, fontSize: 44, bold: true, color: C.background1, fontFace: THEME.headFontFace, valign: 'bottom' }, text: '' } },
    { placeholder: { options: { name: 'body', type: 'body', x: M, y: 3.95, w: L - 2 * M, h: 1.2, fontSize: 24, color: 'D1D5DB', valign: 'top' }, text: '' } },
  ],
})
pres.defineSlideMaster({
  title: 'Continut',
  background: { color: HEX.lt1 },
  margin: [0.5, M, 0.5, M],
  objects: [
    { placeholder: { options: { name: 'title', type: 'title', x: M, y: 0.4, w: L - 2 * M, h: 0.9, fontSize: 36, bold: true, color: C.text1, fontFace: THEME.headFontFace, valign: 'middle' }, text: '' } },
  ],
  slideNumber: { x: L - 1.1, y: 6.95, w: 0.6, h: 0.35, fontSize: 12, color: '6B7280', align: 'right' },
})
pres.defineSlideMaster({
  title: 'Intunecat',
  background: { color: HEX.dk1 },
  objects: [
    { placeholder: { options: { name: 'title', type: 'title', x: M, y: 0.4, w: L - 2 * M, h: 0.9, fontSize: 36, bold: true, color: C.background1, fontFace: THEME.headFontFace, valign: 'middle' }, text: '' } },
  ],
  slideNumber: { x: L - 1.1, y: 6.95, w: 0.6, h: 0.35, fontSize: 12, color: '9CA3AF', align: 'right' },
})

function poza(slide, fisier, x, y, wMax, hMax, nume) {
  // Figura cât mai mare în cutia dată, cu raportul ei, centrată.
  const { w: lp, h: ip } = dimensiuniPng(path.join(FIGURI, fisier))
  const scara = Math.min(wMax / lp, hMax / ip)
  const w = lp * scara
  const h = ip * scara
  slide.addImage({ path: path.join(FIGURI, fisier), x: x + (wMax - w) / 2, y: y + (hMax - h) / 2, w, h, objectName: nume, altText: nume })
}

function dimensiuniPng(cale) {
  const b = fs.readFileSync(cale)
  return { w: b.readUInt32BE(16), h: b.readUInt32BE(20) }
}

function cerc(slide, x, y, d, culoare, text, nume, culoareText = C.background1) {
  slide.addShape(pres.shapes.OVAL, { x, y, w: d, h: d, fill: { color: culoare }, line: { color: culoare }, objectName: nume })
  slide.addText(text, { x, y, w: d, h: d, fontSize: 20, bold: true, color: culoareText, align: 'center', valign: 'middle', margin: 0, isTextBox: true, objectName: nume + ' text' })
}

function card(slide, x, y, w, h, nume, umplere = C.background2) {
  slide.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y, w, h, rectRadius: 0.12, fill: { color: umplere }, line: { color: umplere }, objectName: nume })
}

// Cifra și unitatea nu se despart pe două rânduri: spațiu care nu se rupe (U+00A0).
const nb = (t) => t.replace(/(\d) (s|ms)\b/g,'$1 $2').replace(/ ± /g, ' ± ')

// Un rând: cercul cu semnul, titlul gros și o propoziție dedesubt.
function rand(slide, x, y, w, semn, culoare, titlu, text, nume, culoareText = C.text1) {
  titlu = nb(titlu)
  text = nb(text)
  cerc(slide, x, y + 0.05, 0.62, culoare, semn, nume + ' semn')
  slide.addText([
    { text: titlu, options: { bold: true, fontSize: 22, color: culoareText, breakLine: true } },
    { text, options: { fontSize: 20, color: culoareText === C.text1 ? '4B5563' : 'D1D5DB' } },
  ], { x: x + 0.85, y, w: w - 0.85, h: 1.05, valign: 'top', margin: 0, paraSpaceAfter: 2, isTextBox: true, objectName: nume })
}

// ---------- 1. Titlul ----------
pres.addSection({ title: 'Început' })
let s = pres.addSlide({ masterName: 'Titlu', sectionTitle: 'Început' })
s.addText('Dispecerat de flotă pe rețele nesigure', { placeholder: 'title' })
s.addText([
  { text: 'MQTT, un proxy care strică rețeaua la comandă și ce se pierde când mașina intră în tunel', options: { breakLine: true } },
  { text: `${AUTOR} · Rețele de Calculatoare · UTCN, 2026`, options: { fontSize: 20, color: '9CA3AF' } },
], { placeholder: 'body' })
for (let i = 0; i < 10; i++) {
  const online = i !== 2
  s.addShape(pres.shapes.OVAL, { x: M + i * 0.42, y: 1.45, w: 0.24, h: 0.24, fill: { color: online ? C.accent2 : C.accent3 }, line: { color: online ? C.accent2 : C.accent3 }, objectName: `masina ${i + 1}` })
}
s.addNotes('Mă numesc ... Proiectul meu de Rețele: un dispecerat pentru zece mașini simulate, care raportează prin MQTT, și un proxy care strică rețeaua dintre ele și broker. În cinci minute vă arăt ce se pierde, ce se întârzie și ce ajunge de două ori când o mașină pierde semnalul.')

// ---------- 2. Problema ----------
pres.addSection({ title: 'Problema și soluția' })
s = pres.addSlide({ masterName: 'Continut', sectionTitle: 'Problema și soluția' })
s.addText('O flotă care pierde semnalul', { placeholder: 'title' })
rand(s, M, 1.6, 5.6, '?', C.accent4, 'Pierdut?', 'mesajul nu mai ajunge niciodată', 'problema pierdut')
rand(s, M, 2.85, 5.6, '?', C.accent1, 'Întârziat?', 'ajunge, dar după ieșirea din tunel', 'problema intarziat')
rand(s, M, 4.1, 5.6, '?', C.accent5, 'Dublat?', 'ajunge de două ori și se numără greșit', 'problema dublat')
s.addText('Mașina nu știe de tunel: vede doar că i-a murit conexiunea.', { x: M, y: 5.45, w: 5.6, h: 0.9, fontSize: 20, italic: true, color: '374151', margin: 0, isTextBox: true, objectName: 'ideea' })
poza(s, 'harta.png', 6.6, 1.5, L - 6.6 - M, 5.2, 'harta flotei')
s.addNotes('Zece mașini simulate trimit telemetria prin MQTT, o dată pe secundă. Când una intră într-un tunel, întrebarea e ce se întâmplă cu mesajele ei: se pierd, se întârzie sau ajung de două ori? Pe hartă, fiecare mașină are tura ei pe străzile Clujului.')

// ---------- 3. Arhitectura ----------
s = pres.addSlide({ masterName: 'Continut', sectionTitle: 'Problema și soluția' })
s.addText('Arhitectura: cinci piese pe un laptop', { placeholder: 'title' })
poza(s, 'arhitectura.png', M, 1.4, 5.3, 5.4, 'diagrama bloc')
rand(s, 6.4, 1.7, 6.3, '1', C.accent2, 'Datele', 'mașina → proxy → Mosquitto → server → MySQL și UI', 'drum date')
rand(s, 6.4, 3.05, 6.3, '2', C.accent5, 'Comenzile', 'UI → server → Mosquitto → proxy → mașina', 'drum comenzi')
rand(s, 6.4, 4.4, 6.3, '3', C.accent1, 'Vremea', 'UI → server → proxy, pe portul 1885, în text', 'drum vreme')
s.addNotes('Simulatorul are zece mașini. Ele nu vorbesc direct cu broker-ul: trec prin proxy-ul meu, pe portul 1884. Serverul, Spring Boot cu MySQL, ascultă direct la Mosquitto, deci dispecerul nu stă în tunel. UI-ul primește datele live pe WebSocket. Ordinele de vreme merg separat, pe portul 1885.')

// ---------- 4. MQTT și QoS ----------
s = pres.addSlide({ masterName: 'Continut', sectionTitle: 'Problema și soluția' })
s.addText('MQTT și nivelurile de QoS', { placeholder: 'title' })
poza(s, 'conexiuni.png', M, 1.45, 6.4, 5.2, 'topic-urile MQTT')
const qos = [
  ['0', C.accent3, 'QoS 0 · cel mult o dată', 'telemetria: următoarea o înlocuiește'],
  ['1', C.accent5, 'QoS 1 · cel puțin o dată', 'starea, comenzile, confirmările'],
  ['2', C.accent4, 'QoS 2 · exact o dată', 'alarmele: rare și grave'],
]
qos.forEach(([n, cul, t, d], i) => {
  card(s, 7.35, 1.55 + i * 1.6, L - 7.35 - M, 1.4, `card qos ${n}`)
  rand(s, 7.55, 1.72 + i * 1.6, L - 7.55 - M - 0.15, n, cul, t, d, `qos ${n}`)
})
s.addNotes('Fiecare tip de mesaj are QoS-ul lui. Telemetria e pe QoS 0: vine o dată pe secundă, iar dacă se pierde una, următoarea o înlocuiește. Alarmele sînt pe QoS 2, exact o dată. Starea e reținută, cu Last Will: broker-ul publică singur „offline” când mașina dispare. Sesiunea e persistentă, deci broker-ul păstrează comenzile cât mașina e offline.')

// ---------- 5. Proxy-ul ----------
s = pres.addSlide({ masterName: 'Continut', sectionTitle: 'Problema și soluția' })
s.addText('Proxy-ul și „vremea”', { placeholder: 'title' })
const vreme = [
  ['Întârziere', 'fiecare bucată pleacă după ms ± v; ordinea rămâne cea din TCP', C.accent1],
  ['Bandă îngustă', 'scriitorul așteaptă cât ar trece octeții la viteza dată', C.accent6],
  ['Tunel', 'închide conexiunea pe loc; mașina află imediat', C.accent3],
  ['Gaură neagră', 'conexiunea rămâne deschisă, dar mută; mașina nu află', C.text1],
]
const cw = (L - 2 * M - 0.4) / 2
vreme.forEach(([t, d, cul], i) => {
  const x = M + (i % 2) * (cw + 0.4)
  const y = 1.5 + Math.floor(i / 2) * 2.05
  card(s, x, y, cw, 1.8, `card ${t}`)
  rand(s, x + 0.25, y + 0.3, cw - 0.4, String(i + 1), cul, t, d, `vreme ${t}`)
})
s.addText([
  { text: 'Ordinele, pe portul 1885: ', options: { color: '374151' } },
  { text: 'TUNEL masina-3 15', options: { fontFace: 'Courier New', bold: true, color: C.text1 } },
  { text: '  →  OK', options: { fontFace: 'Courier New', color: C.accent2 } },
], { x: M, y: 5.75, w: L - 2 * M, h: 0.7, fontSize: 22, margin: 0, isTextBox: true, objectName: 'ordinul' })
s.addNotes('Proxy-ul e ca un traducător la telefon: fiecare parte crede că vorbește direct cu cealaltă. El citește doar primul pachet, CONNECT, ca să afle ce mașină e, apoi dă octeții mai departe. La comandă, poate întârzia, poate tăia conexiunea, ca un tunel, sau o poate ține deschisă dar mută, ca o gaură neagră. Ordinele vin pe alt port, într-un protocol text, ca să nu treacă prin drumul pe care îl strică.')

// ---------- 6. Demonstrația ----------
pres.addSection({ title: 'Demonstrația' })
s = pres.addSlide({ masterName: 'Intunecat', sectionTitle: 'Demonstrația' })
s.addText('Demonstrație live', { placeholder: 'title' })
const pasi = ['Login ca dispecer1', 'Zece mașini live', 'Tunel, 15 s', 'Gaură neagră, 10 s', 'Comandă în tunel', 'STARE pe 1885', 'Întârziere 500 ms']
pasi.forEach((p, i) => {
  const col = i < 4 ? 0 : 1
  const r = i < 4 ? i : i - 4
  const x = M + col * 6.2
  const y = 1.65 + r * 1.2
  cerc(s, x, y, 0.75, i === 0 ? C.accent2 : C.accent1, String(i), `pas ${i}`, C.text1)
  s.addText(p, { x: x + 1.0, y, w: 5.0, h: 0.75, fontSize: 26, color: C.background1, valign: 'middle', margin: 0, isTextBox: true, objectName: `pas ${i} text` })
})
s.addNotes([
  '0. Login ca dispecer1, făcut dinainte în tabul 1.',
  '1. Zece mașini live: „Zece mașini simulate trimit telemetria prin MQTT, o dată pe secundă. Între ele și broker stă un proxy scris de mine, care strică rețeaua la comandă.”',
  '2. Tunelul: mașina nu știe de tunel, vede doar că i-a murit conexiunea; cât e offline, motorul pune mesajele în buffer, cu din_buffer; radioul reîncearcă după 1, 2, 4 secunde, cu o abatere la întâmplare; gri a venit din Last Will-ul publicat de broker.',
  '3. Gaura neagră: conexiunea e deschisă, dar mută. Keepalive-ul de 5 secunde o descoperă: broker-ul închide după 1,5 × 5 = 7,5 secunde fără niciun pachet, în practică 6–7 secunde; telemetria e pe QoS 0, deci ce era pe drum s-a pierdut. Alarmele sînt pe QoS 2 și ajung după reconectare, o singură dată; o gaură de 6 secunde nu pierde nimic, doar întârzie.',
  '4. Comanda în tunel: sesiunea persistentă (cleanSession false) face ca broker-ul să păstreze comanda; QoS 1 spune „livrat la broker”, iar confirmarea spune „executat”; aceeași confirmare primită de două ori se numără o dată.',
  '5. STARE: management out-of-band, ordinele merg pe portul 1885, nu pe drumul pe care îl strică; protocolul e text, deci se poate folosi de mână; rândul cu un singur punct spune unde se termină răspunsul, ca la SMTP.',
  '6. Latența: fiecare mesaj are ora mașinii și ora sosirii, iar latența e diferența dintre ele; aici ceasul e același, fiindcă totul rulează pe laptop, pe mașini reale ceasurile s-ar sincroniza prin NTP; variația nu schimbă ordinea octeților, fiindcă pe TCP ordinea e garantată.',
].join('\n'))

// ---------- 7. Rezultatele ----------
pres.addSection({ title: 'Rezultate și limite' })
s = pres.addSlide({ masterName: 'Continut', sectionTitle: 'Rezultate și limite' })
s.addText('Rezultatele măsurate', { placeholder: 'title' })
poza(s, 'tunel-gaura-ro.png', M, 1.35, 5.0, 5.55, 'graficul tunel și gaură')
const cifre = [
  ['0', 'pierdute în tunelul de 15 s', '19 venite din buffer', C.accent2],
  ['6,3', 'pierdute în gaura de 10 s', 'până o descoperă keepalive-ul, la ~6,6 s', C.accent4],
  ['0', 'pierdute în gaura de 4 s', 'doar întârziate, cu până la 4,1 s', C.accent1],
  ['532 ms', 'latența medie la 500 ± 200 ms', 'fără să se schimbe ordinea', C.accent5],
]
cifre.forEach(([n, t, d, cul], i) => {
  const x = 6.0 + (i % 2) * 3.45
  const y = 1.5 + Math.floor(i / 2) * 2.7
  card(s, x, y, 3.2, 2.45, `card cifra ${i}`)
  s.addText([
    { text: n, options: { fontSize: 48, bold: true, color: cul, fontFace: THEME.headFontFace, breakLine: true } },
    { text: nb(t), options: { fontSize: 20, bold: true, color: C.text1, breakLine: true } },
    { text: nb(d), options: { fontSize: 18, color: '4B5563' } },
  ], { x: x + 0.2, y: y + 0.15, w: 2.8, h: 2.2, valign: 'top', margin: 0, isTextBox: true, objectName: `cifra ${i}` })
})
s.addNotes('Am măsurat de câte trei ori. În tunel nu se pierde nimic: mașina află pe loc și pune tot în buffer, iar la ieșire trimite în medie 19 mesaje, în ordine. În gaura de 10 secunde se pierd vreo 6: atâtea a trimis mașina până când keepalive-ul a descoperit gaura. O gaură mai scurtă decât pragul keepalive-ului nu pierde nimic, doar întârzie. Concluzia: contează dacă mașina vede căderea, nu cât durează. Întârzierea de 500 ± 200 ms se vede întreagă în latență, 532 ms în medie, fără să schimbe ordinea.')

// ---------- 8. Login și securitatea ----------
s = pres.addSlide({ masterName: 'Continut', sectionTitle: 'Rezultate și limite' })
s.addText('Login-ul și limitele lui', { placeholder: 'title' })
card(s, M, 1.5, 5.85, 3.9, 'card login')
s.addText('Cum merge', { x: M + 0.3, y: 1.7, w: 5.3, h: 0.6, fontSize: 24, bold: true, color: C.text1, margin: 0, isTextBox: true, objectName: 'titlu login' })
s.addText([
  { text: 'parolele ca hash BCrypt', options: { bullet: true, breakLine: true } },
  { text: 'token de 32 de octeți aleatori, valabil 8 ore', options: { bullet: true, breakLine: true } },
  { text: 'roluri: ADMIN și DISPECER', options: { bullet: true, breakLine: true } },
  { text: 'Ieșire: token-ul se șterge, iar WebSocket-urile lui se închid', options: { bullet: true } },
], { x: M + 0.3, y: 2.4, w: 5.3, h: 2.8, fontSize: 20, color: '374151', paraSpaceAfter: 10, valign: 'top', margin: 0, isTextBox: true, objectName: 'lista login' })
card(s, 6.85, 1.5, L - 6.85 - M, 3.9, 'card limite', 'FEF3C7')
s.addText('Ce vede cine ascultă rețeaua', { x: 7.15, y: 1.7, w: 5.3, h: 0.6, fontSize: 24, bold: true, color: C.text1, margin: 0, isTextBox: true, objectName: 'titlu limite' })
s.addText([
  { text: 'HTTP necriptat: parola, la login, și token-ul, la fiecare cerere', options: { bullet: true, breakLine: true } },
  { text: 'token-ul stă și în adresa WebSocket-ului, deci poate ajunge în loguri', options: { bullet: true, breakLine: true } },
  { text: 'soluția: TLS, adică HTTPS și MQTT peste TLS', options: { bullet: true } },
], { x: 7.15, y: 2.4, w: 5.3, h: 2.8, fontSize: 20, color: '374151', paraSpaceAfter: 10, valign: 'top', margin: 0, isTextBox: true, objectName: 'lista limite' })
s.addNotes('Login-ul e cerut de grilă. Parola se ține ca hash BCrypt, iar la login primești un token aleator, valabil opt ore. De ce token și nu sesiune pe server? Același token merge și la REST, și la WebSocket, iar log off-ul îl șterge și închide canalele deschise cu el. Limita: totul e pe HTTP necriptat, deci cine ascultă rețeaua vede parola și token-ul. TLS ar rezolva asta; l-am lăsat deoparte ca să se vadă protocolul.')

// ---------- 9. Ce n-am făcut ----------
s = pres.addSlide({ masterName: 'Continut', sectionTitle: 'Rezultate și limite' })
s.addText('Ce n-am făcut și ce ar urma', { placeholder: 'title' })
const nu = [['TLS', 'totul circulă în clar'], ['Mașini reale', 'zece mașini simulate, pe un laptop'], ['Un singur ceas', 'latența se măsoară ușor, dar e un caz fericit']]
const da = [['HTTPS și MQTT peste TLS', 'parola și token-ul ascunse'], ['Telemetria pe QoS 1', 'fără pierderi în gaură, cu confirmări în plus'], ['NTP și un broker în cloud', 'pentru mașini adevărate']]
s.addText('N-am făcut', { x: M, y: 1.45, w: 5.8, h: 0.55, fontSize: 24, bold: true, color: C.accent4, margin: 0, isTextBox: true, objectName: 'titlu nu' })
s.addText('Ar urma', { x: 6.95, y: 1.45, w: 5.8, h: 0.55, fontSize: 24, bold: true, color: C.accent2, margin: 0, isTextBox: true, objectName: 'titlu da' })
nu.forEach(([t, d], i) => rand(s, M, 2.2 + i * 1.45, 5.8, '–', C.accent4, t, d, `nu ${i}`))
da.forEach(([t, d], i) => rand(s, 6.95, 2.2 + i * 1.45, 5.8, '+', C.accent2, t, d, `da ${i}`))
s.addNotes('Proiectul e doar software: mașinile sînt simulate, iar totul rulează pe laptop. N-am pus TLS. Pentru mașini reale ar trebui TLS, ceasuri sincronizate prin NTP și un broker în cloud. Iar dacă pierderea din gaură nu e acceptabilă, telemetria poate trece pe QoS 1, cu prețul unei confirmări pentru fiecare mesaj.')

// ---------- 10. Concluzii ----------
pres.addSection({ title: 'Final' })
s = pres.addSlide({ masterName: 'Intunecat', sectionTitle: 'Final' })
s.addText('Concluzii', { placeholder: 'title' })
rand(s, M, 1.7, 7.6, '1', C.accent2, 'Căderea vizibilă costă doar timp', 'tunelul: zero pierderi, totul vine din buffer', 'concluzia 1', C.background1)
rand(s, M, 3.0, 7.6, '2', C.accent4, 'Căderea mută costă mesaje', 'cât durează până o descoperă keepalive-ul', 'concluzia 2', C.background1)
rand(s, M, 4.3, 7.6, '3', C.accent1, 'QoS-ul se vede pe hartă', 'pierdut, întârziat, o singură dată', 'concluzia 3', C.background1)
s.addText('Întrebări?', { x: 8.6, y: 2.6, w: L - 8.6 - M, h: 1.6, fontSize: 54, bold: true, color: C.accent1, fontFace: THEME.headFontFace, align: 'center', valign: 'middle', margin: 0, isTextBox: true, objectName: 'intrebari' })
s.addNotes('Pe scurt: o cădere pe care mașina o vede costă doar timp; una pe care n-o vede costă mesaje, cât îi ia keepalive-ului s-o descopere. Iar QoS-ul din MQTT se vede direct pe hartă. Vă mulțumesc; întrebări?')

async function main() {
  fs.mkdirSync(path.dirname(IESIRE), { recursive: true })
  await pres.writeFile({ fileName: IESIRE })
  const tema = process.env.PPTX_APPLY_THEME
  if (tema && fs.existsSync(tema)) {
    const { applyTheme } = require(tema)
    await applyTheme(IESIRE, THEME)
    console.log('tema aplicată')
  } else {
    console.log('fără apply_theme.js: culorile temei rămân cele implicite din Office')
  }
  console.log('scris: ' + IESIRE)
}
main().catch((e) => {
  console.error(e)
  process.exit(1)
})
