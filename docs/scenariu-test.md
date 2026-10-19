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
- viteza lui `masina-3` nu mai trece de 50.

### 6. `INTARZIERE TOATE 500 200`

- **Din faza 3:**
  - mașinile se mișcă pe hartă cu o jumătate de secundă în urmă;
  - `latenta_ms` urcă la vreo 500;
  - `din_buffer` nu primește rânduri noi.
- **Din faza 5:** graficul de latență din pagina Rețea urcă la vreo 500.

La final, `NORMAL TOATE`.

## Măsurători

Se completează în faza 4 (riscul R6) și se folosesc în scenariul prezentării.

| Ce | Valoarea măsurată | Data |
|---|---|---|
| `GAURA masina-5 10`: secunde până devine gri | | |
| `GAURA masina-5 10`: mesaje pierdute | | |
| `GAURA masina-5 10`: mesaje recuperate | | |
| `TUNEL masina-3 15`: secunde până revine verde | | |
| `GAURA masina-5 N`: cea mai lungă gaură fără pierderi | | |
