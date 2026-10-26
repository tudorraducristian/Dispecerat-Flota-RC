# Scenariul de test, de la cap la coadă

Se rulează de mână la sfârșitul fazelor 3, 4 și 5 (spec §10). La fiecare pas
scrie ce se poate verifica din ce fază.

## Pregătirea

1. Serviciul MySQL pornit.
2. Terminalele din `README.md`, în ordine: A (Mosquitto), P (proxy), S
   (serverul), W (UI-ul), C (simulatorul). UI-ul deschis pe
   http://127.0.0.1:5173.
3. O, pentru ordine:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File unelte\ordine.ps1
   ```

4. D, pentru bază:

   ```powershell
   $mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
   function Sql($q) { & $mysql -h 127.0.0.1 -u flota -pflota flota -e $q 2>$null }
   ```

## Interogările folosite mai jos

Toate se uită la ultimele 2 minute. În bază, orele sînt în UTC, de aceea se
folosește `UTC_TIMESTAMP`.

```powershell
# Pozițiile recuperate din buffer, pe mașină
Sql "SELECT masina_id, COUNT(*) AS din_buffer FROM pozitii WHERE din_buffer AND ora_sosirii > UTC_TIMESTAMP(3) - INTERVAL 2 MINUTE GROUP BY masina_id"

# Numerele de ordine care lipsesc, pe mașină. E corect doar dacă simulatorul n-a fost repornit în interval.
Sql "SELECT masina_id, MAX(seq) - MIN(seq) + 1 - COUNT(*) AS lipsa FROM pozitii WHERE ora_sosirii > UTC_TIMESTAMP(3) - INTERVAL 2 MINUTE GROUP BY masina_id"

# Latența medie a mesajelor live din ultimele 30 de secunde, în milisecunde
Sql "SELECT masina_id, ROUND(AVG(TIMESTAMPDIFF(MICROSECOND, ora_masinii, ora_sosirii)) / 1000) AS latenta_ms FROM pozitii WHERE NOT din_buffer AND ora_sosirii > UTC_TIMESTAMP(3) - INTERVAL 30 SECOND GROUP BY masina_id"

# Din faza 4: contoarele serverului
Sql "SELECT id, stare, recuperate, pierdute FROM masini"

# Din faza 5: ultimele comenzi
Sql "SELECT id, masina_id, tip, stare, ora_trimiterii, ora_confirmarii FROM comenzi ORDER BY id DESC LIMIT 5"
```

## Pașii

### 1. Totul pornit

Aștepți 30 de secunde.

- **Din faza 3:**
  - toate mașinile sînt verzi;
  - `lipsa` e 0 la toate.
- **Din faza 4:** `pierdute` e 0 la toate.

### 2. `TUNEL masina-3 15`

- **Din faza 3:**
  - `masina-3` devine gri într-o secundă, iar după 15–25 de secunde e din nou
    verde;
  - după revenire, `din_buffer` are rânduri pentru `masina-3`.
- **Din faza 4:**
  - `pierdute` a crescut cu cel mult 1, adică mesajul care era pe drum în clipa
    tăierii;
  - `recuperate` a crescut;
  - pe hartă, porțiunea din tunel e punctată.

### 3. `GAURA masina-5 10`

- **Din faza 3:**
  - `masina-5` rămâne verde câteva secunde, apoi devine gri, apoi din nou
    verde;
  - după revenire, `lipsa` e peste 0 pentru `masina-5`: mesajele din primele
    secunde ale găurii s-au pierdut;
  - `din_buffer` are rânduri: cele de după ce mașina a observat gaura.
- **Din faza 4:**
  - `pierdute` a crescut cu mai multe mesaje, iar `recuperate` a crescut și el;
  - pe hartă se vede o porțiune lipsă, urmată de una punctată.

### 4. `GAURA masina-5 4`

- **Din faza 3:**
  - `masina-5` rămâne verde tot timpul;
  - după gaură, cele vreo 4 poziții sosesc deodată;
  - `lipsa` nu crește, iar `din_buffer` nu primește rânduri noi. Gaura e mai
    scurtă decât cele 7,5 secunde ale keepalive-ului, deci nimic nu se pierde,
    doar întârzie.
- **Din faza 4:** `pierdute` și `recuperate` nu se schimbă.

### 5. `LIMITEAZA_VITEZA` în timpul unui tunel

**Din faza 5.**

1. `TUNEL masina-3 30`.
2. Din panoul mașinii, comanda `LIMITEAZA_VITEZA` cu 50 km/h.

Se așteaptă:
- comanda rămâne `TRIMISA`, iar UI-ul spune „așteaptă mașina”;
- după ieșirea din tunel, devine `CONFIRMATA`;
- viteza lui `masina-3` coboară cu cel mult 5 km/h pe secundă până la 50, deci
  2–3 secunde mai trece de 50, apoi nu mai trece.

### 6. `INTARZIERE TOATE 500 200`

- **Din faza 3:**
  - mașinile se mișcă pe hartă cu o jumătate de secundă în urmă;
  - `latenta_ms` urcă la vreo 500;
  - `din_buffer` nu primește rânduri noi.
- **Din faza 5:** graficul de latență din pagina Rețea urcă la vreo 500.

La final, `NORMAL TOATE`.

### 7. Login-ul

**Din faza 6.** Un cont de dispecer, de exemplu `dispecer1`, făcut din Conturi.

1. **Două taburi și ieșirea.** Login ca `dispecer1`, apoi un al doilea tab:
   taburile împart token-ul din `localStorage`. „Ieșire” în primul. Se
   așteaptă: în cel mult 3 secunde, al doilea tab e pe `/login`.
2. **Token-ul expirat cu pagina deschisă.** Cu un tab logat, în D:

   ```powershell
   Sql "UPDATE tokenuri SET expira_la = UTC_TIMESTAMP(3) - INTERVAL 1 SECOND"
   ```

   Se așteaptă: în cel mult 70 de secunde, tab-ul e pe `/login`, iar tabelul
   `tokenuri` e gol.
3. **Resetarea parolei scoate omul din aplicație.** `dispecer1` e logat în
   browser. Adminul îi resetează parola pe endpoint-ul 20, cu `curl.exe` și
   token-ul lui de admin, fiindcă același browser ține un singur login. Se
   așteaptă: tab-ul lui `dispecer1` ajunge pe `/login`, iar parola veche nu mai
   merge.

Pe 2026-10-26, partea de server a pașilor 1–3, verificată cu taburi simulate
din Node: canalele se închid cu 4001 în 16 ms la ieșire, în 57 de secunde la
token-ul expirat și în 80 ms la resetare; token-ul vechi dă 401, iar parola
veche, tot 401.

## Măsurători

Se completează în faza 4 (riscul R6) și se folosesc în scenariul prezentării.

| Ce | Mosquitto 2.1.2, 2026-10-26 | HiveMQ CE 2026.5, 2026-10-22 |
|---|---|---|
| `GAURA masina-5 10`: secunde până devine gri | 6,6 (5,8 · 6,8 · 6,6) | 5,2 (6,5 · 5,2 · 5,0) |
| `GAURA masina-5 10`: mesaje pierdute | 7 (6 · 7 · 7) | 5 (7 · 5 · 5) |
| `GAURA masina-5 10`: mesaje recuperate | 3 (4 · 3 · 3) | 5 (3 · 5 · 6) |
| `TUNEL masina-3 15`: secunde până revine verde | 19,2 (19,9 · 19,2 · 18,9) | 18,6 (18,1 · 19,1 · 18,6) |
| `GAURA masina-5 N`: cea mai lungă gaură fără pierderi | 6 secunde (0 · 0 · 0); la 7, câte 7 pierdute de fiecare dată | 6 secunde; la 7, o pierdere din 3 încercări |

În paranteză sînt cele trei încercări; în tabel e valoarea din mijloc. Cifrele
pentru prezentare sînt cele cu Mosquitto, broker-ul din `README.md`.

Mosquitto le-a măsurat Claude pe laptopul lui Tudor, cu un script care citește
starea din REST la 100 ms, după pașii 3–5 din planul fazei 4. HiveMQ CE l-a
folosit colegul pe 2026-10-22, pe laptopul lui, fiindcă mosquitto.org nu
răspundea atunci. De ce gri vine înainte de 7,5 secunde: broker-ul numără
1,5 × keepalive de la ultimul pachet primit, care a plecat cu până la o secundă
înainte de gaură, iar Mosquitto numără pe secunde întregi.

Tot atunci, verificările din faza 4:
- un tunel de 60 de secunde cu `buffer.marime=20`: în buffer 20 de mesaje,
  aruncate 68, iar serverul a numărat exact 68 de pierdute; revenirea, la 87 de
  secunde, din cauza pauzei de reconectare care ajunsese spre 30;
- simulatorul repornit: câte un „simulatorul a repornit” pentru fiecare mașină,
  iar `pierdute` a rămas 0;
- serverul pornit după simulator, cu `ultimul_seq` gol: `pierdute` a rămas 0
  după 30 de secunde;
- `INTARZIERE TOATE 500 200`: latența între 460 și 540 ms, fără mesaje
  recuperate sau pierdute.

Pe 2026-10-26, cu Mosquitto, verificările fazei 5, prin REST:
- `TUNEL masina-3 30` cerut prin `POST /api/retea/vreme`, apoi
  `LIMITEAZA_VITEZA` 50: comanda a rămas `TRIMISA` cât a ținut tunelul și a
  devenit `CONFIRMATA` la 6 secunde după ieșire; viteza a coborât 63,7 → 58,7 →
  53,7 → 50 și a rămas 50 în cele 120 de poziții de după;
- `/api/statistici`: 10 mașini; `/api/retea/stare`: 10 rânduri;
  `/api/retea/evenimente`: fiecare ordin, cu răspunsul `OK`;
- `INTARZIERE TOATE 500 200`: latența medie pe mașini între 436 și 572 ms,
  media 516; după `NORMAL`, 63 ms; contoarele au rămas 0;
- serverul revine la broker în 4 secunde după o pană de 35 de secunde;
- un `mosquitto_sub -p 1884 -i auto-1A2B` primește „identifier rejected” și nu
  apare în `STARE`.

UI-ul nu l-a văzut nimeni în browser pe 2026-10-26: doar build-ul a trecut.
