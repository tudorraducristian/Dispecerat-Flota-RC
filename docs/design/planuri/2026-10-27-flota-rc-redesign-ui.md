# flota-rc, redesign-ul UI-ului — plan de implementare

> **Pentru agenți:** SUB-SKILL OBLIGATORIU: superpowers:subagent-driven-development
> (recomandat) sau superpowers:executing-plans, ca planul să fie executat task cu
> task. Pașii folosesc căsuțe (`- [ ]`) pentru urmărire.

**Ținta:** toate paginile dispeceratului trec pe stilul „A” ales de Tudor
(bleumarin, alb, albastru, Inter), cu texte pentru oameni, și merg și pe
telefon, în browser. Serverul primește trei cereri mici pentru alarme și
acceptă WebSocket-ul de pe adresele rețelelor private.

**Arhitectura:**
- **Stilul:**
  - o temă Vuetify proprie, deschisă la culoare și setată explicit;
  - un fișier de token-uri CSS;
  - fonturi și iconițe instalate local.
- **Textele pentru oameni** stau într-un singur modul pur, `text.js`, testat cu
  `node --test`, fără dependență nouă. Tot pure și testate sînt calculul
  direcției de mers (`directie.js`) și stiva de alerte (`alerte.js`).
- **Componentele** rămân în stilul existent: Options API, nume în română.
- **Telefonul:** aceleași componente își schimbă așezarea sub 1000 px lățime.
  Pragul se pune o singură dată, în Vuetify (`mobileBreakpoint: 1000`), iar
  componentele citesc `$vuetify.display.mobile`. CSS-ul folosește același prag,
  `@media (max-width: 999px)`. Fiecare task de pagină face pagina deodată pentru
  laptop și pentru telefon. Foaia de pe hartă are un task al ei (Task 7), cu
  logica pură în `foaie.js`, testată.
- **Serverul:** trei metode noi în `Alarme`, expuse de `AlarmeController`, cu
  teste MockMvc. În Task 12, `ConfigWebSocket` acceptă și originile private.

**Stack:** cel de acum, plus trei pachete npm: `@fontsource/inter`,
`@fontsource/jetbrains-mono` și `lucide-vue-next`.

**Spec:** `docs\superpowers\specs\2026-10-27-flota-rc-redesign-ui-design.md`,
deciziile D35–D51. Din spec-ul proiectului, `2026-10-08-flota-rc-design.md`,
contează §7 și §16.4. Machetele de laptop stau în
`RC-Proiect\.superpowers\brainstorm\1220-1793098471\content\`, iar cele de
telefon (design §10) în `RC-Proiect\.superpowers\brainstorm\1636-1793100939\content\`.
Unde textul și macheta diferă, textul câștigă.

*Rescris pe 27 octombrie, după ce telefonul a intrat în design (§10). Versiunea
de dinainte, doar pentru laptop, n-a fost executată.*

## Constrângeri globale

- **Folderul proiectului:** `C:\Users\40747\OneDrive\Documents\Facultate\An 4\Semestrul 1\RC-Proiect\`.
  Remote-ul e `origin`, repo-ul privat `ProiectRC`. După commit-ul fiecărui task
  urmează `git push`. Mesajele de commit urmează stilul de acum, de exemplu
  `UI: bara de sus si meniul`, fără diacritice.
- **Dependențe npm noi,** exact acestea, cu versiunea fixă, fără `^`, ca restul
  din `web\package.json`:
  - `@fontsource/inter` 5.3.0;
  - `@fontsource/jetbrains-mono` 5.3.0;
  - `lucide-vue-next` 1.0.0.

  Se instalează după ce Tudor spune da. Altele nu.
- **Fără dependențe Java noi.** `.\mvnw.cmd -q test` trece la finalul fiecărui
  task care atinge `server\`: cele 342 de teste de acum, plus cele noi.
- **Testele UI:** doar pentru modulele pure, cu `node --test` (Node 24, deja
  instalat), prin scriptul `npm test`. Componentele `.vue` nu au teste automate
  (spec §7). Ele se verifică cu `npm run build` și în browser.
- **O singură temă,** deschisă la culoare. Vuetify nu mai urmează tema
  sistemului.
- **Lățimile** (design §10.1):
  - de la 1280 px în sus: așezarea de laptop, cea frumoasă;
  - 1000–1279 px: tot așezarea de laptop; trebuie doar să meargă, fără nimic
    ieșit din ecran;
  - sub 1000 px: așezarea de telefon. Ținta verificată e 360–430 px, pe
    vertical.
- **Pe telefon, pe toate paginile:**
  - nicio derulare pe orizontală;
  - tot ce se apasă are zona de atingere de cel puțin 44 × 44 px
    (`--atingere`), chiar dacă desenul e mai mic;
  - câmpurile au textul de 16 px;
  - nimic nu depinde de hover.
- **Verificarea pe telefon, în task-urile 3–11:** Tudor deschide pagina pe
  laptop, în Chrome, cu DevTools pe modul telefon (Ctrl+Shift+M), la 360 × 800
  și la 412 × 915. Telefonul adevărat vine abia în Task-urile 12 și 13.
- **Culorile:** doar din token-uri (design §4.1), nu hex scris în componente.
  Excepție face SVG-ul mașinii, care primește culoarea ca parametru.
- **Textele de pe ecran:** exact ca în design. Tabelele de la §4.1 sînt sursa
  pentru tipurile de alarmă, roluri, surse, ținte și stările vremii.
  „Mesaje/s” nu apare nicăieri; se scrie „Mesaje pe secundă”.
- **Codul:**
  - Options API, nume și comentarii în română, cu „sînt”, ca în codul de acum;
  - comentariile explică de ce, nu ce face codul.
- **Serverul, proxy-ul, simulatorul și protocolul** nu se schimbă, în afara
  Task-urilor 1 și 12. Spring Boot rămâne pe `127.0.0.1`.
- **Pașii pentru Tudor** sînt numerotați, cu o singură cale. Ce afișează
  comenzile citește Claude.

## Review Focus

1. **Avalanșa de alarme.** Patru alarme în trei secunde, sau zeci de alarme după
   o reconectare. Rezultatul așteptat:
   - se văd cel mult 3 alerte, cu cea mai nouă sus;
   - aceeași alarmă, cu același `id`, nu apare de două ori;
   - pagina rămâne utilizabilă.

   Testul e în Task 4.
2. **Mașina care stă pe loc, sau o poziție veche venită din buffer.** Mașina nu
   se rotește aiurea și nu sare înapoi. Sub 1 m de mișcare, direcția rămâne
   cea de dinainte. Testul e în Task 5.
3. **Miezul nopții și schimbarea orei, pentru „azi”.** O alarmă de la 00:30, ora
   României, e „azi”, iar una de la 23:30 din ziua de dinainte nu e. Asta trebuie
   să țină și în octombrie (UTC+2), și în iulie (UTC+3). Testul e în Task 1.
4. **Un text neașteptat.** De exemplu, o descriere nouă de la proxy, un id de
   mașină în alt format sau un tip de alarmă necunoscut. Textul se afișează brut,
   fără excepție și fără pagină goală. Testele sînt în Task 2.
5. **Serverul oprit sau token-ul expirat în timp ce cifra de alarme se
   recalculează.** Cifra rămâne ultima știută, fără erori aruncate în buclă. La
   401, `api.js` duce deja la login. Testul e în Task 3.

---

## Harta fișierelor

Toate căile sînt relative la `RC-Proiect\`.

| Fișier | Ce face | Task |
|---|---|---|
| `server\src\main\java\ro\flota\server\flota\Alarme.java` | `lista` cu tip, `numar`, `marcheazaToateVazute` | 1 |
| `server\src\main\java\ro\flota\server\flota\NumarAlarme.java` | nou: `record NumarAlarme(long nevazute, long azi)` | 1 |
| `server\src\main\java\ro\flota\server\flota\TipuriAlarma.java` | nou: cele cinci tipuri | 1 |
| `server\src\main\java\ro\flota\server\api\AlarmeController.java` | cele trei cereri, `inceputulZilei` | 1 |
| `server\src\test\java\ro\flota\server\api\AlarmeControllerTest.java` | nou | 1 |
| `web\package.json` | dependențele, scriptul `test`; scriptul `telefon` | 2, 12 |
| `web\src\tema.js` | nou: tema Vuetify `flota` | 2 |
| `web\src\stil\token-uri.css` | nou: variabilele CSS, fontul, clasele comune | 2 |
| `web\src\text.js` + `text.test.js` | nou: textele pentru oameni | 2 |
| `web\src\main.js` | fonturile, tema, CSS-ul, pragul de 1000 px | 2 |
| `web\src\router.js` | `meta.titlu` pe fiecare rută, pentru bara de telefon | 3 |
| `web\src\api.js` | trei funcții noi pentru alarme | 1 |
| `web\src\numarAlarme.js` + `numarAlarme.test.js` | nou: cifra din meniu | 3 |
| `web\src\components\BaraSus.vue`, `MeniuLateral.vue` | noi | 3 |
| `web\src\App.vue` | cadrul: bară, meniu, conținut, alerte | 3, 4 |
| `web\src\alerte.js` + `alerte.test.js` | nou: stiva de alerte | 4 |
| `web\src\components\IconitaAlarma.vue`, `StivaAlerte.vue` | noi | 4 |
| `web\src\directie.js` + `directie.test.js` | nou: direcția de mers | 5 |
| `web\src\masinaSvg.js` | nou: SVG-ul mașinii văzute de sus | 5 |
| `web\src\components\HartaFlotei.vue`, `ListaMasini.vue` | mașinile, lista | 5 |
| `web\src\pagini\PaginaHarta.vue` | panoul în locul listei, `?masina=`; foaia pe telefon | 4, 6, 7 |
| `web\src\foaie.js` + `foaie.test.js` | nou: înălțimile foii de pe hartă | 7 |
| `web\src\components\FoaieJos.vue` | nou: foaia care urcă de jos | 7 |
| `web\src\components\PanouMasina.vue`, `PanouComenzi.vue` | panoul cu file | 6 |
| `web\src\components\FilaTraseu.vue` | nou; înlocuiește `AlegereInterval.vue`, care se șterge | 6 |
| `web\src\components\EtichetaVreme.vue` | nou: etichetele stării vremii | 6 |
| `web\src\pagini\PaginaRetea.vue` + `components\PanouVreme.vue`, `CifreFlota.vue`, `JurnalRetea.vue`, `GraficLatenta.vue` | Rețea | 8 |
| `web\src\pagini\PaginaAlarme.vue` + `components\CardAlarma.vue` | Alarme | 9 |
| `web\src\pagini\PaginaSetari.vue`, `PaginaConturi.vue` | Setări, Conturi | 10 |
| `web\src\pagini\PaginaLogin.vue` | Login | 11 |
| `server\src\main\java\ro\flota\server\live\ConfigWebSocket.java` + `server\src\test\java\ro\flota\server\live\ConfigWebSocketTest.java` | originile private | 12 |
| `web\index.html`, `web\public\manifest.webmanifest`, `web\public\iconita.svg`, `iconita-192.png`, `iconita-512.png` | manifestul și iconița | 12 |
| `README.md` | „De pe telefon”; „Ce găsești în UI” | 12, 13 |

---

### Task 1: Serverul — numărul alarmelor, „toate văzute”, filtrul după tip

**Files:**
- Create: `server\src\main\java\ro\flota\server\flota\NumarAlarme.java`, `TipuriAlarma.java`
- Modify: `server\src\main\java\ro\flota\server\flota\Alarme.java`
- Modify: `server\src\main\java\ro\flota\server\api\AlarmeController.java`
- Modify: `web\src\api.js`
- Test: `server\src\test\java\ro\flota\server\api\AlarmeControllerTest.java`
- Test: `server\src\test\java\ro\flota\server\flota\AlarmeTest.java`. Fără bază
  de test, cu un `JdbcClient` simulat, ca în `IncarcareDateTest`.

**Interfaces:**
- **Produces, în server:**
  - `TipuriAlarma.TOATE`: un `Set<String>` cu `SUPRAINCALZIRE`,
    `FRANARE_BRUSCA`, `VITEZA_PESTE_LIMITA`, `IESIRE_DIN_ZONA` și
    `OFFLINE_PRELUNGIT`;
  - `Alarme.lista(Boolean vazute, String tip)`, care înlocuiește
    `lista(Boolean)`. `tip` poate fi `null`, iar `LIMIT 200` rămâne;
  - `NumarAlarme Alarme.numar(Instant deLa)`;
  - `int Alarme.marcheazaToateVazute(String tip)`;
  - `static Instant AlarmeController.inceputulZilei(Instant acum)`, după ora
    `Europe/Bucharest`.
- **Produces, în REST:**
  - `GET /api/alarme?vazute=&tip=`;
  - `GET /api/alarme/numar` → `{"nevazute":n,"azi":m}`;
  - `POST /api/alarme/vazute?tip=` → `{"marcate":n}`;
  - un tip necunoscut dă 400 cu `tip de alarmă necunoscut: <tip>`.
- **Produces, în `web\src\api.js`:**
  - `api.alarme(vazute, tip)`;
  - `api.numarAlarme()`;
  - `api.marcheazaToateVazute(tip)`.

- [ ] **Step 1: Scrie testele care pică,** în `AlarmeControllerTest`. Folosește
  `@WebMvcTest(AlarmeController.class)` și `@MockitoBean` pentru `Alarme` și
  `ServiciuAutentificare`, cu token-ul „t” de dispecer, ca în
  `MasiniControllerTest`.

```java
@Test void inceputulZileiIarna() {   // 27 oct.: EET, UTC+2
    assertEquals(Instant.parse("2026-10-26T22:00:00Z"),
            AlarmeController.inceputulZilei(Instant.parse("2026-10-27T21:30:00Z")));
    assertEquals(Instant.parse("2026-10-27T22:00:00Z"),
            AlarmeController.inceputulZilei(Instant.parse("2026-10-27T22:30:00Z")));
}
@Test void inceputulZileiVara() {    // 2 iul.: EEST, UTC+3
    assertEquals(Instant.parse("2026-07-01T21:00:00Z"),
            AlarmeController.inceputulZilei(Instant.parse("2026-07-01T21:30:00Z")));
}
@Test void inceputulZileiInZiuaSchimbariiOrei() {   // 25 oct.: miezul nopții era încă pe UTC+3
    assertEquals(Instant.parse("2026-10-24T21:00:00Z"),
            AlarmeController.inceputulZilei(Instant.parse("2026-10-25T12:00:00Z")));
}
@Test void numarulIeseCaJson()           // when(alarme.numar(any())).thenReturn(new NumarAlarme(12, 148))
                                         // GET /api/alarme/numar → 200, $.nevazute == 12, $.azi == 148
@Test void listaTrimiteTipulMaiDeparte() // GET /api/alarme?vazute=false&tip=FRANARE_BRUSCA → verify(alarme).lista(false, "FRANARE_BRUSCA")
@Test void listaFaraTipTrimiteNull()     // GET /api/alarme → verify(alarme).lista(null, null)
@Test void tipNecunoscutDa400()          // GET /api/alarme?tip=CEVA → 400, $.mesaj == "tip de alarmă necunoscut: CEVA"
@Test void toateVazuteIntoarceCate()     // when(alarme.marcheazaToateVazute(null)).thenReturn(7); POST /api/alarme/vazute → 200, $.marcate == 7
@Test void toateVazuteCuTip()            // POST /api/alarme/vazute?tip=IESIRE_DIN_ZONA → verify(alarme).marcheazaToateVazute("IESIRE_DIN_ZONA")
@Test void toateVazuteCuTipNecunoscutDa400()
@Test void numarulCereToken()            // fără Authorization → 401, $.mesaj == "trebuie să te autentifici"
```

  În `AlarmeTest`, cu `mock(JdbcClient.class, RETURNS_DEEP_STUBS)`, se verifică
  textul SQL trimis:

```java
@Test void listaCuTipFiltreazaSiDupaTip()      // lista(false, "FRANARE_BRUSCA") → verify(jdbc).sql(
        // "SELECT * FROM alarme WHERE vazuta = :vazute AND tip = :tip ORDER BY ora_sosirii DESC, id DESC LIMIT 200")
@Test void listaDoarCuTip()                    // lista(null, "FRANARE_BRUSCA") → "... WHERE tip = :tip ORDER BY ..."
@Test void listaFaraFiltreRamaneCaAcum()       // lista(null, null) → "SELECT * FROM alarme ORDER BY ora_sosirii DESC, id DESC LIMIT 200"
@Test void toateVazuteCuTip()                  // marcheazaToateVazute("X") → "UPDATE alarme SET vazuta = TRUE WHERE vazuta = FALSE AND tip = :tip"
@Test void toateVazuteFaraTip()                // marcheazaToateVazute(null) → "UPDATE alarme SET vazuta = TRUE WHERE vazuta = FALSE"
```

  SQL-ul pe MySQL adevărat se verifică pe viu, la Step 7.

- [ ] **Step 2: Rulează testele și vezi-le picând**

  Run: `.\mvnw.cmd -q -pl server test "-Dtest=AlarmeControllerTest,AlarmeTest"`

  Expected: eroare de compilare, fiindcă `NumarAlarme`, `inceputulZilei` și
  `lista(Boolean, String)` nu există.

- [ ] **Step 3: Scrie `TipuriAlarma` și `NumarAlarme`, apoi metodele din
  `Alarme`.**
  - **`lista`:** adaugă `AND tip = :tip`, respectiv `WHERE tip = :tip`, doar
    când `tip != null`.
  - **`numar`:** un singur `SELECT` cu
    `SUM(vazuta = FALSE)` și `SUM(ora_sosirii >= :deLa)`, cu `COALESCE` la 0,
    fiindcă un tabel gol dă `NULL`. `deLa` trece prin `Ore.ts`, fiindcă baza e
    pe UTC.
  - **`marcheazaToateVazute`:**
    `UPDATE alarme SET vazuta = TRUE WHERE vazuta = FALSE`, plus `AND tip = :tip`
    când e dat. Întoarce numărul de rânduri atinse.

- [ ] **Step 4: Scrie metodele din `AlarmeController`.**
  - Constructorul primește și `Clock ceas`, bean-ul din `ServerApplication`.
  - **Validarea tipului:** se face într-o metodă privată, care aruncă
    `CerereGresita` când tipul nu e în `TipuriAlarma.TOATE`.
  - **`inceputulZilei`:**
    `acum.atZone(ZoneId.of("Europe/Bucharest")).toLocalDate().atStartOfDay(zona).toInstant()`.
  - **`numar()`:** cheamă `alarme.numar(inceputulZilei(ceas.instant()))`.
  - **`vazute`:** întoarce `Map.of("marcate", n)`.

- [ ] **Step 5: Rulează testele și vezi-le trecând**

  Run: `.\mvnw.cmd -q -pl server test "-Dtest=AlarmeControllerTest,AlarmeTest"` → PASS.

  Apoi: `.\mvnw.cmd -q test` → toate trec, 342 plus cele noi.

- [ ] **Step 6: Completează `web\src\api.js`.**
  - **`alarme(vazute, tip)`:** construiește parametrii cu `URLSearchParams` și
    pune doar ce nu e `null`. `PaginaAlarme` de acum cheamă `api.alarme(x)`, deci
    merge și cu un singur argument.
  - **`numarAlarme()`:** `GET /api/alarme/numar`.
  - **`marcheazaToateVazute(tip)`:** `POST /api/alarme/vazute`, cu `?tip=`
    doar când e dat.

- [ ] **Step 7: Verifică pe viu.** Repornește serverul în fundal, apoi fă un
  login prin REST, ca în README, la „Conturile”.

  Run:
  - `GET /api/alarme/numar`;
  - `GET /api/alarme?vazute=false&tip=FRANARE_BRUSCA`, unde toate rândurile
    trebuie să aibă tipul `FRANARE_BRUSCA`;
  - `POST /api/alarme/vazute?tip=FRANARE_BRUSCA`, apoi din nou `numar`.

  Expected: `nevazute` scade exact cu `marcate`.

- [ ] **Step 8: Commit și push**

```powershell
git add server web\src\api.js
git commit -m "Server: numarul alarmelor, toate vazute si filtrul dupa tip"
git push
```

### Task 2: Fundația — tema, fonturile, iconițele, textele

**Files:**
- Modify: `web\package.json`, `web\src\main.js`
- Create: `web\src\tema.js`, `web\src\stil\token-uri.css`, `web\src\text.js`
- Test: `web\src\text.test.js`

**Interfaces:**
- **Produces:**
  - **tema:** `temaFlota`, adică tema Vuetify cu numele `flota`, `dark: false`.
    Culorile sînt:
    - `primary #2563EB`, `background #F8FAFC`, `surface #FFFFFF`;
    - `error #DC2626`, `warning #D97706`, `success #16A34A`, `info #2563EB`;
    - `on-background` și `on-surface` `#0F172A`.
  - **variabilele CSS** din `token-uri.css`, folosite de toate task-urile
    următoare. Lista completă e tabelul din design §4.1:
    - **fonduri:** `--bleumarin`, `--bleumarin-2`, `--fond`, `--suprafata`,
      `--margine`;
    - **text și primar:** `--text`, `--text-2`, `--text-3`, `--primar`,
      `--primar-text`, `--primar-slab`;
    - **stări:** `--verde`, `--verde-slab`, `--verde-text`, `--portocaliu`,
      `--gri-offline`, `--rosu`, `--rosu-slab`, `--rosu-text`, `--ambra`,
      `--ambra-slab`, `--ambra-text`, `--live`, `--live-text`;
    - **fonturi:** `--font`, `--font-mono`;
    - **colțuri și umbră:** `--colt-control` 8px, `--colt-card` 12px,
      `--umbra-plutitor`;
    - **telefon:** `--atingere` 44px, `--bara-telefon` 52px.
  - **pragul de telefon:** `createVuetify` primește
    `display: { mobileBreakpoint: 1000 }`, deci `$vuetify.display.mobile` e
    `true` sub 1000 px. Toate task-urile următoare îl folosesc pe acesta în
    șabloane și `@media (max-width: 999px)` în CSS. Alt prag nu există.
  - **clasele comune** din `token-uri.css`:
    - `.cifre`, adică `font-variant-numeric: tabular-nums`;
    - `.eticheta-mica`: 11 px, majuscule, 600, `letter-spacing: .06em`,
      culoarea `--text-2`;
    - `.titlu-pagina`: 22 px, 700, `letter-spacing: -.02em`.
  - **funcțiile din `text.js`,** toate pure, fără importuri:
    - `numeMasina(id: string): string`;
    - `numeTinta(tinta: string): string`;
    - `numeRol(rol: string): string`;
    - `initiala(nume: string): string`;
    - `sursaAlarmei(sursa: string): string`;
    - `TIPURI_ALARMA`: un obiect `{ [tip]: { nume, ton, iconita } }`, unde `ton`
      e unul din `'rosu' | 'ambra' | 'gri'` și `iconita` e numele componentei
      Lucide;
    - `numeTipAlarma(tip: string): string`;
    - `propozitieAlarma(a: { masinaId, tip, detalii }): string`;
    - `virgula(text: string): string`;
    - `numar(valoare: number, zecimale: number): string`;
    - `stariVreme(descriere: string | null): Array<{ text, ton }>`, unde `ton`
      e unul din `'verde' | 'rosu' | 'bleumarin' | 'ambra' | 'gri'`;
    - `textCifra(n: number): string`;
    - `ora(iso: string): string`;
    - `timpScurs(iso: string, acumMs: number): string`;
    - `grupaTimp(iso: string, acumMs: number): string`;
    - `dataCreare(iso: string): string`;
    - `dataScurta(iso: string): string`, pentru rândul de cont de pe telefon.

- [ ] **Step 1: Cere-i lui Tudor voie pentru cele trei pachete npm,** apoi
  instalează-le cu versiunea exactă:
  `npm install --save-exact @fontsource/inter@5.3.0 @fontsource/jetbrains-mono@5.3.0 lucide-vue-next@1.0.0`.
  În `scripts` adaugă `"test": "node --test"`.

- [ ] **Step 2: Scrie testele care pică,** în `web\src\text.test.js`, cu
  `node:test` și `node:assert/strict`. Datele pentru `grupaTimp` și
  `dataCreare` se fac cu `new Date(an, luna, zi, ora, min)`, adică în ora locală,
  ca testele să nu depindă de fusul orar al calculatorului.

```js
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
```

- [ ] **Step 3: Rulează testele și vezi-le picând**

  Run: `cd web; npm test`

  Expected: FAIL, fiindcă `text.js` nu există.

- [ ] **Step 4: Scrie `text.js`.**
  - Propozițiile, numele și tonurile sînt cele din tabelele din design §4.1.
  - **Iconițele Lucide pentru tipuri:**
    - `Thermometer` pentru supraîncălzire;
    - `OctagonAlert` pentru frânare;
    - `Gauge` pentru viteză;
    - `MapPinOff` pentru zonă;
    - `WifiOff` pentru lipsa semnalului.
  - **`numeMasina`:** recunoaște doar `^masina-(\d+)$`.
  - **`stariVreme`:** citește cuvintele de la stânga la dreapta. `INTARZIERE`
    consumă 2 numere, iar `TUNEL`, `GAURA` și `BANDA` câte unul. La primul
    cuvânt necunoscut, tot restul textului devine o singură etichetă gri.
  - **Formatele de dată și oră:** cu `toLocaleString('ro-RO', …)`.
  - **`timpScurs`:** sub 60 s scrie secunde, sub 60 min minute, apoi ore.

- [ ] **Step 5: Rulează testele și vezi-le trecând**

  Run: `cd web; npm test` → toate trec.

- [ ] **Step 6: Scrie `tema.js` și `token-uri.css`, apoi leagă-le în
  `main.js`.**
  - **Fonturile:** în `main.js` importă `@fontsource/inter/400.css`, `500.css`,
    `600.css` și `700.css`, apoi `@fontsource/jetbrains-mono/400.css` și
    `./stil/token-uri.css`.
  - **Tema:** `createVuetify` primește
    `theme: { defaultTheme: 'flota', themes: { flota: temaFlota } }` și
    `display: { mobileBreakpoint: 1000 }`.
  - **Inter peste tot,** inclusiv în componentele Vuetify. În `token-uri.css`,
    regula pe `.v-application` și pe clasele de tipografie Vuetify.
  - **Pe telefon,** în `token-uri.css`, sub `@media (max-width: 999px)`, textul
    din `input` și `textarea`, inclusiv din câmpurile Vuetify, are 16 px.

- [ ] **Step 7: Verifică.**
  - `npm run build` trece.
  - Cu sistemul pornit și Chrome pe tema întunecată, pagina Alarme are fondul
    deschis, nu negru.
  - DevTools pe un buton arată `font-family` Inter.
  - Pragul se vede abia în Task 3, la bară și la meniu.

  Pentru verificarea în browser, Tudor urmează pașii numerotați ai lui Claude.

- [ ] **Step 8: Commit și push**, cu mesajul `UI: tema, fonturile si textele pentru oameni`.

### Task 3: Bara de sus, meniul și cifra de alarme

**Files:**
- Create: `web\src\numarAlarme.js`, `web\src\numarAlarme.test.js`
- Create: `web\src\components\BaraSus.vue`, `web\src\components\MeniuLateral.vue`
- Modify: `web\src\App.vue`, `web\src\router.js`

**Interfaces:**
- **Consumes:**
  - `api.numarAlarme()` (Task 1);
  - `textCifra`, `numeRol` și `initiala` (Task 2);
  - `flota` și `asculta` din `live.js`.
- **Produces:**
  - `numarAlarme`, un obiect reactiv `{ nevazute: number, azi: number }`;
  - `recalculeaza(cere = api.numarAlarme): Promise<void>`;
  - `pornesteNumararea()` și `opresteNumararea()`.
  - **`BaraSus`:** emite `comuta-meniul`. Ieșirea rămâne în `App.vue`; bara
    emite `iesire`.
  - **`MeniuLateral`:** prop `deschis: boolean`. Pe telefon emite `inchide`,
    când sertarul se închide singur, și `iesire`, de la butonul din josul lui.
  - **`router.js`:** fiecare rută are `meta.titlu`: „Harta”, „Rețea”, „Alarme”,
    „Setări”, „Conturi”.

- [ ] **Step 1: Scrie testul care pică,** în `numarAlarme.test.js`.

```js
test('recalculeaza pune numerele primite', async () => {
  await recalculeaza(async () => ({ nevazute: 12, azi: 148 }))
  assert.deepEqual({ ...numarAlarme }, { nevazute: 12, azi: 148 })
})
test('o eroare lasă ultimele numere știute și nu aruncă', async () => {   // Review Focus 5
  await recalculeaza(async () => ({ nevazute: 5, azi: 9 }))
  await recalculeaza(async () => { throw new Error('serverul a răspuns 503') })
  assert.deepEqual({ ...numarAlarme }, { nevazute: 5, azi: 9 })
})
```

- [ ] **Step 2: Rulează testul și vezi-l picând:** `npm test` → FAIL.

- [ ] **Step 3: Scrie `numarAlarme.js`.**
  - **`pornesteNumararea`:**
    - cheamă `recalculeaza()` pe loc și apoi la 30 de secunde;
    - ascultă `ALARMA` prin `asculta`, iar la fiecare crește `nevazute` și `azi`
      cu 1.
  - **`opresteNumararea`:** oprește intervalul și ascultarea.
  - **Erorile:** se prind în `recalculeaza`, cu un `console.warn`.

- [ ] **Step 4: Rulează testul și vezi-l trecând:** `npm test` → PASS.

- [ ] **Step 5: Scrie `BaraSus.vue`,** după design §4.2 și `bara-de-sus.html`,
  varianta A:
  - **subtitlul** e „Cluj-Napoca · N mașini”, cu N din
    `Object.keys(flota.masini).length`; când N e 0, doar „Cluj-Napoca”;
  - **LIVE:**
    - legat, verde, „LIVE”, cu puls;
    - căzut, roșu, „FĂRĂ LEGĂTURĂ”;
    - pulsul se oprește sub `prefers-reduced-motion: reduce`;
  - **utilizatorul:** cercul cu `initiala(sesiune.nume)`, numele și
    `numeRol(sesiune.rol)`;
  - **ieșirea:** butonul-iconiță `LogOut`, cu `aria-label` și tooltip „Ieșire”.
  - **Pe telefon** (design §10.3 și `telefon-navigare.html`, varianta B),
    bara are 52 px și doar trei lucruri:
    - ☰ (`Menu`), cu un punct roșu de 9 px în colț când
      `numarAlarme.nevazute > 0`; `aria-label` „Meniu” sau, cu punctul, „Meniu,
      alarme nevăzute”;
    - titlul paginii, din `$route.meta.titlu`, 16 px, 600;
    - eticheta LIVE sau „FĂRĂ LEGĂTURĂ”, mai mică.
    - Logo-ul, utilizatorul și ieșirea nu apar; ele stau în sertar.

- [ ] **Step 6: Scrie `MeniuLateral.vue`,** după design §4.3 și `meniul.html`,
  varianta 1:
  - **așezarea:** un `v-navigation-drawer` permanent, nu `temporary`, lat de
    232 px, cu fondul `--bleumarin`;
  - **grupurile:**
    - „Operare”: Harta (`Map`), Rețea (`Activity`), Alarme (`Bell`);
    - „Administrare”: Setări (`Settings`) și, doar pentru admin, Conturi
      (`Users`);
  - **pagina curentă:** fondul `--bleumarin-2`, o linie albastră de 3 px în
    stânga și iconița `#60A5FA`;
  - **la Alarme:** cifra roșie e `textCifra(numarAlarme.nevazute)`, ascunsă când
    textul e gol;
  - **jos:** „Proiect RC · UTCN 2026”.
  - **Pe telefon** (design §10.3), același `v-navigation-drawer` devine
    `temporary`, cu fundalul întunecat Vuetify, lat de `min(78vw, 300px)`:
    - sus, logo-ul, „Dispecerat flotă” și „N mașini”;
    - grupurile, cifra și pagina curentă, ca pe laptop, cu rândurile înalte de
      cel puțin `--atingere`;
    - jos, în locul textului „Proiect RC”, inițiala, numele, rolul și butonul
      `LogOut`, care emite `iesire`;
    - se închide, emițând `inchide`, la alegerea unei pagini și la atingerea
      fundalului;
    - **butonul Înapoi al telefonului** îl închide: la deschidere se pune o
      intrare în istoric cu `history.pushState({ sertar: true }, '')`, iar
      `popstate` închide sertarul. Dacă sertarul se închide altfel, intrarea se
      scoate cu `history.back()`, înainte de schimbarea paginii.

- [ ] **Step 7: Leagă totul în `App.vue`.**
  - **Bara și meniul:** în locul lui `v-app-bar` vin `BaraSus` și
    `MeniuLateral`.
  - **Starea meniului:** `meniu` pornește din `localStorage['flota.meniu']`,
    unde `'strans'` înseamnă închis. Fiecare acces e în `try`/`catch`, ca în
    `sesiune.js`. La comutare se scrie înapoi.
    - **Pe telefon,** `localStorage` nu se citește și nu se scrie. Sertarul
      pornește închis, iar la trecerea peste prag, în orice sens, meniul revine
      la starea potrivită: închis pe telefon, cea din `localStorage` pe laptop.
  - **Conținutul,** pe telefon, ia toată lățimea, fără marginea meniului.
  - **Numărarea:** watch-ul pe `sesiune.token` pornește și oprește și numărarea,
    lângă `pornesteLive` și `pornesteVremea`.
  - **`/login`:** rămâne fără bară și fără meniu, ca acum.

- [ ] **Step 8: Verifică.**
  - `npm test` și `npm run build` trec.
  - În browser, pe Conturi, coloana „Nume” se vede, fiindcă meniul nu mai stă
    peste pagină.
  - ☰ ascunde meniul, iar după F5 rămâne ascuns.
  - Cu serverul oprit, bara arată „FĂRĂ LEGĂTURĂ”, iar în consolă nu apar erori
    în buclă, doar câte un avertisment la 30 s.
  - **La 1000 px** e bara de laptop, iar **la 999 px** e bara de telefon.
  - **În modul telefon, la 360 × 800:**
    1. Bara arată ☰, titlul paginii și LIVE, iar ☰ are punctul roșu.
    2. ☰ deschide sertarul peste pagină. Alegerea „Rețea” îl închide și duce
       la Rețea, cu titlul schimbat.
    3. Sertarul deschis, apoi butonul Înapoi din DevTools, îl închide fără să
       schimbe pagina.
    4. Ieșirea din josul sertarului duce la login.

- [ ] **Step 9: Commit și push**, cu mesajul `UI: bara de sus, meniul si cifra de alarme, si pe telefon`.

### Task 4: Alerta de sub bară și „Vezi pe hartă”

**Files:**
- Create: `web\src\alerte.js`, `web\src\alerte.test.js`
- Create: `web\src\components\IconitaAlarma.vue`, `web\src\components\StivaAlerte.vue`
- Modify: `web\src\App.vue`, `web\src\pagini\PaginaHarta.vue`

**Interfaces:**
- **Consumes:** `TIPURI_ALARMA`, `propozitieAlarma`, `numeTipAlarma` și `ora`
  (Task 2).
- **Produces:**
  - `inStiva(lista: Array<{ id }>, noua: { id }, max = 3): Array`. Întoarce o
    listă nouă, cu cea mai nouă alarmă prima.
  - **`IconitaAlarma.vue`:**
    - props `tip: string` și `marime: number`, implicit 36;
    - desenează pătratul rotunjit pe fondul tonului, cu iconița Lucide în
      culoarea textului tonului;
    - pentru un tip necunoscut, folosește `Bell` pe gri.
  - **Ruta:** `/?masina=<id>` deschide panoul mașinii pe Hartă.

- [ ] **Step 1: Scrie testul care pică,** în `alerte.test.js`. Acoperă Review
  Focus 1.

```js
test('cel mult 3, cea mai nouă prima', () => {
  let s = []
  for (const id of [1, 2, 3, 4]) s = inStiva(s, { id })
  assert.deepEqual(s.map((a) => a.id), [4, 3, 2])
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
```

- [ ] **Step 2: Rulează testul și vezi-l picând:** `npm test` → FAIL.

- [ ] **Step 3: Scrie `inStiva`.** Apoi rulează `npm test` → PASS.

- [ ] **Step 4: Scrie `IconitaAlarma.vue` și `StivaAlerte.vue`,** după design
  §4.4 și `alerta.html`, varianta 1:
  - **locul:** `position: fixed`, la 12 px sub bară, lat de cel mult 430 px. Nu
    acoperă meniul și nici panoul din dreapta.
    - `StivaAlerte` primește prop-ul `dreapta: number`, implicit 0, adică
      lățimea ocupată în dreapta conținutului.
    - Se centrează pe zona dintre meniu și acea lățime.
    - `App.vue` dă 340 pe ruta `/`, iar pe celelalte rute 0;
  - **conținutul cardului:**
    - `IconitaAlarma`;
    - titlul, cu `propozitieAlarma`;
    - sub titlu, `numeTipAlarma` · `ora(alarma.ora)`;
    - „Vezi pe hartă” și X;
  - **durata:**
    - o linie jos, în culoarea tonului, se golește în 6 s, apoi cardul pleacă
      din stivă;
    - `mouseenter` oprește timpul cardului, iar `mouseleave` îl reia de unde a
      rămas;
  - **„Vezi pe hartă”:** `router.push({ path: '/', query: { masina: alarma.masinaId } })`,
    apoi scoate cardul;
  - **accesibilitatea:** containerul are `aria-live="polite"` și nu ia focusul.
    Animația de intrare durează 200 ms și nu există sub `prefers-reduced-motion`.
  - **pe telefon** (design §10.4):
    - stiva stă la 8 px sub bara de 52 px, cu 8 px de margine în stânga și în
      dreapta, iar prop-ul `dreapta` nu mai contează;
    - „Vezi pe hartă” devine „Vezi”;
    - `pointerdown` pe card oprește timpul, iar `pointerup` și `pointercancel`
      îl reiau. Pe laptop rămân `mouseenter` și `mouseleave`;
    - X are zona de atingere `--atingere`.

- [ ] **Step 5: În `App.vue`, `StivaAlerte` înlocuiește `v-snackbar`.**
  Ascultătorul de `ALARMA` din `mounted` cheamă `inStiva`.

- [ ] **Step 6: Fă în `PaginaHarta.vue` legătura cu `?masina=`.**
  - **Watch:** pe `$route.query.masina`, cu `immediate`, plus pe
    `flota.masini`, fiindcă la F5 lista vine după montare. Când id-ul există în
    `flota.masini`, se cheamă `alege(id)`.
  - **Un id necunoscut,** de exemplu `?masina=masina-99`, e ignorat: rămâne
    lista, fără eroare.
  - **`alege(null)`,** cu `?masina` în adresă, face `router.replace({ path: '/' })`.

- [ ] **Step 7: Verifică.**
  - `npm run build` trece.
  - În browser, cu simulatorul pornit, alarmele de viteză din zona Centru vin
    des. Se văd cel mult 3 carduri, cea mai nouă sus, iar sub mouse cardul nu
    dispare.
  - „Vezi pe hartă” de pe pagina Alarme duce la Hartă, cu panoul mașinii
    deschis.
  - `/?masina=masina-99` arată lista.
  - În modul telefon, la 360 × 800, pe Alarme: alerta are toată lățimea, cu
    margini, iar „Vezi” și X încap pe un rând cu titlul. (Pe Hartă, foaia vine
    abia în Task 7.)

- [ ] **Step 8: Commit și push**, cu mesajul `UI: alerta sub bara si Vezi pe harta`.

### Task 5: Mașinile pe hartă și lista flotei

**Files:**
- Create: `web\src\directie.js`, `web\src\directie.test.js`, `web\src\masinaSvg.js`
- Modify: `web\src\components\HartaFlotei.vue`, `web\src\components\ListaMasini.vue`

**Interfaces:**
- **Produces:**
  - `directie(a: { lat, lon }, b: { lat, lon }): number | null`. Întoarce
    unghiul în grade, între 0 și 360, măsurat în sensul acelor de ceasornic de
    la nord. Întoarce `null` sub 1 m.
  - `svgMasina(culoare: string, contur = '#FFFFFF'): string`: SVG-ul mașinii
    văzute de sus, cu `viewBox="0 0 24 40"`, ca în `masini-pe-harta.html`,
    varianta 1. Îl folosește și Task 6, în antetul panoului.
  - `CULORI_STARE = { ONLINE: '#16A34A', VREME: '#EA580C', OFFLINE: '#94A3B8' }`,
    în `masinaSvg.js`. Leaflet are nevoie de valori, nu de variabile CSS.

- [ ] **Step 1: Scrie testul care pică,** în `directie.test.js`. Acoperă Review
  Focus 2.

```js
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
```

- [ ] **Step 2: Rulează testul și vezi-l picând:** `npm test` → FAIL.

- [ ] **Step 3: Scrie `directie`.**
  - **Unghiul:** formula azimutului inițial pe sferă,
    `atan2(sin Δλ·cos φ2, cos φ1·sin φ2 − sin φ1·cos φ2·cos Δλ)`, normalizat în
    `[0, 360)`.
  - **Distanța pentru pragul de 1 m:** haversine, cu R = 6 371 000 m.

  Rulează `npm test` → PASS.

- [ ] **Step 4: Rescrie `HartaFlotei.vue`.**
  - **Markerul:**
    - fiecare mașină e un `L.marker` cu `L.divIcon`, iar `html` e un `div` care
      învelește `svgMasina(culoare)`;
    - `iconSize` e `[22, 36]`, iar `iconAnchor` e `[11, 18]`;
    - rotirea se pune pe `div`-ul interior, prin `transform: rotate(…deg)`,
      niciodată pe elementul markerului, pe care Leaflet îl mută cu `transform`.
  - **Direcția:**
    - `this.ultimulLoc[id]` și `this.directii[id]` stau pe `this`, în afara lui
      `data()`, ca obiectele Leaflet;
    - se recalculează doar când `m.pozitie` s-a schimbat. `aplicaPozitie` din
      `live.js` păstrează deja doar cea mai nouă poziție;
    - când `directie` dă `null`, rămâne direcția veche.
  - **Culoarea:** după regula de acum, din `culoare(m)`.
  - **Mașina selectată:** primește clasa `selectata` pe `div`, care desenează
    inelul albastru, ca în machetă.
  - **Tooltip-ul:** `numeMasina(m.id)`, doar pe laptop. Pe telefon nu se leagă
    tooltip, fiindcă atingerea deschide mașina (design §10.4).
  - **Zona de atingere:** pe telefon, `iconSize` rămâne `[22, 36]`, dar
    `div`-ul exterior are un `padding` transparent, ca atingerea să prindă cel
    puțin 44 × 44 px; `iconAnchor` se mută în centrul zonei mari.
  - **Zonele:** conturul `#2563EB`, de 1,5 px, cu umplerea 0,06.
  - **Traseul:** culoarea primară, de 4 px; „recuperat” rămâne punctat.

- [ ] **Step 5: Restilizează `ListaMasini.vue`,** după `bara-de-sus.html`,
  varianta A:
  - antetul cu `.eticheta-mica`: „Flota · 9 din 10 online”;
  - pe rânduri, punctul stării, `numeMasina` și viteza cu `numar(v, 0)` „km/h”,
    sau „offline” pentru o mașină `OFFLINE`, sau „fără poziție”;
  - rândul selectat are fondul `--primar-slab`;
  - rândurile au cel puțin `--atingere` înălțime, fiindcă pe telefon lista stă
    în foaia de jos (Task 7).

- [ ] **Step 6: Verifică.**
  - `npm test` și `npm run build` trec.
  - În browser, mașinile se rotesc pe străzi în sensul de mers. Se poate
    verifica și cu `TUNEL masina-3 15`: la ieșirea din tunel, mașina 3 nu se
    întoarce invers și nu sare.

- [ ] **Step 7: Commit și push**, cu mesajul `UI: masinile vazute de sus si lista flotei`.

### Task 6: Panoul mașinii, cu file

**Files:**
- Create: `web\src\components\FilaTraseu.vue`, `web\src\components\EtichetaVreme.vue`
- Modify: `web\src\components\PanouMasina.vue`, `web\src\components\PanouComenzi.vue`, `web\src\pagini\PaginaHarta.vue`
- Delete: `web\src\components\AlegereInterval.vue`

**Interfaces:**
- **Consumes:**
  - `svgMasina` și `CULORI_STARE` (Task 5);
  - `stariVreme`, `numeMasina`, `numar` și `timpScurs` (Task 2).
- **Produces:**
  - **`EtichetaVreme.vue`:** prop `descriere: string | null`. Desenează câte o
    etichetă pentru fiecare element din `stariVreme(descriere)`, cu fondul și
    textul tonului. O folosește și Task 8.
  - **`PanouMasina.vue`:** pe lângă cele de acum, prop-ul `zone` și emite
    `inchide`. Nu știe nimic de foaie: pe telefon, Task 7 îl pune în ea așa cum
    e.
  - **`FilaTraseu.vue`:**
    - props `minute: number`;
    - emite `live` și `interval({ de, pana })`, cu ore ISO în UTC, ca
      `AlegereInterval` de acum;
    - `PaginaHarta` folosește aceleași evenimente ca acum.

- [ ] **Step 1: Rescrie `PanouMasina.vue`,** după design §4.5 și
  `panou-si-login.html`, panoul 1:
  - **sus:** „‹ Toată flota”, care emite `inchide`;
  - **antetul:**
    - pătratul cu `svgMasina` pe fondul slab al stării;
    - `numeMasina` și eticheta stării: „Online” verde, „Offline” gri;
    - dedesubt, „zona Centru · văzută acum 1 s”.
      - **Numele zonei:** `PanouMasina` primește prop-ul nou
        `zone: Array<{ id, nume }>`, adică lista pe care `PaginaHarta` o are
        deja. Numele se caută după `zonaId`; dacă lipsește, se afișează
        `zonaId`.
      - **Timpul:** `timpScurs(ultimaVedere, acum)`, cu un `acum` actualizat la
        1 s;
  - **trei plăci:**
    - Viteza `numar(v, 0)` km/h;
    - Temp. `numar(t, 1)` °C;
    - Bateria `numar(b, 0)` %, cu o bară sub cifră, verde peste 20% și roșie la
      20% sau mai puțin;
    - fără poziție, plăcile arată „—”;
  - **rețeaua:** `EtichetaVreme` cu `flota.vreme[id]`, plus
    „N recuperate · M pierdute”;
  - **filele:** `v-tabs` sau un segment propriu, „Comenzi” | „Traseu”. Comenzi e
    implicit.
  - **pe telefon** (design §10.4 și `telefon-harta.html`):
    - rândul „‹ Toată flota” lipsește; în antet, în dreapta, apare X, cu zona
      `--atingere`, care emite `inchide`;
    - cele trei cifre stau pe un rând, ca pe laptop.

- [ ] **Step 2: Restilizează `PanouComenzi.vue`.**
  - **Comanda:** în locul `v-select` vin trei plăci, cu `Gauge`, `House` și
    `MessageCircle`:
    - Limitează viteza;
    - Întoarce la bază;
    - Mesaj pentru șofer.
  - **Câmpurile:** câmpul comenzii alese, cu sufixul „km/h” sau cu contorul de
    200, apoi butonul primar „Trimite”.
  - **„Ultimele comenzi”:** etichetele de stare cu iconiță:
    - `Check` pentru Confirmată;
    - `Clock` pentru Așteaptă mașina;
    - Trimisă fără iconiță;
    - `X` pentru Respinsă.
  - **Textele lungi,** de exemplu un mesaj de 200 de caractere, se taie cu „…”,
    iar textul întreg apare în `title`.
  - **Logica** din `stareAfisata` și `inlocuieste` nu se schimbă.
  - **Pe telefon:**
    - plăcile stau tot trei pe un rând, cu iconița deasupra numelui;
    - ordinea e: plăcile, câmpul, „Ultimele comenzi”, apoi „Trimite”, pe toată
      lățimea, cu `position: sticky; bottom: 0`, ca să rămână lipit de
      marginea de jos a foii cât derulezi.

- [ ] **Step 3: Scrie `FilaTraseu.vue`.**
  - **Butoanele rapide:**
    - „Live · N min” emite `live`;
    - „15 min” și „1 oră” emit `interval`, cu `de = acum − X` și `pana = acum`;
    - „Alt interval…” arată câmpurile `datetime-local` și „Arată”, cu conversia
      de acum din `AlegereInterval`.
  - **Butonul activ** e cel bleumarin.
  - **Legenda:** trei mostre SVG de linie: plină, punctată `1 7` și cu gol, cu
    textele din `panou-si-login.html`.
  - **Ștergerea:** `AlegereInterval.vue` se șterge.
  - **Pe telefon:** butoanele rapide stau câte două pe rând, iar „De la”, „Până
    la” și „Arată” stau unul sub altul, pe toată lățimea. `datetime-local`
    deschide alegătorul telefonului.

- [ ] **Step 4: Schimbă `PaginaHarta.vue`.**
  - **Coloana din dreapta:** e lată de 340 px și arată ori `ListaMasini`, ori
    `PanouMasina`, niciodată pe amândouă.
  - **Comenzile și traseul:** `PanouComenzi` și `FilaTraseu` stau în filele
    panoului.
  - **Erorile de traseu** apar în fila Traseu.
  - **Înălțimea:** se calculează față de bara nouă, de 56 px.
  - **Telefonul** vine în Task 7. Până atunci, sub 1000 px pagina poate arăta
    oricum.

- [ ] **Step 5: Verifică.**
  - `npm run build` trece.
  - În browser:
    - click pe o mașină și „Limitează viteza” 50 duce la „Confirmată”;
    - cu `TUNEL masina-3 15`, mașina 3 are eticheta „Tunel · încă N s”, iar după
      tunel traseul are porțiunea punctată;
    - „15 min” arată traseul fără puncte live noi;
    - „‹ Toată flota” întoarce lista.

- [ ] **Step 6: Commit și push**, cu mesajul `UI: panoul masinii cu file`.

### Task 7: Harta pe telefon — foaia care urcă de jos

**Files:**
- Create: `web\src\foaie.js`, `web\src\foaie.test.js`, `web\src\components\FoaieJos.vue`
- Modify: `web\src\pagini\PaginaHarta.vue`, `web\src\components\HartaFlotei.vue`

**Interfaces:**
- **Consumes:** `ListaMasini` (Task 5), `PanouMasina` cu `inchide` (Task 6),
  `--bara-telefon` și `--atingere` (Task 2).
- **Produces:**
  - `inaltimi(ecranPx: number): { stransa, jumatate, intinsa }`, în px, unde
    `ecranPx` e înălțimea de sub bară;
  - `urmatoarea(stare: string): string`;
  - `dupaTragere(inaltime: number, viteza: number, h: { stransa, jumatate, intinsa }): string`.
    `viteza` e în px/ms, pozitivă în sus;
  - **`FoaieJos.vue`:** prop `stare`, cu `'stransa' | 'jumatate' | 'intinsa'`,
    emite `update:stare`, iar conținutul vine prin slotul implicit;
  - **`HartaFlotei.vue`:** metoda `adu(id: string, josPx: number)`, care mută
    harta ca mașina să fie vizibilă deasupra unei benzi de `josPx` de jos.

- [ ] **Step 1: Scrie testele care pică,** în `foaie.test.js`.

```js
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
```

- [ ] **Step 2: Rulează testele și vezi-le picând:** `npm test` → FAIL.

- [ ] **Step 3: Scrie `foaie.js`.**
  - **`inaltimi`:** strânsă 132 px, pe jumătate `Math.round(ecranPx * 0.48)`,
    întinsă `ecranPx - 24`.
  - **`dupaTragere`:** peste 0,5 px/ms în sus, prima înălțime mai mare decât
    `inaltime`, sau întinsă; sub −0,5 px/ms, prima mai mică, sau strânsă; între
    ele, cea mai apropiată.

  Rulează `npm test` → PASS.

- [ ] **Step 4: Scrie `FoaieJos.vue`,** după design §10.4 și
  `telefon-harta.html`, varianta A.
  - **Așezarea:** `position: absolute`, jos, pe toată lățimea hărții, fond
    `--suprafata`, colțurile de sus de 16 px, o umbră în sus.
  - **Mânerul:** o bară de 36 × 4 px, într-o zonă de `--atingere` înălțime, sus.
    - Tragerea se face cu `pointerdown`, `pointermove` și `pointerup`, cu
      `setPointerCapture`, doar pe zona mânerului; în timpul tragerii, înălțimea
      urmează degetul.
    - La `pointerup`: `dupaTragere`, cu viteza din ultimele ~100 ms.
    - O atingere fără mișcare, sub 6 px, cheamă `urmatoarea`.
    - Mânerul e un `button` cu `aria-label` „Mărește sau micșorează panoul”.
  - **Conținutul:** sub mâner, `overflow-y: auto`. Înălțimile vin din
    `inaltimi(innerHeight - 52)`, recalculate la `resize`.
  - **Animația:** 250 ms între înălțimi, fără ea sub `prefers-reduced-motion`.

- [ ] **Step 5: Adaugă `adu(id, josPx)` în `HartaFlotei.vue`.** Folosește
  `map.panInside(pozitie, { paddingTopLeft: [16, 16], paddingBottomRight: [16, josPx + 16] })`.
  O mașină fără poziție e ignorată.

- [ ] **Step 6: Schimbă `PaginaHarta.vue` pe telefon** (`$vuetify.display.mobile`):
  - **Harta** ia tot spațiul de sub bara de 52 px; coloana de 340 px nu există.
  - **`FoaieJos`** conține `ListaMasini` când nu e aleasă nicio mașină și
    `PanouMasina` când e aleasă.
    - Fără mașină, pornește strânsă.
    - **Alegerea unei mașini,** de pe hartă, din listă sau prin `?masina=`,
      trece foaia pe `jumatate` și cheamă
      `$refs.harta.adu(id, inaltimi(...).jumatate)`.
    - **X din panou** (`inchide`) duce la `alege(null)`, cu foaia strânsă.
  - **La trecerea peste prag,** în orice sens, mașina aleasă rămâne aleasă.
  - **Pe laptop** nu se schimbă nimic din Task 6.

- [ ] **Step 7: Verifică.**
  - `npm test` și `npm run build` trec.
  - În modul telefon, la 360 × 800, cu atingerea emulată de DevTools:
    1. Foaia strânsă arată „FLOTA · N DIN 10 ONLINE” și primele rânduri.
    2. Atingerea unei mașini pe hartă deschide foaia pe jumătate, cu mașina
       vizibilă deasupra ei.
    3. Tras în sus, mânerul întinde foaia, iar fila Comenzi arată „Trimite” lipit
       jos. „Limitează viteza” 50 duce la „Confirmată”.
    4. Fila Traseu are butoanele câte două pe rând.
    5. X întoarce lista, cu foaia strânsă.
    6. Alerta „Vezi” deschide mașina în foaie.
  - La 412 × 915, același drum, fără nimic ieșit din ecran.
  - Pe laptop, la 1280 × 720, Harta arată ca după Task 6.

- [ ] **Step 8: Commit și push**, cu mesajul `UI: harta pe telefon, cu foaia de jos`.

### Task 8: Pagina Rețea

**Files:**
- Create: `web\src\components\PanouVreme.vue`, `web\src\components\CifreFlota.vue`, `web\src\components\JurnalRetea.vue`
- Modify: `web\src\pagini\PaginaRetea.vue`, `web\src\components\GraficLatenta.vue`

**Interfaces:**
- **Consumes:**
  - `EtichetaVreme` (Task 6);
  - `numeMasina`, `numeTinta` și `numar` (Task 2);
  - din `live.js`: `flota.statistici`, `flota.latentaFlotei`, `flota.vreme` și
    `flota.proxyDisponibil`;
  - `api.vreme(cerere)` și `api.evenimente()`.
- **Produces:** `PanouVreme.vue` emite `trimis(eveniment)`, după fiecare răspuns
  de la proxy.

- [ ] **Step 1: Scrie `PanouVreme.vue`,** după design §4.6 și `retea.html`,
  varianta 1. Titlul e „Strică rețeaua”.
  - **„Pentru”:** un `v-select` cu `TOATE` și mașinile, sortate numeric, afișate
    cu `numeTinta`.
  - **Patru plăci,** în grilă 2×2, fiecare cu iconiță, nume și explicația de un
    rând din design:

    | Placa | Iconița | Culoarea | Câmpurile | Butonul | Cererea |
    |---|---|---|---|---|---|
    | Tunel | `Mountain` | roșu | secunde, 1–600 | „Pornește tunelul” | `{ comanda: 'TUNEL', parametri: { secunde } }` |
    | Gaură | `Ban` | bleumarin | secunde, 1–600 | „Deschide gaura” | `GAURA` |
    | Întârziere | `Timer` | chihlimbar | slider 0–3000, pas 50, „Întârziere, ms”; slider 0–1000, pas 50, „Variație, ms” | „Aplică întârzierea” | `INTARZIERE`, `{ ms, variatie }` |
    | Bandă îngustă | `SignalLow` | chihlimbar | kbps, 1–100.000 | „Îngustează banda” | `BANDA`, `{ kbps }` |

  - **Valorile implicite,** ca acum: 15 s, 500 ms, 200 ms și 64 kbps.
  - **Răspunsul:** sub buton, `linie → OK` cu verde sau `ERR …` cu roșu, ca
    acum.
  - **Butonul de jos:** „Rețea normală pentru toată flota”, cu contur verde și
    `CircleCheck`. Trimite `{ comanda: 'NORMAL', tinta: 'TOATE', parametri: {} }`.
  - **Pe telefon** (design §10.5 și `telefon-retea.html`, varianta B, fila
    Comenzi):
    - plăcile au doar iconița și numele; explicația plăcii alese apare o
      singură dată, sub grilă, ca „Tunel: conexiunea cade, mașina ține
      mesajele.”;
    - componenta e o coloană pe toată înălțimea primită: sus, partea care se
      derulează (Pentru, plăcile, explicația, câmpurile); jos, fixe, răspunsul
      proxy-ului, butonul plăcii pe toată lățimea și „Rețea normală pentru toată
      flota”;
    - când apare tastatura, se derulează doar partea de sus, iar cele două
      butoane rămân vizibile deasupra ei.

- [ ] **Step 2: Scrie `CifreFlota.vue`.** Patru plăci albe:
  - „Mesaje pe secundă”: suma `mesajePeSecunda`, cu `numar(x, 1)`;
  - „Latența medie”: ultima valoare care nu e `null` din `latentaFlotei`,
    rotunjită, „ms”, sau „—”;
  - „Recuperate”: suma;
  - „Pierdute”: suma, cu `--rosu-text` când e peste 0.

- [ ] **Step 3: Restilizează `GraficLatenta.vue`.**
  - **Linia:** culoarea `--primar`, de 2 px.
  - **Sub linie:** o umplere cu un gradient vertical de la opacitate 0,22 la 0.
    Poligonul se închide pe axa de jos, de la primul la ultimul punct desenat.
  - **Grila:** în `--margine`, iar etichetele în `--text-3`.
  - **Ce nu se schimbă:** calculul de acum, adică plafonul și `decalaj`.

- [ ] **Step 4: Scrie `JurnalRetea.vue`.**
  - **Așezarea:** fondul `--bleumarin`, colțurile `--colt-card`, fontul
    `--font-mono` de 11,5 px, iar titlul „Jurnalul rețelei”, cu iconița
    `Terminal`.
  - **Fiecare rând:** ora gri, linia brută albă și răspunsul, verde la `OK`,
    roșu în rest.
  - **Liniile rămân brute,** cu `masina-3`.
  - **Lungimea:** cel mult 300 px înălțime, cu derulare internă.
  - **Pe telefon:** ia toată înălțimea filei Jurnal, iar fiecare intrare are
    două rânduri: ora și linia brută, apoi răspunsul, mutat spre dreapta.

- [ ] **Step 5: Recompune `PaginaRetea.vue`.**
  - **Antetul:** `.titlu-pagina` „Rețea”, iar dedesubt punctul verde și
    „Proxy disponibil · mașinile pe 1884, ordinele pe 1885”. Altfel, punctul e
    roșu și scrie „Proxy indisponibil”.
  - **Grila:** `270px 1fr`. În stânga stă `PanouVreme`. În dreapta stau, pe rând,
    `CifreFlota`, graficul, cu titlul „Latența flotei · ultimele 60 de secunde”,
    tabelul și `JurnalRetea`.
  - **Tabelul:** coloanele Mașina, Rețeaua, Mesaje pe secundă, Latența,
    Recuperate și Pierdute.
    - Rândurile vin din `flota.masini`, sortate numeric.
    - Statisticile se potrivesc după `masinaId`.
    - Rețeaua e `EtichetaVreme` cu `flota.vreme[id]`.
    - Pierdutele peste 0 sînt roșii.
  - **Ascultarea de `EVENIMENT_RETEA`** pentru jurnal rămâne ca acum.
  - **Pe telefon** (design §10.5):
    - sub bară, un comutator cu trei file, Comenzi, Flota și Jurnal, pe toată
      lățimea. Pagina se deschide mereu pe Comenzi; fila nu se ține minte;
    - deasupra plăcilor, doar „Proxy disponibil” sau „Proxy indisponibil”, fără
      porturi;
    - **Comenzi:** `PanouVreme`, pe toată înălțimea rămasă;
    - **Flota:** `CifreFlota` două câte două, graficul pe toată lățimea, apoi,
      în locul tabelului, câte un rând de două linii pe mașină: sus,
      `numeMasina` și `EtichetaVreme`; dedesubt, „0,0 mesaje/s · — · 14
      recuperate · 0 pierdute”, cu „pierdute” roșu peste 0;
    - **Jurnal:** `JurnalRetea`;
    - ascultarea jurnalului stă în pagină, nu în fila Jurnal, ca intrările să nu
      se piardă cât ești pe Comenzi.

- [ ] **Step 6: Verifică.**
  - `npm run build` trece.
  - În browser, pe rând:
    1. `TUNEL` pe Mașina 3 dă eticheta roșie, iar în jurnal apare rândul verde.
    2. `INTARZIERE` 500/200 pe Mașina 2 face latența să crească în grafic.
    3. „Rețea normală pentru toată flota” readuce toate etichetele la „Normală”.
    4. Cu proxy-ul oprit, antetul arată „Proxy indisponibil”, iar un buton dă
       eroarea, roșie.
  - În modul telefon, la 360 × 800:
    1. Fila Comenzi încape fără derulare la Tunel, cu butoanele jos.
    2. Întârziere, cu cele două câmpuri: partea de sus se derulează, iar
       butoanele rămân jos.
    3. „Pornește tunelul” pe Mașina 3, apoi fila Flota arată rândul Mașinii 3
       cu „Tunel · încă N s”.
    4. Fila Jurnal are intrarea, pe două rânduri, fără derulare pe orizontală.

- [ ] **Step 7: Commit și push**, cu mesajul `UI: pagina Retea, si pe telefon`.

### Task 9: Pagina Alarme

**Files:**
- Create: `web\src\components\CardAlarma.vue`
- Modify: `web\src\pagini\PaginaAlarme.vue`

**Interfaces:**
- **Consumes:**
  - `api.alarme(vazute, tip)` și `api.marcheazaToateVazute(tip)` (Task 1);
  - `numarAlarme` și `recalculeaza` (Task 3);
  - `IconitaAlarma` (Task 4);
  - din `text.js`: `propozitieAlarma`, `virgula`, `sursaAlarmei`,
    `TIPURI_ALARMA`, `ora`, `timpScurs` și `grupaTimp`.
- **Produces:** `CardAlarma.vue`:
  - props `alarma` și `acum: number`;
  - emite `vazuta(alarma)`;
  - „Vezi pe hartă” face singur `router.push`.

- [ ] **Step 1: Scrie `CardAlarma.vue`,** după design §4.7 și `alarme.html`,
  varianta 2:
  - `IconitaAlarma` de 38 px;
  - titlul, `propozitieAlarma`;
  - dedesubt, `virgula(detalii) · sursaAlarmei(sursa)`;
  - în dreapta, `ora(alarma.ora)` și, sub ea, `timpScurs(alarma.ora, acum)`.
    Ambele folosesc ora mașinii, iar grupele și ordinea folosesc `oraSosirii`
    (design §4.7);
  - „Vezi pe hartă” și butonul-bifă „Am văzut”, cu `aria-label`, doar pentru
    `!vazuta`;
  - **starea:** o alarmă nevăzută are linia albastră de 3 px în stânga; una
    văzută are opacitatea 0,7.
  - **pe telefon** (design §10.6 și `telefon-alarme-v2.html`, aleasă de Tudor):
    - `IconitaAlarma` de 32 px, în stânga;
    - la mijloc, de sus în jos: **ora și timpul scurs pe primul rând,** întregi,
      „14:32 · acum 12 s”; titlul; detaliile și sursa; „Vezi pe hartă”;
    - în dreapta, bifa de 36 px, centrată pe verticală, cu zona `--atingere`;
    - ora nu mai stă în dreapta, unde pe telefon se tăia.

- [ ] **Step 2: Rescrie `PaginaAlarme.vue`.**
  - **Antetul:**
    - „Alarme”, iar dedesubt „N nevăzute · M azi”, din `numarAlarme`;
    - „Le-am văzut pe toate” (`CheckCheck`) și segmentul Nevăzute (cu
      `textCifra`) / Văzute / Toate.
    - Numerele rămân globale. Când e ales un tip, numărul de lângă „Nevăzute”
      se ascunde.
  - **Filtrul după tip:**
    - etichete „Toate tipurile” și cele cinci tipuri, cu punctul tonului;
    - alegerea pune `tip` și reîncarcă prin `api.alarme(FILTRE[filtru], tip)`.
  - **Grupele:**
    - cardurile se grupează după `grupaTimp(oraSosirii, acum)`, în ordinea în
      care vin;
    - fiecare grupă are titlul cu `.eticheta-mica`.
  - **Timpul:** `acum` se actualizează la 10 s, cu `setInterval`, oprit în
    `beforeUnmount`.
  - **„Le-am văzut pe toate”:**
    - cheamă `marcheazaToateVazute(tip)`, apoi `incarca()` și `recalculeaza()`;
    - e dezactivat pe „Văzute” și când lista nu are nicio alarmă nevăzută.
  - **„Am văzut”:** comportamentul de acum, plus `recalculeaza()`.
  - **O alarmă live** intră sus doar dacă se potrivește filtrelor: nu e pe
    „Văzute”, iar `tip` lipsește sau e același.
  - **Lista goală:** „Nicio alarmă aici.”, pe un card simplu.
  - **Pe telefon** (design §10.6):
    - titlul „Alarme” stă în bară, nu în pagină;
    - primul rând: „N nevăzute · M azi” în stânga și „Le-am văzut pe toate”, ca
      link, în dreapta, sus, departe de deget;
    - dedesubt, segmentul Nevăzute / Văzute / Toate, pe toată lățimea;
    - etichetele tipurilor stau pe un singur rând care se derulează pe
      orizontală, doar în interiorul lui, nu cu toată pagina.

- [ ] **Step 3: Verifică.**
  - `npm run build` trece.
  - În browser, pe rând:
    1. Tipurile apar în română, fără underscore, iar zecimalele au virgulă.
    2. Filtrul „Frânare bruscă” arată doar frânări.
    3. „Le-am văzut pe toate”, cu filtrul acela, scade cifra din meniu exact cu
       numărul lor.
    4. O alarmă nouă apare sus, în „Ultimele 5 minute”.
    5. „Vezi pe hartă” deschide panoul mașinii.
  - În modul telefon, la 360 × 800:
    1. Pe fiecare card, ora și „acum …” se văd întregi, pe primul rând.
    2. Rândul de tipuri se derulează pe orizontală, iar pagina nu.
    3. Bifa marchează alarma, iar punctul roșu de pe ☰ dispare când nu mai e
       nicio alarmă nevăzută.
    4. „Vezi pe hartă” deschide mașina în foaia de pe Hartă.

- [ ] **Step 4: Commit și push**, cu mesajul `UI: pagina Alarme ca flux de carduri, si pe telefon`.

### Task 10: Setări și Conturi

**Files:**
- Modify: `web\src\pagini\PaginaSetari.vue`, `web\src\pagini\PaginaConturi.vue`

**Interfaces:** `numeRol`, `initiala`, `dataCreare` și `dataScurta` (Task 2).
Cererile rămân cele de acum.

- [ ] **Step 1: Restilizează `PaginaSetari.vue`,** după design §4.8 și
  `setari-conturi.html`, sus.
  - **Antetul:** „Setări”, iar dedesubt „Contul tău și regulile flotei”.
  - **Două carduri pe o grilă de 2 coloane.**
  - **Cardul contului:**
    - inițiala, numele și eticheta rolului;
    - „Schimbă parola”, cu textul „După schimbare rămâi conectat.”;
    - câmpurile cu eticheta deasupra, nu doar placeholder;
    - un ochi (`Eye`/`EyeOff`) pe fiecare parolă.
  - **Cardul „Flota”:**
    - „Se aplică pe loc, pentru toți dispecerii.”;
    - etichetele „Alarmă când o mașină e offline de” și „Traseul din panoul
      mașinii arată ultimele”, cu sufixele „secunde” și „minute”;
    - ajutorul „Între 10 și 3600.” și „Între 1 și 360.”;
    - **pentru dispecer:** caseta cu `Lock`, „Doar administratorul le poate
      schimba.”, iar câmpurile sînt doar de citit, fără buton.
  - **Ce nu se schimbă:** logica și mesajele de acum.
  - **Pe telefon** (design §10.6): o singură coloană, cardurile unul sub altul,
    câmpurile pe toată lățimea cardului, iar „Schimbă parola” și „Salvează” pe
    toată lățimea, la capătul cardului lor. Titlul „Setări” stă în bară.

- [ ] **Step 2: Restilizează `PaginaConturi.vue`,** după design §4.9.
  - **Antetul:** „Conturi”, cu „N conturi” dedesubt, și butonul primar
    „Cont nou”, cu `Plus`.
  - **Lista,** câte un rând pe cont:
    - inițiala, numele și eticheta „tu” pe rândul propriu;
    - rolul: Administrator pe `--primar-slab`, Dispecer gri;
    - `dataCreare(creatLa)`;
    - „Resetează parola” (`KeyRound`) și „Șterge” (`Trash2`, roșu), doar pe
      celelalte rânduri.
  - **Fereastra „Cont nou”:**
    - textul „Rolul nu se mai poate schimba după creare.”;
    - Nume, cu ajutorul de acum, și Parolă, cu ochi;
    - rolul se alege dintre două plăci, cu textele din design: Dispecer e
      implicit, iar Administrator e a doua placă;
    - „Renunță” și „Creează contul”.
  - **Resetarea și ștergerea:** ferestrele de acum, în stilul nou. Ștergerea
    rămâne roșie.
  - **Pe telefon** (design §10.6 și `telefon-restul.html`):
    - sus, doar „N conturi”; titlul stă în bară;
    - **rândul:** inițiala; numele, cu „tu”; pe al doilea rând, eticheta rolului
      și `dataScurta(creatLa)`; în dreapta, ⋮ (`EllipsisVertical`, cu
      `aria-label` „Acțiuni pentru <nume>”), care deschide un `v-menu` cu
      „Resetează parola” și „Șterge contul”, roșu. Pe rândul propriu, ⋮
      lipsește;
    - **„Cont nou”:** un buton care plutește, `position: fixed`, la 16 px de
      dreapta și de jos, cu `Plus` și textul; lista are `padding-bottom` de
      88 px, ca ultimul rând să nu stea sub buton;
    - **fereastra „Cont nou”:** `v-dialog` cu `:fullscreen="mobil"`; sus, X și
      „Cont nou”; jos, „Renunță” și „Creează contul”, una lângă alta;
    - **resetarea și ștergerea:** ferestre mici, late de `calc(100% - 32px)`.

- [ ] **Step 3: Verifică.**
  - `npm run build` trece.
  - În browser, ca admin:
    1. Creezi contul `proba.ui`, cu rolul Dispecer.
    2. Resetezi parola lui.
  - Ca `proba.ui`:
    1. Meniul n-are Conturi.
    2. Setările Flotei sînt cu lacăt.
    3. Schimbi parola.
  - La final, ca admin, ștergi `proba.ui`.
  - În modul telefon, la 360 × 800, același drum, plus:
    1. „Cont nou” plutește jos, în dreapta, și nu acoperă ultimul rând.
    2. „Cont nou” se deschide pe tot ecranul.
    3. ⋮ deschide „Resetează parola” și „Șterge contul”; ștergerea cere
       confirmare.
    4. Setări: câmpurile și butoanele au toată lățimea, fără derulare pe
       orizontală.

- [ ] **Step 4: Commit și push**, cu mesajul `UI: Setari si Conturi, si pe telefon`.

### Task 11: Login

**Files:**
- Modify: `web\src\pagini\PaginaLogin.vue`

- [ ] **Step 1: Rescrie șablonul,** după design §4.10 și `panou-si-login.html`,
  login-ul 2:
  - **Așezarea:** `display: flex`, pe toată înălțimea ferestrei.
  - **Stânga,** bleumarin, cu harta stilizată în fundal, ca în machetă:
    - sus, logo-ul și „Dispecerat flotă”;
    - fraza de 26 px, 700: „Zece mașini, o rețea care se strică la comandă și
      un dispecerat care rezistă.”;
    - trei rânduri cu iconiță:
      - „MQTT, cu QoS 0, 1 și 2” (`Radio`);
      - „Proxy cu tunel, gaură, întârziere și bandă” (`Mountain`);
      - „Hartă live a Clujului” (`Map`);
    - jos, „Proiect la Rețele de Calculatoare · UTCN · 2026”.
  - **Dreapta,** albă:
    - „Intră în cont” și „Cu contul primit de la administrator.”;
    - Nume și Parolă, cu eticheta deasupra, ochi la parolă și `autocomplete` ca
      acum;
    - eroarea, deasupra butonului;
    - „Intră”, primar, pe toată lățimea.
  - **Sub 1000 px** (design §10.6 și `telefon-restul.html`, Login), prin
    `@media (max-width: 999px)`, ecranul trece pe o coloană:
    - sus, o bandă bleumarin, cu harta stilizată în fundal: logo-ul,
      „Dispecerat flotă” și fraza, la 17 px;
    - cele trei rânduri cu iconiță nu apar;
    - dedesubt, pe alb, formularul, apoi, jos, „Proiect la Rețele de
      Calculatoare · UTCN · 2026”, mic și gri.
  - **Ce nu se schimbă:** scriptul.

- [ ] **Step 2: Verifică.**
  - `npm run build` trece.
  - În browser, pe rând:
    1. O parolă greșită arată eroarea.
    2. Enter trimite formularul.
    3. Login-ul de pe `/login?inapoi=/alarme` duce la Alarme.
    4. Fereastra îngustată sub 1000 px arată banda sus și formularul dedesubt.
    5. În modul telefon, la 360 × 800, tot formularul și „Intră” încap fără
       derulare.

- [ ] **Step 3: Commit și push**, cu mesajul `UI: login cu ecranul impartit, si pe telefon`.

### Task 12: Telefonul ajunge la laptop — originile, `npm run telefon`, iconița

**Files:**
- Modify: `server\src\main\java\ro\flota\server\live\ConfigWebSocket.java`
- Test: `server\src\test\java\ro\flota\server\live\ConfigWebSocketTest.java`, nou
- Modify: `web\package.json`, `web\index.html`
- Create: `web\public\manifest.webmanifest`, `web\public\iconita.svg`,
  `web\public\iconita-192.png`, `web\public\iconita-512.png`
- Modify: `README.md`, secțiune nouă „De pe telefon”, după „Cum pornești”

**Interfaces:**
- **Produces:**
  - `ConfigWebSocket.ORIGINI`: un `String[]` public și static, cu
    `http://localhost:*`, `http://127.0.0.1:*`, `http://192.168.*:*`,
    `http://10.*:*` și `http://172.16.*:*` … `http://172.31.*:*`;
  - scriptul `npm run telefon`.

- [ ] **Step 1: Scrie testul care pică,** în `ConfigWebSocketTest`. Verifică
  lista cu aceeași potrivire pe care o folosește Spring la handshake,
  `CorsConfiguration.checkOrigin`.

```java
private static String verifica(String origine) {
    var c = new CorsConfiguration();
    c.setAllowedOriginPatterns(List.of(ConfigWebSocket.ORIGINI));
    return c.checkOrigin(origine);
}
@Test void laptopulMergeCaAcum() {
    assertNotNull(verifica("http://localhost:5173"));
    assertNotNull(verifica("http://127.0.0.1:5173"));
}
@Test void retelelePrivateTrec() {
    assertNotNull(verifica("http://192.168.137.1:5173"));   // hotspot-ul din Windows
    assertNotNull(verifica("http://192.168.43.12:5173"));   // hotspot de Android
    assertNotNull(verifica("http://10.0.0.5:5173"));
    assertNotNull(verifica("http://172.16.0.2:5173"));
    assertNotNull(verifica("http://172.31.255.1:5173"));
}
@Test void adreselePubliceSintRefuzate() {
    assertNull(verifica("http://172.32.0.1:5173"));
    assertNull(verifica("http://172.15.0.1:5173"));
    assertNull(verifica("http://8.8.8.8:5173"));
    assertNull(verifica("https://exemplu.ro"));
}
```

- [ ] **Step 2: Rulează testul și vezi-l picând**

  Run: `.\mvnw.cmd -q -pl server test "-Dtest=ConfigWebSocketTest"`

  Expected: eroare de compilare, fiindcă `ORIGINI` nu există.

- [ ] **Step 3: Scrie `ORIGINI`** și folosește-l în
  `setAllowedOriginPatterns(ORIGINI)`. Cele 16 intrări `172.16`–`172.31` se
  generează, nu se scriu de mână. Actualizează comentariul clasei: de ce sînt
  acceptate rețelele private (design §10.2).

- [ ] **Step 4: Rulează testele și vezi-le trecând.**
  `.\mvnw.cmd -q -pl server test "-Dtest=ConfigWebSocketTest"` → PASS, apoi
  `.\mvnw.cmd -q test` → toate trec.

- [ ] **Step 5: Adaugă scriptul** `"telefon": "vite --host 0.0.0.0"` în
  `web\package.json`. `vite.config.js` rămâne pe `127.0.0.1`, deci
  `npm run dev` nu se schimbă.

- [ ] **Step 6: Scrie manifestul și iconița** (design §10.7, D51).
  - **`iconita.svg`:** pătratul albastru `#2563EB`, rotunjit, cu iconița
    Lucide `Route` albă, ca logo-ul din bară. Are doar `viewBox="0 0 512 512"`,
    fără `width` și `height`, ca să umple orice fereastră.
  - **PNG-urile:** se fac din SVG cu Edge, fără dependență nouă:
    `& "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" --headless --screenshot="<cale>\iconita-192.png" --window-size=192,192 "file:///<cale>/iconita.svg"`,
    apoi la fel pentru 512. Claude deschide PNG-urile cu Read și verifică să nu
    aibă margini albe.
  - **`manifest.webmanifest`:**
    `{ "name": "Dispecerat flotă", "short_name": "Dispecerat", "start_url": "/", "display": "browser", "background_color": "#F8FAFC", "theme_color": "#0F172A", "icons": [192, 512 și SVG-ul cu "sizes": "any"] }`.
    `display` e `browser`, fiindcă pe `http://192.168…` Chrome oricum deschide
    o filă obișnuită.
  - **`index.html`:** `<link rel="manifest" href="/manifest.webmanifest">`,
    `<meta name="theme-color" content="#0F172A">` și
    `<link rel="icon" href="/iconita.svg">`.

- [ ] **Step 7: Scrie secțiunea „De pe telefon” din README,** după design §10.2:
  1. Laptopul se leagă la hotspot-ul telefonului.
  2. UI-ul pornește cu `npm run telefon`, în loc de `npm run dev`.
  3. Adresa pentru telefon e cea de pe rândul „Network” al lui Vite.
  4. Prima dată, Windows întreabă de Node.js: da, pentru rețele private.
  5. Dacă rețeaua hotspot-ului apare ca publică, profilul ei trece pe
     „Privată”.
  6. Pe telefon, în Chrome, deschizi adresa și intri în cont. Opțional, din
     meniul Chrome, „Adaugă pe ecranul de pornire”.

- [ ] **Step 8: Verifică pe viu,** cu tot sistemul pornit de Claude în fundal și
  UI-ul cu `npm run telefon`.
  - **Claude:**
    - `ipconfig` dă adresa laptopului din rețea, iar `curl` pe
      `http://<adresa>:5173/` întoarce `index.html`, cu manifestul;
    - handshake-ul WebSocket, cu un token luat prin REST, ca în README:
      `curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" -H "Origin: http://192.168.137.1:5173" "http://127.0.0.1:8080/ws?token=<token>"`
      dă `101`, iar cu `Origin: http://8.8.8.8:5173` dă `403`;
    - `npm run dev` ascultă tot doar pe `127.0.0.1`.
  - **Tudor, pe telefonul lui,** după pașii lui Claude: deschide adresa, intră
    în cont, iar harta arată mașinile mișcându-se.

- [ ] **Step 9: Commit și push**, cu mesajul `Telefonul: originile private, npm run telefon si iconita`.

### Task 13: Verificarea de la cap la coadă și documentele

**Files:**
- Modify: `README.md`, secțiunea „Ce găsești în UI”
- Modify: `docs\scenariu-prezentare.md`
- Modify: copiile din `docs\design\`, după D27: spec-ul, designul
  redesign-ului și acest plan, copiat în `docs\design\planuri\`
- Modify, în AIS-OS: `docs\superpowers\specs\2026-10-08-flota-rc-design.md`
  (design §9)

- [ ] **Step 1: Rulează tot.**
  - `.\mvnw.cmd -q test`: toate trec.
  - `cd web; npm test`: toate trec.
  - `cd web; npm run build`: trece.

- [ ] **Step 2: Actualizează „Ce găsești în UI” din README.**
  - **Culorile mașinilor** nu se schimbă.
  - **Ce adaugi:**
    - mașinile văzute de sus, rotite;
    - panoul cu file;
    - cifra de alarme din meniu;
    - filtrul după tip și „Le-am văzut pe toate”;
    - „Vezi pe hartă”.
  - **„Paginile se deschid din meniul lateral…”** devine: pe laptop, meniul stă
    mereu în stânga, iar ☰ îl ascunde; pe telefon, ☰ deschide sertarul, iar
    punctul roșu de pe el arată alarmele nevăzute.
  - **Un paragraf despre telefon:** toată aplicația merge și pe telefon, cu
    trimitere la „De pe telefon” (Task 12).
  - **În `docs\scenariu-prezentare.md`,** comenzile de rețea se dau de pe
    telefon, iar efectul se vede pe laptop, la proiector (design §9). Pornirea
    UI-ului din scenariu devine `npm run telefon`.
  - **În `docs\scenariu-prezentare.md` și în README,** textele vechi din UI devin
    cele noi (design §9):
    - „Normal” devine „Rețea normală pentru toată flota”;
    - „proxy indisponibil” devine „Proxy indisponibil”;
    - „fără legătură cu serverul” devine „FĂRĂ LEGĂTURĂ”.
  - **Spec-ul proiectului, în AIS-OS** (design §9):
    - deciziile D35–D51 intră în tabelul de la §2, iar D30 e marcată ca
      înlocuită de D37 pe laptop și revenită prin D47 pe telefon;
    - Î10 se închide: profesorul vrea comenzi de pe telefon, iar browserul îi
      ajunge;
    - la §13, „aplicația pe telefon” devine „aplicația nativă pe telefon”.
    - Se face commit separat, în AIS-OS:
      `Facultate: flota-rc, spec-ul proiectului cu D35-D51 si I10 inchisa`.
  - **Copiile din `docs\design\`:** copiază din AIS-OS spec-ul proiectului,
    designul redesign-ului și planul, după D27.

- [ ] **Step 3: Parcurgerea lui Tudor,** cu tot sistemul pornit de Claude în
  fundal. Pașii numerotați îi dă Claude în chat, iar Tudor spune ce vede.
  1. Login, ca admin.
  2. Harta, la 1920 × 1080: mașinile, lista, panoul, comenzile și traseul.
  3. Aceeași pagină la 1280 × 720, cu zoom-ul Chrome sau cu fereastra
     micșorată: nimic tăiat și fără derulare pe orizontală.
  4. Rețea: un tunel și o gaură.
  5. Alarme: filtrul și „Vezi pe hartă”.
  6. Setări și Conturi.
  7. Ieșirea, apoi login ca dispecer: fără Conturi, cu lacătul pe Setări.
  8. Chrome pe tema întunecată: aplicația rămâne deschisă la culoare.
  9. La 1024 × 768: nimic ieșit din ecran, iar harta e folosibilă.
  10. **Pe telefonul lui,** cu laptopul pe hotspot și UI-ul pornit cu
      `npm run telefon` (design §10.8):
      1. login-ul, de pe adresa din rețea;
      2. harta, cu mașinile mișcându-se live;
      3. o mașină, foaia trasă în sus și „Limitează viteza”;
      4. pe Rețea, „Pornește tunelul” pe Mașina 3, văzut pe laptop, apoi „Rețea
         normală pentru toată flota”;
      5. o alarmă marcată ca văzută, cu cifra din meniul laptopului scăzută în
         cel mult 30 de secunde;
      6. sertarul, cu ieșirea din cont;
      7. ca admin, Setări, Conturi și „Cont nou”;
      8. „Adaugă pe ecranul de pornire” pune iconița.
  11. Înapoi pe laptop, la 1280 × 720: Harta arată ca la pasul 3.

  Ce găsește Tudor se repară în același task, cu câte un commit pe reparație.

- [ ] **Step 4: Scenariul de test.** Pașii din `docs\scenariu-test.md` care
  ating UI-ul merg în continuare. Claude îi rulează pe cei din REST și din
  proxy, iar Tudor pe cei din browser.

- [ ] **Step 5: Commit și push**, cu mesajul `README, scenariul si copiile: UI-ul nou, si pe telefon`.
  Apoi Claude oprește procesele pornite în fundal.
