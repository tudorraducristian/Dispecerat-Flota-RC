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
  - mașinile, colorate după stare: verde online, gri offline, portocaliu cu
    vreme setată;
  - panoul mașinii, comenzile și istoricul pe interval;
  - traseul: linie plină pentru live, punctată pentru recuperat, lipsă pentru
    pierdut.
- **Rețea:** tunel, gaură, întârziere, bandă și normal; starea proxy-ului,
  jurnalul, statisticile pe mașină și graficul de latență.
- **Alarme:** lista, filtrată după „văzută”. Alarmele noi apar ca notificare pe
  orice pagină.

## Documente

- `docs\scenariu-test.md`: scenariul de test, de la cap la coadă, și
  măsurătorile.
- `docs\scenariu-prezentare.md`: prezentarea, în 5 minute.

## Testele

```powershell
.\mvnw.cmd -q test
```

## Baza, pentru verificări

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
& $mysql -h 127.0.0.1 -u flota -pflota flota
```
