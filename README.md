# flota-rc

Proiectul de Rețele de Calculatoare: o flotă de mașini simulate care raportează
prin MQTT la un dispecerat cu hartă live. Designul:
`AIS-OS\docs\superpowers\specs\2026-10-08-flota-rc-design.md`.

## Prima pornire, o singură dată

1. **MySQL 8.4,** cu serviciul `MySQL84` pornit (`services.msc`). Versiunea:

   ```powershell
   & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" --version
   ```

2. **Baza și utilizatorul `flota`,** cu parola de root a MySQL:

   ```powershell
   Get-Content mysql\creeaza-baza.sql | & "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -h 127.0.0.1 -u root -p
   ```

   Tabelele le face serverul singur, la prima pornire.

3. **Bibliotecile UI-ului:**

   ```powershell
   cd web; npm install
   ```

   Se repetă după un `git pull` care schimbă `web\package.json`.

## Ce trebuie să ruleze înainte

- Serviciul MySQL (`services.msc` → `MySQL84`), cu baza creată mai sus.

## Cum pornești

Fiecare pas e într-un terminal separat, din folderul ăsta.

1. **A, broker-ul:**

   ```powershell
   & "C:\Program Files\mosquitto\mosquitto.exe" -c mosquitto\mosquitto.conf -v
   ```

2. **P, proxy-ul:** mașinile pe 1884, ordinele pe 1885.

   ```powershell
   .\mvnw.cmd -q -pl proxy compile exec:java
   ```

3. **S, serverul:**

   ```powershell
   .\mvnw.cmd -q -pl server spring-boot:run
   ```

4. **W, UI-ul,** apoi deschizi http://127.0.0.1:5173:

   ```powershell
   cd web; npm run dev
   ```

5. **C, simulatorul,** care se conectează la proxy:

   ```powershell
   .\mvnw.cmd -q -pl simulator compile exec:java
   ```

6. **O, ordinele pentru proxy,** de exemplu `TUNEL masina-3 15` sau `STARE`:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File unelte\ordine.ps1
   ```

7. **Opțional, ce circulă pe MQTT:**

   ```powershell
   & "C:\Program Files\mosquitto\mosquitto_sub.exe" -h 127.0.0.1 -t "flota/#" -v
   ```

Se opresc cu Ctrl+C. La „Terminate batch job (Y/N)?” răspunzi Y. Scenariul
complet de test e în `docs\scenariu-test.md`.

## De pe telefon

Telefonul deschide UI-ul de pe laptop, prin Vite. Serverul rămâne pe
`127.0.0.1`: la el ajunge doar Vite, de pe același laptop.

1. **Laptopul se leagă la hotspot-ul telefonului.** Telefonul are date mobile,
   așa că se încarcă și fundalul hărții.
2. **UI-ul pornește cu `npm run telefon`,** în loc de pasul 4, W, de mai sus:

   ```powershell
   cd web; npm run telefon
   ```

   `npm run dev` rămâne doar pe laptop. Pe o rețea străină, de exemplu Wi-Fi-ul
   facultății, laptopul nu se deschide spre rețea decât cât merge
   `npm run telefon`.
3. **Adresa pentru telefon** e cea de pe rândul `Network` afișat de Vite, de
   exemplu `http://192.168.137.1:5173`.
4. **Prima dată, Windows întreabă dacă lasă Node.js în rețea:** da, pentru
   rețele private.
5. **Dacă rețeaua hotspot-ului apare în Windows ca publică,** firewall-ul
   blochează telefonul. Profilul ei trece pe „Privată”, din Setări → Rețea și
   internet → Wi-Fi → numele rețelei.
6. **Pe telefon,** în Chrome, deschizi adresa și intri în cont. Opțional, din
   meniul Chrome, „Adaugă pe ecranul de pornire”.

Cât merge `npm run telefon`, oricine e în aceeași rețea poate vedea codul
UI-ului servit de Vite. Datele cer login.

## Primul login

La prima pornire, dacă nu există niciun administrator, S face contul `admin` și
scrie parola o singură dată în log, ca `WARN`:

```
Am creat contul admin. Parola, afișată o singură dată: …. Schimb-o din Setări.
```

Pe http://127.0.0.1:5173 intri cu `admin` și parola din log, apoi o schimbi din
Setări, la „Contul meu”. Parola nu stă nicăieri în git.

**Dacă parola lui `admin` s-a pierdut,** în baza `flota`:

```sql
DELETE FROM utilizatori WHERE nume = 'admin';
```

La repornirea lui S, serverul face din nou contul, cu o parolă nouă în log.

## Conturile

- **Rolurile:** `DISPECER` lucrează cu harta, Rețea, Alarme și comenzile, își
  schimbă parola și vede setările. `ADMIN` poate în plus conturile și salvarea
  setărilor.
- **Conturile noi** le face adminul, din Conturi. Numele are 3–20 caractere:
  litere mici, cifre, punct, `_` sau `-`. Parola are cel puțin 8 caractere.
  Rolul nu se mai schimbă după creare.
- **Sesiunea** ține 8 ore de la login. După, sau după o resetare de parolă
  făcută de admin, UI-ul te duce înapoi la login.
- **Cererile `/api`** cer token-ul primit la login, în `Authorization`:

  ```powershell
  $r = Invoke-RestMethod -Method Post -ContentType 'application/json' -Body '{"nume":"admin","parola":"…"}' http://127.0.0.1:8080/api/auth/login
  curl.exe -s -H "Authorization: Bearer $($r.token)" http://127.0.0.1:8080/api/masini
  ```

## Dacă Mosquitto nu pornește: HiveMQ CE

Aplicația vorbește MQTT 3.1.1 și nu ține de un broker anume. Orice broker pe
127.0.0.1:1883 merge, fără nicio schimbare de cod. Rezerva e HiveMQ CE, un broker
gratuit scris în Java; Java 21 e deja instalat. Colegul a rulat cu el toată faza
4, pe 2026-10-22.

**O dată, din timp, nu în ziua prezentării:**

1. Descarci `hivemq-ce-2026.5.zip` de pe
   https://github.com/hivemq/hivemq-community-edition/releases și îl dezarhivezi
   în `C:\hivemq-ce-2026.5`, în afara OneDrive.
2. Copiezi configurarea proiectului peste cea din arhivă:

   ```powershell
   Copy-Item hivemq\config.xml C:\hivemq-ce-2026.5\conf\config.xml -Force
   ```

   Ea face ce face și `mosquitto\mosquitto.conf`: ascultă doar pe 127.0.0.1,
   iar sesiunile stau în memorie. Oprește și statisticile anonime pe care HiveMQ
   le trimite implicit. Fără ea, broker-ul ar asculta pe toată rețeaua.

**Când e nevoie,** în locul pasului 1, A:

```powershell
$env:HIVEMQ_JMX_ENABLED = 'false'; & C:\hivemq-ce-2026.5\bin\run.bat
```

`HIVEMQ_JMX_ENABLED` e obligatoriu: altfel `run.bat` deschide portul 9010 spre
toată rețeaua, fără parolă, iar prin el procesul Java se poate controla de pe
alt calculator. Avertismentul despre drepturile de administrator se ignoră.

Restul pașilor rămân la fel. Serverul revine singur în cel mult 10 secunde,
mașinile în cel mult 30. `mosquitto_sub` (pasul 7) merge și cu HiveMQ CE, fiindcă
e doar un client MQTT.

**Ce e altfel,** din `docs\scenariu-test.md`, „Măsurători”:

| Ce | Mosquitto | HiveMQ CE |
|---|---|---|
| `GAURA masina-5 10`: secunde până devine gri | 6,6 | 5,2 |
| `GAURA masina-5 10`: mesaje pierdute | 7 | 5 |
| `TUNEL masina-3 15`: secunde până revine verde | 19,2 | 18,6 |

Cifrele cu HiveMQ CE sînt de pe laptopul colegului.

## Ce găsești în UI

- **Harta:**
  - mașinile, văzute de sus și rotite după direcția de mers, colorate după
    stare: verde online, gri offline, portocaliu cu vreme setată;
  - lista flotei și panoul mașinii, cu filele Comenzi și Traseu;
  - traseul: linie plină pentru live, punctată pentru recuperat, lipsă pentru
    pierdut.
- **Rețea:** tunel, gaură, întârziere, bandă și „Rețea normală pentru toată
  flota”; starea proxy-ului, jurnalul, statisticile pe mașină și graficul de
  latență.
- **Alarme:** fluxul de carduri, cu filtrul Nevăzute / Văzute / Toate, filtrul
  după tip și „Le-am văzut pe toate”. Alarmele noi apar ca alertă pe orice
  pagină, iar „Vezi pe hartă” deschide mașina, din alertă sau din card.
- **Setări:** propria parolă și cele două setări ale flotei: după câte secunde
  offline vine alarma și câte minute arată traseul din panoul mașinii.
- **Conturi,** doar pentru admin: lista, contul nou, resetarea parolei și
  ștergerea.

Pe laptop, meniul stă mereu în stânga, cu cifra alarmelor nevăzute, iar ☰ îl
ascunde. Pe telefon, ☰ deschide sertarul, iar punctul roșu de pe el arată că
există alarme nevăzute.

Toată aplicația merge și pe telefon, în browser: harta cu foaia care urcă de
jos, Rețea pe trei file, Alarme, Setări, Conturi și login-ul. Cum ajunge
telefonul la laptop scrie la „De pe telefon”.

## Documente

- `docs\scenariu-test.md`: scenariul de test, de la cap la coadă, și
  măsurătorile.
- `docs\scenariu-prezentare.md`: prezentarea, în 5 minute.

## Traseele

Cele zece mașini pleacă din trei baze: Mănăștur (mașinile 1–3), Mărăști (4–6)
și Bună Ziua (7–10). Fiecare are tura ei pe străzile Clujului, în
`date\trasee.json`. Bazele și locurile prin care trece fiecare tură stau în
`unelte\trasee.mjs`. După o schimbare acolo, fișierul se face din nou, cu
internet, fiindcă drumul pe străzi îl dă OSRM:

```powershell
node unelte\trasee.mjs
```

Simulatorul citește doar `date\trasee.json`, deci la prezentare nu are nevoie de
internet pentru trasee.

## Testele

```powershell
.\mvnw.cmd -q test
```

## Baza, pentru verificări

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
& $mysql -h 127.0.0.1 -u flota -pflota flota
```
