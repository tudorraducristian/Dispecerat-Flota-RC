# flota-rc, faza 2: serverul și harta — plan de implementare

> **Pentru agenți:** SUB-SKILL OBLIGATORIU: superpowers:subagent-driven-development
> (recomandat) sau superpowers:executing-plans, ca planul să fie executat task cu
> task. Pașii folosesc căsuțe (`- [ ]`) pentru urmărire.

**Ținta:** mașinile din faza 1 apar live pe o hartă în browser. Serverul ascultă
Mosquitto, ține pozițiile, alarmele și starea în MySQL și le împinge pe WebSocket
spre UI. Cu faza asta, cerința profesorului e îndeplinită (spec §8).

**Arhitectura:**
- **Serverul** e un modul Spring Boot 4.1, `server`, listat în agregator, dar cu
  părintele `spring-boot-starter-parent`.
- **Drumul unui mesaj:** `ClientMqtt` → `Decodor` → `Dispecer` → MySQL și
  `CanalLive`.
  - `ClientMqtt` e singura clasă care știe de HiveMQ.
  - `Decodor` e testat și transformă octeții în mesaje verificate.
  - `Dispecer` scrie în MySQL prin `JdbcClient` și trimite datele pe `CanalLive`.
- **UI-ul** e în `web\`, cu Vue 3 și Vuetify. Leaflet e folosit direct, fără strat
  Vue deasupra.

**Stack:**
- **Serverul:** Spring Boot 4.1.1, din al cărui BOM vin Jackson 3.1.5, JUnit 6.0.3
  și Mockito. Plus HiveMQ MQTT Client 1.4.0, pe Netty 4.1.137.
- **Baza:** MySQL Community Server 8.4 LTS, cu Connector/J 9.7.0.
- **UI-ul:** Vue 3.5.43, Vuetify 4.2.4, Leaflet 1.9.4 și Vite 8.3.3, pe Node 24.

**Spec:** `docs\superpowers\specs\2026-10-08-flota-rc-design.md`, faza 2 din §8.
Planul se sprijină pe codul din planul fazelor 0–1
(`docs\superpowers\plans\2026-10-08-flota-rc-faza-0-1.md`): fișierele din
`date\`, Mosquitto pe 1883 și simulatorul care raportează. Dacă la execuție codul
scris în faza 1 diferă de ce presupune un task, se ajustează task-ul, nu codul
fazei 1.

## Constrângeri globale

- **Folderul proiectului:** `C:\Users\40747\OneDrive\Documents\Facultate\An 4\Semestrul 1\RC-Proiect\`,
  cu repo git propriu (D15). Comenzile pornesc din folderul ăsta, dacă nu scrie
  altfel.
- **Remote-ul:** `origin` e repo-ul privat `tudorraducristian/ProiectRC` de pe
  GitHub (D27). După commit-urile unui task urmează `git push`.
- **Java 21.** Maven doar prin `.\mvnw.cmd`.
- **Versiuni fixe:**
  - părintele serverului e `spring-boot-starter-parent` `4.1.1`;
  - HiveMQ MQTT Client `1.4.0`;
  - în `server\pom.xml`, `netty.version` e `4.1.137.Final` (D20);
  - MySQL Community Server `8.4` LTS;
  - npm, fără `^`: `vue` `3.5.43`, `vuetify` `4.2.4`, `@mdi/font` `7.4.47`,
    `leaflet` `1.9.4`, `vite` `8.3.3`, `@vitejs/plugin-vue` `6.0.9`.
- **Pachetele** încep cu `ro.flota.server`. Numele din cod sînt în română, fără
  diacritice. Mesajele și comentariile au diacritice.
- **MySQL se folosește prin `JdbcClient`, cu SQL scris de mână (D19).** Schema stă
  în `server\src\main\resources\schema.sql`. Hibernate și JPA nu intră în proiect.
- **Orele:**
  - în Java sînt `Instant`, iar în MySQL `DATETIME(3)`, în UTC;
  - conexiunea are `connectionTimeZone=UTC`;
  - în JSON-ul spre UI, orele sînt șiruri ISO-8601, iar câmpurile sînt în
    camelCase: `oraMasinii`, `dinBuffer`. Doar JSON-ul de pe MQTT păstrează
    `din_buffer`, fiindcă așa îl scrie mașina.
- **Serverul în MQTT:**
  - client ID-ul e `dispecer`, iar broker-ul e `127.0.0.1:1883`;
  - sesiunea e persistentă, cu `cleanSession(false)`, și reconectarea automată e
    pornită (D11);
  - abonările se refac la fiecare conectare, iar mesajele se primesc printr-un
    filtru global, pe un singur fir (D22).
- **Porturile:** 8080 pentru server și 5173 pentru Vite, doar pe `127.0.0.1`.
  Browserul vorbește doar cu Vite, care trimite `/api` și `/ws` mai departe la
  8080. Așa nu e nevoie de CORS.
- **UI-ul:**
  - componentele se scriu cu `data()`, `computed` și `methods`, ca la SCD (D21);
  - cererile REST trec toate prin `web\src\api.js`;
  - n-are teste automate (spec §7).
- **Metoda mixtă (spec §9).** Fiecare task spune cine scrie codul:
  - **Claude (exemplu explicat):** Claude scrie codul, apoi i-l explică lui
    Tudor, întâi analogia și de ce contează, apoi partea tehnică.
  - **Tudor (repetiție):** Tudor scrie codul după test sau după indicii. Claude
    revizuiește fără să rescrie. Soluția de referință stă într-un bloc ascuns, pe
    care Tudor nu-l deschide înainte să încerce.
- **Commit-urile lui Claude** se termină cu
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Commit-urile lui
  Tudor sînt ale lui.
- **Descărcările** se fac doar după ce Tudor spune da, cu sursa și mărimea spuse
  dinainte:
  - instalatorul MySQL;
  - bibliotecile Spring Boot, aduse de Maven la primul build al serverului, vreo
    40 MB;
  - `npm install`, vreo 60 MB în `web\node_modules\`.
- **Riscul R2:** `target\` și `node_modules\` stau în OneDrive. Dacă build-ul sau
  `npm install` dă `EPERM` sau `EBUSY`, sincronizarea OneDrive se pune pe pauză.

## Completări la spec, făcute la scrierea planului

Intră în spec ca D19–D23, în același commit cu planul:

- **D19.** Serverul vorbește cu MySQL prin `JdbcClient` și SQL scris de mână, nu
  prin JPA.
  - **De ce:** contoarele și idempotența confirmărilor sînt `UPDATE`-uri
    condiționate, vizibile în cod. SCD predă deja JPA.
  - **Ce înlocuiește:** JPA cu entități, care ar ascunde exact aceste `UPDATE`-uri.
- **D20.** În server, Netty e fixat la `4.1.137.Final`. Boot 4.1.1 aduce Netty
  4.2.17, iar HiveMQ 1.4.0 e construit pe 4.1.137, verificat pe Maven Central pe
  8 octombrie. Serverul nu folosește Netty în altă parte.
- **D21.** Versiunile UI-ului și stilul lui:
  - Vuetify 4.2.4, Leaflet 1.9.4 (2.0 e încă alpha) și Vite 8.3.3, plus
    vue-router 5.4.0 din faza 5;
  - componentele se scriu cu `data()` și `methods`, ca la SCD;
  - pentru cereri se folosește `fetch`, nu Axios, ca să fie o dependență mai
    puțin.
- **D22.** Serverul primește mesajele printr-un filtru global (`ALL`), pe un fir
  dedicat, și se abonează din nou la fiecare conectare.
  - **Filtrul global** prinde și mesajele păstrate în sesiunea persistentă, care
    sosesc imediat după `CONNACK`, înainte de orice abonare nouă.
  - **Abonarea refăcută:** Mosquitto, cu `persistence false`, uită abonările când
    repornește.
  - **Firul unic** procesează mesajele în ordine, deci nimic nu se bate pe
    aceeași mașină.
- **D23.** `ultima_vedere` se actualizează la fiecare telemetrie și la fiecare
  schimbare de stare. `GET /api/masini` întoarce și ultima poziție a fiecărei
  mașini, ca o mașină offline să apară gri acolo unde a fost văzută ultima dată.

## Unde trebuie să fie atent review-ul

Cinci situații pe care un om le întâlnește repede, cu comportamentul așteptat:

1. **Pe MQTT vine un mesaj stricat:** JSON invalid, un câmp lipsă, o mașină
   necunoscută sau un topic străin. Serverul scrie motivul în log, ignoră
   mesajul și ascultă mai departe. Testele sînt în Task 4.
2. **Serverul pornește cu Mosquitto oprit.** Pornește oricum, anunță că e
   deconectat și se conectează singur când apare broker-ul. Se verifică în Task
   11, pasul 6.
3. **Serverul e repornit de două ori.** Nicio mașină și nicio zonă nu se
   dublează, iar contoarele rămân. Se verifică în Task 3, pasul 9, și în Task 11,
   pasul 5.
4. **Traseul se cere pe un interval greșit:** `de` după `pana`, o oră scrisă
   greșit sau peste 6 ore. Serverul întoarce 400, cu motivul. Testele sînt în
   Task 7: `IntervalTest`.
5. **Un tab de browser se închide brusc.** Serverul nu cade și nu mai trimite
   spre el, iar celelalte taburi primesc mai departe. Testul e în Task 5:
   `oSesiuneMoartaEsteScoasaFaraSaLeStricePeCelelalte`.

---

## Structura fișierelor

```
RC-Proiect\
├── pom.xml                              agregatorul: se adaugă modulul server
├── mysql\creeaza-baza.sql               baza flota și utilizatorul flota
├── docs\mesaje-test\alarma.json         o alarmă de trimis cu mosquitto_pub
├── server\
│   ├── pom.xml                          părintele e spring-boot-starter-parent 4.1.1
│   └── src\
│       ├── main\resources\application.properties, schema.sql
│       ├── main\java\ro\flota\server\
│       │   ├── ServerApplication.java   pornirea și ceasul
│       │   ├── date\                    Punct, ZonaDefinita, MasinaDefinita, EroareDate,
│       │   │                            CititorDate, RegistruFlota, IncarcareDate
│       │   ├── mqtt\                    MesajPrimit, MesajInvalid, Decodor, ClientMqtt
│       │   ├── flota\                   Ore, PozitieDto, MasinaDto, AlarmaDto, StareMasinaDto,
│       │   │                            ZonaDto, Pozitii, Masini, Alarme, Zone, Dispecer
│       │   ├── live\                    MesajLive, CanalLive, ConfigWebSocket
│       │   └── api\                     Interval, CerereGresita, Negasit, EroriApi,
│       │                                MasiniController, ZoneController
│       └── test\java\ro\flota\server\   CititorDateTest, DecodorTest, CanalLiveTest, IntervalTest
└── web\
    ├── package.json, package-lock.json, vite.config.js, index.html
    └── src\
        ├── main.js, App.vue
        ├── api.js                       toate cererile REST
        ├── live.js                      WebSocket-ul și tabla dispecerului
        ├── pagini\PaginaHarta.vue
        └── components\HartaFlotei.vue, ListaMasini.vue, PanouMasina.vue
```

În AIS-OS se schimbă doar `Facultate\memory\stare.md`, în Task 11.

---

### Task 1: MySQL și baza `flota`

**Cine scrie:** Tudor instalează și rulează scriptul, fiindcă sînt setări de
sistem și parola de root e a lui. Claude scrie scriptul SQL.

**Fișiere:**
- Creează: `mysql\creeaza-baza.sql`

**Interfețe:**
- Produce: baza `flota` pe `127.0.0.1:3306`, cu utilizatorul `flota` și parola
  `flota`. Parola stă în git: baza ascultă doar pe `127.0.0.1`, iar repo-ul de
  pe GitHub e privat.

- [ ] **Pasul 1: Verifică dacă MySQL e deja instalat, de la SCD**

```powershell
Get-Service -Name "MySQL*" -ErrorAction SilentlyContinue | Select-Object Name, Status, StartType
Get-ChildItem "C:\Program Files\MySQL" -Directory -ErrorAction SilentlyContinue | Select-Object Name
Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue
```

- **Dacă există un serviciu care rulează și ascultă pe 3306,** sari la pasul 3.
  Folderul găsit, de exemplu `MySQL Server 8.4`, se folosește în căile de mai
  jos.
- **Dacă ascultă pe alt port,** de exemplu 3308, ca la profesorul de SCD, portul
  se schimbă în `application.properties`, în Task 2.
- **Dacă nu e nimic,** urmează pasul 2.

- [ ] **Pasul 2: Tudor instalează MySQL 8.4 LTS, după ce spune da la descărcare**

Claude îl întreabă întâi: „Instalatorul MSI pentru MySQL Community Server 8.4 LTS
are vreo 130 MB și vine de pe dev.mysql.com. E bine?”. Dacă spune da:

1. Pe https://dev.mysql.com/downloads/mysql/, alege versiunea `8.4.x LTS`,
   sistemul `Microsoft Windows` și pachetul `Windows (x86, 64-bit), MSI
   Installer`. Link-ul „No thanks, just start my download” sare peste cont.
2. Instalarea, tip `Typical`, apoi configuratorul:
   - `Development Computer`, portul `3306`;
   - parola de root o alege Tudor și n-o scrie nicăieri în proiect;
   - serviciul Windows `MySQL84`. Pornirea la startup e la alegerea lui; dacă o
     lasă oprită, pornește serviciul din `services.msc` înainte de lucru.

- [ ] **Pasul 3: Scrie `mysql\creeaza-baza.sql`**

```sql
-- Baza și utilizatorul pentru flota-rc. Se rulează o singură dată, ca root.
-- Parola e „flota”: baza ascultă doar pe 127.0.0.1, iar repo-ul de pe GitHub e privat.
CREATE DATABASE IF NOT EXISTS flota CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'flota'@'localhost' IDENTIFIED BY 'flota';
CREATE USER IF NOT EXISTS 'flota'@'127.0.0.1' IDENTIFIED BY 'flota';

GRANT ALL PRIVILEGES ON flota.* TO 'flota'@'localhost';
GRANT ALL PRIVILEGES ON flota.* TO 'flota'@'127.0.0.1';
```

- [ ] **Pasul 4: Tudor rulează scriptul ca root**

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
& $mysql -u root -p --default-character-set=utf8mb4 -e "source mysql/creeaza-baza.sql"
```

Cere parola de root și nu afișează nimic dacă merge.

- [ ] **Pasul 5: Verifică utilizatorul `flota`**

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
& $mysql -h 127.0.0.1 -u flota -pflota flota -e "SELECT CURRENT_USER() AS eu, DATABASE() AS baza, VERSION() AS versiune"
```

Se așteaptă: un avertisment că parola stă în linia de comandă, care e în regulă
aici, apoi un rând cu `flota@127.0.0.1` sau `flota@localhost`, `flota` și
`8.4.x`.

- [ ] **Pasul 6: Claude explică.** Analogia: MySQL e arhiva dispeceratului, iar
  `flota` e un dulap din ea. Utilizatorul `flota` are cheie doar la dulapul ăsta,
  nu la toată arhiva, ca root-ul. Apoi:
  - de ce sînt doi utilizatori, `localhost` și `127.0.0.1`;
  - ce e `utf8mb4`: UTF-8 complet, cu tot cu diacriticele din `detalii`.

- [ ] **Pasul 7: Commit**

```bash
git add mysql/creeaza-baza.sql
git commit -m "MySQL: baza si utilizatorul flota

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Serverul pornește și face tabelele

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „cum pornește Spring Boot
și cine creează tabelele”. Task-ul n-are logică de testat; verificarea se face pe
bază.

**Fișiere:**
- Modifică: `pom.xml`, agregatorul, unde adaugi modulul
- Creează: `server\pom.xml`
- Creează: `server\src\main\java\ro\flota\server\ServerApplication.java`
- Creează: `server\src\main\resources\application.properties`, `schema.sql`

**Interfețe:**
- Produce:
  - bean-ul `Clock`, pe UTC;
  - din Boot, bean-urile `JdbcClient` și `JsonMapper`, cel din
    `tools.jackson.databind.json`;
  - cele șase tabele din spec §6.2;
  - proprietățile `flota.dosar-date`, `flota.mqtt.host` și `flota.mqtt.port`.

- [ ] **Pasul 1: Adaugă modulul în agregator**

În `pom.xml`, `<modules>` devine:

```xml
  <modules>
    <module>simulator</module>
    <module>server</module>
  </modules>
```

Serverul are alt părinte decât agregatorul, adică `spring-boot-starter-parent`,
așa că versiunile lui de JUnit și Jackson vin din Boot, nu din agregator.
Simulatorul nu e atins.

- [ ] **Pasul 2: Scrie `server\pom.xml`**

Numele starter-elor sînt cele generate de start.spring.io pentru Boot 4.1.1,
verificate pe 8 octombrie.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
    <relativePath/>
  </parent>

  <groupId>ro.flota</groupId>
  <artifactId>server</artifactId>
  <version>0.1.0</version>

  <properties>
    <java.version>21</java.version>
    <hivemq.version>1.4.0</hivemq.version>
    <!-- Boot 4.1.1 aduce Netty 4.2; HiveMQ 1.4.0 e construit pe 4.1 (D20). Serverul nu folosește Netty în altă parte. -->
    <netty.version>4.1.137.Final</netty.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>com.hivemq</groupId>
      <artifactId>hivemq-mqtt-client</artifactId>
      <version>${hivemq.version}</version>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-jdbc-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webmvc-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-websocket-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <!-- Ca ../date din application.properties să fie dosarul date\ al proiectului. -->
          <workingDirectory>${project.basedir}</workingDirectory>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Pasul 3: Scrie `ServerApplication`**

`server\src\main\java\ro\flota\server\ServerApplication.java`:

```java
package ro.flota.server;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Dispecerul: ascultă flota prin Mosquitto, ține registrul în MySQL și împinge datele live spre UI. */
@SpringBootApplication
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    /** Ceasul serverului, pe UTC. Clasele care au nevoie de oră îl primesc, ca testele să-l poată fixa. */
    @Bean
    Clock ceas() {
        return Clock.systemUTC();
    }
}
```

- [ ] **Pasul 4: Scrie `application.properties`**

`server\src\main\resources\application.properties`:

```properties
# Totul rulează pe laptop (spec §3).
server.address=127.0.0.1
server.port=8080

# MySQL, prin JdbcClient (D19). Orele merg în UTC în ambele sensuri.
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/flota?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true
spring.datasource.username=flota
spring.datasource.password=flota

# schema.sql rulează la fiecare pornire; tabelele se creează doar dacă lipsesc.
spring.sql.init.mode=always

# Dosarul cu flota.json și zone.json, față de folderul server\.
flota.dosar-date=../date

# Serverul vorbește direct cu Mosquitto, nu prin proxy (D11).
flota.mqtt.host=127.0.0.1
flota.mqtt.port=1883
```

- [ ] **Pasul 5: Scrie `schema.sql`**

`server\src\main\resources\schema.sql`. Tabelele sînt cele din spec §6.2.
`comenzi` și `evenimente_retea` se folosesc abia în faza 5, dar schema se scrie
o dată, întreagă:

```sql
CREATE TABLE IF NOT EXISTS zone (
  id            VARCHAR(20)  NOT NULL PRIMARY KEY,
  nume          VARCHAR(100) NOT NULL,
  poligon       JSON         NOT NULL,
  viteza_maxima INT          NOT NULL
);

CREATE TABLE IF NOT EXISTS masini (
  id            VARCHAR(20) NOT NULL PRIMARY KEY,
  zona_id       VARCHAR(20) NOT NULL,
  stare         VARCHAR(10) NOT NULL DEFAULT 'OFFLINE',
  ultima_vedere DATETIME(3) NULL,
  ultimul_seq   BIGINT      NULL,
  recuperate    BIGINT      NOT NULL DEFAULT 0,
  pierdute      BIGINT      NOT NULL DEFAULT 0,
  CONSTRAINT fk_masini_zona FOREIGN KEY (zona_id) REFERENCES zone (id)
);

CREATE TABLE IF NOT EXISTS pozitii (
  id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  masina_id   VARCHAR(20) NOT NULL,
  seq         BIGINT      NOT NULL,
  ora_masinii DATETIME(3) NOT NULL,
  ora_sosirii DATETIME(3) NOT NULL,
  lat         DOUBLE      NOT NULL,
  lon         DOUBLE      NOT NULL,
  viteza      DOUBLE      NOT NULL,
  temperatura DOUBLE      NOT NULL,
  baterie     INT         NOT NULL,
  din_buffer  BOOLEAN     NOT NULL,
  INDEX idx_pozitii_masina_ora (masina_id, ora_masinii),
  CONSTRAINT fk_pozitii_masina FOREIGN KEY (masina_id) REFERENCES masini (id)
);

CREATE TABLE IF NOT EXISTS alarme (
  id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  masina_id   VARCHAR(20)  NOT NULL,
  tip         VARCHAR(30)  NOT NULL,
  sursa       VARCHAR(10)  NOT NULL,
  ora         DATETIME(3)  NOT NULL,
  ora_sosirii DATETIME(3)  NOT NULL,
  detalii     VARCHAR(255) NULL,
  vazuta      BOOLEAN      NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_alarme_masina FOREIGN KEY (masina_id) REFERENCES masini (id)
);

CREATE TABLE IF NOT EXISTS comenzi (
  id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  masina_id       VARCHAR(20)  NOT NULL,
  tip             VARCHAR(30)  NOT NULL,
  parametri       JSON         NOT NULL,
  ora_trimiterii  DATETIME(3)  NOT NULL,
  ora_confirmarii DATETIME(3)  NULL,
  stare           VARCHAR(12)  NOT NULL,
  motiv           VARCHAR(255) NULL,
  CONSTRAINT fk_comenzi_masina FOREIGN KEY (masina_id) REFERENCES masini (id)
);

CREATE TABLE IF NOT EXISTS evenimente_retea (
  id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tinta       VARCHAR(20)  NOT NULL,
  linie       VARCHAR(200) NOT NULL,
  raspuns     VARCHAR(200) NOT NULL,
  ora_start   DATETIME(3)  NOT NULL,
  ora_sfarsit DATETIME(3)  NULL
);
```

- [ ] **Pasul 6: Verifică build-ul, după ce Tudor spune da la descărcare**

Claude îl întreabă: „La primul build, Maven aduce Spring Boot 4.1.1 și HiveMQ de
pe Maven Central, vreo 40 MB. E bine?”. Dacă spune da:

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`. Testele simulatorului trec ca înainte, iar
serverul n-are încă teste.

- [ ] **Pasul 7: Pornește serverul și verifică tabelele**

Serviciul MySQL rulează. Terminalul S:

```powershell
.\mvnw.cmd -q -pl server spring-boot:run
```

Se așteaptă: `Tomcat started on port 8080`, apoi `Started ServerApplication in
… seconds`. Alt terminal:

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
& $mysql -h 127.0.0.1 -u flota -pflota flota -e "SHOW TABLES"
```

Se așteaptă: `alarme`, `comenzi`, `evenimente_retea`, `masini`, `pozitii` și
`zone`. Oprește serverul cu Ctrl+C, apoi `Y`. Pornește-l încă o dată și verifică
că pornește fără erori. Asta înseamnă că `CREATE TABLE IF NOT EXISTS` poate rula
de mai multe ori.

- [ ] **Pasul 8: Claude explică.** Analogia: Spring Boot e un administrator de
  clădire. Citește `application.properties` ca pe un caiet de sarcini, deschide
  ușa pe 8080, leagă țeava spre MySQL și rulează `schema.sql` ca pe o listă de
  verificare la deschidere. Apoi:
  - ce e un bean: un obiect pe care Spring îl creează o dată și îl dă oricui îl
    cere în constructor;
  - de ce `Clock` e bean;
  - de ce schema e scrisă de mână (D19): tabelele sînt exact cele din spec, iar
    SQL-ul se vede;
  - de ce `netty.version` e fixat (D20).

- [ ] **Pasul 9: Commit**

```bash
git add pom.xml server
git commit -m "Server: Spring Boot porneste si creeaza tabelele

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Zonele și flota, citite din `date\` și încărcate în bază

**Cine scrie:**
- **Zonele și încărcarea:** Claude, ca exemplu explicat. E primul GeoJSON citit
  de server și primul `INSERT` care poate rula de mai multe ori fără să dubleze
  ceva.
- **Flota:** Tudor, ca repetiție a lui `Flota` din simulator, cu mai puține
  câmpuri.

**Fișiere:**
- Creează, în `server\src\main\java\ro\flota\server\date\`:
  - `Punct.java`, `ZonaDefinita.java`, `MasinaDefinita.java`, `EroareDate.java`;
  - `CititorDate.java`, `RegistruFlota.java`, `IncarcareDate.java`.
- Test: `server\src\test\java\ro\flota\server\date\CititorDateTest.java`

**Interfețe:**
- Consumă: `JdbcClient`, `JsonMapper` și proprietatea `flota.dosar-date`, din
  Task 2.
- Produce:
  - `record Punct(double lat, double lon)`;
  - `record ZonaDefinita(String id, String nume, List<Punct> poligon, int vitezaMaxima)`;
  - `record MasinaDefinita(String id, String zona)`;
  - `EroareDate`, o excepție de tip `RuntimeException` al cărei mesaj începe cu
    numele fișierului;
  - `CititorDate(JsonMapper)`, cu `List<ZonaDefinita> zone(Path)` și
    `List<MasinaDefinita> flota(Path, Set<String> zone)`;
  - bean-ul `RegistruFlota`, cu `void incarca(List<ZonaDefinita>, List<MasinaDefinita>)`,
    `boolean cunoscuta(String id)`, `Set<String> masini()` și
    `Optional<ZonaDefinita> zonaMasinii(String id)`;
  - `IncarcareDate`, un `ApplicationRunner` cu `@Order(1)`, care rulează
    înaintea conectării la MQTT.

- [ ] **Pasul 1: Scrie cele patru tipuri simple**

`Punct.java`:

```java
package ro.flota.server.date;

/** Un punct pe hartă, în grade. Atenție: GeoJSON scrie [lon, lat], invers. */
public record Punct(double lat, double lon) {
}
```

`ZonaDefinita.java`:

```java
package ro.flota.server.date;

import java.util.List;

/** O zonă din date\zone.json: poligonul e inelul exterior, ca listă de [lat, lon]. */
public record ZonaDefinita(String id, String nume, List<Punct> poligon, int vitezaMaxima) {
}
```

`MasinaDefinita.java`:

```java
package ro.flota.server.date;

/** O mașină din date\flota.json, așa cum o vede serverul: doar id-ul și zona ei. */
public record MasinaDefinita(String id, String zona) {
}
```

`EroareDate.java`:

```java
package ro.flota.server.date;

import java.nio.file.Path;

/** Un fișier din date\ e greșit. Mesajul începe mereu cu numele fișierului. */
public final class EroareDate extends RuntimeException {

    public EroareDate(Path fisier, String motiv) {
        super(fisier.getFileName() + ": " + motiv);
    }
}
```

- [ ] **Pasul 2: Scrie testele care pică pentru zone**

`server\src\test\java\ro\flota\server\date\CititorDateTest.java`:

```java
package ro.flota.server.date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class CititorDateTest {

    private static final String ZONE = """
            {"type": "FeatureCollection", "features": [
              {"type": "Feature", "properties": {"id": "z1", "nume": "Centru", "viteza_maxima": 50},
               "geometry": {"type": "Polygon", "coordinates": [[
                 [23.00, 46.00], [23.01, 46.00], [23.01, 46.01], [23.00, 46.01], [23.00, 46.00]]]}}
            ]}""";

    @TempDir
    Path dosar;

    private final CititorDate cititor = new CititorDate(JsonMapper.builder().build());

    private Path scrie(String fisier, String continut) throws IOException {
        Path p = dosar.resolve(fisier);
        Files.writeString(p, continut);
        return p;
    }

    private String eroareZone(String continut) throws IOException {
        Path p = scrie("zone.json", continut);
        return assertThrows(EroareDate.class, () -> cititor.zone(p)).getMessage();
    }

    @Test
    void citesteZonaCuCoordonateleIntoarse() throws IOException {
        List<ZonaDefinita> zone = cititor.zone(scrie("zone.json", ZONE));
        assertEquals(1, zone.size());
        ZonaDefinita z = zone.get(0);
        assertEquals("z1", z.id());
        assertEquals("Centru", z.nume());
        assertEquals(50, z.vitezaMaxima());
        assertEquals(5, z.poligon().size());
        assertEquals(new Punct(46.00, 23.00), z.poligon().get(0));
        assertEquals(new Punct(46.00, 23.01), z.poligon().get(1));
    }

    @Test
    void acceptaZoneSalvateCuBom() throws IOException {
        assertEquals(1, cititor.zone(scrie("zone.json", "\uFEFF" + ZONE)).size());
    }

    @Test
    void unElementCareNuEPoligonEsteRefuzat() throws IOException {
        String mesaj = eroareZone(ZONE.replace("\"Polygon\"", "\"LineString\""));
        assertTrue(mesaj.startsWith("zone.json") && mesaj.contains("Polygon"), mesaj);
    }

    @Test
    void vitezaMaximaScrisaCaTextEsteRefuzata() throws IOException {
        String mesaj = eroareZone(ZONE.replace("\"viteza_maxima\": 50", "\"viteza_maxima\": \"50\""));
        assertTrue(mesaj.contains("z1") && mesaj.contains("viteza_maxima"), mesaj);
    }

    @Test
    void poligonulCuDouaColturiEsteRefuzat() throws IOException {
        String doarDoua = ZONE.replace("[23.01, 46.01], [23.00, 46.01], ", "");
        assertTrue(eroareZone(doarDoua).contains("3 colțuri"));
    }

    @Test
    void jsonStricatSpuneFisierul() throws IOException {
        String mesaj = eroareZone("{\"type\": ");
        assertTrue(mesaj.startsWith("zone.json") && mesaj.contains("nu e JSON valid"), mesaj);
    }

    @Test
    void zonelePropriiSeIncarca() {
        List<ZonaDefinita> zone = cititor.zone(Path.of("..", "date", "zone.json"));
        assertFalse(zone.isEmpty());
        for (ZonaDefinita z : zone) {
            assertTrue(z.vitezaMaxima() > 0, z.id());
        }
    }
}
```

- [ ] **Pasul 3: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `CititorDate`.

- [ ] **Pasul 4: Scrie `CititorDate`, cu zonele**

`server\src\main\java\ro\flota\server\date\CititorDate.java`:

```java
package ro.flota.server.date;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Citește date\zone.json (GeoJSON, D18) și date\flota.json. Coordonatele ies ca [lat, lon]. */
public final class CititorDate {

    private final JsonMapper json;

    public CititorDate(JsonMapper json) {
        this.json = json;
    }

    public List<ZonaDefinita> zone(Path fisier) {
        JsonNode radacina = citesteJson(fisier);
        JsonNode elemente = radacina.path("features");
        if (!eText(radacina.path("type"), "FeatureCollection") || !elemente.isArray() || elemente.isEmpty()) {
            throw new EroareDate(fisier, "trebuie să fie un FeatureCollection GeoJSON cu cel puțin o zonă");
        }
        List<ZonaDefinita> zone = new ArrayList<>();
        Set<String> iduri = new HashSet<>();
        int nr = 0;
        for (JsonNode element : elemente) {
            nr++;
            JsonNode geometrie = element.path("geometry");
            if (!eText(geometrie.path("type"), "Polygon")) {
                throw new EroareDate(fisier, "elementul nr. " + nr + " nu e Polygon; zonele se desenează cu poligonul");
            }
            JsonNode proprietati = element.path("properties");
            String id = text(proprietati, "id", fisier);
            if (!iduri.add(id)) {
                throw new EroareDate(fisier, "zona \"" + id + "\" apare de două ori");
            }
            JsonNode viteza = proprietati.path("viteza_maxima");
            if (!viteza.isIntegralNumber() || viteza.asInt() <= 0) {
                throw new EroareDate(fisier,
                        "zona \"" + id + "\": \"viteza_maxima\" trebuie să fie un număr întreg pozitiv, scris fără ghilimele");
            }
            // Doar inelul exterior, coordinates[0]; găurile din poligon nu se folosesc.
            List<Punct> inel = new ArrayList<>();
            for (JsonNode c : geometrie.path("coordinates").path(0)) {
                if (!c.isArray() || c.size() < 2 || !c.get(0).isNumber() || !c.get(1).isNumber()) {
                    throw new EroareDate(fisier, "zona \"" + id + "\" are o coordonată greșită");
                }
                inel.add(new Punct(c.get(1).asDouble(), c.get(0).asDouble())); // GeoJSON: [lon, lat]
            }
            if (inel.size() < 4) { // un inel închis repetă primul punct la capăt
                throw new EroareDate(fisier, "zona \"" + id + "\" are mai puțin de 3 colțuri");
            }
            zone.add(new ZonaDefinita(id, text(proprietati, "nume", fisier), List.copyOf(inel), viteza.asInt()));
        }
        return zone;
    }

    private JsonNode citesteJson(Path fisier) {
        String continut;
        try {
            continut = Files.readString(fisier);
        } catch (IOException e) {
            throw new EroareDate(fisier, "nu pot citi fișierul (" + e.getClass().getSimpleName() + ")");
        }
        if (continut.startsWith("\uFEFF")) {
            continut = continut.substring(1); // BOM-ul pus de PowerShell 5.1
        }
        try {
            return json.readTree(continut);
        } catch (JacksonException e) {
            throw new EroareDate(fisier, "nu e JSON valid: " + e.getOriginalMessage());
        }
    }

    private static String text(JsonNode nod, String camp, Path fisier) {
        JsonNode valoare = nod.path(camp);
        if (!valoare.isString() || valoare.asString().isBlank()) {
            throw new EroareDate(fisier, "lipsește câmpul text \"" + camp + "\"");
        }
        return valoare.asString();
    }

    private static boolean eText(JsonNode nod, String asteptat) {
        return nod.isString() && nod.asString().equals(asteptat);
    }
}
```

- [ ] **Pasul 5: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`. Dacă pică doar `zonelePropriiSeIncarca`, problema e
în `date\zone.json`, de la faza 0, iar mesajul spune motivul. Se repară datele,
nu testul.

- [ ] **Pasul 6: Scrie testele care pică pentru flotă**

Adaugă în `CititorDateTest`, după constanta `ZONE`:

```java
    private static final String FLOTA = """
            {"masini": [
              {"id": "masina-1", "traseu": "t1", "start": 0.0, "zona": "z1"},
              {"id": "masina-2", "traseu": "t1", "start": 0.5, "zona": "z1"}
            ]}""";

    private static final Set<String> DOAR_Z1 = Set.of("z1");
```

și, la sfârșitul clasei:

```java
    private String eroareFlota(String continut) throws IOException {
        Path p = scrie("flota.json", continut);
        return assertThrows(EroareDate.class, () -> cititor.flota(p, DOAR_Z1)).getMessage();
    }

    @Test
    void citesteIdSiZona() throws IOException {
        List<MasinaDefinita> flota = cititor.flota(scrie("flota.json", FLOTA), DOAR_Z1);
        assertEquals(List.of(new MasinaDefinita("masina-1", "z1"), new MasinaDefinita("masina-2", "z1")), flota);
    }

    @Test
    void zonaNecunoscutaEstePusaPeNume() throws IOException {
        String mesaj = eroareFlota(FLOTA.replaceFirst("\"zona\": \"z1\"", "\"zona\": \"z9\""));
        assertTrue(mesaj.startsWith("flota.json") && mesaj.contains("masina-1") && mesaj.contains("z9"), mesaj);
    }

    @Test
    void idDublatEsteRefuzat() throws IOException {
        assertTrue(eroareFlota(FLOTA.replace("masina-2", "masina-1")).contains("de două ori"));
    }

    @Test
    void flotaFaraMasiniEsteRefuzata() throws IOException {
        assertTrue(eroareFlota("{\"masini\": []}").contains("masini"));
    }

    @Test
    void flotaProprieSeIncarca() {
        Set<String> zone = new HashSet<>();
        cititor.zone(Path.of("..", "date", "zone.json")).forEach(z -> zone.add(z.id()));
        assertEquals(10, cititor.flota(Path.of("..", "date", "flota.json"), zone).size());
    }
```

- [ ] **Pasul 7: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru metoda `flota`.

- [ ] **Pasul 8: Tudor scrie `CititorDate.flota`.** Indicii:
  - e aceeași formă ca `Flota.citeste` din simulator, scrisă în faza 1, doar că
    păstrează doar `id` și `zona`;
  - ajutoarele `citesteJson` și `text` există deja în clasă;
  - zona trebuie să fie în mulțimea primită, altfel eroarea pomenește mașina și
    zona;
  - lista goală sau lipsă dă o eroare care pomenește `"masini"`.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

În `CititorDate.java`, după metoda `zone`:

```java
    public List<MasinaDefinita> flota(Path fisier, Set<String> zone) {
        JsonNode masini = citesteJson(fisier).path("masini");
        if (!masini.isArray() || masini.isEmpty()) {
            throw new EroareDate(fisier, "lipsește lista \"masini\" sau e goală");
        }
        List<MasinaDefinita> rezultat = new ArrayList<>();
        Set<String> iduri = new HashSet<>();
        for (JsonNode m : masini) {
            String id = text(m, "id", fisier);
            if (!iduri.add(id)) {
                throw new EroareDate(fisier, "id-ul \"" + id + "\" apare de două ori");
            }
            String zona = text(m, "zona", fisier);
            if (!zone.contains(zona)) {
                throw new EroareDate(fisier, id + " cere zona \"" + zona + "\", care nu există în zone.json");
            }
            rezultat.add(new MasinaDefinita(id, zona));
        }
        return rezultat;
    }
```

</details>

- [ ] **Pasul 9: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu toate testele din `CititorDateTest` trecute.

- [ ] **Pasul 10: Claude revizuiește `flota`,** fără să rescrie.

- [ ] **Pasul 11: Claude scrie `RegistruFlota` și `IncarcareDate`**

`RegistruFlota.java`:

```java
package ro.flota.server.date;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Ce știe serverul despre flotă, în memorie: ce mașini există și în ce zonă e fiecare.
 * Se umple o dată, la pornire; după aceea doar se citește, din mai multe fire.
 */
@Component
public class RegistruFlota {

    private volatile Map<String, MasinaDefinita> masini = Map.of();
    private volatile Map<String, ZonaDefinita> zone = Map.of();

    public void incarca(List<ZonaDefinita> zoneNoi, List<MasinaDefinita> masiniNoi) {
        Map<String, ZonaDefinita> z = new LinkedHashMap<>();
        zoneNoi.forEach(zona -> z.put(zona.id(), zona));
        Map<String, MasinaDefinita> m = new LinkedHashMap<>();
        masiniNoi.forEach(masina -> m.put(masina.id(), masina));
        this.zone = Map.copyOf(z);
        this.masini = Map.copyOf(m);
    }

    public boolean cunoscuta(String id) {
        return masini.containsKey(id);
    }

    public Set<String> masini() {
        return masini.keySet();
    }

    public Optional<ZonaDefinita> zonaMasinii(String id) {
        MasinaDefinita m = masini.get(id);
        return m == null ? Optional.empty() : Optional.ofNullable(zone.get(m.zona()));
    }
}
```

`IncarcareDate.java`:

```java
package ro.flota.server.date;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * La pornire: citește zone.json și flota.json, le scrie în bază și umple registrul.
 * Rulează primul (@Order(1)), ca mesajele MQTT să găsească mașinile în bază.
 * O greșeală în date oprește serverul, cu fișierul și motivul.
 */
@Component
@Order(1)
public class IncarcareDate implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IncarcareDate.class);

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final RegistruFlota registru;
    private final Path dosar;

    public IncarcareDate(JdbcClient jdbc, JsonMapper json, RegistruFlota registru,
                         @Value("${flota.dosar-date}") String dosar) {
        this.jdbc = jdbc;
        this.json = json;
        this.registru = registru;
        this.dosar = Path.of(dosar);
    }

    @Override
    public void run(ApplicationArguments args) {
        CititorDate cititor = new CititorDate(json);
        List<ZonaDefinita> zone = cititor.zone(dosar.resolve("zone.json"));
        Set<String> iduriZone = zone.stream().map(ZonaDefinita::id).collect(Collectors.toSet());
        List<MasinaDefinita> masini = cititor.flota(dosar.resolve("flota.json"), iduriZone);

        // „AS nou ... ON DUPLICATE KEY UPDATE”: dacă rândul există, se actualizează în loc să se dubleze.
        for (ZonaDefinita z : zone) {
            jdbc.sql("""
                    INSERT INTO zone (id, nume, poligon, viteza_maxima) VALUES (:id, :nume, :poligon, :viteza) AS nou
                    ON DUPLICATE KEY UPDATE nume = nou.nume, poligon = nou.poligon, viteza_maxima = nou.viteza_maxima""")
                    .param("id", z.id())
                    .param("nume", z.nume())
                    .param("poligon", json.writeValueAsString(z.poligon().stream().map(p -> List.of(p.lat(), p.lon())).toList()))
                    .param("viteza", z.vitezaMaxima())
                    .update();
        }
        // La mașini se schimbă doar zona; starea și contoarele rămân cum erau.
        for (MasinaDefinita m : masini) {
            jdbc.sql("""
                    INSERT INTO masini (id, zona_id) VALUES (:id, :zona) AS nou
                    ON DUPLICATE KEY UPDATE zona_id = nou.zona_id""")
                    .param("id", m.id())
                    .param("zona", m.zona())
                    .update();
        }
        registru.incarca(zone, masini);
        log.info("Am încărcat {} zone și {} mașini din {}", zone.size(), masini.size(), dosar.toAbsolutePath().normalize());
    }
}
```

- [ ] **Pasul 12: Verifică încărcarea, de două ori**

Pornește serverul ca la Task 2, pasul 7. Se așteaptă în log:
`Am încărcat 1 zone și 10 mașini din …\RC-Proiect\date`, cu numărul de zone
desenate în faza 0. Apoi:

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
& $mysql -h 127.0.0.1 -u flota -pflota flota -e "SELECT COUNT(*) FROM masini; SELECT id, nume, viteza_maxima, JSON_LENGTH(poligon) AS colturi FROM zone"
```

Se așteaptă: `10`, apoi câte un rând pe zonă. Pune la mână un contor, ca să vezi
că a doua încărcare nu-l atinge:

```powershell
& $mysql -h 127.0.0.1 -u flota -pflota flota -e "UPDATE masini SET pierdute = 7 WHERE id = 'masina-1'"
```

Repornește serverul și rulează din nou primul `SELECT`, plus
`SELECT pierdute FROM masini WHERE id = 'masina-1'`. Se așteaptă: tot `10`, iar
contorul a rămas `7`. Pune-l înapoi la 0 cu același `UPDATE`, scris cu `0`.

- [ ] **Pasul 13: Claude explică.** Analogia: registrul din memorie e lista de
  pe biroul dispecerului, iar baza e arhiva. La începutul turei, dispecerul
  copiază lista din dosarul `date\` și verifică arhiva: mașinile noi se adaugă,
  iar cele vechi își păstrează istoricul. Apoi:
  - de ce `ON DUPLICATE KEY UPDATE` face încărcarea idempotentă;
  - de ce `@Order(1)`;
  - de ce o eroare în date oprește serverul, în loc să pornească pe jumătate.

- [ ] **Pasul 14: Commit-urile.** Întâi cel al lui Tudor, fiindcă
  `IncarcareDate` folosește `CititorDate` și altfel primul commit n-ar compila.
  El conține și partea de zone a lui Claude, fiindcă fișierul și testul sînt
  comune, iar corpul mesajului o spune.

```bash
git add server/src/main/java/ro/flota/server/date/Punct.java server/src/main/java/ro/flota/server/date/ZonaDefinita.java server/src/main/java/ro/flota/server/date/MasinaDefinita.java server/src/main/java/ro/flota/server/date/EroareDate.java server/src/main/java/ro/flota/server/date/CititorDate.java server/src/test/java/ro/flota/server/date/CititorDateTest.java
git commit -m "Server: citirea zonelor si a flotei

Zonele: Claude. Flota: Tudor."
git add server/src/main/java/ro/flota/server/date/RegistruFlota.java server/src/main/java/ro/flota/server/date/IncarcareDate.java
git commit -m "Server: registrul flotei si incarcarea idempotenta

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Decodorul mesajelor MQTT

**Cine scrie:**
- **Topic-ul și telemetria:** Claude, ca exemplu explicat. Tiparul e „nu crede
  nimic din ce vine din rețea până nu verifici”.
- **Alarma și starea:** Tudor, ca repetiție, cu alt canal și alte câmpuri.

**Fișiere:**
- Creează, în `server\src\main\java\ro\flota\server\mqtt\`: `MesajPrimit.java`,
  `MesajInvalid.java` și `Decodor.java`
- Test: `server\src\test\java\ro\flota\server\mqtt\DecodorTest.java`

**Interfețe:**
- Consumă: `RegistruFlota.cunoscuta`, `RegistruFlota.incarca`, `MasinaDefinita`
  (Task 3) și `JsonMapper`.
- Produce:
  - `sealed interface MesajPrimit`, cu `String masinaId()` și cu record-urile
    interne `Telemetrie(String masinaId, long seq, Instant oraMasinii, double lat, double lon, double viteza, double temperatura, int baterie, boolean dinBuffer)`,
    `Alarma(String masinaId, String tip, Instant ora, double valoare)` și
    `Stare(String masinaId, boolean online)`;
  - `MesajInvalid extends Exception`, al cărei mesaj e motivul;
  - bean-ul `Decodor(JsonMapper, RegistruFlota)`, cu
    `MesajPrimit decodeaza(String topic, byte[] payload) throws MesajInvalid`.

- [ ] **Pasul 1: Scrie `MesajPrimit` și `MesajInvalid`**

`MesajPrimit.java`:

```java
package ro.flota.server.mqtt;

import java.time.Instant;

/** Un mesaj de la o mașină, deja verificat de Decodor. Orice altceva nu trece mai departe. */
public sealed interface MesajPrimit {

    String masinaId();

    /** Ce trimite mașina o dată pe secundă (spec §4.2). */
    record Telemetrie(String masinaId, long seq, Instant oraMasinii, double lat, double lon,
                      double viteza, double temperatura, int baterie, boolean dinBuffer) implements MesajPrimit {
    }

    /** O alarmă dată de mașină: SUPRAINCALZIRE sau FRANARE_BRUSCA. */
    record Alarma(String masinaId, String tip, Instant ora, double valoare) implements MesajPrimit {
    }

    /** online sau offline; offline poate veni și de la broker, din Last Will. */
    record Stare(String masinaId, boolean online) implements MesajPrimit {
    }
}
```

`MesajInvalid.java`:

```java
package ro.flota.server.mqtt;

/** Un mesaj care nu poate fi folosit. Se scrie în log și se ignoră (spec §6.1). */
public final class MesajInvalid extends Exception {

    public MesajInvalid(String motiv) {
        super(motiv);
    }
}
```

- [ ] **Pasul 2: Scrie testele care pică pentru topic și telemetrie**

`DecodorTest.java`:

```java
package ro.flota.server.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.server.date.MasinaDefinita;
import ro.flota.server.date.RegistruFlota;
import tools.jackson.databind.json.JsonMapper;

class DecodorTest {

    private static final String TELEMETRIE = """
            {"seq": 1042, "ora": "2026-12-10T10:15:03.120Z", "lat": 46.77121, "lon": 23.6236,
             "viteza": 47.5, "temperatura": 92.1, "baterie": 81, "din_buffer": false}""";

    private final Decodor decodor;

    DecodorTest() {
        RegistruFlota registru = new RegistruFlota();
        registru.incarca(List.of(), List.of(new MasinaDefinita("masina-3", "z1")));
        decodor = new Decodor(JsonMapper.builder().build(), registru);
    }

    private MesajPrimit decodeaza(String topic, String payload) throws MesajInvalid {
        return decodor.decodeaza(topic, payload.getBytes(StandardCharsets.UTF_8));
    }

    private String motiv(String topic, String payload) {
        return assertThrows(MesajInvalid.class, () -> decodeaza(topic, payload)).getMessage();
    }

    @Test
    void telemetriaBunaDevineUnRecord() throws MesajInvalid {
        assertEquals(new MesajPrimit.Telemetrie("masina-3", 1042, Instant.parse("2026-12-10T10:15:03.120Z"),
                        46.77121, 23.6236, 47.5, 92.1, 81, false),
                decodeaza("flota/masina-3/telemetrie", TELEMETRIE));
    }

    @Test
    void jsonStricatEsteRefuzat() {
        assertEquals("nu e JSON valid", motiv("flota/masina-3/telemetrie", "{\"seq\": 1"));
    }

    @Test
    void textulSimpluNuEJson() {
        assertEquals("nu e JSON valid", motiv("flota/masina-3/telemetrie", "nu e json"));
    }

    @Test
    void campulLipsaEstePusPeNume() {
        String mesaj = motiv("flota/masina-3/telemetrie", TELEMETRIE.replace("\"viteza\": 47.5,", ""));
        assertTrue(mesaj.contains("viteza"), mesaj);
    }

    @Test
    void unNumarScrisCaTextEsteRefuzat() {
        String mesaj = motiv("flota/masina-3/telemetrie", TELEMETRIE.replace("\"seq\": 1042", "\"seq\": \"1042\""));
        assertTrue(mesaj.contains("seq"), mesaj);
    }

    @Test
    void oraGresitaEsteRefuzata() {
        String mesaj = motiv("flota/masina-3/telemetrie", TELEMETRIE.replace("2026-12-10T10:15:03.120Z", "ieri"));
        assertTrue(mesaj.contains("ISO-8601"), mesaj);
    }

    @Test
    void masinaNecunoscutaEsteRefuzata() {
        assertEquals("mașină necunoscută: masina-99", motiv("flota/masina-99/telemetrie", TELEMETRIE));
    }

    @Test
    void topicStrainEsteRefuzat() {
        assertEquals("topic necunoscut: test/salut", motiv("test/salut", "merge"));
    }

    @Test
    void canalNecunoscutEsteRefuzat() {
        assertEquals("canal necunoscut: radio", motiv("flota/masina-3/radio", "{}"));
    }
}
```

- [ ] **Pasul 3: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Decodor`.

- [ ] **Pasul 4: Scrie `Decodor`, cu topic-ul și telemetria**

`Decodor.java`:

```java
package ro.flota.server.mqtt;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;
import ro.flota.server.date.RegistruFlota;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Transformă un mesaj MQTT, adică topic plus octeți, într-un MesajPrimit verificat.
 * Tot ce nu se potrivește aruncă MesajInvalid, cu motivul: serverul nu crede nimic din rețea până nu verifică.
 */
@Component
public class Decodor {

    private final JsonMapper json;
    private final RegistruFlota registru;

    public Decodor(JsonMapper json, RegistruFlota registru) {
        this.json = json;
        this.registru = registru;
    }

    public MesajPrimit decodeaza(String topic, byte[] payload) throws MesajInvalid {
        String[] parti = topic.split("/");
        if (parti.length != 3 || !parti[0].equals("flota")) {
            throw new MesajInvalid("topic necunoscut: " + topic);
        }
        String id = parti[1];
        if (!registru.cunoscuta(id)) {
            throw new MesajInvalid("mașină necunoscută: " + id);
        }
        return switch (parti[2]) {
            case "telemetrie" -> telemetrie(id, obiect(payload));
            default -> throw new MesajInvalid("canal necunoscut: " + parti[2]);
        };
    }

    private MesajPrimit.Telemetrie telemetrie(String id, JsonNode j) throws MesajInvalid {
        return new MesajPrimit.Telemetrie(id, intreg(j, "seq"), ora(j, "ora"), numar(j, "lat"), numar(j, "lon"),
                numar(j, "viteza"), numar(j, "temperatura"), (int) intreg(j, "baterie"), adevarat(j, "din_buffer"));
    }

    private JsonNode obiect(byte[] payload) throws MesajInvalid {
        JsonNode j;
        try {
            j = json.readTree(payload);
        } catch (JacksonException e) {
            throw new MesajInvalid("nu e JSON valid");
        }
        if (j == null || !j.isObject()) {
            throw new MesajInvalid("nu e un obiect JSON");
        }
        return j;
    }

    private static double numar(JsonNode j, String camp) throws MesajInvalid {
        JsonNode v = j.get(camp);
        if (v == null || !v.isNumber()) {
            throw new MesajInvalid("lipsește câmpul numeric " + camp);
        }
        return v.asDouble();
    }

    private static long intreg(JsonNode j, String camp) throws MesajInvalid {
        JsonNode v = j.get(camp);
        if (v == null || !v.isIntegralNumber()) {
            throw new MesajInvalid("lipsește câmpul întreg " + camp);
        }
        return v.asLong();
    }

    private static boolean adevarat(JsonNode j, String camp) throws MesajInvalid {
        JsonNode v = j.get(camp);
        if (v == null || !v.isBoolean()) {
            throw new MesajInvalid("lipsește câmpul true/false " + camp);
        }
        return v.asBoolean();
    }

    private static String text(JsonNode j, String camp) throws MesajInvalid {
        JsonNode v = j.get(camp);
        if (v == null || !v.isString()) {
            throw new MesajInvalid("lipsește câmpul text " + camp);
        }
        return v.asString();
    }

    private static Instant ora(JsonNode j, String camp) throws MesajInvalid {
        String t = text(j, camp);
        try {
            return Instant.parse(t);
        } catch (DateTimeParseException e) {
            throw new MesajInvalid(camp + " nu e o oră ISO-8601: " + t);
        }
    }
}
```

- [ ] **Pasul 5: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 9 teste din `DecodorTest` trecute.

- [ ] **Pasul 6: Claude explică.** Analogia: decodorul e portarul
  dispeceratului. Se uită pe plic (topic-ul), verifică dacă expeditorul e pe
  listă și deschide scrisoarea doar ca să vadă că are toate rubricile. Ce nu
  trece de el nu ajunge la dispecer. Apoi:
  - ce e o interfață `sealed`: lista mesajelor posibile e închisă, iar
    compilatorul știe toate cazurile;
  - de ce `MesajInvalid` e o excepție verificată: cine cheamă decodorul e
    obligat să trateze mesajul stricat.

- [ ] **Pasul 7: Commit-ul lui Claude**

```bash
git add server/src/main/java/ro/flota/server/mqtt server/src/test/java/ro/flota/server/mqtt
git commit -m "Server: decodorul mesajelor, topic si telemetrie

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Pasul 8: Scrie testele care pică pentru alarmă și stare**

Adaugă în `DecodorTest`:

```java
    @Test
    void alarmaBunaDevineUnRecord() throws MesajInvalid {
        assertEquals(new MesajPrimit.Alarma("masina-3", "SUPRAINCALZIRE", Instant.parse("2026-12-10T10:15:03.120Z"), 112.4),
                decodeaza("flota/masina-3/alarma",
                        "{\"tip\": \"SUPRAINCALZIRE\", \"ora\": \"2026-12-10T10:15:03.120Z\", \"valoare\": 112.4}"));
    }

    @Test
    void tipulDeAlarmaNecunoscutEsteRefuzat() {
        String mesaj = motiv("flota/masina-3/alarma",
                "{\"tip\": \"INCENDIU\", \"ora\": \"2026-12-10T10:15:03.120Z\", \"valoare\": 1}");
        assertTrue(mesaj.contains("INCENDIU"), mesaj);
    }

    @Test
    void stareaOnlineSiOffline() throws MesajInvalid {
        assertEquals(new MesajPrimit.Stare("masina-3", true), decodeaza("flota/masina-3/stare", "online"));
        assertEquals(new MesajPrimit.Stare("masina-3", false), decodeaza("flota/masina-3/stare", "offline"));
    }

    @Test
    void stareaGoalaEsteRefuzata() {
        assertTrue(motiv("flota/masina-3/stare", "").contains("stare necunoscută"));
    }
```

- [ ] **Pasul 9: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: 4 teste picate, cu `canal necunoscut: alarma` și
`canal necunoscut: stare`.

- [ ] **Pasul 10: Tudor scrie alarma și starea.** Indicii:
  - în `decodeaza`, switch-ul primește două cazuri noi, `"alarma"` și `"stare"`;
  - alarma e JSON: `tip`, `ora` și `valoare`, iar tipul trebuie să fie
    `SUPRAINCALZIRE` sau `FRANARE_BRUSCA`. Ajutoarele `text`, `ora` și `numar`
    există deja;
  - starea nu e JSON, ci textul simplu `online` sau `offline`, în UTF-8. Orice
    altceva, inclusiv textul gol, aruncă `MesajInvalid` cu
    `stare necunoscută: "<text>"`. Textul gol apare când cineva șterge un mesaj
    reținut.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

Importuri noi: `java.nio.charset.StandardCharsets` și `java.util.Set`.

Constanta, la începutul clasei:

```java
    static final Set<String> TIPURI_ALARMA = Set.of("SUPRAINCALZIRE", "FRANARE_BRUSCA");
```

Switch-ul din `decodeaza`:

```java
        return switch (parti[2]) {
            case "telemetrie" -> telemetrie(id, obiect(payload));
            case "alarma" -> alarma(id, obiect(payload));
            case "stare" -> stare(id, payload);
            default -> throw new MesajInvalid("canal necunoscut: " + parti[2]);
        };
```

Cele două metode:

```java
    private static MesajPrimit.Alarma alarma(String id, JsonNode j) throws MesajInvalid {
        String tip = text(j, "tip");
        if (!TIPURI_ALARMA.contains(tip)) {
            throw new MesajInvalid("tip de alarmă necunoscut: " + tip);
        }
        return new MesajPrimit.Alarma(id, tip, ora(j, "ora"), numar(j, "valoare"));
    }

    private static MesajPrimit.Stare stare(String id, byte[] payload) throws MesajInvalid {
        String text = new String(payload, StandardCharsets.UTF_8);
        return switch (text) {
            case "online" -> new MesajPrimit.Stare(id, true);
            case "offline" -> new MesajPrimit.Stare(id, false);
            default -> throw new MesajInvalid("stare necunoscută: \"" + text + "\"");
        };
    }
```

</details>

- [ ] **Pasul 11: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 13 teste din `DecodorTest` trecute.

- [ ] **Pasul 12: Claude revizuiește alarma și starea,** fără să rescrie.

- [ ] **Pasul 13: Commit-ul lui Tudor**

```bash
git add server/src/main/java/ro/flota/server/mqtt/Decodor.java server/src/test/java/ro/flota/server/mqtt/DecodorTest.java
git commit -m "Server: decodorul pentru alarma si stare"
```

---

### Task 5: Canalul live, pe WebSocket

**Cine scrie:** Claude, ca exemplu explicat. E prima jumătate a tiparului 6 din
spec §9, adică trimiterea pe WebSocket.

**Fișiere:**
- Creează, în `server\src\main\java\ro\flota\server\live\`: `MesajLive.java`,
  `CanalLive.java` și `ConfigWebSocket.java`
- Test: `server\src\test\java\ro\flota\server\live\CanalLiveTest.java`

**Interfețe:**
- Consumă: `JsonMapper`.
- Produce:
  - `record MesajLive(String tip, Object date)`;
  - bean-ul `CanalLive`, cu `void trimite(String tip, Object date)` și
    `int numarSesiuni()`;
  - endpoint-ul `ws://127.0.0.1:8080/ws`, care acceptă originile
    `http://localhost:*` și `http://127.0.0.1:*`.

- [ ] **Pasul 1: Scrie testele care pică**

`CanalLiveTest.java`:

```java
package ro.flota.server.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class CanalLiveTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final CanalLive canal = new CanalLive(json);

    /** O sesiune falsă, făcută de Mockito: răspunde doar la ce îi spunem. */
    private static WebSocketSession sesiune(String id) {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getId()).thenReturn(id);
        when(s.isOpen()).thenReturn(true);
        return s;
    }

    @Test
    void trimiteTipulSiDatele() throws Exception {
        WebSocketSession s = sesiune("1");
        canal.afterConnectionEstablished(s);
        canal.trimite("STARE", Map.of("masinaId", "masina-3", "stare", "ONLINE"));

        ArgumentCaptor<TextMessage> trimis = ArgumentCaptor.forClass(TextMessage.class);
        verify(s).sendMessage(trimis.capture());
        JsonNode j = json.readTree(trimis.getValue().getPayload());
        assertEquals("STARE", j.get("tip").asString());
        assertEquals("masina-3", j.get("date").get("masinaId").asString());
    }

    @Test
    void oSesiuneMoartaEsteScoasaFaraSaLeStricePeCelelalte() throws Exception {
        WebSocketSession moarta = sesiune("1");
        doThrow(new IOException("conexiune închisă")).when(moarta).sendMessage(any());
        WebSocketSession vie = sesiune("2");
        canal.afterConnectionEstablished(moarta);
        canal.afterConnectionEstablished(vie);

        canal.trimite("STARE", Map.of());
        canal.trimite("STARE", Map.of());

        assertEquals(1, canal.numarSesiuni());
        verify(vie, times(2)).sendMessage(any());
    }

    @Test
    void inchidereaScoateSesiunea() throws Exception {
        WebSocketSession s = sesiune("1");
        canal.afterConnectionEstablished(s);
        canal.afterConnectionClosed(s, CloseStatus.NORMAL);
        assertEquals(0, canal.numarSesiuni());
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `CanalLive`.

- [ ] **Pasul 3: Scrie `MesajLive` și `CanalLive`**

`MesajLive.java`:

```java
package ro.flota.server.live;

/** Forma oricărui mesaj live (spec §6.6): {"tip": "...", "date": {...}}. */
public record MesajLive(String tip, Object date) {
}
```

`CanalLive.java`:

```java
package ro.flota.server.live;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * Canalul live spre UI (spec §6.6): un handler simplu, fără STOMP și fără token.
 * Serverul doar trimite; ce scrie browserul pe canal se ignoră.
 */
@Component
public class CanalLive extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CanalLive.class);
    private static final int TIMP_MAXIM_TRIMITERE_MS = 2_000;
    private static final int COADA_MAXIMA_OCTETI = 512 * 1024;

    private final JsonMapper json;
    private final Set<WebSocketSession> sesiuni = ConcurrentHashMap.newKeySet();

    public CanalLive(JsonMapper json) {
        this.json = json;
    }

    /**
     * O sesiune WebSocket nu suportă două trimiteri în același timp, iar aici trimit mai multe fire.
     * Decoratorul pune mesajele la rând și închide un browser care rămâne prea mult în urmă.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession sesiune) {
        sesiuni.add(new ConcurrentWebSocketSessionDecorator(sesiune, TIMP_MAXIM_TRIMITERE_MS, COADA_MAXIMA_OCTETI));
        log.info("UI conectat ({} conexiuni)", sesiuni.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession sesiune, CloseStatus stare) {
        sesiuni.removeIf(s -> s.getId().equals(sesiune.getId()));
    }

    public void trimite(String tip, Object date) {
        TextMessage mesaj = new TextMessage(json.writeValueAsString(new MesajLive(tip, date)));
        for (WebSocketSession s : sesiuni) {
            try {
                s.sendMessage(mesaj);
            } catch (IOException | RuntimeException e) {
                sesiuni.remove(s);
                inchideLinistit(s);
                log.info("Scot o conexiune UI moartă: {}", e.getMessage());
            }
        }
    }

    public int numarSesiuni() {
        return sesiuni.size();
    }

    private static void inchideLinistit(WebSocketSession s) {
        try {
            s.close(CloseStatus.SERVER_ERROR);
        } catch (IOException | RuntimeException e) {
            // Era deja închisă.
        }
    }
}
```

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 3 teste din `CanalLiveTest` trecute.

- [ ] **Pasul 5: Scrie `ConfigWebSocket`**

```java
package ro.flota.server.live;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Pune CanalLive pe /ws. Originile acceptate sînt doar paginile de pe laptop, inclusiv Vite pe 5173. */
@Configuration
@EnableWebSocket
public class ConfigWebSocket implements WebSocketConfigurer {

    private final CanalLive canal;

    public ConfigWebSocket(CanalLive canal) {
        this.canal = canal;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(canal, "/ws").setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }
}
```

- [ ] **Pasul 6: Verifică că serverul pornește cu tot cu `/ws`**

Comanda: `.\mvnw.cmd -q -pl server spring-boot:run`
Se așteaptă: `Started ServerApplication`, fără erori. Canalul se încearcă în
browser la Task 7, după ce există un endpoint REST de deschis.

- [ ] **Pasul 7: Claude explică.** Analogia: WebSocket-ul e un apel telefonic
  care rămâne deschis, spre deosebire de REST, unde fiecare cerere e o scrisoare
  cu răspuns. Dispecerul vorbește, iar ecranele doar ascultă. Apoi:
  - de ce decoratorul e necesar: două fire nu au voie să vorbească deodată în
    același telefon;
  - ce face testul cu Mockito: o sesiune falsă, căreia îi spui dinainte ce să
    facă, inclusiv să cadă;
  - de ce o sesiune moartă se scoate pe loc.

- [ ] **Pasul 8: Commit**

```bash
git add server/src/main/java/ro/flota/server/live server/src/test/java/ro/flota/server/live
git commit -m "Server: canalul live pe WebSocket

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Primirea mesajelor, de la MQTT la MySQL și pe canalul live

**Cine scrie:** Claude, ca exemplu explicat. E tiparul 6 din spec §9, întreg:
abonarea MQTT în Spring, salvarea și trimiterea pe WebSocket. Repetițiile lui
Tudor vin în Task 7 și în UI.

**Fișiere:**
- Creează, în `server\src\main\java\ro\flota\server\flota\`:
  - `Ore.java`;
  - `PozitieDto.java`, `MasinaDto.java`, `AlarmaDto.java`, `StareMasinaDto.java`;
  - `Pozitii.java`, `Masini.java`, `Alarme.java`, `Dispecer.java`.
- Creează: `server\src\main\java\ro\flota\server\mqtt\ClientMqtt.java`
- Creează: `docs\mesaje-test\alarma.json`

**Interfețe:**
- Consumă: `MesajPrimit`, `Decodor`, `MesajInvalid` (Task 4), `CanalLive`
  (Task 5), `JdbcClient` și `Clock` (Task 2).
- Produce:
  - `record PozitieDto(String masinaId, long seq, Instant oraMasinii, Instant oraSosirii, double lat, double lon, double viteza, double temperatura, int baterie, boolean dinBuffer)`,
    cu `static PozitieDto din(MesajPrimit.Telemetrie, Instant sosire)`;
  - `record MasinaDto(String id, String zonaId, String stare, Instant ultimaVedere, long recuperate, long pierdute, PozitieDto ultimaPozitie)`;
  - `record AlarmaDto(long id, String masinaId, String tip, String sursa, Instant ora, Instant oraSosirii, String detalii, boolean vazuta)`;
  - `record StareMasinaDto(String masinaId, String stare, Instant ultimaVedere)`,
    unde `stare` e `ONLINE` sau `OFFLINE`;
  - repo-urile, toate bean-uri:
    - `Pozitii`, cu `void salveaza(PozitieDto)`,
      `Optional<PozitieDto> ultima(String masinaId)` și
      `List<PozitieDto> intre(String masinaId, Instant de, Instant pana)`;
    - `Masini`, cu `void vazuta(String id, Instant ora)`,
      `void schimbaStare(String id, boolean online, Instant ora)`,
      `List<MasinaDto> toate()` și `Optional<MasinaDto> una(String id)`;
    - `Alarme`, cu `AlarmaDto salveaza(String masinaId, String tip, String sursa, Instant ora, Instant sosire, String detalii)`;
  - bean-ul `Dispecer`, cu `void primeste(MesajPrimit)`;
  - bean-ul `ClientMqtt`, un `ApplicationRunner` cu `@Order(2)`;
  - pe canalul live: `POZITIE` cu un `PozitieDto`, `STARE` cu un
    `StareMasinaDto` și `ALARMA` cu un `AlarmaDto`.

- [ ] **Pasul 1: Scrie `Ore` și DTO-urile**

`Ore.java`:

```java
package ro.flota.server.flota;

import java.sql.Timestamp;
import java.time.Instant;

/** Trecerea orelor între Java și MySQL. Conexiunea e pe UTC, deci în bază ajunge ora UTC. */
final class Ore {

    private Ore() {
    }

    static Timestamp ts(Instant ora) {
        return ora == null ? null : Timestamp.from(ora);
    }

    static Instant instant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
```

`PozitieDto.java`:

```java
package ro.flota.server.flota;

import java.time.Instant;
import ro.flota.server.mqtt.MesajPrimit;

/** O poziție, așa cum o văd baza și UI-ul: are ambele ore, a mașinii și a sosirii (spec §4). */
public record PozitieDto(String masinaId, long seq, Instant oraMasinii, Instant oraSosirii, double lat, double lon,
                         double viteza, double temperatura, int baterie, boolean dinBuffer) {

    public static PozitieDto din(MesajPrimit.Telemetrie t, Instant sosire) {
        return new PozitieDto(t.masinaId(), t.seq(), t.oraMasinii(), sosire, t.lat(), t.lon(),
                t.viteza(), t.temperatura(), t.baterie(), t.dinBuffer());
    }
}
```

`MasinaDto.java`:

```java
package ro.flota.server.flota;

import java.time.Instant;

/** O mașină din registru, cu ultima poziție știută, ca să apară pe hartă și când e offline (D23). */
public record MasinaDto(String id, String zonaId, String stare, Instant ultimaVedere,
                        long recuperate, long pierdute, PozitieDto ultimaPozitie) {

    MasinaDto cuPozitie(PozitieDto pozitie) {
        return new MasinaDto(id, zonaId, stare, ultimaVedere, recuperate, pierdute, pozitie);
    }
}
```

`AlarmaDto.java`:

```java
package ro.flota.server.flota;

import java.time.Instant;

/** O alarmă din tabelul alarme. sursa e MASINA sau SERVER. */
public record AlarmaDto(long id, String masinaId, String tip, String sursa, Instant ora, Instant oraSosirii,
                        String detalii, boolean vazuta) {
}
```

`StareMasinaDto.java`:

```java
package ro.flota.server.flota;

import java.time.Instant;

/** Ce pleacă pe canalul live când o mașină devine ONLINE sau OFFLINE. */
public record StareMasinaDto(String masinaId, String stare, Instant ultimaVedere) {
}
```

- [ ] **Pasul 2: Scrie repo-urile**

`Pozitii.java`:

```java
package ro.flota.server.flota;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Tabelul pozitii: o linie pe mesaj de telemetrie. */
@Repository
public class Pozitii {

    private static final String COLOANE =
            "masina_id, seq, ora_masinii, ora_sosirii, lat, lon, viteza, temperatura, baterie, din_buffer";

    private final JdbcClient jdbc;

    public Pozitii(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void salveaza(PozitieDto p) {
        jdbc.sql("INSERT INTO pozitii (" + COLOANE + ") VALUES "
                        + "(:masina, :seq, :oraMasinii, :oraSosirii, :lat, :lon, :viteza, :temperatura, :baterie, :dinBuffer)")
                .param("masina", p.masinaId())
                .param("seq", p.seq())
                .param("oraMasinii", Ore.ts(p.oraMasinii()))
                .param("oraSosirii", Ore.ts(p.oraSosirii()))
                .param("lat", p.lat())
                .param("lon", p.lon())
                .param("viteza", p.viteza())
                .param("temperatura", p.temperatura())
                .param("baterie", p.baterie())
                .param("dinBuffer", p.dinBuffer())
                .update();
    }

    /** Ultima poziție după ora mașinii, nu după ora sosirii: o poziție recuperată nu e „ultima”. */
    public Optional<PozitieDto> ultima(String masinaId) {
        return jdbc.sql("SELECT " + COLOANE + " FROM pozitii WHERE masina_id = :id "
                        + "ORDER BY ora_masinii DESC, seq DESC LIMIT 1")
                .param("id", masinaId)
                .query(Pozitii::rand)
                .optional();
    }

    public List<PozitieDto> intre(String masinaId, Instant de, Instant pana) {
        return jdbc.sql("SELECT " + COLOANE + " FROM pozitii WHERE masina_id = :id "
                        + "AND ora_masinii BETWEEN :de AND :pana ORDER BY ora_masinii, seq")
                .param("id", masinaId)
                .param("de", Ore.ts(de))
                .param("pana", Ore.ts(pana))
                .query(Pozitii::rand)
                .list();
    }

    static PozitieDto rand(ResultSet rs, int nr) throws SQLException {
        return new PozitieDto(rs.getString("masina_id"), rs.getLong("seq"),
                Ore.instant(rs.getTimestamp("ora_masinii")), Ore.instant(rs.getTimestamp("ora_sosirii")),
                rs.getDouble("lat"), rs.getDouble("lon"), rs.getDouble("viteza"), rs.getDouble("temperatura"),
                rs.getInt("baterie"), rs.getBoolean("din_buffer"));
    }
}
```

`Masini.java`:

```java
package ro.flota.server.flota;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Tabelul masini: starea și contoarele fiecărei mașini. Rândurile le creează IncarcareDate. */
@Repository
public class Masini {

    private final JdbcClient jdbc;
    private final Pozitii pozitii;

    public Masini(JdbcClient jdbc, Pozitii pozitii) {
        this.jdbc = jdbc;
        this.pozitii = pozitii;
    }

    public void vazuta(String id, Instant ora) {
        jdbc.sql("UPDATE masini SET ultima_vedere = :ora WHERE id = :id")
                .param("ora", Ore.ts(ora))
                .param("id", id)
                .update();
    }

    public void schimbaStare(String id, boolean online, Instant ora) {
        jdbc.sql("UPDATE masini SET stare = :stare, ultima_vedere = :ora WHERE id = :id")
                .param("stare", online ? "ONLINE" : "OFFLINE")
                .param("ora", Ore.ts(ora))
                .param("id", id)
                .update();
    }

    /** În ordinea numerelor: masina-2 înainte de masina-10. „masina-” are 7 caractere. */
    public List<MasinaDto> toate() {
        return jdbc.sql("SELECT * FROM masini ORDER BY CAST(SUBSTRING(id, 8) AS UNSIGNED)")
                .query(Masini::rand)
                .list()
                .stream()
                .map(m -> m.cuPozitie(pozitii.ultima(m.id()).orElse(null)))
                .toList();
    }

    public Optional<MasinaDto> una(String id) {
        return jdbc.sql("SELECT * FROM masini WHERE id = :id")
                .param("id", id)
                .query(Masini::rand)
                .optional()
                .map(m -> m.cuPozitie(pozitii.ultima(id).orElse(null)));
    }

    private static MasinaDto rand(ResultSet rs, int nr) throws SQLException {
        return new MasinaDto(rs.getString("id"), rs.getString("zona_id"), rs.getString("stare"),
                Ore.instant(rs.getTimestamp("ultima_vedere")), rs.getLong("recuperate"), rs.getLong("pierdute"), null);
    }
}
```

`Alarme.java`:

```java
package ro.flota.server.flota;

import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** Tabelul alarme. */
@Repository
public class Alarme {

    private final JdbcClient jdbc;

    public Alarme(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Salvează alarma și întoarce rândul, cu id-ul dat de MySQL. */
    public AlarmaDto salveaza(String masinaId, String tip, String sursa, Instant ora, Instant sosire, String detalii) {
        KeyHolder cheie = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO alarme (masina_id, tip, sursa, ora, ora_sosirii, detalii) "
                        + "VALUES (:masina, :tip, :sursa, :ora, :sosire, :detalii)")
                .param("masina", masinaId)
                .param("tip", tip)
                .param("sursa", sursa)
                .param("ora", Ore.ts(ora))
                .param("sosire", Ore.ts(sosire))
                .param("detalii", detalii)
                .update(cheie);
        return new AlarmaDto(cheie.getKey().longValue(), masinaId, tip, sursa, ora, sosire, detalii, false);
    }
}
```

- [ ] **Pasul 3: Scrie `Dispecer`**

```java
package ro.flota.server.flota;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import ro.flota.server.live.CanalLive;
import ro.flota.server.mqtt.MesajPrimit;

/**
 * Ce face serverul cu fiecare mesaj (spec §6.3). Rulează pe firul unic al lui ClientMqtt,
 * deci mesajele vin pe rând, în ordinea în care au sosit.
 */
@Service
public class Dispecer {

    private final Pozitii pozitii;
    private final Masini masini;
    private final Alarme alarme;
    private final CanalLive live;
    private final Clock ceas;

    public Dispecer(Pozitii pozitii, Masini masini, Alarme alarme, CanalLive live, Clock ceas) {
        this.pozitii = pozitii;
        this.masini = masini;
        this.alarme = alarme;
        this.live = live;
        this.ceas = ceas;
    }

    /** Interfața e sealed, deci compilatorul verifică că fiecare fel de mesaj are un caz. */
    public void primeste(MesajPrimit mesaj) {
        switch (mesaj) {
            case MesajPrimit.Telemetrie t -> laTelemetrie(t);
            case MesajPrimit.Alarma a -> laAlarma(a);
            case MesajPrimit.Stare s -> laStare(s);
        }
    }

    private void laTelemetrie(MesajPrimit.Telemetrie t) {
        Instant sosire = ceas.instant();
        PozitieDto pozitie = PozitieDto.din(t, sosire);
        pozitii.salveaza(pozitie);
        masini.vazuta(t.masinaId(), sosire);
        live.trimite("POZITIE", pozitie);
    }

    /** QoS 2 garantează că alarma vine o singură dată, deci nu caut dubluri (spec §6.3). */
    private void laAlarma(MesajPrimit.Alarma a) {
        AlarmaDto alarma = alarme.salveaza(a.masinaId(), a.tip(), "MASINA", a.ora(), ceas.instant(), detalii(a));
        live.trimite("ALARMA", alarma);
    }

    private void laStare(MesajPrimit.Stare s) {
        Instant acum = ceas.instant();
        masini.schimbaStare(s.masinaId(), s.online(), acum);
        live.trimite("STARE", new StareMasinaDto(s.masinaId(), s.online() ? "ONLINE" : "OFFLINE", acum));
    }

    static String detalii(MesajPrimit.Alarma a) {
        return switch (a.tip()) {
            case "SUPRAINCALZIRE" -> String.format(Locale.ROOT, "temperatura a ajuns la %.1f °C", a.valoare());
            case "FRANARE_BRUSCA" -> String.format(Locale.ROOT, "viteza a scăzut cu %.1f km/h într-o secundă", a.valoare());
            default -> String.format(Locale.ROOT, "valoarea %.1f", a.valoare());
        };
    }
}
```

- [ ] **Pasul 4: Scrie `ClientMqtt`**

```java
package ro.flota.server.mqtt;

import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ro.flota.server.flota.Dispecer;

/**
 * Singura clasă din server care știe de HiveMQ (D17, R4). Clientul „dispecer” se conectează direct la
 * Mosquitto, cu sesiune persistentă și reconectare automată (D11). Pornește după IncarcareDate (@Order(2)).
 */
@Component
@Order(2)
public class ClientMqtt implements ApplicationRunner, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ClientMqtt.class);

    private final Decodor decodor;
    private final Dispecer dispecer;
    /** Un singur fir: mesajele se procesează pe rând, în ordinea sosirii (D22). */
    private final ExecutorService fir = Executors.newSingleThreadExecutor(r -> new Thread(r, "mqtt-dispecer"));
    private final Mqtt3AsyncClient client;

    public ClientMqtt(Decodor decodor, Dispecer dispecer,
                      @Value("${flota.mqtt.host}") String host, @Value("${flota.mqtt.port}") int port) {
        this.decodor = decodor;
        this.dispecer = dispecer;
        this.client = Mqtt3Client.builder()
                .identifier("dispecer")
                .serverHost(host)
                .serverPort(port)
                .automaticReconnectWithDefaultConfig()
                .addConnectedListener(context -> {
                    log.info("Conectat la Mosquitto");
                    aboneaza();
                })
                .addDisconnectedListener(context ->
                        log.warn("Deconectat de la Mosquitto ({}); reîncerc singur", context.getCause().getMessage()))
                .buildAsync();
        // Filtrul global prinde și mesajele păstrate în sesiunea persistentă, care sosesc imediat după CONNACK (D22).
        client.publishes(MqttGlobalPublishFilter.ALL, this::primeste, fir);
    }

    /** Conectarea e asincronă: dacă Mosquitto e oprit, serverul pornește oricum și reîncearcă singur. */
    @Override
    public void run(ApplicationArguments args) {
        client.connectWith().cleanSession(false).keepAlive(30).send();
    }

    /** La fiecare conectare: Mosquitto fără persistență uită abonările când repornește (D22). */
    private void aboneaza() {
        client.subscribeWith()
                .addSubscription().topicFilter("flota/+/telemetrie").qos(MqttQos.AT_MOST_ONCE).applySubscription()
                .addSubscription().topicFilter("flota/+/alarma").qos(MqttQos.EXACTLY_ONCE).applySubscription()
                .addSubscription().topicFilter("flota/+/stare").qos(MqttQos.AT_LEAST_ONCE).applySubscription()
                .send()
                .whenComplete((ack, eroare) -> {
                    if (eroare != null) {
                        log.warn("Abonarea a eșuat: {}", eroare.getMessage());
                    }
                });
    }

    /** Un mesaj stricat se notează și se ignoră; orice altă eroare nu oprește ascultarea (spec §6.1). */
    private void primeste(Mqtt3Publish publicare) {
        String topic = publicare.getTopic().toString();
        try {
            dispecer.primeste(decodor.decodeaza(topic, publicare.getPayloadAsBytes()));
        } catch (MesajInvalid e) {
            log.warn("Ignor mesajul de pe {}: {}", topic, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Eroare la mesajul de pe {}; ascult mai departe", topic, e);
        }
    }

    @Override
    public void destroy() {
        client.disconnect();
        fir.shutdown();
    }
}
```

- [ ] **Pasul 5: Scrie `docs\mesaje-test\alarma.json`**

PowerShell 5.1 strică ghilimelele dintr-un JSON dat ca argument unui program,
așa că mesajele de test stau în fișiere și se trimit cu `mosquitto_pub -f`.

```json
{"tip": "SUPRAINCALZIRE", "ora": "2026-12-10T10:15:03.120Z", "valoare": 112.4}
```

- [ ] **Pasul 6: Verifică build-ul și testele**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`.

- [ ] **Pasul 7: Verifică drumul întreg, cu patru terminale**

- **A:** Mosquitto, ca la faza 0: `& "C:\Program Files\mosquitto\mosquitto.exe" -c mosquitto\mosquitto.conf -v`.
- **S:** serverul: `.\mvnw.cmd -q -pl server spring-boot:run`. Se așteaptă
  `Conectat la Mosquitto`.
- **C:** simulatorul: `.\mvnw.cmd -q -pl simulator compile exec:java`.
- **D:** verificările.

După 30 de secunde, în D:

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
& $mysql -h 127.0.0.1 -u flota -pflota flota -e "SELECT masina_id, COUNT(*) AS pozitii, MAX(seq) AS seq FROM pozitii GROUP BY masina_id; SELECT id, stare, ultima_vedere FROM masini"
```

Se așteaptă: câte un rând pe mașină, cu vreo 30 de poziții, apoi 10 mașini
`ONLINE`, cu `ultima_vedere` în ultimele secunde, în UTC.

- [ ] **Pasul 8: Verifică alarma și mesajul stricat**

În D:

```powershell
& "C:\Program Files\mosquitto\mosquitto_pub.exe" -h 127.0.0.1 -t flota/masina-3/alarma -q 2 -f docs\mesaje-test\alarma.json
& "C:\Program Files\mosquitto\mosquitto_pub.exe" -h 127.0.0.1 -t flota/masina-3/telemetrie -m "nu e json"
& $mysql -h 127.0.0.1 -u flota -pflota flota -e "SELECT id, masina_id, tip, sursa, detalii FROM alarme"
```

Se așteaptă:
- în baza, un rând `masina-3 SUPRAINCALZIRE MASINA temperatura a ajuns la 112.4 °C`;
- în S, `Ignor mesajul de pe flota/masina-3/telemetrie: nu e JSON valid`;
- serverul primește mai departe, iar `COUNT(*)` din pasul 7 crește.

- [ ] **Pasul 9: Verifică starea offline**

Ctrl+C în C, apoi `Y`. Rulează din nou al doilea `SELECT` din pasul 7. Se
așteaptă: 10 mașini `OFFLINE`.

- [ ] **Pasul 10: Claude explică.** Analogia: `ClientMqtt` e receptorul stației
  radio. Ascultă pe toate canalele odată, `flota/+/...`, și dă fiecare mesaj
  portarului, adică decodorului. Ce trece ajunge la dispecer, care scrie în
  registru și strigă pe ecrane. Apoi:
  - de ce un singur fir procesează mesajele (D22): ordinea se păstrează și nimic
    nu se bate pe aceeași mașină;
  - de ce filtrul global e pus înainte de conectare;
  - de ce serverul folosește reconectarea bibliotecii, iar mașina nu (D8 față de
    D11);
  - ce înseamnă `+` într-un topic.

- [ ] **Pasul 11: Commit**

```bash
git add server/src docs/mesaje-test
git commit -m "Server: primirea mesajelor, de la MQTT la MySQL si pe canalul live

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: REST pentru UI

**Cine scrie:**
- **Intervalul, erorile și `MasiniController`:** Claude, ca exemplu explicat.
- **Zonele, adică `Zone` și `ZoneController`:** Tudor, ca repetiție a unui
  repo și a unui controller.

**Fișiere:**
- Creează, în `server\src\main\java\ro\flota\server\api\`:
  - `Interval.java`, `CerereGresita.java`, `Negasit.java`, `EroriApi.java`;
  - `MasiniController.java`, `ZoneController.java`.
- Creează: `server\src\main\java\ro\flota\server\flota\ZonaDto.java`, `Zone.java`
- Test: `server\src\test\java\ro\flota\server\api\IntervalTest.java`

**Interfețe:**
- Consumă: `Masini`, `Pozitii`, `MasinaDto`, `PozitieDto` (Task 6), `Clock`,
  `JdbcClient` și `JsonMapper`.
- Produce:
  - `record Interval(Instant de, Instant pana)`, cu
    `static Interval din(String de, String pana, Clock ceas)`;
  - excepțiile `CerereGresita` (400) și `Negasit` (404), ambele
    `RuntimeException`, transformate de `EroriApi` în `{"status": …, "mesaj": …}`;
  - endpoint-urile 1, 2, 3 și 8 din spec §6.5;
  - `record ZonaDto(String id, String nume, List<List<Double>> poligon, int vitezaMaxima)`,
    unde poligonul e o listă de `[lat, lon]`.

- [ ] **Pasul 1: Scrie testele care pică pentru interval**

`IntervalTest.java`:

```java
package ro.flota.server.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class IntervalTest {

    private static final Clock CEAS = Clock.fixed(Instant.parse("2026-12-10T10:15:00Z"), ZoneOffset.UTC);

    private static Instant ora(String text) {
        return Instant.parse(text);
    }

    private static String eroare(String de, String pana) {
        return assertThrows(CerereGresita.class, () -> Interval.din(de, pana, CEAS)).getMessage();
    }

    @Test
    void implicitSintUltimele5Minute() {
        assertEquals(new Interval(ora("2026-12-10T10:10:00Z"), ora("2026-12-10T10:15:00Z")), Interval.din(null, null, CEAS));
    }

    @Test
    void campurileGoaleInseamnaImplicit() {
        assertEquals(Interval.din(null, null, CEAS), Interval.din("", " ", CEAS));
    }

    @Test
    void doarDeInseamnaPanaAcum() {
        assertEquals(new Interval(ora("2026-12-10T10:00:00Z"), ora("2026-12-10T10:15:00Z")),
                Interval.din("2026-12-10T10:00:00Z", null, CEAS));
    }

    @Test
    void doarPanaInseamnaCu5MinuteInainte() {
        assertEquals(new Interval(ora("2026-12-10T08:55:00Z"), ora("2026-12-10T09:00:00Z")),
                Interval.din(null, "2026-12-10T09:00:00Z", CEAS));
    }

    @Test
    void deDupaPanaEsteRefuzat() {
        assertTrue(eroare("2026-12-10T10:00:00Z", "2026-12-10T09:00:00Z").contains("înainte"));
    }

    @Test
    void oraScrisaGresitEstePusaPeNume() {
        String mesaj = eroare("ieri", null);
        assertTrue(mesaj.contains("de") && mesaj.contains("ieri"), mesaj);
    }

    @Test
    void maiMultDe6OreEsteRefuzat() {
        assertTrue(eroare("2026-12-10T04:14:59Z", "2026-12-10T10:15:00Z").contains("6 ore"));
    }

    @Test
    void exact6OreEsteAcceptat() {
        assertEquals(new Interval(ora("2026-12-10T04:15:00Z"), ora("2026-12-10T10:15:00Z")),
                Interval.din("2026-12-10T04:15:00Z", "2026-12-10T10:15:00Z", CEAS));
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Interval` și
`CerereGresita`.

- [ ] **Pasul 3: Scrie excepțiile și `Interval`**

`CerereGresita.java`:

```java
package ro.flota.server.api;

/** Cererea are o greșeală pe care UI-ul o poate repara. Devine 400. */
public final class CerereGresita extends RuntimeException {

    public CerereGresita(String mesaj) {
        super(mesaj);
    }
}
```

`Negasit.java`:

```java
package ro.flota.server.api;

/** Ce s-a cerut nu există. Devine 404. */
public final class Negasit extends RuntimeException {

    public Negasit(String mesaj) {
        super(mesaj);
    }
}
```

`Interval.java`:

```java
package ro.flota.server.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/** Intervalul pentru traseu (endpoint-ul 3): implicit ultimele 5 minute, cel mult 6 ore. */
public record Interval(Instant de, Instant pana) {

    static final Duration IMPLICIT = Duration.ofMinutes(5);
    static final Duration MAXIM = Duration.ofHours(6);

    public static Interval din(String de, String pana, Clock ceas) {
        Instant p = gol(pana) ? ceas.instant() : ora("pana", pana);
        Instant d = gol(de) ? p.minus(IMPLICIT) : ora("de", de);
        if (!d.isBefore(p)) {
            throw new CerereGresita("„de” trebuie să fie înainte de „pana”");
        }
        if (Duration.between(d, p).compareTo(MAXIM) > 0) {
            throw new CerereGresita("intervalul e mai lung de 6 ore");
        }
        return new Interval(d, p);
    }

    private static boolean gol(String text) {
        return text == null || text.isBlank();
    }

    private static Instant ora(String camp, String text) {
        try {
            return Instant.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new CerereGresita("„" + camp + "” nu e o oră ISO-8601 în UTC: " + text);
        }
    }
}
```

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 8 teste din `IntervalTest` trecute.

- [ ] **Pasul 5: Scrie `EroriApi` și `MasiniController`**

`EroriApi.java`:

```java
package ro.flota.server.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Toate erorile ies la fel, ca JSON {status, mesaj}, cu codul potrivit. */
@RestControllerAdvice
public class EroriApi {

    public record Eroare(int status, String mesaj) {
    }

    @ExceptionHandler(CerereGresita.class)
    ResponseEntity<Eroare> cerereGresita(CerereGresita e) {
        return raspuns(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(Negasit.class)
    ResponseEntity<Eroare> negasit(Negasit e) {
        return raspuns(HttpStatus.NOT_FOUND, e.getMessage());
    }

    static ResponseEntity<Eroare> raspuns(HttpStatus status, String mesaj) {
        return ResponseEntity.status(status).body(new Eroare(status.value(), mesaj));
    }
}
```

`MasiniController.java`:

```java
package ro.flota.server.api;

import java.time.Clock;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.flota.MasinaDto;
import ro.flota.server.flota.Masini;
import ro.flota.server.flota.PozitieDto;
import ro.flota.server.flota.Pozitii;

/** Endpoint-urile 1, 2 și 3 din spec §6.5. */
@RestController
@RequestMapping("/api/masini")
public class MasiniController {

    private final Masini masini;
    private final Pozitii pozitii;
    private final Clock ceas;

    public MasiniController(Masini masini, Pozitii pozitii, Clock ceas) {
        this.masini = masini;
        this.pozitii = pozitii;
        this.ceas = ceas;
    }

    @GetMapping
    public List<MasinaDto> toate() {
        return masini.toate();
    }

    @GetMapping("/{id}")
    public MasinaDto una(@PathVariable String id) {
        return masini.una(id).orElseThrow(() -> new Negasit("mașina " + id + " nu există"));
    }

    @GetMapping("/{id}/traseu")
    public List<PozitieDto> traseu(@PathVariable String id,
                                   @RequestParam(required = false) String de,
                                   @RequestParam(required = false) String pana) {
        una(id);
        Interval interval = Interval.din(de, pana, ceas);
        return pozitii.intre(id, interval.de(), interval.pana());
    }
}
```

- [ ] **Pasul 6: Verifică endpoint-urile cu `curl.exe`**

Mosquitto, serverul și simulatorul pornite, ca la Task 6, pasul 7. În D:

```powershell
curl.exe -s http://127.0.0.1:8080/api/masini
curl.exe -s http://127.0.0.1:8080/api/masini/masina-3
(curl.exe -s http://127.0.0.1:8080/api/masini/masina-3/traseu | ConvertFrom-Json).Count
curl.exe -s -i http://127.0.0.1:8080/api/masini/masina-99
curl.exe -s -i "http://127.0.0.1:8080/api/masini/masina-3/traseu?de=ieri"
```

Se așteaptă:
- o listă de 10 mașini, fiecare cu `ultimaPozitie`;
- `masina-3`, singură;
- un număr care crește cu 1 pe secundă până la vreo 300, adică 5 minute;
- `HTTP/1.1 404`, cu `{"status":404,"mesaj":"mașina masina-99 nu există"}`;
- `HTTP/1.1 400`, cu mesajul despre „de”.

- [ ] **Pasul 7: Verifică și canalul live, din browser**

Deschide http://127.0.0.1:8080/api/masini, apoi F12 → Console și scrie:

```js
const ws = new WebSocket('ws://127.0.0.1:8080/ws'); ws.onmessage = (e) => console.log(e.data)
```

Se așteaptă: zece mesaje `{"tip":"POZITIE",…}` pe secundă. Pagina e pe
`127.0.0.1:8080`, deci originea e acceptată. Închide tabul: în S nu apare nicio
eroare.

- [ ] **Pasul 8: Claude explică `MasiniController`.** Analogia: controller-ul e
  ghișeul de informații: primește o întrebare cu adresa ei, de exemplu
  `/api/masini/masina-3`, o trimite la arhivă și dă răspunsul ca JSON. Apoi:
  - cum devine un `record` JSON;
  - de ce erorile ies printr-un singur loc, `EroriApi`;
  - de ce intervalul are limită: 6 ore înseamnă cel mult 21.600 de puncte pe
    hartă.

- [ ] **Pasul 9: Tudor scrie zonele.** Ce trebuie:
  - `ZonaDto`, cu forma din „Interfețe”;
  - `Zone`, un `@Repository` cu `List<ZonaDto> toate()`, ordonate după `id`.
    Coloana `poligon` e text JSON, iar `JsonMapper` îl citește cu un
    `TypeReference<List<List<Double>>>`, din `tools.jackson.core.type`;
  - `ZoneController`, cu `GET /api/zone` (endpoint-ul 8), după forma lui
    `MasiniController`.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

`flota\ZonaDto.java`:

```java
package ro.flota.server.flota;

import java.util.List;

/** O zonă pentru hartă: poligonul e o listă de [lat, lon], exact ce cere Leaflet. */
public record ZonaDto(String id, String nume, List<List<Double>> poligon, int vitezaMaxima) {
}
```

`flota\Zone.java`:

```java
package ro.flota.server.flota;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Tabelul zone, doar de citit (D13). */
@Repository
public class Zone {

    private static final TypeReference<List<List<Double>>> POLIGON = new TypeReference<>() {
    };

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public Zone(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<ZonaDto> toate() {
        return jdbc.sql("SELECT id, nume, poligon, viteza_maxima FROM zone ORDER BY id")
                .query((rs, nr) -> new ZonaDto(rs.getString("id"), rs.getString("nume"),
                        json.readValue(rs.getString("poligon"), POLIGON), rs.getInt("viteza_maxima")))
                .list();
    }
}
```

`api\ZoneController.java`:

```java
package ro.flota.server.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.flota.ZonaDto;
import ro.flota.server.flota.Zone;

/** Endpoint-ul 8 din spec §6.5: zonele, doar de citit. */
@RestController
public class ZoneController {

    private final Zone zone;

    public ZoneController(Zone zone) {
        this.zone = zone;
    }

    @GetMapping("/api/zone")
    public List<ZonaDto> toate() {
        return zone.toate();
    }
}
```

</details>

- [ ] **Pasul 10: Verifică zonele**

Repornește serverul, apoi:

```powershell
curl.exe -s http://127.0.0.1:8080/api/zone
```

Se așteaptă: zonele, fiecare cu `poligon` ca listă de `[lat, lon]`. Primul număr
e în jur de 46 sau 47, nu 23: coordonatele au fost întoarse.

- [ ] **Pasul 11: Claude revizuiește zonele,** fără să rescrie.

- [ ] **Pasul 12: Commit-urile**

```bash
git add server/src/main/java/ro/flota/server/api/Interval.java server/src/main/java/ro/flota/server/api/CerereGresita.java server/src/main/java/ro/flota/server/api/Negasit.java server/src/main/java/ro/flota/server/api/EroriApi.java server/src/main/java/ro/flota/server/api/MasiniController.java server/src/test/java/ro/flota/server/api/IntervalTest.java
git commit -m "Server: REST pentru masini si traseu

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git add server/src/main/java/ro/flota/server/flota/ZonaDto.java server/src/main/java/ro/flota/server/flota/Zone.java server/src/main/java/ro/flota/server/api/ZoneController.java
git commit -m "Server: REST pentru zone"
```

---

### Task 8: UI-ul pornește și vede serverul

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „cum arată o aplicație
Vue creată de mână și cum ajunge o cerere din browser la server prin Vite”.

**Fișiere:**
- Creează, în `web\`: `package.json`, `vite.config.js` și `index.html`
- Creează, în `web\src\`: `main.js`, `api.js` și `App.vue`
- Creat de `npm install`: `web\package-lock.json`

**Interfețe:**
- Consumă: `GET /api/masini` (Task 7).
- Produce:
  - `api`, exportat din `web\src\api.js`, cu `masini()`, `masina(id)`,
    `traseu(id, de, pana)` și `zone()`. Fiecare întoarce un `Promise` cu JSON-ul
    sau aruncă un `Error` cu `mesaj`-ul de la server;
  - serverul de dezvoltare pe `http://127.0.0.1:5173`, care trimite `/api` și
    `/ws` la 8080.

- [ ] **Pasul 1: Scrie `web\package.json`**

Versiunile sînt fixe, fără `^`, verificate pe npm pe 8 octombrie:

```json
{
  "name": "flota-web",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build"
  },
  "dependencies": {
    "@mdi/font": "7.4.47",
    "leaflet": "1.9.4",
    "vue": "3.5.43",
    "vuetify": "4.2.4"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "6.0.9",
    "vite": "8.3.3"
  }
}
```

- [ ] **Pasul 2: Scrie `web\vite.config.js`**

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Browserul vorbește doar cu Vite; Vite trimite /api și /ws la serverul Spring Boot.
// Așa, pentru browser totul vine de la aceeași adresă și nu e nevoie de CORS.
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      '/ws': { target: 'ws://127.0.0.1:8080', ws: true },
    },
  },
})
```

- [ ] **Pasul 3: Scrie `web\index.html` și `web\src\main.js`**

`index.html`:

```html
<!doctype html>
<html lang="ro">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Dispecerat flotă</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

`src\main.js`:

```js
import { createApp } from 'vue'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import 'leaflet/dist/leaflet.css'
import App from './App.vue'

// Toate componentele Vuetify, fără plugin de Vite: aplicația e mică, iar setarea rămâne simplă.
createApp(App).use(createVuetify({ components, directives })).mount('#app')
```

- [ ] **Pasul 4: Scrie `web\src\api.js`**

```js
/** Toate cererile REST trec pe aici. Vite le trimite mai departe la serverul de pe 8080. */
async function cere(cale, optiuni = {}) {
  const raspuns = await fetch(cale, {
    ...optiuni,
    headers: { 'Content-Type': 'application/json', ...(optiuni.headers ?? {}) },
  })
  const corp = raspuns.status === 204 ? null : await raspuns.json().catch(() => null)
  if (!raspuns.ok) {
    throw new Error(corp?.mesaj ?? `serverul a răspuns ${raspuns.status}`)
  }
  return corp
}

export const api = {
  masini: () => cere('/api/masini'),
  masina: (id) => cere(`/api/masini/${id}`),
  traseu: (id, de, pana) => {
    const parametri = new URLSearchParams()
    if (de) parametri.set('de', de)
    if (pana) parametri.set('pana', pana)
    return cere(`/api/masini/${id}/traseu?${parametri}`)
  },
  zone: () => cere('/api/zone'),
}
```

- [ ] **Pasul 5: Scrie `web\src\App.vue`, varianta de pornire**

```vue
<template>
  <v-app>
    <v-app-bar color="primary" density="compact">
      <v-app-bar-title>Dispecerat flotă</v-app-bar-title>
    </v-app-bar>
    <v-main>
      <v-container>
        <v-alert v-if="eroare" type="error">Serverul nu răspunde: {{ eroare }}</v-alert>
        <p v-else>{{ masini.length }} mașini în registru.</p>
      </v-container>
    </v-main>
  </v-app>
</template>

<script>
import { api } from './api.js'

export default {
  name: 'App',
  data() {
    return { masini: [], eroare: null }
  },
  async mounted() {
    try {
      this.masini = await api.masini()
    } catch (e) {
      this.eroare = e.message
    }
  },
}
</script>
```

- [ ] **Pasul 6: `npm install`, după ce Tudor spune da**

Claude îl întreabă: „`npm install` aduce Vue, Vuetify, Leaflet și Vite de pe
registry.npmjs.org, vreo 60 MB în `web\node_modules\`, care e în `.gitignore`. E
bine?”. Dacă spune da, într-un terminal W:

```powershell
cd web
npm install
```

Se așteaptă: `added … packages`, fără `ERR!`. Avertismentele `npm warn` sînt în
regulă. Dacă apare `EPERM` sau `EBUSY`, e riscul R2: pune OneDrive pe pauză și
rulează din nou.

- [ ] **Pasul 7: Pornește UI-ul și verifică**

Serverul pornit în S. În W, tot din `web\`:

```powershell
npm run dev
```

Se așteaptă: `Local: http://127.0.0.1:5173/`. Deschide adresa. Pe pagină apare
bara albastră „Dispecerat flotă” și `10 mașini în registru.` Oprește serverul și
reîncarcă pagina: apare alerta roșie. Pornește serverul la loc.

- [ ] **Pasul 8: Claude explică.** Analogia: Vite e recepția unei clădiri cu
  două firme. Browserul vorbește doar cu recepția, iar recepția trimite
  întrebările despre `/api` la etajul serverului. Apoi:
  - ce e o componentă `.vue`: șablonul, scriptul și stilul la un loc;
  - `data()` și `mounted()`;
  - de ce versiunile sînt fixe;
  - de ce `package-lock.json` intră în git.

- [ ] **Pasul 9: Commit**

```bash
git add web/package.json web/package-lock.json web/vite.config.js web/index.html web/src
git commit -m "UI: Vue cu Vuetify, legat de server prin Vite

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: Harta live

**Cine scrie:**
- **Tabla live și mașinile pe hartă:** Claude, ca exemplu explicat. E tiparul 7
  din spec §9.
- **Zonele pe hartă:** Tudor, ca repetiție: un strat nou, desenat din date
  venite prin REST.

**Fișiere:**
- Creează: `web\src\live.js`
- Creează: `web\src\components\HartaFlotei.vue`, `web\src\pagini\PaginaHarta.vue`
- Modifică: `web\src\App.vue`

**Interfețe:**
- Consumă: `api.masini()` și `api.zone()` (Task 8), plus mesajele `POZITIE` și
  `STARE` de pe `/ws` (Task 6).
- Produce, în `live.js`:
  - `flota`, un obiect reactiv
    `{ conectat: boolean, masini: { [id]: { id, zonaId, stare, ultimaVedere, recuperate, pierdute, pozitie } } }`;
  - `pornesteLive()`, care deschide WebSocket-ul și se reconectează singur la 2
    secunde;
  - `incarcaMasini()`;
  - `asculta(f) : () => void`: `f(tip, date)` se cheamă la fiecare mesaj live,
    iar funcția întoarsă oprește ascultarea.
- Produce, în `HartaFlotei.vue`:
  - prop-urile `zone` (Array) și `selectata` (String);
  - evenimentul `alege(id)`;
  - metoda `culoare(m)`, pe care faza 5 o extinde cu portocaliul.

- [ ] **Pasul 1: Scrie `web\src\live.js`**

```js
import { reactive } from 'vue'
import { api } from './api.js'

/** Tabla dispecerului: ce știe UI-ul acum despre fiecare mașină. O citesc toate componentele. */
export const flota = reactive({
  conectat: false,
  masini: {}, // id → { id, zonaId, stare, ultimaVedere, recuperate, pierdute, pozitie }
})

const ascultatori = new Set()

/** f(tip, date) se cheamă la fiecare mesaj live. Întoarce funcția care oprește ascultarea. */
export function asculta(f) {
  ascultatori.add(f)
  return () => ascultatori.delete(f)
}

export async function incarcaMasini() {
  for (const m of await api.masini()) {
    flota.masini[m.id] = { ...m, pozitie: m.ultimaPozitie }
  }
}

/** Deschide canalul live. Dacă se închide, inclusiv când serverul e oprit, reîncearcă la 2 secunde. */
export function pornesteLive() {
  const ws = new WebSocket(`ws://${location.host}/ws`)
  ws.onopen = () => {
    flota.conectat = true
    // Ce s-a întâmplat cât canalul a fost închis se ia din REST.
    incarcaMasini().catch((e) => console.warn('nu pot încărca mașinile:', e.message))
  }
  ws.onclose = () => {
    flota.conectat = false
    setTimeout(pornesteLive, 2000)
  }
  ws.onmessage = (e) => {
    const { tip, date } = JSON.parse(e.data)
    aplica(tip, date)
    ascultatori.forEach((f) => f(tip, date))
  }
}

/** Orele vin ca text ISO; se compară ca momente, nu ca text: „03Z” și „03.120Z” s-ar încurca. */
function maiNoua(a, b) {
  return !b || Date.parse(a) >= Date.parse(b)
}

function aplica(tip, date) {
  const m = flota.masini[date.masinaId]
  if (!m) return
  if (tip === 'POZITIE') {
    // O poziție recuperată din buffer e mai veche; markerul rămâne pe cea mai nouă.
    if (!m.pozitie || maiNoua(date.oraMasinii, m.pozitie.oraMasinii)) m.pozitie = date
    m.ultimaVedere = date.oraSosirii
  } else if (tip === 'STARE') {
    m.stare = date.stare
    m.ultimaVedere = date.ultimaVedere
  }
}
```

- [ ] **Pasul 2: Scrie `web\src\components\HartaFlotei.vue`, cu mașinile**

```vue
<template>
  <div ref="harta" class="harta"></div>
</template>

<script>
import L from 'leaflet'
import { flota } from '../live.js'

const CULORI = { ONLINE: '#2e7d32', OFFLINE: '#9e9e9e' }

export default {
  name: 'HartaFlotei',
  props: {
    selectata: { type: String, default: null },
  },
  emits: ['alege'],
  data() {
    return { flota }
  },
  watch: {
    'flota.masini': {
      deep: true,
      handler() {
        this.deseneazaMasini()
      },
    },
    selectata() {
      this.deseneazaMasini()
    },
  },
  mounted() {
    // Obiectele Leaflet stau pe `this`, dar nu în data(): Vue nu are voie să le facă reactive.
    this.harta = L.map(this.$refs.harta).setView([45.9, 24.9], 7)
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(this.harta)
    this.markere = {}
    this.deseneazaMasini()
  },
  beforeUnmount() {
    this.harta.remove()
  },
  methods: {
    deseneazaMasini() {
      for (const m of Object.values(flota.masini)) {
        if (!m.pozitie) continue
        const loc = [m.pozitie.lat, m.pozitie.lon]
        let marker = this.markere[m.id]
        if (!marker) {
          marker = L.circleMarker(loc, { radius: 8, weight: 2, fillOpacity: 0.9 })
            .bindTooltip(m.id)
            .on('click', () => this.$emit('alege', m.id))
            .addTo(this.harta)
          this.markere[m.id] = marker
        }
        marker.setLatLng(loc)
        marker.setStyle({
          color: m.id === this.selectata ? '#000000' : '#ffffff',
          fillColor: this.culoare(m),
        })
      }
    },
    culoare(m) {
      return CULORI[m.stare] ?? CULORI.OFFLINE
    },
  },
}
</script>

<style scoped>
.harta {
  height: 100%;
  min-height: 400px;
}
</style>
```

- [ ] **Pasul 3: Scrie `web\src\pagini\PaginaHarta.vue`**

```vue
<template>
  <div class="pagina">
    <HartaFlotei class="harta" :selectata="selectata" @alege="selectata = $event" />
  </div>
</template>

<script>
import HartaFlotei from '../components/HartaFlotei.vue'

export default {
  name: 'PaginaHarta',
  components: { HartaFlotei },
  data() {
    return { selectata: null }
  },
}
</script>

<style scoped>
.pagina {
  display: flex;
  height: calc(100vh - 48px);
}
.harta {
  flex: 1;
}
</style>
```

- [ ] **Pasul 4: Rescrie `web\src\App.vue`**

```vue
<template>
  <v-app>
    <v-app-bar color="primary" density="compact">
      <v-app-bar-title>Dispecerat flotă</v-app-bar-title>
      <template #append>
        <v-chip :color="flota.conectat ? 'success' : 'error'" variant="flat" size="small" class="mr-2">
          {{ flota.conectat ? 'live' : 'fără legătură cu serverul' }}
        </v-chip>
      </template>
    </v-app-bar>
    <v-main>
      <PaginaHarta />
    </v-main>
  </v-app>
</template>

<script>
import PaginaHarta from './pagini/PaginaHarta.vue'
import { flota, pornesteLive } from './live.js'

export default {
  name: 'App',
  components: { PaginaHarta },
  data() {
    return { flota }
  },
  mounted() {
    pornesteLive()
  },
}
</script>
```

- [ ] **Pasul 5: Verifică mașinile pe hartă**

Mosquitto, serverul, simulatorul și Vite pornite. Deschide
http://127.0.0.1:5173. Se așteaptă:
- chip-ul verde `live`;
- harta României, pe care o mărești până la orașul traseelor: zece cercuri
  verzi, fiecare mutându-se o dată pe secundă;
- click pe un cerc îi face marginea neagră.

Ctrl+C în terminalul simulatorului: în câteva secunde, toate cercurile devin
gri și rămân pe loc. Pornește simulatorul la loc: devin verzi.

- [ ] **Pasul 6: Commit-ul lui Claude**

```bash
git add web/src
git commit -m "UI: harta live cu masinile colorate dupa stare

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Pasul 7: Claude explică.** Analogia: `flota` e tabla magnetică din
  dispecerat. WebSocket-ul mută magneții, iar harta și, mai târziu, lista doar se
  uită la tablă. Când tabla se schimbă, Vue redesenează ce depinde de ea. Apoi:
  - `reactive` și `watch` cu `deep`;
  - de ce obiectele Leaflet nu au voie în `data()`: Vue le-ar învălui și Leaflet
    s-ar strica;
  - de ce `circleMarker`: nu are nevoie de imagini, deci nu se strică la
    împachetarea făcută de Vite;
  - de ce canalul se reconectează singur.

- [ ] **Pasul 8: Tudor desenează zonele.** Ce trebuie:
  - **`PaginaHarta`:** câmpul `zone: []` în `data()`, încărcat în `mounted()` cu
    `api.zone()` și dat hărții prin `:zone="zone"`;
  - **`HartaFlotei`:**
    - prop-ul `zone` (Array, implicit `[]`);
    - în `mounted()`, un strat `this.stratZone = L.layerGroup().addTo(this.harta)`,
      creat înainte de primul desen;
    - un `watch` pe `zone` și metoda `deseneazaZone()`, care golește stratul,
      desenează fiecare zonă cu `L.polygon(z.poligon, …)`, albastră și aproape
      transparentă, cu un tooltip care spune numele și viteza maximă;
    - la sfârșit, harta se încadrează pe zone, cu
      `this.harta.fitBounds(L.featureGroup(poligoane).getBounds())`.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

În `PaginaHarta.vue`, șablonul devine:

```vue
    <HartaFlotei class="harta" :zone="zone" :selectata="selectata" @alege="selectata = $event" />
```

iar scriptul:

```js
import HartaFlotei from '../components/HartaFlotei.vue'
import { api } from '../api.js'

export default {
  name: 'PaginaHarta',
  components: { HartaFlotei },
  data() {
    return { zone: [], selectata: null }
  },
  async mounted() {
    this.zone = await api.zone()
  },
}
```

În `HartaFlotei.vue`, prop-ul:

```js
    zone: { type: Array, default: () => [] },
```

în `watch`:

```js
    zone() {
      this.deseneazaZone()
    },
```

în `mounted()`, după `tileLayer`:

```js
    this.stratZone = L.layerGroup().addTo(this.harta)
    this.deseneazaZone()
```

și metoda:

```js
    deseneazaZone() {
      this.stratZone.clearLayers()
      const poligoane = this.zone.map((z) =>
        L.polygon(z.poligon, { color: '#1565c0', weight: 1, fillOpacity: 0.08 })
          .bindTooltip(`${z.nume}: maxim ${z.vitezaMaxima} km/h`)
          .addTo(this.stratZone),
      )
      if (poligoane.length > 0) {
        this.harta.fitBounds(L.featureGroup(poligoane).getBounds(), { padding: [20, 20] })
      }
    },
```

</details>

- [ ] **Pasul 9: Verifică zonele**

Reîncarcă pagina. Se așteaptă: harta se încadrează singură pe zone, desenate cu
albastru deschis, iar tooltip-ul spune numele și viteza maximă.

- [ ] **Pasul 10: Claude revizuiește zonele,** fără să rescrie.

- [ ] **Pasul 11: Commit-ul lui Tudor**

```bash
git add web/src
git commit -m "UI: zonele pe harta"
```

---

### Task 10: Lista, panoul mașinii și traseul

**Cine scrie:**
- **Lista, traseul și legarea lor în pagină:** Claude, ca exemplu explicat.
- **Panoul mașinii:** Tudor, ca repetiție: o componentă care doar afișează
  date din tabla live.

**Fișiere:**
- Creează: `web\src\components\ListaMasini.vue`, `PanouMasina.vue`
- Modifică: `web\src\components\HartaFlotei.vue`, `web\src\pagini\PaginaHarta.vue`

**Interfețe:**
- Consumă: `flota` și `asculta` (Task 9), `api.traseu(id)` (Task 8).
- Produce:
  - `ListaMasini`, cu prop-ul `selectata` și evenimentul `alege(id)`;
  - `PanouMasina`, cu prop-ul `masina` (obiectul din `flota.masini`) și
    evenimentul `inchide`;
  - în `HartaFlotei`, prop-ul `traseu` (Array de poziții) și metoda
    `deseneazaTraseu()`, pe care faza 4 o rescrie cu linie punctată și goluri.

- [ ] **Pasul 1: Scrie `ListaMasini.vue`**

```vue
<template>
  <v-list density="compact" nav>
    <v-list-subheader>Flota: {{ online }} din {{ masini.length }} online</v-list-subheader>
    <v-list-item
      v-for="m in masini"
      :key="m.id"
      :active="m.id === selectata"
      @click="$emit('alege', m.id)"
    >
      <template #prepend>
        <v-icon :color="m.stare === 'ONLINE' ? 'success' : 'grey'" icon="mdi-car" />
      </template>
      <v-list-item-title>{{ m.id }}</v-list-item-title>
      <v-list-item-subtitle>{{ m.pozitie ? m.pozitie.viteza.toFixed(0) + ' km/h' : 'fără poziție' }}</v-list-item-subtitle>
    </v-list-item>
  </v-list>
</template>

<script>
import { flota } from '../live.js'

export default {
  name: 'ListaMasini',
  props: {
    selectata: { type: String, default: null },
  },
  emits: ['alege'],
  computed: {
    masini() {
      return Object.values(flota.masini).sort((a, b) => numar(a.id) - numar(b.id))
    },
    online() {
      return this.masini.filter((m) => m.stare === 'ONLINE').length
    },
  },
}

/** masina-10 vine după masina-9, nu după masina-1. */
function numar(id) {
  return Number(id.split('-')[1])
}
</script>
```

- [ ] **Pasul 2: Adaugă traseul în `HartaFlotei.vue`**

Prop-ul nou, lângă `selectata`:

```js
    traseu: { type: Array, default: () => [] },
```

în `watch`:

```js
    traseu: {
      deep: true,
      handler() {
        this.deseneazaTraseu()
      },
    },
```

în `mounted()`, după stratul de zone și înainte de `this.markere = {}`:

```js
    this.stratTraseu = L.layerGroup().addTo(this.harta)
```

și metoda:

```js
    /** Faza 2: o singură linie plină. Faza 4 o împarte în live, recuperat și lipsă. */
    deseneazaTraseu() {
      this.stratTraseu.clearLayers()
      if (this.traseu.length < 2) return
      L.polyline(this.traseu.map((p) => [p.lat, p.lon]), { color: '#1565c0', weight: 4, opacity: 0.8 })
        .addTo(this.stratTraseu)
    },
```

- [ ] **Pasul 3: Rescrie `PaginaHarta.vue`**

```vue
<template>
  <div class="pagina">
    <HartaFlotei class="harta" :zone="zone" :traseu="traseu" :selectata="selectata" @alege="alege" />
    <div class="lateral">
      <PanouMasina :masina="masinaSelectata" @inchide="alege(null)" />
      <ListaMasini :selectata="selectata" @alege="alege" />
    </div>
  </div>
</template>

<script>
import HartaFlotei from '../components/HartaFlotei.vue'
import ListaMasini from '../components/ListaMasini.vue'
import PanouMasina from '../components/PanouMasina.vue'
import { api } from '../api.js'
import { flota, asculta } from '../live.js'

export default {
  name: 'PaginaHarta',
  components: { HartaFlotei, ListaMasini, PanouMasina },
  data() {
    return { zone: [], selectata: null, traseu: [] }
  },
  computed: {
    masinaSelectata() {
      return this.selectata ? flota.masini[this.selectata] : null
    },
  },
  async mounted() {
    this.opresteAscultarea = asculta((tip, date) => {
      if (tip === 'POZITIE' && date.masinaId === this.selectata) this.traseu.push(date)
    })
    this.zone = await api.zone()
  },
  beforeUnmount() {
    this.opresteAscultarea()
  },
  methods: {
    /** Traseul vine din REST, ultimele 5 minute; după aceea crește din canalul live. */
    async alege(id) {
      this.selectata = id
      this.traseu = []
      if (!id) return
      const traseu = await api.traseu(id)
      if (this.selectata === id) this.traseu = traseu
    },
  },
}
</script>

<style scoped>
.pagina {
  display: flex;
  height: calc(100vh - 48px);
}
.harta {
  flex: 1;
}
.lateral {
  width: 340px;
  overflow-y: auto;
  border-left: 1px solid rgba(0, 0, 0, 0.12);
}
</style>
```

Până scrie Tudor `PanouMasina.vue`, importul lui nu se rezolvă. Pașii 4 și 5 vin
imediat unul după altul.

- [ ] **Pasul 4: Tudor scrie `PanouMasina.vue`.** Ce trebuie:
  - prop-ul `masina` (Object, implicit `null`) și evenimentul `inchide`;
  - nimic afișat cât `masina` e `null` (`v-if`);
  - un `v-card` cu:
    - în titlu, id-ul și un `v-chip` verde „online” sau gri „offline”;
    - în subtitlu, zona;
    - un `v-table` cu viteza, temperatura, bateria și ora la care a fost
      văzută ultima dată;
    - un buton „Închide”.
  - valorile vin din `masina.pozitie`, iar dacă nu există, se afișează `—`;
  - ora se afișează pe ceasul local, cu `toLocaleTimeString('ro-RO')`.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

```vue
<template>
  <v-card v-if="masina" variant="outlined" class="ma-2">
    <v-card-title class="d-flex align-center">
      {{ masina.id }}
      <v-spacer />
      <v-chip :color="masina.stare === 'ONLINE' ? 'success' : 'grey'" size="small" variant="flat">
        {{ masina.stare === 'ONLINE' ? 'online' : 'offline' }}
      </v-chip>
    </v-card-title>
    <v-card-subtitle>zona {{ masina.zonaId }}</v-card-subtitle>
    <v-card-text>
      <v-table density="compact">
        <tbody>
          <tr><td>Viteza</td><td>{{ valoare('viteza', 1) }} km/h</td></tr>
          <tr><td>Temperatura</td><td>{{ valoare('temperatura', 1) }} °C</td></tr>
          <tr><td>Bateria</td><td>{{ valoare('baterie', 0) }}%</td></tr>
          <tr><td>Văzută ultima dată</td><td>{{ ora(masina.ultimaVedere) }}</td></tr>
        </tbody>
      </v-table>
    </v-card-text>
    <v-card-actions>
      <v-btn variant="text" @click="$emit('inchide')">Închide</v-btn>
    </v-card-actions>
  </v-card>
</template>

<script>
export default {
  name: 'PanouMasina',
  props: {
    masina: { type: Object, default: null },
  },
  emits: ['inchide'],
  methods: {
    valoare(camp, zecimale) {
      const p = this.masina.pozitie
      return p ? p[camp].toFixed(zecimale) : '—'
    },
    ora(iso) {
      return iso ? new Date(iso).toLocaleTimeString('ro-RO') : '—'
    },
  },
}
</script>
```

</details>

- [ ] **Pasul 5: Verifică pagina**

Totul pornit. Reîncarcă http://127.0.0.1:5173. Se așteaptă:
- în dreapta, lista „Flota: 10 din 10 online”, în ordinea numerelor;
- click pe `masina-3`, în listă sau pe hartă:
  - apare panoul, cu valori care se schimbă o dată pe secundă;
  - pe hartă apare o linie albastră, cu ultimele 5 minute, care crește din
    capăt;
- „Închide” ascunde panoul și linia.

- [ ] **Pasul 6: Claude revizuiește panoul,** fără să rescrie.

- [ ] **Pasul 7: Claude explică lista și traseul.** Analogia: lista e catalogul
  clasei, iar traseul e urma lăsată pe hartă de mașina aleasă. Întâi se cere
  urma din arhivă, apoi se lungește cu ce vine live. Apoi:
  - `computed` față de `methods`;
  - de ce `alege` verifică `this.selectata === id` după `await`: un click rapid
    pe altă mașină nu are voie să primească traseul primei;
  - `asculta` și de ce ascultarea se oprește în `beforeUnmount`.

- [ ] **Pasul 8: Commit-urile**

```bash
git add web/src/components/ListaMasini.vue web/src/components/HartaFlotei.vue web/src/pagini/PaginaHarta.vue
git commit -m "UI: lista masinilor si traseul pe harta

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git add web/src/components/PanouMasina.vue
git commit -m "UI: panoul masinii"
```

Primul commit importă `PanouMasina`, deci se face odată cu al doilea, unul după
altul, fără build între ele.

---

### Task 11: Verificarea fazei 2, de la cap la coadă

**Cine face:** Tudor rulează, iar Claude scrie `README.md`, citește ieșirile și
actualizează starea.

**Fișiere:**
- Modifică: `README.md`
- Modifică, în AIS-OS: `Facultate\memory\stare.md`

**Interfețe:** nu are.

- [ ] **Pasul 1: Rescrie `README.md`**

````markdown
# flota-rc

Proiectul de Rețele de Calculatoare: o flotă de mașini simulate care raportează
prin MQTT la un dispecerat cu hartă live. Designul:
`AIS-OS\docs\superpowers\specs\2026-10-08-flota-rc-design.md`.

## Ce trebuie să ruleze înainte

- Serviciul MySQL (`services.msc` → `MySQL84`), cu baza creată de
  `mysql\creeaza-baza.sql`.

## Cum pornești

Fiecare pas e într-un terminal separat, din folderul ăsta.

1. **A, broker-ul:**

   ```powershell
   & "C:\Program Files\mosquitto\mosquitto.exe" -c mosquitto\mosquitto.conf -v
   ```

2. **S, serverul:**

   ```powershell
   .\mvnw.cmd -q -pl server spring-boot:run
   ```

3. **W, UI-ul,** apoi deschizi http://127.0.0.1:5173:

   ```powershell
   cd web; npm run dev
   ```

4. **C, simulatorul:**

   ```powershell
   .\mvnw.cmd -q -pl simulator compile exec:java
   ```

5. **Opțional, ce circulă pe MQTT:**

   ```powershell
   & "C:\Program Files\mosquitto\mosquitto_sub.exe" -h 127.0.0.1 -t "flota/#" -v
   ```

Se opresc cu Ctrl+C. La „Terminate batch job (Y/N)?” răspunzi Y.

## Testele

```powershell
.\mvnw.cmd -q test
```

## Baza, pentru verificări

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
& $mysql -h 127.0.0.1 -u flota -pflota flota
```
````

- [ ] **Pasul 2: Toate testele**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu testele simulatorului și ale serverului
trecute.

- [ ] **Pasul 3: Drumul întreg, după README**

Pornește A, S, W și C, în ordinea din README. Se așteaptă:
- pe hartă, zece mașini verzi care se mișcă;
- lista „10 din 10 online”;
- panoul și traseul la click.

- [ ] **Pasul 4: Mașinile se opresc și revin**

Ctrl+C în C. Se așteaptă: toate devin gri în câteva secunde și rămân unde au fost
văzute ultima dată. Reîncarcă pagina: tot gri, tot acolo, fiindcă ultima poziție
vine din `GET /api/masini` (D23). Pornește C la loc: verzi.

- [ ] **Pasul 5: Serverul repornit de două ori (review, punctul 3)**

Oprește și pornește S de două ori. După fiecare pornire, chip-ul din UI trece pe
roșu, apoi înapoi pe verde, fără reîncărcarea paginii. La final:

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
& $mysql -h 127.0.0.1 -u flota -pflota flota -e "SELECT COUNT(*) FROM masini; SELECT COUNT(*) FROM zone"
```

Se așteaptă: `10` și numărul de zone desenate, nu dublu.

- [ ] **Pasul 6: Serverul pornit cu Mosquitto oprit (review, punctul 2)**

Oprește S și A. Pornește S. Se așteaptă: `Started ServerApplication`, apoi
`Deconectat de la Mosquitto (…); reîncerc singur`. Pornește A. În cel mult 2
minute, cât e pauza maximă implicită a reconectării HiveMQ, apare
`Conectat la Mosquitto`. Pornește C: mașinile apar pe hartă.

- [ ] **Pasul 7: Un mesaj stricat (review, punctul 1)**

Cu totul pornit:

```powershell
& "C:\Program Files\mosquitto\mosquitto_pub.exe" -h 127.0.0.1 -t flota/masina-99/telemetrie -m "{}"
& "C:\Program Files\mosquitto\mosquitto_pub.exe" -h 127.0.0.1 -t flota/masina-3/telemetrie -m "nu e json"
```

Se așteaptă: în S, două rânduri `Ignor mesajul de pe …`, cu motivele
`mașină necunoscută: masina-99` și `nu e JSON valid`. Harta merge mai departe.

- [ ] **Pasul 8: Un tab închis brusc (review, punctul 5)**

Deschide UI-ul în două taburi. Închide unul dintre ele. Se așteaptă: în S, cel
mult un rând `Scot o conexiune UI moartă`, fără stack trace, iar tabul rămas
merge mai departe.

- [ ] **Pasul 9: Commit în proiect**

```bash
git add README.md
git commit -m "README: pornirea cu server, MySQL si UI

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Pasul 10: Actualizează starea în AIS-OS**

În `Facultate\memory\stare.md`, la blocul de RC, rândul „Unde am rămas” devine,
cu data zilei în care se termină pasul 8, în formatul AAAA-LL-ZZ:

```markdown
- **Unde am rămas:** faza 2 e gata pe AAAA-LL-ZZ: serverul ține flota în MySQL,
  iar harta din browser arată mașinile live. Cerința profesorului e îndeplinită.
  Următorul pas: faza 3, adică proxy-ul, după
  `docs\superpowers\plans\2026-10-08-flota-rc-faza-3.md`.
```

- [ ] **Pasul 11: Commit în AIS-OS, după ce Tudor spune da**

```bash
cd "/c/Users/40747/OneDrive/Documents/AIS-OS"
git add Facultate/memory/stare.md
git commit -m "Starea Facultate: flota-rc, faza 2 gata

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
