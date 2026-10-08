# flota-rc, faza 3: proxy-ul de rețea proastă — plan de implementare

> **Pentru agenți:** SUB-SKILL OBLIGATORIU: superpowers:subagent-driven-development
> (recomandat) sau superpowers:executing-plans, ca planul să fie executat task cu
> task. Pașii folosesc căsuțe (`- [ ]`) pentru urmărire.

**Ținta:** mașinile nu mai vorbesc direct cu Mosquitto, ci printr-un proxy scris
de noi. Proxy-ul strică rețeaua la comandă, cu întârziere, bandă îngustă, tunel
și gaură neagră, și primește ordinele printr-un protocol text pe portul 1885. La
sfârșit, `TUNEL masina-3 15` face mașina gri pe hartă.

**Arhitectura:**
- **Modulul `proxy`** e Java simplu, cu socket-uri ca la SCD Lab 1, și n-are
  nicio dependență în afară de JUnit.
- **Conexiunile de la mașini:**
  - `AscultatorMasini` primește fiecare mașină pe 1884;
  - `CititorConnect` citește pachetul `CONNECT` ca să afle cine e;
  - `Legatura` o leagă de Mosquitto prin două `Sens`-uri. Fiecare sens are un
    fir care citește și unul care scrie la ora calculată de `Planificator`.
- **Vremea** fiecărei mașini stă în `Vreme`, iar mașinile văzute stau în
  `Registru`.
- **Ordinele** vin prin `ServerOrdine`, care citește linii cu `CititorLinii` și
  le execută cu `Ordine`.

**Stack:** Java 21, Maven prin `mvnw`, JUnit 6.1.3, Mosquitto 2.1.2. Pentru
ordine se folosește un script PowerShell 5.1.

**Spec:** `docs\superpowers\specs\2026-10-08-flota-rc-design.md`: faza 3 din §8,
proxy-ul din §5 și testele proxy-ului din §10. Planul se sprijină pe planurile
fazelor 0–1 și 2, din același dosar. Dacă la execuție codul real diferă de ce
presupune un task, se ajustează task-ul, nu codul fazelor de dinainte.

## Constrângeri globale

- **Folderul proiectului:** `C:\Users\40747\OneDrive\Documents\Facultate\An 4\Semestrul 1\RC-Proiect\`.
  Comenzile pornesc de aici.
- **Remote-ul:** `origin` e repo-ul privat `ProiectRC` de pe GitHub (D27). După
  commit-urile unui task urmează `git push`.
- **Java 21,** Maven doar prin `.\mvnw.cmd`. Modulul `proxy` are ca părinte
  agregatorul `ro.flota:flota-rc`, deci folosește JUnit `6.1.3` și pluginurile
  fixate acolo.
- **Proxy-ul n-are dependențe** în afară de JUnit (spec §3).
- **Pachetele** încep cu `ro.flota.proxy`. Numele din cod sînt în română, fără
  diacritice. Mesajele din consolă și comentariile au diacritice. Protocolul
  text e ASCII, deci răspunsurile lui n-au diacritice: `ERR masina necunoscuta`.
- **Porturile,** toate doar pe `127.0.0.1`:
  - 1884 pentru mașini;
  - 1885 pentru ordine;
  - Mosquitto rămâne pe 1883.

  Serverul rămâne legat direct de Mosquitto (D11).
- **Proxy-ul nu pierde octeți (D5):** nu schimbă ordinea și nu modifică
  pachetele. Singurul pachet citit înăuntru e primul `CONNECT` (spec §5.2).
- **Intervalele din protocol:** întârzierea și variația între 0 și 10.000 ms,
  banda între 1 și 100.000 kbps, durata între 1 și 600 de secunde.
- **Metoda mixtă (spec §9).** Fiecare task spune cine scrie codul:
  - **Claude (exemplu explicat):** Claude scrie codul, apoi i-l explică lui
    Tudor, întâi analogia și de ce contează, apoi partea tehnică.
  - **Tudor (repetiție):** Tudor scrie codul după test. Claude revizuiește fără
    să rescrie. Soluția de referință stă într-un bloc ascuns, pe care Tudor nu-l
    deschide înainte să încerce.
- **Commit-urile lui Claude** se termină cu
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Scripturile PowerShell** se rulează cu
  `powershell -NoProfile -ExecutionPolicy Bypass -File …`, ca politica de
  execuție a sistemului să rămână neatinsă.

## Completări la spec, făcute la scrierea planului

Intră în spec ca D24, în același commit cu planul:

- **D24.** Trei detalii ale protocolului text și ale vremii, pe care spec §5 nu
  le hotăra:
  - **Linia prea lungă.** Peste 200 de caractere, linia primește
    `ERR linie prea lunga`, iar restul ei se aruncă.
  - **Linia goală** se ignoră, fără răspuns, ca un Enter apăsat din greșeală să
    nu strice nimic.
  - **Un tunel și o gaură nu stau unul peste altul:** ultimul dat îl înlocuiește
    pe celălalt.
  - **În răspunsul la `STARE`, întârzierea apare mereu cu variația,** chiar când
    variația e 0: `INTARZIERE 500 0`.
  - **Ordinele se dau din `unelte\ordine.ps1`,** un client mic în PowerShell. E
    și demonstrația că un protocol text se poate folosi de mână, ca SMTP prin
    telnet.

## Unde trebuie să fie atent review-ul

Cinci situații pe care un om le întâlnește repede, cu comportamentul așteptat:

1. **Pe 1884 se conectează ceva care nu e MQTT,** de exemplu un browser deschis
   pe `http://127.0.0.1:1884`. Proxy-ul scrie motivul, închide conexiunea și
   primește mai departe mașini. Testul e în Task 1:
   `unBrowserNuEsteMqtt`. Se verifică și în Task 9, pasul 7.
2. **Mosquitto e oprit când o mașină trece prin proxy.** Proxy-ul închide mașina
   imediat, cu „broker-ul nu răspunde”, în loc să o țină agățată. Mașina
   reîncearcă singură. Se verifică în Task 9, pasul 8.
3. **`NORMAL` vine în timpul unui tunel sau al unei găuri.** Tunelul sau gaura se
   termină pe loc. O gaură dată peste un tunel îl înlocuiește. Testele sînt în
   Task 2: `normalOpresteTotul` și `gauraDupaTunelInlocuiesteTunelul`.
4. **Pe 1885 vine o linie uriașă, o linie goală sau `\r\n`.** Linia uriașă
   primește `ERR linie prea lunga`, iar conexiunea rămâne deschisă. Linia goală
   se ignoră, iar `\r` nu strică nimic. Testele sînt în Task 6, în
   `CititorLiniiTest`.
5. **Se dă `TOATE` sau `STARE` înainte să se fi conectat vreo mașină.** Primul
   răspunde `OK` și nu face nimic, iar al doilea dă doar rândul cu `.`. Testele
   sînt în Task 6: `toateFaraMasiniEsteOk` și `stareaFaraMasiniEsteDoarPunctul`.

---

## Structura fișierelor

```
RC-Proiect\
├── pom.xml                                  agregatorul: se adaugă modulul proxy
├── unelte\ordine.ps1                        clientul de ordine, în PowerShell
├── docs\scenariu-test.md                    scenariul de la cap la coadă (spec §10)
├── simulator\src\main\resources\simulator.properties   portul devine 1884
└── proxy\
    ├── pom.xml
    └── src\
        ├── main\java\ro\flota\proxy\
        │   ├── Main.java                    pornește cele două porturi
        │   ├── connect\                     PachetStricat, CopieStream, PachetConnect, CititorConnect
        │   ├── vreme\                       Vreme, Registru
        │   ├── transport\                   Bucata, Planificator, Sens, Legatura, AscultatorMasini
        │   └── ordine\                      Linie, CititorLinii, Ordine, ServerOrdine
        └── test\java\ro\flota\proxy\
            ├── testutil\CeasManual.java
            ├── connect\Pachete.java, CititorConnectTest.java
            ├── vreme\VremeTest.java, RegistruTest.java
            ├── transport\PlanificatorTest.java, SensTest.java, LegaturaTest.java
            └── ordine\CititorLiniiTest.java, OrdineTest.java
```

---

### Task 1: Modulul `proxy` și citirea pachetului `CONNECT`

**Cine scrie:** Claude, ca exemplu explicat. E tiparul 2 din spec §9: parsarea
unui pachet binar.

**Fișiere:**
- Modifică: `pom.xml`, agregatorul
- Creează: `proxy\pom.xml`
- Creează, în `proxy\src\main\java\ro\flota\proxy\connect\`:
  `PachetStricat.java`, `CopieStream.java`, `PachetConnect.java` și
  `CititorConnect.java`
- Creează: `proxy\src\test\java\ro\flota\proxy\connect\Pachete.java`
- Test: `proxy\src\test\java\ro\flota\proxy\connect\CititorConnectTest.java`

**Interfețe:**
- Produce:
  - `PachetStricat extends IOException`;
  - `record PachetConnect(String clientId, byte[] octeti)`, unde `octeti` e
    pachetul întreg, exact cum a venit;
  - `static PachetConnect CititorConnect.citeste(InputStream)`, care aruncă
    `PachetStricat` dacă pachetul e greșit și `EOFException` dacă conexiunea se
    închide înainte de primul octet;
  - `static int CititorConnect.lungimeRamasa(InputStream)`, vizibilă în pachet,
    pentru teste;
  - pentru teste: `Pachete.connect(String clientId)` și
    `Pachete.connect(String clientId, int nivel, byte[] dupaClientId)`.

- [ ] **Pasul 1: Adaugă modulul în agregator**

În `pom.xml`, `<modules>` devine:

```xml
  <modules>
    <module>simulator</module>
    <module>server</module>
    <module>proxy</module>
  </modules>
```

- [ ] **Pasul 2: Scrie `proxy\pom.xml`**

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

  <artifactId>proxy</artifactId>

  <dependencies>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <configuration>
          <mainClass>ro.flota.proxy.Main</mainClass>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Pasul 3: Scrie helper-ul de test, care construiește pachete**

`proxy\src\test\java\ro\flota\proxy\connect\Pachete.java`:

```java
package ro.flota.proxy.connect;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Pachete CONNECT construite de mână, octet cu octet, după tabelul din spec §5.2. */
final class Pachete {

    private Pachete() {
    }

    static byte[] connect(String clientId) {
        return connect(clientId, 4, new byte[0]);
    }

    static byte[] connect(String clientId, int nivel, byte[] dupaClientId) {
        ByteArrayOutputStream corp = new ByteArrayOutputStream();
        text(corp, "MQTT");
        corp.write(nivel);
        corp.write(0x02);   // flag-urile: clean session
        corp.write(0x00);   // keepalive-ul, pe 2 octeți: 5 secunde
        corp.write(0x05);
        text(corp, clientId);
        corp.writeBytes(dupaClientId);

        ByteArrayOutputStream pachet = new ByteArrayOutputStream();
        pachet.write(0x10); // tipul: CONNECT
        lungime(pachet, corp.size());
        pachet.writeBytes(corp.toByteArray());
        return pachet.toByteArray();
    }

    /** Lungimea rămasă: 7 biți pe octet, cei mai puțin semnificativi întâi; bitul de sus = „mai urmează”. */
    static void lungime(ByteArrayOutputStream out, int n) {
        do {
            int octet = n % 128;
            n /= 128;
            if (n > 0) {
                octet |= 0x80;
            }
            out.write(octet);
        } while (n > 0);
    }

    private static void text(ByteArrayOutputStream out, String s) {
        byte[] octeti = s.getBytes(StandardCharsets.UTF_8);
        out.write(octeti.length >> 8);
        out.write(octeti.length & 0xFF);
        out.writeBytes(octeti);
    }
}
```

- [ ] **Pasul 4: Scrie testele care pică**

`proxy\src\test\java\ro\flota\proxy\connect\CititorConnectTest.java`:

```java
package ro.flota.proxy.connect;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CititorConnectTest {

    private static InputStream flux(byte[]... bucati) {
        int total = Arrays.stream(bucati).mapToInt(b -> b.length).sum();
        byte[] tot = new byte[total];
        int pozitie = 0;
        for (byte[] b : bucati) {
            System.arraycopy(b, 0, tot, pozitie, b.length);
            pozitie += b.length;
        }
        return new ByteArrayInputStream(tot);
    }

    private static InputStream octeti(int... valori) {
        byte[] b = new byte[valori.length];
        for (int i = 0; i < valori.length; i++) {
            b[i] = (byte) valori[i];
        }
        return new ByteArrayInputStream(b);
    }

    private static String stricat(InputStream in) {
        return assertThrows(PachetStricat.class, () -> CititorConnect.citeste(in)).getMessage();
    }

    @Test
    void citesteClientId() throws IOException {
        assertEquals("masina-3", CititorConnect.citeste(flux(Pachete.connect("masina-3"))).clientId());
    }

    @Test
    void octetiiSintCeiPrimitiIntocmai() throws IOException {
        byte[] cuWill = Pachete.connect("masina-3", 4, new byte[] {0, 3, 'a', '/', 'b', 0, 1, 'x'});
        assertArrayEquals(cuWill, CititorConnect.citeste(flux(cuWill)).octeti());
    }

    @Test
    void nuCitesteNimicDupaPachet() throws IOException {
        InputStream in = flux(Pachete.connect("masina-3"), "XYZ".getBytes(StandardCharsets.US_ASCII));
        CititorConnect.citeste(in);
        assertEquals('X', in.read());
    }

    @Test
    void lungimeaRamasaPeDoiOcteti() throws IOException {
        String lung = "masina-" + "9".repeat(200);
        byte[] pachet = Pachete.connect(lung);
        assertTrue((pachet[1] & 0x80) != 0, "lungimea ar trebui să aibă al doilea octet");
        PachetConnect p = CititorConnect.citeste(flux(pachet));
        assertEquals(lung, p.clientId());
        assertEquals(pachet.length, p.octeti().length);
    }

    @Test
    void lungimeaRamasaPeUnuPinaLaPatruOcteti() throws IOException {
        assertEquals(0, CititorConnect.lungimeRamasa(octeti(0x00)));
        assertEquals(127, CititorConnect.lungimeRamasa(octeti(0x7F)));
        assertEquals(128, CititorConnect.lungimeRamasa(octeti(0x80, 0x01)));
        assertEquals(16_383, CititorConnect.lungimeRamasa(octeti(0xFF, 0x7F)));
        assertEquals(16_384, CititorConnect.lungimeRamasa(octeti(0x80, 0x80, 0x01)));
        assertEquals(268_435_455, CititorConnect.lungimeRamasa(octeti(0xFF, 0xFF, 0xFF, 0x7F)));
    }

    @Test
    void lungimeaRamasaPeCinciOctetiEsteStricata() {
        assertThrows(PachetStricat.class, () -> CititorConnect.lungimeRamasa(octeti(0x80, 0x80, 0x80, 0x80, 0x01)));
    }

    @Test
    void primulPachetCareNuEConnectEsteRefuzat() {
        assertTrue(stricat(octeti(0x30, 0x00)).contains("0x30"));
    }

    @Test
    void unBrowserNuEsteMqtt() {
        String mesaj = stricat(flux("GET / HTTP/1.1\r\n".getBytes(StandardCharsets.US_ASCII)));
        assertTrue(mesaj.contains("nu e CONNECT"), mesaj);
    }

    @Test
    void nivelulGresitEsteRefuzat() {
        assertTrue(stricat(flux(Pachete.connect("masina-3", 5, new byte[0]))).contains("MQTT 3.1.1"));
    }

    @Test
    void pachetulTaiatEsteRefuzat() {
        byte[] pachet = Pachete.connect("masina-3");
        assertTrue(stricat(flux(Arrays.copyOf(pachet, pachet.length - 3))).contains("tăiat"));
    }

    @Test
    void clientIdGolEsteRefuzat() {
        assertTrue(stricat(flux(Pachete.connect(""))).contains("gol"));
    }

    @Test
    void lungimeaUriasaEsteRefuzataFaraSaCiteasca() {
        assertTrue(stricat(octeti(0x10, 0xFF, 0xFF, 0x7F)).contains("maximul"));
    }

    @Test
    void conexiuneaInchisaInainteDeOrice() {
        assertThrows(EOFException.class, () -> CititorConnect.citeste(octeti()));
    }
}
```

- [ ] **Pasul 5: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru
`CititorConnect`, `PachetConnect` și `PachetStricat`.

- [ ] **Pasul 6: Scrie `PachetStricat`, `CopieStream` și `PachetConnect`**

`PachetStricat.java`:

```java
package ro.flota.proxy.connect;

import java.io.IOException;

/** Primul pachet de pe o conexiune nu e un CONNECT bun. Proxy-ul închide conexiunea și scrie motivul. */
public final class PachetStricat extends IOException {

    public PachetStricat(String motiv) {
        super(motiv);
    }
}
```

`CopieStream.java`:

```java
package ro.flota.proxy.connect;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Un InputStream care ține minte tot ce s-a citit prin el, ca pachetul să plece mai departe întocmai. */
final class CopieStream extends FilterInputStream {

    private final ByteArrayOutputStream copie = new ByteArrayOutputStream();

    CopieStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b >= 0) {
            copie.write(b);
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n > 0) {
            copie.write(b, off, n);
        }
        return n;
    }

    byte[] octeti() {
        return copie.toByteArray();
    }
}
```

`PachetConnect.java`:

```java
package ro.flota.proxy.connect;

/** Primul pachet al unei mașini: cine e și octeții lui, exact cum au venit. */
public record PachetConnect(String clientId, byte[] octeti) {
}
```

- [ ] **Pasul 7: Scrie `CititorConnect`**

```java
package ro.flota.proxy.connect;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Citește primul pachet de pe o conexiune nouă, care trebuie să fie CONNECT din MQTT 3.1.1 (spec §5.2).
 * E singurul loc din proiect care se uită înăuntrul unui pachet MQTT.
 */
public final class CititorConnect {

    static final int TIP_CONNECT = 0x10;
    static final int NIVEL_MQTT_311 = 4;
    static final int LUNGIME_MAXIMA = 64 * 1024;

    private CititorConnect() {
    }

    /**
     * Citește exact un pachet, niciun octet în plus: restul conexiunii rămâne pentru Legatura.
     * Întoarce client ID-ul și octeții pachetului, neschimbați, ca să plece mai departe la broker.
     */
    public static PachetConnect citeste(InputStream in) throws IOException {
        CopieStream copie = new CopieStream(in);
        int tip = copie.read();
        if (tip < 0) {
            throw new EOFException("conexiunea s-a închis înainte de CONNECT");
        }
        if (tip != TIP_CONNECT) {
            throw new PachetStricat(String.format("primul pachet nu e CONNECT (0x%02X)", tip));
        }
        int lungime = lungimeRamasa(copie);
        if (lungime > LUNGIME_MAXIMA) {
            throw new PachetStricat("CONNECT ar avea " + lungime + " octeți; maximul e " + LUNGIME_MAXIMA);
        }
        byte[] rest = copie.readNBytes(lungime);
        if (rest.length < lungime) {
            throw new PachetStricat("CONNECT tăiat: " + rest.length + " din " + lungime + " octeți");
        }
        DataInputStream corp = new DataInputStream(new ByteArrayInputStream(rest));
        try {
            String protocol = text(corp);
            if (!protocol.equals("MQTT")) {
                throw new PachetStricat("protocolul e \"" + protocol + "\", nu MQTT");
            }
            int nivel = corp.readUnsignedByte();
            if (nivel != NIVEL_MQTT_311) {
                throw new PachetStricat("nivelul protocolului e " + nivel + ", aștept 4 (MQTT 3.1.1)");
            }
            corp.readUnsignedByte();  // flag-urile conexiunii
            corp.readUnsignedShort(); // keepalive-ul
            String clientId = text(corp);
            if (clientId.isEmpty()) {
                throw new PachetStricat("client ID-ul e gol");
            }
            return new PachetConnect(clientId, copie.octeti());
        } catch (EOFException e) {
            throw new PachetStricat("CONNECT se termină înainte de client ID");
        }
    }

    /**
     * Lungimea rămasă: câte 7 biți pe octet, cei mai puțin semnificativi întâi. Bitul de sus, 0x80,
     * spune că mai urmează un octet. Cel mult 4 octeți.
     */
    static int lungimeRamasa(InputStream in) throws IOException {
        int valoare = 0;
        int multiplicator = 1;
        for (int i = 0; i < 4; i++) {
            int b = in.read();
            if (b < 0) {
                throw new EOFException("lungimea rămasă e tăiată");
            }
            valoare += (b & 0x7F) * multiplicator;
            if ((b & 0x80) == 0) {
                return valoare;
            }
            multiplicator *= 128;
        }
        throw new PachetStricat("lungimea rămasă are mai mult de 4 octeți");
    }

    /** Un text MQTT: lungimea pe 2 octeți, cel mai semnificativ întâi, apoi textul în UTF-8. */
    private static String text(DataInputStream in) throws IOException {
        int n = in.readUnsignedShort();
        byte[] octeti = in.readNBytes(n);
        if (octeti.length < n) {
            throw new EOFException();
        }
        return new String(octeti, StandardCharsets.UTF_8);
    }
}
```

- [ ] **Pasul 8: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 13 teste din `CititorConnectTest` trecute.

- [ ] **Pasul 9: Claude explică.** Analogia: proxy-ul e un curier care
  deschide doar primul plic, ca să afle numele expeditorului, apoi îl lipește
  la loc și nu mai deschide nimic. Apoi:
  - octeții din tabelul din spec §5.2, unul câte unul, cu testul
    `octetiiSintCeiPrimitiIntocmai`;
  - cum se adună lungimea rămasă: 128 e `0x80 0x01`, adică 0 + 1 × 128;
  - de ce citim exact un pachet: ce vine după aparține de `Legatura`;
  - de ce `CopieStream`: pachetul trebuie să plece întocmai, inclusiv Last
    Will-ul de după client ID.

- [ ] **Pasul 10: Commit**

```bash
git add pom.xml proxy
git commit -m "Proxy: modulul si citirea pachetului CONNECT

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Vremea și registrul mașinilor

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „o stare folosită de mai
multe fire, protejată cu `synchronized`, și timpul injectat ca să fie testabil”.

**Fișiere:**
- Creează: `proxy\src\main\java\ro\flota\proxy\vreme\Vreme.java`, `Registru.java`
- Creează: `proxy\src\test\java\ro\flota\proxy\testutil\CeasManual.java`
- Teste: `proxy\src\test\java\ro\flota\proxy\vreme\VremeTest.java`, `RegistruTest.java`

**Interfețe:**
- Produce:
  - `Vreme(Clock)`, cu metodele care schimbă vremea: `intarziere(int ms, int variatie)`,
    `banda(int kbps)`, `tunel(int secunde)`, `gaura(int secunde)` și `normal()`;
  - și cu metodele care o citesc: `boolean inTunel()`,
    `Instant gauraPana()` (`null` dacă nu e gaură acum), `int intarziereMs()`,
    `int variatieMs()`, `int bandaKbps()` (0 înseamnă fără limită) și
    `String descriere()`;
  - `Registru(Clock)`, cu `Vreme vreme(String masina)`, care o și face
    „văzută”, `boolean cunoscuta(String)` și `List<String> masini()`, în ordinea
    numerelor;
  - în `Registru`, legăturile deschise:
    `adaugaLegatura(String, Closeable)`, `scoateLegatura(String, Closeable)` și
    `inchideLegaturile(String)`;
  - pentru teste: `CeasManual(String inceputIso)`, cu `avanseaza(long ms)`.

- [ ] **Pasul 1: Scrie ceasul manual, pentru teste**

`proxy\src\test\java\ro\flota\proxy\testutil\CeasManual.java`:

```java
package ro.flota.proxy.testutil;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Un ceas care stă pe loc până îl muți tu. Așa, un tunel de 15 secunde se testează într-o clipă. */
public final class CeasManual extends Clock {

    private Instant acum;

    public CeasManual(String inceputIso) {
        this.acum = Instant.parse(inceputIso);
    }

    public void avanseaza(long ms) {
        acum = acum.plusMillis(ms);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }

    @Override
    public Instant instant() {
        return acum;
    }
}
```

- [ ] **Pasul 2: Scrie testele care pică**

`VremeTest.java`:

```java
package ro.flota.proxy.vreme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ro.flota.proxy.testutil.CeasManual;

class VremeTest {

    private final CeasManual ceas = new CeasManual("2026-12-10T10:00:00Z");
    private final Vreme vreme = new Vreme(ceas);

    @Test
    void laInceputEsteNormal() {
        assertEquals("NORMAL", vreme.descriere());
        assertFalse(vreme.inTunel());
        assertNull(vreme.gauraPana());
        assertEquals(0, vreme.bandaKbps());
    }

    @Test
    void intarziereaSiBandaSeCombina() {
        vreme.intarziere(500, 200);
        vreme.banda(64);
        assertEquals("INTARZIERE 500 200 BANDA 64", vreme.descriere());
    }

    @Test
    void intarziereaFaraVariatieSeScrieCu0() {
        vreme.intarziere(500, 0);
        assertEquals("INTARZIERE 500 0", vreme.descriere());
    }

    @Test
    void doarBanda() {
        vreme.banda(64);
        assertEquals("BANDA 64", vreme.descriere());
    }

    @Test
    void tunelulTineCatSeCere() {
        vreme.tunel(12);
        assertTrue(vreme.inTunel());
        assertEquals("TUNEL 12", vreme.descriere());
        ceas.avanseaza(11_500);
        assertEquals("TUNEL 1", vreme.descriere());
        ceas.avanseaza(500);
        assertFalse(vreme.inTunel());
        assertEquals("NORMAL", vreme.descriere());
    }

    @Test
    void tunelulAscundeIntarziereaCatDureaza() {
        vreme.intarziere(500, 0);
        vreme.tunel(5);
        assertEquals("TUNEL 5", vreme.descriere());
        ceas.avanseaza(5_000);
        assertEquals("INTARZIERE 500 0", vreme.descriere());
    }

    @Test
    void gauraSpunePanaCand() {
        vreme.gaura(10);
        assertNotNull(vreme.gauraPana());
        assertEquals("GAURA 10", vreme.descriere());
        ceas.avanseaza(10_000);
        assertNull(vreme.gauraPana());
    }

    @Test
    void gauraDupaTunelInlocuiesteTunelul() {
        vreme.tunel(30);
        vreme.gaura(10);
        assertFalse(vreme.inTunel());
        assertEquals("GAURA 10", vreme.descriere());
    }

    @Test
    void tunelDupaGauraInlocuiesteGaura() {
        vreme.gaura(30);
        vreme.tunel(10);
        assertNull(vreme.gauraPana());
        assertEquals("TUNEL 10", vreme.descriere());
    }

    @Test
    void normalOpresteTotul() {
        vreme.intarziere(500, 200);
        vreme.banda(64);
        vreme.gaura(30);
        vreme.normal();
        assertEquals("NORMAL", vreme.descriere());
        assertNull(vreme.gauraPana());
        assertEquals(0, vreme.intarziereMs());
    }
}
```

`RegistruTest.java`:

```java
package ro.flota.proxy.vreme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RegistruTest {

    private final Registru registru = new Registru(Clock.systemUTC());

    @Test
    void masinileVinInOrdineaNumerelor() {
        registru.vreme("masina-10");
        registru.vreme("masina-2");
        registru.vreme("masina-1");
        assertEquals(List.of("masina-1", "masina-2", "masina-10"), registru.masini());
    }

    @Test
    void doarCeleVazuteSintCunoscute() {
        registru.vreme("masina-3");
        assertTrue(registru.cunoscuta("masina-3"));
        assertFalse(registru.cunoscuta("masina-4"));
    }

    @Test
    void aceeasiMasinaAreAceeasiVreme() {
        assertSame(registru.vreme("masina-3"), registru.vreme("masina-3"));
    }

    @Test
    void inchideDoarLegaturileMasiniiCerute() {
        AtomicInteger inchise3 = new AtomicInteger();
        AtomicInteger inchise4 = new AtomicInteger();
        Closeable a = inchise3::incrementAndGet;
        Closeable b = inchise3::incrementAndGet;
        Closeable c = inchise4::incrementAndGet;
        registru.adaugaLegatura("masina-3", a);
        registru.adaugaLegatura("masina-3", b);
        registru.adaugaLegatura("masina-4", c);
        registru.inchideLegaturile("masina-3");
        assertEquals(2, inchise3.get());
        assertEquals(0, inchise4.get());
    }

    @Test
    void oLegaturaScoasaNuMaiEInchisa() {
        AtomicInteger inchise = new AtomicInteger();
        Closeable a = inchise::incrementAndGet;
        registru.adaugaLegatura("masina-3", a);
        registru.scoateLegatura("masina-3", a);
        registru.inchideLegaturile("masina-3");
        assertEquals(0, inchise.get());
    }
}
```

- [ ] **Pasul 3: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Vreme` și
`Registru`.

- [ ] **Pasul 4: Scrie `Vreme`**

```java
package ro.flota.proxy.vreme;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.StringJoiner;

/**
 * Vremea unei mașini (spec §5.3). O schimbă firul de ordine și o citesc firele care scriu,
 * deci toate metodele sînt synchronized: un singur fir pe rând are voie înăuntru.
 */
public final class Vreme {

    private final Clock ceas;
    private int intarziereMs;
    private int variatieMs;
    private int bandaKbps;
    private Instant tunelPana;
    private Instant gauraPana;

    public Vreme(Clock ceas) {
        this.ceas = ceas;
    }

    public synchronized void intarziere(int ms, int variatie) {
        intarziereMs = ms;
        variatieMs = variatie;
    }

    public synchronized void banda(int kbps) {
        bandaKbps = kbps;
    }

    /** Un tunel nou înlocuiește o gaură în curs (D24). */
    public synchronized void tunel(int secunde) {
        tunelPana = ceas.instant().plusSeconds(secunde);
        gauraPana = null;
    }

    /** O gaură nouă înlocuiește un tunel în curs (D24). */
    public synchronized void gaura(int secunde) {
        gauraPana = ceas.instant().plusSeconds(secunde);
        tunelPana = null;
    }

    public synchronized void normal() {
        intarziereMs = 0;
        variatieMs = 0;
        bandaKbps = 0;
        tunelPana = null;
        gauraPana = null;
    }

    public synchronized boolean inTunel() {
        return activ(tunelPana);
    }

    /** Până când ține gaura, sau null dacă acum nu e gaură. */
    public synchronized Instant gauraPana() {
        return activ(gauraPana) ? gauraPana : null;
    }

    public synchronized int intarziereMs() {
        return intarziereMs;
    }

    public synchronized int variatieMs() {
        return variatieMs;
    }

    /** 0 înseamnă fără limită. */
    public synchronized int bandaKbps() {
        return bandaKbps;
    }

    /** Rândul din răspunsul la STARE, fără numele mașinii. Tunelul și gaura le ascund pe celelalte cât durează. */
    public synchronized String descriere() {
        if (activ(tunelPana)) {
            return "TUNEL " + secundeRamase(tunelPana);
        }
        if (activ(gauraPana)) {
            return "GAURA " + secundeRamase(gauraPana);
        }
        StringJoiner d = new StringJoiner(" ");
        if (intarziereMs > 0 || variatieMs > 0) {
            d.add("INTARZIERE " + intarziereMs + " " + variatieMs);
        }
        if (bandaKbps > 0) {
            d.add("BANDA " + bandaKbps);
        }
        return d.length() == 0 ? "NORMAL" : d.toString();
    }

    private boolean activ(Instant pana) {
        return pana != null && ceas.instant().isBefore(pana);
    }

    /** Rotunjit în sus: cu 0,5 secunde rămase, scrie 1, nu 0. */
    private long secundeRamase(Instant pana) {
        long ms = Duration.between(ceas.instant(), pana).toMillis();
        return (ms + 999) / 1000;
    }
}
```

- [ ] **Pasul 5: Scrie `Registru`**

```java
package ro.flota.proxy.vreme;

import java.io.Closeable;
import java.io.IOException;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mașinile văzute de proxy măcar o dată, cu vremea fiecăreia și cu conexiunile ei deschise.
 * Hărțile sînt concurente: le folosesc firele de conexiune și firele de ordine în același timp.
 */
public final class Registru {

    private final Clock ceas;
    private final Map<String, Vreme> vremi = new ConcurrentHashMap<>();
    private final Map<String, Set<Closeable>> legaturi = new ConcurrentHashMap<>();

    public Registru(Clock ceas) {
        this.ceas = ceas;
    }

    /** Vremea mașinii. Prima cerere o face „văzută”, deci intră în TOATE și în STARE. */
    public Vreme vreme(String masina) {
        return vremi.computeIfAbsent(masina, id -> new Vreme(ceas));
    }

    public boolean cunoscuta(String masina) {
        return vremi.containsKey(masina);
    }

    /** În ordinea numerelor: masina-2 înainte de masina-10. Un nume fără număr vine la sfârșit. */
    public List<String> masini() {
        return vremi.keySet().stream()
                .sorted(Comparator.comparingInt(Registru::numar).thenComparing(Comparator.naturalOrder()))
                .toList();
    }

    public void adaugaLegatura(String masina, Closeable legatura) {
        legaturi.computeIfAbsent(masina, id -> ConcurrentHashMap.newKeySet()).add(legatura);
    }

    public void scoateLegatura(String masina, Closeable legatura) {
        Set<Closeable> deschise = legaturi.get(masina);
        if (deschise != null) {
            deschise.remove(legatura);
        }
    }

    /** Închide toate conexiunile deschise ale mașinii: așa începe un tunel. */
    public void inchideLegaturile(String masina) {
        Set<Closeable> deschise = legaturi.get(masina);
        if (deschise == null) {
            return;
        }
        for (Closeable legatura : List.copyOf(deschise)) {
            try {
                legatura.close();
            } catch (IOException e) {
                // Era deja închisă.
            }
        }
    }

    private static int numar(String id) {
        try {
            return Integer.parseInt(id.substring(id.lastIndexOf('-') + 1));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
```

- [ ] **Pasul 6: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu toate testele din `VremeTest` și `RegistruTest`
trecute.

- [ ] **Pasul 7: Claude explică.** Analogia: `Vreme` e tabla de la intrarea
  într-un tunel de munte, pe care scrie „închis până la 10:15”. Șoferii o
  citesc des, iar dispecerul o schimbă rar, dar niciodată în timp ce cineva o
  citește pe jumătate. Asta face `synchronized`. Apoi:
  - de ce ora de sfârșit e un `Instant`, nu un număr care scade;
  - `CeasManual`: testele mută timpul în loc să aștepte;
  - de ce `Registru` folosește `ConcurrentHashMap` și `computeIfAbsent`.

- [ ] **Pasul 8: Commit**

```bash
git add proxy/src
git commit -m "Proxy: vremea si registrul masinilor

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Planificatorul și un sens al conexiunii

**Cine scrie:** Claude, ca exemplu explicat. E tiparul 1 din spec §9: firul pe
conexiune, cu citirea și scrierea separate.

**Fișiere:**
- Creează, în `proxy\src\main\java\ro\flota\proxy\transport\`: `Bucata.java`,
  `Planificator.java` și `Sens.java`
- Teste: `proxy\src\test\java\ro\flota\proxy\transport\PlanificatorTest.java`, `SensTest.java`

**Interfețe:**
- Consumă: `Vreme` (Task 2).
- Produce, toate vizibile doar în pachetul `transport`:
  - `record Bucata(byte[] octeti, long plecareMs)`;
  - `Planificator(RandomGenerator)`, cu
    `long plecare(long acumMs, int intarziereMs, int variatieMs)`;
  - `Sens(String nume, InputStream in, OutputStream out, Vreme vreme, Runnable laSfarsit, RandomGenerator aleator)`,
    cu `void trimiteIntai(byte[])`, `void porneste()` și `void opreste()`.
    `laSfarsit` se cheamă când citirea sau scrierea se termină singură, nu
    când sensul e oprit cu `opreste()`.

- [ ] **Pasul 1: Scrie testele care pică pentru planificator**

`PlanificatorTest.java`:

```java
package ro.flota.proxy.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class PlanificatorTest {

    @Test
    void faraVariatieEsteExactIntarzierea() {
        Planificator p = new Planificator(new Random(1));
        assertEquals(1_500, p.plecare(1_000, 500, 0));
    }

    @Test
    void variatiaNuSchimbaNiciodataOrdinea() {
        Planificator p = new Planificator(new Random(42));
        long acum = 0;
        long anterioara = Long.MIN_VALUE;
        for (int i = 0; i < 10_000; i++) {
            acum += i % 7; // bucăți venite la 0–6 ms una după alta
            long plecare = p.plecare(acum, 500, 400);
            assertTrue(plecare >= anterioara, "bucata " + i + " ar pleca înaintea celei dinainte");
            assertTrue(plecare >= acum + 100, "bucata " + i + " pleacă prea devreme");
            anterioara = plecare;
        }
    }

    @Test
    void variatiaStaInInterval() {
        Planificator p = new Planificator(new Random(7));
        for (long acum = 0; acum < 10_000_000; acum += 10_000) {
            long intarziere = p.plecare(acum, 500, 200) - acum;
            assertTrue(intarziere >= 300 && intarziere <= 700, "întârzierea " + intarziere);
        }
    }

    @Test
    void nuPleacaNiciodataInainteDeAcum() {
        Planificator p = new Planificator(new Random(3));
        for (long acum = 0; acum < 1_000_000; acum += 10_000) {
            assertTrue(p.plecare(acum, 100, 300) >= acum);
        }
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Planificator`.

- [ ] **Pasul 3: Scrie `Bucata` și `Planificator`**

`Bucata.java`:

```java
package ro.flota.proxy.transport;

/** Octeții citiți dintr-o dată de pe o conexiune, cu ora la care au voie să plece. */
record Bucata(byte[] octeti, long plecareMs) {
}
```

`Planificator.java`:

```java
package ro.flota.proxy.transport;

import java.util.random.RandomGenerator;

/**
 * Ora de plecare a fiecărei bucăți (spec §5.3): acum + întârziere ± variație, dar niciodată
 * înaintea bucății dinainte. Fără regula asta, variația ar inversa bucăți, iar TCP garantează ordinea.
 * Îl folosește un singur fir, cel care citește, deci n-are nevoie de synchronized.
 */
final class Planificator {

    private final RandomGenerator aleator;
    private long ultimaPlecare = Long.MIN_VALUE;

    Planificator(RandomGenerator aleator) {
        this.aleator = aleator;
    }

    long plecare(long acumMs, int intarziereMs, int variatieMs) {
        long abatere = variatieMs == 0 ? 0 : aleator.nextLong(-variatieMs, variatieMs + 1L);
        long plecare = Math.max(acumMs, acumMs + intarziereMs + abatere);
        plecare = Math.max(plecare, ultimaPlecare);
        ultimaPlecare = plecare;
        return plecare;
    }
}
```

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 4 teste din `PlanificatorTest` trecute.

- [ ] **Pasul 5: Scrie testele care pică pentru sens**

`SensTest.java`. Testele folosesc fluxuri legate în memorie, nu socket-uri:

```java
package ro.flota.proxy.transport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import ro.flota.proxy.vreme.Vreme;

class SensTest {

    private final Vreme vreme = new Vreme(Clock.systemUTC());
    private final PipedOutputStream intrare = new PipedOutputStream();
    private final ByteArrayOutputStream iesire = new ByteArrayOutputStream();
    private final CountDownLatch sfarsit = new CountDownLatch(1);

    private Sens sens() throws IOException {
        PipedInputStream dinIntrare = new PipedInputStream(intrare, 64 * 1024);
        return new Sens("test", dinIntrare, iesire, vreme, sfarsit::countDown, new Random(7));
    }

    private static void asteapta(BooleanSupplier conditie, long ms) throws InterruptedException {
        long limita = System.currentTimeMillis() + ms;
        while (!conditie.getAsBoolean() && System.currentTimeMillis() < limita) {
            Thread.sleep(10);
        }
    }

    @Test
    void octetiiAjungInOrdineCuIntarziereSiVariatie() throws Exception {
        vreme.intarziere(50, 40);
        Sens s = sens();
        s.porneste();
        byte[] trimis = new byte[2_000];
        for (int i = 0; i < trimis.length; i++) {
            trimis[i] = (byte) i;
            intrare.write(trimis[i]);
            intrare.flush();
            if (i % 100 == 0) {
                Thread.sleep(1);
            }
        }
        asteapta(() -> iesire.size() == trimis.length, 3_000);
        assertArrayEquals(trimis, iesire.toByteArray());
        intrare.close();
        assertTrue(sfarsit.await(2, TimeUnit.SECONDS), "laSfarsit n-a fost chemat la închidere");
    }

    @Test
    void connectPleacaPrimul() throws Exception {
        Sens s = sens();
        s.trimiteIntai("CONNECT".getBytes(StandardCharsets.US_ASCII));
        s.porneste();
        intrare.write("PUBLISH".getBytes(StandardCharsets.US_ASCII));
        intrare.flush();
        asteapta(() -> iesire.size() == 14, 2_000);
        assertEquals("CONNECTPUBLISH", iesire.toString(StandardCharsets.US_ASCII));
        intrare.close();
    }

    @Test
    void gauraTineOctetiiPanaTrece() throws Exception {
        vreme.gaura(1);
        Sens s = sens();
        s.porneste();
        intrare.write("abc".getBytes(StandardCharsets.US_ASCII));
        intrare.flush();
        Thread.sleep(500);
        assertEquals(0, iesire.size(), "în gaură nu trebuie să treacă nimic");
        asteapta(() -> iesire.size() == 3, 2_000);
        assertEquals("abc", iesire.toString(StandardCharsets.US_ASCII));
        intrare.close();
    }
}
```

- [ ] **Pasul 6: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Sens`.

- [ ] **Pasul 7: Scrie `Sens`**

```java
package ro.flota.proxy.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.random.RandomGenerator;
import ro.flota.proxy.vreme.Vreme;

/**
 * Un sens al unei conexiuni, de exemplu mașină → broker (spec §5.1). Un fir citește și pune bucățile în
 * coadă, cu ora de plecare; alt fir le scrie la ora lor. Un singur fir care ar citi, apoi ar dormi cât
 * întârzierea, ar aduna întârzierile una peste alta.
 */
final class Sens {

    private static final int MARIME_BUCATA = 4_096;
    private static final long PAS_ASTEPTARE_MS = 100;

    private final InputStream in;
    private final OutputStream out;
    private final Vreme vreme;
    private final Runnable laSfarsit;
    private final Planificator planificator;
    private final BlockingQueue<Bucata> coada = new LinkedBlockingQueue<>();
    private final Thread cititor;
    private final Thread scriitor;
    private volatile boolean oprit = false;

    Sens(String nume, InputStream in, OutputStream out, Vreme vreme, Runnable laSfarsit, RandomGenerator aleator) {
        this.in = in;
        this.out = out;
        this.vreme = vreme;
        this.laSfarsit = laSfarsit;
        this.planificator = new Planificator(aleator);
        this.cititor = new Thread(this::citeste, nume + "-citire");
        this.scriitor = new Thread(this::scrie, nume + "-scriere");
        cititor.setDaemon(true);
        scriitor.setDaemon(true);
    }

    /** Octeții dați pleacă primii, cu vremea aplicată. Așa trece CONNECT-ul deja citit de proxy. */
    void trimiteIntai(byte[] octeti) {
        coada.add(new Bucata(octeti, planificator.plecare(System.currentTimeMillis(),
                vreme.intarziereMs(), vreme.variatieMs())));
    }

    void porneste() {
        cititor.start();
        scriitor.start();
    }

    /** Oprește scrierea și aruncă ce era în coadă. Citirea se oprește când Legatura închide socket-ul. */
    void opreste() {
        oprit = true;
        coada.clear();
        scriitor.interrupt();
    }

    private void citeste() {
        byte[] buf = new byte[MARIME_BUCATA];
        try {
            int n;
            while (!oprit && (n = in.read(buf)) >= 0) {
                long plecare = planificator.plecare(System.currentTimeMillis(), vreme.intarziereMs(), vreme.variatieMs());
                coada.add(new Bucata(Arrays.copyOf(buf, n), plecare));
            }
        } catch (IOException e) {
            // Conexiunea s-a rupt; mai jos se anunță sfârșitul.
        }
        if (!oprit) {
            laSfarsit.run();
        }
    }

    private void scrie() {
        try {
            while (!oprit) {
                Bucata bucata = coada.take();
                asteaptaPana(bucata.plecareMs());
                asteaptaSaTreacaGaura();
                out.write(bucata.octeti());
                out.flush();
                int kbps = vreme.bandaKbps();
                if (kbps > 0) {
                    Thread.sleep(bucata.octeti().length * 8L / kbps); // kbps = biți pe milisecundă
                }
            }
        } catch (InterruptedException e) {
            return; // oprit de Legatura
        } catch (IOException e) {
            // Celălalt capăt e închis.
        }
        if (!oprit) {
            laSfarsit.run();
        }
    }

    private static void asteaptaPana(long plecareMs) throws InterruptedException {
        long ramas;
        while ((ramas = plecareMs - System.currentTimeMillis()) > 0) {
            Thread.sleep(Math.min(ramas, PAS_ASTEPTARE_MS));
        }
    }

    /** Cât ține gaura, octeții stau în coadă. Verific la 100 ms, ca NORMAL să deblocheze repede. */
    private void asteaptaSaTreacaGaura() throws InterruptedException {
        while (vreme.gauraPana() != null) {
            Thread.sleep(PAS_ASTEPTARE_MS);
        }
    }
}
```

- [ ] **Pasul 8: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 3 teste din `SensTest` trecute. Testele cu
fire depind puțin de cât de ocupat e laptopul. Dacă `gauraTineOctetiiPanaTrece`
pică o dată și trece la a doua rulare, se notează. Dacă pică des, se mărește
gaura la 2 secunde și așteptarea la 3.

- [ ] **Pasul 9: Claude explică.** Analogia: un sens e o bandă de bagaje la
  aeroport. Un om pune bagajele pe bandă și lipește pe fiecare ora la care are
  voie să iasă, iar alt om le ia de la capăt când le vine ora. Dacă un singur
  om ar pune un bagaj, ar aștepta, apoi l-ar lua, al doilea bagaj ar sta după
  primul, deci întârzierile s-ar aduna. Apoi:
  - de ce ora de plecare nu e niciodată mai devreme decât a bucății dinainte;
  - cum aplică gaura neagră și banda îngustă firul care scrie;
  - de ce `laSfarsit` nu se cheamă după `opreste()`.

- [ ] **Pasul 10: Commit**

```bash
git add proxy/src
git commit -m "Proxy: planificatorul si un sens al conexiunii

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Legătura dintre mașină și broker

**Cine scrie:** Tudor, ca repetiție a tiparului 1: două sensuri pe aceeași
conexiune, iar când unul se termină, se închide tot. Claude scrie testul.

**Fișiere:**
- Creează: `proxy\src\main\java\ro\flota\proxy\transport\Legatura.java`
- Test: `proxy\src\test\java\ro\flota\proxy\transport\LegaturaTest.java`

**Interfețe:**
- Consumă: `Sens` (Task 3), `Registru`, `Vreme` (Task 2).
- Produce: `public final class Legatura implements Closeable`, cu:
  - constructorul `Legatura(String masina, Socket spreMasina, Socket spreBroker, Registru registru) throws IOException`;
  - `void porneste(byte[] connect)`: se înregistrează în registru, pune
    `connect` primul spre broker și pornește ambele sensuri;
  - `void close()`, idempotent: oprește sensurile, închide ambele socket-uri și
    se scoate din registru;
  - `boolean inchisa()`.

- [ ] **Pasul 1: Scrie testele care pică**

`LegaturaTest.java`:

```java
package ro.flota.proxy.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ro.flota.proxy.vreme.Registru;

class LegaturaTest {

    private final List<Closeable> deInchis = new ArrayList<>();
    private final Registru registru = new Registru(Clock.systemUTC());

    /** Două socket-uri legate între ele, pe un port ales de sistem: [capătul din test, capătul dat legăturii]. */
    private Socket[] pereche() throws IOException {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Socket client = new Socket(InetAddress.getLoopbackAddress(), server.getLocalPort());
            Socket acceptat = server.accept();
            deInchis.add(client);
            deInchis.add(acceptat);
            return new Socket[] {client, acceptat};
        }
    }

    @AfterEach
    void curata() {
        for (Closeable c : deInchis) {
            try {
                c.close();
            } catch (IOException e) {
                // nimic
            }
        }
    }

    private static String citeste(Socket s, int n) throws IOException {
        s.setSoTimeout(2_000);
        return new String(s.getInputStream().readNBytes(n), StandardCharsets.US_ASCII);
    }

    private static void scrie(Socket s, String text) throws IOException {
        s.getOutputStream().write(text.getBytes(StandardCharsets.US_ASCII));
        s.getOutputStream().flush();
    }

    /** Un socket închis de celălalt capăt dă -1 la citire, sau „connection reset”. Ambele înseamnă închis. */
    private static void assertInchis(Socket s) throws IOException {
        s.setSoTimeout(2_000);
        try {
            assertEquals(-1, s.getInputStream().read());
        } catch (SocketException e) {
            // reset: tot închis
        }
    }

    @Test
    void octetiiTrecInAmbeleSensuriIarConnectPleacaPrimul() throws Exception {
        Socket[] masina = pereche();
        Socket[] broker = pereche();
        Legatura legatura = new Legatura("masina-3", masina[1], broker[0], registru);
        legatura.porneste("CONNECT".getBytes(StandardCharsets.US_ASCII));

        scrie(masina[0], "PUBLISH");
        assertEquals("CONNECTPUBLISH", citeste(broker[1], 14));
        scrie(broker[1], "CONNACK");
        assertEquals("CONNACK", citeste(masina[0], 7));
    }

    @Test
    void candBrokerulInchideSeInchideSiMasina() throws Exception {
        Socket[] masina = pereche();
        Socket[] broker = pereche();
        Legatura legatura = new Legatura("masina-3", masina[1], broker[0], registru);
        legatura.porneste(new byte[0]);

        broker[1].close();
        assertInchis(masina[0]);
        assertTrue(legatura.inchisa());
    }

    @Test
    void candMasinaInchideSeInchideSiBrokerul() throws Exception {
        Socket[] masina = pereche();
        Socket[] broker = pereche();
        new Legatura("masina-3", masina[1], broker[0], registru).porneste(new byte[0]);

        masina[0].close();
        assertInchis(broker[1]);
    }

    @Test
    void tunelulDinRegistruInchideAmbeleCapete() throws Exception {
        Socket[] masina = pereche();
        Socket[] broker = pereche();
        new Legatura("masina-3", masina[1], broker[0], registru).porneste(new byte[0]);

        registru.inchideLegaturile("masina-3");
        assertInchis(masina[0]);
        assertInchis(broker[1]);
    }

    @Test
    void aDouaInchidereNuStricaNimic() throws Exception {
        Socket[] masina = pereche();
        Socket[] broker = pereche();
        Legatura legatura = new Legatura("masina-3", masina[1], broker[0], registru);
        legatura.porneste(new byte[0]);
        legatura.close();
        legatura.close();
        assertTrue(legatura.inchisa());
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Legatura`.

- [ ] **Pasul 3: Tudor scrie `Legatura`.** Indicii:
  - două câmpuri `Sens`:
    - „urcarea” citește de la mașină și scrie la broker;
    - „coborârea” citește de la broker și scrie la mașină.

    Amândouă primesc vremea mașinii din registru și, ca `laSfarsit`, o metodă
    care închide legătura;
  - fiecare sens primește `new Random()` propriu;
  - închiderea trebuie să meargă o singură dată, chiar dacă o cer două fire
    deodată, cititorul din urcare și cel din coborâre. Un `AtomicBoolean` cu
    `compareAndSet(false, true)` face asta;
  - la închidere: oprești ambele sensuri, închizi ambele socket-uri fără să
    arunci excepții, te scoți din registru și scrii `[masina-3] legătura
    închisă` în consolă;
  - `porneste`:
    1. se adaugă în registru;
    2. dă `connect` urcării cu `trimiteIntai`;
    3. pornește ambele sensuri.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

```java
package ro.flota.proxy.transport;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import ro.flota.proxy.vreme.Registru;
import ro.flota.proxy.vreme.Vreme;

/**
 * O mașină legată de broker prin proxy: două socket-uri și două sensuri (spec §5.1).
 * Când oricare capăt se închide, se închide tot, iar octeții din cozi se aruncă.
 */
public final class Legatura implements Closeable {

    private final String masina;
    private final Socket spreMasina;
    private final Socket spreBroker;
    private final Registru registru;
    private final Sens urcare;
    private final Sens coborare;
    private final AtomicBoolean inchisa = new AtomicBoolean(false);

    public Legatura(String masina, Socket spreMasina, Socket spreBroker, Registru registru) throws IOException {
        this.masina = masina;
        this.spreMasina = spreMasina;
        this.spreBroker = spreBroker;
        this.registru = registru;
        Vreme vreme = registru.vreme(masina);
        this.urcare = new Sens(masina + "-urcare", spreMasina.getInputStream(), spreBroker.getOutputStream(),
                vreme, this::close, new Random());
        this.coborare = new Sens(masina + "-coborare", spreBroker.getInputStream(), spreMasina.getOutputStream(),
                vreme, this::close, new Random());
    }

    public void porneste(byte[] connect) {
        registru.adaugaLegatura(masina, this);
        urcare.trimiteIntai(connect);
        urcare.porneste();
        coborare.porneste();
    }

    @Override
    public void close() {
        if (!inchisa.compareAndSet(false, true)) {
            return;
        }
        urcare.opreste();
        coborare.opreste();
        inchide(spreMasina);
        inchide(spreBroker);
        registru.scoateLegatura(masina, this);
        System.out.printf("[%s] legătura închisă%n", masina);
    }

    public boolean inchisa() {
        return inchisa.get();
    }

    private static void inchide(Socket s) {
        try {
            s.close();
        } catch (IOException e) {
            // Era deja închis.
        }
    }
}
```

</details>

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 5 teste din `LegaturaTest` trecute.

- [ ] **Pasul 5: Claude revizuiește `Legatura`,** fără să rescrie. Se uită în
  special la închiderea făcută o singură dată și la ordinea din `porneste`.

- [ ] **Pasul 6: Commit-urile.** Testul e al lui Claude, iar codul al lui Tudor.

```bash
git add proxy/src/test/java/ro/flota/proxy/transport/LegaturaTest.java
git commit -m "Proxy: testul legaturii

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git add proxy/src/main/java/ro/flota/proxy/transport/Legatura.java
git commit -m "Proxy: legatura dintre masina si broker"
```

Primul commit nu compilează singur, fiindcă testul cere clasa din al doilea. Se
fac unul după altul.

---

### Task 5: Ascultătorul de mașini, iar simulatorul trece prin proxy

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „un server care acceptă
conexiuni și dă fiecăreia firul ei”, ca `ServerMultifir` de la SCD, plus tunelul
care ține mașina fără răspuns.

**Fișiere:**
- Creează: `proxy\src\main\java\ro\flota\proxy\transport\AscultatorMasini.java`
- Creează: `proxy\src\main\java\ro\flota\proxy\Main.java`
- Modifică: `simulator\src\main\resources\simulator.properties`
- Modifică: `simulator\src\test\java\ro\flota\simulator\config\SetariTest.java`

**Interfețe:**
- Consumă: `CititorConnect`, `PachetConnect` (Task 1), `Registru`, `Vreme`
  (Task 2) și `Legatura` (Task 4).
- Produce:
  - `AscultatorMasini(ServerSocket server, InetSocketAddress broker, Registru registru)`,
    care implementează `Runnable`;
  - `Main`, care deschide portul 1884;
  - simulatorul conectat la `127.0.0.1:1884`.

- [ ] **Pasul 1: Scrie `AscultatorMasini`**

```java
package ro.flota.proxy.transport;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import ro.flota.proxy.connect.CititorConnect;
import ro.flota.proxy.connect.PachetConnect;
import ro.flota.proxy.vreme.Registru;
import ro.flota.proxy.vreme.Vreme;

/**
 * Primește mașinile pe 1884 (spec §5.1). Fiecare conexiune are firul ei: citește CONNECT, află cine e
 * mașina și o leagă de Mosquitto, cu excepția cazului în care mașina e în tunel.
 */
public final class AscultatorMasini implements Runnable {

    private static final int TIMP_CONNECT_MS = 10_000;
    private static final int TIMP_CONECTARE_BROKER_MS = 5_000;
    private static final int PAS_TUNEL_MS = 200;

    private final ServerSocket server;
    private final InetSocketAddress broker;
    private final Registru registru;

    public AscultatorMasini(ServerSocket server, InetSocketAddress broker, Registru registru) {
        this.server = server;
        this.broker = broker;
        this.registru = registru;
    }

    @Override
    public void run() {
        while (!server.isClosed()) {
            try {
                Socket masina = server.accept();
                Thread fir = new Thread(() -> trateaza(masina), "conexiune-" + masina.getPort());
                fir.setDaemon(true);
                fir.start();
            } catch (IOException e) {
                if (!server.isClosed()) {
                    System.out.println("Nu pot accepta o conexiune: " + e.getMessage());
                }
            }
        }
    }

    private void trateaza(Socket masina) {
        PachetConnect connect;
        try {
            masina.setSoTimeout(TIMP_CONNECT_MS);
            connect = CititorConnect.citeste(masina.getInputStream());
            masina.setSoTimeout(0);
        } catch (IOException e) {
            System.out.printf("Închid o conexiune de la %s: %s%n", masina.getRemoteSocketAddress(), e.getMessage());
            inchide(masina);
            return;
        }
        String id = connect.clientId();
        Vreme vreme = registru.vreme(id);
        if (vreme.inTunel()) {
            tineInTunel(id, masina, vreme);
            return;
        }
        Socket spreBroker = new Socket();
        try {
            spreBroker.connect(broker, TIMP_CONECTARE_BROKER_MS);
            Legatura legatura = new Legatura(id, masina, spreBroker, registru);
            legatura.porneste(connect.octeti());
            // Un TUNEL venit între verificarea de mai sus și înregistrarea legăturii n-ar fi găsit-o.
            if (vreme.inTunel()) {
                legatura.close();
                return;
            }
            System.out.printf("[%s] legată prin proxy%n", id);
        } catch (IOException e) {
            System.out.printf("[%s] broker-ul nu răspunde (%s); închid mașina%n", id, e.getMessage());
            inchide(spreBroker);
            inchide(masina);
        }
    }

    /**
     * În tunel (spec §5.3): conexiunea TCP e acceptată și CONNECT-ul citit, dar nu primește niciodată
     * răspuns. Mașina renunță singură după 5 secunde. Spre Mosquitto nu se deschide nimic.
     */
    private static void tineInTunel(String id, Socket masina, Vreme vreme) {
        System.out.printf("[%s] în tunel: țin conexiunea fără răspuns%n", id);
        try {
            masina.setSoTimeout(PAS_TUNEL_MS);
            InputStream in = masina.getInputStream();
            while (vreme.inTunel()) {
                try {
                    if (in.read() < 0) {
                        break; // mașina a renunțat
                    }
                } catch (SocketTimeoutException e) {
                    // Mașina încă așteaptă; verific din nou tunelul.
                }
            }
        } catch (IOException e) {
            // Mașina a închis.
        }
        inchide(masina);
    }

    private static void inchide(Socket s) {
        try {
            s.close();
        } catch (IOException e) {
            // Era deja închis.
        }
    }
}
```

- [ ] **Pasul 2: Scrie `Main`**

```java
package ro.flota.proxy;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Clock;
import ro.flota.proxy.transport.AscultatorMasini;
import ro.flota.proxy.vreme.Registru;

/** Proxy-ul de rețea proastă: mașinile pe 1884, legate mai departe de Mosquitto pe 1883 (spec §3). */
public final class Main {

    static final int PORT_MASINI = 1884;
    static final InetSocketAddress BROKER = new InetSocketAddress("127.0.0.1", 1883);

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        InetAddress local = InetAddress.getByName("127.0.0.1");
        Registru registru = new Registru(Clock.systemUTC());
        ServerSocket masini = deschide(PORT_MASINI, local);
        System.out.printf("Proxy: mașinile pe %d → Mosquitto pe %d%n", PORT_MASINI, BROKER.getPort());
        new AscultatorMasini(masini, BROKER, registru).run();
    }

    static ServerSocket deschide(int port, InetAddress adresa) throws IOException {
        try {
            return new ServerSocket(port, 50, adresa);
        } catch (BindException e) {
            System.err.printf("Portul %d e ocupat. Mai rulează un proxy?%n", port);
            System.exit(1);
            throw e;
        }
    }
}
```

- [ ] **Pasul 3: Mută simulatorul pe 1884**

În `simulator\src\main\resources\simulator.properties`, primele trei rânduri
devin:

```properties
# Din faza 3, mașinile trec prin proxy (1884), care le leagă mai departe de Mosquitto (1883).
broker.host=127.0.0.1
broker.port=1884
```

În `SetariTest.java`, testul `fisierulDinResursePornesteDirectPeBroker` devine:

```java
    @Test
    void fisierulDinResurseTrecePrinProxy() {
        assertEquals(new Setari("127.0.0.1", 1884, 5, 1000), Setari.implicite());
    }
```

- [ ] **Pasul 4: Rulează testele**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu testele simulatorului, ale serverului și ale
proxy-ului trecute.

- [ ] **Pasul 5: Verifică drumul prin proxy**

Terminalele: A cu Mosquitto, S cu serverul, W cu UI-ul. Apoi P, cu proxy-ul:

```powershell
.\mvnw.cmd -q -pl proxy compile exec:java
```

Se așteaptă: `Proxy: mașinile pe 1884 → Mosquitto pe 1883`. Pornește
simulatorul în C. Se așteaptă:
- în P, zece rânduri `[masina-N] legată prin proxy`;
- în A, zece rânduri `New client connected from 127.0.0.1:… as masina-N`, cu
  porturi noi, fiindcă acum proxy-ul e cel care se conectează la Mosquitto;
- pe hartă, mașinile se mișcă exact ca înainte.

- [ ] **Pasul 6: Proxy-ul oprit și pornit la loc**

Ctrl+C în P. Se așteaptă: toate mașinile devin gri pe hartă. Mosquitto vede
socket-urile închise fără `DISCONNECT`, deci trimite Last Will-urile. În C apare
`conexiune pierdută`, apoi încercările de reconectare. Pornește P la loc: în cel
mult 10 secunde, toate mașinile sînt din nou verzi.

- [ ] **Pasul 7: Claude explică.** Analogia: ascultătorul e portarul unei
  clădiri. Pentru fiecare vizitator cheamă un însoțitor, adică un fir, care îl
  întreabă cum îl cheamă și îl duce la etajul broker-ului. În tunel,
  însoțitorul îl ține la recepție fără să-i răspundă. Apoi:
  - de ce `TIMP_CONNECT_MS`: un client care tace nu are voie să țină un fir la
    nesfârșit;
  - de ce se verifică tunelul și după înregistrarea legăturii;
  - de ce Mosquitto trimite Last Will-ul când proxy-ul moare.

- [ ] **Pasul 8: Commit**

```bash
git add proxy/src simulator/src
git commit -m "Proxy: ascultatorul de masini; simulatorul trece prin 1884

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Protocolul text: liniile, `INTARZIERE` și `STARE`

**Cine scrie:** Claude, ca exemplu explicat. E tiparul 3 din spec §9: parsarea
liniei și răspunsul.

**Fișiere:**
- Creează, în `proxy\src\main\java\ro\flota\proxy\ordine\`: `Linie.java`,
  `CititorLinii.java` și `Ordine.java`
- Teste: `proxy\src\test\java\ro\flota\proxy\ordine\CititorLiniiTest.java`, `OrdineTest.java`

**Interfețe:**
- Consumă: `Registru` și `Vreme` (Task 2), `CeasManual` (Task 2, teste).
- Produce:
  - `record Linie(String text, boolean preaLunga)`;
  - `CititorLinii(Reader)`, cu `Linie urmatoarea() throws IOException`, care
    întoarce `null` când conexiunea s-a închis, și constanta `MAXIM = 200`;
  - `Ordine(Registru)`, cu `List<String> executa(String linie)`;
  - constantele `OK`, `ERR_COMANDA`, `ERR_MASINA`, `ERR_PARAMETRI`,
    `ERR_LINIE` și `SFARSIT` (`"."`).

- [ ] **Pasul 1: Scrie testele care pică pentru linii**

`CititorLiniiTest.java`:

```java
package ro.flota.proxy.ordine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class CititorLiniiTest {

    private static CititorLinii din(String text) {
        return new CititorLinii(new StringReader(text));
    }

    @Test
    void citesteLiniiCuLfSiCuCrLf() throws IOException {
        CititorLinii c = din("A\nB\r\nC\n");
        assertEquals(new Linie("A", false), c.urmatoarea());
        assertEquals(new Linie("B", false), c.urmatoarea());
        assertEquals(new Linie("C", false), c.urmatoarea());
        assertNull(c.urmatoarea());
    }

    @Test
    void liniaDe200EsteAcceptataChiarCuCr() throws IOException {
        String l = "x".repeat(200);
        assertEquals(new Linie(l, false), din(l + "\r\n").urmatoarea());
    }

    @Test
    void liniaDe201EstePreaLungaIarUrmatoareaSeCitesteNormal() throws IOException {
        CititorLinii c = din("x".repeat(201) + "\nSTARE\n");
        assertTrue(c.urmatoarea().preaLunga());
        assertEquals(new Linie("STARE", false), c.urmatoarea());
    }

    @Test
    void oLinieUriasaEsteAruncataFaraSaStriceConexiunea() throws IOException {
        CititorLinii c = din("x".repeat(100_000) + "\nSTARE\n");
        Linie lunga = c.urmatoarea();
        assertTrue(lunga.preaLunga());
        assertEquals("", lunga.text());
        assertEquals("STARE", c.urmatoarea().text());
    }

    @Test
    void liniaGoalaEsteOLinieGoala() throws IOException {
        assertEquals(new Linie("", false), din("\n").urmatoarea());
    }

    @Test
    void ultimaLinieFaraLfSeCitesteOricum() throws IOException {
        CititorLinii c = din("STARE");
        assertEquals(new Linie("STARE", false), c.urmatoarea());
        assertNull(c.urmatoarea());
    }

    @Test
    void conexiuneaInchisaDaNull() throws IOException {
        assertNull(din("").urmatoarea());
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `CititorLinii` și
`Linie`.

- [ ] **Pasul 3: Scrie `Linie` și `CititorLinii`**

`Linie.java`:

```java
package ro.flota.proxy.ordine;

/** O linie din protocolul text, fără \r\n. Dacă a depășit 200 de caractere, textul e gol (D24). */
public record Linie(String text, boolean preaLunga) {
}
```

`CititorLinii.java`:

```java
package ro.flota.proxy.ordine;

import java.io.IOException;
import java.io.Reader;

/**
 * Citește linii terminate cu \n; se acceptă și \r\n (spec §5.4). O linie are cel mult 200 de caractere.
 * Ce trece de limită nu se ține în memorie, ci se aruncă până la capătul liniei.
 */
public final class CititorLinii {

    public static final int MAXIM = 200;

    private final Reader in;

    public CititorLinii(Reader in) {
        this.in = in;
    }

    /** Următoarea linie, sau null dacă conexiunea s-a închis înainte de orice caracter. */
    public Linie urmatoarea() throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean depasita = false;
        int c;
        while ((c = in.read()) >= 0 && c != '\n') {
            if (sb.length() <= MAXIM) {
                sb.append((char) c); // cel mult 201 caractere: 200, plus un \r
            } else {
                depasita = true;
            }
        }
        if (c < 0 && sb.length() == 0 && !depasita) {
            return null;
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\r') {
            sb.setLength(sb.length() - 1);
        }
        boolean preaLunga = depasita || sb.length() > MAXIM;
        return new Linie(preaLunga ? "" : sb.toString(), preaLunga);
    }
}
```

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 7 teste din `CititorLiniiTest` trecute.

- [ ] **Pasul 5: Scrie testele care pică pentru ordine**

`OrdineTest.java`:

```java
package ro.flota.proxy.ordine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.flota.proxy.testutil.CeasManual;
import ro.flota.proxy.vreme.Registru;

class OrdineTest {

    private final CeasManual ceas = new CeasManual("2026-12-10T10:00:00Z");
    private final Registru registru = new Registru(ceas);
    private final Ordine ordine = new Ordine(registru);

    @BeforeEach
    void masiniVazute() {
        registru.vreme("masina-3");
        registru.vreme("masina-5");
    }

    /** Răspunsul la o comandă care trebuie să dea un singur rând. */
    private String unu(String linie) {
        List<String> raspuns = ordine.executa(linie);
        assertEquals(1, raspuns.size(), raspuns.toString());
        return raspuns.get(0);
    }

    private String descriere(String masina) {
        return registru.vreme(masina).descriere();
    }

    @Test
    void intarziereaPeOMasina() {
        assertEquals("OK", unu("INTARZIERE masina-3 500 200"));
        assertEquals("INTARZIERE 500 200", descriere("masina-3"));
        assertEquals("NORMAL", descriere("masina-5"));
    }

    @Test
    void variatiaEsteOptionala() {
        assertEquals("OK", unu("INTARZIERE masina-3 500"));
        assertEquals("INTARZIERE 500 0", descriere("masina-3"));
    }

    @Test
    void toateInseamnaToateMasinileVazute() {
        assertEquals("OK", unu("INTARZIERE TOATE 300"));
        assertEquals("INTARZIERE 300 0", descriere("masina-3"));
        assertEquals("INTARZIERE 300 0", descriere("masina-5"));
    }

    @Test
    void literelemariSauMiciNuConteaza() {
        assertEquals("OK", unu("intarziere Masina-3 100"));
        assertEquals("OK", unu("Intarziere toate 100"));
    }

    @Test
    void spatiileInPlusSintIgnorate() {
        assertEquals("OK", unu("  INTARZIERE   masina-3  500  "));
    }

    @Test
    void comandaNecunoscuta() {
        assertEquals(Ordine.ERR_COMANDA, unu("PLOAIE masina-3"));
    }

    @Test
    void masinaNecunoscuta() {
        assertEquals(Ordine.ERR_MASINA, unu("INTARZIERE masina-99 500"));
    }

    @Test
    void parametriGresiti() {
        for (String linie : List.of(
                "INTARZIERE",
                "INTARZIERE masina-3",
                "INTARZIERE masina-3 abc",
                "INTARZIERE masina-3 -1",
                "INTARZIERE masina-3 10001",
                "INTARZIERE masina-3 500 10001",
                "INTARZIERE masina-3 500 200 7")) {
            assertEquals(Ordine.ERR_PARAMETRI, unu(linie), linie);
        }
    }

    @Test
    void limiteleSintAcceptate() {
        assertEquals("OK", unu("INTARZIERE masina-3 0"));
        assertEquals("OK", unu("INTARZIERE masina-3 10000 10000"));
    }

    @Test
    void stareaSeTerminaCuPunct() {
        unu("INTARZIERE masina-5 500 200");
        assertEquals(List.of("masina-3 NORMAL", "masina-5 INTARZIERE 500 200", "."), ordine.executa("STARE"));
    }

    @Test
    void stareaNuPrimesteParametri() {
        assertEquals(Ordine.ERR_PARAMETRI, unu("STARE masina-3"));
    }

    @Test
    void stareaFaraMasiniEsteDoarPunctul() {
        assertEquals(List.of("."), new Ordine(new Registru(ceas)).executa("STARE"));
    }

    @Test
    void toateFaraMasiniEsteOk() {
        assertEquals(List.of("OK"), new Ordine(new Registru(ceas)).executa("INTARZIERE TOATE 100"));
    }
}
```

- [ ] **Pasul 6: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `Ordine`.

- [ ] **Pasul 7: Scrie `Ordine`, cu `INTARZIERE` și `STARE`**

```java
package ro.flota.proxy.ordine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import ro.flota.proxy.vreme.Registru;

/**
 * Protocolul text de pe portul 1885 (spec §5.4): primește o linie și întoarce răspunsul, rând cu rând.
 * Cuvântul de comandă și TOATE nu țin cont de litere mari sau mici.
 */
public final class Ordine {

    public static final String OK = "OK";
    public static final String ERR_COMANDA = "ERR comanda necunoscuta";
    public static final String ERR_MASINA = "ERR masina necunoscuta";
    public static final String ERR_PARAMETRI = "ERR parametri gresiti";
    public static final String ERR_LINIE = "ERR linie prea lunga";
    public static final String SFARSIT = ".";

    private final Registru registru;

    public Ordine(Registru registru) {
        this.registru = registru;
    }

    public List<String> executa(String linie) {
        String[] cuvinte = linie.trim().split("\\s+");
        String comanda = cuvinte[0].toUpperCase(Locale.ROOT);
        String[] p = Arrays.copyOfRange(cuvinte, 1, cuvinte.length);
        try {
            return switch (comanda) {
                case "STARE" -> stare(p);
                case "INTARZIERE" -> intarziere(p);
                default -> List.of(ERR_COMANDA);
            };
        } catch (MasinaNecunoscuta e) {
            return List.of(ERR_MASINA);
        } catch (ParametriGresiti e) {
            return List.of(ERR_PARAMETRI);
        }
    }

    /** INTARZIERE <masina|TOATE> <ms> [<variatie_ms>] */
    private List<String> intarziere(String[] p) {
        numarParametri(p, 2, 3);
        List<String> masini = tinte(p[0]);
        int ms = numar(p[1], 0, 10_000);
        int variatie = p.length == 3 ? numar(p[2], 0, 10_000) : 0;
        masini.forEach(m -> registru.vreme(m).intarziere(ms, variatie));
        return List.of(OK);
    }

    /** STARE: câte un rând pe mașină, apoi un rând care are doar „.”, ca la SMTP. */
    private List<String> stare(String[] p) {
        numarParametri(p, 0, 0);
        List<String> raspuns = new ArrayList<>();
        for (String masina : registru.masini()) {
            raspuns.add(masina + " " + registru.vreme(masina).descriere());
        }
        raspuns.add(SFARSIT);
        return raspuns;
    }

    /** Ținta unei comenzi: o mașină văzută de proxy, sau TOATE, adică toate cele văzute. */
    private List<String> tinte(String tinta) {
        if (tinta.equalsIgnoreCase("TOATE")) {
            return registru.masini();
        }
        String masina = tinta.toLowerCase(Locale.ROOT);
        if (!registru.cunoscuta(masina)) {
            throw new MasinaNecunoscuta();
        }
        return List.of(masina);
    }

    private static void numarParametri(String[] p, int minim, int maxim) {
        if (p.length < minim || p.length > maxim) {
            throw new ParametriGresiti();
        }
    }

    private static int numar(String text, int minim, int maxim) {
        int n;
        try {
            n = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new ParametriGresiti();
        }
        if (n < minim || n > maxim) {
            throw new ParametriGresiti();
        }
        return n;
    }

    /** Excepțiile duc greșeala direct la răspuns, din orice adâncime a parsării. */
    private static final class MasinaNecunoscuta extends RuntimeException {
    }

    private static final class ParametriGresiti extends RuntimeException {
    }
}
```

- [ ] **Pasul 8: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 13 teste din `OrdineTest` trecute.

- [ ] **Pasul 9: Claude explică.** Analogia: protocolul e un formular cu rubrici
  fixe, pe care ghișeul îl citește de la stânga la dreapta: întâi ce vrei, apoi
  pentru cine, apoi cât. La prima rubrică greșită, ghișeul îți spune care și se
  oprește. Apoi:
  - de ce răspunsul la `STARE` se termină cu `.`: cine citește trebuie să știe
    unde se termină un răspuns de mai multe rânduri;
  - ordinea verificărilor: numărul de parametri, ținta, apoi numerele;
  - de ce `CititorLinii` nu ține în memorie o linie uriașă.

- [ ] **Pasul 10: Commit**

```bash
git add proxy/src
git commit -m "Proxy: protocolul text, cu INTARZIERE si STARE

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Celelalte ordine: `BANDA`, `TUNEL`, `GAURA`, `NORMAL`

**Cine scrie:** Tudor, ca repetiție a lui `INTARZIERE`. Claude scrie testele.

**Fișiere:**
- Modifică: `proxy\src\main\java\ro\flota\proxy\ordine\Ordine.java`
- Modifică: `proxy\src\test\java\ro\flota\proxy\ordine\OrdineTest.java`

**Interfețe:**
- Consumă: `Vreme.banda`, `tunel`, `gaura`, `normal` și
  `Registru.inchideLegaturile` (Task 2).
- Produce: comenzile `BANDA <tinta> <kbps>`, `TUNEL <tinta> <secunde>`,
  `GAURA <tinta> <secunde>` și `NORMAL <tinta>`, cu intervalele din
  constrângerile globale.

- [ ] **Pasul 1: Scrie testele care pică**

Adaugă în `OrdineTest` importurile `java.io.Closeable` și
`java.util.concurrent.atomic.AtomicInteger`, apoi testele:

```java
    @Test
    void banda() {
        assertEquals("OK", unu("BANDA masina-3 64"));
        assertEquals("BANDA 64", descriere("masina-3"));
    }

    @Test
    void bandaInAfaraIntervalului() {
        for (String linie : List.of("BANDA masina-3 0", "BANDA masina-3 100001", "BANDA masina-3", "BANDA masina-3 64 1")) {
            assertEquals(Ordine.ERR_PARAMETRI, unu(linie), linie);
        }
    }

    @Test
    void tunelulTineSiInchideLegaturile() {
        AtomicInteger inchise = new AtomicInteger();
        Closeable legatura = inchise::incrementAndGet;
        registru.adaugaLegatura("masina-3", legatura);
        assertEquals("OK", unu("TUNEL masina-3 15"));
        assertEquals(1, inchise.get());
        assertEquals("TUNEL 15", descriere("masina-3"));
        ceas.avanseaza(15_000);
        assertEquals("NORMAL", descriere("masina-3"));
    }

    @Test
    void tunelPeToateInchideToateMasinile() {
        AtomicInteger inchise = new AtomicInteger();
        Closeable a = inchise::incrementAndGet;
        Closeable b = inchise::incrementAndGet;
        registru.adaugaLegatura("masina-3", a);
        registru.adaugaLegatura("masina-5", b);
        assertEquals("OK", unu("TUNEL TOATE 10"));
        assertEquals(2, inchise.get());
    }

    @Test
    void gaura() {
        assertEquals("OK", unu("GAURA masina-5 10"));
        assertEquals("GAURA 10", descriere("masina-5"));
    }

    @Test
    void durataInAfaraIntervalului() {
        for (String linie : List.of("TUNEL masina-3 0", "TUNEL masina-3 601", "TUNEL masina-3 -5", "TUNEL masina-3",
                "GAURA masina-3 0", "GAURA masina-3 601", "GAURA masina-3 1.5")) {
            assertEquals(Ordine.ERR_PARAMETRI, unu(linie), linie);
        }
    }

    @Test
    void normalStergeTot() {
        unu("INTARZIERE masina-3 500 200");
        unu("BANDA masina-3 64");
        unu("GAURA masina-3 30");
        assertEquals("OK", unu("NORMAL masina-3"));
        assertEquals("NORMAL", descriere("masina-3"));
    }

    @Test
    void normalNuPrimesteDurata() {
        assertEquals(Ordine.ERR_PARAMETRI, unu("NORMAL masina-3 5"));
    }

    @Test
    void comenzileNoiCerMasiniCunoscute() {
        for (String linie : List.of("BANDA masina-99 64", "TUNEL masina-99 10", "GAURA masina-99 10", "NORMAL masina-99")) {
            assertEquals(Ordine.ERR_MASINA, unu(linie), linie);
        }
    }
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: 9 teste picate, cu `ERR comanda necunoscuta` în loc de răspunsul
așteptat.

- [ ] **Pasul 3: Tudor scrie cele patru comenzi.** Indicii:
  - fiecare primește câte un `case` în `executa` și câte o metodă, după forma
    lui `intarziere`: întâi numărul de parametri, apoi ținta, apoi numerele;
  - `BANDA` are exact 2 parametri, cu kbps între 1 și 100.000;
  - `TUNEL` și `GAURA` au exact 2 parametri, cu durata între 1 și 600;
  - `TUNEL` mai face un lucru pe lângă `Vreme.tunel`: închide conexiunile
    deschise ale fiecărei mașini-țintă, prin registru;
  - `NORMAL` are exact un parametru, ținta.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

Switch-ul din `executa`:

```java
            return switch (comanda) {
                case "STARE" -> stare(p);
                case "INTARZIERE" -> intarziere(p);
                case "BANDA" -> banda(p);
                case "TUNEL" -> tunel(p);
                case "GAURA" -> gaura(p);
                case "NORMAL" -> normal(p);
                default -> List.of(ERR_COMANDA);
            };
```

Metodele noi, după `intarziere`:

```java
    /** BANDA <masina|TOATE> <kbps> */
    private List<String> banda(String[] p) {
        numarParametri(p, 2, 2);
        List<String> masini = tinte(p[0]);
        int kbps = numar(p[1], 1, 100_000);
        masini.forEach(m -> registru.vreme(m).banda(kbps));
        return List.of(OK);
    }

    /** TUNEL <masina|TOATE> <secunde>: conexiunile deschise se taie pe loc. */
    private List<String> tunel(String[] p) {
        numarParametri(p, 2, 2);
        List<String> masini = tinte(p[0]);
        int secunde = numar(p[1], 1, 600);
        for (String m : masini) {
            registru.vreme(m).tunel(secunde);
            registru.inchideLegaturile(m);
        }
        return List.of(OK);
    }

    /** GAURA <masina|TOATE> <secunde>: conexiunile rămân deschise, dar nu mai trece nimic. */
    private List<String> gaura(String[] p) {
        numarParametri(p, 2, 2);
        List<String> masini = tinte(p[0]);
        int secunde = numar(p[1], 1, 600);
        masini.forEach(m -> registru.vreme(m).gaura(secunde));
        return List.of(OK);
    }

    /** NORMAL <masina|TOATE> */
    private List<String> normal(String[] p) {
        numarParametri(p, 1, 1);
        tinte(p[0]).forEach(m -> registru.vreme(m).normal());
        return List.of(OK);
    }
```

</details>

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 22 de teste din `OrdineTest` trecute.

- [ ] **Pasul 5: Claude revizuiește cele patru comenzi,** fără să rescrie.

- [ ] **Pasul 6: Commit-urile**

```bash
git add proxy/src/test/java/ro/flota/proxy/ordine/OrdineTest.java
git commit -m "Proxy: testele pentru BANDA, TUNEL, GAURA si NORMAL

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git add proxy/src/main/java/ro/flota/proxy/ordine/Ordine.java
git commit -m "Proxy: comenzile BANDA, TUNEL, GAURA si NORMAL"
```

---

### Task 8: Serverul de ordine pe 1885 și clientul în PowerShell

**Cine scrie:** Claude, ca exemplu explicat. E a doua jumătate a tiparului 3:
conexiunea care duce oricâte comenzi și răspunsul scris înapoi.

**Fișiere:**
- Creează: `proxy\src\main\java\ro\flota\proxy\ordine\ServerOrdine.java`
- Modifică: `proxy\src\main\java\ro\flota\proxy\Main.java`
- Creează: `unelte\ordine.ps1`

**Interfețe:**
- Consumă: `CititorLinii`, `Linie`, `Ordine` (Task 6 și 7) și `Main.deschide`
  (Task 5).
- Produce:
  - `ServerOrdine(ServerSocket, Ordine)`, care implementează `Runnable`;
  - portul 1885 deschis de `Main`;
  - scriptul `unelte\ordine.ps1`, cu parametrul opțional `-Ordine`.

- [ ] **Pasul 1: Scrie `ServerOrdine`**

```java
package ro.flota.proxy.ordine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Portul de ordine, 1885 (spec §5.4): management out-of-band, pe alt drum decât cel pe care îl strică.
 * Fiecare client are firul lui, iar conexiunea rămâne deschisă pentru oricâte comenzi.
 */
public final class ServerOrdine implements Runnable {

    private final ServerSocket server;
    private final Ordine ordine;

    public ServerOrdine(ServerSocket server, Ordine ordine) {
        this.server = server;
        this.ordine = ordine;
    }

    @Override
    public void run() {
        while (!server.isClosed()) {
            try {
                Socket client = server.accept();
                Thread fir = new Thread(() -> deserveste(client), "ordine-" + client.getPort());
                fir.setDaemon(true);
                fir.start();
            } catch (IOException e) {
                if (!server.isClosed()) {
                    System.out.println("Ordine: nu pot accepta o conexiune: " + e.getMessage());
                }
            }
        }
    }

    private void deserveste(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII));
             Writer out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.US_ASCII))) {
            CititorLinii linii = new CititorLinii(in);
            Linie linie;
            while ((linie = linii.urmatoarea()) != null) {
                if (!linie.preaLunga() && linie.text().isBlank()) {
                    continue; // un Enter apăsat din greșeală (D24)
                }
                List<String> raspuns = linie.preaLunga() ? List.of(Ordine.ERR_LINIE) : ordine.executa(linie.text());
                for (String rand : raspuns) {
                    out.write(rand);
                    out.write('\n');
                }
                out.flush();
                System.out.printf("Ordine: %s → %s%n", linie.preaLunga() ? "(linie prea lungă)" : linie.text(), raspuns.get(0));
            }
        } catch (IOException e) {
            // Clientul a închis conexiunea.
        }
    }
}
```

- [ ] **Pasul 2: Rescrie `Main`, cu ambele porturi**

```java
package ro.flota.proxy;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Clock;
import ro.flota.proxy.ordine.Ordine;
import ro.flota.proxy.ordine.ServerOrdine;
import ro.flota.proxy.transport.AscultatorMasini;
import ro.flota.proxy.vreme.Registru;

/** Proxy-ul de rețea proastă: mașinile pe 1884, legate de Mosquitto pe 1883; ordinele pe 1885 (spec §3). */
public final class Main {

    static final int PORT_MASINI = 1884;
    static final int PORT_ORDINE = 1885;
    static final InetSocketAddress BROKER = new InetSocketAddress("127.0.0.1", 1883);

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        InetAddress local = InetAddress.getByName("127.0.0.1");
        Registru registru = new Registru(Clock.systemUTC());
        ServerSocket ordine = deschide(PORT_ORDINE, local);
        ServerSocket masini = deschide(PORT_MASINI, local);
        new Thread(new ServerOrdine(ordine, new Ordine(registru)), "ordine").start();
        System.out.printf("Proxy: mașinile pe %d → Mosquitto pe %d; ordinele pe %d%n",
                PORT_MASINI, BROKER.getPort(), PORT_ORDINE);
        new AscultatorMasini(masini, BROKER, registru).run();
    }

    static ServerSocket deschide(int port, InetAddress adresa) throws IOException {
        try {
            return new ServerSocket(port, 50, adresa);
        } catch (BindException e) {
            System.err.printf("Portul %d e ocupat. Mai rulează un proxy?%n", port);
            System.exit(1);
            throw e;
        }
    }
}
```

- [ ] **Pasul 3: Scrie `unelte\ordine.ps1`**

```powershell
# Vorbește cu proxy-ul pe portul 1885 (spec §5.4).
#   powershell -NoProfile -ExecutionPolicy Bypass -File unelte\ordine.ps1
#       mod interactiv; o linie goală închide
#   powershell -NoProfile -ExecutionPolicy Bypass -File unelte\ordine.ps1 -Ordine "TUNEL masina-3 15"
#       o singură ordine, apoi iese
param([string]$Ordine)

$client = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 1885)
$flux = $client.GetStream()
$ascii = [System.Text.Encoding]::ASCII
$scriitor = [System.IO.StreamWriter]::new($flux, $ascii)
$scriitor.NewLine = "`n"
$scriitor.AutoFlush = $true
$cititor = [System.IO.StreamReader]::new($flux, $ascii)

# Trimite o linie și afișează răspunsul. La STARE citește până la rândul cu un singur punct.
function Trimite([string]$linie) {
    $scriitor.WriteLine($linie)
    $raspuns = $cititor.ReadLine()
    Write-Output $raspuns
    if ($linie.Trim() -ieq 'STARE') {
        while ($null -ne $raspuns -and $raspuns -ne '.') {
            $raspuns = $cititor.ReadLine()
            Write-Output $raspuns
        }
    }
}

try {
    if ($Ordine) {
        Trimite $Ordine
        return
    }
    while ($true) {
        $linie = Read-Host 'ordine'
        if ([string]::IsNullOrWhiteSpace($linie)) { break }
        Trimite $linie
    }
} finally {
    $client.Close()
}
```

- [ ] **Pasul 4: Rulează testele**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`.

- [ ] **Pasul 5: Primul tunel**

Terminalele A, P (repornit, cu noul `Main`), S, W și C pornite. Proxy-ul spune
acum `… ordinele pe 1885`. Terminalul O:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File unelte\ordine.ps1
```

La `ordine:` scrie pe rând:

```
STARE
TUNEL masina-3 15
STARE
```

Se așteaptă:
- la primul `STARE`, zece rânduri `masina-N NORMAL`, apoi `.`;
- la `TUNEL`, `OK`. Pe hartă, `masina-3` devine gri într-o secundă. În P apare
  `[masina-3] legătura închisă`, apoi, la câteva secunde, `[masina-3] în tunel:
  țin conexiunea fără răspuns`;
- la al doilea `STARE`, `masina-3 TUNEL` urmat de secundele rămase;
- după vreo 15–25 de secunde, `masina-3` e din nou verde. Mai durează până la
  10 secunde peste cele 15, cât reîncearcă mașina la 5 secunde și cât îi ia să
  renunțe la o încercare făcută spre sfârșitul tunelului.

O linie goală închide scriptul.

- [ ] **Pasul 6: O ordine dată dintr-o singură comandă**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File unelte\ordine.ps1 -Ordine "INTARZIERE masina-3 1000"
powershell -NoProfile -ExecutionPolicy Bypass -File unelte\ordine.ps1 -Ordine "NORMAL masina-3"
```

Se așteaptă: `OK` de fiecare dată. Cât ține întârzierea, `masina-3` se mișcă pe
hartă cu o secundă în urmă.

- [ ] **Pasul 7: Claude explică.** Analogia: portul 1885 e telefonul fix al
  dispecerului cu tehnicianul de la antenă. Merge chiar când stația radio e
  bruiată, fiindcă e alt fir: asta înseamnă management out-of-band. Apoi:
  - de ce un protocol text se poate folosi de mână, cu un script de 40 de
    rânduri;
  - de ce scriptul citește la `STARE` până la `.`, iar la celelalte un singur
    rând;
  - de ce conexiunea rămâne deschisă între comenzi.

- [ ] **Pasul 8: Commit**

```bash
git add proxy/src unelte/ordine.ps1
git commit -m "Proxy: portul de ordine si clientul in PowerShell

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: Scenariul de test și verificarea fazei 3

**Cine face:** Tudor rulează, iar Claude scrie scenariul și README-ul, citește
ieșirile și actualizează starea.

**Fișiere:**
- Creează: `docs\scenariu-test.md`
- Modifică: `README.md`
- Modifică, în AIS-OS: `Facultate\memory\stare.md`

**Interfețe:**
- Produce: `docs\scenariu-test.md`, cu cei șase pași din spec §10, pe care
  fazele 4 și 5 îl rulează din nou și îl completează.

- [ ] **Pasul 1: Scrie `docs\scenariu-test.md`**

````markdown
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
````

- [ ] **Pasul 2: Actualizează `README.md`**

În „Cum pornești”, între pașii 1 și 2 intră proxy-ul, iar numerotarea se
schimbă. Secțiunea devine:

````markdown
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

Se opresc cu Ctrl+C. La „Terminate batch job (Y/N)?” răspunzi Y. Scenariul
complet de test e în `docs\scenariu-test.md`.
````

- [ ] **Pasul 3: Toate testele**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu testele celor trei module trecute.

- [ ] **Pasul 4: Scenariul, pașii pentru faza 3**

Rulează pașii 1, 2, 3, 4 și 6 din `docs\scenariu-test.md`, cu așteptările
„Din faza 3”. Claude citește ieșirile interogărilor.

- [ ] **Pasul 5: Ce scrie `STARE` după scenariu**

În O: `STARE`. Se așteaptă: zece rânduri `NORMAL`, după `NORMAL TOATE` de la
pasul 6, apoi `.`.

- [ ] **Pasul 6: O linie prea lungă și una goală (review, punctul 4)**

În O, lipește o linie de peste 200 de caractere, de exemplu
`STARE` urmat de 300 de `x`. Se așteaptă: `ERR linie prea lunga`. Apoi `STARE`
merge normal, pe aceeași conexiune.

- [ ] **Pasul 7: Un browser pe portul mașinilor (review, punctul 1)**

Deschide http://127.0.0.1:1884 în browser. Se așteaptă:
- în P, `Închid o conexiune de la /127.0.0.1:…: primul pachet nu e CONNECT (0x47)`;
- browserul spune că pagina nu răspunde;
- mașinile merg mai departe.

- [ ] **Pasul 8: Mosquitto oprit (review, punctul 2)**

Ctrl+C în A. Se așteaptă:
- în P, `legătura închisă` pentru fiecare mașină;
- la reîncercări, `[masina-N] broker-ul nu răspunde (Connection refused…); închid
  mașina`, imediat, fără să rămână agățat.

Pornește A la loc. În cel mult 10 secunde, mașinile sînt din nou verzi.

- [ ] **Pasul 9: Commit în proiect**

```bash
git add docs/scenariu-test.md README.md
git commit -m "Scenariul de test si pornirea cu proxy

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Pasul 10: Actualizează starea în AIS-OS**

În `Facultate\memory\stare.md`, la blocul de RC, rândul „Unde am rămas” devine,
cu data zilei în care se termină pasul 8, în formatul AAAA-LL-ZZ:

```markdown
- **Unde am rămas:** faza 3 e gata pe AAAA-LL-ZZ: mașinile trec prin proxy, iar
  `TUNEL`, `GAURA`, `INTARZIERE`, `BANDA` și `NORMAL` merg din
  `unelte\ordine.ps1`. Următorul pas: faza 4, după
  `docs\superpowers\plans\2026-10-08-flota-rc-faza-4.md`.
```

- [ ] **Pasul 11: Commit în AIS-OS, după ce Tudor spune da**

```bash
cd "/c/Users/40747/OneDrive/Documents/AIS-OS"
git add Facultate/memory/stare.md
git commit -m "Starea Facultate: flota-rc, faza 3 gata

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
