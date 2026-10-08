# flota-rc, fazele 0 și 1: pregătirea și mașinile care vorbesc — plan de implementare

> **Pentru agenți:** SUB-SKILL OBLIGATORIU: superpowers:subagent-driven-development
> (recomandat) sau superpowers:executing-plans, ca planul să fie executat task cu
> task. Pașii folosesc căsuțe (`- [ ]`) pentru urmărire.

**Ținta:** zece mașini simulate, fiecare cu motorul și radioul ei, raportează prin
Mosquitto o dată pe secundă. Starea lor e reținută, iar Last Will-ul e `offline`.

**Arhitectura:** un modul Maven `simulator`, în Java 21, într-un proiect Maven cu
mai multe module. Fiecare mașină are două fire de execuție: motorul produce
mesaje într-o coadă, iar radioul le golește spre broker. Singura clasă care știe
de biblioteca MQTT e `ConexiuneHiveMq`. Restul se testează cu o conexiune falsă.

**Stack:** Java 21.0.6, Maven 3.9 prin `mvnw`, HiveMQ MQTT Client 1.4.0 pe MQTT 3,
Jackson 3.2.3, JUnit 6.1.3, Mosquitto 2.1.2.

**Spec:** `docs\superpowers\specs\2026-10-08-flota-rc-design.md`. Planul acoperă
fazele 0 și 1 din spec §8. Fazele 2–5 au câte un plan propriu, în același dosar:
`2026-10-08-flota-rc-faza-2.md` până la `…-faza-5.md`.

## Constrângeri globale

- **Folderul proiectului:** `C:\Users\40747\OneDrive\Documents\Facultate\An 4\Semestrul 1\RC-Proiect\`,
  cu repo git propriu (D15). Comenzile din plan pornesc din folderul ăsta, dacă
  nu scrie altfel.
- **Remote-ul:** `origin` e repo-ul privat `tudorraducristian/ProiectRC` de pe
  GitHub, creat pe 8 octombrie (D27). După commit-urile unui task urmează
  `git push`.
- **Java 21** (`maven.compiler.release` = 21). Maven doar prin `.\mvnw.cmd`;
  Maven global nu se instalează.
- **Versiuni fixe:** HiveMQ MQTT Client `1.4.0`, Jackson `tools.jackson.core:jackson-databind:3.2.3`,
  JUnit `6.1.3` (prin `junit-bom`), `maven-compiler-plugin` `3.16.0`,
  `maven-surefire-plugin` `3.6.0`, `exec-maven-plugin` `3.6.4`.
- **Pachetele** încep cu `ro.flota.simulator`. Numele din cod sînt în română,
  fără diacritice. Mesajele afișate și comentariile au diacritice.
- **Topic-urile:** `flota/<id>/telemetrie`, `flota/<id>/alarma`, `flota/<id>/stare`,
  cu QoS 0, 2 și 1. Doar `stare` e reținut. Id-urile au forma `masina-N`.
- **Orele** sînt în UTC, în formatul `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`, mereu cu
  milisecunde.
- **Conexiunea:** `cleanSession(false)`, `keepAlive(5)`, 5 secunde pentru
  conectarea TCP și pentru `CONNACK`, 10 secunde maxim la publicare, fără
  reconectare automată în bibliotecă (D8).
- **Totul ascultă și se conectează doar pe `127.0.0.1`.** În fazele 0–1,
  simulatorul vorbește direct cu Mosquitto, pe 1883. Trece pe 1884, prin
  proxy, abia în faza 3.
- **Metoda mixtă (spec §9).** Fiecare task spune cine scrie codul:
  - **Claude (exemplu explicat):** Claude scrie codul, apoi i-l explică lui
    Tudor, întâi analogia și de ce contează, apoi partea tehnică.
  - **Tudor (repetiție):** Tudor scrie codul după test. Claude revizuiește fără
    să rescrie. Soluția de referință stă într-un bloc ascuns, pe care Tudor nu-l
    deschide înainte să încerce.
- **Commit-urile lui Claude** se termină cu
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Commit-urile lui
  Tudor sînt ale lui.
- **Descărcări:** orice descărcare, fie că e `starter.zip`, Maven-ul adus de
  `mvnw` sau instalatorul Mosquitto, se face doar după ce Tudor spune da, cu
  sursa și mărimea spuse dinainte.

## Unde trebuie să fie atent review-ul

Cinci situații pe care un om le întâlnește repede, cu comportamentul așteptat.
Fiecare are testul ei în task-ul care deține codul:

1. **Broker-ul e oprit când pornește simulatorul.** Mașinile nu cad. Fiecare
   anunță o singură dată „broker indisponibil" și reîncearcă la 5 secunde. Când
   broker-ul apare, se conectează singure. Testul e în Task 11:
   `brokerIndisponibilLaPornire`.
2. **Broker-ul cade în timpul rulării.** Niciun mesaj din coadă nu se pierde.
   Mesajul care n-a putut pleca rămâne primul la rând. Testul e în Task 11:
   `mesajulNuSePierdeCandPublicareaEsueaza`.
3. **Un fișier din `date\` e greșit:** un traseu inexistent, JSON stricat, un
   fișier lipsă, un element desenat greșit sau un fișier salvat de PowerShell cu
   BOM. Simulatorul nu pornește nicio mașină și spune fișierul și motivul. BOM-ul
   e acceptat. Testele sînt în Task 6.
4. **Un traseu e desenat cu puncte dublate sau e închis de mână.** Mașina merge
   normal, fără împărțire la zero. Testele sînt în Task 5:
   `puncteleDubluriNuStricaTraseul` și `buclaInchisaDeManaAreAceeasiLungime`.
5. **Ctrl+C.** Fiecare mașină publică singură `offline` înainte să se
   deconecteze, ca harta din faza 2 să nu arate mașini-fantomă. Testul e în
   Task 12: `oprireaAnuntaOfflineInainteDeDeconectare`.

---

## Structura fișierelor

```
RC-Proiect\
├── .gitignore, .gitattributes, README.md
├── pom.xml                          agregatorul; deține versiunile
├── mvnw, mvnw.cmd, .mvn\wrapper\    wrapper-ul Maven, din start.spring.io
├── mosquitto\mosquitto.conf
├── date\flota.json, trasee.json, zone.json
└── simulator\
    ├── pom.xml
    └── src\
        ├── main\resources\simulator.properties
        ├── main\java\ro\flota\simulator\
        │   ├── Main.java                  pornește flota, oprește curat la Ctrl+C
        │   ├── Json.java                  singurul JsonMapper
        │   ├── config\Setari.java         simulator.properties
        │   ├── config\Flota.java          flota.json și trasee.json
        │   ├── config\DefinitieMasina.java
        │   ├── config\EroareConfigurare.java
        │   ├── geo\Punct.java             distanța haversine, interpolarea
        │   ├── geo\Traseu.java            bucla și punctul aflat la o distanță dată
        │   ├── mesaje\Ore.java            formatul orei
        │   ├── mesaje\TipMesaj.java, Mesaj.java, Mesaje.java
        │   ├── mesaje\Telemetrie.java, Alarma.java, TipAlarma.java
        │   ├── simulare\Aleator.java      sursa de întâmplare
        │   ├── simulare\Viteza.java, Temperatura.java
        │   ├── simulare\DetectorSupraincalzire.java, DetectorFranare.java
        │   └── masina\Motor.java, Radio.java, Somn.java, Conexiune.java,
        │       EroareConexiune.java, ConexiuneHiveMq.java, Masina.java
        └── test\java\ro\flota\simulator\
            ├── testutil\Trasee.java, AleatorScriptat.java
            ├── masina\ConexiuneFalsa.java
            └── câte un *Test.java lângă fiecare clasă testată
```

În AIS-OS se schimbă: `Facultate\flota-rc\CLAUDE.md`, care e nou,
`Facultate\CLAUDE.md`, `CLAUDE.md` (tabelul E) și `Facultate\memory\stare.md`.

---

### Task 1: AIS-OS află de flota-rc

**Cine scrie:** Claude. Sînt doar documente, fără explicație de tipar.

**Fișiere, în `C:\Users\40747\OneDrive\Documents\AIS-OS\`:**
- Creează: `Facultate\flota-rc\CLAUDE.md`
- Rescrie: `Facultate\CLAUDE.md`
- Modifică: `CLAUDE.md`, antetul și tabelul E
- Modifică: `Facultate\memory\stare.md`, blocul de RC

**Interfețe:** nu are.

- [ ] **Pasul 1: Verifică pe disc ce scrie în `Facultate\CLAUDE.md`**

```powershell
(Get-ChildItem "C:\Users\40747\OneDrive\Documents\Facultate\An 4\Semestrul 1" -Directory).Name
(Get-ChildItem "C:\Users\40747\OneDrive\Documents\Facultate\An 4\Semestrul 2" -Force | Measure-Object).Count
```

Se așteaptă: `FD-Laborator`, `RC-Proiect`, `SCD-Laborator`, `SCD-Proiect`,
`SCPC-Laborator`, apoi `0`. Dacă iese altceva, la pasul 3 se scrie ce iese.

- [ ] **Pasul 2: Creează `Facultate\flota-rc\CLAUDE.md`**

```markdown
# flota-rc — CLAUDE.md
*Ultima actualizare: 2026-10-08 · Owner: Tudor*

---

## A · Ce este

Proiectul de Rețele de Calculatoare din anul 4, semestrul 1. Zece mașini simulate
raportează prin MQTT la un dispecerat. Între ele și broker stă un proxy care
strică rețeaua la comandă: întârziere, bandă îngustă, tunel, gaură neagră.
Dispeceratul arată live, pe hartă, cum rezistă sistemul.

Stadiu: spec-ul e aprobat, iar planurile pentru toate fazele sînt scrise, pe 8 octombrie
2026. Nu există încă cod.

## B · Obiectivul

- **De ce există:** e cerința profesorului, o aplicație client-server cu UI, pe o
  temă IoT, doar software. Ținta lui Tudor: notă maximă și învățare serioasă.
- **„Gata" înseamnă:** prezentarea din spec §11 merge de la cap la coadă, înainte
  de vacanța de iarnă.
- **În afara scopului:** ce scrie în spec §13: login, TLS, broker scris de mână,
  hardware real, deploy.

## C · Stack și rulare

- **Cod:** `C:\Users\40747\OneDrive\Documents\Facultate\An 4\Semestrul 1\RC-Proiect\`,
  cu git și remote-ul privat `ProiectRC` pe GitHub.
- **Stack:**
  - acum: Java 21 cu Maven prin `mvnw`, HiveMQ MQTT Client 1.4.0, Jackson 3,
    JUnit 6 și Mosquitto 2.1.2;
  - din faza 2: Spring Boot 4.1, MySQL, Vue 3 cu Vuetify și Leaflet.

  Totul se scrie în VS Code.
- **Rulare:** după `README.md` din proiect.
- **Teste:** `.\mvnw.cmd -q test`, din rădăcina proiectului.

## D · Decizii

- Deciziile proiectului stau în spec, D1–D18.
- `2026-10-08` — câte un plan pe fază, nu unul pentru tot proiectul. Motiv: fiecare fază se termină cu ceva de arătat, iar un plan pe fază rămâne de citit. Toate cinci au fost scrise pe 8 octombrie, la cererea lui Tudor; fiecare se ajustează dacă codul real al fazelor de dinainte diferă de ce presupune.

## E · Harta memoriei

Starea stă în `..\memory\stare.md`, alături de celelalte proiecte din `Facultate\`.

## F · Referințe

- **Spec:** `..\..\docs\superpowers\specs\2026-10-08-flota-rc-design.md`
- **Planurile:** `..\..\docs\superpowers\plans\2026-10-08-flota-rc-faza-0-1.md`, apoi `…-faza-2.md` până la `…-faza-5.md`
- **Tiparul pentru proxy:** `Documents\Facultate\An 4\Semestrul 1\SCD-Laborator\Laborator 1\`

## G · Reguli specifice

- **Metoda mixtă (spec §9).** Primul exemplu din fiecare tipar îl scrie Claude și
  îl explică. Repetițiile le scrie Tudor, iar Claude le revizuiește fără să le
  rescrie.
- **Explicațiile încep cu analogia,** apoi vine partea tehnică.
- **Codul nu stă în AIS-OS.** Aici stau doar harta și starea.
```

- [ ] **Pasul 3: Rescrie `Facultate\CLAUDE.md`**

Cele două decizii din 22 septembrie rămân întocmai, fiindcă sînt istorie. Fișierul
întreg devine:

```markdown
# Facultate — CLAUDE.md
*Ultima actualizare: 2026-10-08 · Owner: Tudor*

---

## A · Ce este acest folder

Partea academică din afara licenței: semestrul curent, examenele, practica,
proiectele de semestru și materialele din anii trecuți. Licența are categorie
proprie, `Licenta\`, fiindcă are alt ritm și termene care nu se negociază.

Stadiu: în lucru din 8 octombrie 2026.
- **Proiectul de Rețele de Calculatoare,** `flota-rc`, are spec și plan.
- **Proiectul de SCD,** AI Study Hub, are designul gata, în `sesiuni\`, dar încă
  n-are nici spec, nici folder de proiect aici.

## B · Obiectivul

- **De ce există:** ca munca de facultate să aibă un loc al ei, vizibil pentru
  orice sesiune AI, nu un sertar de materiale pe care nu-l deschide nimeni.
- **„Gata" înseamnă:** nu e stabilit pentru categorie. Fiecare proiect își are
  „gata"-ul în spec-ul lui.
- **În afara scopului:** licența, cu tot ce ține de ea.

## C · Stack-ul

| Proiect | Ce e | Unde e codul |
|---|---|---|
| `flota-rc` | proiectul de RC: o flotă de mașini simulate, MQTT, un proxy de rețea | `C:\Users\40747\OneDrive\Documents\Facultate\An 4\Semestrul 1\RC-Proiect\` |

Materialele stau în arhivă, pe disc, în afara AIS-OS:
`C:\Users\40747\OneDrive\Documents\Facultate\`, cu `An 1`, `An 2`, `An 3`,
`An 4` și `Practica`. În `An 4` sînt `Semestrul 1`, `Semestrul 2` și `Licenta`.
La 8 octombrie 2026, `Semestrul 1` are `FD-Laborator`, `SCD-Laborator`,
`SCD-Proiect` (gol), `SCPC-Laborator` și `RC-Proiect`. `Semestrul 2` e gol.

## D · Decizii

- `2026-09-22` — Facultate revine ca a 6-a categorie, după ce pe 21 septembrie fusese scoasă. Motiv: pe disc practica are muncă reală, 15 fișiere din 4, 7 și 11 septembrie 2026, iar un sertar fără `CLAUDE.md` e invizibil pentru orice sesiune AI. Restul lui `An 4` din afara licenței, `Semestrul 1` și `Semestrul 2`, e complet gol.
- `2026-09-22` — licența rămâne categorie separată, nu coboară sub Facultate. Motiv: decizia din 21 septembrie ține. Licența e miza anului și are alt ritm.
- `2026-10-08` — proiectul de Rețele de Calculatoare intră ca `flota-rc`, cu folder propriu și `CLAUDE.md`. Motiv: regula mamei, după care orice proiect are folder în categoria lui. Codul rămâne în `Documents\Facultate`, lângă celelalte materiale.

## E · Harta memoriei

Un singur fișier pentru toată categoria: `memory\stare.md`. Spune unde s-a rămas
la fiecare proiect.

## F · Referințe

- **Arhiva pe disc:** `C:\Users\40747\OneDrive\Documents\Facultate\`
- **Proiectul de RC:** `flota-rc\CLAUDE.md`
- **Licența, separat:** `AIS-OS\Licenta\CLAUDE.md`
- **Starea:** `memory\stare.md`

## G · Reguli specifice

- **Nu inventa termene, materii sau obligații.** Ce s-a hotărât stă în spec-uri și
  în `memory\stare.md`. Restul nu se presupune.
```

- [ ] **Pasul 4: Actualizează mama, `CLAUDE.md`**

În antet, `*Ultima actualizare: 2026-09-25 · Owner: Tudor*` devine
`*Ultima actualizare: 2026-10-08 · Owner: Tudor*`. În tabelul E, rândul:

```
| `Facultate\` | restul facultății: semestrul curent, examene, practica | nu are | nu are |
```

devine:

```
| `Facultate\` | restul facultății: semestrul curent, examene, practica, proiectele de semestru | flota-rc | `Facultate\An 4\Semestrul 1\RC-Proiect` |
```

- [ ] **Pasul 5: Restrânge blocul de RC din `Facultate\memory\stare.md`**

Antetul devine `*Ultima atingere: 2026-10-08*`. Tot blocul care începe cu
`**Proiect viitor, neînceput:**` și se termină cu rândul
`cod nu se scrie până atunci.` se înlocuiește cu:

```markdown
**Proiectul de RC, `flota-rc`:** o flotă de mașini simulate care raportează prin
MQTT, cu un proxy care strică rețeaua la comandă. Spec-ul a fost aprobat pe 8
octombrie 2026: `docs\superpowers\specs\2026-10-08-flota-rc-design.md`. **Acolo
e sursa; aici e doar rezumatul.**
- **Planurile:** câte unul pe fază, în `docs\superpowers\plans\`:
  `2026-10-08-flota-rc-faza-0-1.md`, apoi `…-faza-2.md` până la `…-faza-5.md`.
- **Codul:** `Facultate\An 4\Semestrul 1\RC-Proiect\`, cu git și remote-ul
  privat `ProiectRC` pe GitHub.
- **Unde am rămas:** s-a făcut Task 1 din plan.
- **Deschise:** orașul traseelor (Task 4), data prezentării și ce cere profesorul
  la predare.
```

În „Fire deschise", primul rând devine: `Facultate\CLAUDE.md` știe acum de
`flota-rc` și de folderul `SCD-Proiect`. Proiectul SCD își primește
`CLAUDE.md`-ul și rândul din tabelul E al mamei după spec-ul lui.

- [ ] **Pasul 6: Verifică**

```powershell
Select-String -Path "C:\Users\40747\OneDrive\Documents\AIS-OS\CLAUDE.md" -Pattern "flota-rc"
Test-Path "C:\Users\40747\OneDrive\Documents\AIS-OS\Facultate\flota-rc\CLAUDE.md"
```

Se așteaptă: rândul din tabelul E, apoi `True`.

- [ ] **Pasul 7: Commit în AIS-OS**

Întâi întreabă-l pe Tudor dacă `Facultate\memory\stare.md` se comite cu tot cu
rezumatul SCD din 3 octombrie, care e încă necomis. Dacă spune nu, scoate
fișierul din comanda de mai jos.

```bash
cd "/c/Users/40747/OneDrive/Documents/AIS-OS"
git add CLAUDE.md Facultate/CLAUDE.md Facultate/flota-rc/CLAUDE.md Facultate/memory/stare.md
git commit -m "AIS-OS afla de flota-rc, proiectul de RC

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Repo-ul, wrapper-ul Maven și primul test

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „cum arată un proiect
Maven cu mai multe module și cum trece un test din roșu în verde".

**Fișiere:**
- Există deja, din 8 octombrie: repo-ul, cu `.gitignore` și `.gitattributes`
  (D27)
- Creează: `pom.xml`, `simulator\pom.xml`
- Creează: `mvnw`, `mvnw.cmd`, `.mvn\wrapper\maven-wrapper.properties`, copiate din `starter.zip`
- Creează: `simulator\src\main\java\ro\flota\simulator\mesaje\Ore.java`
- Test: `simulator\src\test\java\ro\flota\simulator\mesaje\OreTest.java`

**Interfețe:**
- Produce: `Ore.format(Instant) : String`, adică ora în UTC, mereu cu
  milisecunde.

- [ ] **Pasul 1: Verifică repo-ul**

Repo-ul a fost creat pe 8 octombrie, împreună cu `ProiectRC` de pe GitHub (D27).
Are deja `.gitignore`, `.gitattributes` și copiile documentelor, în
`docs\design\`.

```powershell
cd "C:\Users\40747\OneDrive\Documents\Facultate\An 4\Semestrul 1\RC-Proiect"
git status
git remote -v
```

Se așteaptă: `nothing to commit, working tree clean`, iar `origin` arată spre
`https://github.com/tudorraducristian/ProiectRC.git`. Conținutul celor două
fișiere, ca să se poată verifica:

`.gitignore`:

```
target/
node_modules/
dist/
*.log
.idea/
*.iml
.vscode/
```

`.gitattributes`:

```
* text=auto
mvnw text eol=lf
*.cmd text eol=crlf
```

- [ ] **Pasul 2: Adu wrapper-ul Maven, după ce Tudor spune da**

Întreabă-l întâi: „Descarc `starter.zip` de la start.spring.io, câteva zeci de KB,
doar ca să iau din el `mvnw`. La prima rulare, `mvnw` descarcă Maven 3.9, vreo
9 MB, de pe repo.maven.apache.org, iar build-ul aduce bibliotecile de pe Maven
Central. E bine?". Dacă spune da:

```powershell
$zip = "$env:TEMP\flota-wrapper.zip"; $dir = "$env:TEMP\flota-wrapper"
curl.exe -fsSL https://start.spring.io/starter.zip -d type=maven-project -d language=java -d javaVersion=21 -d groupId=ro.flota -d artifactId=wrapper -o $zip
Expand-Archive $zip -DestinationPath $dir -Force
$src = (Get-ChildItem $dir -Recurse -Filter mvnw.cmd | Select-Object -First 1).Directory.FullName
Copy-Item "$src\mvnw", "$src\mvnw.cmd" .
Copy-Item "$src\.mvn" . -Recurse
Remove-Item $zip, $dir -Recurse -Force
```

- [ ] **Pasul 3: Scrie `pom.xml`, agregatorul**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>ro.flota</groupId>
  <artifactId>flota-rc</artifactId>
  <version>0.1.0</version>
  <packaging>pom</packaging>

  <modules>
    <module>simulator</module>
  </modules>

  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <junit.version>6.1.3</junit.version>
    <jackson.version>3.2.3</jackson.version>
    <hivemq.version>1.4.0</hivemq.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.junit</groupId>
        <artifactId>junit-bom</artifactId>
        <version>${junit.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.16.0</version>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>3.6.0</version>
        </plugin>
        <plugin>
          <groupId>org.codehaus.mojo</groupId>
          <artifactId>exec-maven-plugin</artifactId>
          <version>3.6.4</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
```

- [ ] **Pasul 4: Scrie `simulator\pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>ro.flota</groupId>
    <artifactId>flota-rc</artifactId>
    <version>0.1.0</version>
  </parent>

  <artifactId>simulator</artifactId>

  <dependencies>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

- [ ] **Pasul 5: Scrie testul care pică**

`simulator\src\test\java\ro\flota\simulator\mesaje\OreTest.java`:

```java
package ro.flota.simulator.mesaje;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class OreTest {

    @Test
    void scrieMilisecundeleChiarCandSintZero() {
        assertEquals("2026-12-10T10:15:03.000Z", Ore.format(Instant.parse("2026-12-10T10:15:03Z")));
    }

    @Test
    void taieTotCeEDupaMilisecunde() {
        assertEquals("2026-12-10T10:15:03.120Z", Ore.format(Instant.parse("2026-12-10T10:15:03.120987Z")));
    }

    @Test
    void scrieMereuInUtc() {
        assertEquals("2026-07-01T21:30:00.500Z", Ore.format(Instant.parse("2026-07-02T00:30:00.500+03:00")));
    }
}
```

- [ ] **Pasul 6: Rulează testul și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Ore`. Dacă
eroarea pomenește calea, din cauza spațiilor din `An 4\Semestrul 1`, te oprești
și îi spui lui Tudor: e același risc ca la SCD.

- [ ] **Pasul 7: Scrie codul**

`simulator\src\main\java\ro\flota\simulator\mesaje\Ore.java`:

```java
package ro.flota.simulator.mesaje;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Orele din mesaje: UTC, ISO-8601, mereu cu milisecunde (spec §4.2). */
public final class Ore {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private Ore() {
    }

    public static String format(Instant ora) {
        return FORMAT.format(ora);
    }
}
```

- [ ] **Pasul 8: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu `Tests run: 3, Failures: 0`.

- [ ] **Pasul 9: Claude explică.** Analogia: un pom agregator e cuprinsul unei
  cărți, iar fiecare modul e un capitol. `mvnw` e o carte care își aduce singură
  tiparnița. Apoi cum ai văzut ciclul: roșu, verde, commit.

- [ ] **Pasul 10: Commit**

```bash
git add .gitignore .gitattributes pom.xml mvnw mvnw.cmd .mvn simulator
git commit -m "Proiectul Maven si primul test: ora in UTC

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Mosquitto pornit și verificat

**Cine scrie:** Tudor instalează, iar Claude scrie configurarea și `README.md`.
Task-ul n-are cod Java; verificarea se face de mână.

**Fișiere:**
- Creează: `mosquitto\mosquitto.conf`, `README.md`

**Interfețe:**
- Produce: un broker pe `127.0.0.1:1883`, fără parolă.

- [ ] **Pasul 1: Tudor instalează Mosquitto**

Tudor descarcă `mosquitto-2.1.2-install-windows-x64.exe` de pe
https://mosquitto.org/download/ și îl rulează. Dacă instalatorul are componenta
„Service", o debifează, fiindcă broker-ul îl pornim noi, cu configurarea
proiectului. Apoi verifică:

```powershell
Test-Path "C:\Program Files\mosquitto\mosquitto.exe"
Get-Service mosquitto -ErrorAction SilentlyContinue
Get-NetTCPConnection -LocalPort 1883 -State Listen -ErrorAction SilentlyContinue
```

Se așteaptă: `True`, apoi nimic, apoi nimic. Dacă serviciul există și rulează,
Tudor îl oprește din `services.msc` și îl trece pe „Manual". E o setare de
sistem, deci o face el.

- [ ] **Pasul 2: Scrie `mosquitto\mosquitto.conf`**

```
# Mosquitto pentru flota-rc: doar pe laptop, fără parolă (spec §3).
listener 1883 127.0.0.1
allow_anonymous true

# Sesiunile persistente stau în memorie cât rulează broker-ul.
persistence false
```

- [ ] **Pasul 3: Pornește broker-ul, în terminalul A**

```powershell
& "C:\Program Files\mosquitto\mosquitto.exe" -c mosquitto\mosquitto.conf -v
```

Se așteaptă: `mosquitto version 2.1.2 starting` și
`Opening ipv4 listen socket on port 1883.` Fanionul `-v` arată fiecare pachet:
`CONNECT`, `PINGREQ`, `PUBLISH`. Îl ținem pornit, fiindcă din el se învață.

- [ ] **Pasul 4: Verifică un mesaj simplu și unul reținut**

Terminalul B:

```powershell
& "C:\Program Files\mosquitto\mosquitto_sub.exe" -h 127.0.0.1 -t "test/#" -v
```

Terminalul C:

```powershell
& "C:\Program Files\mosquitto\mosquitto_pub.exe" -h 127.0.0.1 -t test/salut -m "merge"
& "C:\Program Files\mosquitto\mosquitto_pub.exe" -h 127.0.0.1 -t test/retinut -m "sint aici" -r
```

Se așteaptă în B: `test/salut merge` și `test/retinut sint aici`. Oprește B cu
Ctrl+C și pornește-l din nou. Acum apare doar `test/retinut sint aici`, fiindcă
el e reținut. Ștergerea lui, din C:

```powershell
& "C:\Program Files\mosquitto\mosquitto_pub.exe" -h 127.0.0.1 -t test/retinut -r -n
```

- [ ] **Pasul 5: Scrie `README.md`**

````markdown
# flota-rc

Proiectul de Rețele de Calculatoare: o flotă de mașini simulate care raportează
prin MQTT. Designul: `AIS-OS\docs\superpowers\specs\2026-10-08-flota-rc-design.md`.

## Cum pornești

1. Broker-ul, din folderul ăsta:

   ```powershell
   & "C:\Program Files\mosquitto\mosquitto.exe" -c mosquitto\mosquitto.conf -v
   ```

2. Ce circulă, într-un al doilea terminal:

   ```powershell
   & "C:\Program Files\mosquitto\mosquitto_sub.exe" -h 127.0.0.1 -t "flota/#" -v
   ```
````

- [ ] **Pasul 6: Claude explică.** Analogia: broker-ul e oficiul poștal, iar un
  mesaj reținut e ultima scrisoare lăsată la ghișeu pentru oricine întreabă. Apoi
  ce înseamnă rândurile din log-ul `-v`.

- [ ] **Pasul 7: Commit**

```bash
git add mosquitto/mosquitto.conf README.md
git commit -m "Mosquitto: configurarea si pornirea

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Traseele, zonele și flota

**Cine scrie:** Tudor. Alege orașul (Î1) și desenează pe hartă. Claude
verifică fișierele.

**Fișiere:**
- Creează: `date\trasee.json`, `date\zone.json`, `date\flota.json`

**Interfețe:**
- Produce: fișierele citite de `Flota.citeste` (Task 6) și, din faza 2, de server.
  Formatul lor e în spec §4.4 și §6.2.

- [ ] **Pasul 1: Tudor alege orașul.** E întrebarea deschisă Î1 din spec §14.
  Se trece în `Facultate\memory\stare.md`.

- [ ] **Pasul 2: Tudor desenează traseele în https://geojson.io**
  - 3–5 linii (unealta „Draw a polyline"), fiecare o buclă pe străzi reale, de
    2–5 km. Ultimul punct se pune lângă primul, nu e nevoie să coincidă.
  - Pentru fiecare linie: o selectezi, apoi la „Properties" adaugi rândul `id`, cu
    valoarea `t1`, `t2` și tot așa.
  - „Save" → „GeoJSON", iar fișierul se salvează ca `date\trasee.json`.

- [ ] **Pasul 3: Tudor desenează zonele, într-un document geojson.io nou**
  - 1–2 poligoane („Draw a polygon"), fiecare acoperind doar o parte din trasee,
    ca ieșirea din zonă să se poată întâmpla.
  - Proprietățile: `id` (`z1`, `z2`), `nume` (de exemplu `Centru`) și
    `viteza_maxima` (de exemplu `50`, ca număr).
  - Fișierul se salvează ca `date\zone.json`.

- [ ] **Pasul 4: Tudor scrie `date\flota.json`**

Sînt 10 mașini, împărțite pe trasee, fiecare cu un `start` diferit, între 0 și
1, adică ce parte din buclă e deja parcursă la pornire. Exemplul de mai jos
folosește `t1`–`t3` și `z1`. Se înlocuiesc cu id-urile desenate.

```json
{
  "masini": [
    {"id": "masina-1",  "traseu": "t1", "start": 0.0,  "zona": "z1"},
    {"id": "masina-2",  "traseu": "t1", "start": 0.33, "zona": "z1"},
    {"id": "masina-3",  "traseu": "t1", "start": 0.66, "zona": "z1"},
    {"id": "masina-4",  "traseu": "t2", "start": 0.0,  "zona": "z1"},
    {"id": "masina-5",  "traseu": "t2", "start": 0.25, "zona": "z1"},
    {"id": "masina-6",  "traseu": "t2", "start": 0.5,  "zona": "z1"},
    {"id": "masina-7",  "traseu": "t2", "start": 0.75, "zona": "z1"},
    {"id": "masina-8",  "traseu": "t3", "start": 0.0,  "zona": "z1"},
    {"id": "masina-9",  "traseu": "t3", "start": 0.33, "zona": "z1"},
    {"id": "masina-10", "traseu": "t3", "start": 0.66, "zona": "z1"}
  ]
}
```

- [ ] **Pasul 5: Claude verifică fișierele**

```powershell
Get-Content date\trasee.json -Raw | ConvertFrom-Json | ForEach-Object { $_.features | ForEach-Object { "$($_.properties.id): $($_.geometry.type), $($_.geometry.coordinates.Count) puncte" } }
Get-Content date\zone.json -Raw | ConvertFrom-Json | ForEach-Object { $_.features | ForEach-Object { "$($_.properties.id) $($_.properties.nume): $($_.geometry.type), max $($_.properties.viteza_maxima)" } }
(Get-Content date\flota.json -Raw | ConvertFrom-Json).masini.Count
```

Se așteaptă:
- câte un rând `tN: LineString, …` pe fiecare traseu, fiecare cu cel puțin 10
  puncte;
- câte un rând `zN …: Polygon, max 50` pe fiecare zonă;
- `10`.

- [ ] **Pasul 6: Commit, făcut de Tudor**

```bash
git add date
git commit -m "Datele: traseele, zonele si flota"
```

---

### Task 5: Geometria traseului

**Cine scrie:** Claude, ca exemplu explicat. E matematica proiectului: distanța
pe Pământ și „unde e mașina după d metri".

**Fișiere:**
- Creează: `simulator\src\main\java\ro\flota\simulator\geo\Punct.java`, `Traseu.java`
- Creează: `simulator\src\test\java\ro\flota\simulator\testutil\Trasee.java`
- Test: `simulator\src\test\java\ro\flota\simulator\geo\TraseuTest.java`

**Interfețe:**
- Produce:
  - `record Punct(double lat, double lon)`, cu `double distantaPana(Punct)`, în
    metri, și `Punct spre(Punct, double f)`;
  - `final class Traseu`, cu constructorul `Traseu(String id, List<Punct> puncte)`,
    care aruncă `IllegalArgumentException` dacă are mai puțin de 2 puncte sau
    lungimea 0, plus metodele `String id()`, `double lungime()`,
    `Punct punctLa(double metri)` și `double normalizeaza(double metri)`;
  - pentru teste: `Trasee.patrat() : Traseu`.

- [ ] **Pasul 1: Scrie helper-ul de test**

`simulator\src\test\java\ro\flota\simulator\testutil\Trasee.java`:

```java
package ro.flota.simulator.testutil;

import java.util.List;
import ro.flota.simulator.geo.Punct;
import ro.flota.simulator.geo.Traseu;

/** Trasee mici, cu lungimi știute, pentru teste. */
public final class Trasee {

    private Trasee() {
    }

    /**
     * Un pătrat lângă 46°N 23°E: spre nord, spre est, spre sud, apoi înapoi spre vest.
     * Laturile nord-sud au 111,19493 m; cele est-vest, 77,24109 m sus și 77,24249 m jos.
     */
    public static Traseu patrat() {
        return new Traseu("patrat", List.of(
                new Punct(46.000, 23.000),
                new Punct(46.001, 23.000),
                new Punct(46.001, 23.001),
                new Punct(46.000, 23.001)));
    }
}
```

- [ ] **Pasul 2: Scrie testele care pică**

`simulator\src\test\java\ro\flota\simulator\geo\TraseuTest.java`:

```java
package ro.flota.simulator.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.testutil.Trasee;

class TraseuTest {

    private static final double LATURA_NS = 111.19493;
    private static final double LATURA_EV_SUS = 77.24109;
    private static final double LATURA_EV_JOS = 77.24249;

    private static final Punct A = new Punct(46.000, 23.000);
    private static final Punct B = new Punct(46.001, 23.000);
    private static final Punct C = new Punct(46.001, 23.001);
    private static final Punct D = new Punct(46.000, 23.001);

    @Test
    void oMiimeDeGradPeMeridianInseamna111Metri() {
        assertEquals(LATURA_NS, A.distantaPana(B), 0.001);
    }

    @Test
    void lungimeaIncludeDrumulInapoiLaStart() {
        assertEquals(2 * LATURA_NS + LATURA_EV_SUS + LATURA_EV_JOS, Trasee.patrat().lungime(), 0.01);
    }

    @Test
    void laDistanta0EsteStartul() {
        assertEquals(A, Trasee.patrat().punctLa(0));
    }

    @Test
    void laJumatateaPrimeiLaturiEsteMijloculEi() {
        Punct p = Trasee.patrat().punctLa(LATURA_NS / 2);
        assertEquals(46.0005, p.lat(), 1e-7);
        assertEquals(23.0, p.lon(), 1e-9);
    }

    @Test
    void dupaOTuraCompletaOiaDeLaCapat() {
        Traseu t = Trasee.patrat();
        assertAcelasiPunct(t.punctLa(10), t.punctLa(t.lungime() + 10));
    }

    @Test
    void distantaNegativaSeNumaraDeLaCapat() {
        Traseu t = Trasee.patrat();
        assertAcelasiPunct(t.punctLa(t.lungime() - 10), t.punctLa(-10));
    }

    @Test
    void puncteleDubluriNuStricaTraseul() {
        Traseu fara = Trasee.patrat();
        Traseu cuDubluri = new Traseu("dubluri", List.of(A, A, B, C, C, D));
        assertEquals(fara.lungime(), cuDubluri.lungime(), 1e-9);
        for (double d = 0; d < fara.lungime(); d += 7.3) {
            assertAcelasiPunct(fara.punctLa(d), cuDubluri.punctLa(d));
        }
    }

    @Test
    void buclaInchisaDeManaAreAceeasiLungime() {
        Traseu inchis = new Traseu("inchis", List.of(A, B, C, D, A));
        assertEquals(Trasee.patrat().lungime(), inchis.lungime(), 1e-9);
        assertAcelasiPunct(Trasee.patrat().punctLa(300), inchis.punctLa(300));
    }

    @Test
    void refuzaTraseulCuUnSingurPunct() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Traseu("t7", List.of(A)));
        assertTrue(e.getMessage().contains("t7"));
    }

    @Test
    void refuzaTraseulCuToatePuncteleIdentice() {
        assertThrows(IllegalArgumentException.class, () -> new Traseu("t8", List.of(A, A, A)));
    }

    private static void assertAcelasiPunct(Punct asteptat, Punct real) {
        assertEquals(asteptat.lat(), real.lat(), 1e-9);
        assertEquals(asteptat.lon(), real.lon(), 1e-9);
    }
}
```

- [ ] **Pasul 3: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Punct` și
`Traseu`.

- [ ] **Pasul 4: Scrie `Punct`**

`simulator\src\main\java\ro\flota\simulator\geo\Punct.java`:

```java
package ro.flota.simulator.geo;

/** Un punct pe hartă, în grade. Atenție: GeoJSON scrie [lon, lat], invers. */
public record Punct(double lat, double lon) {

    private static final double RAZA_PAMANT_M = 6_371_000.0;

    /** Distanța pe suprafața Pământului, în metri (formula haversine). */
    public double distantaPana(Punct altul) {
        double fi1 = Math.toRadians(lat);
        double fi2 = Math.toRadians(altul.lat);
        double dFi = fi2 - fi1;
        double dLambda = Math.toRadians(altul.lon - lon);
        double a = Math.sin(dFi / 2) * Math.sin(dFi / 2)
                + Math.cos(fi1) * Math.cos(fi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        return 2 * RAZA_PAMANT_M * Math.asin(Math.sqrt(a));
    }

    /** Punctul aflat la fracțiunea f, de la 0 la 1, din drumul de aici până la altul. */
    public Punct spre(Punct altul, double f) {
        return new Punct(lat + (altul.lat - lat) * f, lon + (altul.lon - lon) * f);
    }
}
```

- [ ] **Pasul 5: Scrie `Traseu`**

`simulator\src\main\java\ro\flota\simulator\geo\Traseu.java`:

```java
package ro.flota.simulator.geo;

import java.util.List;

/** O buclă: după ultimul punct, drumul continuă spre primul. */
public final class Traseu {

    private final String id;
    private final List<Punct> puncte;
    /** inceput[i] = câți metri sînt de la start până la începutul segmentului i. */
    private final double[] inceput;
    private final double lungime;

    public Traseu(String id, List<Punct> puncte) {
        if (puncte.size() < 2) {
            throw new IllegalArgumentException("Traseul " + id + " are mai puțin de 2 puncte");
        }
        this.id = id;
        this.puncte = List.copyOf(puncte);
        this.inceput = new double[this.puncte.size()];
        double total = 0;
        for (int i = 0; i < this.puncte.size(); i++) {
            inceput[i] = total;
            total += this.puncte.get(i).distantaPana(urmatorul(i));
        }
        if (total <= 0) {
            throw new IllegalArgumentException("Traseul " + id + " are lungimea 0");
        }
        this.lungime = total;
    }

    public String id() {
        return id;
    }

    public double lungime() {
        return lungime;
    }

    /** Aduce orice distanță în bucla [0, lungime). */
    public double normalizeaza(double metri) {
        double x = metri % lungime;
        return x < 0 ? x + lungime : x;
    }

    /** Punctul aflat la atâția metri de start, mergând pe buclă. */
    public Punct punctLa(double metri) {
        double x = normalizeaza(metri);
        int i = segmentul(x);
        double sfarsit = i + 1 < puncte.size() ? inceput[i + 1] : lungime;
        double lungimeSegment = sfarsit - inceput[i];
        if (lungimeSegment == 0) {
            return puncte.get(i);
        }
        return puncte.get(i).spre(urmatorul(i), (x - inceput[i]) / lungimeSegment);
    }

    private Punct urmatorul(int i) {
        return puncte.get((i + 1) % puncte.size());
    }

    /**
     * Ultimul segment care începe la cel mult x, găsit prin căutare binară.
     * Segmentele de lungime 0, adică punctele dublate, au același început ca următorul,
     * așa că sînt sărite.
     */
    private int segmentul(double x) {
        int st = 0;
        int dr = puncte.size() - 1;
        while (st < dr) {
            int mij = (st + dr + 1) / 2;
            if (inceput[mij] <= x) {
                st = mij;
            } else {
                dr = mij - 1;
            }
        }
        return st;
    }
}
```

- [ ] **Pasul 6: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu toate testele `TraseuTest` trecute.

- [ ] **Pasul 7: Claude explică.** Analogia: traseul e o bandă de alergare
  rulată în cerc. Poziția mașinii e un singur număr, metrii de la start, iar
  `punctLa` e „unde ajungi după atâția metri". Apoi formula haversine, de ce
  GeoJSON scrie `[lon, lat]` și cum sare căutarea binară peste punctele dublate.

- [ ] **Pasul 8: Commit**

```bash
git add simulator/src
git commit -m "Simulator: geometria traseului

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Configurarea: flota și setările

**Cine scrie:**
- **`Flota`:** Claude, ca exemplu explicat. E primul fișier JSON citit în
  proiect.
- **`Setari`:** Tudor, ca repetiție. Citește un fișier, verifică fiecare
  valoare și spune clar ce lipsește.

**Fișiere:**
- Modifică: `simulator\pom.xml`, adaugi Jackson
- Creează: `simulator\src\main\java\ro\flota\simulator\Json.java`
- Creează, în `simulator\src\main\java\ro\flota\simulator\config\`:
  `DefinitieMasina.java`, `EroareConfigurare.java`, `Flota.java` și `Setari.java`
- Creează: `simulator\src\main\resources\simulator.properties`
- Teste: `simulator\src\test\java\ro\flota\simulator\config\FlotaTest.java`, `SetariTest.java`

**Interfețe:**
- Consumă: `Traseu(String, List<Punct>)` și `Punct(lat, lon)`, din Task 5.
- Produce:
  - `Json.MAPPER : JsonMapper`;
  - `record DefinitieMasina(String id, Traseu traseu, double start, String zona)`;
  - `Flota.citeste(Path dosarDate) : List<DefinitieMasina>`, care aruncă
    `EroareConfigurare`, o excepție de tip `RuntimeException` al cărei mesaj
    începe cu numele fișierului;
  - `record Setari(String brokerHost, int brokerPort, int keepaliveSecunde, long perioadaMs)`,
    cu `Setari.implicite()` și `Setari.din(Properties)`.

- [ ] **Pasul 1: Adaugă Jackson în `simulator\pom.xml`**, în `<dependencies>`:

```xml
    <dependency>
      <groupId>tools.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>${jackson.version}</version>
    </dependency>
```

- [ ] **Pasul 2: Scrie testele care pică pentru `Flota`**

`simulator\src\test\java\ro\flota\simulator\config\FlotaTest.java`:

```java
package ro.flota.simulator.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ro.flota.simulator.geo.Punct;

class FlotaTest {

    private static final String TRASEE = """
            {"type": "FeatureCollection", "features": [
              {"type": "Feature", "properties": {"id": "t1"},
               "geometry": {"type": "LineString",
                            "coordinates": [[23.000, 46.000], [23.000, 46.001], [23.001, 46.001]]}}
            ]}""";

    private static final String FLOTA = """
            {"masini": [
              {"id": "masina-1", "traseu": "t1", "start": 0.25, "zona": "z1"},
              {"id": "masina-2", "traseu": "t1", "start": 0.0,  "zona": "z1"}
            ]}""";

    @TempDir
    Path dosar;

    private void scrie(String fisier, String continut) throws IOException {
        Files.writeString(dosar.resolve(fisier), continut);
    }

    private String eroare(String trasee, String flota) throws IOException {
        scrie("trasee.json", trasee);
        if (flota != null) {
            scrie("flota.json", flota);
        }
        return assertThrows(EroareConfigurare.class, () -> Flota.citeste(dosar)).getMessage();
    }

    @Test
    void citesteFlotaSiTraseele() throws IOException {
        scrie("trasee.json", TRASEE);
        scrie("flota.json", FLOTA);
        List<DefinitieMasina> flota = Flota.citeste(dosar);
        assertEquals(2, flota.size());
        DefinitieMasina prima = flota.get(0);
        assertEquals("masina-1", prima.id());
        assertEquals("t1", prima.traseu().id());
        assertEquals(0.25, prima.start());
        assertEquals("z1", prima.zona());
    }

    @Test
    void geoJsonScrieIntaiLongitudinea() throws IOException {
        scrie("trasee.json", TRASEE);
        scrie("flota.json", FLOTA);
        assertEquals(new Punct(46.000, 23.000), Flota.citeste(dosar).get(0).traseu().punctLa(0));
    }

    @Test
    void acceptaFisierSalvatCuBom() throws IOException {
        scrie("trasee.json", "\uFEFF" + TRASEE);
        scrie("flota.json", "\uFEFF" + FLOTA);
        assertEquals(2, Flota.citeste(dosar).size());
    }

    @Test
    void traseulInexistentEstePusPeNume() throws IOException {
        String mesaj = eroare(TRASEE, FLOTA.replace("\"traseu\": \"t1\", \"start\": 0.25", "\"traseu\": \"t9\", \"start\": 0.25"));
        assertTrue(mesaj.startsWith("flota.json"), mesaj);
        assertTrue(mesaj.contains("masina-1") && mesaj.contains("t9"), mesaj);
    }

    @Test
    void jsonStricatSpuneFisierul() throws IOException {
        String mesaj = eroare(TRASEE, "{\"masini\": [");
        assertTrue(mesaj.startsWith("flota.json") && mesaj.contains("nu e JSON valid"), mesaj);
    }

    @Test
    void fisierulLipsaSpuneFisierul() throws IOException {
        String mesaj = eroare(TRASEE, null);
        assertTrue(mesaj.startsWith("flota.json") && mesaj.contains("nu pot citi"), mesaj);
    }

    @Test
    void idDublatEsteRefuzat() throws IOException {
        String mesaj = eroare(TRASEE, FLOTA.replace("masina-2", "masina-1"));
        assertTrue(mesaj.contains("de două ori"), mesaj);
    }

    @Test
    void startulTrebuieSaFieSub1() throws IOException {
        String mesaj = eroare(TRASEE, FLOTA.replace("0.25", "1.0"));
        assertTrue(mesaj.contains("start"), mesaj);
    }

    @Test
    void unElementCareNuELinieEsteRefuzat() throws IOException {
        String mesaj = eroare(TRASEE.replace("LineString", "Point"), FLOTA);
        assertTrue(mesaj.startsWith("trasee.json") && mesaj.contains("LineString"), mesaj);
    }

    @Test
    void datelePropriiSeIncarca() {
        List<DefinitieMasina> flota = Flota.citeste(Path.of("..", "date"));
        assertEquals(10, flota.size());
        for (DefinitieMasina m : flota) {
            assertTrue(m.traseu().lungime() > 500, m.traseu().id() + " are sub 500 m");
        }
    }
}
```

- [ ] **Pasul 3: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Flota`,
`DefinitieMasina` și `EroareConfigurare`.

- [ ] **Pasul 4: Scrie `Json`, `DefinitieMasina`, `EroareConfigurare`**

`simulator\src\main\java\ro\flota\simulator\Json.java`:

```java
package ro.flota.simulator;

import tools.jackson.databind.json.JsonMapper;

/** Un singur JsonMapper pentru tot simulatorul. În Jackson 3 e imutabil, deci îl pot folosi mai multe fire. */
public final class Json {

    public static final JsonMapper MAPPER = JsonMapper.builder().build();

    private Json() {
    }
}
```

`simulator\src\main\java\ro\flota\simulator\config\DefinitieMasina.java`:

```java
package ro.flota.simulator.config;

import ro.flota.simulator.geo.Traseu;

/** O mașină din date\flota.json: start e partea din buclă deja parcursă la pornire, între 0 și 1. */
public record DefinitieMasina(String id, Traseu traseu, double start, String zona) {
}
```

`simulator\src\main\java\ro\flota\simulator\config\EroareConfigurare.java`:

```java
package ro.flota.simulator.config;

import java.nio.file.Path;

/** Un fișier din date\ e greșit. Mesajul începe mereu cu numele fișierului. */
public final class EroareConfigurare extends RuntimeException {

    public EroareConfigurare(Path fisier, String motiv) {
        super(fisier.getFileName() + ": " + motiv);
    }
}
```

- [ ] **Pasul 5: Scrie `Flota`**

`simulator\src\main\java\ro\flota\simulator\config\Flota.java`:

```java
package ro.flota.simulator.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ro.flota.simulator.Json;
import ro.flota.simulator.geo.Punct;
import ro.flota.simulator.geo.Traseu;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/** Citește flota din date\flota.json și traseele din date\trasee.json, un fișier GeoJSON (D18). */
public final class Flota {

    public static final int MAXIM_MASINI = 20;

    private Flota() {
    }

    public static List<DefinitieMasina> citeste(Path dosarDate) {
        Map<String, Traseu> trasee = citesteTrasee(dosarDate.resolve("trasee.json"));
        Path fisier = dosarDate.resolve("flota.json");
        JsonNode masini = citesteJson(fisier).path("masini");
        if (!masini.isArray() || masini.isEmpty()) {
            throw new EroareConfigurare(fisier, "lipsește lista \"masini\" sau e goală");
        }
        if (masini.size() > MAXIM_MASINI) {
            throw new EroareConfigurare(fisier, "are " + masini.size() + " mașini, iar limita e " + MAXIM_MASINI);
        }
        List<DefinitieMasina> rezultat = new ArrayList<>();
        Set<String> iduri = new HashSet<>();
        for (JsonNode m : masini) {
            String id = text(m, "id", fisier);
            if (!id.matches("masina-[0-9]+")) {
                throw new EroareConfigurare(fisier, "id-ul \"" + id + "\" nu are forma masina-N");
            }
            if (!iduri.add(id)) {
                throw new EroareConfigurare(fisier, "id-ul \"" + id + "\" apare de două ori");
            }
            String idTraseu = text(m, "traseu", fisier);
            Traseu traseu = trasee.get(idTraseu);
            if (traseu == null) {
                throw new EroareConfigurare(fisier,
                        id + " cere traseul \"" + idTraseu + "\", care nu există în trasee.json");
            }
            JsonNode start = m.path("start");
            if (!start.isNumber() || start.asDouble() < 0 || start.asDouble() >= 1) {
                throw new EroareConfigurare(fisier,
                        id + ": \"start\" trebuie să fie un număr de la 0 (inclusiv) la 1 (exclusiv)");
            }
            rezultat.add(new DefinitieMasina(id, traseu, start.asDouble(), text(m, "zona", fisier)));
        }
        return rezultat;
    }

    private static Map<String, Traseu> citesteTrasee(Path fisier) {
        JsonNode radacina = citesteJson(fisier);
        JsonNode elemente = radacina.path("features");
        if (!eText(radacina.path("type"), "FeatureCollection") || !elemente.isArray() || elemente.isEmpty()) {
            throw new EroareConfigurare(fisier, "trebuie să fie un FeatureCollection GeoJSON cu cel puțin un traseu");
        }
        Map<String, Traseu> trasee = new HashMap<>();
        int nr = 0;
        for (JsonNode element : elemente) {
            nr++;
            JsonNode geometrie = element.path("geometry");
            if (!eText(geometrie.path("type"), "LineString")) {
                throw new EroareConfigurare(fisier,
                        "elementul nr. " + nr + " nu e LineString; traseele se desenează cu linia");
            }
            String id = text(element.path("properties"), "id", fisier);
            if (trasee.containsKey(id)) {
                throw new EroareConfigurare(fisier, "traseul \"" + id + "\" apare de două ori");
            }
            List<Punct> puncte = new ArrayList<>();
            for (JsonNode c : geometrie.path("coordinates")) {
                if (!c.isArray() || c.size() < 2 || !c.get(0).isNumber() || !c.get(1).isNumber()) {
                    throw new EroareConfigurare(fisier, "traseul \"" + id + "\" are o coordonată greșită");
                }
                puncte.add(new Punct(c.get(1).asDouble(), c.get(0).asDouble())); // GeoJSON: [lon, lat]
            }
            try {
                trasee.put(id, new Traseu(id, puncte));
            } catch (IllegalArgumentException e) {
                throw new EroareConfigurare(fisier, e.getMessage());
            }
        }
        return trasee;
    }

    private static JsonNode citesteJson(Path fisier) {
        String continut;
        try {
            continut = Files.readString(fisier);
        } catch (IOException e) {
            throw new EroareConfigurare(fisier, "nu pot citi fișierul (" + e.getClass().getSimpleName() + ")");
        }
        if (continut.startsWith("\uFEFF")) {
            continut = continut.substring(1); // BOM-ul pus de PowerShell 5.1
        }
        try {
            return Json.MAPPER.readTree(continut);
        } catch (JacksonException e) {
            throw new EroareConfigurare(fisier, "nu e JSON valid: " + e.getOriginalMessage());
        }
    }

    private static String text(JsonNode nod, String camp, Path fisier) {
        JsonNode valoare = nod.path(camp);
        if (!valoare.isString() || valoare.asString().isBlank()) {
            throw new EroareConfigurare(fisier, "lipsește câmpul text \"" + camp + "\"");
        }
        return valoare.asString();
    }

    private static boolean eText(JsonNode nod, String asteptat) {
        return nod.isString() && nod.asString().equals(asteptat);
    }
}
```

- [ ] **Pasul 6: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`. Dacă pică doar `datelePropriiSeIncarca`, problema
e în datele din Task 4, iar mesajul spune fișierul și motivul. Se repară
datele, nu testul.

- [ ] **Pasul 7: Claude explică `Flota`.** Analogia: JSON-ul e un dulap cu
  sertare etichetate, iar `path("masini")` deschide un sertar fără să cadă dacă
  sertarul lipsește. Apoi de ce fiecare greșeală spune fișierul și motivul, și
  ce e BOM-ul.

- [ ] **Pasul 8: Scrie `simulator.properties`**

`simulator\src\main\resources\simulator.properties`:

```properties
# Fazele 0-1: direct pe Mosquitto. În faza 3, portul devine 1884, adică proxy-ul.
broker.host=127.0.0.1
broker.port=1883
keepalive.secunde=5
perioada.ms=1000
```

- [ ] **Pasul 9: Scrie testele care pică pentru `Setari`**

`simulator\src\test\java\ro\flota\simulator\config\SetariTest.java`:

```java
package ro.flota.simulator.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.junit.jupiter.api.Test;

class SetariTest {

    private static Properties bune() {
        Properties p = new Properties();
        p.setProperty("broker.host", "127.0.0.1");
        p.setProperty("broker.port", "1883");
        p.setProperty("keepalive.secunde", "5");
        p.setProperty("perioada.ms", "1000");
        return p;
    }

    @Test
    void citesteToateCheile() {
        assertEquals(new Setari("127.0.0.1", 1883, 5, 1000), Setari.din(bune()));
    }

    @Test
    void cheiaLipsaEstePusaPeNume() {
        Properties p = bune();
        p.remove("broker.port");
        String mesaj = assertThrows(IllegalArgumentException.class, () -> Setari.din(p)).getMessage();
        assertTrue(mesaj.contains("broker.port"), mesaj);
    }

    @Test
    void unPortCareNuENumarEstePusPeNume() {
        Properties p = bune();
        p.setProperty("broker.port", "abc");
        String mesaj = assertThrows(IllegalArgumentException.class, () -> Setari.din(p)).getMessage();
        assertTrue(mesaj.contains("broker.port") && mesaj.contains("abc"), mesaj);
    }

    @Test
    void unPortInAfaraIntervaluluiEsteRefuzat() {
        Properties p = bune();
        p.setProperty("broker.port", "70000");
        assertThrows(IllegalArgumentException.class, () -> Setari.din(p));
    }

    @Test
    void fisierulDinResursePornesteDirectPeBroker() {
        assertEquals(new Setari("127.0.0.1", 1883, 5, 1000), Setari.implicite());
    }
}
```

- [ ] **Pasul 10: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Setari`.

- [ ] **Pasul 11: Tudor scrie `Setari`.** Indicii:
  - e un `record` cu patru câmpuri;
  - `din` citește fiecare cheie printr-o metodă ajutătoare care aruncă
    `IllegalArgumentException` cu numele cheii;
  - intervalele sînt: portul între 1 și 65535, keepalive-ul între 1 și 3600,
    perioada între 10 și 60.000;
  - `implicite()` citește resursa `/simulator.properties` prin
    `getResourceAsStream`, în UTF-8.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

`simulator\src\main\java\ro\flota\simulator\config\Setari.java`:

```java
package ro.flota.simulator.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Setările de rețea ale simulatorului, din simulator.properties. */
public record Setari(String brokerHost, int brokerPort, int keepaliveSecunde, long perioadaMs) {

    public static Setari implicite() {
        Properties p = new Properties();
        try (InputStream in = Setari.class.getResourceAsStream("/simulator.properties")) {
            if (in == null) {
                throw new IllegalStateException("lipsește simulator.properties din resurse");
            }
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return din(p);
    }

    public static Setari din(Properties p) {
        return new Setari(
                obligatoriu(p, "broker.host"),
                intreg(p, "broker.port", 1, 65_535),
                intreg(p, "keepalive.secunde", 1, 3_600),
                intreg(p, "perioada.ms", 10, 60_000));
    }

    private static String obligatoriu(Properties p, String cheie) {
        String valoare = p.getProperty(cheie);
        if (valoare == null || valoare.isBlank()) {
            throw new IllegalArgumentException("simulator.properties: lipsește " + cheie);
        }
        return valoare.trim();
    }

    private static int intreg(Properties p, String cheie, int minim, int maxim) {
        String valoare = obligatoriu(p, cheie);
        int n;
        try {
            n = Integer.parseInt(valoare);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("simulator.properties: " + cheie + " nu e număr: " + valoare);
        }
        if (n < minim || n > maxim) {
            throw new IllegalArgumentException(
                    "simulator.properties: " + cheie + " trebuie să fie între " + minim + " și " + maxim);
        }
        return n;
    }
}
```

</details>

- [ ] **Pasul 12: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`.

- [ ] **Pasul 13: Claude revizuiește `Setari`,** fără să rescrie: compară cu
  soluția de referință și comentează doar ce contează.

- [ ] **Pasul 14: Commit-urile.** Claude comite `Flota`, iar Tudor comite
  `Setari`.

```bash
git add simulator/pom.xml simulator/src/main/java/ro/flota/simulator/Json.java simulator/src/main/java/ro/flota/simulator/config/DefinitieMasina.java simulator/src/main/java/ro/flota/simulator/config/EroareConfigurare.java simulator/src/main/java/ro/flota/simulator/config/Flota.java simulator/src/test/java/ro/flota/simulator/config/FlotaTest.java
git commit -m "Simulator: flota si traseele din date

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git add simulator/src/main/resources/simulator.properties simulator/src/main/java/ro/flota/simulator/config/Setari.java simulator/src/test/java/ro/flota/simulator/config/SetariTest.java
git commit -m "Simulator: setarile de retea"
```

---

### Task 7: Mesajele

**Cine scrie:**
- **Telemetria, starea și forma comună `Mesaj`:** Claude, ca exemplu explicat.
- **Alarma:** Tudor, ca repetiție a telemetriei, cu alt topic și alt QoS.

**Fișiere:**
- Creează, în `simulator\src\main\java\ro\flota\simulator\mesaje\`:
  `TipMesaj.java`, `Mesaj.java`, `Telemetrie.java`, `Mesaje.java`,
  `TipAlarma.java` și `Alarma.java`
- Test: `simulator\src\test\java\ro\flota\simulator\mesaje\MesajeTest.java`

**Interfețe:**
- Consumă: `Json.MAPPER`, din Task 6.
- Produce:
  - `enum TipMesaj { TELEMETRIE, ALARMA, STARE }`;
  - `record Mesaj(TipMesaj tip, String topic, byte[] payload, int qos, boolean retain)`,
    cu `String text()`;
  - `record Telemetrie(long seq, String ora, double lat, double lon, double viteza, double temperatura, int baterie, boolean dinBuffer)`,
    unde `dinBuffer` apare în JSON ca `din_buffer`;
  - `enum TipAlarma { SUPRAINCALZIRE, FRANARE_BRUSCA }`;
  - `record Alarma(TipAlarma tip, String ora, double valoare)`;
  - în `Mesaje`: `topic(String idMasina, String canal)`,
    `telemetrie(String, Telemetrie)`, `alarma(String, Alarma)` și
    `stare(String, boolean online)`.

- [ ] **Pasul 1: Scrie testele care pică**

`simulator\src\test\java\ro\flota\simulator\mesaje\MesajeTest.java`:

```java
package ro.flota.simulator.mesaje;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ro.flota.simulator.Json;

class MesajeTest {

    private static void assertJson(String asteptat, Mesaj mesaj) {
        assertEquals(Json.MAPPER.readTree(asteptat), Json.MAPPER.readTree(mesaj.payload()));
    }

    @Test
    void telemetriaMergePeQos0SiNuERetinuta() {
        Mesaj m = Mesaje.telemetrie("masina-3", new Telemetrie(
                1042, "2026-12-10T10:15:03.120Z", 46.77121, 23.6236, 47.5, 92.1, 81, false));
        assertEquals(TipMesaj.TELEMETRIE, m.tip());
        assertEquals("flota/masina-3/telemetrie", m.topic());
        assertEquals(0, m.qos());
        assertFalse(m.retain());
        assertJson("""
                {"seq": 1042, "ora": "2026-12-10T10:15:03.120Z", "lat": 46.77121, "lon": 23.6236,
                 "viteza": 47.5, "temperatura": 92.1, "baterie": 81, "din_buffer": false}""", m);
    }

    @Test
    void stareaOnlineEsteRetinutaPeQos1() {
        Mesaj m = Mesaje.stare("masina-3", true);
        assertEquals(TipMesaj.STARE, m.tip());
        assertEquals("flota/masina-3/stare", m.topic());
        assertEquals(1, m.qos());
        assertTrue(m.retain());
        assertEquals("online", m.text());
    }

    @Test
    void stareaOfflineSeScrieOffline() {
        assertEquals("offline", Mesaje.stare("masina-3", false).text());
    }

    @Test
    void alarmaMergePeQos2SiNuERetinuta() {
        Mesaj m = Mesaje.alarma("masina-3", new Alarma(TipAlarma.SUPRAINCALZIRE, "2026-12-10T10:15:03.120Z", 112.4));
        assertEquals(TipMesaj.ALARMA, m.tip());
        assertEquals("flota/masina-3/alarma", m.topic());
        assertEquals(2, m.qos());
        assertFalse(m.retain());
        assertJson("""
                {"tip": "SUPRAINCALZIRE", "ora": "2026-12-10T10:15:03.120Z", "valoare": 112.4}""", m);
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Mesaje`,
`Telemetrie` și `Alarma`.

- [ ] **Pasul 3: Claude scrie forma comună, telemetria și starea**

`TipMesaj.java`:

```java
package ro.flota.simulator.mesaje;

/** Felul unui mesaj. Din faza 4, bufferul aruncă doar TELEMETRIE. */
public enum TipMesaj {
    TELEMETRIE,
    ALARMA,
    STARE
}
```

`Mesaj.java`:

```java
package ro.flota.simulator.mesaje;

import java.nio.charset.StandardCharsets;

/** Un mesaj gata de trimis: unde merge, ce conține și cum se livrează. */
public record Mesaj(TipMesaj tip, String topic, byte[] payload, int qos, boolean retain) {

    public String text() {
        return new String(payload, StandardCharsets.UTF_8);
    }
}
```

`Telemetrie.java`:

```java
package ro.flota.simulator.mesaje;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Ce trimite mașina o dată pe secundă pe flota/<id>/telemetrie (spec §4.2). */
public record Telemetrie(
        long seq,
        String ora,
        double lat,
        double lon,
        double viteza,
        double temperatura,
        int baterie,
        @JsonProperty("din_buffer") boolean dinBuffer) {
}
```

`Mesaje.java`:

```java
package ro.flota.simulator.mesaje;

import java.nio.charset.StandardCharsets;
import ro.flota.simulator.Json;

/** Locul unde se hotărăște, pentru fiecare fel de mesaj, topic-ul, QoS-ul și reținerea (spec §4.1). */
public final class Mesaje {

    private Mesaje() {
    }

    public static String topic(String idMasina, String canal) {
        return "flota/" + idMasina + "/" + canal;
    }

    /** QoS 0: vine o dată pe secundă, iar dacă una se pierde, următoarea o înlocuiește. */
    public static Mesaj telemetrie(String idMasina, Telemetrie t) {
        return new Mesaj(TipMesaj.TELEMETRIE, topic(idMasina, "telemetrie"), Json.MAPPER.writeValueAsBytes(t), 0, false);
    }

    /** QoS 1 și reținut: cine se abonează mai târziu află starea pe loc. */
    public static Mesaj stare(String idMasina, boolean online) {
        byte[] text = (online ? "online" : "offline").getBytes(StandardCharsets.UTF_8);
        return new Mesaj(TipMesaj.STARE, topic(idMasina, "stare"), text, 1, true);
    }
}
```

- [ ] **Pasul 4: Claude explică.** Analogia: `Mesaj` e un plic, cu adresa pe el
  (topic-ul), felul livrării (QoS) și mențiunea „păstrați la ghișeu" (retain).
  `Mesaje` e ghișeul unde se pun aceste etichete, într-un singur loc. Apoi de ce
  QoS-ul se hotărăște după felul mesajului.

- [ ] **Pasul 5: Tudor scrie alarma.** Ce trebuie:
  - `TipAlarma`, cu cele două valori;
  - `record Alarma(TipAlarma tip, String ora, double valoare)`;
  - `Mesaje.alarma(String, Alarma)`, pe canalul `alarma`, cu QoS 2 și fără
    reținere.

  Comentariul de deasupra metodei spune de ce e QoS 2. Cât `Mesaje.alarma` nu
  există, testul nu compilează.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

`TipAlarma.java`:

```java
package ro.flota.simulator.mesaje;

/** Alarmele trimise de mașină. Valoarea e temperatura, respectiv scăderea de viteză. */
public enum TipAlarma {
    SUPRAINCALZIRE,
    FRANARE_BRUSCA
}
```

`Alarma.java`:

```java
package ro.flota.simulator.mesaje;

/** Ce trimite mașina pe flota/<id>/alarma (spec §4.2). */
public record Alarma(TipAlarma tip, String ora, double valoare) {
}
```

În `Mesaje.java`:

```java
    /** QoS 2: e rară și gravă, deci nu are voie nici să se piardă, nici să fie numărată de două ori. */
    public static Mesaj alarma(String idMasina, Alarma a) {
        return new Mesaj(TipMesaj.ALARMA, topic(idMasina, "alarma"), Json.MAPPER.writeValueAsBytes(a), 2, false);
    }
```

</details>

- [ ] **Pasul 6: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu toate cele 4 teste din `MesajeTest` trecute.

- [ ] **Pasul 7: Claude revizuiește alarma,** fără să rescrie.

- [ ] **Pasul 8: Commit-urile.** Claude comite forma, telemetria și starea, iar
  Tudor comite alarma. Testul a fost scris de la început cu tot cu alarmă, deci
  intră în commit-ul lui Tudor, singurul după care testul compilează.

```bash
git add simulator/src/main/java/ro/flota/simulator/mesaje/TipMesaj.java simulator/src/main/java/ro/flota/simulator/mesaje/Mesaj.java simulator/src/main/java/ro/flota/simulator/mesaje/Telemetrie.java simulator/src/main/java/ro/flota/simulator/mesaje/Mesaje.java
git commit -m "Simulator: mesajele de telemetrie si stare

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git add simulator/src/main/java/ro/flota/simulator/mesaje simulator/src/test/java/ro/flota/simulator/mesaje/MesajeTest.java
git commit -m "Simulator: alarma pe QoS 2"
```

---

### Task 8: Viteza

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „întâmplarea pusă
într-o interfață, ca testele să fie deterministe".

**Fișiere:**
- Creează: `simulator\src\main\java\ro\flota\simulator\simulare\Aleator.java`, `Viteza.java`
- Creează: `simulator\src\test\java\ro\flota\simulator\testutil\AleatorScriptat.java`
- Test: `simulator\src\test\java\ro\flota\simulator\simulare\VitezaTest.java`

**Interfețe:**
- Produce:
  - `interface Aleator { double urmator(); static Aleator din(Random r); }`, cu
    valori între 0 (inclusiv) și 1 (exclusiv);
  - `Viteza(Aleator pentruTinta, Aleator pentruFranare, double vitezaInitiala)`,
    cu `double tick()`, care întoarce viteza după o secundă, în km/h,
    `double viteza()` și `double tinta()`;
  - pentru teste: `AleatorScriptat(double implicit, double... valori)`.

- [ ] **Pasul 1: Scrie helper-ul de test**

`simulator\src\test\java\ro\flota\simulator\testutil\AleatorScriptat.java`:

```java
package ro.flota.simulator.testutil;

import java.util.ArrayDeque;
import java.util.Deque;
import ro.flota.simulator.simulare.Aleator;

/** Dă pe rând valorile primite, apoi doar valoarea implicită. Cu 0.99, „nu se întâmplă nimic rar". */
public final class AleatorScriptat implements Aleator {

    private final Deque<Double> valori = new ArrayDeque<>();
    private final double implicit;

    public AleatorScriptat(double implicit, double... valori) {
        this.implicit = implicit;
        for (double v : valori) {
            this.valori.addLast(v);
        }
    }

    @Override
    public double urmator() {
        return valori.isEmpty() ? implicit : valori.removeFirst();
    }
}
```

- [ ] **Pasul 2: Scrie testele care pică**

`simulator\src\test\java\ro\flota\simulator\simulare\VitezaTest.java`:

```java
package ro.flota.simulator.simulare;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import ro.flota.simulator.testutil.AleatorScriptat;

class VitezaTest {

    private static final AleatorScriptat FARA_FRANARE = new AleatorScriptat(0.99);

    /** 0.5 înseamnă „nu e 90", iar al doilea 0.5 dă ținta 30 + 40 × 0.5 = 50. */
    private static Viteza spre50(double initiala) {
        return new Viteza(new AleatorScriptat(0.99, 0.5, 0.5), FARA_FRANARE, initiala);
    }

    @Test
    void primaTintaEsteIntre30Si70() {
        Viteza v = spre50(0);
        v.tick();
        assertEquals(50.0, v.tinta(), 1e-9);
    }

    @Test
    void rarTintaSare90() {
        Viteza v = new Viteza(new AleatorScriptat(0.99, 0.05), FARA_FRANARE, 0);
        v.tick();
        assertEquals(90.0, v.tinta(), 1e-9);
    }

    @Test
    void accelereazaCuCelMult5PeSecunda() {
        Viteza v = spre50(0);
        assertEquals(5.0, v.tick(), 1e-9);
        for (int i = 0; i < 9; i++) {
            v.tick();
        }
        assertEquals(50.0, v.viteza(), 1e-9);
        assertEquals(50.0, v.tick(), 1e-9);
    }

    @Test
    void incetinesteSpreTinta() {
        assertEquals(75.0, spre50(80).tick(), 1e-9);
    }

    @Test
    void tintaNouaVineDupa30DeSecunde() {
        Viteza v = new Viteza(new AleatorScriptat(0.99, 0.5, 0.5, 0.5, 0.0), FARA_FRANARE, 0);
        for (int i = 0; i < 30; i++) {
            v.tick();
        }
        assertEquals(50.0, v.tinta(), 1e-9);
        v.tick();
        assertEquals(30.0, v.tinta(), 1e-9);
    }

    @Test
    void franareaBruscaScade30() {
        Viteza v = new Viteza(new AleatorScriptat(0.99, 0.5, 0.75), new AleatorScriptat(0.99, 0.0), 60);
        assertEquals(30.0, v.tick(), 1e-9);
    }

    @Test
    void subViteza30NuFraneazaBrusc() {
        Viteza v = new Viteza(new AleatorScriptat(0.99, 0.5, 0.5), new AleatorScriptat(0.99, 0.0), 20);
        assertEquals(25.0, v.tick(), 1e-9);
    }
}
```

- [ ] **Pasul 3: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Aleator` și
`Viteza`.

- [ ] **Pasul 4: Scrie `Aleator`**

```java
package ro.flota.simulator.simulare;

import java.util.Random;

/** Sursa de întâmplare: un număr de la 0 (inclusiv) la 1 (exclusiv). În teste se înlocuiește cu o listă. */
@FunctionalInterface
public interface Aleator {

    double urmator();

    static Aleator din(Random random) {
        return random::nextDouble;
    }
}
```

- [ ] **Pasul 5: Scrie `Viteza`**

```java
package ro.flota.simulator.simulare;

/**
 * Viteza unei mașini (spec §4.4). Ținta se schimbă la 30 de secunde; viteza se apropie de ea cu cel mult
 * 5 km/h pe secundă. Rar, mașina frânează brusc. Aleatorul de frânare se consultă doar peste 30 km/h.
 */
public final class Viteza {

    static final double PAS_MAXIM = 5.0;
    static final int SECUNDE_PE_TINTA = 30;
    static final double SANSA_TINTA_90 = 0.10;
    static final double SANSA_FRANARE = 1.0 / 600; // cam o dată la 10 minute
    static final double SCADERE_FRANARE = 30.0;
    static final double VITEZA_MINIMA_FRANARE = 30.0;

    private final Aleator pentruTinta;
    private final Aleator pentruFranare;
    private double viteza;
    private double tinta;
    private int secundePanaLaTintaNoua = 0;

    public Viteza(Aleator pentruTinta, Aleator pentruFranare, double vitezaInitiala) {
        this.pentruTinta = pentruTinta;
        this.pentruFranare = pentruFranare;
        this.viteza = vitezaInitiala;
    }

    /** Trece o secundă. Întoarce viteza nouă, în km/h. */
    public double tick() {
        if (secundePanaLaTintaNoua == 0) {
            tinta = pentruTinta.urmator() < SANSA_TINTA_90 ? 90.0 : 30.0 + 40.0 * pentruTinta.urmator();
            secundePanaLaTintaNoua = SECUNDE_PE_TINTA;
        }
        secundePanaLaTintaNoua--;
        if (viteza >= VITEZA_MINIMA_FRANARE && pentruFranare.urmator() < SANSA_FRANARE) {
            viteza -= SCADERE_FRANARE;
        } else {
            viteza += Math.max(-PAS_MAXIM, Math.min(PAS_MAXIM, tinta - viteza));
        }
        return viteza;
    }

    public double viteza() {
        return viteza;
    }

    public double tinta() {
        return tinta;
    }
}
```

- [ ] **Pasul 6: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`.

- [ ] **Pasul 7: Claude explică.** Analogia: un zar pe care, la teste, îl
  înlocuiești cu unul măsluit, ca să știi dinainte ce iese. Apoi ordinea în care
  se cer numerele și de ce ordinea face parte din contract.

- [ ] **Pasul 8: Commit**

```bash
git add simulator/src
git commit -m "Simulator: viteza, cu intamplarea injectata

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: Temperatura și detectorii de alarmă

**Cine scrie:**
- **`Temperatura` și `DetectorSupraincalzire`:** Claude, ca exemplu explicat.
  Tiparul e „alarma se dă la schimbare, o singură dată pe episod".
- **`DetectorFranare`:** Tudor, ca repetiție a detectorului.

**Fișiere:**
- Creează, în `simulator\src\main\java\ro\flota\simulator\simulare\`:
  `Temperatura.java`, `DetectorSupraincalzire.java` și `DetectorFranare.java`
- Teste, în `simulator\src\test\java\ro\flota\simulator\simulare\`:
  `TemperaturaTest.java`, `DetectorSupraincalzireTest.java` și
  `DetectorFranareTest.java`

**Interfețe:**
- Consumă: `Aleator` și `AleatorScriptat`, din Task 8.
- Produce:
  - `Temperatura(Aleator pentruDefect, Aleator pentruZgomot, double initiala)`,
    cu `double tick(double viteza)` și constanta `Temperatura.DE_BAZA = 85.0`;
  - `DetectorSupraincalzire`, cu `boolean verifica(double temperatura)`;
  - `DetectorFranare`, cu `boolean verifica(double viteza)` și
    `double ultimaScadere()`.

- [ ] **Pasul 1: Scrie testele care pică pentru temperatură și supraîncălzire**

`TemperaturaTest.java`:

```java
package ro.flota.simulator.simulare;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import ro.flota.simulator.testutil.AleatorScriptat;

class TemperaturaTest {

    /** Zgomotul 0.5 înseamnă „fără zgomot": (0.5 × 2 − 1) × 0,3 = 0. */
    private static Temperatura faraDefect(double initiala) {
        return new Temperatura(new AleatorScriptat(0.99), new AleatorScriptat(0.5), initiala);
    }

    @Test
    void seApropieCuJumatateDeGradSpreTintaDupaViteza() {
        Temperatura t = faraDefect(85);
        assertEquals(85.5, t.tick(50), 1e-9); // ținta e 85 + 0,1 × 50 = 90
        for (int i = 0; i < 19; i++) {
            t.tick(50);
        }
        assertEquals(90.0, t.tick(50), 1e-9);
    }

    @Test
    void zgomotulEsteCelMult0_3() {
        Temperatura t = new Temperatura(new AleatorScriptat(0.99), new AleatorScriptat(0.0), 90);
        assertEquals(89.7, t.tick(50), 1e-9);
    }

    @Test
    void defectulUrca2GradePeSecunda20DeSecunde() {
        Temperatura t = new Temperatura(new AleatorScriptat(0.99, 0.0), new AleatorScriptat(0.5), 90);
        double dupa20 = 0;
        for (int i = 0; i < 20; i++) {
            dupa20 = t.tick(50);
        }
        assertEquals(130.0, dupa20, 1e-9);
    }

    @Test
    void dupaDefectSeRacesteCuJumatateDeGrad() {
        Temperatura t = new Temperatura(new AleatorScriptat(0.99, 0.0), new AleatorScriptat(0.5), 90);
        for (int i = 0; i < 20; i++) {
            t.tick(50);
        }
        assertEquals(129.5, t.tick(0), 1e-9); // ținta la viteza 0 e 85
    }
}
```

`DetectorSupraincalzireTest.java`:

```java
package ro.flota.simulator.simulare;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DetectorSupraincalzireTest {

    @Test
    void daAlarmaOSinguraDataPeEpisod() {
        DetectorSupraincalzire d = new DetectorSupraincalzire();
        assertFalse(d.verifica(105));
        assertTrue(d.verifica(111));
        assertFalse(d.verifica(115));
        assertFalse(d.verifica(105)); // între 100 și 110, alarma rămâne consumată
    }

    @Test
    void seRearmeazaSub100() {
        DetectorSupraincalzire d = new DetectorSupraincalzire();
        assertTrue(d.verifica(111));
        assertFalse(d.verifica(99));
        assertTrue(d.verifica(111));
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Temperatura`
și `DetectorSupraincalzire`.

- [ ] **Pasul 3: Scrie `Temperatura` și `DetectorSupraincalzire`**

`Temperatura.java`:

```java
package ro.flota.simulator.simulare;

/**
 * Temperatura motorului (spec §4.4): urcă odată cu viteza, cu un mic zgomot. Rar, o defecțiune o urcă
 * cu 2 °C pe secundă timp de 20 de secunde. Aleatorul de defect nu se consultă cât ține defectul.
 */
public final class Temperatura {

    public static final double DE_BAZA = 85.0;
    static final double PE_KMH = 0.1;
    static final double PAS_MAXIM = 0.5;
    static final double ZGOMOT_MAXIM = 0.3;
    static final double SANSA_DEFECT = 1.0 / 1200;
    static final int SECUNDE_DEFECT = 20;
    static final double URCARE_DEFECT = 2.0;

    private final Aleator pentruDefect;
    private final Aleator pentruZgomot;
    private double temperatura;
    private int secundeDefectRamase = 0;

    public Temperatura(Aleator pentruDefect, Aleator pentruZgomot, double initiala) {
        this.pentruDefect = pentruDefect;
        this.pentruZgomot = pentruZgomot;
        this.temperatura = initiala;
    }

    /** Trece o secundă la viteza dată. Întoarce temperatura nouă, în °C. */
    public double tick(double viteza) {
        if (secundeDefectRamase == 0 && pentruDefect.urmator() < SANSA_DEFECT) {
            secundeDefectRamase = SECUNDE_DEFECT;
        }
        if (secundeDefectRamase > 0) {
            secundeDefectRamase--;
            temperatura += URCARE_DEFECT;
        } else {
            double tinta = DE_BAZA + PE_KMH * viteza;
            temperatura += Math.max(-PAS_MAXIM, Math.min(PAS_MAXIM, tinta - temperatura));
            temperatura += (pentruZgomot.urmator() * 2 - 1) * ZGOMOT_MAXIM;
        }
        return temperatura;
    }
}
```

`DetectorSupraincalzire.java`:

```java
package ro.flota.simulator.simulare;

/** Dă alarma la schimbare (D12): o dată, când temperatura trece de 110. Se rearmează sub 100. */
public final class DetectorSupraincalzire {

    static final double PRAG_ALARMA = 110.0;
    static final double PRAG_REARMARE = 100.0;

    private boolean armat = true;

    public boolean verifica(double temperatura) {
        if (armat && temperatura > PRAG_ALARMA) {
            armat = false;
            return true;
        }
        if (!armat && temperatura < PRAG_REARMARE) {
            armat = true;
        }
        return false;
    }
}
```

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`.

- [ ] **Pasul 5: Claude explică.** Analogia: o sonerie care sună o dată când
  intră cineva, nu cât timp stă omul în ușă. Distanța dintre 110 și 100 e
  *histerezisul*: fără ea, o temperatură care tremură în jur de 110 ar suna
  întruna. Aceeași regulă o aplică serverul în faza 5.

- [ ] **Pasul 6: Scrie testul care pică pentru frânare**

`DetectorFranareTest.java`:

```java
package ro.flota.simulator.simulare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DetectorFranareTest {

    @Test
    void primaVitezaNuPoateFiFranare() {
        assertFalse(new DetectorFranare().verifica(60));
    }

    @Test
    void oScadereDePeste25IntrOSecundaEsteFranareBrusca() {
        DetectorFranare d = new DetectorFranare();
        d.verifica(60);
        assertTrue(d.verifica(30));
        assertEquals(30.0, d.ultimaScadere(), 1e-9);
    }

    @Test
    void exact25NuEsteFranareBrusca() {
        DetectorFranare d = new DetectorFranare();
        d.verifica(60);
        assertFalse(d.verifica(35));
    }

    @Test
    void oIncetinireTreptataNuEsteFranare() {
        DetectorFranare d = new DetectorFranare();
        for (double v = 60; v >= 0; v -= 5) {
            assertFalse(d.verifica(v));
        }
    }

    @Test
    void dupaFranareUrmatoareaSecundaNuMaiDaAlarma() {
        DetectorFranare d = new DetectorFranare();
        d.verifica(60);
        d.verifica(30);
        assertFalse(d.verifica(30));
    }
}
```

- [ ] **Pasul 7: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `DetectorFranare`.

- [ ] **Pasul 8: Tudor scrie `DetectorFranare`.** Indicii:
  - forma e aceeași ca la `DetectorSupraincalzire`: primește câte o valoare pe
    secundă și ține minte ce îi trebuie de la secunda trecută;
  - pragul e o constantă de 25.0, iar condiția e „strict peste".

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

```java
package ro.flota.simulator.simulare;

/** Frânare bruscă: viteza a scăzut cu peste 25 km/h într-o singură secundă (spec §4.4). */
public final class DetectorFranare {

    static final double PRAG_SCADERE = 25.0;

    private Double vitezaAnterioara = null;
    private double ultimaScadere = 0;

    public boolean verifica(double viteza) {
        boolean franare = vitezaAnterioara != null && vitezaAnterioara - viteza > PRAG_SCADERE;
        if (franare) {
            ultimaScadere = vitezaAnterioara - viteza;
        }
        vitezaAnterioara = viteza;
        return franare;
    }

    /** Cu cât a scăzut viteza la ultima frânare bruscă, în km/h. */
    public double ultimaScadere() {
        return ultimaScadere;
    }
}
```

</details>

- [ ] **Pasul 9: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`.

- [ ] **Pasul 10: Claude revizuiește `DetectorFranare`,** fără să rescrie.

- [ ] **Pasul 11: Commit-urile**

```bash
git add simulator/src/main/java/ro/flota/simulator/simulare/Temperatura.java simulator/src/main/java/ro/flota/simulator/simulare/DetectorSupraincalzire.java simulator/src/test/java/ro/flota/simulator/simulare/TemperaturaTest.java simulator/src/test/java/ro/flota/simulator/simulare/DetectorSupraincalzireTest.java
git commit -m "Simulator: temperatura si alarma la schimbare

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git add simulator/src/main/java/ro/flota/simulator/simulare/DetectorFranare.java simulator/src/test/java/ro/flota/simulator/simulare/DetectorFranareTest.java
git commit -m "Simulator: detectorul de franare brusca"
```

---

### Task 10: Motorul

**Cine scrie:** Claude, ca exemplu explicat. E prima jumătate a tiparului
producător-consumator, adică producătorul (D9).

**Fișiere:**
- Creează: `simulator\src\main\java\ro\flota\simulator\masina\Motor.java`
- Test: `simulator\src\test\java\ro\flota\simulator\masina\MotorTest.java`

**Interfețe:**
- Consumă:
  - `DefinitieMasina` (Task 6);
  - `Traseu.punctLa`, `normalizeaza`, `lungime` (Task 5);
  - `Viteza.tick` (Task 8);
  - `Temperatura.tick`, `DetectorSupraincalzire.verifica`,
    `DetectorFranare.verifica` și `ultimaScadere` (Task 9);
  - `Mesaje.telemetrie`, `alarma`, `Telemetrie`, `Alarma`, `TipAlarma` (Task 7);
  - `Ore.format` (Task 2).
- Produce: `Motor(DefinitieMasina def, Viteza viteza, Temperatura temperatura, Clock ceas, BlockingDeque<Mesaj> coada, BooleanSupplier conectat, double secundePeTick)`,
  cu `void tick()`. Motorul pune mesajele la coada cozii, cu `offerLast`.

- [ ] **Pasul 1: Scrie testele care pică**

`MotorTest.java`:

```java
package ro.flota.simulator.masina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.Json;
import ro.flota.simulator.config.DefinitieMasina;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.TipMesaj;
import ro.flota.simulator.simulare.Temperatura;
import ro.flota.simulator.simulare.Viteza;
import ro.flota.simulator.testutil.AleatorScriptat;
import ro.flota.simulator.testutil.Trasee;
import tools.jackson.databind.JsonNode;

class MotorTest {

    private static final Clock CEAS = Clock.fixed(Instant.parse("2026-12-10T10:15:03.120Z"), ZoneOffset.UTC);

    private final BlockingDeque<Mesaj> coada = new LinkedBlockingDeque<>();

    private static Viteza spre50() {
        return new Viteza(new AleatorScriptat(0.99, 0.5, 0.5), new AleatorScriptat(0.99), 0);
    }

    private static Temperatura normala(double initiala) {
        return new Temperatura(new AleatorScriptat(0.99), new AleatorScriptat(0.5), initiala);
    }

    private Motor motor(double start, Viteza viteza, Temperatura temperatura, boolean conectat) {
        DefinitieMasina def = new DefinitieMasina("masina-1", Trasee.patrat(), start, "z1");
        return new Motor(def, viteza, temperatura, CEAS, coada, () -> conectat, 1.0);
    }

    private static JsonNode json(Mesaj m) {
        return Json.MAPPER.readTree(m.payload());
    }

    @Test
    void primulTickPuneOTelemetrieCuSeq1() {
        motor(0, spre50(), normala(85), true).tick();
        assertEquals(1, coada.size());
        Mesaj m = coada.peekFirst();
        assertEquals(TipMesaj.TELEMETRIE, m.tip());
        assertEquals("flota/masina-1/telemetrie", m.topic());
        JsonNode j = json(m);
        assertEquals(1, j.get("seq").asLong());
        assertEquals("2026-12-10T10:15:03.120Z", j.get("ora").asString());
        assertEquals(5.0, j.get("viteza").asDouble(), 1e-9);
        assertEquals(100, j.get("baterie").asInt());
        assertFalse(j.get("din_buffer").asBoolean());
    }

    @Test
    void seqCresteCuUnu() {
        Motor motor = motor(0, spre50(), normala(85), true);
        motor.tick();
        motor.tick();
        motor.tick();
        assertEquals(3, json(coada.peekLast()).get("seq").asLong());
    }

    @Test
    void dinBufferSpuneCaRadioulEraDeconectat() {
        motor(0, spre50(), normala(85), false).tick();
        assertTrue(json(coada.peekFirst()).get("din_buffer").asBoolean());
    }

    @Test
    void masinaInainteazaPeTraseuCuVitezaEi() {
        Motor motor = motor(0, spre50(), normala(85), true);
        for (int i = 0; i < 10; i++) {
            motor.tick(); // 5 + 10 + ... + 50 = 275 km/h·s, adică 76,389 m spre nord
        }
        JsonNode j = json(coada.peekLast());
        assertEquals(46.00069, j.get("lat").asDouble(), 1e-9);
        assertEquals(23.0, j.get("lon").asDouble(), 1e-9);
    }

    @Test
    void startulDinFlotaMutaMasinaPeBucla() {
        motor(0.25, spre50(), normala(85), true).tick(); // 94,218 m + 1,389 m
        assertEquals(46.00086, json(coada.peekFirst()).get("lat").asDouble(), 1e-9);
    }

    @Test
    void bateriaScadeCuUnProcentPeMinut() {
        Motor motor = motor(0, spre50(), normala(85), true);
        for (int i = 0; i < 61; i++) {
            motor.tick();
        }
        assertEquals(99, json(coada.peekLast()).get("baterie").asInt());
    }

    @Test
    void supraincalzireaPuneOAlarmaDupaTelemetrie() {
        Temperatura cuDefect = new Temperatura(new AleatorScriptat(0.99, 0.0), new AleatorScriptat(0.5), 109.8);
        motor(0, spre50(), cuDefect, true).tick();
        assertEquals(2, coada.size());
        Mesaj alarma = coada.peekLast();
        assertEquals(TipMesaj.ALARMA, alarma.tip());
        assertEquals(2, alarma.qos());
        assertEquals("SUPRAINCALZIRE", json(alarma).get("tip").asString());
        assertEquals(111.8, json(alarma).get("valoare").asDouble(), 1e-9);
    }

    @Test
    void franareaBruscaPuneOAlarmaCuScaderea() {
        Viteza franeaza = new Viteza(new AleatorScriptat(0.99, 0.5, 0.75), new AleatorScriptat(0.99, 0.99, 0.0), 60);
        Motor motor = motor(0, franeaza, normala(85), true);
        motor.tick(); // 60
        motor.tick(); // 30
        Mesaj alarma = coada.peekLast();
        assertEquals(TipMesaj.ALARMA, alarma.tip());
        assertEquals("FRANARE_BRUSCA", json(alarma).get("tip").asString());
        assertEquals(30.0, json(alarma).get("valoare").asDouble(), 1e-9);
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Motor`.

- [ ] **Pasul 3: Scrie `Motor`**

```java
package ro.flota.simulator.masina;

import java.time.Clock;
import java.util.concurrent.BlockingDeque;
import java.util.function.BooleanSupplier;
import ro.flota.simulator.config.DefinitieMasina;
import ro.flota.simulator.geo.Punct;
import ro.flota.simulator.geo.Traseu;
import ro.flota.simulator.mesaje.Alarma;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;
import ro.flota.simulator.mesaje.Ore;
import ro.flota.simulator.mesaje.Telemetrie;
import ro.flota.simulator.mesaje.TipAlarma;
import ro.flota.simulator.simulare.DetectorFranare;
import ro.flota.simulator.simulare.DetectorSupraincalzire;
import ro.flota.simulator.simulare.Temperatura;
import ro.flota.simulator.simulare.Viteza;

/**
 * Producătorul (D9): la fiecare bătaie mută mașina, calculează valorile și pune mesajele în coadă.
 * Nu știe nimic de rețea, ci doar dacă radioul e conectat, ca să marcheze din_buffer.
 */
public final class Motor {

    private final String id;
    private final Traseu traseu;
    private final Viteza viteza;
    private final Temperatura temperatura;
    private final DetectorSupraincalzire supraincalzire = new DetectorSupraincalzire();
    private final DetectorFranare franare = new DetectorFranare();
    private final Clock ceas;
    private final BlockingDeque<Mesaj> coada;
    private final BooleanSupplier conectat;
    private final double secundePeTick;
    private double distanta;
    private double secundeScurse = 0;
    private long seq = 0;

    public Motor(DefinitieMasina def, Viteza viteza, Temperatura temperatura, Clock ceas,
                 BlockingDeque<Mesaj> coada, BooleanSupplier conectat, double secundePeTick) {
        this.id = def.id();
        this.traseu = def.traseu();
        this.viteza = viteza;
        this.temperatura = temperatura;
        this.ceas = ceas;
        this.coada = coada;
        this.conectat = conectat;
        this.secundePeTick = secundePeTick;
        this.distanta = def.start() * traseu.lungime();
    }

    public void tick() {
        double v = viteza.tick();
        double t = temperatura.tick(v);
        distanta = traseu.normalizeaza(distanta + v / 3.6 * secundePeTick);
        secundeScurse += secundePeTick;
        Punct p = traseu.punctLa(distanta);
        String ora = Ore.format(ceas.instant());
        seq++;
        coada.offerLast(Mesaje.telemetrie(id, new Telemetrie(seq, ora,
                rotunjit(p.lat(), 5), rotunjit(p.lon(), 5), rotunjit(v, 1), rotunjit(t, 1),
                baterie(), !conectat.getAsBoolean())));
        if (supraincalzire.verifica(t)) {
            coada.offerLast(Mesaje.alarma(id, new Alarma(TipAlarma.SUPRAINCALZIRE, ora, rotunjit(t, 1))));
        }
        if (franare.verifica(v)) {
            coada.offerLast(Mesaje.alarma(id, new Alarma(TipAlarma.FRANARE_BRUSCA, ora,
                    rotunjit(franare.ultimaScadere(), 1))));
        }
    }

    /** Scade cu 1% pe minut, de la 100 la 5, apoi rămâne la 5. */
    private int baterie() {
        return (int) Math.max(5, 100 - Math.floor(secundeScurse / 60.0));
    }

    private static double rotunjit(double x, int zecimale) {
        double f = Math.pow(10, zecimale);
        return Math.round(x * f) / f;
    }
}
```

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`.

- [ ] **Pasul 5: Claude explică.** Analogia: motorul e bucătarul care pune
  farfuriile pe tejghea, iar radioul e chelnerul care le duce. Bucătarul nu se
  oprește când chelnerul e plecat; farfuriile se adună pe tejghea, iar tejgheaua
  e bufferul. Apoi de ce `din_buffer` îl pune producătorul, nu serverul (D10).

- [ ] **Pasul 6: Commit**

```bash
git add simulator/src
git commit -m "Simulator: motorul, producatorul de mesaje

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 11: Conexiunea și radioul

**Cine scrie:** Claude, ca exemplu explicat. E a doua jumătate a tiparului,
consumatorul, plus singura clasă care vorbește cu biblioteca MQTT.

**Fișiere:**
- Modifică: `simulator\pom.xml`, adaugi HiveMQ
- Creează, în `simulator\src\main\java\ro\flota\simulator\masina\`:
  `Conexiune.java`, `EroareConexiune.java`, `Somn.java`, `Radio.java` și
  `ConexiuneHiveMq.java`
- Creează: `simulator\src\test\java\ro\flota\simulator\masina\ConexiuneFalsa.java`
- Test: `simulator\src\test\java\ro\flota\simulator\masina\RadioTest.java`

**Interfețe:**
- Consumă: `Mesaj`, `Mesaje.stare`, `TipMesaj`, `Telemetrie` (Task 7) și
  `Setari` (Task 6).
- Produce:
  - `interface Conexiune`, cu metodele
    `void conecteaza() throws EroareConexiune`,
    `void publica(Mesaj) throws EroareConexiune`, `boolean conectata()` și
    `void deconecteaza()`;
  - `class EroareConexiune extends Exception`, cu constructorul
    `(String, Throwable)`;
  - `interface Somn { void dormi(long ms) throws InterruptedException; }`;
  - `Radio(String id, Conexiune conexiune, BlockingDeque<Mesaj> coada, Somn somn)`,
    care implementează `Runnable` și are `void pas()`, package-private, plus
    `void opreste()`;
  - `ConexiuneHiveMq(String id, Setari setari)`;
  - pentru teste: `ConexiuneFalsa`, cu câmpurile `publicate`, `evenimente`,
    `conectari` și metodele `esueazaLaConectare(int)`,
    `esueazaLaPublicare(int)`, `pierdeConexiunea()` și `conecteazaDirect()`.

- [ ] **Pasul 1: Scrie interfețele `Conexiune`, `EroareConexiune` și `Somn`**

`Conexiune.java`:

```java
package ro.flota.simulator.masina;

import ro.flota.simulator.mesaje.Mesaj;

/** Legătura mașinii cu broker-ul. Implementarea reală e ConexiuneHiveMq, iar testele folosesc una falsă. */
public interface Conexiune {

    void conecteaza() throws EroareConexiune;

    void publica(Mesaj mesaj) throws EroareConexiune;

    boolean conectata();

    void deconecteaza();
}
```

`EroareConexiune.java`:

```java
package ro.flota.simulator.masina;

/** Conectarea sau publicarea n-a reușit. E verificată de compilator, ca radioul să fie obligat s-o trateze. */
public final class EroareConexiune extends Exception {

    public EroareConexiune(String mesaj, Throwable cauza) {
        super(mesaj, cauza);
    }
}
```

`Somn.java`:

```java
package ro.flota.simulator.masina;

/** O pauză. În program e Thread::sleep; în teste, o listă care ține minte cât s-a „dormit". */
@FunctionalInterface
public interface Somn {

    void dormi(long ms) throws InterruptedException;
}
```

- [ ] **Pasul 2: Scrie conexiunea falsă**

`simulator\src\test\java\ro\flota\simulator\masina\ConexiuneFalsa.java`:

```java
package ro.flota.simulator.masina;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.TipMesaj;

/** O conexiune de test: ține minte ce s-a publicat și poate eșua la comandă. O pot folosi mai multe fire. */
final class ConexiuneFalsa implements Conexiune {

    final List<Mesaj> publicate = new CopyOnWriteArrayList<>();
    final List<String> evenimente = new CopyOnWriteArrayList<>();
    int conectari;

    private boolean conectata;
    private int esecuriLaConectare;
    private int esecuriLaPublicare;

    synchronized void esueazaLaConectare(int ori) {
        esecuriLaConectare = ori;
    }

    synchronized void esueazaLaPublicare(int ori) {
        esecuriLaPublicare = ori;
    }

    synchronized void pierdeConexiunea() {
        conectata = false;
        evenimente.add("pierdută");
    }

    synchronized void conecteazaDirect() {
        conectata = true;
    }

    @Override
    public synchronized void conecteaza() throws EroareConexiune {
        conectari++;
        if (esecuriLaConectare > 0) {
            esecuriLaConectare--;
            evenimente.add("conectare refuzată");
            throw new EroareConexiune("refuzat", null);
        }
        conectata = true;
        evenimente.add("conectată");
    }

    @Override
    public synchronized void publica(Mesaj mesaj) throws EroareConexiune {
        if (!conectata) {
            throw new EroareConexiune("nu e conectată", null);
        }
        if (esecuriLaPublicare > 0) {
            esecuriLaPublicare--;
            throw new EroareConexiune("publicare eșuată", null);
        }
        publicate.add(mesaj);
        evenimente.add("publică " + mesaj.topic() + (mesaj.tip() == TipMesaj.STARE ? " " + mesaj.text() : ""));
    }

    @Override
    public synchronized boolean conectata() {
        return conectata;
    }

    @Override
    public synchronized void deconecteaza() {
        conectata = false;
        evenimente.add("deconectată");
    }
}
```

- [ ] **Pasul 3: Scrie testele care pică pentru `Radio`**

`RadioTest.java`:

```java
package ro.flota.simulator.masina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.Json;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;
import ro.flota.simulator.mesaje.Telemetrie;
import ro.flota.simulator.mesaje.TipMesaj;

class RadioTest {

    private final ConexiuneFalsa conexiune = new ConexiuneFalsa();
    private final BlockingDeque<Mesaj> coada = new LinkedBlockingDeque<>();
    private final List<Long> somnuri = new ArrayList<>();
    private final Radio radio = new Radio("masina-1", conexiune, coada, somnuri::add);

    private static Mesaj telemetrie(long seq) {
        return Mesaje.telemetrie("masina-1",
                new Telemetrie(seq, "2026-12-10T10:15:03.120Z", 46, 23, 50, 90, 100, false));
    }

    private List<Long> seqTrimise() {
        return conexiune.publicate.stream()
                .filter(m -> m.tip() == TipMesaj.TELEMETRIE)
                .map(m -> Json.MAPPER.readTree(m.payload()).get("seq").asLong())
                .toList();
    }

    @Test
    void primulPasSeConecteazaSiAnuntaOnline() throws Exception {
        radio.pas();
        assertEquals(1, conexiune.conectari);
        Mesaj stare = conexiune.publicate.get(0);
        assertEquals(TipMesaj.STARE, stare.tip());
        assertEquals("online", stare.text());
        assertTrue(stare.retain());
    }

    @Test
    void golesteCoadaInOrdine() throws Exception {
        coada.add(telemetrie(1));
        coada.add(telemetrie(2));
        radio.pas();
        radio.pas();
        radio.pas();
        assertEquals(List.of(1L, 2L), seqTrimise());
        assertTrue(coada.isEmpty());
    }

    @Test
    void brokerIndisponibilLaPornire() throws Exception {
        conexiune.esueazaLaConectare(2);
        coada.add(telemetrie(1));
        radio.pas();
        radio.pas();
        assertEquals(List.of(5_000L, 5_000L), somnuri);
        assertTrue(conexiune.publicate.isEmpty());
        radio.pas(); // a treia încercare reușește
        radio.pas();
        assertEquals(3, conexiune.conectari);
        assertEquals(List.of(1L), seqTrimise());
    }

    @Test
    void mesajulNuSePierdeCandPublicareaEsueaza() throws Exception {
        radio.pas(); // conectat
        Mesaj primul = telemetrie(1);
        coada.add(primul);
        coada.add(telemetrie(2));
        conexiune.esueazaLaPublicare(1);
        radio.pas();
        assertSame(primul, coada.peekFirst());
        assertEquals(List.of(1_000L), somnuri);
        conexiune.pierdeConexiunea();
        radio.pas(); // reconectare și „online"
        radio.pas();
        radio.pas();
        assertEquals(List.of(1L, 2L), seqTrimise());
    }

    @Test
    void oprireaIeseDinBucla() throws Exception {
        Radio real = new Radio("masina-1", conexiune, coada, Thread::sleep);
        Thread fir = new Thread(real);
        fir.start();
        long limita = System.currentTimeMillis() + 2_000;
        while (!conexiune.conectata() && System.currentTimeMillis() < limita) {
            Thread.sleep(10);
        }
        real.opreste();
        fir.join(3_000);
        assertFalse(fir.isAlive());
    }
}
```

- [ ] **Pasul 4: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Radio`.

- [ ] **Pasul 5: Scrie `Radio`**

```java
package ro.flota.simulator.masina;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;

/**
 * Consumatorul (D9): ține conexiunea și golește coada, în ordine. Un mesaj care n-a putut pleca revine
 * primul la rând. Faza 1 reîncearcă la 5 secunde fix; faza 4 aduce pauza care se dublează.
 */
public final class Radio implements Runnable {

    static final long PAUZA_RECONECTARE_MS = 5_000;
    static final long PAUZA_DUPA_EROARE_MS = 1_000;
    static final long ASTEPTARE_COADA_MS = 1_000;

    private final String id;
    private final Conexiune conexiune;
    private final BlockingDeque<Mesaj> coada;
    private final Somn somn;
    private volatile boolean oprit = false;
    private boolean anuntatIndisponibil = false;

    public Radio(String id, Conexiune conexiune, BlockingDeque<Mesaj> coada, Somn somn) {
        this.id = id;
        this.conexiune = conexiune;
        this.coada = coada;
        this.somn = somn;
    }

    @Override
    public void run() {
        while (!oprit) {
            try {
                pas();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void opreste() {
        oprit = true;
    }

    /** Un pas: dacă nu e conectat, încearcă o dată; altfel trimite următorul mesaj din coadă. */
    void pas() throws InterruptedException {
        if (!conexiune.conectata()) {
            incearcaSaTeConectezi();
            return;
        }
        Mesaj mesaj = coada.pollFirst(ASTEPTARE_COADA_MS, TimeUnit.MILLISECONDS);
        if (mesaj == null) {
            return;
        }
        try {
            conexiune.publica(mesaj);
        } catch (EroareConexiune e) {
            coada.offerFirst(mesaj);
            System.out.printf("[%s] n-am putut trimite (%s); păstrez mesajul%n", id, e.getMessage());
            somn.dormi(PAUZA_DUPA_EROARE_MS);
        }
    }

    private void incearcaSaTeConectezi() throws InterruptedException {
        try {
            conexiune.conecteaza();
            conexiune.publica(Mesaje.stare(id, true));
            System.out.printf("[%s] conectată%n", id);
            anuntatIndisponibil = false;
        } catch (EroareConexiune e) {
            if (!anuntatIndisponibil) {
                System.out.printf("[%s] broker indisponibil (%s); reîncerc la 5 secunde%n", id, e.getMessage());
                anuntatIndisponibil = true;
            }
            somn.dormi(PAUZA_RECONECTARE_MS);
        }
    }
}
```

- [ ] **Pasul 6: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu toate cele 5 teste din `RadioTest` trecute.

- [ ] **Pasul 7: Adaugă HiveMQ în `simulator\pom.xml`**, în `<dependencies>`:

```xml
    <dependency>
      <groupId>com.hivemq</groupId>
      <artifactId>hivemq-mqtt-client</artifactId>
      <version>${hivemq.version}</version>
    </dependency>
```

- [ ] **Pasul 8: Scrie `ConexiuneHiveMq`**

```java
package ro.flota.simulator.masina;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import ro.flota.simulator.config.Setari;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;

/** Singura clasă din simulator care știe de biblioteca HiveMQ (D17, R4). MQTT 3.1.1. */
public final class ConexiuneHiveMq implements Conexiune {

    private static final long SECUNDE_CONECTARE = 5;
    private static final long SECUNDE_PUBLICARE = 10;

    private final String id;
    private final int keepalive;
    private final Mqtt3BlockingClient client;
    private volatile boolean conectata = false;

    public ConexiuneHiveMq(String id, Setari setari) {
        this.id = id;
        this.keepalive = setari.keepaliveSecunde();
        this.client = Mqtt3Client.builder()
                .identifier(id)
                .transportConfig()
                    .serverHost(setari.brokerHost())
                    .serverPort(setari.brokerPort())
                    .socketConnectTimeout(SECUNDE_CONECTARE, TimeUnit.SECONDS)
                    .mqttConnectTimeout(SECUNDE_CONECTARE, TimeUnit.SECONDS)
                    .applyTransportConfig()
                .addConnectedListener(context -> conectata = true)
                .addDisconnectedListener(context -> {
                    if (conectata) {
                        System.out.printf("[%s] conexiune pierdută: %s%n", id, context.getCause().getMessage());
                    }
                    conectata = false;
                })
                .buildBlocking();
    }

    @Override
    public void conecteaza() throws EroareConexiune {
        try {
            client.connectWith()
                    .cleanSession(false)
                    .keepAlive(keepalive)
                    .willPublish()
                        .topic(Mesaje.topic(id, "stare"))
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .payload("offline".getBytes(StandardCharsets.UTF_8))
                        .retain(true)
                        .applyWillPublish()
                    .send();
        } catch (RuntimeException e) {
            throw new EroareConexiune(e.getMessage(), e);
        }
    }

    @Override
    public void publica(Mesaj mesaj) throws EroareConexiune {
        try {
            client.toAsync().publishWith()
                    .topic(mesaj.topic())
                    .qos(qos(mesaj.qos()))
                    .retain(mesaj.retain())
                    .payload(mesaj.payload())
                    .send()
                    .get(SECUNDE_PUBLICARE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EroareConexiune("publicare întreruptă", e);
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            throw new EroareConexiune(e.getMessage(), e);
        }
    }

    @Override
    public boolean conectata() {
        return conectata;
    }

    @Override
    public void deconecteaza() {
        try {
            client.disconnect();
        } catch (RuntimeException e) {
            // Era deja deconectată.
        }
        conectata = false;
    }

    private static MqttQos qos(int nivel) {
        return switch (nivel) {
            case 0 -> MqttQos.AT_MOST_ONCE;
            case 1 -> MqttQos.AT_LEAST_ONCE;
            case 2 -> MqttQos.EXACTLY_ONCE;
            default -> throw new IllegalArgumentException("QoS necunoscut: " + nivel);
        };
    }
}
```

- [ ] **Pasul 9: Verifică că se compilează și că testele trec în continuare**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`. `ConexiuneHiveMq` n-are test unitar, fiindcă îi
trebuie un broker. Se verifică de la cap la coadă în Task 13.

- [ ] **Pasul 10: Claude explică.** Analogia, partea a doua: chelnerul duce
  farfuriile în ordine. Dacă scapă una, o pune înapoi prima pe tejghea, nu la
  coadă. Apoi:
  - de ce `Conexiune` e o interfață: testele n-au nevoie de broker, iar
    biblioteca se schimbă într-un singur loc;
  - ce înseamnă `cleanSession(false)`, keepalive-ul și Last Will-ul în codul
    real.

- [ ] **Pasul 11: Commit**

```bash
git add simulator/pom.xml simulator/src
git commit -m "Simulator: radioul si conexiunea HiveMQ

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 12: Mașina, `Main` și rularea

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „cum leagi piesele și cum
oprești curat un program cu mai multe fire".

**Fișiere:**
- Modifică: `simulator\pom.xml`, adaugi `exec-maven-plugin`
- Creează: `simulator\src\main\java\ro\flota\simulator\masina\Masina.java`
- Creează: `simulator\src\main\java\ro\flota\simulator\Main.java`
- Modifică: `README.md`, partea despre simulator și teste
- Test: `simulator\src\test\java\ro\flota\simulator\masina\MasinaTest.java`

**Interfețe:**
- Consumă: tot ce produc Task-urile 5–11.
- Produce: `Masina(DefinitieMasina def, Setari setari, Conexiune conexiune, Clock ceas, Random random)`,
  cu `void porneste()` și `void opreste() throws InterruptedException`.

- [ ] **Pasul 1: Scrie testele care pică**

`MasinaTest.java`:

```java
package ro.flota.simulator.masina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.config.DefinitieMasina;
import ro.flota.simulator.config.Setari;
import ro.flota.simulator.mesaje.TipMesaj;
import ro.flota.simulator.testutil.Trasee;

class MasinaTest {

    private static final Setari RAPID = new Setari("127.0.0.1", 1883, 5, 100);
    private final DefinitieMasina def = new DefinitieMasina("masina-1", Trasee.patrat(), 0.0, "z1");

    private static List<String> ultimele2(List<String> evenimente) {
        return evenimente.subList(evenimente.size() - 2, evenimente.size());
    }

    @Test
    void oprireaAnuntaOfflineInainteDeDeconectare() throws Exception {
        ConexiuneFalsa conexiune = new ConexiuneFalsa();
        conexiune.conecteazaDirect();
        new Masina(def, RAPID, conexiune, Clock.systemUTC(), new Random(1)).opreste();
        assertEquals(List.of("publică flota/masina-1/stare offline", "deconectată"), ultimele2(conexiune.evenimente));
    }

    @Test
    void pornitaTrimiteTelemetrieSiSeOpresteCurat() throws Exception {
        ConexiuneFalsa conexiune = new ConexiuneFalsa();
        Masina masina = new Masina(def, RAPID, conexiune, Clock.systemUTC(), new Random(1));
        masina.porneste();
        long limita = System.currentTimeMillis() + 3_000;
        while (conexiune.publicate.stream().noneMatch(m -> m.tip() == TipMesaj.TELEMETRIE)
                && System.currentTimeMillis() < limita) {
            Thread.sleep(20);
        }
        masina.opreste();
        assertTrue(conexiune.publicate.stream().anyMatch(m -> m.tip() == TipMesaj.TELEMETRIE),
                "nicio telemetrie în 3 secunde");
        assertEquals(List.of("publică flota/masina-1/stare offline", "deconectată"), ultimele2(conexiune.evenimente));
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Masina`.

- [ ] **Pasul 3: Scrie `Masina`**

```java
package ro.flota.simulator.masina;

import java.time.Clock;
import java.util.Random;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import ro.flota.simulator.config.DefinitieMasina;
import ro.flota.simulator.config.Setari;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;
import ro.flota.simulator.simulare.Aleator;
import ro.flota.simulator.simulare.Temperatura;
import ro.flota.simulator.simulare.Viteza;

/** O mașină: motorul pe firul lui, radioul pe al lui, iar între ele coada (D9). */
public final class Masina {

    private final String id;
    private final Conexiune conexiune;
    private final BlockingDeque<Mesaj> coada = new LinkedBlockingDeque<>();
    private final Motor motor;
    private final Radio radio;
    private final long perioadaMs;
    private ScheduledExecutorService firMotor;
    private Thread firRadio;

    public Masina(DefinitieMasina def, Setari setari, Conexiune conexiune, Clock ceas, Random random) {
        this.id = def.id();
        this.conexiune = conexiune;
        this.perioadaMs = setari.perioadaMs();
        Viteza viteza = new Viteza(Aleator.din(random), Aleator.din(random), 0);
        Temperatura temperatura = new Temperatura(Aleator.din(random), Aleator.din(random), Temperatura.DE_BAZA);
        this.motor = new Motor(def, viteza, temperatura, ceas, coada, conexiune::conectata, perioadaMs / 1000.0);
        this.radio = new Radio(id, conexiune, coada, Thread::sleep);
    }

    public void porneste() {
        firRadio = new Thread(radio, id + "-radio");
        firRadio.start();
        firMotor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, id + "-motor"));
        firMotor.scheduleAtFixedRate(this::tickProtejat, 0, perioadaMs, TimeUnit.MILLISECONDS);
    }

    /** O excepție scăpată dintr-o sarcină programată o oprește în tăcere, de aceea o prindem aici. */
    private void tickProtejat() {
        try {
            motor.tick();
        } catch (RuntimeException e) {
            System.out.printf("[%s] eroare în motor: %s%n", id, e);
        }
    }

    /** Oprirea curată: motorul, apoi radioul, apoi mașina își anunță singură „offline" și se deconectează. */
    public void opreste() throws InterruptedException {
        if (firMotor != null) {
            firMotor.shutdownNow();
            firMotor.awaitTermination(2, TimeUnit.SECONDS);
        }
        radio.opreste();
        if (firRadio != null) {
            firRadio.interrupt();
            firRadio.join(2_000);
        }
        if (conexiune.conectata()) {
            try {
                conexiune.publica(Mesaje.stare(id, false));
            } catch (EroareConexiune e) {
                System.out.printf("[%s] n-am putut anunța offline: %s%n", id, e.getMessage());
            }
        }
        conexiune.deconecteaza();
    }
}
```

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`.

- [ ] **Pasul 5: Scrie `Main`**

```java
package ro.flota.simulator;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import ro.flota.simulator.config.DefinitieMasina;
import ro.flota.simulator.config.EroareConfigurare;
import ro.flota.simulator.config.Flota;
import ro.flota.simulator.config.Setari;
import ro.flota.simulator.masina.ConexiuneHiveMq;
import ro.flota.simulator.masina.Masina;

/** Pornește flota din date\ și o oprește curat la Ctrl+C. Argumentul e dosarul cu date. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        Path dosarDate = Path.of(args.length > 0 ? args[0] : "date");
        Setari setari = Setari.implicite();
        List<DefinitieMasina> flota;
        try {
            flota = Flota.citeste(dosarDate);
        } catch (EroareConfigurare e) {
            System.err.println("Configurare greșită, nu pornesc nicio mașină. " + e.getMessage());
            System.exit(1);
            return;
        }
        System.out.printf("Pornesc %d mașini spre %s:%d, cu keepalive de %d s%n",
                flota.size(), setari.brokerHost(), setari.brokerPort(), setari.keepaliveSecunde());

        List<Masina> masini = new ArrayList<>();
        for (DefinitieMasina def : flota) {
            masini.add(new Masina(def, setari, new ConexiuneHiveMq(def.id(), setari), Clock.systemUTC(), new Random()));
        }
        CountDownLatch oprita = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Opresc flota...");
            for (Masina m : masini) {
                try {
                    m.opreste();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            oprita.countDown();
        }));
        masini.forEach(Masina::porneste);
        oprita.await();
    }
}
```

- [ ] **Pasul 6: Adaugă `exec-maven-plugin` în `simulator\pom.xml`**, după
  `</dependencies>`:

```xml
  <build>
    <plugins>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <configuration>
          <mainClass>ro.flota.simulator.Main</mainClass>
          <arguments>
            <argument>${project.basedir}/../date</argument>
          </arguments>
        </configuration>
      </plugin>
    </plugins>
  </build>
```

- [ ] **Pasul 7: Rulează simulatorul o dată, scurt**

Cu broker-ul pornit în terminalul A, ca la Task 3 pasul 3:

```powershell
.\mvnw.cmd -q -pl simulator compile exec:java
```

Se așteaptă:
- `Pornesc 10 mașini spre 127.0.0.1:1883, cu keepalive de 5 s`, apoi zece
  rânduri `[masina-N] conectată`;
- în A, zece rânduri de forma `New client connected from 127.0.0.1:… as masina-N (p2, c0, k5…)`.
  `p2` înseamnă MQTT 3.1.1, `c0` sesiunea persistentă, iar `k5` keepalive-ul.

Ctrl+C, iar la „Terminate batch job (Y/N)?" răspunzi `Y`.

- [ ] **Pasul 8: Completează `README.md`**, după pasul 2 din „Cum pornești":

````markdown
3. Simulatorul, într-un al treilea terminal:

   ```powershell
   .\mvnw.cmd -q -pl simulator compile exec:java
   ```

   Se oprește cu Ctrl+C. La „Terminate batch job (Y/N)?" răspunzi Y.

## Testele

```powershell
.\mvnw.cmd -q test
```
````

- [ ] **Pasul 9: Claude explică.** Analogia: o tură de taxi care se termină. Șoferul
  oprește motorul, închide stația, apoi spune singur la dispecerat „ies din
  tură", înainte să închidă telefonul. Last Will-ul e mesajul pe care dispeceratul
  îl dă în locul lui, dacă dispare fără să anunțe. Apoi:
  - ce e un shutdown hook;
  - de ce `tickProtejat` prinde excepțiile;
  - de ce `c0 k5` din log-ul Mosquitto sînt chiar `cleanSession(false)` și
    `keepAlive(5)`.

- [ ] **Pasul 10: Commit**

```bash
git add simulator/pom.xml simulator/src README.md
git commit -m "Simulator: masina, pornirea si oprirea curata

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 13: Verificarea fazei 1, de la cap la coadă

**Cine face:** Tudor rulează, iar Claude citește ieșirile și le verifică.

**Fișiere:**
- Modifică, în AIS-OS: `Facultate\memory\stare.md`

**Interfețe:** nu are.

- [ ] **Pasul 1: Toate testele**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, fără niciun test picat.

- [ ] **Pasul 2: Zece mașini raportează**

Ai trei terminale: A, cu broker-ul, ca la Task 3 pasul 3; B, cu
`mosquitto_sub -h 127.0.0.1 -t "flota/#" -v`; C, cu simulatorul.

Se așteaptă în B:
- câte un `flota/masina-N/stare online`;
- apoi, în fiecare secundă, câte un `flota/masina-N/telemetrie {...}` pentru
  fiecare mașină, cu `seq` care crește și `lat`/`lon` care se schimbă;
- primele mesaje pot avea `din_buffer: true`: au fost produse înainte de
  conectare.

- [ ] **Pasul 3: Starea e reținută**

Oprește B și pornește
`mosquitto_sub -h 127.0.0.1 -t "flota/+/stare" -v`.
Se așteaptă: imediat, 10 rânduri `online`.

- [ ] **Pasul 4: Oprirea curată**

Ctrl+C în C. Se așteaptă:
- în B, 10 rânduri `offline`;
- în A, `Client masina-N disconnected.` (deconectare curată, fără Will).

- [ ] **Pasul 5: Last Will-ul**

Pornește din nou simulatorul, apoi oprește-l brutal, din alt terminal:

```powershell
Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object { $_.CommandLine -like '*plexus-classworlds*' } | Select-Object ProcessId
Stop-Process -Id <ProcessId> -Force
```

Se așteaptă:
- în B, 10 rânduri `offline`, trimise de broker din Last Will-uri, fiindcă
  mașinile n-au apucat să le trimită;
- în A, `Client masina-N closed its connection.` sau `Socket error on client …`.

Will-ul vine imediat, fiindcă Windows închide socket-urile procesului omorât.
Cazul în care socket-ul rămâne deschis, adică gaura neagră, vine în faza 3.

- [ ] **Pasul 6: Broker-ul e oprit la pornire (review, punctul 1)**

Oprește broker-ul (Ctrl+C în A) și pornește simulatorul. Se așteaptă: câte un
singur rând `[masina-N] broker indisponibil (…); reîncerc la 5 secunde` pe
fiecare mașină. Pornește broker-ul. În cel mult 5 secunde apar 10 rânduri
`[masina-N] conectată`, iar în B vin mesajele.

- [ ] **Pasul 7: Broker-ul cade în timpul rulării (review, punctul 2)**

Cu simulatorul pornit, oprește broker-ul și așteaptă 15 secunde. Se așteaptă
`[masina-N] conexiune pierdută: …`. Pornește broker-ul și B. În cel mult 5
secunde mașinile se reconectează, iar în B vin întâi mesajele din cele 15
secunde, cu `din_buffer: true`, apoi cele live.

- [ ] **Pasul 8: Alarmele**

Abonează-te la `flota/+/alarma` și lasă simulatorul să ruleze 5 minute. La 10
mașini, șansele dau în medie vreo 5 frânări bruște și 2–3 supraîncălziri în 5
minute. Se așteaptă: cel puțin o alarmă, cu `tip` și `valoare`.

- [ ] **Pasul 9: Actualizează starea în AIS-OS**

În `Facultate\memory\stare.md`, la blocul de RC, „Unde am rămas" devine, cu data
zilei în care se termină pasul 8, în formatul AAAA-LL-ZZ: `Fazele 0 și 1 sînt gata
pe AAAA-LL-ZZ: zece mașini raportează prin Mosquitto, iar toate testele trec.
Următorul pas: faza 2, adică serverul și harta, după `docs\superpowers\plans\2026-10-08-flota-rc-faza-2.md`.`
Orașul ales în Task 4 se trece la „Deschise", ca închis.

- [ ] **Pasul 10: Commit în AIS-OS, după ce Tudor spune da**

```bash
cd "/c/Users/40747/OneDrive/Documents/AIS-OS"
git add Facultate/memory/stare.md
git commit -m "Starea Facultate: flota-rc, fazele 0 si 1 gata

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
