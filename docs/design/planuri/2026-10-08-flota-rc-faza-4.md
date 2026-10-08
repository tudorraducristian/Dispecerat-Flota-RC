# flota-rc, faza 4: mașina rezistă — plan de implementare

> **Pentru agenți:** SUB-SKILL OBLIGATORIU: superpowers:subagent-driven-development
> (recomandat) sau superpowers:executing-plans, ca planul să fie executat task cu
> task. Pașii folosesc căsuțe (`- [ ]`) pentru urmărire.

**Ținta:** o mașină care intră în tunel nu pierde nimic din ce produce cât e
offline, se reconectează cu pauze care se dublează, iar serverul numără ce s-a
recuperat și ce s-a pierdut. Pe hartă, porțiunea din tunel apare punctată, iar
cea din gaura neagră lipsește.

**Arhitectura:**
- **În simulator:**
  - coada dintre motor și radio devine `Buffer`, un monitor cu `synchronized`,
    `wait` și `notifyAll`, limitat la 300 de mesaje, care aruncă întâi
    telemetria cea mai veche;
  - radioul își ia pauzele din `PauzaReconectare`.
- **În server:**
  - `Secventa` spune, din numărul de ordine, câte mesaje lipsesc;
  - `Masini` adună contoarele într-un singur `UPDATE`.
- **În UI:** `traseu.js` împarte traseul în bucăți live și recuperate, cu goluri
  acolo unde lipsesc numere de ordine.

**Stack:** cel din fazele 1–3, fără nimic nou: Java 21, JUnit 6, Spring Boot
4.1.1, Vue 3 cu Leaflet.

**Spec:** `docs\superpowers\specs\2026-10-08-flota-rc-design.md`:
- faza 4 din §8;
- §4.3, adică bufferul și reconectarea;
- §6.3, pasul 2 de la telemetrie, adică golurile;
- §7, adică traseul pe hartă;
- §10, adică testele.

Planul se sprijină pe planurile fazelor 0–3, din același dosar. Dacă la execuție
codul real diferă de ce presupune un task, se ajustează task-ul, nu codul
fazelor de dinainte.

## Constrângeri globale

- **Folderul proiectului:** `C:\Users\40747\OneDrive\Documents\Facultate\An 4\Semestrul 1\RC-Proiect\`.
- **Remote-ul:** `origin` e repo-ul privat `ProiectRC` de pe GitHub (D27). După
  commit-urile unui task urmează `git push`.
- **Java 21,** Maven doar prin `.\mvnw.cmd`. Nicio dependență nouă.
- **Bufferul** are implicit 300 de mesaje, citite din `buffer.marime` în
  `simulator.properties`. Aruncă doar telemetrie, niciodată alarme sau alte
  mesaje (spec §4.3).
- **Pauzele de reconectare:** min(30, 2ⁿ⁻¹) secunde după încercarea n, fiecare
  înmulțită cu un factor între 0,8 și 1,2. După o conectare reușită, numărarea
  o ia de la capăt (spec §4.3).
- **Contoarele serverului:**
  - `pierdute` crește cu `seq − ultimul_seq − 1` la un gol;
  - dacă `seq < ultimul_seq`, simulatorul a repornit, deci `ultimul_seq` se
    resetează fără pierderi;
  - `recuperate` crește cu 1 la fiecare mesaj cu `din_buffer` (spec §6.3).
- **Metoda mixtă (spec §9):**
  - **Claude (exemplu explicat)** scrie codul și îl explică, întâi analogia,
    apoi partea tehnică;
  - **Tudor (repetiție)** scrie codul după testul scris de Claude. Claude
    revizuiește fără să rescrie. Soluția de referință stă într-un bloc ascuns.
- **Commit-urile lui Claude** se termină cu
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Completări la spec, făcute la scrierea planului

Intră în spec ca D25, în același commit cu planul:

- **D25.** Trei detalii de reziliență, pe care spec §4.3 și §6.3 nu le hotărau:
  - **`ultimul_seq` gol înseamnă „nicio telemetrie văzută încă”.** Primul mesaj
    doar îl setează, fără pierderi, chiar dacă vine cu `seq` 500. Altfel, un
    server pornit prima oară după simulator ar număra 499 de mesaje „pierdute”,
    deși nu le-a așteptat nimeni.
  - **Dacă mașina se conectează, dar `online` nu pleacă,** mașina se
    deconectează și încearcă din nou, ca să nu rămână conectată fără să se fi
    anunțat.
  - **Până la faza 5, panoul mașinii citește contoarele la 2 secunde,** din
    `GET /api/masini/{id}`. În faza 5 vin live, pe `STATISTICI`.

## Unde trebuie să fie atent review-ul

Cinci situații pe care un om le întâlnește repede, cu comportamentul așteptat:

1. **Un tunel mai lung decât bufferul.** Se aruncă telemetria cea mai veche,
   iar alarmele rămân. Mașina spune câte a aruncat, iar serverul le numără ca
   pierdute. Testele sînt în Task 2: `plinAruncaTelemetriaCeaMaiVeche` și
   `alarmeleNuSeAruncaNiciodata`. Se verifică și în Task 9, pasul 6.
2. **Simulatorul e repornit.** `seq` o ia de la 1, iar `pierdute` nu crește.
   Testul e în Task 5: `repornireaNuNumaraPierderi`. Se verifică și în Task 9,
   pasul 7.
3. **Serverul pornește prima oară după simulator,** cu `ultimul_seq` gol.
   `pierdute` rămâne 0. Testul e în Task 5: `primulMesajNuNumaraPierderi`. Se
   verifică și în Task 9, pasul 8.
4. **Broker-ul sau proxy-ul repornește.** Cele zece mașini nu bat toate la ușă
   în aceeași secundă. Testul e în Task 3: `zeceMasiniNuBatDeodata`.
5. **Conectarea reușește, dar `online` nu pleacă.** Mașina se deconectează și
   reîncearcă, în loc să trimită date fără să se fi anunțat. Testul e în Task 4:
   `onlineEsuatInseamnaReconectare`.

---

## Structura fișierelor

```
RC-Proiect\
├── simulator\src\
│   ├── main\resources\simulator.properties          + buffer.marime
│   ├── main\java\ro\flota\simulator\
│   │   ├── config\Setari.java                       + marimeBuffer
│   │   └── masina\Buffer.java (nou), PauzaReconectare.java (nou),
│   │       Motor.java, Radio.java, Masina.java
│   └── test\java\ro\flota\simulator\
│       ├── config\SetariTest.java
│       └── masina\BufferTest.java (nou), PauzaReconectareTest.java (nou),
│           MotorTest.java, RadioTest.java, MasinaTest.java
├── server\src\
│   ├── main\java\ro\flota\server\flota\Secventa.java (nou), Masini.java, Dispecer.java
│   └── test\java\ro\flota\server\flota\SecventaTest.java (nou)
├── web\src\
│   ├── traseu.js (nou)                              bucățile traseului
│   ├── live.js                                      + actualizeazaContoare
│   └── components\HartaFlotei.vue, PanouMasina.vue
└── docs\scenariu-test.md                            măsurătorile
```

---

### Task 1: Mărimea bufferului în setări

**Cine scrie:** Tudor, ca repetiție a propriului `Setari` din faza 1. Claude
scrie testele.

**Fișiere:**
- Modifică: `simulator\src\main\resources\simulator.properties`
- Modifică: `simulator\src\main\java\ro\flota\simulator\config\Setari.java`
- Modifică: `simulator\src\test\java\ro\flota\simulator\config\SetariTest.java`
- Modifică: `simulator\src\test\java\ro\flota\simulator\masina\MasinaTest.java`

**Interfețe:**
- Produce:
  `record Setari(String brokerHost, int brokerPort, int keepaliveSecunde, long perioadaMs, int marimeBuffer)`,
  cu `marimeBuffer` citit din `buffer.marime`, între 10 și 100.000.

- [ ] **Pasul 1: Adaugă cheia în `simulator.properties`**

La sfârșitul fișierului:

```properties
# Câte mesaje ține mașina cât e offline: 300 = cinci minute de telemetrie (spec §4.3).
buffer.marime=300
```

- [ ] **Pasul 2: Schimbă testele**

În `SetariTest.java`:
- în `bune()`, înainte de `return p;`, adaugă
  `p.setProperty("buffer.marime", "300");`;
- în `citesteToateCheile`, așteptarea devine
  `new Setari("127.0.0.1", 1883, 5, 1000, 300)`;
- în `fisierulDinResurseTrecePrinProxy`, așteptarea devine
  `new Setari("127.0.0.1", 1884, 5, 1000, 300)`;
- la sfârșitul clasei, testul nou:

```java
    @Test
    void bufferulPreaMicEsteRefuzat() {
        Properties p = bune();
        p.setProperty("buffer.marime", "5");
        String mesaj = assertThrows(IllegalArgumentException.class, () -> Setari.din(p)).getMessage();
        assertTrue(mesaj.contains("buffer.marime"), mesaj);
    }
```

În `MasinaTest.java`, constanta devine
`private static final Setari RAPID = new Setari("127.0.0.1", 1883, 5, 100, 300);`.

- [ ] **Pasul 3: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`: constructorul lui `Setari` nu primește 5
argumente.

- [ ] **Pasul 4: Tudor adaugă `marimeBuffer`.** Indicii:
  - al cincilea câmp al record-ului, `int marimeBuffer`;
  - în `din`, se citește cu același ajutor ca restul, între 10 și 100.000.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

În `Setari.java`, antetul record-ului:

```java
public record Setari(String brokerHost, int brokerPort, int keepaliveSecunde, long perioadaMs, int marimeBuffer) {
```

iar `din`:

```java
    public static Setari din(Properties p) {
        return new Setari(
                obligatoriu(p, "broker.host"),
                intreg(p, "broker.port", 1, 65_535),
                intreg(p, "keepalive.secunde", 1, 3_600),
                intreg(p, "perioada.ms", 10, 60_000),
                intreg(p, "buffer.marime", 10, 100_000));
    }
```

</details>

- [ ] **Pasul 5: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`.

- [ ] **Pasul 6: Claude revizuiește,** fără să rescrie.

- [ ] **Pasul 7: Commit-ul lui Tudor**

```bash
git add simulator/src
git commit -m "Simulator: marimea bufferului in setari"
```

---

### Task 2: Bufferul

**Cine scrie:** Tudor, ca repetiție a tiparului 4, producător și consumator.
Primul exemplu a fost coada dintre motor și radio, din faza 1. Acum coada își
scrie singură regulile. Claude scrie testele.

**Fișiere:**
- Creează: `simulator\src\main\java\ro\flota\simulator\masina\Buffer.java`
- Test: `simulator\src\test\java\ro\flota\simulator\masina\BufferTest.java`

**Interfețe:**
- Consumă: `Mesaj`, `TipMesaj`, `Mesaje`, `Telemetrie`, `Alarma`, `TipAlarma`
  (faza 1, Task 7).
- Produce: `public final class Buffer`, cu:
  - constructorul `Buffer(int capacitate)`;
  - pentru producător: `void adauga(Mesaj)`. Dacă bufferul e plin, aruncă întâi
    cea mai veche telemetrie și crește `aruncate`. Dacă nu are ce telemetrie
    să arunce, crește peste capacitate;
  - pentru consumator: `Mesaj ia(long ms) throws InterruptedException`, care
    întoarce `null` dacă în `ms` milisecunde nu vine nimic, și
    `void puneInapoi(Mesaj)`, care pune mesajul primul la rând fără să arunce
    nimic;
  - pentru log și teste: `int marime()`, `long aruncate()` și
    `List<Mesaj> continut()`, o copie, în ordine.

- [ ] **Pasul 1: Scrie testele care pică**

`BufferTest.java`:

```java
package ro.flota.simulator.masina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.Json;
import ro.flota.simulator.mesaje.Alarma;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;
import ro.flota.simulator.mesaje.Telemetrie;
import ro.flota.simulator.mesaje.TipAlarma;

class BufferTest {

    private static Mesaj telemetrie(long seq) {
        return Mesaje.telemetrie("masina-1",
                new Telemetrie(seq, "2026-12-10T10:15:03.120Z", 46, 23, 50, 90, 100, true));
    }

    private static Mesaj alarma() {
        return Mesaje.alarma("masina-1", new Alarma(TipAlarma.SUPRAINCALZIRE, "2026-12-10T10:15:03.120Z", 111));
    }

    private static long seq(Mesaj m) {
        return Json.MAPPER.readTree(m.payload()).get("seq").asLong();
    }

    @Test
    void scoateInOrdineaInCareAuIntrat() throws InterruptedException {
        Buffer b = new Buffer(10);
        Mesaj t1 = telemetrie(1);
        Mesaj t2 = telemetrie(2);
        b.adauga(t1);
        b.adauga(t2);
        assertSame(t1, b.ia(100));
        assertSame(t2, b.ia(100));
        assertEquals(0, b.marime());
    }

    @Test
    void plinAruncaTelemetriaCeaMaiVeche() {
        Buffer b = new Buffer(3);
        Mesaj t1 = telemetrie(1);
        Mesaj t2 = telemetrie(2);
        Mesaj t3 = telemetrie(3);
        Mesaj t4 = telemetrie(4);
        b.adauga(t1);
        b.adauga(t2);
        b.adauga(t3);
        b.adauga(t4);
        assertEquals(List.of(t2, t3, t4), b.continut());
        assertEquals(1, b.aruncate());
    }

    @Test
    void alarmeleNuSeAruncaNiciodata() {
        Buffer b = new Buffer(3);
        Mesaj a = alarma();
        Mesaj t1 = telemetrie(1);
        Mesaj t2 = telemetrie(2);
        Mesaj t3 = telemetrie(3);
        b.adauga(a);
        b.adauga(t1);
        b.adauga(t2);
        b.adauga(t3);
        assertEquals(List.of(a, t2, t3), b.continut());
        assertEquals(1, b.aruncate());
    }

    @Test
    void plinDoarCuAlarmeCresteTotusi() {
        Buffer b = new Buffer(2);
        b.adauga(alarma());
        b.adauga(alarma());
        b.adauga(alarma());
        assertEquals(3, b.marime());
        assertEquals(0, b.aruncate());
    }

    @Test
    void puneInapoiEstePrimulSiNuAruncaNimic() throws InterruptedException {
        Buffer b = new Buffer(2);
        Mesaj t1 = telemetrie(1);
        Mesaj t2 = telemetrie(2);
        Mesaj t3 = telemetrie(3);
        b.adauga(t1);
        b.adauga(t2);
        assertSame(t1, b.ia(100));
        b.adauga(t3);
        b.puneInapoi(t1);
        assertEquals(List.of(t1, t2, t3), b.continut());
        assertEquals(0, b.aruncate());
    }

    @Test
    void iaAsteaptaCelMultCatISeSpune() throws InterruptedException {
        Buffer b = new Buffer(10);
        long inceput = System.nanoTime();
        assertNull(b.ia(150));
        long ms = (System.nanoTime() - inceput) / 1_000_000;
        assertTrue(ms >= 140 && ms < 1_000, "a așteptat " + ms + " ms");
    }

    @Test
    void iaSeTreziesteCandVineUnMesaj() throws InterruptedException {
        Buffer b = new Buffer(10);
        Mesaj t1 = telemetrie(1);
        Thread producator = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }
            b.adauga(t1);
        });
        producator.start();
        long inceput = System.nanoTime();
        assertSame(t1, b.ia(5_000));
        assertTrue((System.nanoTime() - inceput) / 1_000_000 < 2_000, "consumatorul n-a fost trezit");
        producator.join();
    }

    @Test
    void unProducatorSiUnConsumatorNuPierdNimic() throws InterruptedException {
        Buffer b = new Buffer(100_000);
        int n = 10_000;
        Thread producator = new Thread(() -> {
            for (int i = 1; i <= n; i++) {
                b.adauga(telemetrie(i));
            }
        });
        producator.start();
        List<Long> primite = new ArrayList<>();
        while (primite.size() < n) {
            Mesaj m = b.ia(2_000);
            if (m == null) {
                break;
            }
            primite.add(seq(m));
        }
        producator.join();
        assertEquals(n, primite.size());
        for (int i = 0; i < n; i++) {
            assertEquals(i + 1, primite.get(i));
        }
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Buffer`.

- [ ] **Pasul 3: Tudor scrie `Buffer`.** Indicii:
  - în interior, un `ArrayDeque<Mesaj>`, care nu e sigur pentru mai multe fire,
    deci toate metodele publice sînt `synchronized`;
  - `ia` așteaptă cu `wait(ramas)` într-o buclă `while (gol)`. Bucla e
    obligatorie: un fir se poate trezi și fără motiv, iar timpul rămas se
    recalculează la fiecare tură;
  - `adauga` și `puneInapoi` cheamă `notifyAll()` după ce pun mesajul;
  - aruncarea parcurge coada de la cap cu un `Iterator` și scoate prima
    telemetrie, cu `it.remove()`;
  - un `Buffer` cu capacitatea sub 1 aruncă `IllegalArgumentException`.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

```java
package ro.flota.simulator.masina;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.TipMesaj;

/**
 * Coada dintre motor și radio (spec §4.3): un monitor, adică o clasă în care un singur fir intră pe rând.
 * Cât mașina e conectată, e aproape goală. În tunel se umple; plină, aruncă întâi telemetria cea mai veche.
 * Alarmele și celelalte mesaje nu se aruncă niciodată.
 */
public final class Buffer {

    private final int capacitate;
    private final Deque<Mesaj> mesaje = new ArrayDeque<>();
    private long aruncate = 0;

    public Buffer(int capacitate) {
        if (capacitate < 1) {
            throw new IllegalArgumentException("capacitatea bufferului trebuie să fie cel puțin 1");
        }
        this.capacitate = capacitate;
    }

    /** Producătorul: pune la coadă. Dacă e plin, face loc aruncând cea mai veche telemetrie. */
    public synchronized void adauga(Mesaj mesaj) {
        if (mesaje.size() >= capacitate && aruncaCeaMaiVecheTelemetrie()) {
            aruncate++;
        }
        mesaje.addLast(mesaj);
        notifyAll();
    }

    /** Consumatorul: primul mesaj, așteptând cel mult ms milisecunde. null dacă nu vine nimic. */
    public synchronized Mesaj ia(long ms) throws InterruptedException {
        long limita = System.currentTimeMillis() + ms;
        while (mesaje.isEmpty()) {
            long ramas = limita - System.currentTimeMillis();
            if (ramas <= 0) {
                return null;
            }
            wait(ramas);
        }
        return mesaje.pollFirst();
    }

    /** Consumatorul: un mesaj care n-a putut pleca revine primul la rând. Nu aruncă nimic. */
    public synchronized void puneInapoi(Mesaj mesaj) {
        mesaje.addFirst(mesaj);
        notifyAll();
    }

    public synchronized int marime() {
        return mesaje.size();
    }

    public synchronized long aruncate() {
        return aruncate;
    }

    public synchronized List<Mesaj> continut() {
        return List.copyOf(mesaje);
    }

    private boolean aruncaCeaMaiVecheTelemetrie() {
        Iterator<Mesaj> it = mesaje.iterator();
        while (it.hasNext()) {
            if (it.next().tip() == TipMesaj.TELEMETRIE) {
                it.remove();
                return true;
            }
        }
        return false;
    }
}
```

</details>

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 8 teste din `BufferTest` trecute.

- [ ] **Pasul 5: Claude revizuiește `Buffer`,** fără să rescrie. Se uită în
  special la bucla din jurul lui `wait` și la `notifyAll`.

- [ ] **Pasul 6: Commit-urile**

```bash
git add simulator/src/test/java/ro/flota/simulator/masina/BufferTest.java
git commit -m "Simulator: testele bufferului

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git add simulator/src/main/java/ro/flota/simulator/masina/Buffer.java
git commit -m "Simulator: bufferul limitat, cu alarmele pastrate"
```

Primul commit nu compilează singur. Se fac unul după altul.

---

### Task 3: Pauza de reconectare

**Cine scrie:** Claude, ca exemplu explicat. E tiparul 5 din spec §9:
reconectarea cu pauză care se dublează.

**Fișiere:**
- Creează: `simulator\src\main\java\ro\flota\simulator\masina\PauzaReconectare.java`
- Test: `simulator\src\test\java\ro\flota\simulator\masina\PauzaReconectareTest.java`

**Interfețe:**
- Consumă: `Aleator` și `AleatorScriptat` (faza 1, Task 8).
- Produce: `PauzaReconectare(Aleator)`, cu:
  - `long dupaEsec()`: numără încă o încercare eșuată și întoarce pauza, în
    milisecunde;
  - `void reseteaza()`, chemată după o conectare reușită;
  - `int incercari()`: câte încercări au eșuat de la ultima conectare.

- [ ] **Pasul 1: Scrie testele care pică**

`PauzaReconectareTest.java`:

```java
package ro.flota.simulator.masina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.simulare.Aleator;
import ro.flota.simulator.testutil.AleatorScriptat;

class PauzaReconectareTest {

    private static final List<Long> SIR = List.of(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L);

    @Test
    void faraAbatereSirulEste1_2_4_8_16_30_30() {
        PauzaReconectare p = new PauzaReconectare(new AleatorScriptat(0.5)); // 0.5 → factorul 1
        assertEquals(SIR, IntStream.range(0, 7).mapToObj(i -> p.dupaEsec()).toList());
    }

    @Test
    void abatereaEsteCelMult20LaSuta() {
        assertEquals(800, new PauzaReconectare(new AleatorScriptat(0.0)).dupaEsec());
        assertEquals(1_200, new PauzaReconectare(new AleatorScriptat(0.99999)).dupaEsec());
    }

    @Test
    void cuIntamplareRealaFiecarePauzaStaInInterval() {
        for (int saminta = 0; saminta < 200; saminta++) {
            PauzaReconectare p = new PauzaReconectare(Aleator.din(new Random(saminta)));
            for (long baza : SIR) {
                long pauza = p.dupaEsec();
                assertTrue(pauza >= baza * 0.8 && pauza <= baza * 1.2, "pauza " + pauza + " pentru " + baza);
            }
        }
    }

    @Test
    void dupaConectareSeIaDeLaCapat() {
        PauzaReconectare p = new PauzaReconectare(new AleatorScriptat(0.5));
        p.dupaEsec();
        p.dupaEsec();
        p.dupaEsec();
        p.reseteaza();
        assertEquals(0, p.incercari());
        assertEquals(1_000, p.dupaEsec());
    }

    @Test
    void dupaMulteIncercariRamane30() {
        PauzaReconectare p = new PauzaReconectare(new AleatorScriptat(0.5));
        long ultima = 0;
        for (int i = 0; i < 100; i++) {
            ultima = p.dupaEsec();
        }
        assertEquals(30_000, ultima);
    }

    @Test
    void zeceMasiniNuBatDeodata() {
        Set<Long> primele = new HashSet<>();
        for (int masina = 1; masina <= 10; masina++) {
            primele.add(new PauzaReconectare(Aleator.din(new Random(masina))).dupaEsec());
        }
        assertTrue(primele.size() > 5, "pauzele au ieșit prea asemănătoare: " + primele);
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru
`PauzaReconectare`.

- [ ] **Pasul 3: Scrie `PauzaReconectare`**

```java
package ro.flota.simulator.masina;

import ro.flota.simulator.simulare.Aleator;

/**
 * Pauza dintre încercările de reconectare (spec §4.3): min(30, 2ⁿ⁻¹) secunde după încercarea n, adică
 * 1, 2, 4, 8, 16, 30, 30... Fiecare se înmulțește cu un factor între 0,8 și 1,2, ca mașinile ieșite
 * deodată din tunel să nu bată toate la ușă în aceeași secundă.
 */
public final class PauzaReconectare {

    static final long MAXIM_MS = 30_000;
    static final double ABATERE = 0.2;

    private final Aleator aleator;
    private int incercari = 0;

    public PauzaReconectare(Aleator aleator) {
        this.aleator = aleator;
    }

    /** Încă o încercare a eșuat. Întoarce cât să aștepte radioul până la următoarea, în milisecunde. */
    public long dupaEsec() {
        incercari++;
        long baza = incercari >= 6 ? MAXIM_MS : Math.min(MAXIM_MS, 1_000L << (incercari - 1));
        double factor = 1 - ABATERE + 2 * ABATERE * aleator.urmator();
        return Math.round(baza * factor);
    }

    /** După o conectare reușită, numărătoarea o ia de la capăt. */
    public void reseteaza() {
        incercari = 0;
    }

    public int incercari() {
        return incercari;
    }
}
```

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 6 teste din `PauzaReconectareTest` trecute.

- [ ] **Pasul 5: Claude explică.** Analogia: suni la un prieten care nu
  răspunde. Suni din nou după un minut, apoi după două, apoi după patru: nu-l
  bombardezi, dar nici nu renunți. Dacă zece oameni sună deodată după o pană de
  curent, fiecare așteaptă puțin diferit, ca să nu se blocheze centrala. Apoi:
  - de ce `incercari >= 6` se tratează separat: `1000 << 40` ar depăși un
    `long`;
  - de ce factorul vine din `Aleator`: testul îl fixează, ca la `Viteza`;
  - ce spune testul `zeceMasiniNuBatDeodata`.

- [ ] **Pasul 6: Commit**

```bash
git add simulator/src
git commit -m "Simulator: pauza de reconectare care se dubleaza

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Motorul, radioul și mașina pe buffer și pe pauza nouă

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „schimbi o piesă din
mijloc, iar testele vechi îți spun ce s-a stricat”.

**Fișiere:**
- Modifică, în `simulator\src\main\java\ro\flota\simulator\masina\`:
  `Motor.java`, `Radio.java` și `Masina.java`
- Modifică, în `simulator\src\test\java\ro\flota\simulator\masina\`:
  `MotorTest.java` și `RadioTest.java`

**Interfețe:**
- Consumă: `Buffer` (Task 2), `PauzaReconectare` (Task 3) și
  `Setari.marimeBuffer()` (Task 1).
- Produce:
  - `Motor(DefinitieMasina def, Viteza viteza, Temperatura temperatura, Clock ceas, Buffer coada, BooleanSupplier conectat, double secundePeTick)`;
  - `Radio(String id, Conexiune conexiune, Buffer coada, PauzaReconectare pauza, Somn somn)`;
  - constructorul lui `Masina` rămâne la fel.

- [ ] **Pasul 1: Schimbă `MotorTest`**

În `MotorTest.java`:
- scoate importurile `java.util.concurrent.BlockingDeque` și
  `java.util.concurrent.LinkedBlockingDeque`;
- câmpul devine `private final Buffer coada = new Buffer(300);`;
- înlocuiește peste tot:
  - `coada.size()` cu `coada.marime()`;
  - `coada.peekFirst()` cu `coada.continut().getFirst()`;
  - `coada.peekLast()` cu `coada.continut().getLast()`.

Numele câmpului rămâne `coada`, ca restul testelor să nu se schimbe.

- [ ] **Pasul 2: Rescrie `RadioTest`**

```java
package ro.flota.simulator.masina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.Json;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;
import ro.flota.simulator.mesaje.Telemetrie;
import ro.flota.simulator.mesaje.TipMesaj;
import ro.flota.simulator.testutil.AleatorScriptat;

class RadioTest {

    private final ConexiuneFalsa conexiune = new ConexiuneFalsa();
    private final Buffer coada = new Buffer(300);
    private final List<Long> somnuri = new ArrayList<>();

    /** Cu 0.5, factorul la întâmplare e exact 1, deci pauzele sînt 1, 2, 4... secunde. */
    private static PauzaReconectare pauzaFixa() {
        return new PauzaReconectare(new AleatorScriptat(0.5));
    }

    private final Radio radio = new Radio("masina-1", conexiune, coada, pauzaFixa(), somnuri::add);

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
        coada.adauga(telemetrie(1));
        coada.adauga(telemetrie(2));
        radio.pas();
        radio.pas();
        radio.pas();
        assertEquals(List.of(1L, 2L), seqTrimise());
        assertEquals(0, coada.marime());
    }

    @Test
    void brokerIndisponibilLaPornire() throws Exception {
        conexiune.esueazaLaConectare(3);
        coada.adauga(telemetrie(1));
        radio.pas();
        radio.pas();
        radio.pas();
        assertEquals(List.of(1_000L, 2_000L, 4_000L), somnuri);
        assertTrue(conexiune.publicate.isEmpty());
        radio.pas(); // a patra încercare reușește
        radio.pas();
        assertEquals(4, conexiune.conectari);
        assertEquals(List.of(1L), seqTrimise());
    }

    @Test
    void dupaConectarePauzaOIaDeLaCapat() throws Exception {
        conexiune.esueazaLaConectare(2);
        radio.pas();
        radio.pas();
        radio.pas(); // conectată la a treia
        conexiune.pierdeConexiunea();
        conexiune.esueazaLaConectare(1);
        radio.pas();
        assertEquals(List.of(1_000L, 2_000L, 1_000L), somnuri);
    }

    @Test
    void mesajulNuSePierdeCandPublicareaEsueaza() throws Exception {
        radio.pas(); // conectat
        Mesaj primul = telemetrie(1);
        coada.adauga(primul);
        coada.adauga(telemetrie(2));
        conexiune.esueazaLaPublicare(1);
        radio.pas();
        assertSame(primul, coada.continut().getFirst());
        assertEquals(List.of(1_000L), somnuri);
        conexiune.pierdeConexiunea();
        radio.pas(); // reconectare și „online”
        radio.pas();
        radio.pas();
        assertEquals(List.of(1L, 2L), seqTrimise());
    }

    @Test
    void onlineEsuatInseamnaReconectare() throws Exception {
        conexiune.esueazaLaPublicare(1);
        radio.pas();
        assertFalse(conexiune.conectata(), "fără „online”, mașina nu are voie să rămână conectată");
        radio.pas();
        assertEquals(2, conexiune.conectari);
        assertEquals("online", conexiune.publicate.get(0).text());
    }

    @Test
    void oprireaIeseDinBucla() throws Exception {
        Radio real = new Radio("masina-1", conexiune, coada, pauzaFixa(), Thread::sleep);
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

- [ ] **Pasul 3: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`: constructorii lui `Motor` și `Radio` nu
primesc un `Buffer`.

- [ ] **Pasul 4: Schimbă `Motor`**

În `Motor.java`:
- scoate importul `java.util.concurrent.BlockingDeque`;
- câmpul `private final BlockingDeque<Mesaj> coada;` devine
  `private final Buffer coada;`;
- în constructor, parametrul `BlockingDeque<Mesaj> coada` devine `Buffer coada`;
- cele trei `coada.offerLast(` devin `coada.adauga(`.

Javadoc-ul clasei primește o propoziție la sfârșit:
`Coada e un Buffer: cât radioul e offline, se umple, iar plină aruncă telemetria cea mai veche.`

- [ ] **Pasul 5: Rescrie `Radio`**

```java
package ro.flota.simulator.masina;

import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;

/**
 * Consumatorul (D9): ține conexiunea și golește bufferul, în ordine. Un mesaj care n-a putut pleca revine
 * primul la rând. Între încercările de reconectare, pauzele se dublează (spec §4.3).
 */
public final class Radio implements Runnable {

    static final long PAUZA_DUPA_EROARE_MS = 1_000;
    static final long ASTEPTARE_COADA_MS = 1_000;

    private final String id;
    private final Conexiune conexiune;
    private final Buffer coada;
    private final PauzaReconectare pauza;
    private final Somn somn;
    private volatile boolean oprit = false;

    public Radio(String id, Conexiune conexiune, Buffer coada, PauzaReconectare pauza, Somn somn) {
        this.id = id;
        this.conexiune = conexiune;
        this.coada = coada;
        this.pauza = pauza;
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

    /** Un pas: dacă nu e conectat, încearcă o dată; altfel trimite următorul mesaj din buffer. */
    void pas() throws InterruptedException {
        if (!conexiune.conectata()) {
            incearcaSaTeConectezi();
            return;
        }
        Mesaj mesaj = coada.ia(ASTEPTARE_COADA_MS);
        if (mesaj == null) {
            return;
        }
        try {
            conexiune.publica(mesaj);
        } catch (EroareConexiune e) {
            coada.puneInapoi(mesaj);
            System.out.printf("[%s] n-am putut trimite (%s); păstrez mesajul%n", id, e.getMessage());
            somn.dormi(PAUZA_DUPA_EROARE_MS);
        }
    }

    /** La reconectare: întâi „online”, apoi bufferul, în ordine; abia după aceea vin mesajele live. */
    private void incearcaSaTeConectezi() throws InterruptedException {
        try {
            conexiune.conecteaza();
            conexiune.publica(Mesaje.stare(id, true));
            System.out.printf("[%s] conectată; în buffer: %d mesaje, aruncate până acum: %d%n",
                    id, coada.marime(), coada.aruncate());
            pauza.reseteaza();
        } catch (EroareConexiune e) {
            if (conexiune.conectata()) {
                conexiune.deconecteaza(); // conectată, dar fără „online”: încearcă de la capăt (D25)
            }
            long ms = pauza.dupaEsec();
            if (pauza.incercari() == 1) {
                System.out.printf("[%s] nu mă pot conecta (%s); reîncerc cu pauze tot mai lungi%n", id, e.getMessage());
            }
            somn.dormi(ms);
        }
    }
}
```

- [ ] **Pasul 6: Rescrie `Masina`**

```java
package ro.flota.simulator.masina;

import java.time.Clock;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import ro.flota.simulator.config.DefinitieMasina;
import ro.flota.simulator.config.Setari;
import ro.flota.simulator.mesaje.Mesaje;
import ro.flota.simulator.simulare.Aleator;
import ro.flota.simulator.simulare.Temperatura;
import ro.flota.simulator.simulare.Viteza;

/** O mașină: motorul pe firul lui, radioul pe al lui, iar între ele bufferul (D9). */
public final class Masina {

    private final String id;
    private final Conexiune conexiune;
    private final Buffer coada;
    private final Motor motor;
    private final Radio radio;
    private final long perioadaMs;
    private ScheduledExecutorService firMotor;
    private Thread firRadio;

    public Masina(DefinitieMasina def, Setari setari, Conexiune conexiune, Clock ceas, Random random) {
        this.id = def.id();
        this.conexiune = conexiune;
        this.perioadaMs = setari.perioadaMs();
        this.coada = new Buffer(setari.marimeBuffer());
        Viteza viteza = new Viteza(Aleator.din(random), Aleator.din(random), 0);
        Temperatura temperatura = new Temperatura(Aleator.din(random), Aleator.din(random), Temperatura.DE_BAZA);
        this.motor = new Motor(def, viteza, temperatura, ceas, coada, conexiune::conectata, perioadaMs / 1000.0);
        this.radio = new Radio(id, conexiune, coada, new PauzaReconectare(Aleator.din(random)), Thread::sleep);
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

    /** Oprirea curată: motorul, apoi radioul, apoi mașina își anunță singură „offline” și se deconectează. */
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

`java.util.Random` e sigur pentru mai multe fire, așa că motorul și radioul pot
folosi același `random`.

- [ ] **Pasul 7: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu `MotorTest`, `RadioTest` (7 teste) și
`MasinaTest` trecute.

- [ ] **Pasul 8: Verifică pe viu**

A, P, S, W și C pornite, după README. În O: `TUNEL masina-3 20`. Se așteaptă
în C:
- `[masina-3] conexiune pierdută`;
- `[masina-3] nu mă pot conecta (…); reîncerc cu pauze tot mai lungi`;
- după ieșirea din tunel, `[masina-3] conectată; în buffer: N mesaje,
  aruncate până acum: 0`, cu N în jur de 20–30.

- [ ] **Pasul 9: Claude explică.** Analogia: bucătarul și chelnerul au acum o
  tejghea cu loc pentru 300 de farfurii. Când e plină, bucătarul aruncă cea mai
  veche salată, dar niciodată un tort comandat, adică o alarmă. Chelnerul care
  nu găsește clientul încearcă din nou după tot mai mult timp. Apoi:
  - de ce testele vechi n-au trebuit rescrise, ci doar adaptate: au păstrat
    comportamentul;
  - de ce o conectare fără `online` se desface (D25).

- [ ] **Pasul 10: Commit**

```bash
git add simulator/src
git commit -m "Simulator: masina pe buffer si pe pauza care se dubleaza

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Golurile din numerele de ordine

**Cine scrie:** Tudor, ca repetiție a detectorilor din faza 1: o regulă mică,
care compară valoarea de acum cu cea de dinainte. Claude scrie testele.

**Fișiere:**
- Creează: `server\src\main\java\ro\flota\server\flota\Secventa.java`
- Test: `server\src\test\java\ro\flota\server\flota\SecventaTest.java`

**Interfețe:**
- Produce: `record Secventa(long pierdute, boolean repornire)`, cu
  `static Secventa dupa(Long ultimulSeq, long seq)`. `ultimulSeq` e `null` dacă
  mașina n-a trimis încă telemetrie (D25).

- [ ] **Pasul 1: Scrie testele care pică**

`SecventaTest.java`:

```java
package ro.flota.server.flota;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SecventaTest {

    @Test
    void primulMesajNuNumaraPierderi() {
        assertEquals(new Secventa(0, false), Secventa.dupa(null, 500));
    }

    @Test
    void urmatorulNuPierdeNimic() {
        assertEquals(new Secventa(0, false), Secventa.dupa(41L, 42));
    }

    @Test
    void golulNumaraCeLipseste() {
        assertEquals(new Secventa(3, false), Secventa.dupa(41L, 45));
    }

    @Test
    void repornireaNuNumaraPierderi() {
        assertEquals(new Secventa(0, true), Secventa.dupa(5_000L, 1));
    }

    @Test
    void acelasiNumarDeDouaOriNuNumaraNimic() {
        assertEquals(new Secventa(0, false), Secventa.dupa(42L, 42));
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Secventa`.

- [ ] **Pasul 3: Tudor scrie `Secventa`.** Indicii:
  - e un `record` cu metoda statică `dupa`;
  - cazurile, în ordine:
    1. nimic văzut încă;
    2. număr mai mic, adică simulatorul a repornit;
    3. gol;
    4. orice altceva;
  - golul dintre 41 și 45 înseamnă 3 mesaje pierdute: 42, 43 și 44.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

```java
package ro.flota.server.flota;

/** Ce spune un număr de ordine nou despre mesajele dintre el și cel de dinainte (spec §6.3, pasul 2). */
public record Secventa(long pierdute, boolean repornire) {

    public static Secventa dupa(Long ultimulSeq, long seq) {
        if (ultimulSeq == null) {
            return new Secventa(0, false); // primul mesaj văzut (D25)
        }
        if (seq < ultimulSeq) {
            return new Secventa(0, true); // simulatorul a repornit și numără iar de la 1
        }
        if (seq > ultimulSeq + 1) {
            return new Secventa(seq - ultimulSeq - 1, false);
        }
        return new Secventa(0, false); // următorul, sau același încă o dată
    }
}
```

</details>

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 5 teste din `SecventaTest` trecute.

- [ ] **Pasul 5: Claude revizuiește `Secventa`,** fără să rescrie.

- [ ] **Pasul 6: Commit-urile**

```bash
git add server/src/test/java/ro/flota/server/flota/SecventaTest.java
git commit -m "Server: testele pentru golurile din seq

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git add server/src/main/java/ro/flota/server/flota/Secventa.java
git commit -m "Server: golurile din numerele de ordine"
```

---

### Task 6: Contoarele în server

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „citești, hotărăști,
scrii, pe un singur fir, iar scrierea e un singur `UPDATE`”.

**Fișiere:**
- Modifică: `server\src\main\java\ro\flota\server\flota\Masini.java`
- Modifică: `server\src\main\java\ro\flota\server\flota\Dispecer.java`

**Interfețe:**
- Consumă: `Secventa` (Task 5).
- Produce, în `Masini`:
  - `Long ultimulSeq(String id)`;
  - `void dupaTelemetrie(String id, long seq, long pierdute, boolean recuperata, Instant ora)`,
    care înlocuiește `vazuta`. Metoda `vazuta` dispare.

- [ ] **Pasul 1: Schimbă `Masini`**

Metoda `vazuta` se șterge. În locul ei:

```java
    /** Ultimul număr de ordine văzut, sau null dacă mașina n-a trimis încă telemetrie (D25). */
    public Long ultimulSeq(String id) {
        List<Long> rezultat = jdbc.sql("SELECT ultimul_seq FROM masini WHERE id = :id")
                .param("id", id)
                .query((rs, nr) -> rs.getObject("ultimul_seq", Long.class))
                .list();
        return rezultat.isEmpty() ? null : rezultat.get(0);
    }

    /** Contoarele după o telemetrie, într-un singur UPDATE (spec §6.3). */
    public void dupaTelemetrie(String id, long seq, long pierdute, boolean recuperata, Instant ora) {
        jdbc.sql("""
                        UPDATE masini SET ultimul_seq = :seq, pierdute = pierdute + :pierdute,
                                          recuperate = recuperate + :recuperata, ultima_vedere = :ora
                        WHERE id = :id""")
                .param("seq", seq)
                .param("pierdute", pierdute)
                .param("recuperata", recuperata ? 1 : 0)
                .param("ora", Ore.ts(ora))
                .param("id", id)
                .update();
    }
```

- [ ] **Pasul 2: Schimbă `Dispecer`**

Importuri noi: `org.slf4j.Logger` și `org.slf4j.LoggerFactory`. Câmpul nou, primul
din clasă:

```java
    private static final Logger log = LoggerFactory.getLogger(Dispecer.class);
```

`laTelemetrie` devine:

```java
    /**
     * Citește ultimul seq, hotărăște ce lipsește și scrie contoarele. Între citire și scriere nu se poate
     * strecura alt mesaj al aceleiași mașini: toate trec pe firul unic al lui ClientMqtt (D22).
     */
    private void laTelemetrie(MesajPrimit.Telemetrie t) {
        Instant sosire = ceas.instant();
        PozitieDto pozitie = PozitieDto.din(t, sosire);
        pozitii.salveaza(pozitie);
        Long ultimul = masini.ultimulSeq(t.masinaId());
        Secventa secventa = Secventa.dupa(ultimul, t.seq());
        if (secventa.repornire()) {
            log.info("{}: simulatorul a repornit (seq {} după {})", t.masinaId(), t.seq(), ultimul);
        } else if (secventa.pierdute() > 0) {
            log.info("{}: lipsesc {} mesaje înainte de seq {}", t.masinaId(), secventa.pierdute(), t.seq());
        }
        masini.dupaTelemetrie(t.masinaId(), t.seq(), secventa.pierdute(), t.dinBuffer(), sosire);
        live.trimite("POZITIE", pozitie);
    }
```

- [ ] **Pasul 3: Rulează testele**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`. Dacă ceva încă cheamă `masini.vazuta`, compilarea
spune unde.

- [ ] **Pasul 4: Verifică contoarele pe viu**

Totul pornit după README. În D, cu `Sql` definit ca în `docs\scenariu-test.md`:

```powershell
Sql "UPDATE masini SET pierdute = 0, recuperate = 0"
```

În O: `TUNEL masina-3 15`. După ce `masina-3` revine verde:

```powershell
Sql "SELECT id, recuperate, pierdute FROM masini WHERE id IN ('masina-3', 'masina-5')"
```

Se așteaptă:
- pentru `masina-3`, `recuperate` în jur de 20 și `pierdute` 0 sau 1;
- pentru `masina-5`, 0 și 0.

În O: `GAURA masina-5 10`. După revenire, pentru `masina-5`, `pierdute` e peste 0,
iar `recuperate` crește. În S apare `masina-5: lipsesc N mesaje înainte de seq …`.

- [ ] **Pasul 5: Claude explică.** Analogia: contabilul numără bonurile după
  numărul lor. Dacă după bonul 41 vine 45, lipsesc trei. Dacă vine 1, s-a
  schimbat casa de marcat, nu s-au pierdut 40 de bonuri. Apoi:
  - de ce citirea și scrierea nu au nevoie de tranzacție aici: firul unic din
    D22;
  - `pierdute = pierdute + :pierdute`: adunarea o face MySQL, nu Java.

- [ ] **Pasul 6: Commit**

```bash
git add server/src
git commit -m "Server: contoarele de mesaje recuperate si pierdute

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Traseul cu linie punctată și goluri

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „datele se pregătesc
într-o funcție simplă, iar componenta doar le desenează”.

**Fișiere:**
- Creează: `web\src\traseu.js`
- Modifică: `web\src\components\HartaFlotei.vue`

**Interfețe:**
- Consumă: pozițiile, cu `seq`, `dinBuffer`, `lat` și `lon`, venite din REST și
  de pe canalul live (faza 2).
- Produce: `segmente(pozitii) : Array<{ fel: 'live' | 'recuperat', puncte: Array<[lat, lon]> }>`.

- [ ] **Pasul 1: Scrie `web\src\traseu.js`**

```js
/**
 * Împarte traseul unei mașini în bucăți de desenat (spec §7):
 * - „live”: mesaje venite pe loc, desenate cu linie plină;
 * - „recuperat”: mesaje cu dinBuffer, venite după o întrerupere, desenate cu linie punctată;
 * - între două poziții cu numere de ordine care nu se urmează nu se trage nimic: acolo lipsesc mesaje,
 *   sau simulatorul a repornit.
 * O bucată leagă două poziții vecine, iar felul ei îl dă a doua poziție.
 */
export function segmente(pozitii) {
  const rezultat = []
  let curent = null
  for (let i = 1; i < pozitii.length; i++) {
    const a = pozitii[i - 1]
    const b = pozitii[i]
    if (b.seq !== a.seq + 1) {
      curent = null // gol
      continue
    }
    const fel = b.dinBuffer ? 'recuperat' : 'live'
    if (!curent || curent.fel !== fel) {
      curent = { fel, puncte: [[a.lat, a.lon]] }
      rezultat.push(curent)
    }
    curent.puncte.push([b.lat, b.lon])
  }
  return rezultat
}
```

- [ ] **Pasul 2: Schimbă `deseneazaTraseu` în `HartaFlotei.vue`**

Importul nou, lângă cel al lui `live.js`:

```js
import { segmente } from '../traseu.js'
```

Metoda devine:

```js
    /** Linie plină pentru live, punctată pentru ce s-a recuperat din buffer, nimic unde lipsesc mesaje. */
    deseneazaTraseu() {
      this.stratTraseu.clearLayers()
      for (const s of segmente(this.traseu)) {
        const stil = { color: '#1565c0', weight: 4, opacity: 0.8 }
        if (s.fel === 'recuperat') stil.dashArray = '2 8'
        L.polyline(s.puncte, stil).addTo(this.stratTraseu)
      }
    },
```

- [ ] **Pasul 3: Verifică pe hartă**

Totul pornit. Click pe `masina-3`, ca traseul ei să fie desenat. În O:
`TUNEL masina-3 15`. După revenire se așteaptă:
- o porțiune punctată, cât a ținut tunelul, legată de linia plină de dinainte
  și de cea de după;
- cel mult o mică întrerupere la intrarea în tunel, adică mesajul care era pe
  drum.

Click pe `masina-5`, apoi `GAURA masina-5 10`. După revenire se așteaptă:
- o porțiune fără linie, unde s-au pierdut primele secunde ale găurii;
- după ea, o porțiune punctată, apoi din nou plină.

- [ ] **Pasul 4: Claude explică.** Analogia: un fir cu mărgele numerotate.
  Unde lipsește o mărgea, firul se rupe. Mărgelele venite mai târziu, din buffer,
  sînt de altă culoare. Apoi:
  - de ce felul bucății îl dă a doua poziție: bucata dintre ultima poziție live
    și prima recuperată aparține deja întreruperii;
  - de ce funcția e separată de componentă: n-are nevoie de Leaflet ca să fie
    înțeleasă.

- [ ] **Pasul 5: Commit**

```bash
git add web/src/traseu.js web/src/components/HartaFlotei.vue
git commit -m "UI: traseul cu linie punctata pentru recuperat si goluri pentru pierdut

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Contoarele și legenda în panoul mașinii

**Cine scrie:** Tudor, ca repetiție a panoului din faza 2, plus un cronometru
care citește periodic din REST.

**Fișiere:**
- Modifică: `web\src\live.js`
- Modifică: `web\src\components\PanouMasina.vue`

**Interfețe:**
- Consumă: `api.masina(id)` (faza 2) și `flota` (faza 2).
- Produce, în `live.js`: `actualizeazaContoare(id) : Promise`, care pune
  `recuperate` și `pierdute` din REST pe tabla `flota.masini[id]`. Faza 5 o
  înlocuiește cu `STATISTICI`.

- [ ] **Pasul 1: Tudor scrie partea lui.** Ce trebuie:
  - **în `live.js`,** funcția exportată `actualizeazaContoare(id)`: cere
    `api.masina(id)` și copiază cele două contoare pe tabla mașinii, dacă
    mașina există pe tablă;
  - **în `PanouMasina.vue`:**
    - două rânduri noi în tabel, „Recuperate” și „Pierdute”, din
      `masina.recuperate` și `masina.pierdute`;
    - sub tabel, o legendă scurtă: „Linie plină: live · punctată: recuperat din
      buffer · lipsă: pierdut”;
    - cât e deschis panoul, contoarele se cer la 2 secunde. Cronometrul
      pornește când se schimbă mașina și se oprește în `beforeUnmount`. Un
      `computed` `idMasina`, urmărit cu `watch` și `immediate: true`, e cel mai
      simplu. O eroare de rețea se ignoră.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

În `live.js`, după `incarcaMasini`:

```js
/** Contoarele unei mașini, din REST. Până la faza 5, panoul le cere la 2 secunde (D25). */
export async function actualizeazaContoare(id) {
  const m = await api.masina(id)
  const tabla = flota.masini[id]
  if (tabla) {
    tabla.recuperate = m.recuperate
    tabla.pierdute = m.pierdute
  }
}
```

În `PanouMasina.vue`, rândurile noi din `<tbody>`, după baterie:

```vue
          <tr><td>Recuperate</td><td>{{ masina.recuperate }}</td></tr>
          <tr><td>Pierdute</td><td>{{ masina.pierdute }}</td></tr>
```

după `</v-table>`:

```vue
      <div class="text-caption mt-2">Linie plină: live · punctată: recuperat din buffer · lipsă: pierdut</div>
```

iar scriptul devine:

```js
import { actualizeazaContoare } from '../live.js'

export default {
  name: 'PanouMasina',
  props: {
    masina: { type: Object, default: null },
  },
  emits: ['inchide'],
  computed: {
    idMasina() {
      return this.masina ? this.masina.id : null
    },
  },
  watch: {
    idMasina: {
      immediate: true,
      handler(id) {
        clearInterval(this.cronometru)
        if (id) {
          this.cronometru = setInterval(() => actualizeazaContoare(id).catch(() => {}), 2000)
        }
      },
    },
  },
  beforeUnmount() {
    clearInterval(this.cronometru)
  },
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
```

</details>

- [ ] **Pasul 2: Verifică**

Click pe `masina-3`, apoi `TUNEL masina-3 15`. După revenire, în cel mult 2
secunde, „Recuperate” crește în panou.

- [ ] **Pasul 3: Claude revizuiește,** fără să rescrie. Se uită în special la
  oprirea cronometrului: un cronometru uitat ar cere date pentru o mașină care
  nu mai e afișată.

- [ ] **Pasul 4: Commit-ul lui Tudor**

```bash
git add web/src/live.js web/src/components/PanouMasina.vue
git commit -m "UI: contoarele si legenda in panoul masinii"
```

---

### Task 9: Măsurătorile și verificarea fazei 4

**Cine face:** Tudor rulează și cronometrează. Claude citește ieșirile, trece
măsurătorile în scenariu și actualizează starea.

**Fișiere:**
- Modifică: `docs\scenariu-test.md`, tabelul „Măsurători”
- Modifică, în AIS-OS: `Facultate\memory\stare.md`

**Interfețe:** nu are.

- [ ] **Pasul 1: Toate testele**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu testele celor trei module trecute.

- [ ] **Pasul 2: Scenariul, pașii 1–4 și 6, cu așteptările „Din faza 4”**

Înainte, în D: `Sql "UPDATE masini SET pierdute = 0, recuperate = 0"`. Apoi
pașii din `docs\scenariu-test.md`. Claude citește interogările după fiecare pas.

- [ ] **Pasul 3: Cronometrează gaura (riscul R6)**

`GAURA masina-5 10`, cu un cronometru pornit în clipa în care scriptul răspunde
`OK`. Se notează:
- după câte secunde devine gri `masina-5` (spec: în jur de 7,5);
- `pierdute` și `recuperate` adăugate de pas, din interogarea contoarelor.

Se repetă de 3 ori, iar în tabel intră valoarea din mijloc.

- [ ] **Pasul 4: Cronometrează tunelul**

`TUNEL masina-3 15`: după câte secunde e din nou verde `masina-3`. Se repetă
de 3 ori. Acum pauzele cresc, deci revenirea poate fi mai târzie decât în faza 3.

- [ ] **Pasul 5: Găsește cea mai lungă gaură fără pierderi**

Pe rând, `GAURA masina-5 6`, `7` și `8`, cu `Sql "UPDATE masini SET pierdute = 0, recuperate = 0"`
înainte de fiecare. Cea mai lungă după care `pierdute` rămâne 0 intră în tabel.

- [ ] **Pasul 6: Un tunel mai lung decât bufferul (review, punctul 1)**

1. În `simulator.properties`, pune temporar `buffer.marime=20` și repornește C.
2. În D, rulează `UPDATE`-ul care pune contoarele la 0.
3. În O: `TUNEL masina-3 60`.

După revenire se așteaptă:
- în C, `[masina-3] conectată; în buffer: 20 mesaje, aruncate până acum: N`, cu
  N în jur de 45;
- în D, pentru `masina-3`, `recuperate` 20 și `pierdute` în jur de N.

Pune la loc `buffer.marime=300`, fără commit, și repornește C.

- [ ] **Pasul 7: Simulatorul repornit (review, punctul 2)**

Notează `pierdute` pentru toate mașinile. Ctrl+C în C, apoi pornește-l la loc.
Se așteaptă:
- în S, câte un rând `masina-N: simulatorul a repornit (seq 1 după …)`;
- `pierdute` nu crește din cauza repornirii.

- [ ] **Pasul 8: Serverul pornit prima oară după simulator (review, punctul 3)**

Oprește S. În D:

```powershell
Sql "UPDATE masini SET ultimul_seq = NULL, pierdute = 0, recuperate = 0"
```

Cu C pornit, pornește S și așteaptă 30 de secunde. Se așteaptă: `pierdute`
rămâne 0 la toate mașinile.

- [ ] **Pasul 9: Trece măsurătorile în `docs\scenariu-test.md`**

Claude completează tabelul „Măsurători” cu valorile din pașii 3–5 și cu data
zilei.

- [ ] **Pasul 10: Commit în proiect**

```bash
git add docs/scenariu-test.md
git commit -m "Scenariul de test: masuratorile din faza 4

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Pasul 11: Actualizează starea în AIS-OS**

În `Facultate\memory\stare.md`, la blocul de RC, rândul „Unde am rămas” devine,
cu data zilei în care se termină pasul 8, în formatul AAAA-LL-ZZ:

```markdown
- **Unde am rămas:** faza 4 e gata pe AAAA-LL-ZZ: mașinile țin bufferul în tunel,
  se reconectează cu pauze care se dublează, iar harta arată recuperatul punctat
  și pierdutul lipsă. Măsurătorile găurii sînt în `docs\scenariu-test.md`.
  Următorul pas: faza 5, după
  `docs\superpowers\plans\2026-10-08-flota-rc-faza-5.md`.
```

- [ ] **Pasul 12: Commit în AIS-OS, după ce Tudor spune da**

```bash
cd "/c/Users/40747/OneDrive/Documents/AIS-OS"
git add Facultate/memory/stare.md
git commit -m "Starea Facultate: flota-rc, faza 4 gata

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
