# Flotă de mașini conectate: proiectul de Rețele de Calculatoare — design

*Scris: 2026-10-08 · Owner: Tudor · Stare: aprobat în discuție, așteaptă review-ul
specului · Completat pe 2026-10-19 cu faza 6, după grila profesorului (§16)*

---

## 1 · Contextul

Proiectul e la **Rețele de Calculatoare**, în anul 4, semestrul 1. Cerința, așa
cum a spus-o Tudor pe 8 octombrie 2026: o aplicație client-server cu UI, pe o
temă IoT. Ajunge partea de software, hardware nu trebuie. Profesorul acceptă
orice tehnologie de rețea.

Ce a mai hotărât Tudor în brainstorming:

- **Mărimea e ambițioasă:** notă maximă și învățare serioasă.
- **Termenul e înainte de vacanța de iarnă,** deci vreo 10 săptămâni, împărțite
  cu proiectul de SCD, AI Study Hub. Data exactă a vacanței nu e știută.
- **Lucrează singur.** E o presupunere spusă în discuție, pe care Tudor n-a
  corectat-o.

Ce e pe disc la 8 octombrie 2026, verificat în sesiune:

- **Folderul pentru cod** există și e gol:
  `C:\Users\40747\OneDrive\Documents\Facultate\An 4\Semestrul 1\RC-Proiect\`.
- **Materiale de RC nu există** nicăieri în `Documents\Facultate`.
- **SCD Lab 1** are un server multifir în Java cu socket-uri, `ServerMultifir.java`,
  `TratareClient.java` și `ClientMultiserver.java`, în
  `Facultate\An 4\Semestrul 1\SCD-Laborator\Laborator 1\`. E tiparul de pornire
  pentru proxy.
- **Ce e instalat**, după sesiunea SCD din 3 octombrie: Java 21, Node 24, Git și
  VS Code. MySQL lipsește și se instalează în faza 0 de la SCD. Maven lipsește,
  iar lipsa se rezolvă cu `mvnw`. Mosquitto nu e instalat.

**Ideea, într-o frază.** Zece mașini simulate raportează prin MQTT la un
dispecerat. Între mașini și broker stă un proxy care strică rețeaua la comandă,
iar dispeceratul arată live, pe hartă, cum supraviețuiește sistemul.

**Analogia.** E un dispecerat de taxiuri:

- **Mașinile** sînt taxiurile, care raportează prin stația radio.
- **Mosquitto** e stația radio, iar topic-urile sînt canalele ei.
- **Serverul** e dispecerul: ascultă tot, ține registrul, dă alarma și trimite
  ordine.
- **UI-ul** e ecranul dispecerului.
- **Proxy-ul** e vremea și relieful dintre taxi și antenă, pe care le controlezi
  de la un buton.

## 2 · Deciziile

| # | Decizia | Alternativele respinse |
|---|---|---|
| D1 | **Tema e o flotă de mașini conectate.** Pierderea semnalului în tunel face din problemele de rețea chiar subiectul proiectului | o sală de fitness, unde nimic nu se deconectează natural; o seră, mai puțin spectaculoasă la prezentare; o casă inteligentă, tema cea mai aleasă, deci ușor de confundat cu alte proiecte |
| D2 | **Protocolul principal e MQTT 3.1.1, prin broker-ul Mosquitto** | HTTP cu interogări repetate, care nu e tipic pentru IoT; MQTT 5, mai puțin documentat și cu mai multe de explicat; un broker scris de mână, prea scump în timp, alături de SCD |
| D3 | **Java peste tot (varianta A):** simulatorul, proxy-ul și serverul sînt în Java 21, iar UI-ul în Vue 3. Totul se scrie în VS Code | B: simulatorul și proxy-ul în Python, deci două limbaje, iar partea în Python nu ajută la SCD; C: fără server propriu, cu browserul abonat direct la Mosquitto, care face cerința client-server subțire și lasă istoricul și alertele fără loc |
| D4 | **Singura piesă peste bază e proxy-ul de rețea proastă** | comparația măsurată de protocoale; TLS; broker-ul scris de mână; descoperirea pe UDP, care nu se potrivește la mașini, fiindcă ele se conectează prin rețeaua mobilă la o adresă cunoscută |
| D5 | **Proxy-ul nu pierde pachete.** Simulează întârziere, bandă îngustă, tunel și gaură neagră | pierderea de octeți în proxy, care strică fluxul TCP și face MQTT să primească mesaje fără sens; unelte de sistem ca `clumsy` sau `netem`, care cer o descărcare sau Linux și nu se controlează din UI |
| D6 | **Ordinele pentru proxy vin printr-un protocol text scris de Tudor, pe un port separat** (management out-of-band) | ordinele prin MQTT: mai puțin cod, dar fără lecția despre protocoale și cu controlul pe același drum pe care îl strică |
| D7 | **QoS diferit pe tip de mesaj:** telemetria pe 0, alarma pe 2, iar starea, comenzile și confirmările pe 1 | același QoS peste tot, care ascunde de ce există cele trei niveluri |
| D8 | **Bufferul și reconectarea mașinii le scriem noi.** Reconectarea automată a clientului MQTT rămâne oprită | reconectarea automată a bibliotecii, care ar ascunde exact lecția |
| D9 | **Două fire de execuție pe mașină: motorul și radioul.** Bufferul e coada dintre ele | un singur fir, care s-ar opri până la 5 secunde la fiecare încercare de conectare, deci ar sări poziții |
| D10 | **Telemetria are un număr de ordine și un semn `din_buffer`, pus de mașină** | „recuperat" dedus din diferența dintre ore, care s-ar încurca la `INTARZIERE`: un mesaj live întârziat 3 secunde ar arăta ca unul recuperat |
| D11 | **Serverul se conectează direct la Mosquitto,** cu sesiune persistentă și cu reconectarea automată a bibliotecii | serverul prin proxy, caz în care dispecerul ar intra și el în tunel |
| D12 | **Alarmele se dau la schimbare, nu la fiecare mesaj:** o alarmă când mașina iese din zonă, nu una pe secundă cât stă afară | o alarmă la fiecare mesaj care încalcă regula |
| D13 | **Zonele vin dintr-un fișier JSON și doar se afișează** | un editor de zone pe hartă, adică muncă de interfață, nu de rețele |
| D14 | ~~**Fără login.** E un singur dispecer~~ **Răsturnată pe 19 octombrie:** grila profesorului punctează login-ul și meniul cu conturi. Vezi D28 | autentificarea ca la SCD, care nu e subiectul la Rețele |
| D15 | **Codul stă în `Facultate\An 4\Semestrul 1\RC-Proiect\`,** cu git. În AIS-OS proiectul se numește `flota-rc`. Remote-ul de pe GitHub a venit totuși, pe 8 octombrie: vezi D27 | codul în AIS-OS, care încalcă regula mamei |
| D16 | **Metoda de lucru e cea mixtă de la SCD** (§9) | Claude scrie tot, care contrazice ținta de învățare; Tudor scrie tot, prea lent pentru 10 săptămâni |
| D17 | **Clientul MQTT e HiveMQ MQTT Client 1.4.0,** atât în simulator, cât și în server. Adăugată pe 8 octombrie, la scrierea planului | Eclipse Paho 1.2.5: ultima versiune e din iulie 2020, verificat pe Maven Central, deci riscul R4 s-a confirmat |
| D18 | **Traseele și zonele sînt fișiere GeoJSON,** desenate în geojson.io. Atenție: GeoJSON scrie coordonatele ca `[lon, lat]`. Adăugată pe 8 octombrie | o listă proprie de `[lat, lon]`, care ar trebui convertită de mână din ce se desenează; GeoJSON se desenează direct pe hartă, iar Leaflet îl citește fără conversie |
| D19 | **Serverul vorbește cu MySQL prin `JdbcClient`, cu SQL scris de mână.** Schema stă în `schema.sql`. Contoarele și idempotența confirmărilor sînt `UPDATE`-uri condiționate, vizibile în cod. Adăugată pe 8 octombrie, la planul fazei 2 | JPA cu entități, ca la SCD, care ar ascunde exact aceste `UPDATE`-uri; SCD predă deja JPA |
| D20 | **În server, Netty e fixat la `4.1.137.Final`.** Boot 4.1.1 aduce Netty 4.2.17, iar HiveMQ 1.4.0 e construit pe 4.1.137, verificat pe Maven Central. Serverul nu folosește Netty în altă parte. Adăugată pe 8 octombrie | Netty 4.2, adus de Boot, sub o bibliotecă necompilată pe el |
| D21 | **UI-ul:** Vuetify 4.2.4, Leaflet 1.9.4, Vite 8.3.3 și, din faza 5, vue-router 5.4.0. Componentele se scriu cu `data()` și `methods`, ca la SCD. Cererile merg cu `fetch`, printr-un singur `api.js`. Adăugată pe 8 octombrie | Leaflet 2.0, încă alpha; Axios, o dependență în plus pentru câteva cereri |
| D22 | **Serverul primește mesajele printr-un filtru global, pe un singur fir, și se abonează din nou la fiecare conectare.** Filtrul prinde mesajele din sesiunea persistentă, care vin imediat după `CONNACK`. Firul unic păstrează ordinea. Mosquitto fără persistență uită abonările la repornire. Simulatorul face la fel cu comenzile. Adăugată pe 8 octombrie | abonarea cu callback, care pierde mesajele sosite înaintea ei; mai multe fire, care s-ar bate pe aceeași mașină |
| D23 | **`ultima_vedere` se actualizează la fiecare telemetrie și la fiecare schimbare de stare.** `GET /api/masini` întoarce și ultima poziție, ca o mașină offline să apară gri unde a fost văzută ultima dată. Adăugată pe 8 octombrie | ultima poziție cerută separat, câte o cerere pe mașină |
| D24 | **Protocolul text:** linia de peste 200 de caractere primește `ERR linie prea lunga`, iar linia goală se ignoră. Un tunel și o gaură nu stau unul peste altul, ci ultimul îl înlocuiește pe celălalt. `STARE` scrie întârzierea mereu cu variația. Ordinele se dau din `unelte\ordine.ps1`. Adăugată pe 8 octombrie, la planul fazei 3 | telnet, care cere o setare de sistem; un client Java, mai lent de pornit |
| D25 | **Reziliența:** `ultimul_seq` gol înseamnă „nicio telemetrie văzută”, deci primul mesaj nu numără pierderi. O conectare fără `online` se desface și se reîncearcă. Mărimea bufferului e `buffer.marime` în `simulator.properties`. Adăugată pe 8 octombrie, la planul fazei 4 | pierderi numărate de la 0, care ar umfla `pierdute` la prima pornire a serverului |
| D26 | **Faza 5:** comenzile se execută la următoarea bătaie a motorului, iar confirmarea trece prin buffer. `INTOARCE_LA_BAZA` merge înapoi pe traseu, iar oprirea la bază nu e frânare bruscă. O comandă cerută cu broker-ul deconectat dă 503. `IESIRE_DIN_ZONA` cere întâi o poziție în zonă. `VITEZA_PESTE_LIMITA` se rearmează strict sub limită. Statisticile folosesc ultimele 10 secunde și ultimele 10 mesaje live. `POST /api/retea/vreme` întoarce 200 și la `ERR`. UI-ul citește `STARE` la 3 secunde. Adăugată pe 8 octombrie, la planul fazei 5 | execuția comenzii pe firul MQTT, care s-ar bate cu motorul pe viteză și poziție |
| D27 | **Repo-ul are remote pe GitHub: `tudorraducristian/ProiectRC`, privat.** Creat pe 8 octombrie, la cererea lui Tudor. În `docs\design\` stau copii ale spec-ului, ale planurilor și ale rezumatului de sesiune. Originalele rămân în AIS-OS, iar o schimbare se face întâi acolo, apoi se copiază. După commit-urile unui task urmează `git push` | doar git local, fără nicio copie în afara laptopului |
| D28 | **Login cu două roluri, `ADMIN` și `DISPECER`,** după tiparul de la SCD: parolă cu BCrypt, token aleator ținut în bază, interceptor pe `/api/**` și token în `/ws?token=`. Primul `admin` îl face serverul, cu o parolă aleatoare scrisă o dată în log. Adăugată pe 19 octombrie (§16.2) | conturi și pentru mașini, la Mosquitto, cu listă de drepturi pe topic-uri: subiect de rețele, dar muncă în plus, respinsă de Tudor; Spring Security întreg, care ascunde exact mecanismul |
| D29 | **Setările stau în tabelul `setari`:** pragul pentru `OFFLINE_PRELUNGIT` și intervalul implicit al traseului. Le schimbă doar `ADMIN`. Adăugată pe 19 octombrie (§16.3) | limitele de viteză ale zonelor, care se încarcă din `zone.json` la fiecare pornire (D13) și s-ar suprascrie |
| D30 | **Meniul e un sertar lateral,** cu Harta, Rețea, Alarme, Setări și, doar pentru `ADMIN`, Conturi. Numele, rolul și „Ieșire” stau în bara de sus. Adăugată pe 19 octombrie (§16.4) | un meniu doar în bara de sus, înghesuit lângă chip-ul live |
| D31 | **Articolul științific îl scrie Claude în întregime,** în engleză, pe template-ul IEEE dat de profesor. Nu se publică. Adăugată pe 19 octombrie, la cererea lui Tudor (§16.5) | Tudor scrie, iar Claude revizuiește; textul în română, deși template-ul și numele materiei sînt și în engleză |
| D32 | **PowerPoint-ul și manualul sînt în română,** ca fișiere `.pptx` și `.docx`. PowerPoint-ul se face din articol, ca în nota *** a grilei. Adăugată pe 19 octombrie (§16.6, §16.7) | o prezentare în browser, pe care grila n-o numește |
| D33 | **Faza 6 vine după faza 5,** iar repetiția prezentării se mută din faza 5 în ea, fiindcă are nevoie de PowerPoint. Login-ul e primul în fază, iar manualul vine după ce UI-ul e final. Adăugată pe 19 octombrie (§16.8) | login-ul înaintea fazei 3, care ar fi întârziat partea de rețele, miezul notei |
| D34 | **Detaliile fazei 6, hotărâte la scrierea planului:** numele de cont se normalizează (`trim`, litere mici); parola are 8 caractere – 72 de octeți UTF-8, limita BCrypt; adminul nu-și resetează parola din Conturi, ci din Setări; token-urile expirate se caută o dată pe minut, iar canalele lor se închid cu codul 4001; metodele de admin poartă `@DoarAdmin`; token-ul stă în `localStorage['flota.token']`; documentele se fac cu Word și PowerPoint prin COM, din Markdown, iar figurile din SVG, cu Edge; template-ul se mută în `docs\articol\model\`; tunelul și găurile se măsoară de câte 3 ori; la repetiție, demonstrația ține sub 5 minute, iar totul sub 8. Adăugată pe 19 octombrie, odată cu planul fazei 6 | parola ca text liber de orice lungime, care la BCrypt ar da 500; resetarea propriei parole din Conturi, care l-ar scoate pe admin din aplicație |

## 3 · Arhitectura

```
 Simulator ──TCP──► Proxy ──TCP──► Mosquitto ◄──MQTT──► Server ──► MySQL
 (N mașini)  :1884    ▲     :1883                       │   ▲
                      └──── ordine text, TCP :1885 ─────┘   │
                                            WebSocket ▼     │ REST  :8080
                                                     UI Vue (Vite :5173)
```

**Cele cinci piese:**

| Piesa | Ce face | De ce depinde |
|---|---|---|
| Simulatorul | 10 mașini implicit, cel mult 20, din `date\flota.json`. Fiecare merge pe un traseu, publică telemetria o dată pe secundă, ține un buffer cât e offline și execută comenzi | Mosquitto, prin proxy |
| Proxy-ul | Dă octeții mai departe între fiecare mașină și Mosquitto și aplică vremea cerută. Primește ordine pe portul 1885 | Mosquitto; ordinele vin de la server |
| Mosquitto | Broker-ul MQTT. N-are cod de-al nostru, doar un `mosquitto.conf` | nimic |
| Serverul | Ascultă toate mașinile, salvează istoricul, aplică regulile de alarmă, trimite comenzi, traduce cererile de vreme în protocolul text și împinge datele live spre UI | Mosquitto, MySQL, proxy-ul |
| UI-ul | Harta, pagina Rețea și pagina Alarme; din faza 6, login, Setări și Conturi (§16.4) | serverul |

**Cele trei drumuri:**

- **Datele:** mașină → proxy → Mosquitto → server → MySQL și UI.
- **Comenzile:** UI → server → Mosquitto → proxy → mașină.
- **Vremea:** UI → server → proxy.

**Porturile.** Toate ascultă doar pe `127.0.0.1`, fiindcă totul rulează pe
același laptop:

| Port | Cine ascultă |
|---|---|
| 1883 | Mosquitto |
| 1884 | proxy-ul, pentru mașini |
| 1885 | proxy-ul, pentru ordine |
| 3306 | MySQL |
| 8080 | serverul Spring Boot |
| 5173 | serverul de dezvoltare Vite |

**Structura pe disc:**

```
RC-Proiect\
├── mosquitto\mosquitto.conf
├── date\            flota.json, trasee.json, zone.json
├── simulator\       Java 21, Maven prin mvnw: HiveMQ MQTT Client, Jackson 3, JUnit 6
├── proxy\           Java 21, Maven prin mvnw: doar JUnit 6
├── server\          Spring Boot 4.1, Java 21, Maven prin mvnw
├── web\             Vue 3, Vuetify, Leaflet, Vite, Node 24
└── docs\            scenariul de test și scenariul prezentării; din faza 6,
                     articol\, prezentare\ și manual\ (§16)
```

Proxy-ul n-are nicio dependență în afară de JUnit. E Java simplu, cu socket-uri,
ca la SCD Lab 1.

## 4 · Mașina și mesajele ei

**Analogia.** Fiecare mesaj e o scrisoare cu două ore. Una e scrisă de mână de
mașină, când s-a întâmplat ceva. Cealaltă e ștampila poștei, pusă când scrisoarea
a ajuns. Cât mașina e în tunel, cele două se despart, iar serverul le ține
separat.

### 4.1 · Topic-urile

Mașinile se numesc `masina-1`, `masina-2` și așa mai departe. Numele e și
client ID-ul MQTT. Serverul are client ID-ul `dispecer`.

| Topic | Cine → cine | QoS | Reținut | De ce |
|---|---|---|---|---|
| `flota/<id>/telemetrie` | mașina → server | 0 | nu | vine o dată pe secundă, iar dacă una se pierde, următoarea o înlocuiește |
| `flota/<id>/alarma` | mașina → server | 2 | nu | e rară și gravă: nu are voie nici să se piardă, nici să fie numărată de două ori |
| `flota/<id>/stare` | mașina → toți, plus broker-ul prin Last Will | 1 | da | cine se abonează mai târziu află starea pe loc |
| `flota/<id>/comenzi` | server → mașina | 1 | nu | sesiunea e persistentă, deci broker-ul păstrează comanda cât mașina e offline |
| `flota/<id>/confirmari` | mașina → server | 1 | nu | QoS spune doar că broker-ul a primit comanda; confirmarea spune că mașina a executat-o |

### 4.2 · Mesajele

Toate orele sînt în UTC, în format ISO-8601, cu milisecunde.

**Telemetria:**

```json
{"seq": 1042, "ora": "2026-12-10T10:15:03.120Z", "lat": 46.77121, "lon": 23.62360,
 "viteza": 47.5, "temperatura": 92.1, "baterie": 81, "din_buffer": false}
```

- `seq` crește cu 1 la fiecare mesaj de telemetrie și pornește de la 1 la fiecare
  pornire a simulatorului.
- `din_buffer` e `true` dacă mesajul a fost produs cât mașina era offline.

**Alarma:** `tip` e `SUPRAINCALZIRE` sau `FRANARE_BRUSCA`.

```json
{"tip": "SUPRAINCALZIRE", "ora": "2026-12-10T10:15:03.120Z", "valoare": 112.4}
```

**Starea:** textul simplu `online` sau `offline`. La conectare, mașina publică
`online`, ca mesaj reținut. Last Will-ul e `offline`, tot reținut, pe QoS 1. La
o oprire curată, mașina publică singură `offline` înainte să se deconecteze.

**Comanda:** `id` e cheia din tabelul `comenzi`.

```json
{"id": 57, "tip": "LIMITEAZA_VITEZA", "parametri": {"kmh": 50}}
```

| Tip | Parametri | Ce face mașina |
|---|---|---|
| `LIMITEAZA_VITEZA` | `kmh`, între 10 și 130 | nu mai depășește viteza dată |
| `INTOARCE_LA_BAZA` | nimic | se întoarce pe traseu până la primul punct, apoi stă pe loc, cu viteza 0, și raportează în continuare |
| `MESAJ_SOFER` | `text`, cel mult 200 de caractere | îl scrie în consola simulatorului |

**Confirmarea:**

```json
{"comanda_id": 57, "ora": "2026-12-10T10:15:04.002Z", "rezultat": "EXECUTATA"}
```

`rezultat` e `EXECUTATA` sau `RESPINSA`. Pentru `RESPINSA` mai vine câmpul
`motiv`, de exemplu „kmh în afara intervalului".

### 4.3 · Cum e construită o mașină

**Două fire de execuție pe mașină (D9),** legate printr-o coadă:

- **Motorul** rulează o dată pe secundă. Mută mașina pe traseu, calculează
  valorile și pune mesajul în coadă. Mesajul primește `din_buffer: true` dacă
  radioul e deconectat în momentul acela.
- **Radioul** ține conexiunea. Cât e conectat, scoate mesajele din coadă și le
  publică, în ordine. Cât nu e, încearcă să se reconecteze.

Coada e bufferul. Cât mașina e conectată, coada e aproape goală. Cât e în tunel,
se umple. E tiparul producător-consumator, același tip de problemă de fire de
execuție ca la SCD.

**Bufferul** are cel mult 300 de mesaje, adică cinci minute de telemetrie. Dacă
se umple, aruncă mesajul cel mai vechi și crește contorul `aruncate`. Alarmele
și confirmările trec prin aceeași coadă, dar nu se aruncă niciodată: dacă
trebuie aruncat ceva, se aruncă telemetria cea mai veche.

**Conexiunea**, cu clientul HiveMQ pe MQTT 3 (D17):

- `cleanSession(false)`, ca broker-ul să păstreze comenzile cât mașina e
  offline;
- `keepAlive(5)`, deci Mosquitto declară mașina pierdută după vreo 7,5
  secunde, adică 1,5 × keepalive-ul;
- 5 secunde pentru conectare, atât pentru TCP (`socketConnectTimeout`), cât și
  pentru răspunsul `CONNACK` (`mqttConnectTimeout`);
- timp maxim de așteptare la publicare de 10 secunde, ca radioul să nu rămână
  blocat într-o gaură neagră: publicarea e asincronă, cu `get(10, SECONDS)`;
- fără reconectare automată (D8). E și comportamentul implicit al clientului.

**Reconectarea.** Pauza după încercarea numărul n e min(30, 2ⁿ⁻¹) secunde, adică
1, 2, 4, 8, 16, 30, 30 și tot așa. Fiecare pauză se înmulțește cu un factor la
întâmplare între 0,8 și 1,2. Factorul există ca, atunci când zece mașini ies
deodată din tunel sau broker-ul repornește, să nu bată toate la ușă în aceeași
secundă. După o conectare reușită, numărătoarea pornește din nou de la 1.

**La reconectare,** radioul publică `online`, apoi golește coada în ordine. Abia
după aceea vin mesajele live, deci numerele de ordine ajung la server în ordine.

**Mașina nu știe nimic de tunel.** Vede doar că i-a murit conexiunea, ca o
mașină reală.

### 4.4 · Simularea

- **Traseele** stau în `date\trasee.json`, un `FeatureCollection` GeoJSON cu câte
  un `LineString` pe traseu și `properties.id` pentru fiecare (D18). Fiecare
  traseu e o buclă: după ultimul punct, mașina merge înapoi la primul. Zece
  mașini merg pe 3–5 trasee, pornite din puncte diferite.
- **Viteza** se apropie de o viteză-țintă, aleasă la întâmplare la vreo 30 de
  secunde, între 30 și 70 km/h. Rar, ținta sare la 90, ca regula de viteză să
  aibă ce prinde. `LIMITEAZA_VITEZA` taie ținta.
- **Temperatura** urcă odată cu viteza, cu un mic zgomot. Rar, o „defecțiune"
  o duce peste 110 °C, iar atunci se trimite `SUPRAINCALZIRE`, o singură dată pe
  episod.
- **Frânarea bruscă** e un eveniment rar, cam o dată la 10 minute pe mașină:
  viteza scade cu peste 25 km/h într-o secundă.
- **Bateria** scade cu 1% pe minut, de la 100 la 5, apoi rămâne la 5. Doar se
  afișează.
- **Flota** stă în `date\flota.json`: pentru fiecare mașină, id-ul, traseul,
  punctul de pornire și zona ei. Sînt 10 mașini implicit, cel mult 20. Fișierul
  îl citesc și simulatorul, și serverul, deci flota e scrisă într-un singur loc.
- **Setările de rețea** stau în `simulator.properties`: adresa broker-ului,
  implicit `tcp://127.0.0.1:1884`, adică proxy-ul, plus keepalive-ul, perioada și
  mărimea bufferului.

## 5 · Proxy-ul

**Analogia.** Proxy-ul e un traducător care stă între două persoane la telefon.
Fiecare crede că vorbește direct cu cealaltă. Traducătorul poate repeta mai
încet, poate tăcea o vreme sau poate închide telefonul.

### 5.1 · Cum e construit

- **Un fir de execuție ascultă pe portul 1884.** Pentru fiecare mașină nouă,
  proxy-ul deschide o conexiune spre Mosquitto, pe 1883.
- **Fiecare sens are două fire:** unul care citește și unul care scrie la ora
  potrivită. Între ele stă o coadă cu bucăți de octeți, fiecare cu ora la care
  are voie să plece. Ambele capete pot vorbi oricând, așa că un singur fir pe
  conexiune, ca la `ServerMultifir`, nu ajunge. Un fir care ar citi și apoi ar
  dormi în timpul întârzierii ar aduna întârzierile una peste alta. Ies patru
  fire pe mașină; la 20 de mașini sînt 80, ceea ce e în regulă.
- **Dacă un capăt închide,** proxy-ul închide și celălalt capăt și golește
  cozile.

### 5.2 · Cum recunoaște mașina

Proxy-ul citește primul pachet MQTT până la capăt, înainte să dea ceva mai
departe. Pachetul trebuie să fie `CONNECT`. Structura lui, în MQTT 3.1.1:

| Octeții | Ce sînt |
|---|---|
| 1 | tipul pachetului, `0x10` pentru `CONNECT` |
| 1–4 | „lungimea rămasă": 7 biți pe octet, iar bitul de sus spune dacă mai urmează un octet |
| 2 + 4 | lungimea, apoi numele protocolului, `MQTT` |
| 1 | nivelul protocolului, 4 pentru 3.1.1 |
| 1 | flag-urile conexiunii |
| 2 | keepalive-ul |
| 2 + n | lungimea, apoi client ID-ul, în UTF-8, de exemplu `masina-3` |

Restul pachetului, adică Last Will-ul, se dă mai departe neschimbat. Dacă primul
pachet nu e `CONNECT` sau e stricat, proxy-ul închide conexiunea și scrie motivul
în consolă. E singurul loc în care proiectul se uită înăuntrul unui pachet MQTT.

### 5.3 · Vremea

Fiecare mașină are o stare a vremii, protejată pentru accesul din mai multe fire.

| Felul | Ce face proxy-ul |
|---|---|
| Întârziere `ms`, cu variație opțională `v` | fiecare bucată pleacă după `ms ± v` milisecunde. Ora de plecare nu e niciodată mai devreme decât a bucății dinainte, ca ordinea octeților să rămână cea garantată de TCP |
| Bandă `kbps` | după fiecare bucată scrisă, firul care scrie așteaptă cât îi ia octeților ei să treacă la viteza dată |
| Tunel `s` | închide pe loc ambele capete. Cât ține tunelul, o mașină care încearcă să se reconecteze are conexiunea TCP acceptată, își trimite `CONNECT`, dar nu primește niciodată răspuns. Încercarea expiră după 5 secunde, ca în realitate. Spre Mosquitto nu se deschide nimic |
| Gaură neagră `s` | conexiunile rămân deschise, dar firele care scriu nu mai scriu nimic. Octeții stau în coadă. Dacă după `s` secunde conexiunea mai trăiește, octeții pleacă mai departe |
| Normal | șterge întârzierea și banda și oprește mai devreme tunelul sau gaura |

Întârzierea și banda se pot combina. Tunelul și gaura sînt temporare și le
suprascriu pe celelalte cât durează.

**Diferența dintre tunel și gaură** e demonstrația principală:

- **În tunel,** mașina află pe loc că a pierdut conexiunea, iar motorul pune
  mesajele în buffer. Pe hartă, porțiunea apare punctată.
- **În gaură,** mașina crede câteva secunde că e conectată și publică mai
  departe. Keepalive-ul descoperă gaura: la broker, trec 1,5 × keepalive, adică
  7,5 secunde, fără niciun pachet; la mașină, lipsește răspunsul la ping. Cine
  observă primul închide, iar proxy-ul închide și celălalt capăt, aruncând
  octeții ținuți. Mesajele de telemetrie din secundele acelea, pe QoS 0, se pierd
  pentru totdeauna. Din clipa în care mașina a observat, mesajele intră în
  buffer, ca la tunel.
  - Pe hartă rămâne o porțiune lipsă, urmată de una punctată.
  - Alarmele și comenzile, pe QoS 1 și 2, ajung după reconectare.
  - O gaură mai scurtă de 7,5 secunde nu pierde nimic, doar întârzie mesajele.
    Asta e lecția despre keepalive.

### 5.4 · Protocolul text, pe portul 1885

- **Formatul:** text ASCII, câte o comandă pe linie. Linia se termină cu `\n`;
  se acceptă și `\r\n`. Cuvintele sînt despărțite prin spații, iar comenzile nu
  țin cont de litere mari sau mici. O linie are cel mult 200 de caractere.
- **Răspunsul:** fiecare comandă primește `OK` sau `ERR <motiv>`, pe o linie.
- **Conexiunea** rămâne deschisă după fiecare comandă și poate duce oricâte
  comenzi.

| Comanda | Ce face |
|---|---|
| `INTARZIERE <masina\|TOATE> <ms> [<variatie_ms>]` | semnal slab |
| `BANDA <masina\|TOATE> <kbps>` | rețea înceată |
| `TUNEL <masina\|TOATE> <secunde>` | conexiunea tăiată |
| `GAURA <masina\|TOATE> <secunde>` | conexiunea deschisă, dar mută |
| `NORMAL <masina\|TOATE>` | șterge tot |
| `STARE` | câte un rând pe mașină, apoi un rând care are doar `.` |

`TOATE` înseamnă toate mașinile pe care proxy-ul le-a văzut măcar o dată.

Un exemplu de răspuns la `STARE`:

```
masina-1 NORMAL
masina-3 TUNEL 12
masina-5 INTARZIERE 500 200 BANDA 64
.
```

Rândul cu un singur punct îi spune celui care citește unde se termină răspunsul,
ca la SMTP.

**Erorile:** `ERR comanda necunoscuta`, `ERR masina necunoscuta` și
`ERR parametri gresiti`, aceasta din urmă pentru numere lipsă, negative sau în
afara intervalului. Conexiunea rămâne deschisă și după o eroare. Intervalele:
întârzierea și variația între 0 și 10.000 ms, banda între 1 și 100.000 kbps,
durata între 1 și 600 de secunde.

## 6 · Serverul

**Analogia.** Serverul e dispecerul. Registrul e baza de date, lista de reguli
spune când se dă alarma, comenzile sînt telefonul spre șoferi, iar UI-ul e
ecranul pe care îl ține la zi. Dispecerul nu stă în tunel: se conectează direct
la Mosquitto.

### 6.1 · Conexiunea MQTT

- Clientul HiveMQ are ID-ul `dispecer` și se conectează la `127.0.0.1:1883`, cu
  `cleanSession(false)` și cu reconectarea automată pornită (D11).
- Se abonează la `flota/+/telemetrie` pe QoS 0, la `flota/+/alarma` pe QoS 2, la
  `flota/+/stare` pe QoS 1 și la `flota/+/confirmari` pe QoS 1.
- Dacă serverul repornește, Mosquitto îi păstrează între timp alarmele și
  confirmările.
- Un mesaj stricat, de exemplu un JSON invalid sau o mașină necunoscută, se
  notează în log și se ignoră. Ascultarea nu se oprește.

### 6.2 · Baza de date, în MySQL

| Tabel | Coloanele |
|---|---|
| `masini` | `id` (`masina-3`, cheie), `zona_id`, `stare` (`ONLINE`/`OFFLINE`), `ultima_vedere`, `ultimul_seq`, `recuperate`, `pierdute` |
| `pozitii` | `id`, `masina_id`, `seq`, `ora_masinii`, `ora_sosirii`, `lat`, `lon`, `viteza`, `temperatura`, `baterie`, `din_buffer`; index pe (`masina_id`, `ora_masinii`) |
| `alarme` | `id`, `masina_id`, `tip`, `sursa` (`MASINA`/`SERVER`), `ora`, `ora_sosirii`, `detalii`, `vazuta` |
| `comenzi` | `id`, `masina_id`, `tip`, `parametri` (JSON), `ora_trimiterii`, `ora_confirmarii`, `stare` (`TRIMISA`/`CONFIRMATA`/`RESPINSA`), `motiv` |
| `zone` | `id`, `nume`, `poligon` (JSON, listă de `[lat, lon]`), `viteza_maxima` |
| `evenimente_retea` | `id`, `tinta` (o mașină sau `TOATE`), `linie` (comanda trimisă), `raspuns`, `ora_start`, `ora_sfarsit` |

Orele sînt `DATETIME(3)`, în UTC. Mașinile și zonele se încarcă la pornire din
`date\flota.json` și `date\zone.json`. `zone.json` e un `FeatureCollection`
GeoJSON cu câte un `Polygon` pe zonă, cu `properties.id`, `properties.nume` și
`properties.viteza_maxima`. La încărcare, coordonatele se întorc din `[lon, lat]`
în `[lat, lon]`. Încărcarea poate rula de mai multe ori
fără să dubleze ceva. La 10 mașini, `pozitii` crește cu 36.000 de
rânduri pe oră, ceea ce MySQL duce fără grijă.

### 6.3 · Ce face serverul cu fiecare mesaj

**Telemetria:**

1. Salvează poziția, cu `ora_sosirii` luată de pe ceasul serverului.
2. Verifică numărul de ordine:
   - dacă `seq > ultimul_seq + 1`, adaugă diferența, `seq − ultimul_seq − 1`, la
     `pierdute`;
   - dacă `seq < ultimul_seq`, înseamnă că simulatorul a repornit, deci
     `ultimul_seq` se resetează fără să numere pierderi.
3. Dacă mesajul are `din_buffer = true`, crește `recuperate`. Altfel, notează
   latența, adică `ora_sosirii − ora_masinii`. Latența se măsoară exact, fiindcă
   simulatorul și serverul rulează pe același laptop, cu același ceas. Pe mașini
   reale, ceasurile ar trebui sincronizate prin NTP.
4. Aplică regulile de alarmă (§6.4) și trimite poziția pe WebSocket.

**Alarma:** se salvează cu `sursa = MASINA` și se trimite pe WebSocket. QoS 2
garantează că vine o singură dată, așa că serverul nu caută dubluri. Proiectul
chiar asta demonstrează.

**Starea:** se actualizează `masini.stare` și `ultima_vedere`, apoi starea se
trimite pe WebSocket.

**Confirmarea:** comanda trece în `CONFIRMATA` sau `RESPINSA` doar dacă era
`TRIMISA`. O confirmare primită a doua oară, care pe QoS 1 se poate întâmpla, nu
mai schimbă nimic. Asta se cheamă idempotență.

### 6.4 · Regulile de alarmă

Toate se dau la schimbare (D12) și au `sursa = SERVER`:

| Tip | Când se dă | Când se rearmează |
|---|---|---|
| `IESIRE_DIN_ZONA` | poziția trece din zona mașinii în afara ei; verificarea se face cu algoritmul razei, pe poligon | mașina revine în zonă |
| `VITEZA_PESTE_LIMITA` | viteza trece peste `viteza_maxima` a zonei mașinii | viteza coboară sub limită |
| `OFFLINE_PRELUNGIT` | mașina e `OFFLINE` de peste 60 de secunde; verificarea rulează la 5 secunde | mașina revine `ONLINE` |

Regulile se aplică și pozițiilor recuperate. Așa se poate afla că o mașină a
ieșit din zonă cât era în tunel, iar alarma poartă ora mașinii.

### 6.5 · REST, pentru UI

| # | Endpoint | Ce face |
|---|---|---|
| 1 | `GET /api/masini` | lista mașinilor, cu starea și contoarele |
| 2 | `GET /api/masini/{id}` | o mașină, cu ultima poziție |
| 3 | `GET /api/masini/{id}/traseu?de=&pana=` | pozițiile din interval, implicit ultimele 5 minute |
| 4 | `GET /api/alarme?vazute=` | alarmele, filtrate după „văzută" |
| 5 | `PATCH /api/alarme/{id}` | marchează o alarmă ca văzută |
| 6 | `POST /api/masini/{id}/comenzi` | trimite o comandă: o salvează ca `TRIMISA` și o publică |
| 7 | `GET /api/masini/{id}/comenzi` | istoricul comenzilor unei mașini |
| 8 | `GET /api/zone` | zonele, doar de citit (D13) |
| 9 | `GET /api/retea/stare` | trimite `STARE` la proxy și întoarce răspunsul, despărțit pe mașini |
| 10 | `POST /api/retea/vreme` | primește `{comanda, tinta, parametri}`, construiește linia, o trimite la proxy, salvează evenimentul și întoarce `OK` sau `ERR` |
| 11 | `GET /api/retea/evenimente` | jurnalul evenimentelor de rețea |
| 12 | `GET /api/statistici` | pentru fiecare mașină: mesaje pe secundă, latența medie pe ultimele 10 mesaje live, recuperate, pierdute |

**Legătura cu proxy-ul:** pentru fiecare cerere, serverul deschide o conexiune
TCP pe 1885, trimite linia, citește răspunsul și închide. La `STARE` citește
până la rândul cu punct. Dacă proxy-ul nu răspunde în 2 secunde, endpoint-urile
9 și 10 întorc 503, iar UI-ul afișează „proxy indisponibil".

### 6.6 · WebSocket, pentru ce se întâmplă live

E un handler simplu, fără STOMP, pe `/ws`, ca la SCD. Până la faza 6 merge fără
token; din faza 6 cere `/ws?token=` (§16.2). Mesajele
au forma `{"tip": "...", "date": {...}}`, cu tipurile:

- `POZITIE`, cu semnul `din_buffer`;
- `STARE`;
- `ALARMA`;
- `CONFIRMARE`;
- `EVENIMENT_RETEA`;
- `STATISTICI`, trimis o dată pe secundă, cu aceleași cifre ca la endpoint-ul 12.

## 7 · UI-ul

**Analogia.** UI-ul e camera de control a dispeceratului: un perete cu harta
mare, unul cu „vremea" din rețea și un sertar cu alarmele.

E făcut cu Vue 3, Vuetify și Leaflet, folosit direct, fără un strat Vue
deasupra. Harta folosește imaginile OpenStreetMap, cu atribuirea cerută de ele.

**1. Harta**, pagina principală:

- Mașinile apar colorate după stare: verde dacă sînt online, gri dacă sînt
  offline, portocaliu dacă au vreme setată. Zonele sînt desenate pe hartă, iar
  lângă ea stă lista mașinilor.
- Panoul lateral al unei mașini arată viteza, temperatura, bateria, ora la care
  a fost văzută ultima dată și traseul.
  - Traseul are linie plină pentru live, linie punctată pentru pozițiile cu
    `din_buffer` și o porțiune lipsă acolo unde lipsesc numere de ordine.
  - Istoricul se alege pe interval.
- Tot din panou se trimit comenzile, fiecare cu starea ei: „trimisă",
  „așteaptă mașina" (trimisă, dar mașina e offline) sau „confirmată".

**2. Rețeaua:**

- Pentru o mașină sau pentru `TOATE`: butoane pentru tunel și gaură, cu durata
  în secunde; un slider pentru întârziere și unul pentru variație; un câmp
  pentru bandă; butonul „Normal".
- Starea curentă, reîmprospătată din `STARE`, și jurnalul evenimentelor.
- Pentru fiecare mașină, live: mesaje pe secundă, latență, recuperate, pierdute.
  Latența are un grafic simplu, desenat cu SVG, fără bibliotecă de grafice.

**3. Alarmele:** o listă filtrabilă, în care marchezi alarmele ca văzute.
Alarmele noi apar ca notificare pe orice pagină.

UI-ul nu are teste automate. Se verifică după scenariul de test (§10).

## 8 · Fazele

**Analogia.** E ca la o casă construită pe etaje. După fiecare etaj, casa e
locuibilă așa cum e, iar etajul următor doar o face mai bună.

| Faza | Ce se face | Ce poți arăta la sfârșit | Durata |
|---|---|---|---|
| 0 · Pregătirea | instalezi Mosquitto și scrii `mosquitto.conf`; creezi structura din §3, sub git; scrii un traseu și o zonă | un mesaj de test care trece prin Mosquitto | ½ săpt. |
| 1 · Mașinile vorbesc | întâi o mașină, apoi zece, cu motorul și radioul, conectate direct la broker, pe 1883; `stare` cu Last Will | zece mașini care raportează în consolă | 1½ săpt. |
| 2 · Serverul și harta | Spring Boot, tabelele, primirea mesajelor, WebSocket-ul, harta în Vue | mașinile mișcându-se live pe hartă | 2½ săpt. |
| 3 · Proxy-ul | dă octeții mai departe, citește `CONNECT`, aplică vremea, primește ordine pe portul 1885; simulatorul trece pe 1884 | un tunel pornit cu o comandă text | 2 săpt. |
| 4 · Mașina rezistă | bufferul, reconectarea, numărul de ordine, `din_buffer`, contoarele, liniile punctate și golurile | linia punctată după tunel și porțiunea lipsă după gaură | 1½ săpt. |
| 5 · Restul | pagina Rețea, regulile de alarmă, comenzile cu confirmare, latența, scenariul prezentării; repetiția s-a mutat în faza 6 (D33) | demonstrația live completă (§11) | 1½ săpt. |
| 6 · Login și documentele | login cu `ADMIN` și `DISPECER`, setările, meniul, articolul științific, PowerPoint-ul, manualul, repetiția prezentării (§16) | prezentarea completă, cu documentele cerute de grilă | 2 săpt. |

În total, 11½ săptămâni. Planul vechi, de 9½ săptămâni, se termina în jur de 18
decembrie. Faza 2 s-a terminat însă pe 19 octombrie, cu vreo trei săptămâni mai
devreme, deci și cu faza 6 sfârșitul rămâne înainte de vacanță.

**După faza 2, cerința de bază e îndeplinită:** o aplicație client-server cu UI,
pe temă IoT. Fazele 3–5 aduc nota mare la Rețele. Faza 6 nu se taie: fără
documentație, manual și PowerPoint, grila nu ia în calcul celelalte puncte
(§16.1).

**Ce se taie, în ordine, dacă timpul nu ajunge:**

1. banda îngustă și variația întârzierii;
2. pagina Alarme, păstrând doar notificările;
3. istoricul pe interval, păstrând doar ultimele 5 minute.

## 9 · Metoda

E metoda mixtă de la SCD:

- Primul exemplu din fiecare tipar îl scrie Claude și îl explică.
- Repetițiile le scrie Tudor, iar Claude le revizuiește fără să le rescrie.

Motivul e același: la prezentare, Tudor explică un cod scris de el.

Tiparele de la RC:

1. firul de execuție pe conexiune, cu citire și scriere separate;
2. parsarea unui pachet binar, adică `CONNECT`;
3. protocolul text, cu parsarea liniei și răspunsul;
4. producătorul și consumatorul, adică motorul, radioul și coada;
5. reconectarea cu pauză care se dublează;
6. abonarea MQTT în Spring și trimiterea pe WebSocket;
7. harta live în Vue;
8. din faza 6, autentificarea: token-ul, interceptorul și garda de rută din UI.
   Tudor îl învață și la SCD, deci aici poate fi o repetiție.

Articolul, PowerPoint-ul și manualul nu urmează metoda: le scrie Claude (D31,
D32).

Explicațiile urmează regula lui Tudor: întâi analogia și de ce contează, apoi
partea tehnică.

## 10 · Testele

**Automate, cu JUnit 6,** în fiecare modul Java:

| Modulul | Ce se testează |
|---|---|
| proxy | `CONNECT` citit dintr-un șir de octeți, inclusiv lungimea rămasă pe mai mulți octeți și un pachet stricat |
| proxy | protocolul text: fiecare comandă bună dă `OK`, iar fiecare caz de eroare dă `ERR` cu motivul corect; răspunsul la `STARE` se termină cu `.` |
| proxy | întârzierea cu variație nu schimbă niciodată ordinea bucăților |
| simulator | bufferul aruncă telemetria cea mai veche și păstrează alarmele |
| simulator | șirul de pauze: 1, 2, 4, 8, 16, 30, 30, fiecare în intervalul ±20% |
| simulator | `seq` crește cu 1, iar `din_buffer` urmează starea radioului |
| server | golurile din `seq`, inclusiv repornirea simulatorului |
| server | punctul în poligon, inclusiv pe margine |
| server | alarmele se dau la schimbare, nu la fiecare mesaj |
| server | aceeași confirmare primită de două ori se numără o dată |
| server | din faza 6, login-ul, token-ul, rolurile și setările (§16.9) |

**De la cap la coadă,** după scenariul scris în `docs\scenariu-test.md`, rulat de
mână la sfârșitul fazelor 3, 4 și 5:

1. Pornești tot și aștepți 30 de secunde: toate mașinile sînt verzi, iar
   `pierdute` e 0.
2. `TUNEL masina-3 15`: mașina devine gri imediat. După ieșire, în `pozitii`
   există rânduri cu `din_buffer = true`. `pierdute` a crescut cu cel mult 1,
   adică mesajul care era pe drum în clipa tăierii.
3. `GAURA masina-5 10`: după revenire, `pierdute` a crescut cu mai multe mesaje,
   cele din primele secunde ale găurii, iar `recuperate` a crescut cu cele de după
   ce mașina a observat gaura.
4. `GAURA masina-5 4`: nu se pierde și nu se recuperează nimic. Mesajele doar
   întârzie, fiindcă gaura e mai scurtă decât 7,5 secunde.
5. Comanda `LIMITEAZA_VITEZA`, trimisă către `masina-3` în timpul unui tunel:
   rămâne `TRIMISA`, iar după ieșire devine `CONFIRMATA`.
6. `INTARZIERE TOATE 500 200`: latența medie urcă la vreo 500 ms, iar mesajele
   nu sînt marcate `din_buffer`.

## 11 · Prezentarea

Durează vreo 5 minute:

1. Harta, cu zece mașini live.
2. `TUNEL masina-3 15`: mașina devine gri, iar la ieșire apar linia punctată și
   contorul „recuperate", care crește.
3. `GAURA masina-5 10`: mașina rămâne verde câteva secunde, deși e moartă, apoi
   devine gri. Pe hartă rămâne o porțiune lipsă, urmată de una punctată. Aici se
   explică nivelurile de QoS și keepalive-ul.
4. `LIMITEAZA_VITEZA`, trimisă unei mașini aflate în tunel: întâi „așteaptă
   mașina", apoi „confirmată" la ieșire. Aici se explică sesiunea persistentă și
   diferența dintre „livrat" și „executat".
5. `STARE`, arătat brut, așa cum vine din protocol. Aici se explică management-ul
   out-of-band și rândul cu punct.
6. `INTARZIERE TOATE 500 200`: graficul de latență urcă sub ochii publicului.
   Aici se explică ora mașinii, ora sosirii și NTP.

Prezentarea are nevoie de internet pentru harta OpenStreetMap, de exemplu prin
hotspot-ul telefonului.

**Din faza 6:** demonstrația începe cu login-ul, ca dispecer, și stă în mijlocul
PowerPoint-ului (§16.6). Repetiția, cu cronometrul, e ultimul pas din faza 6.

## 12 · Ce se schimbă în AIS-OS

După aprobarea spec-ului, odată cu planul:

- **Proiectul nou** `Facultate\flota-rc\CLAUDE.md`, după scheletul A–G al
  manualului de operare. Arată spre acest spec și spre codul din `RC-Proiect\`.
- **`Facultate\CLAUDE.md`** primește proiectul în §A, §C și §E. Corectura e
  aceeași care e deja deschisă pentru SCD: `Semestrul 1` nu mai e gol.
- **Mama, `CLAUDE.md`, tabelul E:** pe rândul Facultate, la proiecte apare
  `flota-rc`, iar codul e în `Facultate\An 4\Semestrul 1\RC-Proiect`.
- **`Facultate\memory\stare.md`** spune unde s-a rămas.

## 13 · În afara scopului

- ~~**Login și conturi** (D14).~~ Intră în faza 6 (D28).
- **Conturi pentru mașini la Mosquitto** (D28).
- **TLS, comparația măsurată de protocoale, broker-ul scris de mână,
  descoperirea pe UDP** (D4).
- **Pierderea reală de pachete IP,** cu `clumsy` sau `netem` (D5).
- **Editorul de zone** (D13).
- **MQTT 5** (D2).
- **Hardware real,** inclusiv o legătură cu `jetson-car-logger`. Ar fi o
  poveste bună mai târziu, dar nu e cerută.
- **Deploy-ul** în afara laptopului și **aplicația pe telefon.**
- **Testele automate pentru UI** (§7).

## 14 · Deschise

| # | Ce | Până când |
|---|---|---|
| Î1 | **Orașul traseelor.** Hotărăște unde se desenează traseele și zonele | înainte de faza 1 |
| Î2 | **Data vacanței de iarnă și data prezentării.** Planul se termină în jur de 18 decembrie | la următorul curs sau laborator de RC |
| Î3 | ~~Ce cere profesorul la predare~~ | **închisă pe 19 octombrie:** grila profesorului, cu 20 de sarcini; rezumată în §16.1. Ce acceptă la folosirea AI a rămas în Î9 |
| Î4 | ~~Numele proiectului în AIS-OS~~ | **închisă pe 8 octombrie:** `flota-rc`, aprobat de Tudor |
| Î5 | **Echipa:** aflat pe 19 octombrie: Tudor și un coleg formează o echipă și fac împreună flota-rc, deci presupunerea din §1, „lucrează singur”, nu mai ține. Rămân de discutat cu Tudor, mai încolo: ce componentă face și prezintă fiecare, dacă sarcinile 10–13 se acoperă prin componentele care vorbesc între ele, cum se împart documentele și legătura cu altă echipă (sarcinile 14–17). Valorează 4 puncte | de discutat cu Tudor, apoi de confirmat la profesor |
| Î6 | **„Colectare date din Internet”:** nota pentru proiectele fără hardware cere o aplicație IoT cu mai multe servere și clienți, cu date luate din Internet. Acum datele sînt generate local. O variantă: trasee reale sau vremea reală, luate de pe net | de întrebat la profesor |
| Î7 | **Rândurile roșii din grilă (14–18)** și numerotarea care sare de la 5 la 7 | de întrebat la profesor |
| Î8 | **Datele de autor pentru articol:** numele complet, facultatea, universitatea, orașul și emailul, cum le cere template-ul | înainte de Task-ul cu articolul din faza 6 |
| Î9 | **Folosirea AI:** articolul îl scrie Claude în întregime (D31). Dacă profesorul cere o mențiune, ea intră la Acknowledgment | înainte de predarea articolului |
| Î10 | **Sarcina 5, „Conexiune Intranet sau Internet”:** acum totul ascultă pe `127.0.0.1`, pe un singur laptop. Dacă profesorul vrea două dispozitive, UI-ul se deschide de pe telefon, prin hotspot, iar serverul și Vite ascultă și pe adresa din rețea | de întrebat la profesor |

**Verificat pe 8 octombrie, la scrierea planului,** pe Maven Central, în javadoc și
pe mosquitto.org:

- **Paho 1.2.5** e din iulie 2020, deci se trece pe **HiveMQ MQTT Client 1.4.0**
  (D17). API-ul folosit există în 1.4.0: `cleanSession`, `keepAlive`,
  `willPublish`, `socketConnectTimeout`, `mqttConnectTimeout` și
  `addDisconnectedListener`;
- **Jackson 3.2.3**, cu pachetul `tools.jackson`, păstrează adnotările din
  `com.fasterxml.jackson.annotation`;
- **JUnit 6.1.3**, cu același API Jupiter;
- **Spring Boot 4.1.1** e ultima versiune de pe linia 4.1;
- **Mosquitto 2.1.2** pentru Windows.

**Verificat tot pe 8 octombrie, la scrierea planurilor fazelor 2–5,** pe
start.spring.io, Maven Central și npm:

- **Starter-ele Spring Boot 4.1.1:** `spring-boot-starter-webmvc`,
  `-websocket` și `-jdbc`, plus `mysql-connector-j` 9.7.0. Pentru teste:
  `-jdbc-test`, `-webmvc-test` și `-websocket-test`. Boot fixează Jackson 3.1.5,
  JUnit 6.0.3 și Netty 4.2.17. Netty se coboară la 4.1.137 (D20);
- **HiveMQ 1.4.0** are `publishes(MqttGlobalPublishFilter, Consumer, Executor)`,
  `automaticReconnectWithDefaultConfig()` și `addSubscription()` pe
  builder-ele de abonare;
- **npm:** Vue 3.5.43, Vuetify 4.2.4, Leaflet 1.9.4, Vite 8.3.3,
  `@vitejs/plugin-vue` 6.0.9, vue-router 5.4.0 și `@mdi/font` 7.4.47.

**Planurile:** câte unul pe fază, în `docs\superpowers\plans\`:
`2026-10-08-flota-rc-faza-0-1.md`, apoi `…-faza-2.md`, `…-faza-3.md`,
`…-faza-4.md` și `…-faza-5.md`. Pentru faza 6, `2026-10-19-flota-rc-faza-6.md`.

## 15 · Riscuri

| # | Riscul | Ce facem |
|---|---|---|
| R1 | **Timpul:** SCD rulează în paralel | fiecare fază se termină cu ceva ce se poate arăta; după faza 2 cerința e îndeplinită; ordinea tăieturilor e fixată în §8 |
| R2 | **OneDrive blochează fișiere la build,** în `target\` și `node_modules\` | același risc acceptat ca la SCD. Dacă apare, sincronizarea OneDrive se pune pe pauză cât se lucrează |
| R3 | **Fără internet la prezentare,** deci fără imaginile hărții | hotspot-ul telefonului. Fără el, mașinile și traseele se văd pe fond gol |
| R4 | **Biblioteca MQTT nu mai primește versiuni noi** | s-a întâmplat cu Paho, rezolvat prin D17. Folosirea bibliotecii stă în două clase, una în simulator și una în server, deci o nouă schimbare ar atinge doar acele două clase |
| R5 | **Profesorul cere altceva la predare** | s-a întâmplat: grila cere login, manual, articol și PowerPoint. Rezolvat prin faza 6 (§16). Rămân deschise Î5–Î10 |
| R6 | **Cronometrajul găurii negre depinde de clientul MQTT și de Mosquitto,** deci cele „câteva secunde" pot varia | se măsoară în faza 4 și se trec în scenariul prezentării |
| R7 | **Faza 6 atinge tot UI-ul și tot REST-ul,** fiindcă login-ul se adaugă la final | interceptorul e unul singur, pe `/api/**`, iar UI-ul trimite token-ul dintr-un singur loc, `api.js`. Paginile din fazele 2–5 nu se rescriu |
| R8 | **Articolul are nevoie de cifre măsurate,** care apar abia după fazele 4 și 5 | măsurătorile se fac în faza 6, după scenariul de test (§10), cu aceleași interogări SQL; articolul nu inventează cifre |

## 16 · Faza 6: login, conturi și documentele de predare

*Adăugată pe 19 octombrie 2026, după grila profesorului.*

**Analogia.** Până acum am construit dispeceratul. Faza 6 îi pune ușă cu
ecuson, un panou de setări și o cutie cu acte: manualul pentru cine îl
folosește, articolul pentru cine vrea să știe cum merge și prezentarea pentru
ziua examenului.

### 16.1 · Grila și ce o acoperă

Grila are 20 de sarcini, fiecare de 0,5 puncte, deci 10 puncte. Numerotarea sare
de la 5 la 7 (Î7).

| Nr. | Sarcina | Cine o acoperă |
|---|---|---|
| 1 | aplicație PC, web server sau aplicație mobilă | fazele 2–5 |
| 2 | interfață simplu de utilizat | fazele 2–6 |
| 3 | meniu: setări, administrare conturi, log off | faza 6 (§16.3, §16.4) |
| 4 | acces pe bază de user și parolă | faza 6 (§16.2) |
| 5 | conexiune Intranet sau Internet, pe fir sau radio | fazele 1–5; vezi Î10 |
| 7 | funcționare online și offline | fazele 1 și 4 |
| 8 | structură de produs IoT | fazele 1–5 |
| 9 | aspect de produs | fazele 2–6 |
| 10–13 | comunicare, schimb de date, decizii și transmisie cu proiectele din propria echipă | neacoperite (Î5) |
| 14–17 | aceleași patru, cu proiectele altei echipe (rânduri roșii) | neacoperite (Î5, Î7) |
| 18 | manual de utilizare (rând roșu) | faza 6 (§16.7) |
| 19 | articol științific: descriere cod, scheme logice, diagrame bloc, scheme de conexiuni, montaj hardware | faza 6 (§16.5) |
| 20 | prezentare PowerPoint | faza 6 (§16.6) |
| 21 | susținerea prezentării | §11, repetată în faza 6 |

**Notele de subsol care contează:**

- Fiecare student face un proiect independent, cu documentație, manual și
  prezentare.
- Un proiect fără hardware trebuie să fie o aplicație IoT cu mai multe servere
  și clienți, cu date colectate din Internet (Î6). Componentele care lipsesc se
  pot simula din înregistrări disponibile online.
- **Fără documentație, manual și PowerPoint, proiectul nu poate fi susținut,
  iar celelalte sarcini nu se punctează.** De aceea faza 6 nu se taie.

### 16.2 · Login și conturi (D28)

**Rolurile:**

| Rol | Ce poate |
|---|---|
| `DISPECER` | tot ce era în fazele 2–5: harta, Rețea, Alarme, comenzile, vremea; își schimbă propria parolă; vede setările, fără să le schimbe |
| `ADMIN` | tot ce poate dispecerul, plus conturile și salvarea setărilor |

**Mecanismul, ca la SCD:**

1. Parola se ține ca hash BCrypt, din `spring-security-crypto`, singura bucată
   din Spring Security adusă în proiect.
2. Login-ul face un token aleator: 32 de octeți din `SecureRandom`, scriși în
   Base64 fără padding. Token-ul se ține în tabelul `tokenuri` și expiră la 8
   ore după login, cât o zi de laborator. Termenul e fix: folosirea nu-l
   prelungește. Ambele sînt alegerile lui Claude. Token-urile expirate se șterg
   la fiecare login.
3. UI-ul trimite token-ul în header-ul `Authorization: Bearer <token>`.
4. Un interceptor pe `/api/**`, cu excepția login-ului, întoarce 401 fără un
   token valid și 403 când rolul nu ajunge.
5. WebSocket-ul primește token-ul în `/ws?token=`, fiindcă browserul nu poate
   pune header-e la WebSocket. Fără un token valid, conexiunea se refuză la
   handshake.
6. Log off-ul șterge token-ul din bază, iar serverul închide WebSocket-urile
   deschise cu el. Același lucru se întâmplă la ștergerea unui cont sau la
   resetarea parolei lui. E alegerea lui Claude.

**Primul cont.** La pornire, dacă nu există niciun `ADMIN`, serverul creează
contul `admin`, cu o parolă aleatoare scrisă o singură dată în log, ca `WARN`.
Tudor o schimbă din Setări. Astfel, nicio parolă de cont nu stă în git.

**Regulile conturilor,** alese de Claude:

- numele are 3–20 caractere, din `a-z`, `0-9`, `.`, `_` și `-`, și e unic;
- parola are cel puțin 8 caractere;
- rolul se alege la creare și nu se mai schimbă;
- un admin nu-și poate șterge propriul cont, iar ultimul `ADMIN` nu se poate
  șterge;
- la ștergerea unui cont sau la resetarea parolei lui, token-urile lui se
  șterg, deci omul e scos din aplicație.

**Tabelele noi:**

| Tabel | Coloanele |
|---|---|
| `utilizatori` | `id`, `nume` (unic), `hash_parola`, `rol` (`ADMIN`/`DISPECER`), `creat_la` |
| `tokenuri` | `token` (cheie), `utilizator_id` (cu ștergere în cascadă), `creat_la`, `expira_la` |
| `setari` | `cheie` (cheie), `valoare` |

**Endpoint-urile noi,** în continuarea celor din §6.5:

| # | Endpoint | Cine | Ce face |
|---|---|---|---|
| 13 | `POST /api/auth/login` | oricine | `{nume, parola}` → `{token, nume, rol}`; la greșeală, 401 cu același mesaj, fie că e greșit numele, fie parola |
| 14 | `POST /api/auth/logout` | logat | șterge token-ul curent |
| 15 | `GET /api/auth/eu` | logat | `{nume, rol}` |
| 16 | `PUT /api/auth/parola` | logat | `{parolaVeche, parolaNoua}`; 400 dacă parola veche e greșită |
| 17 | `GET /api/utilizatori` | `ADMIN` | lista conturilor, fără hash-uri |
| 18 | `POST /api/utilizatori` | `ADMIN` | `{nume, parola, rol}` → 201; 409 dacă numele există |
| 19 | `DELETE /api/utilizatori/{id}` | `ADMIN` | 409 pentru propriul cont sau pentru ultimul `ADMIN` |
| 20 | `PUT /api/utilizatori/{id}/parola` | `ADMIN` | `{parolaNoua}`; șterge token-urile contului |
| 21 | `GET /api/setari` | logat | setările din §16.3 |
| 22 | `PUT /api/setari` | `ADMIN` | le salvează, după validare; 400 la o valoare în afara limitelor |

Endpoint-urile 1–12 cer un token valid, de orice rol.

**Legătura cu Rețele, pentru articol:** token-ul și parola circulă necriptate pe
HTTP, fiindcă TLS e în afara scopului (§13). Articolul discută asta ca limită:
ce vede cineva care ascultă rețeaua și ce ar rezolva HTTPS.

### 16.3 · Setările (D29)

| Cheie | Implicit | Limite | Ce schimbă |
|---|---|---|---|
| `offline_prelungit_secunde` | 60 | 10–3600 | pragul regulii `OFFLINE_PRELUNGIT` (§6.4), care în faza 5 e fix |
| `traseu_minute` | 5 | 1–360 | intervalul implicit al traseului din panoul mașinii (§7) |

Dacă tabelul e gol, serverul scrie valorile implicite la pornire. Pe lângă ele,
oricine își schimbă parola, din aceeași pagină.

### 16.4 · Meniul și paginile noi (D30)

- **Login,** pe `/login`: numele, parola, butonul „Intră” și mesajul de eroare.
  Fără cont nou de aici: conturile le face doar adminul.
- **Bara de sus:** titlul, chip-ul live, numele și rolul, butonul „Ieșire”.
- **Sertarul lateral:** Harta, Rețea, Alarme, Setări și, doar pentru `ADMIN`,
  Conturi.
- **Setări:** un card „Contul meu”, cu schimbarea parolei, și un card „Flota”,
  cu pragurile din §16.3. Dispecerul vede cardul „Flota”, dar nu-l poate salva.
- **Conturi,** doar pentru `ADMIN`: tabelul conturilor, „Cont nou” într-un
  dialog, ștergerea cu confirmare și resetarea parolei.

**În UI:**

- token-ul stă în `localStorage`, ca o reîncărcare a paginii să nu scoată omul
  din aplicație; „Ieșire” îl șterge. Alegerea e a lui Claude;
- `api.js` pune header-ul la fiecare cerere, iar la 401 șterge token-ul și duce
  la `/login`;
- o gardă în router trimite la `/login` fără token, iar o rută de admin, cerută
  fără rol, duce la Harta;
- `live.js` deschide WebSocket-ul cu `?token=`. Când conexiunea e refuzată
  sau închisă de server, întreabă `GET /api/auth/eu`: la 401 duce la `/login`,
  altfel se reconectează ca până acum. E alegerea lui Claude.

### 16.5 · Articolul științific (D31)

- **Template-ul:** `RC-Proiect\Model articol stiintific RC.docx`, modelul IEEE
  pentru conferințe: A4, două coloane, stilurile lui (titlu, autori, Abstract,
  Keywords, Heading 1–5, figure caption, table head, references). Articolul se
  scrie în copia template-ului, păstrând stilurile, iar textul de îndrumare din
  template se scoate tot.
- **Cine scrie:** Claude, tot textul. Tudor citește și aprobă. Nu se publică,
  deci regulile IEEE de trimitere nu se aplică, dar forma da.
- **Limba:** engleza. **Lungimea:** 4–6 pagini, alegerea lui Claude.
- **Autorul:** Tudor singur, cu datele din Î8.

**Structura și legătura cu grila (sarcina 19):**

| Secțiunea | Ce conține | Ce cere grila |
|---|---|---|
| Abstract, Keywords | problema, soluția, rezultatul în cifre | |
| I. Introduction | IoT pe rețele nesigure: o flotă care trece prin tunele | |
| II. Related work | MQTT și alte protocoale IoT, emularea rețelei | |
| III. System architecture | diagrama bloc a celor cinci piese; schema de conexiuni, cu porturi, protocoale și topic-uri | diagrame bloc, scheme de conexiuni |
| IV. Implementation | mașina: motorul, radioul, bufferul; proxy-ul: `CONNECT`, vremea, protocolul text; serverul: regulile, idempotența, WebSocket-ul; autentificarea | descrierea codului, scheme logice |
| V. Experimental evaluation | scenariul din §10, măsurat: tunelul, gaura de 10 s față de cea de 4 s, întârzierea de 500 ± 200 ms; tabele și grafice | |
| VI. Discussion and limitations | HTTP necriptat, un singur laptop, mașini simulate | |
| VII. Conclusion | | |
| Acknowledgment | după răspunsul la Î9 | |
| References | cel puțin 8 surse reale, de exemplu standardul MQTT 3.1.1 (OASIS), RFC 6455 pentru WebSocket, RFC 9293 pentru TCP, articolul despre Mosquitto din JOSS (2017) și BCrypt (Provos și Mazières, 1999) | |

**Figurile,** cel puțin:

- diagrama bloc a sistemului;
- schema de conexiuni;
- două scheme logice: reconectarea cu pauză care se dublează și citirea
  pachetului `CONNECT`;
- graficul unui tunel și al unei găuri, cu mesajele pierdute și recuperate;
- graficul latenței sub `INTARZIERE`;
- o captură a hărții.

„Montajul hardware” din grilă nu se aplică: articolul spune că proiectul e doar
software, cum permit notele grilei.

**Cifrele:** se măsoară în faza 6, după scenariul din §10, pe codul final, cu
interogări SQL păstrate lângă articol (R8).

**Fișierele:** `RC-Proiect\docs\articol\`, cu articolul `.docx`, figurile și
sursele lor, ca să poată fi refăcute. Unealta pentru figuri o alege planul.

### 16.6 · PowerPoint-ul (D32)

- Un fișier `.pptx`, în română, făcut din articol, pentru prezentarea de vreo 5
  minute din §11.
- Vreo 10 slide-uri:
  1. titlul;
  2. problema: o flotă care pierde semnalul;
  3. arhitectura;
  4. MQTT și nivelurile de QoS;
  5. proxy-ul și „vremea”;
  6. demonstrația live, după pașii din §11, cu login-ul la început;
  7. rezultatele măsurate;
  8. login-ul și limitele de securitate;
  9. ce n-am făcut și ce ar urma;
  10. concluziile și întrebările.
- Figurile sînt cele din articol.
- Fișierul: `RC-Proiect\docs\prezentare\flota-rc.pptx`.

### 16.7 · Manualul de utilizare (D32)

- Un fișier `.docx`, în română, pentru cineva care n-a văzut proiectul, cu
  capturi de ecran din aplicația finală.
- Capitolele:
  1. ce este aplicația;
  2. ce trebuie instalat: Java 21, Node 24, MySQL 8.4 și Mosquitto 2.1.2, plus
     crearea bazei;
  3. pornirea pieselor, în ordine;
  4. login-ul și contul de admin;
  5. Harta;
  6. Rețea;
  7. Alarme;
  8. Setări și Conturi;
  9. probleme frecvente, de exemplu „Access denied” la MySQL, un port ocupat,
     OneDrive care blochează build-ul și broker-ul oprit.
- Fișierul: `RC-Proiect\docs\manual\manual-flota-rc.docx`, cu capturile în
  `docs\manual\capturi\`.

### 16.8 · Ordinea în faza 6 (D33)

1. Login-ul, conturile, setările și meniul.
2. Măsurătorile pentru articol.
3. Articolul.
4. PowerPoint-ul, din articol.
5. Manualul, cu capturile din UI-ul final.
6. Repetiția prezentării, cu cronometrul, și verificarea finală. Repetiția vine
   din Task 16 al planului fazei 5. Planul fazei 5 se ajustează odată cu
   scrierea planului fazei 6: Task 16 păstrează scenariul și verificarea fazei 5.

Copia spec-ului din `RC-Proiect\docs\design\` se actualizează după regula D27,
în același commit cu primul cod din faza 6.

Durata e de vreo 2 săptămâni.

### 16.9 · Testele fazei 6

Automate, cu JUnit 6, în server:

- BCrypt: parola bună trece, cea greșită nu;
- fără token, cu token necunoscut sau expirat: 401;
- dispecerul pe un endpoint de admin: 403;
- primul `admin` se face doar când nu există niciun `ADMIN`;
- propriul cont și ultimul `ADMIN` nu se pot șterge;
- schimbarea parolei cere parola veche, iar resetarea ei șterge token-urile;
- setările în afara limitelor dau 400;
- WebSocket-ul fără token valid e refuzat.

UI-ul se verifică de mână: login, log off, ruta de admin cerută de un dispecer,
reîncărcarea paginii și o sesiune expirată.

Documentele se verifică citindu-le: articolul și manualul, randate ca PDF și
privite pagină cu pagină; PowerPoint-ul, slide cu slide.
