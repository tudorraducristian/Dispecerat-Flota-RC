# Prezentarea: 8 minute, cu demonstrația în 5

După spec §11 și §16.6. Cifrele vin din tabelul „Măsurători” din `scenariu-test.md`.

**Ordinea:** slide-urile 1–5 din `docs\prezentare\flota-rc.pptx`, apoi
demonstrația (slide-ul 6 și pașii 1–6 de mai jos), apoi slide-urile 7–10.
**Ținta (D34):** demonstrația sub 5 minute, totul sub 8, până răspunde
profesorul cu data și durata (Î2). Textul de spus la fiecare slide stă în notele
vorbitorului din PowerPoint.

**Scena:** harta stă pe laptop, la proiector. Comenzile de rețea le dai de pe
telefon, din pagina Rețea, iar sala vede efectul pe laptop (redesign §10).

## Cu o zi înainte

- Repetiția completă, cu cronometrul: slide-urile și toți cei șase pași, sub 8
  minute, cu demonstrația sub 5.
- Contul `dispecer1` există: dacă nu, îl faci ca `admin`, din Conturi, cu rolul
  Dispecer.
- Hotspot-ul telefonului încercat pe laptop. Harta are nevoie de internet (R3).
- Telefonul încercat pe adresa laptopului, după „De pe telefon” din `README.md`.

## Cu 15 minute înainte

1. Laptopul pe hotspot-ul telefonului.
2. Serviciul MySQL pornit, apoi terminalele A, P, S, W, C și O, după `README.md`.
   W pornește cu `npm run telefon`, nu cu `npm run dev`.
3. Curățenie, în D:

   ```powershell
   Sql "UPDATE masini SET pierdute = 0, recuperate = 0"
   Sql "UPDATE alarme SET vazuta = TRUE"
   ```

4. Pe laptop, în browser, login ca `dispecer1` în tabul 1, apoi:
   - tabul 1: Harta, mărită pe zonă, cu Mașina 3 aleasă;
   - tabul 2: Rețea.
5. PowerPoint-ul deschis la slide-ul 1, `docs\prezentare\flota-rc.pptx`, cu
   vederea prezentatorului, ca notele să se vadă doar pe laptop.
6. Pe telefon, în Chrome: adresa de pe rândul `Network` al lui W, login, apoi
   Rețea, fila Comenzi.
7. În O, `STARE`: zece rânduri `NORMAL`.

## Slide-urile 1–5, vreo 2 minute

Titlul, problema, arhitectura, MQTT cu QoS-ul și proxy-ul cu „vremea”. La
slide-ul 6, „Demonstrație live”, treci pe browser.

## Demonstrația, sub 5 minute

### 1. Zece mașini live, 30 de secunde

**Arăți:** harta, lista „FLOTA · 10 DIN 10 ONLINE”, apoi panoul Mașinii 3.

**Spui:** „Zece mașini simulate trimit telemetria prin MQTT, o dată pe secundă.
Între ele și broker stă un proxy scris de mine, care strică rețeaua la comandă.”

### 2. Tunelul, 60 de secunde

**Faci:** pe telefon, „Pentru” Mașina 3, placa Tunel, 15 secunde, apoi
„Pornește tunelul”. Rezerva, dacă telefonul nu merge: în O, `TUNEL masina-3 15`.

**Se vede, pe laptop:**
- Mașina 3 devine gri pe loc;
- după vreo 19 secunde e din nou verde;
- porțiunea din tunel e punctată, iar „Recuperate” crește.

**Spui:**
- mașina nu știe de tunel, vede doar că i-a murit conexiunea;
- cât e offline, motorul pune mesajele în buffer, cu `din_buffer`;
- radioul reîncearcă după 1, 2, 4 secunde, cu o abatere la întâmplare;
- gri a venit din Last Will-ul publicat de broker.

### 3. Gaura neagră, 75 de secunde

**Faci:** pe telefon, „Pentru” Mașina 5, placa Gaură, 10 secunde, apoi
„Deschide gaura”. Rezerva: în O, `GAURA masina-5 10`.

**Se vede, pe laptop:**
- Mașina 5 rămâne verde câteva secunde, deși nu mai trece nimic: 6,6 măsurat
  cu Mosquitto (5,2 cu HiveMQ CE);
- apoi devine gri, iar la revenire pe hartă e o porțiune lipsă, urmată de una
  punctată;
- pe Rețea, „Pierdute” crește cu vreo 7.

**Spui:**
- conexiunea e deschisă, dar mută. Keepalive-ul de 5 secunde o descoperă:
  broker-ul închide după 1,5 × 5 = 7,5 secunde fără niciun pachet. Le numără de
  la ultimul pachet primit, care a plecat cu până la o secundă înainte de gaură,
  iar Mosquitto numără pe secunde întregi, deci în practică ies 6–7 secunde;
- telemetria e pe QoS 0, deci ce era pe drum s-a pierdut. Alarmele sînt pe
  QoS 2 și ajung după reconectare, o singură dată;
- o gaură de 6 secunde nu pierde nimic, doar întârzie; de la 7 secunde, broker-ul
  închide conexiunea și se pierde ce era pe drum.

### 4. O comandă dată în tunel, 60 de secunde

**Faci:**
1. pe telefon, tunel pe Mașina 3, 30 de secunde;
2. pe laptop, în panoul Mașinii 3, fila Comenzi, „Limitează viteza”, cu 50.

**Se vede:** „Așteaptă mașina”, apoi, la ieșirea din tunel, „Confirmată”.

**Spui:**
- sesiunea persistentă (`cleanSession false`) face ca broker-ul să păstreze
  comanda;
- QoS 1 spune „livrat la broker”, iar confirmarea spune „executat”;
- aceeași confirmare primită de două ori se numără o dată.

### 5. Protocolul, așa cum vine, 30 de secunde

**Faci:** în O, `STARE`.

**Spui:**
- management out-of-band: ordinele merg pe portul 1885, nu pe drumul pe care îl
  strică;
- protocolul e text, deci se poate folosi de mână;
- rândul cu un singur punct spune unde se termină răspunsul, ca la SMTP.

### 6. Latența, 45 de secunde

**Faci:** pe telefon, „Pentru” Toată flota, placa Întârziere, 500 ms și
variația 200, apoi „Aplică întârzierea”.

**Se vede, pe laptop,** în tabul Rețea: graficul urcă la vreo 500 ms; pe Hartă,
mașinile devin portocalii.

**Spui:**
- fiecare mesaj are ora mașinii și ora sosirii, iar latența e diferența dintre
  ele;
- aici ceasul e același, fiindcă totul rulează pe laptop. Pe mașini reale,
  ceasurile s-ar sincroniza prin NTP;
- variația nu schimbă ordinea octeților, fiindcă pe TCP ordinea e garantată.

La final, pe telefon, „Rețea normală pentru toată flota”.

## Slide-urile 7–10, vreo 1,5 minute

Înapoi în PowerPoint, la slide-ul 7: rezultatele măsurate, login-ul și limitele
lui, ce n-am făcut, concluziile. Slide-ul 10 rămâne pe ecran la întrebări.

## Dacă ceva merge prost

| Ce vezi | Ce faci |
|---|---|
| Harta e goală, fără străzi | Hotspot-ul. Fără el, mașinile și traseele se văd pe fond gol (R3) |
| „Proxy indisponibil” pe Rețea | P nu rulează: îl pornești; mașinile revin în cel mult 30 de secunde |
| Telefonul nu deschide adresa laptopului | W e pornit cu `npm run dev` în loc de `npm run telefon`, sau rețeaua hotspot-ului e „Publică” în Windows: „De pe telefon” din `README.md`. Între timp, comenzile din O |
| O mașină rămâne gri după tunel peste 40 de secunde | În C, ultimele rânduri ale mașinii; dacă C a căzut, îl pornești |
| „FĂRĂ LEGĂTURĂ” în bara de sus | S nu rulează: îl pornești; UI-ul se reconectează singur |
| În S, „Deconectat de la Mosquitto”, iar toate mașinile sînt gri | A nu rulează: îl pornești; dacă Mosquitto nu pornește, HiveMQ CE, după `README.md`. Serverul revine în cel mult 10 secunde, mașinile în cel mult 30 |

## Întrebări probabile

- **De ce MQTT și nu HTTP?** Mașina trimite des și puțin, iar conexiunea rămâne
  deschisă; QoS, Last Will și sesiunea persistentă vin gata făcute.
- **De ce proxy-ul nu pierde pachete?** Pe TCP, un octet lipsă ar strica fluxul,
  iar MQTT ar primi mesaje fără sens. Tunelul și gaura arată pierderea reală, la
  nivel de conexiune.
- **Ce se întâmplă dacă bufferul se umple?** Se aruncă telemetria cea mai veche;
  alarmele și confirmările rămân. Serverul le numără ca pierdute.
- **De ce serverul nu trece prin proxy?** Dispecerul nu stă în tunel: ar intra
  și el în tunel și n-ar mai vedea nimic.
- **De ce token și nu sesiune pe server?** Același token merge și la REST, și la
  WebSocket, unde browserul nu poate pune cookie-uri sau header-e la fel de
  simplu; serverul îl ține în tabelul `tokenuri`, deci log off-ul îl șterge și
  închide pe loc canalele deschise cu el.
- **Ce vede cineva care ascultă rețeaua?** Totul: HTTP-ul e necriptat, deci
  parola la login, token-ul la fiecare cerere și în adresa WebSocket-ului, plus
  mesajele MQTT. TLS, adică HTTPS și MQTT peste TLS, ar ascunde tot; a rămas în
  afara scopului (spec §13).
- **Ce ar trebui pentru mașini reale?** TLS, autentificare, NTP pentru ceasuri
  și un broker în cloud. Proiectul a lăsat asta deoparte (spec §13).
