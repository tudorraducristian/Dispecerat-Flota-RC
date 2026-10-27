# flota-rc — redesign-ul UI-ului

*2026-10-27 · Owner: Tudor · Design: Claude, din brainstorming-ul cu Tudor*

Completează spec-ul proiectului, `2026-10-08-flota-rc-design.md`, la §7 (UI-ul) și
§16.4 (meniul și paginile noi). Deciziile noi continuă numerotarea de acolo, de
la D35.

**Completat tot pe 27 octombrie cu telefonul (§10, D45–D51).** Profesorul vrea
comenzi date de pe telefon către laptop, iar Tudor vrea pe telefon toată
aplicația.

---

## 1 · Intenția

Tudor a deschis UI-ul pe 27 octombrie și a spus că „designul e foarte urât”.
Ce l-a deranjat, în cuvintele lui și din capturile lui:

- bara albastră de sus, cu „Dispecerat flotă” scris banal;
- alerta portocalie din dreapta sus, care acoperă lista mașinilor;
- `masina-1` în loc de „Mașina 1”, `live` cu litere mici, `admin · ADMIN`;
- tipurile de alarmă scrise cu underscore, de exemplu `VITEZA_PESTE_LIMITA`;
- meniul, pagina Alarme și pagina Rețea, care arată ca Vuetify din cutie;
- cercurile de pe hartă, în loc de mașini.

Claude a mai găsit două probleme în capturi:

- **Tema e amestecată.** Vuetify ia tema întunecată din Chrome, așa că lista și
  paginile ies negre, iar bara și harta, deschise la culoare.
- **Meniul se deschide peste pagină.** Pe Conturi, el acoperea coloana „Nume”.

**De ce acum:** Task-urile 10–14 din faza 6 (figurile, articolul, PowerPoint-ul,
manualul și repetiția) au capturi din UI. Tudor a hotărât pe 26 octombrie ca ele
să vină după redesign, ca să nu se refacă.

**Cum arată succesul:** profesorul vede, de la login până la ultima pagină, o
aplicație care arată ca un produs: sobră, unitară, scrisă în română pentru
oameni, nu pentru cod.

## 2 · Ținte, non-ținte, anti-ținte

**Ținte:**

- Toate paginile primesc un singur stil, „A”: bleumarin, alb și albastru, cu
  fontul Inter.
- Peste tot apare textul pentru oameni: „Mașina 3”, „Viteză peste limită”,
  „Administrator”, „Mesaje pe secundă”.
- Alerta nu mai acoperă nimic important.
- Mașinile apar pe hartă ca mașini.
- Pe telefon merge toată aplicația, în browser, cu o așezare gândită pentru
  degetul mare (§10). Laptopul rămâne ecranul principal, la proiector, cu
  lățimea de lucru de la 1280 px în sus.

**Non-ținte:**

- Aplicația nativă de Android, scrisă în Android Studio. Profesorul acceptă
  browserul.
- Tableta și telefonul ținut pe orizontală, ca ținte proprii. Ele primesc
  așezarea care li se potrivește după lățime (§10.1), fără verificare separată.
- Tema întunecată.
- Funcții noi, în afara celor patru mici de la Alarme (§5).

**Anti-ținte,** adică un eșec chiar dacă tehnic merge:

- Prezentarea din spec §11 nu mai merge de la cap la coadă.
- Se schimbă comportamentul serverului, al proxy-ului sau protocolul MQTT. Aici
  se schimbă doar ce se vede, plus completarea de la §5 și originile
  WebSocket-ului de la §10.2.
- Aplicația cere internet pentru fonturi sau iconițe. Doar harta OpenStreetMap
  are nevoie de el, ca și acum.
- Pagina arată pe jumătate deschisă, pe jumătate închisă la culoare.
- Așezarea de telefon strică ceva pe laptop, la 1280 px sau mai mult.
- Pe telefon, un buton sau un text iese din ecran, iar pagina se poate trage
  într-o parte.

## 3 · Constrângeri

- **Stack-ul rămâne:** Vue 3.5, Vuetify 4.2, Leaflet 1.9, vue-router, Vite.
  Codul urmează stilul existent: Options API, nume și comentarii în română.
- **Graficul de latență rămâne desenat de mână în SVG,** fără bibliotecă (spec
  §7). Se schimbă doar stilul lui.
- **Componentele UI nu au teste automate** (spec §7).
  - Modulele pure din UI, adică textele, direcția de mers și stiva de alerte,
    primesc teste cu `node --test`, fără nicio dependență nouă. **Alegerea lui
    Claude,** la scrierea planului.
  - Partea nouă de server primește teste JUnit, ca restul serverului; cele 342
    de teste de acum trebuie să treacă în continuare.
- **Codul stă în** `Documents\Facultate\An 4\Semestrul 1\RC-Proiect\web\`, iar
  pentru §5 în `server\`.

## 4 · Ce a hotărât Tudor, ecran cu ecran

Machetele alese stau pe laptopul lui Tudor, în
`RC-Proiect\.superpowers\brainstorm\1220-1793098471\content\`. Sînt pagini HTML
care se deschid direct în browser, iar fișierul fiecărui ecran e numit mai jos.
Folderul `.superpowers\` nu e în git. **Machetele sînt sursa vizuală:** unde
textul de aici și macheta diferă, textul câștigă.

### 4.1 · Fundația vizuală (D35)

**Culorile,** ca token-uri în tema Vuetify și ca variabile CSS, nu ca hex
împrăștiat prin componente:

| Rol | Culoare | Unde |
|---|---|---|
| Bleumarin | `#0F172A` | bara, meniul, fundalul jurnalului, stânga login-ului |
| Bleumarin deschis | `#1E293B` | elementul activ din meniu, marginile pe fond închis |
| Fundalul paginii | `#F8FAFC` | în spatele cardurilor |
| Suprafață | `#FFFFFF` | carduri, liste, panouri |
| Margine | `#E2E8F0` | carduri, tabele, separatori |
| Text | `#0F172A`, secundar `#64748B`, slab `#94A3B8` | |
| Primar | `#2563EB`, text pe alb `#1D4ED8`, fond slab `#EFF6FF` | butoane, link-uri, selecție |
| Online | `#16A34A`, fond `#DCFCE7`, text `#166534` | mașini, „Rețea normală”, „Confirmată” |
| Rețea proastă | `#EA580C` | mașini cu vreme setată |
| Offline | `#94A3B8` | mașini |
| Roșu | `#DC2626`, fond `#FEE2E2`, text `#B91C1C` | alarme de mașină, tunel, pierdute, ștergere |
| Chihlimbar | `#D97706`, fond `#FEF3C7`, text `#B45309` | alarme de regulă, întârziere, „Așteaptă mașina” |
| Live | `#22C55E`, text `#4ADE80` pe bleumarin | punctul LIVE |

**Tema:**

- E o singură temă, deschisă la culoare, setată explicit. Vuetify nu mai urmează
  tema sistemului.
- Fără tema întunecată.

**Tipografia:**

- **Fontul e Inter,** instalat în proiect, de exemplu cu `@fontsource/inter`, nu
  de pe Google Fonts. Greutăți: 400, 500, 600 și 700.
- **Mărimile:**
  - titlul paginii: 22 px, greutatea 700;
  - titlul unui card: 15 px, 600;
  - textul obișnuit: 13–14 px;
  - etichetele mici: 11 px, cu majuscule, 600, cu spațiere de 0,06 em.
- **Cifrele din coloane și din contoare** au aceeași lățime (`tabular-nums`).
- **Jurnalul rețelei** e scris cu un font monospace, tot local.

**Iconițele:**

- Toate iconițele aplicației vin dintr-o singură familie, Lucide (de exemplu
  `lucide-vue-next`), ca în machete.
- Iconițele interne ale Vuetify, cum e săgeata din `v-select`, pot rămâne `mdi`.
- Fără emoji folosite ca iconițe.

**Formele:**

- Colțuri: 8 px la controale, 10–12 px la carduri și rotunjite complet la
  etichete.
- O singură umbră, pentru ce plutește: alerta și dialogurile.

**Textele,** din cod spre oameni:

| În cod | Pe ecran |
|---|---|
| `masina-7` | Mașina 7 |
| `ADMIN` / `DISPECER` | Administrator / Dispecer |
| `MASINA` / `SERVER` (sursa alarmei) | de la mașină / de la server |
| `TOATE` (ținta vremii) | Toată flota |
| zecimale | cu virgulă, după `ro-RO`: „110,8 °C” |
| „Mesaje/s” | Mesaje pe secundă |

Tipurile de alarmă, folosite în alertă, în pagina Alarme și în filtru:

| În cod | Numele | Propoziția | Iconița | Culoarea |
|---|---|---|---|---|
| `SUPRAINCALZIRE` | Supraîncălzire | Mașina 6 s-a supraîncălzit | termometru | roșu |
| `FRANARE_BRUSCA` | Frânare bruscă | Mașina 8 a frânat brusc | octogon cu semn de exclamare | roșu |
| `VITEZA_PESTE_LIMITA` | Viteză peste limită | Mașina 3 a depășit limita de viteză | vitezometru | chihlimbar |
| `IESIRE_DIN_ZONA` | Ieșire din zonă | Mașina 3 a ieșit din zona Centru* | pin tăiat | chihlimbar |
| `OFFLINE_PRELUNGIT` | Fără semnal | Mașina 4 nu mai trimite date | wifi tăiat | gri |

\* Numele zonei vine din detaliile alarmei. Dacă nu se poate citi, rămâne „a
ieșit din zonă”.

Detaliile alarmelor sînt scrise de server cu punct zecimal (`53.7 km/h`). UI-ul
le afișează cu virgulă, înlocuind punctul dintre două cifre. **Alegerea lui
Claude;** serverul rămâne neatins.

Stările vremii, din răspunsul proxy-ului la `STARE`:

| Proxy | Pe ecran | Culoarea |
|---|---|---|
| `NORMAL` | Normală | verde |
| `TUNEL 12` | Tunel · încă 12 s | roșu |
| `GAURA 4` | Gaură · încă 4 s | bleumarin |
| `INTARZIERE 500 200` | Întârziere 500 ± 200 ms | chihlimbar |
| `BANDA 64` | Bandă 64 kbps | chihlimbar |

Când proxy-ul trimite `INTARZIERE … BANDA …` împreună, apar două etichete.

### 4.2 · Bara de sus, varianta A (D36) — `bara-de-sus.html`

- **Fond și dimensiuni:** bleumarin, cu înălțimea de 56 px.
- **În stânga:**
  - butonul ☰, care strânge meniul;
  - un pătrat albastru cu iconița de traseu;
  - „Dispecerat flotă”, cu 17 px și greutatea 600, iar dedesubt, mai mic, gri,
    „Cluj-Napoca · 10 mașini”. Numărul vine din lista mașinilor.
- **În dreapta:**
  - **LIVE:** o etichetă cu majuscule, cu un punct verde care pulsează. Când pică
    legătura cu serverul, punctul se face roșu și scrie „FĂRĂ LEGĂTURĂ”. Pulsul se
    oprește dacă sistemul cere mișcare redusă.
  - **Utilizatorul:** un cerc cu inițiala, numele și, dedesubt, rolul în cuvinte.
  - **Ieșirea:** un buton-iconiță, cu tooltip și `aria-label` „Ieșire”.

### 4.3 · Meniul, varianta 1 (D37) — `meniul.html`

- **Locul:**
  - stă mereu lângă pagină, nu peste ea, cu lățimea de aproximativ 220–232 px;
  - e bleumarin, ca bara, așa că bara și meniul fac un „L” în jurul conținutului.
- **Grupurile:**
  - **„Operare”:** Harta, Rețea, Alarme;
  - **„Administrare”:** Setări, Conturi. Conturi apare doar pentru admin, ca
    acum.
- **Pagina curentă:** are un fond `#1E293B`, o linie albastră în stânga și
  iconița albastră.
- **La Alarme:** o cifră roșie arată câte alarme sînt nevăzute (§5). Peste 99
  scrie „99+”.
- **Jos:** „Proiect RC · UTCN 2026”, mic și gri.
- **Butonul ☰** îl ascunde de tot și îl readuce. Starea se ține minte în
  `localStorage`. **Alegerea lui Claude.**

### 4.4 · Alerta, varianta 1 (D38) — `alerta.html`

- **Locul:** un card alb, lat de cel mult 430 px, la 12 px sub bară. Nu
  acoperă meniul și nici panoul din dreapta hărții.
  - Pe Hartă, se centrează pe zona hărții, între meniu și panoul de 340 px.
    Centrat pe tot conținutul, la 1280 px ar intra cu 20–30 px peste panou.
  - Pe celelalte pagini, se centrează pe zona de conținut.
  - **Alegerea lui Claude,** după recenzie.
- **Conținutul:**
  - în stânga, iconița tipului, pe fondul culorii lui;
  - un titlu, care e propoziția din tabelul de la §4.1;
  - dedesubt, numele tipului și ora mașinii (`ora`);
  - în dreapta, „Vezi pe hartă” și un X.
- **Durata:**
  - jos, o linie în culoarea tipului se golește în 6 secunde, apoi cardul
    dispare;
  - când mouse-ul e pe card, timpul stă pe loc. **Alegerea lui Claude.**
- **Mai multe alerte deodată:**
  - se pun una sub alta, cea mai nouă sus, cel mult 3;
  - a patra o împinge afară pe cea mai veche.
  - Motiv: capturile arată 4 alarme în 3 secunde.
- **„Vezi pe hartă”:** duce la `/?masina=masina-10`, iar Harta deschide panoul
  mașinii respective. Adresa merge și scrisă de mână.
- **Accesibilitatea:** `aria-live="polite"`, iar alerta nu fură focusul.

### 4.5 · Harta

**Mașinile, varianta 1 (D39)** — `masini-pe-harta.html`:

- Fiecare mașină e desenată văzută de sus, în SVG, cu culoarea stării: online,
  rețea proastă sau offline.
- E rotită în direcția de mers. Direcția se calculează în UI, din poziția
  anterioară și cea nouă. Dacă mașina s-a mișcat sub circa 1 m, păstrează
  direcția de dinainte.
- Mașina selectată are un inel albastru în jur.
- Numele apare la hover, ca acum.
- Zonele rămân desenate în albastru, cu un fond slab.

**Lista flotei,** când nu e aleasă nicio mașină:

- Lista albă din dreapta, cu antetul „FLOTA · 9 DIN 10 ONLINE”.
- Pe fiecare rând: un punct colorat, „Mașina N” și viteza, sau „offline”.

**Panoul mașinii, varianta 1, cu file (D40)** — `panou-si-login.html`, sus:

- **Locul:** ia locul listei. „‹ Toată flota” și X te întorc la listă.
- **Antetul:**
  - mașina văzută de sus, pe un pătrat colorat;
  - „Mașina 2”, cu eticheta stării, de exemplu „Online”;
  - dedesubt, „zona Centru · văzută acum 1 s”.
- **Trei cifre:** viteza, temperatura și bateria, cea din urmă cu o bară sub
  cifră.
- **Rețeaua:** eticheta stării vremii, ca la §4.1, plus „14 recuperate · 0
  pierdute”.
- **Fila „Comenzi”:**
  - trei plăci: Limitează viteza, Întoarce la bază, Mesaj pentru șofer;
  - sub plăci, doar câmpul comenzii alese: km/h, între 10 și 130, sau textul, de
    cel mult 200 de caractere;
  - butonul „Trimite”;
  - „Ultimele comenzi”, fiecare cu starea în cuvinte: Confirmată (verde),
    Trimisă (albastru), Așteaptă mașina (chihlimbar), Respinsă și motivul (roșu).
    Logica stărilor rămâne cea de acum, din `PanouComenzi.vue`.
- **Fila „Traseu”:**
  - butoane rapide: „Live · N min”, unde N vine din setări, apoi „15 min”, „1
    oră” și „Alt interval…”;
  - „Alt interval…” deschide câmpurile „De la” / „Până la” și „Arată”;
  - dedesubt, legenda liniilor: plină pentru live, punctată pentru recuperat,
    gol pentru pierdut, desenate ca pe hartă.
  - 15 min și 1 oră sînt intervale istorice, cu `pana` = acum, fără poziții live
    adăugate. **Alegerea lui Claude.**

### 4.6 · Rețea, varianta 1 (D41) — `retea.html`

- **Antetul:** „Rețea”, iar dedesubt „Proxy disponibil · mașinile pe 1884,
  ordinele pe 1885”, cu un punct verde. Când proxy-ul nu răspunde, punctul e roșu
  și scrie „Proxy indisponibil”.
- **Stânga, panoul „Strică rețeaua”:**
  - **„Pentru”:** „Toată flota” sau „Mașina N”;
  - **patru plăci,** fiecare cu iconiță și o explicație de un rând:
    - Tunel: „Conexiunea cade, mașina ține mesajele”;
    - Gaură: „Mesajele se pierd pe drum”;
    - Întârziere: „Fiecare mesaj stă pe loc”;
    - Bandă îngustă: „Puțini kilobiți pe secundă”;
  - **câmpurile plăcii alese,** cu limitele de acum: secunde între 1 și 600,
    întârziere între 0 și 3000 ms, variație între 0 și 1000 ms, bandă între 1 și
    100.000 kbps;
  - **butonul plăcii,** de exemplu „Pornește tunelul”, în culoarea ei;
  - **răspunsul proxy-ului,** OK sau ERR, sub buton;
  - **separat,** un buton cu contur verde, „Rețea normală pentru toată flota”.
    Trimite `NORMAL` cu ținta `TOATE`, oricare ar fi ținta aleasă sus.
    **Alegerea lui Claude.**
- **Dreapta:**
  - **patru cifre mari pentru flotă:**
    - „Mesaje pe secundă”, suma pe mașini;
    - „Latența medie”, ultima valoare din `latentaFlotei`;
    - „Recuperate”, suma;
    - „Pierdute”, suma, cu roșu când e peste 0;
  - **graficul de latență,** cu o umplere slabă sub linie;
  - **un singur tabel pe mașină,** cu Mașina, Rețeaua (eticheta de la §4.1),
    Mesaje pe secundă, Latența, Recuperate și Pierdute. Înlocuiește cele două
    tabele de acum;
  - **jurnalul rețelei** pe fond bleumarin, cu font monospace: ora, linia brută
    trimisă proxy-ului și răspunsul, verde la OK și roșu la ERR. **Liniile rămân
    brute,** cu `masina-3`, fiindcă așa pleacă pe portul 1885.

### 4.7 · Alarme, varianta 2, fluxul de carduri (D42) — `alarme.html`

- **Antetul:**
  - „Alarme”, iar dedesubt „12 nevăzute · 148 azi”, cu numerele de la §5;
  - în dreapta, „Le-am văzut pe toate” și filtrul Nevăzute / Văzute / Toate.
    La Nevăzute apare și numărul.
- **Filtrul după tip:** etichete, de la „Toate tipurile” la cele cinci tipuri,
  fiecare cu punctul culorii lui.
- **Fiecare alarmă e un card:**
  - iconița tipului;
  - titlul, adică propoziția;
  - dedesubt, detaliile și sursa, de exemplu „… · de la server”;
  - în dreapta, ora mașinii și timpul scurs, de exemplu „acum 12 s”, care se
    actualizează singur;
  - „Vezi pe hartă” și un buton-bifă „Am văzut”.
- **Starea alarmei:** cele nevăzute au o linie albastră în stânga. Cele văzute
  sînt mai șterse și n-au bifă.
- **Grupele de timp:** „Ultimele 5 minute”, „Mai devreme azi”, „Ieri”, apoi data.
  **Alegerea lui Claude.**
- **Ce oră folosește fiecare:**
  - Ora afișată și „acum …” vin din ora mașinii (`ora`).
  - Grupele și ordinea vin din ora sosirii (`oraSosirii`), ca sortarea
    serverului.
  - Motiv: după un tunel, o alarmă de la mașină sosește cu minute de întârziere.
    Ea apare sus, ca nouă, dar arată când s-a întâmplat de fapt. Se vede la
    prezentare, la pasul 3.
  - **Alegerea lui Claude,** după recenzie.
- **Numerele rămân globale și cu filtrul după tip pus:** cele din antet și cifra
  din meniu. Când e ales un tip, numărul de lângă „Nevăzute” se ascunde, ca să nu
  pară că numără doar tipul acela. **Alegerea lui Claude,** după recenzie.
- **O alarmă nouă,** venită live, apare sus, ca acum.

### 4.8 · Setări — `setari-conturi.html`, sus

- **Antetul:** „Setări”, iar dedesubt „Contul tău și regulile flotei”.
- **Cardul contului:**
  - sus, inițiala, numele și rolul;
  - apoi „Schimbă parola”: Parola de acum, Parola nouă („Cel puțin 8
    caractere.”) și Parola nouă, încă o dată;
  - parolele au un ochi care arată ce ai scris.
- **Cardul „Flota”:**
  - „Se aplică pe loc, pentru toți dispecerii.”;
  - „Alarmă când o mașină e offline de” […] secunde, între 10 și 3600;
  - „Traseul din panoul mașinii arată ultimele” […] minute, între 1 și 360;
  - „Salvează”;
  - **pentru dispecer:** câmpurile sînt doar de citit, cu o casetă cu lacăt,
    „Doar administratorul le poate schimba.”, iar butonul nu apare.
- **Mesajele de reușită și de eroare** apar în card, ca acum, în stilul nou.

### 4.9 · Conturi — `setari-conturi.html`, mijloc și jos

- **Antetul:** „Conturi”, cu „N conturi” dedesubt, iar în dreapta butonul
  primar „+ Cont nou”.
- **Lista conturilor,** câte un rând pe cont:
  - inițiala, numele și eticheta „tu” pe rândul propriu;
  - rolul, ca etichetă: Administrator în albastru, Dispecer în gri;
  - „creat pe 26 oct. 2026, 13:57”;
  - „Resetează parola” și „Șterge”, cel din urmă cu roșu. Pe rândul propriu
    lipsesc, ca acum.
- **Fereastra „Cont nou”:**
  - Nume, cu ajutorul de acum, și Parolă, cu ochi;
  - Rolul se alege dintre două plăci:
    - „Dispecer”: „Harta, rețeaua, alarmele și comenzile.”;
    - „Administrator”: „În plus, conturile și setările flotei.”;
  - jos, „Renunță” și „Creează contul”;
  - sus, „Rolul nu se mai poate schimba după creare.”
- **Ferestrele de resetare și de ștergere** rămân cum sînt, în stilul nou.
  Ștergerea păstrează confirmarea și butonul roșu.

### 4.10 · Login, varianta 2 (D43) — `panou-si-login.html`, jos

- **Așezarea:** ecranul e împărțit în două.
- **Stânga, bleumarin,** cu o hartă stilizată estompată în fundal:
  - sus, logo-ul și „Dispecerat flotă”;
  - fraza mare: „Zece mașini, o rețea care se strică la comandă și un dispecerat
    care rezistă.”;
  - trei rânduri cu iconiță: „MQTT, cu QoS 0, 1 și 2”, „Proxy cu tunel, gaură,
    întârziere și bandă”, „Hartă live a Clujului”;
  - jos, „Proiect la Rețele de Calculatoare · UTCN · 2026”.
- **Dreapta, albă:**
  - „Intră în cont” și „Cu contul primit de la administrator.”;
  - câmpurile Nume și Parolă, cea din urmă cu ochi;
  - butonul primar „Intră”, pe toată lățimea;
  - mesajul de eroare apare deasupra butonului.
- **Ce rămâne:** comportamentul de acum, adică Enter, încărcarea și întoarcerea
  la pagina cerută.
- **Sub 1000 px lățime,** partea stângă dispare. **Alegerea lui Claude.**

## 5 · Completarea de pe server, pentru Alarme (D44)

Lista de alarme e tăiată la 200 de rânduri (`Alarme.lista`, `LIMIT 200`). De aceea
UI-ul nu poate număra singur alarmele nevăzute sau pe cele de azi. Tudor a
acceptat o completare mică pe server.

1. **`GET /api/alarme/numar`** întoarce `{ "nevazute": 12, "azi": 148 }`. „Azi”
   numără alarmele cu `ora_sosirii` de la miezul nopții, ora României.
2. **`POST /api/alarme/vazute`** marchează ca văzute toate alarmele nevăzute și
   întoarce câte a marcat. Dacă primește `?tip=…`, le marchează doar pe cele de
   tipul acela, ca butonul să urmeze filtrul.
3. **`GET /api/alarme?vazute=…&tip=…`:** se adaugă parametrul opțional `tip`.
   Limita de 200 rămâne.

Toate trei cer token, ca restul `/api`, și merg pentru ambele roluri: dispecerul
lucrează cu alarmele.

**Testele** sînt JUnit, în stilul celor existente, fără bază de test. Pom-ul a
scos-o intenționat.
- **Miezul nopții românesc** e o funcție pură, testată și peste schimbarea orei
  din 25 octombrie.
- **SQL-ul** e testat cu un `JdbcClient` simulat, ca în `IncarcareDateTest`.
- **Controller-ul** e testat cu `@WebMvcTest`.
- **SQL-ul pe MySQL adevărat** se verifică pe viu, prin REST (§8).

**Alegerea lui Claude,** după recenzie.

**Cifra din meniu se recalculează:** la pornire, la fiecare `ALARMA` live (+1),
după „Am văzut” și „Le-am văzut pe toate”, și o dată la 30 de secunde, pentru ce
au marcat alți dispeceri. **Alegerea lui Claude:** fără mesaj live nou pe
WebSocket.

## 6 · Alegerile lui Claude

Tudor le-a văzut în rezumatul din chat și nu le-a schimbat, dar nu le-a hotărât
el:

- **Biblioteci:** rămâne Vuetify, cu temă proprie și CSS propriu, fără altă
  bibliotecă de componente. Iconițele sînt Lucide; fonturile sînt locale.
- **Tema:** o singură temă, deschisă la culoare. Telefonul a intrat în scop
  după aceea, prin §10, și are lista lui de alegeri, la §10.9.
- **Meniul:** starea strâns sau deschis se ține minte.
- **Alerta:** se oprește pe loc sub mouse, iar cel mult 3 alerte stau una sub
  alta.
- **Pe Rețea:** „Rețea normală pentru toată flota” trimite mereu la `TOATE`.
- **În panoul mașinii:** 15 min și 1 oră sînt intervale istorice.
- **Alarme:** grupele de timp, iar recalcularea cifrei din meniu se face prin
  interogare.
- **Detaliile alarmelor:** punctul zecimal devine virgulă în UI.
- **Login-ul:** partea stângă dispare pe ecrane înguste.

## 7 · Ce rămâne la constructor

- Cum se împart componentele. Probabil o componentă pentru alertă, una pentru
  mașina SVG și una pentru eticheta de stare.
- Cum se pun token-urile: variabile în tema Vuetify, CSS global sau amândouă.
- Animațiile, între 150 și 300 ms, cu respect pentru mișcarea redusă.
- Starea goală a fiecărei liste, de exemplu „Nicio alarmă nevăzută.”, și starea
  de încărcare.
- Ce fac paginile la erori de rețea. Rămâne ce fac acum, în stilul nou.

## 8 · Verificarea

- `npm run build` trece, fără avertismente noi.
- `.\mvnw.cmd -q test` trece: cele 342 de teste de acum, plus cele de la §5.
- Pe viu, cu tot sistemul pornit după README, Claude verifică cererile noi de la
  §5 prin REST.
- Tudor parcurge în browser fiecare pagină, ca admin și ca dispecer, după pașii
  lui Claude. Verifică pe rând:
  - ecranul complet la 1920 × 1080 și la 1280 × 720;
  - alerta, cu mai multe alarme deodată;
  - „Vezi pe hartă”;
  - un tunel și o gaură pe Rețea, cu eticheta din tabel și cu linia punctată din
    traseu;
  - Setări ca dispecer.
- Scenariul de test din `RC-Proiect\docs\scenariu-test.md` merge în continuare.
- Telefonul se verifică după §10.8.

## 9 · Documentele care se aliniază

- **Spec-ul proiectului** primește deciziile D35–D51 în tabelul de la §2. D30,
  sertarul care se deschide peste pagină, e înlocuită de D37 pe laptop. Pe
  telefon, sertarul revine prin D47.
- **Tot în spec-ul proiectului:**
  - **Î10 se închide.** Profesorul vrea comenzi date de pe telefon către laptop,
    iar browserul îi ajunge.
  - **La §13,** „aplicația pe telefon” devine „aplicația nativă pe telefon”.
    Deploy-ul în afara laptopului rămâne în afara scopului.
- **README-ul** primește o secțiune „De pe telefon”, scrisă după §10.2.
- **În `docs\scenariu-prezentare.md`,** comenzile de rețea se dau de pe
  telefon, iar efectul se vede pe laptop, la proiector. **Alegerea lui Claude:**
  asta e scena pe care o cere profesorul.
- **Copiile din `RC-Proiect\docs\design\`** se actualizează după D27: spec-ul,
  acest design și planul.
- **În `docs\scenariu-prezentare.md` și în README,** textele vechi din UI devin
  cele noi:
  - „Normal” devine „Rețea normală pentru toată flota”;
  - „proxy indisponibil” devine „Proxy indisponibil”;
  - „fără legătură cu serverul” devine „FĂRĂ LEGĂTURĂ”.

## 10 · Telefonul (D45–D51)

**De ce:** profesorul vrea comenzi date de pe telefon către laptop. Nu a spus
care, iar browserul îi ajunge; aplicația instalată e opțională. Tudor vrea pe
telefon toată aplicația, nu doar comenzile, iar așezarea butoanelor e lăsată la
Claude. Pe asta se închide și Î10 (D45).

**Scena de la prezentare:** harta stă pe laptop, la proiector. Tudor ține
telefonul, alege „Mașina 3”, apasă „Pornește tunelul”, iar sala vede pe
proiector cum mașina intră în tunel și apoi își recuperează mesajele.

**Machetele** stau în
`RC-Proiect\.superpowers\brainstorm\1636-1793100939\content\`, cu aceeași regulă
ca la §4: unde textul și macheta diferă, textul câștigă.

| Fișierul | Ce arată | Ce a ales Tudor |
|---|---|---|
| `telefon-navigare.html` | trecerea între pagini | B, meniul ☰ |
| `telefon-harta.html` | panoul mașinii | A, foaia care urcă de jos |
| `telefon-retea.html` | pagina Rețea | B, trei file |
| `telefon-restul.html` | Alarme, Setări, Conturi, Cont nou, Login | aprobate, cu Alarme schimbată |
| `telefon-alarme-v2.html` | Alarme, cu ora mutată sus | aprobată |

### 10.1 · Când apare așezarea de telefon (alegerea lui Claude)

- **Sub 1000 px lățime,** paginile trec pe așezarea de telefon. Pragul e același
  cu cel de la Login (§4.10), ca aplicația să aibă un singur prag.
- De la 1000 px în sus rămâne așezarea de laptop de la §4, neschimbată.
- **Proiectorul.** Rezoluția lui nu e cunoscută. Un proiector de 1024 × 768 sau
  scalarea Windows de 150% coboară lățimea browserului sub 1280 px. De aceea,
  intervalul 1000–1279 px nu e frumos, dar trebuie să meargă: nimic nu iese din
  ecran, iar harta rămâne folosibilă, chiar dacă e îngustă. Se verifică la
  1024 × 768 (§10.8). Dacă la prezentare lățimea iese totuși sub 1000 px,
  soluția e scalarea Windows pusă pe 100%, nu o a treia așezare. **Alegerea lui
  Claude,** după recenzie.
- **Codul e unul singur.** Aceleași componente își schimbă așezarea după lățime,
  fără pagini separate pentru telefon și fără altă adresă.
- **Ținta de verificare** e un telefon Android ținut vertical, cu lățimea între
  360 și 430 px.
- **Reguli pentru toate paginile:**
  - nimic nu iese din ecran pe orizontală;
  - tot ce se apasă are o zonă de atingere de cel puțin 44 × 44 px, chiar dacă
    desenul e mai mic, ca bifa de 36 px de la Alarme;
  - câmpurile au textul de cel puțin 16 px, ca Chrome să nu mărească pagina la
    atingere;
  - nimic nu depinde de hover. Ce apare pe laptop la hover, cum e numele
    mașinii pe hartă, are pe telefon alt drum, descris la fiecare pagină.

### 10.2 · Rețeaua: telefonul ajunge la laptop (D46)

Acum totul ascultă pe `127.0.0.1`. Browserul vorbește doar cu Vite, iar Vite
trimite `/api` și `/ws` mai departe, la Spring Boot (`web\vite.config.js`).

- **Vite ascultă pe toate interfețele doar la cerere,** printr-un script nou,
  `npm run telefon`. `npm run dev` rămâne pe `127.0.0.1`, ca acum. Așa, pe
  Wi-Fi-ul facultății, laptopul nu se deschide spre rețea decât când e nevoie de
  telefon. Pe adresa laptopului din rețea, de exemplu
  `http://192.168.137.1:5173`, telefonul îl găsește. **Alegerea lui Claude,**
  după recenzie.
- **Spring Boot rămâne pe `127.0.0.1`.** Ajunge la el doar Vite, de pe același
  laptop. Așa, telefonul nu atinge niciodată direct serverul.
- **WebSocket-ul.** `ConfigWebSocket` acceptă azi doar originile
  `http://localhost:*` și `http://127.0.0.1:*`. Pagina deschisă de pe telefon
  vine cu originea adresei din rețea, așa că, fără o schimbare, harta n-ar
  primi date live. Serverul acceptă în plus originile din rețelele private:
  `http://192.168.*:*`, `http://10.*:*` și `http://172.16.*:*` până la
  `http://172.31.*:*`, nu tot `172.*`, care cuprinde și adrese publice.
  **Alegerea lui Claude.** E singura schimbare de server din §10 și are test
  JUnit: o origine privată trece, iar una publică e refuzată.
- **Restul nu se schimbă:** login-ul, token-ul și rolurile sînt aceleași pe
  telefon. Același cont poate fi logat deodată pe laptop și pe telefon, fiindcă
  serverul ține mai multe token-uri pe cont. Cine e în aceeași rețea, cât merge
  `npm run telefon`, poate vedea codul UI-ului servit de Vite, dar datele cer
  login.
- **Rețeaua de la prezentare:** laptopul se leagă la hotspot-ul telefonului, iar
  telefonul deschide adresa laptopului. Telefonul are date mobile, așa că și
  fundalul hărții se încarcă (R3).
- **README-ul, secțiunea „De pe telefon”:**
  - UI-ul se pornește cu `npm run telefon` în loc de `npm run dev`;
  - adresa o arată Vite la pornire, pe rândul „Network”;
  - prima dată, Windows întreabă dacă lasă Node.js în rețea, iar răspunsul e da,
    pentru rețele private;
  - dacă rețeaua hotspot-ului apare în Windows ca publică, firewall-ul blochează
    telefonul, iar profilul ei trebuie trecut pe „Privată”.

### 10.3 · Bara și meniul, varianta B (D47) — `telefon-navigare.html`

- **Bara de sus,** bleumarin, cu înălțimea de 52 px:
  - în stânga, butonul ☰;
  - lângă el, titlul paginii curente: „Harta”, „Rețea”, „Alarme”, „Setări”,
    „Conturi”;
  - în dreapta, eticheta LIVE sau „FĂRĂ LEGĂTURĂ”, ca la §4.2.
- **Punctul roșu de pe ☰** apare când există alarme nevăzute, fiindcă cifra din
  meniu se vede doar cu meniul deschis. **Alegerea lui Claude.**
- **Meniul e un sertar** care alunecă din stânga peste pagină, cu fundalul
  întunecat dedesubt. Lățimea lui e de aproximativ 78% din ecran, cel mult
  300 px.
  - **Sus:** logo-ul, „Dispecerat flotă” și „10 mașini”.
  - **Grupurile, ca pe laptop:** „Operare”, cu Harta, Rețea și Alarme, cu cifra
    roșie, apoi „Administrare”, cu Setări și Conturi. Conturi apare doar pentru
    admin.
  - **Pagina curentă** e marcată ca la §4.3.
  - **Jos:** inițiala, numele, rolul și butonul de ieșire. Pe telefon, ele nu
    mai stau în bară.
- **Sertarul se închide** când alegi o pagină, când atingi fundalul întunecat
  sau cu butonul Înapoi al telefonului.
- **Starea din `localStorage` de la §4.3** (meniu strâns sau deschis) privește
  doar laptopul. Pe telefon, sertarul pornește mereu închis.

### 10.4 · Harta, varianta A (D48) — `telefon-harta.html`

- **Harta ia tot ecranul de sub bară.** Mutarea se face cu degetul, iar zoom-ul
  cu două degete, cum le face deja Leaflet.
- **Foaia de jos** înlocuiește lista și panoul din dreapta de pe laptop. Are un
  mâner sus și trei înălțimi:
  - **strânsă:** se văd doar „FLOTA · 9 DIN 10 ONLINE” și primele rânduri ale
    listei;
  - **pe jumătate:** cea în care se deschide o mașină. Harta rămâne vizibilă
    deasupra;
  - **întinsă:** urcă aproape până sub bară, cu loc pentru comenzi sau traseu.
- **Foaia se trage** de mâner, în sus și în jos. O atingere pe mâner o duce la
  înălțimea următoare.
- **Lista flotei,** cu foaia strânsă sau trasă în sus: aceleași rânduri ca la
  §4.5. Atingi un rând sau o mașină de pe hartă, iar foaia trece pe mașina aceea,
  pe jumătate.
- **Mașina deschisă, pe jumătate:**
  - antetul de la §4.5: mașina pe pătratul colorat, „Mașina 2” cu eticheta,
    „zona Centru · văzută acum 1 s”, plus X, care te întoarce la listă;
  - cele trei cifre, pe un rând;
  - rețeaua, cu eticheta și „14 recuperate · 0 pierdute”;
  - filele „Comenzi” și „Traseu”.
- **Mașina deschisă, întinsă,** pe fila Comenzi:
  - cele trei plăci, pe un rând;
  - câmpul comenzii alese;
  - „Ultimele comenzi”;
  - **„Trimite”,** pe toată lățimea, lipit de marginea de jos a foii.
- **Fila Traseu, pe telefon:** butoanele rapide, câte două pe rând. „Alt
  interval…” deschide câmpurile unul sub altul, cu alegătorul de dată și oră al
  telefonului.
- **Harta urmează mașina:** când se deschide o mașină, harta o aduce în partea
  vizibilă de deasupra foii, nu sub ea.
- **Numele de la hover** nu există pe telefon. Atingerea deschide mașina, iar
  numele apare în foaie.
- **Alerta,** de la §4.4, pe telefon:
  - stă sus, sub bară, pe toată lățimea, cu 8 px de margine;
  - „Vezi pe hartă” se scurtează la „Vezi”;
  - timpul se oprește cât ții degetul pe ea, în locul mouse-ului;
  - rămân cel mult 3 alerte, una sub alta.

### 10.5 · Rețea, varianta B (D49) — `telefon-retea.html`

- **Sus,** sub bară, un comutator cu trei file: Comenzi, Flota, Jurnal. Pagina se
  deschide mereu pe Comenzi. **Alegerea lui Claude.**
- **Fila Comenzi** încape pe un ecran, fără derulare, la 360 px lățime:
  - „Proxy disponibil”, cu punctul verde, sau „Proxy indisponibil”, cu roșu.
    Porturile se văd doar pe laptop;
  - „Pentru”, cu „Toată flota” sau „Mașina N”;
  - cele patru plăci, două câte două, cu iconița și numele;
  - **explicația plăcii alese,** sub plăci, de exemplu „Tunel: conexiunea cade,
    mașina ține mesajele.” Pe laptop, explicația stă pe fiecare placă;
  - câmpurile plăcii alese, cu limitele de la §4.6;
  - **jos, sub degetul mare,** de sus în jos: răspunsul proxy-ului, butonul
    plăcii pe toată lățimea, de exemplu „Pornește tunelul” în roșu, apoi „Rețea
    normală pentru toată flota”, cu contur verde.
  - **Când nu încape,** de exemplu la Întârziere, care are două câmpuri, sau
    când apare tastatura: partea de sus se derulează, iar cele două butoane
    rămân lipite jos.
- **Fila Flota:**
  - cele patru cifre, două câte două;
  - graficul de latență, pe toată lățimea;
  - tabelul de pe laptop devine **câte un rând de două linii pe mașină:** sus,
    „Mașina 3” și eticheta rețelei; dedesubt, „0,0 mesaje/s · — · 14 recuperate
    · 0 pierdute”, cu „pierdute” în roșu când e peste 0.
- **Fila Jurnal:** jurnalul bleumarin, cu font monospace, pe toată înălțimea.
  Fiecare intrare are două rânduri: ora și linia brută, apoi, mai la dreapta,
  răspunsul, verde sau roșu.

### 10.6 · Restul paginilor (D50) — `telefon-restul.html`, `telefon-alarme-v2.html`

**Regula lui Claude pentru butoane,** aprobată de Tudor: butonul care duce
acțiunea la capăt stă jos, la degetul mare. Acțiunile rare sau periculoase stau
departe de deget sau ascunse după un meniu.

**Alarme:**

- **Sus:** „12 nevăzute · 148 azi”, iar în dreapta, „Le-am văzut pe toate”,
  departe de deget, ca să nu se apese din greșeală.
- **Dedesubt:** filtrul Nevăzute / Văzute / Toate, pe toată lățimea, cu numărul
  la Nevăzute, după regula de la §4.7.
- **Tipurile:** un rând de etichete care se derulează pe orizontală.
- **Cardul alarmei,** de sus în jos:
  - **ora mașinii și timpul scurs, pe primul rând,** de exemplu „14:32 · acum
    12 s”. Pe laptop, ora stă în dreapta. Pe telefon, acolo se tăia, iar Tudor a
    cerut-o întreagă, sus;
  - titlul, adică propoziția;
  - detaliile și sursa;
  - „Vezi pe hartă”.
- **Bifa „Am văzut”:** un buton de 36 px, în dreapta cardului, centrat pe
  verticală.
- **Grupele de timp, linia albastră și starea alarmelor văzute** rămân cele de
  la §4.7.

**Setări:**

- Cardurile stau unul sub altul, pe toată lățimea.
- Câmpurile au toată lățimea cardului.
- Fiecare card are butonul lui la capăt, pe toată lățimea: „Schimbă parola”,
  respectiv „Salvează”.
- Regula pentru dispecer, cu lacătul, rămâne cea de la §4.8.

**Conturi:**

- **Sus:** „N conturi”.
- **Pe fiecare rând:** inițiala, numele, cu „tu” pe rândul propriu, apoi, pe al
  doilea rând, eticheta rolului și data creării, scurtă: „26 oct. 2026”.
- **„Resetează parola” și „Șterge contul”** stau după un buton ⋮, pe fiecare
  rând. „Șterge contul” e roșu și păstrează confirmarea. Pe rândul propriu, ⋮
  lipsește.
- **„Cont nou”** e un buton care plutește jos, în dreapta. Lista are loc liber
  dedesubt, ca butonul să nu acopere ultimul rând.

**Cont nou:**

- Se deschide pe tot ecranul, ca tastatura să nu acopere câmpurile.
- **Sus:** X și „Cont nou”, apoi avertismentul „Rolul nu se mai poate schimba
  după creare.”
- **Câmpurile:** Nume și Parolă, apoi cele două plăci de rol, una lângă alta.
- **Jos:** „Renunță” și „Creează contul”.
- **Ferestrele de resetare și de ștergere** rămân ferestre mici, pe lățimea
  ecranului, cu 16 px de margine.

**Login:**

- **Sus,** o bandă bleumarin: logo-ul, „Dispecerat flotă” și fraza mare.
- **Dedesubt, pe alb:** „Intră în cont”, câmpurile, „Intră” pe toată lățimea și,
  jos, „Proiect la Rețele de Calculatoare · UTCN · 2026”.
- **Cele trei rânduri cu iconițe** apar doar pe laptop.
- Banda înlocuiește, sub 1000 px, regula de la §4.10, unde partea stângă
  dispărea de tot.

### 10.7 · Pe ecranul principal al telefonului (D51)

Profesorul a spus că aplicația instalată e opțională. Ideea lui Claude a fost ca
pagina să se poată adăuga pe ecranul principal, cu iconiță, și să se deschidă
fără bara browserului.

- **Limita:** Chrome deschide fără bara browserului doar paginile servite prin
  HTTPS sau de pe `localhost`. Adresa din rețea, `http://192.168…`, nu e
  niciuna.
- **Tudor a ales doar iconița** (varianta „a”), pe 27 octombrie:
  - pagina primește un manifest, cu numele „Dispecerat flotă”, culoarea
    bleumarin și o iconiță cu traseul;
  - primește și `theme-color` bleumarin, ca bara de stare a telefonului să se
    potrivească;
  - adăugată pe ecranul principal, pagina se deschide ca o filă de Chrome, cu
    bara de adrese.
- **Respinse:**
  - setarea din `chrome://flags` a telefonului, fiindcă adresa laptopului pe
    hotspot se poate schimba;
  - HTTPS, fiindcă un certificat refuzat ar strica prezentarea, iar TLS e în
    afara scopului (spec §13).

### 10.8 · Verificarea pe telefon

Se adaugă la §8:

- **Pe laptop, în modul telefon din DevTools,** la 360 × 800 și la 412 × 915,
  ca admin și ca dispecer: fiecare pagină, fără derulare pe orizontală și fără
  nimic ieșit din ecran. O face Tudor, după pașii lui Claude, fiindcă sesiunea
  din VS Code n-are browser (decizia din 26 octombrie).
- **Tudor, pe telefonul lui Android,** cu laptopul pe hotspot, după pașii lui
  Claude:
  - login-ul, de pe adresa din rețea;
  - harta, cu mașinile mișcându-se live, ceea ce dovedește că merge
    WebSocket-ul;
  - o mașină deschisă, foaia trasă în sus și o comandă „Limitează viteza”;
  - pe Rețea, un tunel pornit de pe telefon, văzut pe laptop, apoi „Rețea
    normală”;
  - o alarmă marcată ca văzută de pe telefon, cu cifra din meniul laptopului
    scăzută în cel mult 30 de secunde;
  - sertarul, cu ieșirea din cont;
  - Setări, Conturi și „Cont nou”, ca admin.
- **Laptopul,** după toate astea: ecranul complet la 1280 × 720 arată ca
  înainte de §10, iar la 1024 × 768 nimic nu iese din ecran.

**Capturile pentru documente** (faza 6, Task-urile 10–14): manualul primește și
o secțiune „De pe telefon”, cu capturi din Harta, Rețea și Alarme pe telefon. Un
slide din PowerPoint arată telefonul lângă laptop. De aceea, documentele vin după
tot planul redesign-ului, cu telefonul inclus, nu doar după laptop. **Alegerea
lui Claude,** după recenzie.

### 10.9 · Alegerile lui Claude, pentru telefon

Tudor a ales navigarea, foaia, filele de la Rețea și ora de la Alarme și a
aprobat restul ecranelor. Restul sînt alegerile lui Claude:

- un singur prag, de 1000 px, iar 1000–1279 px trebuie doar să meargă;
- zona de atingere de cel puțin 44 px și textul de 16 px în câmpuri;
- Vite se deschide spre rețea doar cu `npm run telefon`;
- serverul acceptă originile din rețelele private, iar Spring Boot rămâne pe
  `127.0.0.1`;
- manualul și PowerPoint-ul au și capturi de pe telefon;
- punctul roșu de pe ☰;
- sertarul pornește mereu închis pe telefon;
- foaia de pe hartă are trei înălțimi;
- Rețea se deschide mereu pe Comenzi, iar explicația plăcii stă sub plăci;
- în scenariul de prezentare, comenzile de rețea se dau de pe telefon.

### 10.10 · Ce rămâne la constructor, pentru telefon

- **Cum se detectează lățimea:** `useDisplay` din Vuetify, `@media` în CSS sau
  amândouă, cu condiția să existe un singur prag, de 1000 px.
- **Foaia de pe hartă:** se poate scrie de mână, cu evenimente de atingere, sau
  pe o componentă Vuetify, dacă aceasta știe trasul între înălțimi.
- **Butonul Înapoi al telefonului:** închide sertarul. Dacă îl face ușor, închide
  și foaia sau fereastra „Cont nou”.

## 10b · Schimbări hotărâte de Tudor la execuție (D52, D53)

Le-a cerut Tudor pe 27 octombrie, la punctele de oprire ale planului. Unde
contrazic textul de mai sus, ele câștigă.

- **D52 · Coloana din dreapta a Hărții e bleumarin.** Lista mașinilor și panoul
  mașinii stau pe bleumarin, cu text deschis, ca meniul (ales din trei
  variante). Rândul ales din listă arată ca elementul activ din meniu. Pe
  telefon, foaia de jos e la fel. Schimbă §4.5, unde coloana era albă.
- **D53 · Pe telefon, o singură alertă odată.** Trei alerte umpleau ecranul. Pe
  laptop rămân cel mult trei (§4.4). La trecerea pragului de 1000 px, lista se
  taie la numărul nou.

## 11 · Ce urmează după

Faza 6, Task-urile 10–14, cu capturile făcute din UI-ul nou (D33 din spec, cu
ordinea hotărâtă pe 26 octombrie).
