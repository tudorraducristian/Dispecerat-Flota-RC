# flota-rc, faza 5: restul și repetiția — plan de implementare

> **Pentru agenți:** SUB-SKILL OBLIGATORIU: superpowers:subagent-driven-development
> (recomandat) sau superpowers:executing-plans, ca planul să fie executat task cu
> task. Pașii folosesc căsuțe (`- [ ]`) pentru urmărire.

**Ținta:** prezentarea din spec §11 merge de la cap la coadă. Pentru asta:
- dispecerul trimite comenzi, iar mașina le confirmă, inclusiv după un tunel;
- serverul dă alarme de zonă, de viteză și de mașină offline prea mult;
- pagina Rețea strică rețeaua din UI și arată latența live;
- alarmele au pagina lor și apar ca notificări.

**Arhitectura:**
- **În simulator:** comenzile vin printr-un filtru global HiveMQ. Motorul le
  execută la următoarea bătaie, prin `ExecutantComenzi`, iar confirmarea trece
  prin buffer.
- **În server:**
  - comenzile se salvează `TRIMISA` și se publică pe QoS 1;
  - confirmarea le trece în `CONFIRMATA` printr-un `UPDATE` condiționat, adică
    idempotent;
  - `ReguliAlarma` și `RegulaOffline` țin starea fiecărei mașini și dau alarma
    doar la schimbare;
  - `Statistici` numără mesajele și latența, iar `ClientProxy` vorbește
    protocolul text pe 1885.
- **În UI:** trei pagini, cu vue-router: Harta, Rețea și Alarme. Graficul de
  latență e un SVG desenat de mână.

**Stack:** cel din fazele 1–4, plus vue-router 5.4.0.

**Spec:** `docs\superpowers\specs\2026-10-08-flota-rc-design.md`:
- faza 5 din §8;
- comenzile și confirmările din §4.2;
- serverul din §6.3–§6.6;
- UI-ul din §7;
- testele din §10 și prezentarea din §11.

Planul se sprijină pe planurile fazelor 0–4, din același dosar. Dacă la execuție
codul real diferă de ce presupune un task, se ajustează task-ul, nu codul
fazelor de dinainte.

## Constrângeri globale

- **Folderul proiectului:** `C:\Users\40747\OneDrive\Documents\Facultate\An 4\Semestrul 1\RC-Proiect\`.
- **Remote-ul:** `origin` e repo-ul privat `ProiectRC` de pe GitHub (D27). După
  commit-urile unui task urmează `git push`.
- **Java 21,** Maven doar prin `.\mvnw.cmd`. Nicio dependență Java nouă.
- **Comenzile** (spec §4.2):
  - `LIMITEAZA_VITEZA`, cu `kmh` între 10 și 130;
  - `INTOARCE_LA_BAZA`, fără parametri;
  - `MESAJ_SOFER`, cu `text` de cel mult 200 de caractere.

  Pleacă pe `flota/<id>/comenzi`, pe QoS 1, iar confirmările vin pe
  `flota/<id>/confirmari`, tot pe QoS 1. `rezultat` e `EXECUTATA` sau
  `RESPINSA`, cu `motiv`.
- **Stările unei comenzi:** `TRIMISA`, `CONFIRMATA` sau `RESPINSA`. O comandă se
  schimbă doar dacă e `TRIMISA` (spec §6.3).
- **Regulile de alarmă** (spec §6.4):
  - `IESIRE_DIN_ZONA`, `VITEZA_PESTE_LIMITA` și `OFFLINE_PRELUNGIT` (peste 60 de
    secunde, verificat la 5 secunde);
  - toate cu `sursa = SERVER` și date la schimbare;
  - la pozițiile recuperate, alarma poartă ora mașinii.
- **Protocolul cu proxy-ul:** o conexiune TCP pe cerere, cu 2 secunde timp
  maxim. Dacă proxy-ul nu răspunde, serverul întoarce 503 (spec §6.5).
- **Pe canalul live** apar și `CONFIRMARE`, `EVENIMENT_RETEA` și `STATISTICI`,
  ultimul o dată pe secundă (spec §6.6).
- **npm:** `vue-router` `5.4.0`, fără `^`. Descărcarea se face după ce Tudor
  spune da.
- **Metoda mixtă (spec §9):**
  - **Claude (exemplu explicat)** scrie codul și îl explică, întâi analogia,
    apoi partea tehnică;
  - **Tudor (repetiție)** scrie codul după test sau după indicii. Claude
    revizuiește fără să rescrie. Soluția de referință stă într-un bloc ascuns.
- **Commit-urile lui Claude** se termină cu
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Ce se taie, în ordine, dacă timpul nu ajunge (spec §8):**
  1. banda îngustă și variația întârzierii, adică două controale din pagina
     Rețea, Task 11;
  2. pagina Alarme, păstrând doar notificările, Task 13;
  3. istoricul pe interval, adică tot Task 15.

## Completări la spec, făcute la scrierea planului

Intră în spec ca D26, în același commit cu planul:

- **D26.** Detaliile fazei 5, pe care spec-ul nu le hotăra:
  - **Comenzile:**
    - mașina le execută la următoarea bătaie a motorului, pe firul lui, iar
      confirmarea trece prin buffer;
    - `INTOARCE_LA_BAZA` merge înapoi pe traseu, cu distanța care scade, până
      la primul punct. Oprirea la bază nu e frânare bruscă;
    - o comandă cerută cât broker-ul e deconectat de server primește 503 și nu
      se salvează;
    - serverul verifică parametrii înainte să trimită. Mașina îi verifică încă
      o dată, pentru comenzile venite pe alt drum, de exemplu cu `mosquitto_pub`.
  - **Regulile:**
    - `IESIRE_DIN_ZONA` cere întâi o poziție în zonă: o mașină pornită afară nu
      dă alarmă până nu intră și iese;
    - `VITEZA_PESTE_LIMITA` se dă la prima poziție peste limită și se rearmează
      sub limită. Exact la limită nu schimbă nimic;
    - `OFFLINE_PRELUNGIT` ține starea doar în memorie, deci după o repornire a
      serverului o mașină încă offline poate primi alarma încă o dată.
  - **Statisticile:**
    - mesajele pe secundă se socotesc pe ultimele 10 secunde;
    - latența e media ultimelor 10 mesaje live; cele recuperate nu intră;
    - graficul arată media flotei pe ultimele 60 de secunde.
  - **Pagina Rețea:**
    - `POST /api/retea/vreme` întoarce 200 și când proxy-ul răspunde `ERR`,
      fiindcă `ERR` e un răspuns al protocolului;
    - 400 vine doar pentru o cerere care nu se poate traduce într-o linie;
    - UI-ul citește `STARE` la 3 secunde și după fiecare eveniment de rețea, ca
      să coloreze cu portocaliu mașinile cu vreme setată;
    - `PATCH /api/alarme/{id}` n-are corp: doar marchează alarma ca văzută.

## Unde trebuie să fie atent review-ul

Cinci situații pe care un om le întâlnește repede, cu comportamentul așteptat:

1. **Proxy-ul e oprit.** Pagina Rețea spune „proxy indisponibil”, iar cererile
   întorc 503 în cel mult 2 secunde. Harta merge mai departe. Testele sînt în
   Task 9: `proxyOpritDa503` și `proxyCareTaceDa503InDouaSecunde`.
2. **Aceeași confirmare vine de două ori,** ceea ce pe QoS 1 se poate întâmpla.
   Comanda se schimbă și se anunță o singură dată. Testul e în Task 5:
   `aceeasiConfirmareDeDouaOriSeAnuntaOData`. Se verifică și în Task 16, pasul 5.
3. **O comandă are parametri greșiți:** `kmh` 200, un text de 300 de caractere
   sau un tip necunoscut. Serverul o refuză cu 400. Dacă ajunge totuși la
   mașină, pe alt drum, mașina o respinge cu motivul. Testele sînt în Task 4:
   `ValidatorComenziTest`, și în Task 1: `ExecutantComenziTest`.
4. **O mașină pornește în afara zonei ei.** Nu primește alarmă până nu intră și
   iese. Testul e în Task 6:
   `oMasinaPornitaAfaraNuDaAlarmaPanaNuIntraSiIese`.
5. **O poziție e exact pe marginea zonei.** Se socotește înăuntru. Testul e în
   Task 6: `margineaEsteInauntru`.

---

## Structura fișierelor

```
RC-Proiect\
├── simulator\src\main\java\ro\flota\simulator\
│   ├── mesaje\TipMesaj.java, Mesaje.java, Confirmare.java (nou)
│   ├── simulare\Viteza.java                         + limiteaza
│   └── masina\ExecutantComenzi.java (nou), Motor.java, Conexiune.java,
│       ConexiuneHiveMq.java, Masina.java
├── server\src\main\
│   ├── resources\application.properties             + flota.proxy.*
│   └── java\ro\flota\server\
│       ├── ServerApplication.java                   + @EnableScheduling
│       ├── flota\Ore.java (public), Alarme.java, Masini.java, Dispecer.java
│       ├── mqtt\MesajPrimit.java, Decodor.java, ClientMqtt.java
│       ├── comenzi\ (nou)  CerereComanda, ComandaDto, ValidatorComenzi, Comenzi, ServiciuComenzi
│       ├── reguli\ (nou)   PunctInPoligon, ReguliAlarma, RegulaOffline, VerificareOffline
│       ├── statistici\ (nou) FereastraStatistici, StatisticaDto, Statistici, TrimitereStatistici
│       ├── retea\ (nou)    CerereVreme, LiniiVreme, VremeMasinaDto, EvenimentDto,
│       │                   EvenimenteRetea, ClientProxy
│       └── api\ ServiciuIndisponibil (nou), EroriApi, ComenziController (nou),
│                AlarmeController (nou), ReteaController (nou), StatisticiController (nou)
├── web\src\
│   ├── main.js, App.vue, router.js (nou), api.js, live.js
│   ├── pagini\PaginaHarta.vue, PaginaRetea.vue (nou), PaginaAlarme.vue (nou)
│   └── components\HartaFlotei.vue, PanouMasina.vue, PanouComenzi.vue (nou),
│       GraficLatenta.vue (nou), AlegereInterval.vue (nou)
└── docs\scenariu-prezentare.md (nou)
```

---

### Task 1: Comenzile în simulator: confirmarea și executantul

**Cine scrie:**
- **Confirmarea, `LIMITEAZA_VITEZA` și `INTOARCE_LA_BAZA`:** Claude, ca exemplu
  explicat.
- **`MESAJ_SOFER`:** Tudor, ca repetiție: altă comandă, cu altă regulă de
  verificare.

**Fișiere:**
- Modifică: `simulator\src\main\java\ro\flota\simulator\mesaje\TipMesaj.java`, `Mesaje.java`
- Creează: `simulator\src\main\java\ro\flota\simulator\mesaje\Confirmare.java`
- Modifică: `simulator\src\main\java\ro\flota\simulator\simulare\Viteza.java`
- Creează: `simulator\src\main\java\ro\flota\simulator\masina\ExecutantComenzi.java`
- Teste: `simulator\src\test\java\ro\flota\simulator\simulare\VitezaTest.java`,
  `simulator\src\test\java\ro\flota\simulator\masina\ExecutantComenziTest.java`

**Interfețe:**
- Consumă: `Json.MAPPER`, `Mesaj`, `Mesaje.topic` (faza 1) și `Viteza` (faza 1,
  Task 8).
- Produce:
  - `TipMesaj.CONFIRMARE`;
  - `record Confirmare(long comandaId, String ora, String rezultat, String motiv)`,
    cu `comanda_id` în JSON, fără `motiv` când e `null`, plus
    `static executata(long, String)` și `static respinsa(long, String, String)`;
  - `Mesaje.confirmare(String idMasina, Confirmare)`, pe QoS 1 și fără reținere;
  - `Viteza.limiteaza(double kmh)`;
  - `ExecutantComenzi(String idMasina, Viteza viteza, Runnable intoarceLaBaza, Consumer<String> sofer)`,
    cu `Optional<Mesaj> executa(byte[] payload, String ora)`. Întoarce gol dacă
    mesajul n-are măcar un `id`.

- [ ] **Pasul 1: Scrie testul care pică pentru limita de viteză**

În `VitezaTest.java`, la sfârșitul clasei:

```java
    @Test
    void limitaTaieTintaDeAcumSiPeCeleViitoare() {
        Viteza v = new Viteza(new AleatorScriptat(0.99, 0.05, 0.05), FARA_FRANARE, 0); // ținta 90, apoi tot 90
        v.tick();
        v.limiteaza(50);
        assertEquals(50.0, v.tinta(), 1e-9);
        for (int i = 0; i < 30; i++) {
            v.tick(); // la a 31-a secundă vine o țintă nouă
        }
        assertEquals(50.0, v.tinta(), 1e-9);
    }
```

- [ ] **Pasul 2: Scrie testele care pică pentru executant**

`ExecutantComenziTest.java`:

```java
package ro.flota.simulator.masina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.Json;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.TipMesaj;
import ro.flota.simulator.simulare.Viteza;
import ro.flota.simulator.testutil.AleatorScriptat;
import tools.jackson.databind.JsonNode;

class ExecutantComenziTest {

    private static final String ORA = "2026-12-10T10:15:04.002Z";

    private final Viteza viteza = new Viteza(new AleatorScriptat(0.99, 0.05), new AleatorScriptat(0.99), 0);
    private final AtomicInteger intoarceri = new AtomicInteger();
    private final List<String> spuseSoferului = new ArrayList<>();
    private final ExecutantComenzi executant =
            new ExecutantComenzi("masina-3", viteza, intoarceri::incrementAndGet, spuseSoferului::add);

    private static byte[] octeti(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    /** Execută comanda și întoarce confirmarea, după ce verifică unde și cum pleacă. */
    private JsonNode confirmare(String comanda) {
        Mesaj m = executant.executa(octeti(comanda), ORA).orElseThrow();
        assertEquals(TipMesaj.CONFIRMARE, m.tip());
        assertEquals("flota/masina-3/confirmari", m.topic());
        assertEquals(1, m.qos());
        assertFalse(m.retain());
        return Json.MAPPER.readTree(m.payload());
    }

    @Test
    void limiteazaVitezaTaieTinta() {
        viteza.tick(); // ținta devine 90
        JsonNode c = confirmare("{\"id\": 57, \"tip\": \"LIMITEAZA_VITEZA\", \"parametri\": {\"kmh\": 50}}");
        assertEquals(57, c.get("comanda_id").asLong());
        assertEquals(ORA, c.get("ora").asString());
        assertEquals("EXECUTATA", c.get("rezultat").asString());
        assertFalse(c.has("motiv"));
        assertEquals(50.0, viteza.tinta(), 1e-9);
    }

    @Test
    void kmhInAfaraIntervaluluiEsteRespins() {
        for (String kmh : List.of("9", "131", "\"50\"", "50.5")) {
            JsonNode c = confirmare("{\"id\": 58, \"tip\": \"LIMITEAZA_VITEZA\", \"parametri\": {\"kmh\": " + kmh + "}}");
            assertEquals("RESPINSA", c.get("rezultat").asString(), kmh);
            assertEquals("kmh în afara intervalului", c.get("motiv").asString(), kmh);
        }
    }

    @Test
    void intoarceLaBaza() {
        JsonNode c = confirmare("{\"id\": 59, \"tip\": \"INTOARCE_LA_BAZA\", \"parametri\": {}}");
        assertEquals("EXECUTATA", c.get("rezultat").asString());
        assertEquals(1, intoarceri.get());
    }

    @Test
    void tipNecunoscutEsteRespins() {
        JsonNode c = confirmare("{\"id\": 60, \"tip\": \"ZBOARA\", \"parametri\": {}}");
        assertEquals("RESPINSA", c.get("rezultat").asString());
        assertTrue(c.get("motiv").asString().contains("ZBOARA"));
    }

    @Test
    void comandaFaraIdNuPrimesteConfirmare() {
        assertTrue(executant.executa(octeti("nu e json"), ORA).isEmpty());
        assertTrue(executant.executa(octeti("{\"tip\": \"INTOARCE_LA_BAZA\"}"), ORA).isEmpty());
        assertEquals(0, intoarceri.get());
    }
}
```

- [ ] **Pasul 3: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `limiteaza` și
`ExecutantComenzi`.

- [ ] **Pasul 4: Schimbă `TipMesaj` și `Mesaje`, scrie `Confirmare`**

`TipMesaj.java` devine:

```java
package ro.flota.simulator.mesaje;

/** Felul unui mesaj. Bufferul aruncă doar TELEMETRIE. */
public enum TipMesaj {
    TELEMETRIE,
    ALARMA,
    STARE,
    CONFIRMARE
}
```

`Confirmare.java`:

```java
package ro.flota.simulator.mesaje;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Ce trimite mașina pe flota/<id>/confirmari după o comandă (spec §4.2). motiv apare doar la RESPINSA. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Confirmare(@JsonProperty("comanda_id") long comandaId, String ora, String rezultat, String motiv) {

    public static Confirmare executata(long comandaId, String ora) {
        return new Confirmare(comandaId, ora, "EXECUTATA", null);
    }

    public static Confirmare respinsa(long comandaId, String ora, String motiv) {
        return new Confirmare(comandaId, ora, "RESPINSA", motiv);
    }
}
```

În `Mesaje.java`, după `alarma`:

```java
    /** QoS 1: QoS spune doar că broker-ul a primit comanda; confirmarea spune că mașina a executat-o. */
    public static Mesaj confirmare(String idMasina, Confirmare c) {
        return new Mesaj(TipMesaj.CONFIRMARE, topic(idMasina, "confirmari"), Json.MAPPER.writeValueAsBytes(c), 1, false);
    }
```

- [ ] **Pasul 5: Adaugă limita în `Viteza`**

Câmpul nou, după `tinta`:

```java
    private double limita = Double.POSITIVE_INFINITY;
```

În `tick()`, imediat după blocul `if (secundePanaLaTintaNoua == 0) { … }`:

```java
        tinta = Math.min(tinta, limita);
```

Metoda nouă, după `tick()`:

```java
    /** LIMITEAZA_VITEZA (spec §4.2): ținta de acum și toate cele viitoare nu mai trec de limită. */
    public void limiteaza(double kmh) {
        limita = kmh;
        tinta = Math.min(tinta, limita);
    }
```

- [ ] **Pasul 6: Scrie `ExecutantComenzi`**

```java
package ro.flota.simulator.masina;

import java.util.Optional;
import java.util.function.Consumer;
import ro.flota.simulator.Json;
import ro.flota.simulator.mesaje.Confirmare;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;
import ro.flota.simulator.simulare.Viteza;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/**
 * Comenzile venite de la dispecer (spec §4.2): le verifică, le execută și pregătește confirmarea.
 * Rulează pe firul motorului, deci nu se bate cu el pe viteză sau pe poziție.
 */
public final class ExecutantComenzi {

    static final int KMH_MINIM = 10;
    static final int KMH_MAXIM = 130;
    static final int TEXT_MAXIM = 200;

    private final String idMasina;
    private final Viteza viteza;
    private final Runnable intoarceLaBaza;
    private final Consumer<String> sofer;

    public ExecutantComenzi(String idMasina, Viteza viteza, Runnable intoarceLaBaza, Consumer<String> sofer) {
        this.idMasina = idMasina;
        this.viteza = viteza;
        this.intoarceLaBaza = intoarceLaBaza;
        this.sofer = sofer;
    }

    /** Execută comanda și întoarce confirmarea de trimis. Gol dacă mesajul n-are nici măcar un id. */
    public Optional<Mesaj> executa(byte[] payload, String ora) {
        JsonNode j;
        try {
            j = Json.MAPPER.readTree(payload);
        } catch (JacksonException e) {
            System.out.printf("[%s] comandă care nu e JSON; o ignor%n", idMasina);
            return Optional.empty();
        }
        JsonNode idNod = j == null ? null : j.get("id");
        if (idNod == null || !idNod.isIntegralNumber()) {
            System.out.printf("[%s] comandă fără id; o ignor%n", idMasina);
            return Optional.empty();
        }
        long id = idNod.asLong();
        JsonNode tipNod = j.get("tip");
        String tip = tipNod != null && tipNod.isString() ? tipNod.asString() : "";
        JsonNode parametri = j.path("parametri");
        String motiv = switch (tip) {
            case "LIMITEAZA_VITEZA" -> limiteazaViteza(parametri);
            case "INTOARCE_LA_BAZA" -> {
                intoarceLaBaza.run();
                yield null;
            }
            default -> "tip necunoscut: " + tip;
        };
        Confirmare c = motiv == null ? Confirmare.executata(id, ora) : Confirmare.respinsa(id, ora, motiv);
        System.out.printf("[%s] comanda %d %s: %s%s%n", idMasina, id, tip, c.rezultat(), motiv == null ? "" : " (" + motiv + ")");
        return Optional.of(Mesaje.confirmare(idMasina, c));
    }

    /** Întoarce motivul respingerii, sau null dacă a executat-o. */
    private String limiteazaViteza(JsonNode p) {
        JsonNode kmh = p.get("kmh");
        if (kmh == null || !kmh.isIntegralNumber() || kmh.asInt() < KMH_MINIM || kmh.asInt() > KMH_MAXIM) {
            return "kmh în afara intervalului";
        }
        viteza.limiteaza(kmh.asInt());
        return null;
    }
}
```

- [ ] **Pasul 7: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 5 teste din `ExecutantComenziTest` și noul
test din `VitezaTest` trecute.

- [ ] **Pasul 8: Commit-ul lui Claude**

```bash
git add simulator/src
git commit -m "Simulator: confirmarea si executantul de comenzi

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Pasul 9: Claude explică.** Analogia: șoferul primește un bilet de la
  dispecer. Îl citește, face ce scrie dacă are sens și trimite înapoi „am făcut”
  sau „nu pot, uite de ce”. Un bilet fără număr nu poate primi răspuns, fiindcă
  dispecerul n-ar ști la ce se referă. Apoi:
  - diferența dintre „livrat”, adică QoS 1, și „executat”, adică confirmarea;
  - de ce mașina verifică parametrii chiar dacă serverul i-a verificat deja;
  - `@JsonInclude(NON_NULL)`.

- [ ] **Pasul 10: Scrie testele care pică pentru `MESAJ_SOFER`**

Adaugă în `ExecutantComenziTest`:

```java
    @Test
    void mesajulAjungeLaSofer() {
        JsonNode c = confirmare("{\"id\": 61, \"tip\": \"MESAJ_SOFER\", \"parametri\": {\"text\": \"Treci pe la bază\"}}");
        assertEquals("EXECUTATA", c.get("rezultat").asString());
        assertEquals(List.of("Treci pe la bază"), spuseSoferului);
    }

    @Test
    void mesajulPreaLungEsteRespins() {
        String lung = "x".repeat(201);
        JsonNode c = confirmare("{\"id\": 62, \"tip\": \"MESAJ_SOFER\", \"parametri\": {\"text\": \"" + lung + "\"}}");
        assertEquals("RESPINSA", c.get("rezultat").asString());
        assertTrue(c.get("motiv").asString().contains("200"));
        assertTrue(spuseSoferului.isEmpty());
    }

    @Test
    void mesajulGolEsteRespins() {
        JsonNode c = confirmare("{\"id\": 63, \"tip\": \"MESAJ_SOFER\", \"parametri\": {\"text\": \"  \"}}");
        assertEquals("RESPINSA", c.get("rezultat").asString());
        assertTrue(spuseSoferului.isEmpty());
    }
```

- [ ] **Pasul 11: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: 3 teste picate. `MESAJ_SOFER` e încă „tip necunoscut”.

- [ ] **Pasul 12: Tudor scrie `MESAJ_SOFER`.** Indicii:
  - un `case` nou în switch și o metodă, după forma lui `limiteazaViteza`;
  - `text` trebuie să fie șir, să nu fie gol sau doar spații și să aibă cel
    mult `TEXT_MAXIM` caractere;
  - motivele: `text lipsă` și `text mai lung de 200 de caractere`;
  - textul bun ajunge la `sofer.accept(text)`.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

În switch, înainte de `default`:

```java
            case "MESAJ_SOFER" -> mesajSofer(parametri);
```

Metoda, după `limiteazaViteza`:

```java
    private String mesajSofer(JsonNode p) {
        JsonNode text = p.get("text");
        if (text == null || !text.isString() || text.asString().isBlank()) {
            return "text lipsă";
        }
        if (text.asString().length() > TEXT_MAXIM) {
            return "text mai lung de " + TEXT_MAXIM + " de caractere";
        }
        sofer.accept(text.asString());
        return null;
    }
```

</details>

- [ ] **Pasul 13: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 8 teste din `ExecutantComenziTest` trecute.

- [ ] **Pasul 14: Claude revizuiește,** fără să rescrie.

- [ ] **Pasul 15: Commit-ul lui Tudor**

```bash
git add simulator/src
git commit -m "Simulator: mesajul pentru sofer"
```

---

### Task 2: Motorul execută comenzile și se întoarce la bază

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „un fir primește, alt fir
execută”: comanda vine pe firul clientului MQTT, dar o execută motorul, pe firul
lui.

**Fișiere:**
- Modifică: `simulator\src\main\java\ro\flota\simulator\masina\Motor.java`
- Modifică: `simulator\src\test\java\ro\flota\simulator\masina\MotorTest.java`

**Interfețe:**
- Consumă: `ExecutantComenzi` (Task 1) și `Buffer` (faza 4).
- Produce: `Motor.primesteComanda(byte[] payload)`, sigur de chemat din orice fir.

- [ ] **Pasul 1: Scrie testele care pică**

În `MotorTest.java`, importuri noi: `java.nio.charset.StandardCharsets`. La
sfârșitul clasei:

```java
    private static byte[] comanda(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private Mesaj ultimaTelemetrie() {
        return coada.continut().reversed().stream()
                .filter(m -> m.tip() == TipMesaj.TELEMETRIE)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void comandaSeExecutaLaUrmatoareaBatereIarConfirmareaIntraInCoada() {
        Motor motor = motor(0, spre50(), normala(85), true);
        motor.primesteComanda(comanda("{\"id\": 57, \"tip\": \"LIMITEAZA_VITEZA\", \"parametri\": {\"kmh\": 30}}"));
        assertEquals(0, coada.marime(), "comanda nu se execută pe firul care o primește");
        motor.tick();
        Mesaj primul = coada.continut().getFirst();
        assertEquals(TipMesaj.CONFIRMARE, primul.tip());
        assertEquals(57, json(primul).get("comanda_id").asLong());
    }

    @Test
    void intoarcereaMergeInapoiPanaLaStartSiStaPeLoc() {
        Motor motor = motor(0.1, spre50(), normala(85), true); // 37,7 m după start
        motor.tick();
        motor.primesteComanda(comanda("{\"id\": 1, \"tip\": \"INTOARCE_LA_BAZA\", \"parametri\": {}}"));
        for (int i = 0; i < 60; i++) {
            motor.tick();
        }
        JsonNode ultima = json(ultimaTelemetrie());
        assertEquals(46.0, ultima.get("lat").asDouble(), 1e-9);
        assertEquals(23.0, ultima.get("lon").asDouble(), 1e-9);
        assertEquals(0.0, ultima.get("viteza").asDouble(), 1e-9);
    }

    @Test
    void sosireaLaBazaNuEsteFranareBrusca() {
        Motor motor = motor(0.1, spre50(), normala(85), true);
        motor.tick();
        motor.primesteComanda(comanda("{\"id\": 1, \"tip\": \"INTOARCE_LA_BAZA\", \"parametri\": {}}"));
        for (int i = 0; i < 60; i++) {
            motor.tick();
        }
        assertTrue(coada.continut().stream().noneMatch(m -> m.tip() == TipMesaj.ALARMA));
    }
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru
`primesteComanda`.

- [ ] **Pasul 3: Schimbă `Motor`**

Importuri noi: `java.util.Queue` și `java.util.concurrent.ConcurrentLinkedQueue`.

Câmpurile noi, după `seq`:

```java
    /** Comenzile vin pe firul clientului MQTT și așteaptă aici următoarea bătaie a motorului. */
    private final Queue<byte[]> comenziPrimite = new ConcurrentLinkedQueue<>();
    private final ExecutantComenzi executant;
    private boolean spreBaza = false;
    private boolean laBaza = false;
    private double panaLaBaza;
```

La sfârșitul constructorului:

```java
        this.executant = new ExecutantComenzi(id, viteza, this::intoarceLaBaza,
                text -> System.out.printf("[%s] mesaj pentru șofer: %s%n", id, text));
```

`tick()` devine:

```java
    public void tick() {
        String ora = Ore.format(ceas.instant());
        executaComenzile(ora);
        double v = laBaza ? 0 : viteza.tick();
        double t = temperatura.tick(v);
        double pas = v / 3.6 * secundePeTick;
        if (spreBaza) {
            panaLaBaza -= pas;
            if (panaLaBaza <= 0) {
                distanta = 0;
                spreBaza = false;
                laBaza = true;
                v = 0;
                System.out.printf("[%s] a ajuns la bază%n", id);
            } else {
                distanta = panaLaBaza;
            }
        } else if (!laBaza) {
            distanta = traseu.normalizeaza(distanta + pas);
        }
        secundeScurse += secundePeTick;
        Punct p = traseu.punctLa(distanta);
        seq++;
        coada.adauga(Mesaje.telemetrie(id, new Telemetrie(seq, ora,
                rotunjit(p.lat(), 5), rotunjit(p.lon(), 5), rotunjit(v, 1), rotunjit(t, 1),
                baterie(), !conectat.getAsBoolean())));
        if (supraincalzire.verifica(t)) {
            coada.adauga(Mesaje.alarma(id, new Alarma(TipAlarma.SUPRAINCALZIRE, ora, rotunjit(t, 1))));
        }
        // Oprirea la bază nu e o frânare bruscă (D26).
        if (!laBaza && franare.verifica(v)) {
            coada.adauga(Mesaje.alarma(id, new Alarma(TipAlarma.FRANARE_BRUSCA, ora,
                    rotunjit(franare.ultimaScadere(), 1))));
        }
    }

    /** Vine din orice fir, de obicei de la clientul MQTT. Se execută la următoarea bătaie. */
    public void primesteComanda(byte[] payload) {
        comenziPrimite.add(payload);
    }

    private void executaComenzile(String ora) {
        byte[] comanda;
        while ((comanda = comenziPrimite.poll()) != null) {
            executant.executa(comanda, ora).ifPresent(coada::adauga);
        }
    }

    /** INTOARCE_LA_BAZA (spec §4.2): înapoi pe traseu, cu distanța care scade până la primul punct (D26). */
    private void intoarceLaBaza() {
        if (laBaza) {
            return;
        }
        spreBaza = true;
        panaLaBaza = distanta;
    }
```

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`. Testele vechi din `MotorTest` trec la fel ca
înainte.

- [ ] **Pasul 5: Claude explică.** Analogia: biletele de la dispecer se pun
  într-o cutie lângă volan. Șoferul se uită în cutie o dată pe secundă, înainte
  să facă orice altceva. Nimeni nu-i smulge volanul din mână. Apoi:
  - `ConcurrentLinkedQueue`: o cutie în care pot pune mai multe fire deodată;
  - de ce motorul e singurul care schimbă viteza și poziția, deci n-are nevoie
    de `synchronized`;
  - cum merge întoarcerea pe traseu.

- [ ] **Pasul 6: Commit**

```bash
git add simulator/src
git commit -m "Simulator: motorul executa comenzile si se intoarce la baza

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Comenzile pe MQTT, în simulator

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „abonarea la propriul
topic, într-o sesiune persistentă”.

**Fișiere:**
- Modifică, în `simulator\src\main\java\ro\flota\simulator\masina\`:
  `Conexiune.java`, `ConexiuneHiveMq.java` și `Masina.java`
- Modifică: `simulator\src\test\java\ro\flota\simulator\masina\ConexiuneFalsa.java`, `MasinaTest.java`

**Interfețe:**
- Consumă: `Motor.primesteComanda` (Task 2).
- Produce:
  - `Conexiune.laComanda(Consumer<byte[]> ascultator)`;
  - în `ConexiuneFalsa`, pentru teste: `livreazaComanda(String json)`.

- [ ] **Pasul 1: Scrie testul care pică**

În `MasinaTest.java`, importul nou e `ro.flota.simulator.mesaje.Mesaj`. La
sfârșitul clasei:

```java
    @Test
    void oComandaPrimitaPrimesteConfirmarePeMqtt() throws Exception {
        ConexiuneFalsa conexiune = new ConexiuneFalsa();
        Masina masina = new Masina(def, RAPID, conexiune, Clock.systemUTC(), new Random(1));
        masina.porneste();
        conexiune.livreazaComanda("{\"id\": 7, \"tip\": \"LIMITEAZA_VITEZA\", \"parametri\": {\"kmh\": 40}}");
        long limita = System.currentTimeMillis() + 3_000;
        while (conexiune.publicate.stream().noneMatch(m -> m.tip() == TipMesaj.CONFIRMARE)
                && System.currentTimeMillis() < limita) {
            Thread.sleep(20);
        }
        masina.opreste();
        Mesaj confirmare = conexiune.publicate.stream()
                .filter(m -> m.tip() == TipMesaj.CONFIRMARE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("nicio confirmare în 3 secunde"));
        assertEquals("flota/masina-1/confirmari", confirmare.topic());
    }
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `livreazaComanda`.

- [ ] **Pasul 3: Schimbă `Conexiune` și `ConexiuneFalsa`**

În `Conexiune.java`, importul nou e `java.util.function.Consumer`, iar metoda
nouă:

```java
    /** Cine primește comenzile venite pe flota/<id>/comenzi. Se setează o dată, înainte de prima conectare. */
    void laComanda(Consumer<byte[]> ascultator);
```

În `ConexiuneFalsa.java`, importurile noi sînt `java.nio.charset.StandardCharsets`
și `java.util.function.Consumer`, plus:

```java
    private volatile Consumer<byte[]> laComanda = payload -> { };

    @Override
    public void laComanda(Consumer<byte[]> ascultator) {
        laComanda = ascultator;
    }

    /** În teste: ca și cum broker-ul ar livra o comandă. */
    void livreazaComanda(String json) {
        laComanda.accept(json.getBytes(StandardCharsets.UTF_8));
    }
```

- [ ] **Pasul 4: Schimbă `ConexiuneHiveMq`**

Importuri noi: `com.hivemq.client.mqtt.MqttGlobalPublishFilter` și
`java.util.function.Consumer`. Câmpul nou:

```java
    private volatile Consumer<byte[]> laComanda = payload -> { };
```

La sfârșitul constructorului, după `.buildBlocking();`:

```java
        // Comenzile păstrate de broker cât mașina a fost offline vin imediat după CONNACK, înainte de orice
        // abonare nouă; filtrul global le prinde (ca la server, D22).
        client.toAsync().publishes(MqttGlobalPublishFilter.ALL, p -> laComanda.accept(p.getPayloadAsBytes()));
```

În `conecteaza()`, în `try`, după `.send();` de la `connectWith()`:

```java
            // La fiecare conectare: un broker repornit fără persistență a uitat abonarea.
            client.subscribeWith()
                    .addSubscription().topicFilter(Mesaje.topic(id, "comenzi")).qos(MqttQos.AT_LEAST_ONCE).applySubscription()
                    .send();
```

Metoda nouă:

```java
    @Override
    public void laComanda(Consumer<byte[]> ascultator) {
        this.laComanda = ascultator;
    }
```

- [ ] **Pasul 5: Leagă comenzile de motor în `Masina`**

La sfârșitul constructorului lui `Masina`:

```java
        conexiune.laComanda(motor::primesteComanda);
```

- [ ] **Pasul 6: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`.

- [ ] **Pasul 7: Verifică o comandă trimisă de mână**

A, P, S și C pornite. PowerShell 5.1 strică ghilimelele dintr-un argument JSON,
deci comanda stă într-un fișier `docs\mesaje-test\comanda.json`:

```json
{"id": 9001, "tip": "MESAJ_SOFER", "parametri": {"text": "Salut de la mosquitto_pub"}}
```

```powershell
& "C:\Program Files\mosquitto\mosquitto_pub.exe" -h 127.0.0.1 -t flota/masina-3/comenzi -q 1 -f docs\mesaje-test\comanda.json
```

Se așteaptă:
- în C, `[masina-3] mesaj pentru șofer: Salut de la mosquitto_pub`;
- `[masina-3] comanda 9001 MESAJ_SOFER: EXECUTATA`;
- confirmarea pleacă pe `flota/masina-3/confirmari`. Serverul o ignoră, fiindcă
  nu e încă abonat, dar se vede cu
  `mosquitto_sub -h 127.0.0.1 -t "flota/+/confirmari" -v`, pornit înainte.

- [ ] **Pasul 8: Claude explică.** Analogia: fiecare mașină are o căsuță
  poștală la oficiul poștal, adică sesiunea persistentă. Ce sosește cât lipsește
  rămâne în căsuță și îi e dat la prima vizită, chiar înainte să apuce să ceară.
  Asta prinde filtrul global. Apoi:
  - `cleanSession(false)` plus QoS 1 înseamnă „păstrează-mi comenzile”;
  - de ce abonarea se repetă la fiecare conectare.

- [ ] **Pasul 9: Commit**

```bash
git add simulator/src docs/mesaje-test/comanda.json
git commit -m "Simulator: comenzile vin pe MQTT, in sesiunea persistenta

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Trimiterea comenzilor din server

**Cine scrie:**
- **Validarea, salvarea, publicarea și `POST`:** Claude, ca exemplu explicat.
- **`MESAJ_SOFER` în validare și istoricul comenzilor, adică endpoint-ul 7:**
  Tudor, ca repetiție.

**Fișiere:**
- Creează: `server\src\main\java\ro\flota\server\api\ServiciuIndisponibil.java`
- Modifică: `server\src\main\java\ro\flota\server\api\EroriApi.java`
- Modifică: `server\src\main\java\ro\flota\server\flota\Ore.java`, care devine public
- Modifică: `server\src\main\java\ro\flota\server\mqtt\ClientMqtt.java`
- Creează, în `server\src\main\java\ro\flota\server\comenzi\`:
  `CerereComanda.java`, `ComandaDto.java`, `ValidatorComenzi.java`,
  `Comenzi.java` și `ServiciuComenzi.java`
- Creează: `server\src\main\java\ro\flota\server\api\ComenziController.java`
- Creează: `docs\mesaje-test\cerere-limiteaza.json`, `cerere-gresita.json`
- Test: `server\src\test\java\ro\flota\server\comenzi\ValidatorComenziTest.java`

**Interfețe:**
- Consumă: `RegistruFlota.cunoscuta` (faza 2), `CerereGresita`, `Negasit`
  (faza 2, Task 7) și `JdbcClient`.
- Produce:
  - `ServiciuIndisponibil extends RuntimeException`, care devine 503;
  - `Ore.ts` și `Ore.instant`, acum publice;
  - în `ClientMqtt`: `boolean conectat()` și
    `void publica(String topic, byte[] payload)`, pe QoS 1;
  - `record CerereComanda(String tip, JsonNode parametri)`;
  - `record ComandaDto(long id, String masinaId, String tip, JsonNode parametri, Instant oraTrimiterii, Instant oraConfirmarii, String stare, String motiv)`;
  - `static ObjectNode ValidatorComenzi.verifica(CerereComanda)`, care întoarce
    parametrii curățați sau aruncă `CerereGresita`;
  - `Comenzi`, cu `ComandaDto salveaza(String masinaId, String tip, JsonNode parametri, Instant ora)`
    și `List<ComandaDto> pentruMasina(String masinaId)`;
  - `ServiciuComenzi.trimite(String masinaId, CerereComanda) : ComandaDto`;
  - endpoint-urile 6 (`POST /api/masini/{id}/comenzi`) și 7
    (`GET /api/masini/{id}/comenzi`).

- [ ] **Pasul 1: Scrie testele care pică pentru validare**

`ValidatorComenziTest.java`:

```java
package ro.flota.server.comenzi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.server.api.CerereGresita;
import tools.jackson.databind.json.JsonMapper;

class ValidatorComenziTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static CerereComanda cerere(String tip, String parametri) {
        return new CerereComanda(tip, parametri == null ? null : JSON.readTree(parametri));
    }

    private static String eroare(String tip, String parametri) {
        return assertThrows(CerereGresita.class, () -> ValidatorComenzi.verifica(cerere(tip, parametri))).getMessage();
    }

    @Test
    void limiteazaVitezaBunaPastreazaDoarKmh() {
        assertEquals(JSON.readTree("{\"kmh\": 50}"),
                ValidatorComenzi.verifica(cerere("LIMITEAZA_VITEZA", "{\"kmh\": 50, \"altceva\": 1}")));
    }

    @Test
    void kmhGresitEsteRefuzat() {
        for (String p : List.of("{}", "{\"kmh\": 9}", "{\"kmh\": 131}", "{\"kmh\": 200}", "{\"kmh\": \"50\"}", "{\"kmh\": 50.5}")) {
            assertTrue(eroare("LIMITEAZA_VITEZA", p).contains("kmh"), p);
        }
    }

    @Test
    void intoarceLaBazaNuAreParametri() {
        assertEquals(JSON.readTree("{}"), ValidatorComenzi.verifica(cerere("INTOARCE_LA_BAZA", null)));
    }

    @Test
    void tipNecunoscutEsteRefuzat() {
        assertTrue(eroare("ZBOARA", "{}").contains("ZBOARA"));
    }

    @Test
    void tipLipsaEsteRefuzat() {
        assertTrue(eroare(null, "{}").contains("tipul"));
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `CerereComanda`
și `ValidatorComenzi`.

- [ ] **Pasul 3: Scrie `CerereComanda`, `ComandaDto` și `ValidatorComenzi`**

`CerereComanda.java`:

```java
package ro.flota.server.comenzi;

import tools.jackson.databind.JsonNode;

/** Ce trimite UI-ul la POST /api/masini/{id}/comenzi: {"tip": "...", "parametri": {...}}. */
public record CerereComanda(String tip, JsonNode parametri) {
}
```

`ComandaDto.java`:

```java
package ro.flota.server.comenzi;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/** Un rând din tabelul comenzi. stare e TRIMISA, CONFIRMATA sau RESPINSA. */
public record ComandaDto(long id, String masinaId, String tip, JsonNode parametri, Instant oraTrimiterii,
                         Instant oraConfirmarii, String stare, String motiv) {
}
```

`ValidatorComenzi.java`:

```java
package ro.flota.server.comenzi;

import ro.flota.server.api.CerereGresita;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Verifică o comandă înainte să plece spre mașină (spec §4.2) și întoarce parametrii curățați, adică doar
 * câmpurile pe care le înțelege mașina. Mașina îi verifică și ea, a doua oară (D26).
 */
public final class ValidatorComenzi {

    static final int KMH_MINIM = 10;
    static final int KMH_MAXIM = 130;
    static final int TEXT_MAXIM = 200;

    private ValidatorComenzi() {
    }

    public static ObjectNode verifica(CerereComanda cerere) {
        if (cerere == null || cerere.tip() == null) {
            throw new CerereGresita("lipsește tipul comenzii");
        }
        JsonNode p = cerere.parametri() == null ? JsonNodeFactory.instance.objectNode() : cerere.parametri();
        ObjectNode curati = JsonNodeFactory.instance.objectNode();
        switch (cerere.tip()) {
            case "LIMITEAZA_VITEZA" -> {
                JsonNode kmh = p.get("kmh");
                if (kmh == null || !kmh.isIntegralNumber() || kmh.asInt() < KMH_MINIM || kmh.asInt() > KMH_MAXIM) {
                    throw new CerereGresita("kmh trebuie să fie un număr întreg între 10 și 130");
                }
                curati.put("kmh", kmh.asInt());
            }
            case "INTOARCE_LA_BAZA" -> {
                // fără parametri
            }
            default -> throw new CerereGresita("tip de comandă necunoscut: " + cerere.tip());
        }
        return curati;
    }
}
```

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 5 teste din `ValidatorComenziTest` trecute.

- [ ] **Pasul 5: Scrie `ServiciuIndisponibil` și adaug-o în `EroriApi`**

`ServiciuIndisponibil.java`:

```java
package ro.flota.server.api;

/** Un serviciu de care depinde cererea nu răspunde: broker-ul sau proxy-ul. Devine 503. */
public final class ServiciuIndisponibil extends RuntimeException {

    public ServiciuIndisponibil(String mesaj) {
        super(mesaj);
    }
}
```

În `EroriApi.java`, după `negasit`:

```java
    @ExceptionHandler(ServiciuIndisponibil.class)
    ResponseEntity<Eroare> indisponibil(ServiciuIndisponibil e) {
        return raspuns(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
```

În `flota\Ore.java`, clasa și cele două metode devin `public`, ca să le poată
folosi și pachetul `comenzi`.

- [ ] **Pasul 6: Adaugă publicarea în `ClientMqtt`**

După `aboneaza()`:

```java
    public boolean conectat() {
        return client.getState().isConnected();
    }

    /** Publică pe QoS 1 (spec §4.1, comenzile). Nu așteaptă; o eroare se scrie în log. */
    public void publica(String topic, byte[] payload) {
        client.publishWith()
                .topic(topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(payload)
                .send()
                .whenComplete((rezultat, eroare) -> {
                    if (eroare != null) {
                        log.warn("N-am putut publica pe {}: {}", topic, eroare.getMessage());
                    }
                });
    }
```

- [ ] **Pasul 7: Scrie `Comenzi` și `ServiciuComenzi`**

`Comenzi.java`:

```java
package ro.flota.server.comenzi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ro.flota.server.flota.Ore;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Tabelul comenzi. id-ul dat de MySQL e și id-ul din mesajul trimis mașinii (spec §4.2). */
@Repository
public class Comenzi {

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public Comenzi(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public ComandaDto salveaza(String masinaId, String tip, JsonNode parametri, Instant ora) {
        KeyHolder cheie = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO comenzi (masina_id, tip, parametri, ora_trimiterii, stare) "
                        + "VALUES (:masina, :tip, :parametri, :ora, 'TRIMISA')")
                .param("masina", masinaId)
                .param("tip", tip)
                .param("parametri", json.writeValueAsString(parametri))
                .param("ora", Ore.ts(ora))
                .update(cheie);
        return new ComandaDto(cheie.getKey().longValue(), masinaId, tip, parametri, ora, null, "TRIMISA", null);
    }

    ComandaDto rand(ResultSet rs, int nr) throws SQLException {
        return new ComandaDto(rs.getLong("id"), rs.getString("masina_id"), rs.getString("tip"),
                json.readTree(rs.getString("parametri")), Ore.instant(rs.getTimestamp("ora_trimiterii")),
                Ore.instant(rs.getTimestamp("ora_confirmarii")), rs.getString("stare"), rs.getString("motiv"));
    }
}
```

`ServiciuComenzi.java`:

```java
package ro.flota.server.comenzi;

import java.time.Clock;
import org.springframework.stereotype.Service;
import ro.flota.server.api.Negasit;
import ro.flota.server.api.ServiciuIndisponibil;
import ro.flota.server.date.RegistruFlota;
import ro.flota.server.mqtt.ClientMqtt;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Trimiterea unei comenzi (spec §6.5, endpoint-ul 6): o verifică, o salvează TRIMISA, apoi o publică pe
 * QoS 1. Dacă mașina e offline, broker-ul o păstrează în sesiunea ei persistentă până revine.
 */
@Service
public class ServiciuComenzi {

    private final Comenzi comenzi;
    private final RegistruFlota registru;
    private final ClientMqtt mqtt;
    private final JsonMapper json;
    private final Clock ceas;

    public ServiciuComenzi(Comenzi comenzi, RegistruFlota registru, ClientMqtt mqtt, JsonMapper json, Clock ceas) {
        this.comenzi = comenzi;
        this.registru = registru;
        this.mqtt = mqtt;
        this.json = json;
        this.ceas = ceas;
    }

    public ComandaDto trimite(String masinaId, CerereComanda cerere) {
        if (!registru.cunoscuta(masinaId)) {
            throw new Negasit("mașina " + masinaId + " nu există");
        }
        ObjectNode parametri = ValidatorComenzi.verifica(cerere);
        if (!mqtt.conectat()) {
            throw new ServiciuIndisponibil("serverul nu e conectat la Mosquitto; comanda n-a fost trimisă");
        }
        ComandaDto comanda = comenzi.salveaza(masinaId, cerere.tip(), parametri, ceas.instant());
        ObjectNode mesaj = json.createObjectNode();
        mesaj.put("id", comanda.id());
        mesaj.put("tip", comanda.tip());
        mesaj.set("parametri", parametri);
        mqtt.publica("flota/" + masinaId + "/comenzi", json.writeValueAsBytes(mesaj));
        return comanda;
    }
}
```

- [ ] **Pasul 8: Scrie `ComenziController`, cu `POST`**

```java
package ro.flota.server.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.comenzi.CerereComanda;
import ro.flota.server.comenzi.ComandaDto;
import ro.flota.server.comenzi.ServiciuComenzi;

/** Endpoint-urile 6 și 7 din spec §6.5: comenzile unei mașini. */
@RestController
@RequestMapping("/api/masini/{id}/comenzi")
public class ComenziController {

    private final ServiciuComenzi serviciu;

    public ComenziController(ServiciuComenzi serviciu) {
        this.serviciu = serviciu;
    }

    @PostMapping
    public ComandaDto trimite(@PathVariable String id, @RequestBody CerereComanda cerere) {
        return serviciu.trimite(id, cerere);
    }
}
```

- [ ] **Pasul 9: Scrie cererile de test**

`docs\mesaje-test\cerere-limiteaza.json`:

```json
{"tip": "LIMITEAZA_VITEZA", "parametri": {"kmh": 50}}
```

`docs\mesaje-test\cerere-gresita.json`:

```json
{"tip": "LIMITEAZA_VITEZA", "parametri": {"kmh": 200}}
```

- [ ] **Pasul 10: Verifică trimiterea**

Totul pornit după README. În D:

```powershell
curl.exe -s -X POST -H "Content-Type: application/json" --data-binary "@docs/mesaje-test/cerere-limiteaza.json" http://127.0.0.1:8080/api/masini/masina-3/comenzi
curl.exe -s -i -X POST -H "Content-Type: application/json" --data-binary "@docs/mesaje-test/cerere-gresita.json" http://127.0.0.1:8080/api/masini/masina-3/comenzi
Sql "SELECT id, masina_id, tip, parametri, stare FROM comenzi ORDER BY id DESC LIMIT 3"
```

Se așteaptă:
- la prima cerere, JSON-ul comenzii, cu `"stare":"TRIMISA"`. În C apare
  `[masina-3] comanda N LIMITEAZA_VITEZA: EXECUTATA`, iar în câteva secunde
  viteza lui `masina-3` nu mai trece de 50;
- la a doua, `HTTP/1.1 400`, cu mesajul despre `kmh`;
- în bază, un singur rând nou, `TRIMISA`. Rămâne așa până în Task 5, fiindcă
  serverul încă nu ascultă confirmările.

- [ ] **Pasul 11: Commit-ul lui Claude**

```bash
git add server/src docs/mesaje-test
git commit -m "Server: trimiterea comenzilor, salvate TRIMISA si publicate pe QoS 1

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Pasul 12: Claude explică.** Analogia: dispecerul scrie comanda în
  registru cu un număr, apoi o dă la stația radio. Numărul din registru e și
  numărul de pe bilet, ca răspunsul șoferului să se potrivească. Apoi:
  - de ce comanda se salvează înainte să plece: răspunsul poate veni foarte
    repede;
  - de ce o comandă cerută cât broker-ul e deconectat dă 503 în loc să se
    piardă (D26);
  - cum construiește Jackson un `ObjectNode`.

- [ ] **Pasul 13: Scrie testele care pică pentru `MESAJ_SOFER`**

Adaugă în `ValidatorComenziTest`:

```java
    @Test
    void mesajSoferBun() {
        assertEquals(JSON.readTree("{\"text\": \"Treci pe la bază\"}"),
                ValidatorComenzi.verifica(cerere("MESAJ_SOFER", "{\"text\": \"Treci pe la bază\"}")));
    }

    @Test
    void mesajSoferPreaLung() {
        assertTrue(eroare("MESAJ_SOFER", "{\"text\": \"" + "x".repeat(201) + "\"}").contains("200"));
    }

    @Test
    void mesajSoferGol() {
        assertTrue(eroare("MESAJ_SOFER", "{\"text\": \" \"}").contains("text"));
        assertTrue(eroare("MESAJ_SOFER", "{}").contains("text"));
    }
```

- [ ] **Pasul 14: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: 3 teste picate, cu „tip de comandă necunoscut: MESAJ_SOFER”.

- [ ] **Pasul 15: Tudor scrie partea lui.** Ce trebuie:
  - **în `ValidatorComenzi`,** cazul `MESAJ_SOFER`: `text` e un șir care nu e
    gol și are cel mult 200 de caractere, iar parametrii curățați îl păstrează
    doar pe el. Mesajele de eroare pomenesc `text` și, la cel prea lung, `200`;
  - **în `Comenzi`,** `List<ComandaDto> pentruMasina(String masinaId)`: cele mai
    noi 50, în ordinea id-ului, descrescător, cu `rand` deja scris;
  - **în `ComenziController`,** `GET` pe aceeași adresă: 404 pentru o mașină
    necunoscută, altfel istoricul. Controller-ul primește în constructor și
    `Comenzi` și `RegistruFlota`.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

În `ValidatorComenzi`, cazul nou, înainte de `default`:

```java
            case "MESAJ_SOFER" -> {
                JsonNode text = p.get("text");
                if (text == null || !text.isString() || text.asString().isBlank()) {
                    throw new CerereGresita("lipsește textul mesajului");
                }
                if (text.asString().length() > TEXT_MAXIM) {
                    throw new CerereGresita("textul are peste 200 de caractere");
                }
                curati.put("text", text.asString());
            }
```

În `Comenzi`, cu importul `java.util.List`:

```java
    public List<ComandaDto> pentruMasina(String masinaId) {
        return jdbc.sql("SELECT * FROM comenzi WHERE masina_id = :masina ORDER BY id DESC LIMIT 50")
                .param("masina", masinaId)
                .query(this::rand)
                .list();
    }
```

`ComenziController`, întreg:

```java
package ro.flota.server.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.comenzi.CerereComanda;
import ro.flota.server.comenzi.ComandaDto;
import ro.flota.server.comenzi.Comenzi;
import ro.flota.server.comenzi.ServiciuComenzi;
import ro.flota.server.date.RegistruFlota;

/** Endpoint-urile 6 și 7 din spec §6.5: comenzile unei mașini. */
@RestController
@RequestMapping("/api/masini/{id}/comenzi")
public class ComenziController {

    private final ServiciuComenzi serviciu;
    private final Comenzi comenzi;
    private final RegistruFlota registru;

    public ComenziController(ServiciuComenzi serviciu, Comenzi comenzi, RegistruFlota registru) {
        this.serviciu = serviciu;
        this.comenzi = comenzi;
        this.registru = registru;
    }

    @PostMapping
    public ComandaDto trimite(@PathVariable String id, @RequestBody CerereComanda cerere) {
        return serviciu.trimite(id, cerere);
    }

    @GetMapping
    public List<ComandaDto> istoric(@PathVariable String id) {
        if (!registru.cunoscuta(id)) {
            throw new Negasit("mașina " + id + " nu există");
        }
        return comenzi.pentruMasina(id);
    }
}
```

</details>

- [ ] **Pasul 16: Rulează testele și verifică istoricul**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 8 teste din `ValidatorComenziTest` trecute.
Repornește S, apoi:

```powershell
curl.exe -s http://127.0.0.1:8080/api/masini/masina-3/comenzi
curl.exe -s -i http://127.0.0.1:8080/api/masini/masina-99/comenzi
```

Se așteaptă: comanda de la pasul 10, apoi `HTTP/1.1 404`.

- [ ] **Pasul 17: Claude revizuiește,** fără să rescrie.

- [ ] **Pasul 18: Commit-ul lui Tudor**

```bash
git add server/src
git commit -m "Server: mesajul pentru sofer si istoricul comenzilor"
```

---

### Task 5: Confirmările și idempotența

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e idempotența: același
mesaj primit de două ori schimbă starea o singură dată.

**Fișiere:**
- Modifică, în `server\src\main\java\ro\flota\server\mqtt\`:
  `MesajPrimit.java`, `Decodor.java` și `ClientMqtt.java`
- Modifică: `server\src\main\java\ro\flota\server\comenzi\Comenzi.java`
- Modifică: `server\src\main\java\ro\flota\server\flota\Dispecer.java`
- Teste: `server\src\test\java\ro\flota\server\mqtt\DecodorTest.java`,
  `server\src\test\java\ro\flota\server\flota\DispecerConfirmariTest.java`

**Interfețe:**
- Consumă: `Comenzi` și `ComandaDto` (Task 4).
- Produce:
  - `MesajPrimit.Confirmare(String masinaId, long comandaId, Instant ora, String rezultat, String motiv)`;
  - în `Comenzi`:
    `boolean confirmaDacaTrimisa(long id, String masinaId, String stare, Instant ora, String motiv)`
    și `Optional<ComandaDto> una(long id)`;
  - constructorul lui `Dispecer` primește și `Comenzi`;
  - pe canalul live, `CONFIRMARE`, cu `ComandaDto` după schimbare.

- [ ] **Pasul 1: Scrie testele care pică pentru decodor**

Adaugă în `DecodorTest`:

```java
    @Test
    void confirmareaExecutata() throws MesajInvalid {
        assertEquals(new MesajPrimit.Confirmare("masina-3", 57, Instant.parse("2026-12-10T10:15:04.002Z"), "EXECUTATA", null),
                decodeaza("flota/masina-3/confirmari",
                        "{\"comanda_id\": 57, \"ora\": \"2026-12-10T10:15:04.002Z\", \"rezultat\": \"EXECUTATA\"}"));
    }

    @Test
    void confirmareaRespinsaAreMotiv() throws MesajInvalid {
        MesajPrimit.Confirmare c = (MesajPrimit.Confirmare) decodeaza("flota/masina-3/confirmari",
                "{\"comanda_id\": 58, \"ora\": \"2026-12-10T10:15:04.002Z\", \"rezultat\": \"RESPINSA\", \"motiv\": \"kmh în afara intervalului\"}");
        assertEquals("kmh în afara intervalului", c.motiv());
    }

    @Test
    void rezultatulNecunoscutEsteRefuzat() {
        assertTrue(motiv("flota/masina-3/confirmari",
                "{\"comanda_id\": 59, \"ora\": \"2026-12-10T10:15:04.002Z\", \"rezultat\": \"POATE\"}").contains("POATE"));
    }
```

- [ ] **Pasul 2: Scrie testul care pică pentru idempotență**

`server\src\test\java\ro\flota\server\flota\DispecerConfirmariTest.java`. Mockito
dă fiecărei dependențe a dispecerului o variantă falsă. Pentru tipurile pe care
testul nu le declară, `@InjectMocks` pune `null`, așa că testul rămâne valabil
și după ce dispecerul primește dependențe noi în task-urile următoare:

```java
package ro.flota.server.flota;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.flota.server.comenzi.ComandaDto;
import ro.flota.server.comenzi.Comenzi;
import ro.flota.server.live.CanalLive;
import ro.flota.server.mqtt.MesajPrimit;

@ExtendWith(MockitoExtension.class)
class DispecerConfirmariTest {

    private static final Instant ORA = Instant.parse("2026-12-10T10:15:04.002Z");

    @Mock
    Pozitii pozitii;
    @Mock
    Masini masini;
    @Mock
    Alarme alarme;
    @Mock
    Comenzi comenzi;
    @Mock
    CanalLive live;
    @Mock
    Clock ceas;

    @InjectMocks
    Dispecer dispecer;

    @Test
    void aceeasiConfirmareDeDouaOriSeAnuntaOData() {
        MesajPrimit.Confirmare confirmare = new MesajPrimit.Confirmare("masina-3", 57, ORA, "EXECUTATA", null);
        ComandaDto confirmata = new ComandaDto(57, "masina-3", "LIMITEAZA_VITEZA", null,
                Instant.parse("2026-12-10T10:15:03Z"), ORA, "CONFIRMATA", null);
        when(comenzi.confirmaDacaTrimisa(57, "masina-3", "CONFIRMATA", ORA, null)).thenReturn(true, false);
        when(comenzi.una(57)).thenReturn(Optional.of(confirmata));

        dispecer.primeste(confirmare);
        dispecer.primeste(confirmare);

        verify(live, times(1)).trimite("CONFIRMARE", confirmata);
    }

    @Test
    void respinsaDevineRespinsaCuMotiv() {
        dispecer.primeste(new MesajPrimit.Confirmare("masina-3", 58, ORA, "RESPINSA", "kmh în afara intervalului"));
        verify(comenzi).confirmaDacaTrimisa(58, "masina-3", "RESPINSA", ORA, "kmh în afara intervalului");
        verify(live, never()).trimite(anyString(), any());
    }
}
```

- [ ] **Pasul 3: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru
`MesajPrimit.Confirmare`.

- [ ] **Pasul 4: Adaugă confirmarea în `MesajPrimit` și în `Decodor`**

În `MesajPrimit.java`, după `Stare`:

```java
    /** Răspunsul mașinii la o comandă: EXECUTATA sau RESPINSA, cu motiv. */
    record Confirmare(String masinaId, long comandaId, Instant ora, String rezultat, String motiv) implements MesajPrimit {
    }
```

În `Decodor.java`, cazul nou din switch:

```java
            case "confirmari" -> confirmare(id, obiect(payload));
```

și metoda:

```java
    private static MesajPrimit.Confirmare confirmare(String id, JsonNode j) throws MesajInvalid {
        String rezultat = text(j, "rezultat");
        if (!rezultat.equals("EXECUTATA") && !rezultat.equals("RESPINSA")) {
            throw new MesajInvalid("rezultat necunoscut: " + rezultat);
        }
        JsonNode motiv = j.get("motiv");
        return new MesajPrimit.Confirmare(id, intreg(j, "comanda_id"), ora(j, "ora"), rezultat,
                motiv != null && motiv.isString() ? motiv.asString() : null);
    }
```

În `ClientMqtt.aboneaza()`, o abonare nouă, înainte de `.send()`:

```java
                .addSubscription().topicFilter("flota/+/confirmari").qos(MqttQos.AT_LEAST_ONCE).applySubscription()
```

- [ ] **Pasul 5: Adaugă în `Comenzi` confirmarea condiționată**

Importul nou e `java.util.Optional`, apoi:

```java
    /**
     * Comanda trece în CONFIRMATA sau RESPINSA doar dacă era TRIMISA (spec §6.3). A doua confirmare
     * nu mai găsește niciun rând de schimbat: asta e idempotența, garantată chiar de MySQL.
     */
    public boolean confirmaDacaTrimisa(long id, String masinaId, String stare, Instant ora, String motiv) {
        int schimbate = jdbc.sql("""
                        UPDATE comenzi SET stare = :stare, ora_confirmarii = :ora, motiv = :motiv
                        WHERE id = :id AND masina_id = :masina AND stare = 'TRIMISA'""")
                .param("stare", stare)
                .param("ora", Ore.ts(ora))
                .param("motiv", motiv)
                .param("id", id)
                .param("masina", masinaId)
                .update();
        return schimbate == 1;
    }

    public Optional<ComandaDto> una(long id) {
        return jdbc.sql("SELECT * FROM comenzi WHERE id = :id").param("id", id).query(this::rand).optional();
    }
```

- [ ] **Pasul 6: Adaugă confirmarea în `Dispecer`**

Importul nou e `ro.flota.server.comenzi.Comenzi`. Câmpul și constructorul devin:

```java
    private final Pozitii pozitii;
    private final Masini masini;
    private final Alarme alarme;
    private final Comenzi comenzi;
    private final CanalLive live;
    private final Clock ceas;

    public Dispecer(Pozitii pozitii, Masini masini, Alarme alarme, Comenzi comenzi, CanalLive live, Clock ceas) {
        this.pozitii = pozitii;
        this.masini = masini;
        this.alarme = alarme;
        this.comenzi = comenzi;
        this.live = live;
        this.ceas = ceas;
    }
```

Switch-ul din `primeste` primește cazul nou. Fără el nu compilează, fiindcă
`MesajPrimit` e `sealed`:

```java
            case MesajPrimit.Confirmare c -> laConfirmare(c);
```

Metoda:

```java
    /** Pe QoS 1, aceeași confirmare poate veni de două ori; doar prima schimbă ceva (spec §6.3). */
    private void laConfirmare(MesajPrimit.Confirmare c) {
        String stare = c.rezultat().equals("EXECUTATA") ? "CONFIRMATA" : "RESPINSA";
        if (comenzi.confirmaDacaTrimisa(c.comandaId(), c.masinaId(), stare, c.ora(), c.motiv())) {
            comenzi.una(c.comandaId()).ifPresent(comanda -> live.trimite("CONFIRMARE", comanda));
        } else {
            log.info("{}: confirmarea comenzii {} a mai venit o dată sau nu se potrivește; o ignor",
                    c.masinaId(), c.comandaId());
        }
    }
```

- [ ] **Pasul 7: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu noile teste din `DecodorTest` și cele 2 din
`DispecerConfirmariTest` trecute.

- [ ] **Pasul 8: Verifică pe viu**

Repornește S. Trimite comanda din Task 4, pasul 10. Apoi:

```powershell
Sql "SELECT id, stare, ora_trimiterii, ora_confirmarii FROM comenzi ORDER BY id DESC LIMIT 1"
```

Se așteaptă: `CONFIRMATA`, cu `ora_confirmarii` la o secundă sau două după
`ora_trimiterii`.

- [ ] **Pasul 9: Claude explică.** Analogia: un bon de livrare pe care scrie
  „de semnat doar dacă e încă în curs”. Al doilea curier care vine cu același
  bon găsește livrarea deja semnată și pleacă fără să schimbe nimic. Apoi:
  - de ce QoS 1 poate livra de două ori: confirmarea broker-ului se poate pierde
    pe drum;
  - de ce condiția stă în `WHERE`, nu într-un `if` din Java;
  - ce verifică testul cu Mockito: o singură trimitere pe canalul live.

- [ ] **Pasul 10: Commit**

```bash
git add server/src
git commit -m "Server: confirmarile comenzilor, idempotente

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Regulile de zonă și de viteză

**Cine scrie:**
- **Punctul în poligon și `IESIRE_DIN_ZONA`:** Claude, ca exemplu explicat.
- **`VITEZA_PESTE_LIMITA`:** Tudor, ca repetiție a regulii date la schimbare.

**Fișiere:**
- Creează, în `server\src\main\java\ro\flota\server\reguli\`:
  `PunctInPoligon.java` și `ReguliAlarma.java`
- Modifică: `server\src\main\java\ro\flota\server\flota\Dispecer.java`
- Teste, în `server\src\test\java\ro\flota\server\reguli\`:
  `PunctInPoligonTest.java` și `ReguliAlarmaTest.java`

**Interfețe:**
- Consumă: `Punct`, `ZonaDefinita`, `RegistruFlota.zonaMasinii` (faza 2, Task 3)
  și `Alarme.salveaza` (faza 2, Task 6).
- Produce:
  - `static boolean PunctInPoligon.contine(List<Punct> poligon, Punct p)`. Un
    punct pe margine e înăuntru;
  - bean-ul `ReguliAlarma(RegistruFlota)`, cu
    `List<ReguliAlarma.AlarmaNoua> verifica(String masinaId, Punct p, double viteza)`
    și `record AlarmaNoua(String tip, String detalii)`;
  - constructorul lui `Dispecer` primește și `ReguliAlarma`.

- [ ] **Pasul 1: Scrie testele care pică pentru poligon**

`PunctInPoligonTest.java`:

```java
package ro.flota.server.reguli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.server.date.Punct;

class PunctInPoligonTest {

    /** Un pătrat de 0,01° pe latură, închis cum îl salvează geojson.io: primul punct repetat la capăt. */
    private static final List<Punct> PATRAT = List.of(
            new Punct(46.00, 23.00), new Punct(46.00, 23.01), new Punct(46.01, 23.01),
            new Punct(46.01, 23.00), new Punct(46.00, 23.00));

    /** Un L: pătratul fără sfertul din dreapta sus. */
    private static final List<Punct> L = List.of(
            new Punct(46.00, 23.00), new Punct(46.00, 23.01), new Punct(46.005, 23.01),
            new Punct(46.005, 23.005), new Punct(46.01, 23.005), new Punct(46.01, 23.00));

    @Test
    void centrulEsteInauntru() {
        assertTrue(PunctInPoligon.contine(PATRAT, new Punct(46.005, 23.005)));
    }

    @Test
    void puncteleDinAfaraSintAfara() {
        assertFalse(PunctInPoligon.contine(PATRAT, new Punct(46.02, 23.005)));
        assertFalse(PunctInPoligon.contine(PATRAT, new Punct(46.005, 22.99)));
    }

    @Test
    void margineaEsteInauntru() {
        assertTrue(PunctInPoligon.contine(PATRAT, new Punct(46.00, 23.005)));
        assertTrue(PunctInPoligon.contine(PATRAT, new Punct(46.005, 23.01)));
    }

    @Test
    void coltulEsteInauntru() {
        assertTrue(PunctInPoligon.contine(PATRAT, new Punct(46.01, 23.01)));
    }

    @Test
    void scobituraUnuiPoligonConcavEsteAfara() {
        assertTrue(PunctInPoligon.contine(L, new Punct(46.002, 23.008)));
        assertFalse(PunctInPoligon.contine(L, new Punct(46.008, 23.008)));
    }

    @Test
    void inelulNeinchisMergeLaFel() {
        List<Punct> deschis = PATRAT.subList(0, 4);
        assertTrue(PunctInPoligon.contine(deschis, new Punct(46.005, 23.005)));
        assertFalse(PunctInPoligon.contine(deschis, new Punct(46.02, 23.005)));
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `PunctInPoligon`.

- [ ] **Pasul 3: Scrie `PunctInPoligon`**

```java
package ro.flota.server.reguli;

import java.util.List;
import ro.flota.server.date.Punct;

/**
 * Algoritmul razei (spec §6.4): din punct pornește o rază spre est. Dacă taie marginea poligonului de un
 * număr impar de ori, punctul e înăuntru. Un punct chiar pe margine se socotește înăuntru. Longitudinea e
 * x, iar latitudinea e y; pe o zonă de câțiva kilometri, curbura Pământului nu contează.
 */
public final class PunctInPoligon {

    private static final double EPSILON = 1e-12;

    private PunctInPoligon() {
    }

    public static boolean contine(List<Punct> poligon, Punct p) {
        int n = poligon.size();
        boolean inauntru = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Punct a = poligon.get(i);
            Punct b = poligon.get(j);
            if (peSegment(a, b, p)) {
                return true;
            }
            boolean taieOrizontala = (a.lat() > p.lat()) != (b.lat() > p.lat());
            if (taieOrizontala) {
                double lonIntersectie = a.lon() + (p.lat() - a.lat()) * (b.lon() - a.lon()) / (b.lat() - a.lat());
                if (p.lon() < lonIntersectie) {
                    inauntru = !inauntru;
                }
            }
        }
        return inauntru;
    }

    /** p e pe segmentul ab: coliniar cu el și în dreptunghiul dintre capete. */
    private static boolean peSegment(Punct a, Punct b, Punct p) {
        double produs = (b.lon() - a.lon()) * (p.lat() - a.lat()) - (b.lat() - a.lat()) * (p.lon() - a.lon());
        if (Math.abs(produs) > EPSILON) {
            return false;
        }
        return p.lat() >= Math.min(a.lat(), b.lat()) - EPSILON && p.lat() <= Math.max(a.lat(), b.lat()) + EPSILON
                && p.lon() >= Math.min(a.lon(), b.lon()) - EPSILON && p.lon() <= Math.max(a.lon(), b.lon()) + EPSILON;
    }
}
```

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 6 teste din `PunctInPoligonTest` trecute.

- [ ] **Pasul 5: Scrie testele care pică pentru reguli**

`ReguliAlarmaTest.java`:

```java
package ro.flota.server.reguli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.server.date.MasinaDefinita;
import ro.flota.server.date.Punct;
import ro.flota.server.date.RegistruFlota;
import ro.flota.server.date.ZonaDefinita;

class ReguliAlarmaTest {

    private static final List<Punct> PATRAT = List.of(
            new Punct(46.00, 23.00), new Punct(46.00, 23.01), new Punct(46.01, 23.01),
            new Punct(46.01, 23.00), new Punct(46.00, 23.00));
    private static final Punct INAUNTRU = new Punct(46.005, 23.005);
    private static final Punct AFARA = new Punct(46.02, 23.005);

    private final ReguliAlarma reguli;

    ReguliAlarmaTest() {
        RegistruFlota registru = new RegistruFlota();
        registru.incarca(List.of(new ZonaDefinita("z1", "Centru", PATRAT, 50)),
                List.of(new MasinaDefinita("masina-3", "z1")));
        reguli = new ReguliAlarma(registru);
    }

    private List<String> tipuri(Punct p, double viteza) {
        return reguli.verifica("masina-3", p, viteza).stream().map(ReguliAlarma.AlarmaNoua::tip).toList();
    }

    @Test
    void iesireaDinZonaDaOAlarma() {
        assertEquals(List.of(), tipuri(INAUNTRU, 40));
        assertEquals(List.of("IESIRE_DIN_ZONA"), tipuri(AFARA, 40));
    }

    @Test
    void catStaAfaraNuMaiDaAlarme() {
        tipuri(INAUNTRU, 40);
        tipuri(AFARA, 40);
        assertEquals(List.of(), tipuri(AFARA, 40));
        assertEquals(List.of(), tipuri(AFARA, 40));
    }

    @Test
    void seRearmeazaCandRevineInZona() {
        tipuri(INAUNTRU, 40);
        tipuri(AFARA, 40);
        tipuri(INAUNTRU, 40);
        assertEquals(List.of("IESIRE_DIN_ZONA"), tipuri(AFARA, 40));
    }

    @Test
    void oMasinaPornitaAfaraNuDaAlarmaPanaNuIntraSiIese() {
        assertEquals(List.of(), tipuri(AFARA, 40));
        assertEquals(List.of(), tipuri(AFARA, 40));
        assertEquals(List.of(), tipuri(INAUNTRU, 40));
        assertEquals(List.of("IESIRE_DIN_ZONA"), tipuri(AFARA, 40));
    }

    @Test
    void detaliileSpunZona() {
        tipuri(INAUNTRU, 40);
        assertTrue(reguli.verifica("masina-3", AFARA, 40).get(0).detalii().contains("Centru"));
    }

    @Test
    void oMasinaFaraZonaNuDaAlarme() {
        assertEquals(List.of(), reguli.verifica("masina-99", AFARA, 200));
    }
}
```

- [ ] **Pasul 6: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `ReguliAlarma`.

- [ ] **Pasul 7: Scrie `ReguliAlarma`, cu `IESIRE_DIN_ZONA`**

```java
package ro.flota.server.reguli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import ro.flota.server.date.Punct;
import ro.flota.server.date.RegistruFlota;
import ro.flota.server.date.ZonaDefinita;

/**
 * Regulile de alarmă ale serverului, aplicate fiecărei poziții (spec §6.4). Se dau la schimbare (D12): o
 * alarmă când mașina iese din zonă, nu una pe secundă cât stă afară. Ține ultima stare a fiecărei mașini.
 * Rulează doar pe firul MQTT (D22), deci n-are nevoie de synchronized.
 */
@Component
public class ReguliAlarma {

    public record AlarmaNoua(String tip, String detalii) {
    }

    private final RegistruFlota registru;
    private final Map<String, Boolean> inZona = new HashMap<>();

    public ReguliAlarma(RegistruFlota registru) {
        this.registru = registru;
    }

    public List<AlarmaNoua> verifica(String masinaId, Punct p, double viteza) {
        Optional<ZonaDefinita> zona = registru.zonaMasinii(masinaId);
        if (zona.isEmpty()) {
            return List.of();
        }
        ZonaDefinita z = zona.get();
        List<AlarmaNoua> alarme = new ArrayList<>();

        // IESIRE_DIN_ZONA: doar trecerea din zonă în afara ei. O mașină pornită afară nu dă alarmă (D26).
        boolean acumInZona = PunctInPoligon.contine(z.poligon(), p);
        Boolean inainte = inZona.put(masinaId, acumInZona);
        if (Boolean.TRUE.equals(inainte) && !acumInZona) {
            alarme.add(new AlarmaNoua("IESIRE_DIN_ZONA", "a ieșit din zona " + z.nume()));
        }
        return alarme;
    }
}
```

- [ ] **Pasul 8: Leagă regulile în `Dispecer`**

Importurile noi: `ro.flota.server.date.Punct` și `ro.flota.server.reguli.ReguliAlarma`.
Câmpul nou `private final ReguliAlarma reguli;`. Constructorul primește
`ReguliAlarma reguli` după `Comenzi comenzi` și îl salvează. În `laTelemetrie`,
înainte de `live.trimite("POZITIE", pozitie);`:

```java
        // Regulile se aplică și pozițiilor recuperate; alarma poartă ora mașinii (spec §6.4).
        for (ReguliAlarma.AlarmaNoua a : reguli.verifica(t.masinaId(), new Punct(t.lat(), t.lon()), t.viteza())) {
            live.trimite("ALARMA", alarme.salveaza(t.masinaId(), a.tip(), "SERVER", t.oraMasinii(), sosire, a.detalii()));
        }
```

- [ ] **Pasul 9: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 6 teste din `ReguliAlarmaTest` trecute.

- [ ] **Pasul 10: Commit-ul lui Claude**

```bash
git add server/src
git commit -m "Server: punctul in poligon si alarma de iesire din zona

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Pasul 11: Claude explică.** Analogia: ești într-o curte cu gard și
  mergi drept spre est până departe. Dacă treci gardul o dată, ai pornit din
  curte. Dacă îl treci de două ori, ai pornit de afară. Apoi:
  - de ce marginea e tratată separat;
  - starea „ultima dată în zonă” și de ce `null` nu dă alarmă;
  - de ce regulile rulează și pe pozițiile recuperate: așa se află că o mașină a
    ieșit din zonă cât era în tunel.

- [ ] **Pasul 12: Scrie testele care pică pentru viteză**

Adaugă în `ReguliAlarmaTest`:

```java
    @Test
    void vitezaPesteLimitaDaOAlarma() {
        assertEquals(List.of(), tipuri(INAUNTRU, 40));
        assertEquals(List.of("VITEZA_PESTE_LIMITA"), tipuri(INAUNTRU, 52));
    }

    @Test
    void catRamanePesteLimitaNuMaiDaAlarme() {
        tipuri(INAUNTRU, 52);
        assertEquals(List.of(), tipuri(INAUNTRU, 60));
        assertEquals(List.of(), tipuri(INAUNTRU, 55));
    }

    @Test
    void vitezaSeRearmeazaSubLimita() {
        tipuri(INAUNTRU, 52);
        tipuri(INAUNTRU, 49);
        assertEquals(List.of("VITEZA_PESTE_LIMITA"), tipuri(INAUNTRU, 52));
    }

    @Test
    void exactLaLimitaNuEPeste() {
        assertEquals(List.of(), tipuri(INAUNTRU, 50));
    }

    @Test
    void detaliileSpunVitezaSiLimita() {
        String detalii = reguli.verifica("masina-3", INAUNTRU, 52).get(0).detalii();
        assertTrue(detalii.contains("52.0") && detalii.contains("50"), detalii);
    }
```

- [ ] **Pasul 13: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: 3 teste picate. Două nu primesc alarma așteptată, iar
`detaliileSpunVitezaSiLimita` dă `IndexOutOfBoundsException`. Celelalte două
trec deja, fiindcă cer să nu apară nicio alarmă.

- [ ] **Pasul 14: Tudor scrie `VITEZA_PESTE_LIMITA`.** Indicii:
  - încă o hartă de stare, „era peste limită”, cu `false` pentru o mașină
    nevăzută: mașinile pornesc de pe loc (D26);
  - alarma se dă când viteza trece strict peste `vitezaMaxima` și înainte nu
    era peste;
  - starea redevine `false` doar când viteza coboară strict sub limită. Exact
    la limită nu se schimbă nimic;
  - detaliile, cu `String.format(Locale.ROOT, …)`: viteza cu o zecimală,
    numele zonei și limita.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

Importul nou e `java.util.Locale`. Câmpul nou:

```java
    private final Map<String, Boolean> pesteLimita = new HashMap<>();
```

În `verifica`, înainte de `return alarme;`:

```java
        // VITEZA_PESTE_LIMITA: la trecerea peste limita zonei; se rearmează sub limită (D26).
        boolean acumPeste = viteza > z.vitezaMaxima();
        boolean eraPeste = pesteLimita.getOrDefault(masinaId, false);
        if (acumPeste && !eraPeste) {
            alarme.add(new AlarmaNoua("VITEZA_PESTE_LIMITA", String.format(Locale.ROOT,
                    "%.1f km/h; limita zonei %s e %d km/h", viteza, z.nume(), z.vitezaMaxima())));
        }
        if (acumPeste) {
            pesteLimita.put(masinaId, true);
        } else if (viteza < z.vitezaMaxima()) {
            pesteLimita.put(masinaId, false);
        }
```

</details>

- [ ] **Pasul 15: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 11 teste din `ReguliAlarmaTest` trecute.

- [ ] **Pasul 16: Verifică pe viu**

Totul pornit. Lasă simulatorul 5 minute: o țintă de 90 km/h apare rar, deci
alarma de viteză poate întârzia. Apoi:

```powershell
Sql "SELECT masina_id, tip, sursa, ora, detalii FROM alarme WHERE sursa = 'SERVER' ORDER BY id DESC LIMIT 10"
```

Se așteaptă: rânduri `VITEZA_PESTE_LIMITA`. Rânduri `IESIRE_DIN_ZONA` apar dacă
traseele desenate în faza 0 ies din zonă.

- [ ] **Pasul 17: Claude revizuiește,** fără să rescrie.

- [ ] **Pasul 18: Commit-ul lui Tudor**

```bash
git add server/src
git commit -m "Server: alarma de viteza peste limita zonei"
```

---

### Task 7: Mașina offline prea mult

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „o verificare programată,
care citește o stare scrisă de alt fir”.

**Fișiere:**
- Creează, în `server\src\main\java\ro\flota\server\reguli\`:
  `RegulaOffline.java` și `VerificareOffline.java`
- Modifică: `server\src\main\java\ro\flota\server\ServerApplication.java`
- Modifică: `server\src\main\java\ro\flota\server\flota\Dispecer.java`
- Test: `server\src\test\java\ro\flota\server\reguli\RegulaOfflineTest.java`

**Interfețe:**
- Consumă: `Alarme.salveaza` și `CanalLive.trimite`.
- Produce:
  - bean-ul `RegulaOffline`, cu
    `void stare(String masinaId, boolean online, Instant ora)`,
    `List<String> deAlarmat(Instant acum)` și `Instant offlineDin(String)`;
  - bean-ul `VerificareOffline`, cu `@Scheduled` la 5 secunde;
  - constructorul lui `Dispecer` primește și `RegulaOffline`.

- [ ] **Pasul 1: Scrie testele care pică**

`RegulaOfflineTest.java`:

```java
package ro.flota.server.reguli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegulaOfflineTest {

    private static final Instant T0 = Instant.parse("2026-12-10T10:00:00Z");

    private final RegulaOffline regula = new RegulaOffline();

    private static Instant dupa(int secunde) {
        return T0.plusSeconds(secunde);
    }

    @Test
    void exact60DeSecundeNuDaIncaAlarma() {
        regula.stare("masina-3", false, T0);
        assertEquals(List.of(), regula.deAlarmat(dupa(60)));
    }

    @Test
    void pestePragDaOSinguraData() {
        regula.stare("masina-3", false, T0);
        assertEquals(List.of("masina-3"), regula.deAlarmat(dupa(61)));
        assertEquals(List.of(), regula.deAlarmat(dupa(70)));
    }

    @Test
    void revenireaOnlineRearmeaza() {
        regula.stare("masina-3", false, T0);
        regula.deAlarmat(dupa(61));
        regula.stare("masina-3", true, dupa(80));
        regula.stare("masina-3", false, dupa(90));
        assertEquals(List.of(), regula.deAlarmat(dupa(140)));
        assertEquals(List.of("masina-3"), regula.deAlarmat(dupa(151)));
    }

    @Test
    void doarMasinileOfflineSintVerificate() {
        regula.stare("masina-3", false, T0);
        regula.stare("masina-5", true, T0);
        assertEquals(List.of("masina-3"), regula.deAlarmat(dupa(61)));
    }

    @Test
    void unOfflineRepetatNuMutaOra() {
        regula.stare("masina-3", false, T0);
        regula.stare("masina-3", false, dupa(30)); // de exemplu, starea reținută primită din nou
        assertEquals(List.of("masina-3"), regula.deAlarmat(dupa(61)));
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `RegulaOffline`.

- [ ] **Pasul 3: Scrie `RegulaOffline`**

```java
package ro.flota.server.reguli;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * OFFLINE_PRELUNGIT (spec §6.4): o mașină OFFLINE de peste 60 de secunde, dată o singură dată pe episod și
 * rearmată când mașina revine ONLINE. Starea o scrie firul MQTT și o citește verificarea programată, deci
 * metodele sînt synchronized. Starea stă doar în memorie (D26).
 */
@Component
public class RegulaOffline {

    static final Duration PRAG = Duration.ofSeconds(60);

    private final Map<String, Instant> offlineDin = new HashMap<>();
    private final Set<String> alarmate = new HashSet<>();

    public synchronized void stare(String masinaId, boolean online, Instant ora) {
        if (online) {
            offlineDin.remove(masinaId);
            alarmate.remove(masinaId);
        } else {
            offlineDin.putIfAbsent(masinaId, ora);
        }
    }

    /** Mașinile care au trecut acum de prag și n-au primit încă alarma, în ordine alfabetică. */
    public synchronized List<String> deAlarmat(Instant acum) {
        List<String> rezultat = new ArrayList<>();
        for (Map.Entry<String, Instant> e : offlineDin.entrySet()) {
            boolean pestePrag = Duration.between(e.getValue(), acum).compareTo(PRAG) > 0;
            if (pestePrag && alarmate.add(e.getKey())) {
                rezultat.add(e.getKey());
            }
        }
        rezultat.sort(Comparator.naturalOrder());
        return rezultat;
    }

    public synchronized Instant offlineDin(String masinaId) {
        return offlineDin.get(masinaId);
    }
}
```

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 5 teste din `RegulaOfflineTest` trecute.

- [ ] **Pasul 5: Scrie `VerificareOffline` și pornește programarea**

`VerificareOffline.java`:

```java
package ro.flota.server.reguli;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.flota.server.flota.Alarme;
import ro.flota.server.live.CanalLive;

/** La 5 secunde, caută mașinile offline de peste 60 de secunde (spec §6.4). */
@Component
public class VerificareOffline {

    private final RegulaOffline regula;
    private final Alarme alarme;
    private final CanalLive live;
    private final Clock ceas;

    public VerificareOffline(RegulaOffline regula, Alarme alarme, CanalLive live, Clock ceas) {
        this.regula = regula;
        this.alarme = alarme;
        this.live = live;
        this.ceas = ceas;
    }

    @Scheduled(fixedRate = 5_000)
    public void verifica() {
        Instant acum = ceas.instant();
        for (String id : regula.deAlarmat(acum)) {
            long secunde = Duration.between(regula.offlineDin(id), acum).toSeconds();
            live.trimite("ALARMA", alarme.salveaza(id, "OFFLINE_PRELUNGIT", "SERVER", acum, acum,
                    "offline de " + secunde + " de secunde"));
        }
    }
}
```

În `ServerApplication.java`, adnotarea nouă, sub `@SpringBootApplication`:

```java
@EnableScheduling
```

cu importul `org.springframework.scheduling.annotation.EnableScheduling`.

- [ ] **Pasul 6: Spune regulii starea, din `Dispecer`**

Importul nou e `ro.flota.server.reguli.RegulaOffline`. Câmpul nou e
`private final RegulaOffline regulaOffline;`. Constructorul primește
`RegulaOffline regulaOffline` după `ReguliAlarma reguli`. În `laStare`, după
`masini.schimbaStare(…)`:

```java
        regulaOffline.stare(s.masinaId(), s.online(), acum);
```

- [ ] **Pasul 7: Rulează testele și verifică pe viu**

Comanda: `.\mvnw.cmd -q test`, iar rezultatul așteptat e `BUILD SUCCESS`. Apoi
repornește S. În O, `TUNEL masina-3 90`. Se așteaptă: după vreo 65 de secunde,
în bază apare un rând `OFFLINE_PRELUNGIT` pentru `masina-3`, cu detaliile
`offline de 6x de secunde`. După ieșirea din tunel, un al doilea tunel lung dă
din nou alarma.

- [ ] **Pasul 8: Claude explică.** Analogia: paznicul face o tură la 5 minute
  și verifică tabla cu „plecat de la ora…”. Dispecerul scrie pe tablă, paznicul
  doar citește, dar nu au voie să scrie și să citească în același timp. Apoi:
  - `@Scheduled` și `@EnableScheduling`;
  - de ce aici e nevoie de `synchronized`, iar la `ReguliAlarma` nu.

- [ ] **Pasul 9: Commit**

```bash
git add server/src
git commit -m "Server: alarma pentru masina offline de peste un minut

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Statisticile și latența

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „o fereastră care
alunecă”: se țin doar ultimele 10 secunde și ultimele 10 măsurători.

**Fișiere:**
- Creează, în `server\src\main\java\ro\flota\server\statistici\`:
  `FereastraStatistici.java`, `StatisticaDto.java`, `Statistici.java` și
  `TrimitereStatistici.java`
- Creează: `server\src\main\java\ro\flota\server\flota\ContoareDto.java`
- Modifică: `server\src\main\java\ro\flota\server\flota\Masini.java`, `Dispecer.java`
- Creează: `server\src\main\java\ro\flota\server\api\StatisticiController.java`
- Test: `server\src\test\java\ro\flota\server\statistici\FereastraStatisticiTest.java`

**Interfețe:**
- Consumă: `CanalLive.numarSesiuni` și `CanalLive.trimite` (faza 2) și
  `@EnableScheduling` (Task 7).
- Produce:
  - `record StatisticaDto(String masinaId, double mesajePeSecunda, Double latentaMedieMs, long recuperate, long pierdute)`,
    unde `latentaMedieMs` e `null` dacă n-a venit încă niciun mesaj live;
  - bean-ul `Statistici`, cu
    `void mesaj(String masinaId, Instant oraMasinii, Instant oraSosirii, boolean dinBuffer)`
    și `List<StatisticaDto> acum()`;
  - `Masini.contoare() : List<ContoareDto>`, cu
    `record ContoareDto(String id, long recuperate, long pierdute)`;
  - endpoint-ul 12, `GET /api/statistici`, și `STATISTICI` pe canalul live, o
    dată pe secundă;
  - constructorul lui `Dispecer` primește și `Statistici`.

- [ ] **Pasul 1: Scrie testele care pică**

`FereastraStatisticiTest.java`:

```java
package ro.flota.server.statistici;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class FereastraStatisticiTest {

    private static final Instant T0 = Instant.parse("2026-12-10T10:00:00Z");

    private final FereastraStatistici f = new FereastraStatistici();

    private static Instant la(long ms) {
        return T0.plusMillis(ms);
    }

    @Test
    void zeceMesajeInZeceSecundeInseamnaUnulPeSecunda() {
        for (int i = 0; i < 10; i++) {
            f.mesaj(la(i * 1_000L), la(i * 1_000L + 20), false);
        }
        assertEquals(1.0, f.mesajePeSecunda(la(9_500)), 1e-9);
    }

    @Test
    void mesajeleVechiIesDinFereastra() {
        for (int i = 0; i < 10; i++) {
            f.mesaj(la(i * 1_000L), la(i * 1_000L + 20), false);
        }
        assertEquals(0.5, f.mesajePeSecunda(la(15_000)), 1e-9);
    }

    @Test
    void latentaEsteMediaUltimelorZeceMesajeLive() {
        for (int i = 1; i <= 12; i++) {
            f.mesaj(la(i * 1_000L), la(i * 1_000L + i * 100L), false); // latențe de 100, 200, ..., 1200 ms
        }
        assertEquals(750.0, f.latentaMedieMs(), 1e-9); // media lui 300..1200
    }

    @Test
    void mesajeleRecuperateNuIntraInLatenta() {
        f.mesaj(la(0), la(100), false);
        f.mesaj(la(1_000), la(30_000), true);
        assertEquals(100.0, f.latentaMedieMs(), 1e-9);
    }

    @Test
    void mesajeleRecuperateIntraInMesajePeSecunda() {
        f.mesaj(la(0), la(100), false);
        f.mesaj(la(1_000), la(200), true);
        assertEquals(0.2, f.mesajePeSecunda(la(1_000)), 1e-9);
    }

    @Test
    void faraMesajeLiveLatentaLipseste() {
        assertNull(f.latentaMedieMs());
        f.mesaj(la(0), la(5_000), true);
        assertNull(f.latentaMedieMs());
    }
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru
`FereastraStatistici`.

- [ ] **Pasul 3: Scrie `FereastraStatistici`**

```java
package ro.flota.server.statistici;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Statisticile unei mașini (spec §6.5, endpoint-ul 12): câte mesaje au sosit în ultimele 10 secunde și
 * latența medie a ultimelor 10 mesaje live. Mesajele recuperate nu intră în latență: ora lor de sosire
 * spune cât a stat mașina în tunel, nu cât e de lentă rețeaua (D26).
 */
final class FereastraStatistici {

    static final Duration FEREASTRA = Duration.ofSeconds(10);
    static final int ULTIMELE = 10;

    private final Deque<Instant> sosiri = new ArrayDeque<>();
    private final Deque<Long> latente = new ArrayDeque<>();

    void mesaj(Instant oraMasinii, Instant oraSosirii, boolean dinBuffer) {
        sosiri.addLast(oraSosirii);
        if (!dinBuffer) {
            latente.addLast(Duration.between(oraMasinii, oraSosirii).toMillis());
            if (latente.size() > ULTIMELE) {
                latente.removeFirst();
            }
        }
    }

    double mesajePeSecunda(Instant acum) {
        Instant limita = acum.minus(FEREASTRA);
        while (!sosiri.isEmpty() && sosiri.peekFirst().isBefore(limita)) {
            sosiri.removeFirst();
        }
        return sosiri.size() / (double) FEREASTRA.toSeconds();
    }

    Double latentaMedieMs() {
        return latente.isEmpty() ? null : latente.stream().mapToLong(Long::longValue).average().orElseThrow();
    }
}
```

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 6 teste din `FereastraStatisticiTest`
trecute.

- [ ] **Pasul 5: Scrie restul**

`flota\ContoareDto.java`:

```java
package ro.flota.server.flota;

/** Contoarele unei mașini, fără nimic altceva: se citesc o dată pe secundă. */
public record ContoareDto(String id, long recuperate, long pierdute) {
}
```

În `Masini.java`:

```java
    /** Doar contoarele, fără poziții: se cer o dată pe secundă, pentru STATISTICI. */
    public List<ContoareDto> contoare() {
        return jdbc.sql("SELECT id, recuperate, pierdute FROM masini ORDER BY CAST(SUBSTRING(id, 8) AS UNSIGNED)")
                .query((rs, nr) -> new ContoareDto(rs.getString("id"), rs.getLong("recuperate"), rs.getLong("pierdute")))
                .list();
    }
```

`statistici\StatisticaDto.java`:

```java
package ro.flota.server.statistici;

/** Un rând din statistici (endpoint-ul 12 și STATISTICI). latentaMedieMs e null până vine un mesaj live. */
public record StatisticaDto(String masinaId, double mesajePeSecunda, Double latentaMedieMs, long recuperate, long pierdute) {
}
```

`statistici\Statistici.java`:

```java
package ro.flota.server.statistici;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ro.flota.server.flota.ContoareDto;
import ro.flota.server.flota.Masini;

/**
 * Ferestrele tuturor mașinilor. Scrie firul MQTT, iar citesc firul programat și cererile REST, deci
 * accesul la ferestre e synchronized. Interogarea bazei stă în afara lacătului.
 */
@Component
public class Statistici {

    private final Masini masini;
    private final Clock ceas;
    private final Map<String, FereastraStatistici> ferestre = new HashMap<>();

    public Statistici(Masini masini, Clock ceas) {
        this.masini = masini;
        this.ceas = ceas;
    }

    public synchronized void mesaj(String masinaId, Instant oraMasinii, Instant oraSosirii, boolean dinBuffer) {
        ferestre.computeIfAbsent(masinaId, id -> new FereastraStatistici()).mesaj(oraMasinii, oraSosirii, dinBuffer);
    }

    public List<StatisticaDto> acum() {
        Instant acum = ceas.instant();
        return masini.contoare().stream().map(c -> statistica(c, acum)).toList();
    }

    private synchronized StatisticaDto statistica(ContoareDto c, Instant acum) {
        FereastraStatistici f = ferestre.get(c.id());
        return new StatisticaDto(c.id(), f == null ? 0 : f.mesajePeSecunda(acum),
                f == null ? null : f.latentaMedieMs(), c.recuperate(), c.pierdute());
    }
}
```

`statistici\TrimitereStatistici.java`:

```java
package ro.flota.server.statistici;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.flota.server.live.CanalLive;

/** STATISTICI pe canalul live, o dată pe secundă (spec §6.6), doar dacă e cineva conectat. */
@Component
public class TrimitereStatistici {

    private final Statistici statistici;
    private final CanalLive live;

    public TrimitereStatistici(Statistici statistici, CanalLive live) {
        this.statistici = statistici;
        this.live = live;
    }

    @Scheduled(fixedRate = 1_000)
    public void trimite() {
        if (live.numarSesiuni() > 0) {
            live.trimite("STATISTICI", statistici.acum());
        }
    }
}
```

`api\StatisticiController.java`:

```java
package ro.flota.server.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.statistici.StatisticaDto;
import ro.flota.server.statistici.Statistici;

/** Endpoint-ul 12 din spec §6.5. */
@RestController
public class StatisticiController {

    private final Statistici statistici;

    public StatisticiController(Statistici statistici) {
        this.statistici = statistici;
    }

    @GetMapping("/api/statistici")
    public List<StatisticaDto> toate() {
        return statistici.acum();
    }
}
```

În `Dispecer.java`:
- importul nou e `ro.flota.server.statistici.Statistici`;
- câmpul nou e `private final Statistici statistici;`, iar constructorul îl
  primește după `RegulaOffline regulaOffline`;
- în `laTelemetrie`, după `masini.dupaTelemetrie(…)`:

```java
        statistici.mesaj(t.masinaId(), t.oraMasinii(), sosire, t.dinBuffer());
```

- [ ] **Pasul 6: Rulează testele și verifică pe viu**

Comanda: `.\mvnw.cmd -q test`, iar rezultatul așteptat e `BUILD SUCCESS`. Apoi
repornește S și, cu totul pornit:

```powershell
curl.exe -s http://127.0.0.1:8080/api/statistici
```

Se așteaptă: zece rânduri, fiecare cu `mesajePeSecunda` în jur de `1.0` și
`latentaMedieMs` de câteva milisecunde. În O, `INTARZIERE TOATE 500 200`.
După 15 secunde, cererea dă latențe în jur de 500. La final, `NORMAL TOATE`.

- [ ] **Pasul 7: Claude explică.** Analogia: un cronometru care ține minte doar
  ultimele zece ture, nu toată cursa. Fiecare tură nouă o împinge afară pe cea
  mai veche. Apoi:
  - de ce mesajele recuperate nu intră în latență;
  - de ce interogarea bazei stă în afara lui `synchronized`: un lacăt ținut cât
    durează o interogare ar opri firul MQTT;
  - de ce `STATISTICI` nu pleacă dacă nimeni nu ascultă.

- [ ] **Pasul 8: Commit**

```bash
git add server/src
git commit -m "Server: statisticile si latenta, pe REST si live

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: Legătura serverului cu proxy-ul

**Cine scrie:**
- **Clientul protocolului, traducerea cererilor și endpoint-urile 9 și 10:**
  Claude, ca exemplu explicat. E cealaltă parte a protocolului scris în faza 3.
- **Jurnalul, adică endpoint-ul 11:** Tudor, ca repetiție a unui repo și a unui
  `GET`.

**Fișiere:**
- Modifică: `server\src\main\resources\application.properties`
- Creează, în `server\src\main\java\ro\flota\server\retea\`:
  - `CerereVreme.java`, `LiniiVreme.java`, `VremeMasinaDto.java`;
  - `EvenimentDto.java`, `EvenimenteRetea.java`, `ClientProxy.java`.
- Creează: `server\src\main\java\ro\flota\server\api\ReteaController.java`
- Teste, în `server\src\test\java\ro\flota\server\retea\`:
  `LiniiVremeTest.java` și `ClientProxyTest.java`

**Interfețe:**
- Consumă: `ServiciuIndisponibil` și `CerereGresita` (`api`), `Ore` (`flota`) și
  `CanalLive`.
- Produce:
  - `record CerereVreme(String comanda, String tinta, Map<String, Integer> parametri)`,
    cu cheile de parametri `ms`, `variatie`, `kbps` și `secunde`;
  - `static String LiniiVreme.linie(CerereVreme)` și
    `static Instant LiniiVreme.sfarsit(CerereVreme, Instant start)`;
  - `record VremeMasinaDto(String masina, String descriere)`, cu
    `static din(String rand)`;
  - `record EvenimentDto(long id, String tinta, String linie, String raspuns, Instant oraStart, Instant oraSfarsit)`;
  - `EvenimenteRetea`, cu `salveaza(String tinta, String linie, String raspuns, Instant start, Instant sfarsit)`
    și `List<EvenimentDto> ultimele(int n)`;
  - `ClientProxy(String host, int port)`, cu `List<String> trimite(String linie)`,
    care aruncă `ServiciuIndisponibil` după 2 secunde fără răspuns;
  - endpoint-urile 9, 10 și 11, plus `EVENIMENT_RETEA` pe canalul live.

- [ ] **Pasul 1: Adaugă adresa proxy-ului în `application.properties`**

```properties
# Proxy-ul primește ordinele pe 1885, printr-un protocol text (spec §5.4).
flota.proxy.host=127.0.0.1
flota.proxy.port=1885
```

- [ ] **Pasul 2: Scrie testele care pică pentru traducere**

`LiniiVremeTest.java`:

```java
package ro.flota.server.retea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ro.flota.server.api.CerereGresita;

class LiniiVremeTest {

    private static String linie(String comanda, String tinta, Map<String, Integer> parametri) {
        return LiniiVreme.linie(new CerereVreme(comanda, tinta, parametri));
    }

    @Test
    void fiecareComandaDevineLiniaEi() {
        assertEquals("TUNEL masina-3 15", linie("TUNEL", "masina-3", Map.of("secunde", 15)));
        assertEquals("GAURA masina-5 10", linie("GAURA", "masina-5", Map.of("secunde", 10)));
        assertEquals("INTARZIERE TOATE 500 200", linie("INTARZIERE", "TOATE", Map.of("ms", 500, "variatie", 200)));
        assertEquals("INTARZIERE TOATE 500 0", linie("INTARZIERE", "TOATE", Map.of("ms", 500)));
        assertEquals("BANDA masina-3 64", linie("BANDA", "masina-3", Map.of("kbps", 64)));
        assertEquals("NORMAL TOATE", linie("NORMAL", "TOATE", null));
    }

    @Test
    void literelemariSauMiciSeNormalizeaza() {
        assertEquals("TUNEL TOATE 15", linie("tunel", "toate", Map.of("secunde", 15)));
        assertEquals("NORMAL masina-3", linie("normal", "Masina-3", Map.of()));
    }

    @Test
    void tintaCuSpatiiSauRandNouEsteRefuzata() {
        for (String tinta : new String[] {"masina-3\nSTARE", "masina 3", "", null, "masina-"}) {
            assertThrows(CerereGresita.class, () -> linie("NORMAL", tinta, Map.of()), String.valueOf(tinta));
        }
    }

    @Test
    void numarulLipsaEsteRefuzat() {
        assertThrows(CerereGresita.class, () -> linie("TUNEL", "masina-3", Map.of()));
        assertThrows(CerereGresita.class, () -> linie("BANDA", "masina-3", null));
    }

    @Test
    void comandaNecunoscutaEsteRefuzata() {
        assertThrows(CerereGresita.class, () -> linie("PLOAIE", "masina-3", Map.of()));
        assertThrows(CerereGresita.class, () -> linie(null, "masina-3", Map.of()));
    }

    @Test
    void intervaleleLeVerificaProxyulNuServerul() {
        assertEquals("TUNEL masina-3 9999", linie("TUNEL", "masina-3", Map.of("secunde", 9_999)));
    }

    @Test
    void sfarsitulExistaDoarLaTunelSiGaura() {
        Instant t0 = Instant.parse("2026-12-10T10:00:00Z");
        assertEquals(t0.plusSeconds(15), LiniiVreme.sfarsit(new CerereVreme("TUNEL", "masina-3", Map.of("secunde", 15)), t0));
        assertNull(LiniiVreme.sfarsit(new CerereVreme("NORMAL", "masina-3", Map.of()), t0));
    }

    @Test
    void stareaSeImparteLaPrimulSpatiu() {
        assertEquals(new VremeMasinaDto("masina-5", "INTARZIERE 500 200 BANDA 64"),
                VremeMasinaDto.din("masina-5 INTARZIERE 500 200 BANDA 64"));
        assertEquals(List.of(new VremeMasinaDto("masina-1", "NORMAL")), List.of(VremeMasinaDto.din("masina-1 NORMAL")));
    }
}
```

- [ ] **Pasul 3: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `LiniiVreme`,
`CerereVreme` și `VremeMasinaDto`.

- [ ] **Pasul 4: Scrie `CerereVreme`, `VremeMasinaDto` și `LiniiVreme`**

`CerereVreme.java`:

```java
package ro.flota.server.retea;

import java.util.Map;

/** Ce trimite pagina Rețea: {"comanda": "TUNEL", "tinta": "masina-3", "parametri": {"secunde": 15}}. */
public record CerereVreme(String comanda, String tinta, Map<String, Integer> parametri) {
}
```

`VremeMasinaDto.java`:

```java
package ro.flota.server.retea;

/** Un rând din răspunsul la STARE, despărțit în mașină și vreme: „masina-3 TUNEL 12”. */
public record VremeMasinaDto(String masina, String descriere) {

    public static VremeMasinaDto din(String rand) {
        int spatiu = rand.indexOf(' ');
        return spatiu < 0 ? new VremeMasinaDto(rand, "") : new VremeMasinaDto(rand.substring(0, spatiu), rand.substring(spatiu + 1));
    }
}
```

`LiniiVreme.java`:

```java
package ro.flota.server.retea;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import ro.flota.server.api.CerereGresita;

/**
 * Traduce o cerere din pagina Rețea într-o linie a protocolului proxy-ului (spec §5.4). Verifică doar
 * forma: comanda, ținta și numerele cerute. Intervalele le verifică proxy-ul, care răspunde cu ERR (D26).
 * Ținta e verificată strict, ca nimeni să nu poată strecura un \n și o a doua comandă în aceeași linie.
 */
public final class LiniiVreme {

    private static final Pattern TINTA = Pattern.compile("TOATE|masina-[0-9]+", Pattern.CASE_INSENSITIVE);

    private LiniiVreme() {
    }

    public static String linie(CerereVreme c) {
        if (c == null || c.comanda() == null) {
            throw new CerereGresita("lipsește comanda");
        }
        if (c.tinta() == null || !TINTA.matcher(c.tinta()).matches()) {
            throw new CerereGresita("ținta trebuie să fie TOATE sau masina-N");
        }
        String comanda = c.comanda().toUpperCase(Locale.ROOT);
        String tinta = c.tinta().equalsIgnoreCase("TOATE") ? "TOATE" : c.tinta().toLowerCase(Locale.ROOT);
        Map<String, Integer> p = c.parametri() == null ? Map.of() : c.parametri();
        return switch (comanda) {
            case "INTARZIERE" -> "INTARZIERE " + tinta + " " + numar(p, "ms") + " " + p.getOrDefault("variatie", 0);
            case "BANDA" -> "BANDA " + tinta + " " + numar(p, "kbps");
            case "TUNEL", "GAURA" -> comanda + " " + tinta + " " + numar(p, "secunde");
            case "NORMAL" -> "NORMAL " + tinta;
            default -> throw new CerereGresita("comandă de rețea necunoscută: " + c.comanda());
        };
    }

    /** Când se termină un TUNEL sau o GAURA cerute acum; null pentru celelalte comenzi. */
    public static Instant sfarsit(CerereVreme c, Instant start) {
        String comanda = c.comanda().toUpperCase(Locale.ROOT);
        if (!comanda.equals("TUNEL") && !comanda.equals("GAURA")) {
            return null;
        }
        return start.plusSeconds(c.parametri().get("secunde"));
    }

    private static int numar(Map<String, Integer> p, String cheie) {
        Integer n = p.get(cheie);
        if (n == null) {
            throw new CerereGresita("lipsește „" + cheie + "”");
        }
        return n;
    }
}
```

- [ ] **Pasul 5: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 8 teste din `LiniiVremeTest` trecute.

- [ ] **Pasul 6: Scrie testele care pică pentru client**

`ClientProxyTest.java`:

```java
package ro.flota.server.retea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ro.flota.server.api.ServiciuIndisponibil;

class ClientProxyTest {

    private ServerSocket server;
    private final List<String> primite = new CopyOnWriteArrayList<>();

    /** Un proxy fals: primește o conexiune, citește o linie și răspunde cu textul dat. */
    private ClientProxy proxyCareRaspunde(String raspuns) throws IOException {
        server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        Thread fir = new Thread(() -> {
            try (Socket s = server.accept()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII));
                primite.add(in.readLine());
                s.getOutputStream().write(raspuns.getBytes(StandardCharsets.US_ASCII));
                s.getOutputStream().flush();
                Thread.sleep(200);
            } catch (IOException | InterruptedException e) {
                // testul s-a terminat
            }
        });
        fir.setDaemon(true);
        fir.start();
        return new ClientProxy("127.0.0.1", server.getLocalPort());
    }

    @AfterEach
    void inchide() throws IOException {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void raspunsulUnuiRand() throws IOException {
        ClientProxy client = proxyCareRaspunde("OK\n");
        assertEquals(List.of("OK"), client.trimite("TUNEL masina-3 15"));
        assertEquals(List.of("TUNEL masina-3 15"), primite);
    }

    @Test
    void stareaCitestePanaLaPunctFaraEl() throws IOException {
        ClientProxy client = proxyCareRaspunde("masina-1 NORMAL\nmasina-3 TUNEL 12\n.\n");
        assertEquals(List.of("masina-1 NORMAL", "masina-3 TUNEL 12"), client.trimite("STARE"));
    }

    @Test
    void proxyOpritDa503() throws IOException {
        int port;
        try (ServerSocket liber = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = liber.getLocalPort();
        }
        ClientProxy client = new ClientProxy("127.0.0.1", port);
        assertThrows(ServiciuIndisponibil.class, () -> client.trimite("STARE"));
    }

    @Test
    void proxyCareTaceDa503InDouaSecunde() throws IOException {
        // Sistemul acceptă conexiunea din coada de așteptare, dar nimeni nu citește și nu răspunde.
        server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        ClientProxy client = new ClientProxy("127.0.0.1", server.getLocalPort());
        long inceput = System.nanoTime();
        assertThrows(ServiciuIndisponibil.class, () -> client.trimite("STARE"));
        long ms = (System.nanoTime() - inceput) / 1_000_000;
        assertTrue(ms >= 1_900 && ms < 3_500, "a durat " + ms + " ms");
    }
}
```

- [ ] **Pasul 7: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `COMPILATION ERROR`, cu `cannot find symbol` pentru `ClientProxy`.

- [ ] **Pasul 8: Scrie `ClientProxy`**

```java
package ro.flota.server.retea;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ro.flota.server.api.ServiciuIndisponibil;

/**
 * Clientul protocolului text al proxy-ului (spec §5.4, §6.5): câte o conexiune TCP pe 1885 pentru fiecare
 * cerere. Dacă proxy-ul nu răspunde în 2 secunde, aruncă ServiciuIndisponibil, care devine 503.
 */
@Component
public class ClientProxy {

    static final int TIMP_MAXIM_MS = 2_000;

    private final String host;
    private final int port;

    public ClientProxy(@Value("${flota.proxy.host}") String host, @Value("${flota.proxy.port}") int port) {
        this.host = host;
        this.port = port;
    }

    /** Trimite o linie și întoarce răspunsul: un rând, sau, la STARE, toate rândurile până la „.”, fără el. */
    public List<String> trimite(String linie) {
        boolean stare = linie.trim().equalsIgnoreCase("STARE");
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), TIMP_MAXIM_MS);
            s.setSoTimeout(TIMP_MAXIM_MS);
            Writer out = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.US_ASCII);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII));
            out.write(linie + "\n");
            out.flush();
            List<String> raspuns = new ArrayList<>();
            String rand;
            while ((rand = in.readLine()) != null) {
                if (stare && rand.equals(".")) {
                    return raspuns;
                }
                raspuns.add(rand);
                if (!stare) {
                    return raspuns;
                }
            }
            throw new ServiciuIndisponibil("proxy-ul a închis conexiunea înainte să răspundă");
        } catch (IOException e) {
            throw new ServiciuIndisponibil("proxy indisponibil: " + e.getMessage());
        }
    }
}
```

- [ ] **Pasul 9: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 4 teste din `ClientProxyTest` trecute.

- [ ] **Pasul 10: Scrie evenimentele și controller-ul**

`EvenimentDto.java`:

```java
package ro.flota.server.retea;

import java.time.Instant;

/** Un rând din jurnalul rețelei: ce linie s-a trimis proxy-ului și ce a răspuns. */
public record EvenimentDto(long id, String tinta, String linie, String raspuns, Instant oraStart, Instant oraSfarsit) {
}
```

`EvenimenteRetea.java`:

```java
package ro.flota.server.retea;

import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ro.flota.server.flota.Ore;

/** Tabelul evenimente_retea: jurnalul vremii trimise proxy-ului. */
@Repository
public class EvenimenteRetea {

    private final JdbcClient jdbc;

    public EvenimenteRetea(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public EvenimentDto salveaza(String tinta, String linie, String raspuns, Instant start, Instant sfarsit) {
        KeyHolder cheie = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO evenimente_retea (tinta, linie, raspuns, ora_start, ora_sfarsit) "
                        + "VALUES (:tinta, :linie, :raspuns, :start, :sfarsit)")
                .param("tinta", tinta)
                .param("linie", linie)
                .param("raspuns", raspuns)
                .param("start", Ore.ts(start))
                .param("sfarsit", Ore.ts(sfarsit))
                .update(cheie);
        return new EvenimentDto(cheie.getKey().longValue(), tinta, linie, raspuns, start, sfarsit);
    }
}
```

`api\ReteaController.java`:

```java
package ro.flota.server.api;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.live.CanalLive;
import ro.flota.server.retea.CerereVreme;
import ro.flota.server.retea.ClientProxy;
import ro.flota.server.retea.EvenimentDto;
import ro.flota.server.retea.EvenimenteRetea;
import ro.flota.server.retea.LiniiVreme;
import ro.flota.server.retea.VremeMasinaDto;

/** Endpoint-urile 9, 10 și 11 din spec §6.5: vremea din rețea, prin proxy. */
@RestController
@RequestMapping("/api/retea")
public class ReteaController {

    private final ClientProxy proxy;
    private final EvenimenteRetea evenimente;
    private final CanalLive live;
    private final Clock ceas;

    public ReteaController(ClientProxy proxy, EvenimenteRetea evenimente, CanalLive live, Clock ceas) {
        this.proxy = proxy;
        this.evenimente = evenimente;
        this.live = live;
        this.ceas = ceas;
    }

    /** Endpoint-ul 9: STARE, despărțit pe mașini. */
    @GetMapping("/stare")
    public List<VremeMasinaDto> stare() {
        return proxy.trimite("STARE").stream().map(VremeMasinaDto::din).toList();
    }

    /** Endpoint-ul 10: construiește linia, o trimite, salvează evenimentul și întoarce răspunsul, OK sau ERR. */
    @PostMapping("/vreme")
    public EvenimentDto vreme(@RequestBody CerereVreme cerere) {
        String linie = LiniiVreme.linie(cerere);
        Instant start = ceas.instant();
        String raspuns = proxy.trimite(linie).get(0);
        Instant sfarsit = raspuns.equals("OK") ? LiniiVreme.sfarsit(cerere, start) : null;
        EvenimentDto eveniment = evenimente.salveaza(linie.split(" ")[1], linie, raspuns, start, sfarsit);
        live.trimite("EVENIMENT_RETEA", eveniment);
        return eveniment;
    }
}
```

- [ ] **Pasul 11: Verifică endpoint-urile 9 și 10**

Totul pornit. Fișierul `docs\mesaje-test\vreme-tunel.json`:

```json
{"comanda": "TUNEL", "tinta": "masina-3", "parametri": {"secunde": 15}}
```

și `docs\mesaje-test\vreme-gresita.json`:

```json
{"comanda": "TUNEL", "tinta": "masina-3", "parametri": {"secunde": 9999}}
```

```powershell
curl.exe -s http://127.0.0.1:8080/api/retea/stare
curl.exe -s -X POST -H "Content-Type: application/json" --data-binary "@docs/mesaje-test/vreme-tunel.json" http://127.0.0.1:8080/api/retea/vreme
curl.exe -s -X POST -H "Content-Type: application/json" --data-binary "@docs/mesaje-test/vreme-gresita.json" http://127.0.0.1:8080/api/retea/vreme
```

Se așteaptă:
- zece obiecte `{"masina":"masina-N","descriere":"NORMAL"}`;
- un eveniment cu `"raspuns":"OK"` și `oraSfarsit` la 15 secunde după
  `oraStart`, iar `masina-3` devine gri pe hartă;
- un eveniment cu `"raspuns":"ERR parametri gresiti"`.

Oprește P și rulează din nou prima comandă, cu `-i`. Se așteaptă:
`HTTP/1.1 503`, cu `proxy indisponibil: …`, în cel mult 2 secunde. Pornește P la
loc.

- [ ] **Pasul 12: Commit-ul lui Claude**

```bash
git add server/src docs/mesaje-test
git commit -m "Server: legatura cu proxy-ul, prin protocolul text

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Pasul 13: Claude explică.** Analogia: serverul vorbește acum cu proxy-ul
  la telefonul fix de pe 1885, ca scriptul din faza 3, doar că din Java. Dacă
  nu răspunde nimeni în 2 secunde, închide și îi spune UI-ului „indisponibil”,
  în loc să-l țină agățat. Apoi:
  - de ce ținta e verificată cu o expresie regulată: un `\n` strecurat ar trimite
    a doua comandă;
  - de ce `ERR` întoarce 200: e un răspuns valid al protocolului (D26);
  - cum testează `proxyCareTaceDa503InDouaSecunde` un server care nu răspunde.

- [ ] **Pasul 14: Tudor scrie jurnalul, endpoint-ul 11.** Ce trebuie:
  - în `EvenimenteRetea`, `List<EvenimentDto> ultimele(int n)`: cele mai noi
    `n`, după `id`, descrescător;
  - în `ReteaController`, `GET /api/retea/evenimente`, care întoarce ultimele
    50.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

În `EvenimenteRetea`, cu importul `java.util.List`:

```java
    public List<EvenimentDto> ultimele(int n) {
        return jdbc.sql("SELECT * FROM evenimente_retea ORDER BY id DESC LIMIT :n")
                .param("n", n)
                .query((rs, nr) -> new EvenimentDto(rs.getLong("id"), rs.getString("tinta"), rs.getString("linie"),
                        rs.getString("raspuns"), Ore.instant(rs.getTimestamp("ora_start")),
                        Ore.instant(rs.getTimestamp("ora_sfarsit"))))
                .list();
    }
```

În `ReteaController`:

```java
    /** Endpoint-ul 11: jurnalul evenimentelor de rețea. */
    @GetMapping("/evenimente")
    public List<EvenimentDto> evenimente() {
        return evenimente.ultimele(50);
    }
```

</details>

- [ ] **Pasul 15: Verifică jurnalul**

Repornește S, apoi `curl.exe -s http://127.0.0.1:8080/api/retea/evenimente`. Se
așteaptă: cele două evenimente de la pasul 11, cel mai nou primul.

- [ ] **Pasul 16: Claude revizuiește,** fără să rescrie.

- [ ] **Pasul 17: Commit-ul lui Tudor**

```bash
git add server/src
git commit -m "Server: jurnalul evenimentelor de retea"
```

---

### Task 10: Alarmele în REST

**Cine scrie:** Tudor, ca repetiție a repo-urilor și a controller-elor. Claude
revizuiește. Task-ul n-are logică nouă de testat automat; se verifică cu
`curl.exe`.

**Fișiere:**
- Modifică: `server\src\main\java\ro\flota\server\flota\Alarme.java`
- Creează: `server\src\main\java\ro\flota\server\api\AlarmeController.java`

**Interfețe:**
- Consumă: `AlarmaDto`, `Ore` și `Negasit`.
- Produce:
  - în `Alarme`: `List<AlarmaDto> lista(Boolean vazute)`, unde `null` înseamnă
    toate, și `Optional<AlarmaDto> marcheazaVazuta(long id)`;
  - endpoint-urile 4 (`GET /api/alarme?vazute=`) și 5 (`PATCH /api/alarme/{id}`,
    fără corp, D26).

- [ ] **Pasul 1: Tudor scrie repo-ul și controller-ul.** Ce trebuie:
  - **`Alarme.lista(Boolean vazute)`:**
    - cele mai noi 200, după `ora_sosirii`, apoi `id`, descrescător;
    - cu `WHERE vazuta = :vazute` doar când `vazute` nu e `null`;
    - transformarea rândului într-un `AlarmaDto` stă într-o metodă `rand`,
      după forma celei din `Pozitii`;
  - **`Alarme.marcheazaVazuta(long id)`:** un `UPDATE`, apoi rândul citit din
    nou;
  - **`AlarmeController`:**
    - `GET` cu `@RequestParam(required = false) Boolean vazute`;
    - `PATCH /{id}`, care dă 404 cu `Negasit` dacă alarma nu există.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

În `Alarme.java`, cu importurile `java.sql.ResultSet`, `java.sql.SQLException`,
`java.util.List` și `java.util.Optional`:

```java
    public List<AlarmaDto> lista(Boolean vazute) {
        String unde = vazute == null ? "" : "WHERE vazuta = :vazute ";
        JdbcClient.StatementSpec cerere = jdbc.sql("SELECT * FROM alarme " + unde + "ORDER BY ora_sosirii DESC, id DESC LIMIT 200");
        if (vazute != null) {
            cerere = cerere.param("vazute", vazute);
        }
        return cerere.query(Alarme::rand).list();
    }

    public Optional<AlarmaDto> marcheazaVazuta(long id) {
        jdbc.sql("UPDATE alarme SET vazuta = TRUE WHERE id = :id").param("id", id).update();
        return jdbc.sql("SELECT * FROM alarme WHERE id = :id").param("id", id).query(Alarme::rand).optional();
    }

    private static AlarmaDto rand(ResultSet rs, int nr) throws SQLException {
        return new AlarmaDto(rs.getLong("id"), rs.getString("masina_id"), rs.getString("tip"), rs.getString("sursa"),
                Ore.instant(rs.getTimestamp("ora")), Ore.instant(rs.getTimestamp("ora_sosirii")),
                rs.getString("detalii"), rs.getBoolean("vazuta"));
    }
```

`api\AlarmeController.java`:

```java
package ro.flota.server.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.flota.AlarmaDto;
import ro.flota.server.flota.Alarme;

/** Endpoint-urile 4 și 5 din spec §6.5. */
@RestController
@RequestMapping("/api/alarme")
public class AlarmeController {

    private final Alarme alarme;

    public AlarmeController(Alarme alarme) {
        this.alarme = alarme;
    }

    @GetMapping
    public List<AlarmaDto> lista(@RequestParam(required = false) Boolean vazute) {
        return alarme.lista(vazute);
    }

    @PatchMapping("/{id}")
    public AlarmaDto vazuta(@PathVariable long id) {
        return alarme.marcheazaVazuta(id).orElseThrow(() -> new Negasit("alarma " + id + " nu există"));
    }
}
```

</details>

- [ ] **Pasul 2: Rulează testele și verifică**

Comanda: `.\mvnw.cmd -q test`, iar rezultatul așteptat e `BUILD SUCCESS`. Repornește
S, apoi:

```powershell
curl.exe -s "http://127.0.0.1:8080/api/alarme?vazute=false"
curl.exe -s -X PATCH http://127.0.0.1:8080/api/alarme/1
curl.exe -s "http://127.0.0.1:8080/api/alarme?vazute=true"
curl.exe -s -i -X PATCH http://127.0.0.1:8080/api/alarme/999999
```

Se așteaptă:
- alarmele nevăzute, cea mai nouă prima;
- alarma 1 cu `"vazuta":true`;
- alarma 1 în lista celor văzute;
- `HTTP/1.1 404`.

- [ ] **Pasul 3: Claude revizuiește,** fără să rescrie.

- [ ] **Pasul 4: Commit-ul lui Tudor**

```bash
git add server/src
git commit -m "Server: lista alarmelor si marcarea ca vazuta"
```

---

### Task 11: Paginile și pagina Rețea

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „o pagină de comandă care
vorbește cu serverul prin REST și se ține la zi din canalul live”.

**Fișiere:**
- Modifică: `web\package.json`, `web\package-lock.json`, prin `npm install`
- Creează: `web\src\router.js`, `web\src\pagini\PaginaRetea.vue`
- Modifică: `web\src\main.js`, `App.vue`, `api.js`, `live.js`
- Modifică: `web\src\components\HartaFlotei.vue`

**Interfețe:**
- Consumă: endpoint-urile 9, 10 și 11 (Task 9), plus `EVENIMENT_RETEA` de pe
  canalul live.
- Produce:
  - în `api.js`: `stareRetea()`, `vreme(cerere)` și `evenimente()`;
  - în `live.js`:
    - pe tabla `flota`, câmpurile `vreme`, de forma `{ [id]: descriere }`, și
      `proxyDisponibil`;
    - funcțiile `actualizeazaVremea()` și `pornesteVremea()`;
  - `router`, cu `/` pentru Harta și `/retea` pentru Rețea;
  - harta colorează cu portocaliu o mașină online care are vreme setată.

- [ ] **Pasul 1: Instalează vue-router, după ce Tudor spune da**

Claude întreabă: „Instalez vue-router 5.4.0 de pe registry.npmjs.org, câteva sute
de KB. E bine?”. Dacă da, în W, din `web\`:

```powershell
npm install vue-router@5.4.0 --save-exact
```

Se așteaptă: în `package.json`, la `dependencies`, `"vue-router": "5.4.0"`.

- [ ] **Pasul 2: Scrie `web\src\router.js` și schimbă `main.js`**

`router.js`:

```js
import { createRouter, createWebHistory } from 'vue-router'
import PaginaHarta from './pagini/PaginaHarta.vue'
import PaginaRetea from './pagini/PaginaRetea.vue'

/** Paginile din spec §7. Pagina Alarme intră în Task 13. */
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: PaginaHarta },
    { path: '/retea', component: PaginaRetea },
  ],
})
```

În `main.js`, importul nou e `import { router } from './router.js'`, iar ultimul
rând devine:

```js
createApp(App).use(router).use(createVuetify({ components, directives })).mount('#app')
```

- [ ] **Pasul 3: Adaugă cererile de rețea în `api.js`**

În obiectul `api`, după `zone`:

```js
  stareRetea: () => cere('/api/retea/stare'),
  vreme: (cerere) => cere('/api/retea/vreme', { method: 'POST', body: JSON.stringify(cerere) }),
  evenimente: () => cere('/api/retea/evenimente'),
```

- [ ] **Pasul 4: Rescrie `live.js`, cu vremea**

```js
import { reactive } from 'vue'
import { api } from './api.js'

/** Tabla dispecerului: ce știe UI-ul acum despre flotă și despre rețea. O citesc toate componentele. */
export const flota = reactive({
  conectat: false,
  masini: {}, // id → { id, zonaId, stare, ultimaVedere, recuperate, pierdute, pozitie }
  vreme: {}, // id → descrierea de la proxy, de exemplu „TUNEL 12”
  proxyDisponibil: true,
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

/** Contoarele unei mașini, din REST. Până la Task 12, panoul le cere la 2 secunde (D25). */
export async function actualizeazaContoare(id) {
  const m = await api.masina(id)
  const tabla = flota.masini[id]
  if (tabla) {
    tabla.recuperate = m.recuperate
    tabla.pierdute = m.pierdute
  }
}

/** Vremea fiecărei mașini, din STARE-ul proxy-ului. Dacă proxy-ul nu răspunde, o spune (spec §6.5). */
export async function actualizeazaVremea() {
  try {
    const vreme = {}
    for (const r of await api.stareRetea()) vreme[r.masina] = r.descriere
    flota.vreme = vreme
    flota.proxyDisponibil = true
  } catch {
    flota.proxyDisponibil = false
  }
}

/** STARE la 3 secunde: tunelurile și găurile se termină singure, fără vreun eveniment (D26). */
export function pornesteVremea() {
  actualizeazaVremea()
  setInterval(actualizeazaVremea, 3000)
}

/** Deschide canalul live. Dacă se închide, inclusiv când serverul e oprit, reîncearcă la 2 secunde. */
export function pornesteLive() {
  const ws = new WebSocket(`ws://${location.host}/ws`)
  ws.onopen = () => {
    flota.conectat = true
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

function aplica(tip, date) {
  switch (tip) {
    case 'POZITIE':
      aplicaPozitie(date)
      break
    case 'STARE':
      aplicaStare(date)
      break
    case 'EVENIMENT_RETEA':
      actualizeazaVremea()
      break
  }
}

/** Orele vin ca text ISO; se compară ca momente, nu ca text: „03Z” și „03.120Z” s-ar încurca. */
function maiNoua(a, b) {
  return !b || Date.parse(a) >= Date.parse(b)
}

function aplicaPozitie(date) {
  const m = flota.masini[date.masinaId]
  if (!m) return
  // O poziție recuperată din buffer e mai veche; markerul rămâne pe cea mai nouă.
  if (!m.pozitie || maiNoua(date.oraMasinii, m.pozitie.oraMasinii)) m.pozitie = date
  m.ultimaVedere = date.oraSosirii
}

function aplicaStare(date) {
  const m = flota.masini[date.masinaId]
  if (!m) return
  m.stare = date.stare
  m.ultimaVedere = date.ultimaVedere
}
```

- [ ] **Pasul 5: Portocaliul pe hartă**

În `HartaFlotei.vue`, `CULORI` devine:

```js
const CULORI = { ONLINE: '#2e7d32', OFFLINE: '#9e9e9e', VREME: '#ef6c00' }
```

în `watch`:

```js
    'flota.vreme': {
      deep: true,
      handler() {
        this.deseneazaMasini()
      },
    },
```

iar `culoare`:

```js
    /** Verde online, gri offline, portocaliu dacă e online, dar are vreme setată (spec §7). */
    culoare(m) {
      const vreme = flota.vreme[m.id]
      if (m.stare === 'ONLINE' && vreme && vreme !== 'NORMAL') return CULORI.VREME
      return CULORI[m.stare] ?? CULORI.OFFLINE
    },
```

- [ ] **Pasul 6: Rescrie `App.vue`, cu navigarea**

```vue
<template>
  <v-app>
    <v-app-bar color="primary" density="compact">
      <v-app-bar-title>Dispecerat flotă</v-app-bar-title>
      <v-btn to="/" exact>Harta</v-btn>
      <v-btn to="/retea">Rețea</v-btn>
      <template #append>
        <v-chip :color="flota.conectat ? 'success' : 'error'" variant="flat" size="small" class="mr-2">
          {{ flota.conectat ? 'live' : 'fără legătură cu serverul' }}
        </v-chip>
      </template>
    </v-app-bar>
    <v-main>
      <router-view />
    </v-main>
  </v-app>
</template>

<script>
import { flota, pornesteLive, pornesteVremea } from './live.js'

export default {
  name: 'App',
  data() {
    return { flota }
  },
  mounted() {
    pornesteLive()
    pornesteVremea()
  },
}
</script>
```

- [ ] **Pasul 7: Scrie `web\src\pagini\PaginaRetea.vue`**

Controalele pentru bandă și variație sînt primele care se taie dacă timpul nu
ajunge (spec §8). Fiecare stă într-un bloc marcat, ca să poată fi scos întreg.

```vue
<template>
  <v-container fluid>
    <v-row>
      <v-col cols="12" md="5">
        <v-card variant="outlined">
          <v-card-title>Vremea din rețea</v-card-title>
          <v-card-text>
            <v-select v-model="tinta" :items="tinte" label="Pentru" density="compact" />

            <div class="d-flex ga-2 align-center">
              <v-text-field v-model.number="secunde" type="number" label="Secunde (1–600)" density="compact" hide-details />
              <v-btn color="error" @click="trimite('TUNEL', { secunde })">Tunel</v-btn>
              <v-btn color="warning" @click="trimite('GAURA', { secunde })">Gaură</v-btn>
            </div>

            <v-slider v-model="ms" :min="0" :max="3000" :step="50" label="Întârziere, ms" thumb-label class="mt-4" />
            <!-- se taie întâi (spec §8): variația -->
            <v-slider v-model="variatie" :min="0" :max="1000" :step="50" label="Variație, ms" thumb-label />
            <v-btn @click="trimite('INTARZIERE', { ms, variatie })">Aplică întârzierea</v-btn>

            <!-- se taie întâi (spec §8): banda -->
            <div class="d-flex ga-2 align-center mt-4">
              <v-text-field v-model.number="kbps" type="number" label="Bandă, kbps (1–100.000)" density="compact" hide-details />
              <v-btn @click="trimite('BANDA', { kbps })">Aplică banda</v-btn>
            </div>

            <v-btn color="success" class="mt-4" block @click="trimite('NORMAL', {})">Normal</v-btn>
            <v-alert v-if="raspuns" :type="raspuns.ok ? 'success' : 'error'" density="compact" class="mt-4">
              {{ raspuns.text }}
            </v-alert>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="7">
        <v-card variant="outlined">
          <v-card-title class="d-flex align-center">
            Starea proxy-ului
            <v-spacer />
            <v-chip :color="flota.proxyDisponibil ? 'success' : 'error'" size="small" variant="flat">
              {{ flota.proxyDisponibil ? 'proxy disponibil' : 'proxy indisponibil' }}
            </v-chip>
          </v-card-title>
          <v-table density="compact">
            <thead>
              <tr><th>Mașina</th><th>Vremea</th></tr>
            </thead>
            <tbody>
              <tr v-for="(descriere, masina) in flota.vreme" :key="masina">
                <td>{{ masina }}</td>
                <td>{{ descriere }}</td>
              </tr>
            </tbody>
          </v-table>
        </v-card>

        <v-card variant="outlined" class="mt-4">
          <v-card-title>Jurnalul rețelei</v-card-title>
          <v-table density="compact">
            <thead>
              <tr><th>Ora</th><th>Linia trimisă</th><th>Răspunsul</th></tr>
            </thead>
            <tbody>
              <tr v-for="e in evenimente" :key="e.id">
                <td>{{ ora(e.oraStart) }}</td>
                <td><code>{{ e.linie }}</code></td>
                <td>{{ e.raspuns }}</td>
              </tr>
            </tbody>
          </v-table>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import { api } from '../api.js'
import { flota, asculta } from '../live.js'

export default {
  name: 'PaginaRetea',
  data() {
    return { flota, tinta: 'TOATE', secunde: 15, ms: 500, variatie: 200, kbps: 64, raspuns: null, evenimente: [] }
  },
  computed: {
    tinte() {
      const masini = Object.keys(flota.masini).sort((a, b) => Number(a.split('-')[1]) - Number(b.split('-')[1]))
      return ['TOATE', ...masini]
    },
  },
  async mounted() {
    this.opresteAscultarea = asculta((tip, date) => {
      if (tip === 'EVENIMENT_RETEA') this.evenimente.unshift(date)
    })
    this.evenimente = await api.evenimente().catch(() => [])
  },
  beforeUnmount() {
    this.opresteAscultarea()
  },
  methods: {
    /** Trimite o cerere de vreme. OK sau ERR vin de la proxy; o eroare HTTP înseamnă proxy indisponibil. */
    async trimite(comanda, parametri) {
      this.raspuns = null
      try {
        const e = await api.vreme({ comanda, tinta: this.tinta, parametri })
        this.raspuns = { ok: e.raspuns === 'OK', text: `${e.linie} → ${e.raspuns}` }
      } catch (err) {
        this.raspuns = { ok: false, text: err.message }
      }
    },
    ora(iso) {
      return new Date(iso).toLocaleTimeString('ro-RO')
    },
  },
}
</script>
```

- [ ] **Pasul 8: Verifică pagina**

Totul pornit. Reîncarcă UI-ul. Se așteaptă:
- în bară, butoanele „Harta” și „Rețea”;
- pe Rețea, tabelul cu zece rânduri `NORMAL` și chip-ul „proxy disponibil”;
- cu ținta `masina-3` și 15 secunde, „Tunel” dă `TUNEL masina-3 15 → OK`. Pe
  hartă, `masina-3` devine gri. În tabel, `TUNEL` cu secundele care scad la
  fiecare 3 secunde, iar în jurnal apare rândul;
- cu ținta `masina-5`, „Aplică întârzierea”: pe hartă, `masina-5` devine
  portocalie. „Normal” o face verde la loc;
- cu P oprit, chip-ul devine „proxy indisponibil” în cel mult 3 secunde, iar
  „Tunel” dă eroarea cu 503. Pornește P la loc.

- [ ] **Pasul 9: Claude explică.** Analogia: pagina Rețea e panoul
  tehnicianului de la antenă. Butoanele trimit ordine pe telefonul fix, iar
  tabelul e ce spune tehnicianul când e întrebat „cum stăm?”, la 3 secunde.
  Apoi:
  - router-ul: o singură pagină HTML, cu mai multe „ecrane” alese după adresă;
  - de ce vremea se citește periodic, deși vine și pe canalul live: tunelurile
    se termină singure, fără vreun eveniment (D26);
  - de ce `ERR` apare ca răspuns, nu ca eroare de rețea.

- [ ] **Pasul 10: Commit**

```bash
git add web
git commit -m "UI: router si pagina Retea, cu vremea pe harta

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 12: Statisticile și graficul de latență în UI

**Cine scrie:**
- **Statisticile pe tablă, graficul SVG și curățenia din panou:** Claude, ca
  exemplu explicat.
- **Tabelul cu statistici pe mașină:** Tudor, ca repetiție a unui tabel din
  tabla live.

**Fișiere:**
- Modifică: `web\src\live.js`
- Creează: `web\src\components\GraficLatenta.vue`
- Modifică: `web\src\pagini\PaginaRetea.vue`, `web\src\components\PanouMasina.vue`

**Interfețe:**
- Consumă: `STATISTICI` de pe canalul live (Task 8).
- Produce:
  - pe tabla `flota`:
    - `statistici`, adică ultima listă de `StatisticaDto`;
    - `latentaFlotei`, adică ultimele 60 de medii, una pe secundă, cu `null`
      unde nu e nicio valoare;
  - `recuperate` și `pierdute` actualizate pe fiecare mașină, o dată pe
    secundă;
  - `GraficLatenta`, cu prop-urile `valori` (Array) și `puncte` (Number,
    implicit 60).

- [ ] **Pasul 1: Statisticile pe tablă, în `live.js`**

1. În `flota`, după `proxyDisponibil`:

   ```js
     statistici: [], // ultima listă primită pe STATISTICI
     latentaFlotei: [], // media flotei, una pe secundă, ultimele 60
   ```

2. Funcția `actualizeazaContoare` se șterge: contoarele vin acum pe
   `STATISTICI`.
3. În `aplica`, cazul nou:

   ```js
       case 'STATISTICI':
         aplicaStatistici(date)
         break
   ```

4. La sfârșitul fișierului:

   ```js
   const SECUNDE_GRAFIC = 60

   /** O dată pe secundă: contoarele fiecărei mașini și un punct nou în graficul de latență. */
   function aplicaStatistici(lista) {
     flota.statistici = lista
     for (const s of lista) {
       const m = flota.masini[s.masinaId]
       if (m) {
         m.recuperate = s.recuperate
         m.pierdute = s.pierdute
       }
     }
     const latente = lista.map((s) => s.latentaMedieMs).filter((v) => v != null)
     flota.latentaFlotei.push(latente.length ? latente.reduce((a, b) => a + b, 0) / latente.length : null)
     if (flota.latentaFlotei.length > SECUNDE_GRAFIC) flota.latentaFlotei.shift()
   }
   ```

- [ ] **Pasul 2: Scoate cronometrul din `PanouMasina.vue`**

Din `PanouMasina.vue` se scot:
- importul lui `actualizeazaContoare`;
- `computed.idMasina`, tot `watch`-ul și `beforeUnmount`.

Rândurile „Recuperate” și „Pierdute” rămân: acum le ține la zi `STATISTICI`.

- [ ] **Pasul 3: Scrie `web\src\components\GraficLatenta.vue`**

```vue
<template>
  <svg :viewBox="`0 0 ${LATIME} ${INALTIME}`" class="grafic" role="img" aria-label="Latența medie, ultimele 60 de secunde">
    <g v-for="g in grila" :key="g.valoare">
      <line :x1="STANGA" :x2="LATIME" :y1="g.y" :y2="g.y" class="grila" />
      <text :x="STANGA - 6" :y="g.y + 4" class="eticheta" text-anchor="end">{{ g.valoare }}</text>
    </g>
    <polyline :points="linie" class="linie" />
  </svg>
</template>

<script>
const LATIME = 600
const INALTIME = 160
const STANGA = 44
const SUS = 10
const JOS = 10

/**
 * Graficul de latență, desenat de mână în SVG, fără bibliotecă (spec §7). Ultima valoare stă la dreapta;
 * axa verticală se rotunjește în sus la sute de milisecunde.
 */
export default {
  name: 'GraficLatenta',
  props: {
    valori: { type: Array, default: () => [] },
    puncte: { type: Number, default: 60 },
  },
  data() {
    return { LATIME, INALTIME, STANGA }
  },
  computed: {
    plafon() {
      const maxim = Math.max(100, ...this.valori.filter((v) => v != null))
      return Math.ceil(maxim / 100) * 100
    },
    grila() {
      return [0, 0.5, 1].map((f) => ({ valoare: Math.round(this.plafon * f), y: this.y(this.plafon * f) }))
    },
    linie() {
      const pas = (LATIME - STANGA) / (this.puncte - 1)
      const decalaj = this.puncte - this.valori.length
      return this.valori
        .map((v, i) => (v == null ? null : `${STANGA + (decalaj + i) * pas},${this.y(v)}`))
        .filter(Boolean)
        .join(' ')
    },
  },
  methods: {
    y(valoare) {
      return SUS + (INALTIME - SUS - JOS) * (1 - valoare / this.plafon)
    },
  },
}
</script>

<style scoped>
.grafic {
  width: 100%;
  height: 160px;
}
.grila {
  stroke: rgba(0, 0, 0, 0.12);
}
.eticheta {
  font-size: 11px;
  fill: rgba(0, 0, 0, 0.6);
}
.linie {
  fill: none;
  stroke: #1565c0;
  stroke-width: 2;
}
</style>
```

- [ ] **Pasul 4: Pune graficul în `PaginaRetea.vue`**

Importul nou e `import GraficLatenta from '../components/GraficLatenta.vue'`, iar
în export apare `components: { GraficLatenta },`. În șablon, în prima coloană,
după cardul „Vremea din rețea”:

```vue
        <v-card variant="outlined" class="mt-4">
          <v-card-title>Latența medie a flotei, ultimele 60 de secunde</v-card-title>
          <v-card-text>
            <GraficLatenta :valori="flota.latentaFlotei" />
          </v-card-text>
        </v-card>
```

- [ ] **Pasul 5: Verifică graficul**

Pe Rețea, linia graficului stă aproape de 0. Cu ținta `TOATE`, întârzierea 500 și
variația 200, „Aplică întârzierea”. În vreo 10 secunde, linia urcă la vreo 500.
„Normal” o coboară la loc. În panoul unei mașini de pe Hartă, „Recuperate” se
schimbă după un tunel fără reîncărcare.

- [ ] **Pasul 6: Commit-ul lui Claude**

```bash
git add web/src
git commit -m "UI: statisticile live si graficul de latenta in SVG

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Pasul 7: Claude explică.** Analogia: graficul e o bandă de hârtie care
  iese dintr-un aparat, o coloană pe secundă. Cea mai nouă e la dreapta, iar ce
  iese din cadru la stânga se uită. Apoi:
  - cum se transformă o latență în coordonata `y`: sus e plafonul, jos e 0;
  - `viewBox`: desenul se scalează singur la lățimea cardului;
  - de ce nu e nevoie de bibliotecă de grafice pentru o singură linie.

- [ ] **Pasul 8: Tudor scrie tabelul cu statistici.** Ce trebuie:
  - în `PaginaRetea.vue`, în a doua coloană, după cardul „Starea proxy-ului”,
    un card „Live, pe mașină” cu un `v-table`;
  - coloanele: mașina, mesaje pe secundă (cu o zecimală), latența în ms
    (rotunjită, sau `—` dacă e `null`), recuperate și pierdute;
  - rândurile vin din `flota.statistici`, deja în ordinea numerelor.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

```vue
        <v-card variant="outlined" class="mt-4">
          <v-card-title>Live, pe mașină</v-card-title>
          <v-table density="compact">
            <thead>
              <tr><th>Mașina</th><th>Mesaje/s</th><th>Latența, ms</th><th>Recuperate</th><th>Pierdute</th></tr>
            </thead>
            <tbody>
              <tr v-for="s in flota.statistici" :key="s.masinaId">
                <td>{{ s.masinaId }}</td>
                <td>{{ s.mesajePeSecunda.toFixed(1) }}</td>
                <td>{{ s.latentaMedieMs == null ? '—' : Math.round(s.latentaMedieMs) }}</td>
                <td>{{ s.recuperate }}</td>
                <td>{{ s.pierdute }}</td>
              </tr>
            </tbody>
          </v-table>
        </v-card>
```

</details>

- [ ] **Pasul 9: Verifică tabelul**

Pe Rețea, tabelul are zece rânduri cu vreo `1.0` mesaje pe secundă. După un
`GAURA masina-5 10`, la `masina-5` cresc „Pierdute” și „Recuperate”.

- [ ] **Pasul 10: Claude revizuiește,** fără să rescrie.

- [ ] **Pasul 11: Commit-ul lui Tudor**

```bash
git add web/src/pagini/PaginaRetea.vue
git commit -m "UI: tabelul cu statistici pe masina"
```

---

### Task 13: Pagina Alarme și notificările

**Cine scrie:**
- **Pagina Alarme:** Tudor, ca repetiție a unei pagini cu listă din REST, ținută
  la zi din canalul live.
- **Notificările de pe orice pagină:** Claude, ca exemplu explicat.

**Fișiere:**
- Creează: `web\src\pagini\PaginaAlarme.vue`
- Modifică: `web\src\api.js`, `web\src\router.js`, `web\src\App.vue`

**Interfețe:**
- Consumă: endpoint-urile 4 și 5 (Task 10) și `ALARMA` de pe canalul live.
- Produce:
  - în `api.js`: `alarme(vazute)`, unde `vazute` e `true`, `false` sau `null`
    pentru toate, și `marcheazaVazuta(id)`;
  - ruta `/alarme`.

- [ ] **Pasul 1: Tudor scrie pagina.** Ce trebuie:
  - **în `api.js`,** cele două cereri. `PATCH`-ul n-are corp;
  - **`PaginaAlarme.vue`:**
    - un `v-btn-toggle` cu „Nevăzute” (implicit), „Văzute” și „Toate”, care
      reîncarcă lista;
    - un `v-table` cu ora mașinii, mașina, tipul, sursa, detaliile și un buton
      „Am văzut” la cele nevăzute;
    - o alarmă nouă de pe canalul live intră prima în listă, cât filtrul nu e
      „Văzute”;
    - ascultarea se oprește în `beforeUnmount`;
  - **în `router.js`,** ruta `/alarme`.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

În `api.js`:

```js
  alarme: (vazute) => cere('/api/alarme' + (vazute == null ? '' : `?vazute=${vazute}`)),
  marcheazaVazuta: (id) => cere(`/api/alarme/${id}`, { method: 'PATCH' }),
```

În `router.js`, importul și ruta:

```js
import PaginaAlarme from './pagini/PaginaAlarme.vue'
```

```js
    { path: '/alarme', component: PaginaAlarme },
```

`PaginaAlarme.vue`:

```vue
<template>
  <v-container>
    <div class="d-flex align-center mb-4">
      <h2 class="text-h6">Alarme</h2>
      <v-spacer />
      <v-btn-toggle v-model="filtru" mandatory density="compact" @update:model-value="incarca">
        <v-btn value="nevazute">Nevăzute</v-btn>
        <v-btn value="vazute">Văzute</v-btn>
        <v-btn value="toate">Toate</v-btn>
      </v-btn-toggle>
    </div>
    <v-alert v-if="eroare" type="error" class="mb-4">{{ eroare }}</v-alert>
    <v-table density="compact">
      <thead>
        <tr><th>Ora mașinii</th><th>Mașina</th><th>Tip</th><th>Sursa</th><th>Detalii</th><th></th></tr>
      </thead>
      <tbody>
        <tr v-for="a in alarme" :key="a.id">
          <td>{{ ora(a.ora) }}</td>
          <td>{{ a.masinaId }}</td>
          <td>{{ a.tip }}</td>
          <td>{{ a.sursa }}</td>
          <td>{{ a.detalii }}</td>
          <td><v-btn v-if="!a.vazuta" size="small" variant="text" @click="vazuta(a)">Am văzut</v-btn></td>
        </tr>
        <tr v-if="alarme.length === 0">
          <td colspan="6">Nicio alarmă.</td>
        </tr>
      </tbody>
    </v-table>
  </v-container>
</template>

<script>
import { api } from '../api.js'
import { asculta } from '../live.js'

const FILTRE = { nevazute: false, vazute: true, toate: null }

export default {
  name: 'PaginaAlarme',
  data() {
    return { filtru: 'nevazute', alarme: [], eroare: null }
  },
  async mounted() {
    this.opresteAscultarea = asculta((tip, date) => {
      if (tip === 'ALARMA' && this.filtru !== 'vazute') this.alarme.unshift(date)
    })
    await this.incarca()
  },
  beforeUnmount() {
    this.opresteAscultarea()
  },
  methods: {
    async incarca() {
      try {
        this.alarme = await api.alarme(FILTRE[this.filtru])
        this.eroare = null
      } catch (e) {
        this.eroare = e.message
      }
    },
    async vazuta(alarma) {
      const noua = await api.marcheazaVazuta(alarma.id)
      if (this.filtru === 'nevazute') {
        this.alarme = this.alarme.filter((a) => a.id !== alarma.id)
      } else {
        Object.assign(alarma, noua)
      }
    },
    ora(iso) {
      return new Date(iso).toLocaleString('ro-RO')
    },
  },
}
</script>
```

</details>

- [ ] **Pasul 2: Verifică pagina**

Deschide http://127.0.0.1:5173/alarme. Se așteaptă: alarmele nevăzute. „Am văzut”
scoate una din listă, iar pe „Văzute” ea apare. Cu filtrul „Nevăzute”, o alarmă
nouă apare sus fără reîncărcare. Ca să vină una repede, trimite
`docs\mesaje-test\alarma.json` cu `mosquitto_pub`, ca în faza 2.

- [ ] **Pasul 3: Claude revizuiește,** fără să rescrie.

- [ ] **Pasul 4: Commit-ul lui Tudor**

```bash
git add web/src
git commit -m "UI: pagina Alarme"
```

- [ ] **Pasul 5: Claude adaugă notificările și butonul din bară**

`App.vue` devine:

```vue
<template>
  <v-app>
    <v-app-bar color="primary" density="compact">
      <v-app-bar-title>Dispecerat flotă</v-app-bar-title>
      <v-btn to="/" exact>Harta</v-btn>
      <v-btn to="/retea">Rețea</v-btn>
      <v-btn to="/alarme">Alarme</v-btn>
      <template #append>
        <v-chip :color="flota.conectat ? 'success' : 'error'" variant="flat" size="small" class="mr-2">
          {{ flota.conectat ? 'live' : 'fără legătură cu serverul' }}
        </v-chip>
      </template>
    </v-app-bar>
    <v-main>
      <router-view />
    </v-main>
    <v-snackbar v-model="notificare.vizibila" :timeout="6000" color="warning" location="top right">
      <strong>{{ notificare.masina }}</strong>: {{ notificare.tip }}<br />{{ notificare.detalii }}
      <template #actions>
        <v-btn variant="text" to="/alarme" @click="notificare.vizibila = false">Vezi</v-btn>
      </template>
    </v-snackbar>
  </v-app>
</template>

<script>
import { flota, asculta, pornesteLive, pornesteVremea } from './live.js'

export default {
  name: 'App',
  data() {
    return { flota, notificare: { vizibila: false, masina: '', tip: '', detalii: '' } }
  },
  mounted() {
    pornesteLive()
    pornesteVremea()
    // Alarmele noi apar ca notificare pe orice pagină (spec §7).
    asculta((tip, date) => {
      if (tip === 'ALARMA') {
        this.notificare = { vizibila: true, masina: date.masinaId, tip: date.tip, detalii: date.detalii }
      }
    })
  },
}
</script>
```

- [ ] **Pasul 6: Verifică notificarea**

Pe Hartă, trimite din nou `alarma.json` cu `mosquitto_pub`. Se așteaptă: în
dreapta sus, o notificare portocalie cu `masina-3: SUPRAINCALZIRE` și detaliile.
„Vezi” duce la pagina Alarme.

- [ ] **Pasul 7: Claude explică.** Analogia: o sonerie în tot dispeceratul, pe
  lângă registrul de alarme. Soneria nu ține minte nimic, registrul ține tot.
  Apoi:
  - de ce ascultarea notificărilor stă în `App`: `App` nu dispare niciodată,
    deci nu trebuie oprită;
  - `v-snackbar` și `timeout`.

- [ ] **Pasul 8: Commit-ul lui Claude**

```bash
git add web/src/App.vue
git commit -m "UI: notificarile de alarma pe orice pagina

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 14: Comenzile din panoul mașinii

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „o stare afișată care
combină două surse”: starea comenzii, din server, și starea mașinii, din tabla
live.

**Fișiere:**
- Creează: `web\src\components\PanouComenzi.vue`
- Modifică: `web\src\api.js`, `web\src\pagini\PaginaHarta.vue`

**Interfețe:**
- Consumă: endpoint-urile 6 și 7 (Task 4) și `CONFIRMARE` de pe canalul live
  (Task 5).
- Produce:
  - în `api.js`: `trimiteComanda(id, cerere)` și `comenzi(id)`;
  - `PanouComenzi`, cu prop-ul `masina`.

- [ ] **Pasul 1: Adaugă cererile în `api.js`**

```js
  trimiteComanda: (id, cerere) => cere(`/api/masini/${id}/comenzi`, { method: 'POST', body: JSON.stringify(cerere) }),
  comenzi: (id) => cere(`/api/masini/${id}/comenzi`),
```

- [ ] **Pasul 2: Scrie `PanouComenzi.vue`**

```vue
<template>
  <v-card variant="outlined" class="ma-2">
    <v-card-title>Comenzi</v-card-title>
    <v-card-text>
      <v-select v-model="tip" :items="TIPURI" item-title="titlu" item-value="valoare" label="Comanda" density="compact" />
      <v-text-field v-if="tip === 'LIMITEAZA_VITEZA'" v-model.number="kmh" type="number" label="km/h (10–130)" density="compact" />
      <v-text-field v-if="tip === 'MESAJ_SOFER'" v-model="text" label="Mesajul" counter="200" density="compact" />
      <v-btn color="primary" :loading="seTrimite" @click="trimite">Trimite</v-btn>
      <v-alert v-if="eroare" type="error" density="compact" class="mt-2">{{ eroare }}</v-alert>

      <v-list density="compact" class="mt-2">
        <v-list-item v-for="c in comenzi" :key="c.id" :title="descriere(c)" :subtitle="ora(c.oraTrimiterii)">
          <template #append>
            <v-chip :color="stareAfisata(c).culoare" size="small" variant="flat">{{ stareAfisata(c).text }}</v-chip>
          </template>
        </v-list-item>
      </v-list>
    </v-card-text>
  </v-card>
</template>

<script>
import { api } from '../api.js'
import { asculta } from '../live.js'

const TIPURI = [
  { titlu: 'Limitează viteza', valoare: 'LIMITEAZA_VITEZA' },
  { titlu: 'Întoarce la bază', valoare: 'INTOARCE_LA_BAZA' },
  { titlu: 'Mesaj pentru șofer', valoare: 'MESAJ_SOFER' },
]

export default {
  name: 'PanouComenzi',
  props: {
    masina: { type: Object, required: true },
  },
  data() {
    return { TIPURI, tip: 'LIMITEAZA_VITEZA', kmh: 50, text: '', comenzi: [], seTrimite: false, eroare: null }
  },
  computed: {
    idMasina() {
      return this.masina.id
    },
  },
  watch: {
    idMasina: {
      immediate: true,
      handler() {
        this.incarca()
      },
    },
  },
  mounted() {
    this.opresteAscultarea = asculta((tip, date) => {
      if (tip === 'CONFIRMARE' && date.masinaId === this.masina.id) this.inlocuieste(date)
    })
  },
  beforeUnmount() {
    this.opresteAscultarea()
  },
  methods: {
    async incarca() {
      const id = this.masina.id
      this.comenzi = []
      this.eroare = null
      const lista = await api.comenzi(id).catch(() => [])
      if (id === this.masina.id) this.comenzi = lista
    },
    async trimite() {
      this.eroare = null
      this.seTrimite = true
      try {
        const parametri =
          this.tip === 'LIMITEAZA_VITEZA' ? { kmh: this.kmh } : this.tip === 'MESAJ_SOFER' ? { text: this.text } : {}
        this.inlocuieste(await api.trimiteComanda(this.masina.id, { tip: this.tip, parametri }))
      } catch (e) {
        this.eroare = e.message
      } finally {
        this.seTrimite = false
      }
    },
    /**
     * Pune comanda în listă sau o înlocuiește pe cea cu același id. Confirmarea poate sosi înaintea
     * răspunsului la POST, așa că o comandă deja confirmată nu se mai întoarce la TRIMISA.
     */
    inlocuieste(comanda) {
      const i = this.comenzi.findIndex((c) => c.id === comanda.id)
      if (i < 0) this.comenzi.unshift(comanda)
      else if (this.comenzi[i].stare === 'TRIMISA') this.comenzi.splice(i, 1, comanda)
    },
    /** „trimisă” cât mașina e online; „așteaptă mașina” cât e offline: broker-ul o păstrează (spec §7). */
    stareAfisata(c) {
      if (c.stare === 'CONFIRMATA') return { text: 'confirmată', culoare: 'success' }
      if (c.stare === 'RESPINSA') return { text: `respinsă: ${c.motiv}`, culoare: 'error' }
      return this.masina.stare === 'ONLINE'
        ? { text: 'trimisă', culoare: 'info' }
        : { text: 'așteaptă mașina', culoare: 'warning' }
    },
    descriere(c) {
      if (c.tip === 'LIMITEAZA_VITEZA') return `Limitează la ${c.parametri.kmh} km/h`
      if (c.tip === 'MESAJ_SOFER') return `Mesaj: ${c.parametri.text}`
      return 'Întoarce la bază'
    },
    ora(iso) {
      return iso ? new Date(iso).toLocaleTimeString('ro-RO') : ''
    },
  },
}
</script>
```

- [ ] **Pasul 3: Pune panoul în `PaginaHarta.vue`**

Importul nou e `import PanouComenzi from '../components/PanouComenzi.vue'`, iar
componenta se adaugă în `components`. În șablon, în coloana laterală, între
`PanouMasina` și `ListaMasini`:

```vue
      <PanouComenzi v-if="masinaSelectata" :masina="masinaSelectata" />
```

- [ ] **Pasul 4: Verifică, inclusiv în tunel**

Totul pornit. Click pe `masina-3`. Se așteaptă:
- „Limitează viteza” cu 50, apoi „Trimite”: comanda apare „trimisă”, iar într-o
  secundă sau două devine „confirmată”;
- cu 200 km/h, alerta roșie spune mesajul serverului despre `kmh`;
- în O, `TUNEL masina-3 30`, apoi „Întoarce la bază”: comanda apare „așteaptă
  mașina”, cât `masina-3` e gri. După tunel devine „confirmată”, iar mașina
  merge înapoi pe traseu și se oprește la primul punct.

- [ ] **Pasul 5: Claude explică.** Analogia: o comandă e o scrisoare
  recomandată. „Trimisă” înseamnă că a plecat de la ghișeu, „așteaptă mașina”
  că stă la oficiul poștal, iar „confirmată” că s-a întors confirmarea de
  primire, semnată. Apoi:
  - de ce „așteaptă mașina” nu e o stare din bază, ci se calculează din două
    stări;
  - de ce o comandă confirmată nu se mai întoarce la „trimisă”.

- [ ] **Pasul 6: Commit**

```bash
git add web/src
git commit -m "UI: comenzile din panoul masinii, cu starea lor

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 15: Istoricul pe interval

**Cine scrie:** Tudor, ca repetiție: o componentă mică, cu evenimente, și o
cerere REST cu parametri. E a treia tăietură din spec §8, deci se face doar
dacă e timp.

**Fișiere:**
- Creează: `web\src\components\AlegereInterval.vue`
- Modifică: `web\src\pagini\PaginaHarta.vue`

**Interfețe:**
- Consumă: `api.traseu(id, de, pana)` (faza 2) și intervalul din endpoint-ul 3,
  cel mult 6 ore, cu erorile de la server.
- Produce: `AlegereInterval`, cu evenimentele `interval({ de, pana })`, ca ore ISO
  în UTC, și `live`.

- [ ] **Pasul 1: Tudor scrie partea lui.** Ce trebuie:
  - **`AlegereInterval.vue`:**
    - două `v-text-field` cu `type="datetime-local"`, „De la” și „Până la”;
    - butonul „Arată” trimite `interval`, cu orele transformate în ISO UTC prin
      `new Date(valoare).toISOString()`, și e inactiv cât lipsește una dintre
      ele;
    - butonul „Înapoi la live” trimite `live`;
  - **în `PaginaHarta.vue`:**
    - componenta apare în coloana laterală, sub `PanouComenzi`, cât e aleasă o
      mașină;
    - în modul istoric, pozițiile live nu se mai adaugă la traseu;
    - eroarea de la server, de exemplu „intervalul e mai lung de 6 ore”, apare
      într-un `v-alert`;
    - „Înapoi la live” reîncarcă ultimele 5 minute și pornește din nou
      adăugarea.

<details>
<summary>Soluția de referință, pentru review. Tudor n-o deschide înainte să încerce.</summary>

`AlegereInterval.vue`:

```vue
<template>
  <v-card variant="outlined" class="ma-2">
    <v-card-title>Istoric</v-card-title>
    <v-card-text>
      <v-text-field v-model="de" type="datetime-local" label="De la" density="compact" />
      <v-text-field v-model="pana" type="datetime-local" label="Până la" density="compact" />
      <div class="d-flex ga-2">
        <v-btn :disabled="!de || !pana" @click="arata">Arată</v-btn>
        <v-btn variant="text" @click="$emit('live')">Înapoi la live</v-btn>
      </div>
    </v-card-text>
  </v-card>
</template>

<script>
export default {
  name: 'AlegereInterval',
  emits: ['interval', 'live'],
  data() {
    return { de: '', pana: '' }
  },
  methods: {
    /** datetime-local dă ora locală, fără fus; serverul vrea UTC. */
    arata() {
      this.$emit('interval', { de: new Date(this.de).toISOString(), pana: new Date(this.pana).toISOString() })
    },
  },
}
</script>
```

`PaginaHarta.vue`, întreg:

```vue
<template>
  <div class="pagina">
    <HartaFlotei class="harta" :zone="zone" :traseu="traseu" :selectata="selectata" @alege="alege" />
    <div class="lateral">
      <PanouMasina :masina="masinaSelectata" @inchide="alege(null)" />
      <PanouComenzi v-if="masinaSelectata" :masina="masinaSelectata" />
      <AlegereInterval v-if="masinaSelectata" @interval="arataInterval" @live="alege(selectata)" />
      <v-alert v-if="eroareTraseu" type="error" density="compact" class="ma-2">{{ eroareTraseu }}</v-alert>
      <ListaMasini :selectata="selectata" @alege="alege" />
    </div>
  </div>
</template>

<script>
import AlegereInterval from '../components/AlegereInterval.vue'
import HartaFlotei from '../components/HartaFlotei.vue'
import ListaMasini from '../components/ListaMasini.vue'
import PanouComenzi from '../components/PanouComenzi.vue'
import PanouMasina from '../components/PanouMasina.vue'
import { api } from '../api.js'
import { flota, asculta } from '../live.js'

export default {
  name: 'PaginaHarta',
  components: { AlegereInterval, HartaFlotei, ListaMasini, PanouComenzi, PanouMasina },
  data() {
    return { zone: [], selectata: null, traseu: [], istoric: false, eroareTraseu: null }
  },
  computed: {
    masinaSelectata() {
      return this.selectata ? flota.masini[this.selectata] : null
    },
  },
  async mounted() {
    this.opresteAscultarea = asculta((tip, date) => {
      if (tip === 'POZITIE' && !this.istoric && date.masinaId === this.selectata) this.traseu.push(date)
    })
    this.zone = await api.zone()
  },
  beforeUnmount() {
    this.opresteAscultarea()
  },
  methods: {
    /** Ultimele 5 minute din REST, apoi traseul crește din canalul live. */
    async alege(id) {
      this.selectata = id
      this.istoric = false
      this.eroareTraseu = null
      this.traseu = []
      if (!id) return
      const traseu = await api.traseu(id)
      if (this.selectata === id && !this.istoric) this.traseu = traseu
    },
    /** Modul istoric: doar intervalul cerut, fără poziții live adăugate. */
    async arataInterval({ de, pana }) {
      const id = this.selectata
      this.istoric = true
      this.eroareTraseu = null
      try {
        const traseu = await api.traseu(id, de, pana)
        if (this.selectata === id) this.traseu = traseu
      } catch (e) {
        this.eroareTraseu = e.message
      }
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

</details>

- [ ] **Pasul 2: Verifică**

Click pe o mașină. Alege un interval de 20 de minute din ultima oră și „Arată”:
pe hartă apare doar traseul din interval, iar linia nu mai crește. Un interval de
7 ore dă alerta „intervalul e mai lung de 6 ore”. „Înapoi la live” arată ultimele
5 minute, care cresc din nou.

- [ ] **Pasul 3: Claude revizuiește,** fără să rescrie.

- [ ] **Pasul 4: Commit-ul lui Tudor**

```bash
git add web/src
git commit -m "UI: istoricul traseului pe interval"
```

---

### Task 16: Scenariul prezentării, repetiția și verificarea finală

**Cine face:** Claude scrie scenariul prezentării și README-ul. Tudor rulează
scenariul de test complet și face repetiția, cu cronometrul.

**Fișiere:**
- Creează: `docs\scenariu-prezentare.md`
- Modifică: `README.md`
- Modifică, în AIS-OS: `Facultate\memory\stare.md`

**Interfețe:** nu are.

- [ ] **Pasul 1: Scrie `docs\scenariu-prezentare.md`**

Cifrele dintre paranteze unghiulare se iau din tabelul „Măsurători” din
`docs\scenariu-test.md`, completat în faza 4, pasul 9. Claude le copiază în
fișier când îl scrie, așa că în fișierul final nu rămâne nicio paranteză.

````markdown
# Prezentarea, în 5 minute

După spec §11. Cifrele vin din tabelul „Măsurători” din `scenariu-test.md`.

## Cu o zi înainte

- Repetiția completă, cu cronometrul: toți cei șase pași, sub 5 minute.
- Hotspot-ul telefonului încercat pe laptop. Harta are nevoie de internet (R3).

## Cu 15 minute înainte

1. Serviciul MySQL pornit, apoi terminalele A, P, S, W, C și O, după `README.md`.
2. Curățenie, în D:

   ```powershell
   Sql "UPDATE masini SET pierdute = 0, recuperate = 0"
   Sql "UPDATE alarme SET vazuta = TRUE"
   ```

3. În browser:
   - tabul 1: Harta, mărită pe zonă, cu `masina-3` aleasă;
   - tabul 2: Rețea.
4. În O, `STARE`: zece rânduri `NORMAL`.

## Pașii

### 1. Zece mașini live, 30 de secunde

**Arăți:** harta, lista „10 din 10 online”, apoi panoul lui `masina-3`.

**Spui:** „Zece mașini simulate trimit telemetria prin MQTT, o dată pe secundă.
Între ele și broker stă un proxy scris de mine, care strică rețeaua la comandă.”

### 2. Tunelul, 60 de secunde

**Faci:** în O, `TUNEL masina-3 15`.

**Se vede:**
- `masina-3` devine gri pe loc;
- după vreo <secunde până revine verde> secunde e din nou verde;
- porțiunea din tunel e punctată, iar „Recuperate” crește.

**Spui:**
- mașina nu știe de tunel, vede doar că i-a murit conexiunea;
- cât e offline, motorul pune mesajele în buffer, cu `din_buffer`;
- radioul reîncearcă după 1, 2, 4 secunde, cu o abatere la întâmplare;
- gri a venit din Last Will-ul publicat de broker.

### 3. Gaura neagră, 75 de secunde

**Faci:** `GAURA masina-5 10`.

**Se vede:**
- `masina-5` rămâne verde vreo <secunde până devine gri> secunde, deși nu mai
  trece nimic;
- apoi devine gri, iar la revenire pe hartă e o porțiune lipsă, urmată de una
  punctată;
- pe Rețea, „Pierdute” crește cu vreo <mesaje pierdute>.

**Spui:**
- conexiunea e deschisă, dar mută. Keepalive-ul de 5 secunde o descoperă:
  broker-ul închide după 1,5 × 5 = 7,5 secunde fără niciun pachet;
- telemetria e pe QoS 0, deci ce era pe drum s-a pierdut. Alarmele sînt pe
  QoS 2 și ajung după reconectare;
- o gaură mai scurtă de 7,5 secunde nu pierde nimic, doar întârzie.

### 4. O comandă dată în tunel, 60 de secunde

**Faci:**
1. `TUNEL masina-3 30`;
2. din panou, „Limitează viteza”, cu 50.

**Se vede:** „așteaptă mașina”, apoi, la ieșirea din tunel, „confirmată”.

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

**Faci:** pe Rețea, ținta `TOATE`, întârzierea 500 și variația 200, apoi
„Aplică întârzierea”.

**Se vede:** graficul urcă la vreo 500 ms, iar mașinile devin portocalii.

**Spui:**
- fiecare mesaj are ora mașinii și ora sosirii, iar latența e diferența dintre
  ele;
- aici ceasul e același, fiindcă totul rulează pe laptop. Pe mașini reale,
  ceasurile s-ar sincroniza prin NTP;
- variația nu schimbă ordinea octeților, fiindcă pe TCP ordinea e garantată.

La final, „Normal”.

## Dacă ceva merge prost

| Ce vezi | Ce faci |
|---|---|
| Harta e goală, fără străzi | Hotspot-ul. Fără el, mașinile și traseele se văd pe fond gol (R3) |
| „proxy indisponibil” pe Rețea | P nu rulează: îl pornești; mașinile revin în cel mult 30 de secunde |
| O mașină rămâne gri după tunel peste 40 de secunde | În C, ultimele rânduri ale mașinii; dacă C a căzut, îl pornești |
| Chip-ul „fără legătură cu serverul” | S nu rulează: îl pornești; UI-ul se reconectează singur |

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
- **Ce ar trebui pentru mașini reale?** TLS, autentificare, NTP pentru ceasuri
  și un broker în cloud. Proiectul a lăsat asta deoparte (spec §13).
````

- [ ] **Pasul 2: Actualizează `README.md`**

După secțiunea „Cum pornești”, se adaugă:

````markdown
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
````

- [ ] **Pasul 3: Toate testele**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu testele simulatorului, ale serverului și ale
proxy-ului trecute.

- [ ] **Pasul 4: Scenariul de test complet, cu așteptările „Din faza 5”**

Toți cei șase pași din `docs\scenariu-test.md`. Claude citește interogările după
fiecare pas. Pasul 5 se face din panoul mașinii.

- [ ] **Pasul 5: Aceeași confirmare de două ori (review, punctul 2)**

1. Oprește C. Toate mașinile devin gri.
2. Din panoul lui `masina-3`, trimite „Limitează viteza”, cu 60. Notează id-ul
   comenzii din `Sql "SELECT id FROM comenzi ORDER BY id DESC LIMIT 1"`.
3. Scrie `docs\mesaje-test\confirmare.json`, cu id-ul notat în loc de `<ID>`:

   ```json
   {"comanda_id": <ID>, "ora": "2026-12-10T10:15:04.002Z", "rezultat": "EXECUTATA"}
   ```

4. Trimite-l de două ori:

   ```powershell
   & "C:\Program Files\mosquitto\mosquitto_pub.exe" -h 127.0.0.1 -t flota/masina-3/confirmari -q 1 -f docs\mesaje-test\confirmare.json
   & "C:\Program Files\mosquitto\mosquitto_pub.exe" -h 127.0.0.1 -t flota/masina-3/confirmari -q 1 -f docs\mesaje-test\confirmare.json
   ```

Se așteaptă:
- comanda e `CONFIRMATA` o singură dată;
- în S, la a doua, `confirmarea comenzii <ID> a mai venit o dată … o ignor`;
- în UI, „confirmată” apare o dată.

Pornește C: mașina primește comanda păstrată de broker, o execută și trimite
propria confirmare, pe care serverul o ignoră la fel. Fișierul
`confirmare.json` nu intră în git.

- [ ] **Pasul 6: Proxy-ul oprit (review, punctul 1)**

Oprește P. Se așteaptă:
- pe Rețea, chip-ul „proxy indisponibil” în cel mult 3 secunde;
- „Tunel” dă eroarea în cel mult 2 secunde;
- harta merge mai departe cu mașinile, care devin gri fiindcă și ele treceau
  prin proxy.

Pornește P la loc.

- [ ] **Pasul 7: Repetiția, de două ori**

Tudor parcurge `docs\scenariu-prezentare.md` de două ori, cu cronometrul, de la
„Cu 15 minute înainte” până la pasul 6. Ținta e sub 5 minute. Claude notează ce
s-a încurcat, iar scenariul se corectează după fiecare repetiție.

- [ ] **Pasul 8: Commit în proiect**

```bash
git add docs/scenariu-prezentare.md README.md
git commit -m "Scenariul prezentarii si README-ul final

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

- [ ] **Pasul 9: Actualizează starea în AIS-OS**

În `Facultate\memory\stare.md`, la blocul de RC, rândul „Unde am rămas” devine,
cu data zilei în care se termină pasul 7, în formatul AAAA-LL-ZZ:

```markdown
- **Unde am rămas:** proiectul e gata pe AAAA-LL-ZZ: toate cele cinci faze,
  scenariul de test trece, iar prezentarea, din `docs\scenariu-prezentare.md`,
  a fost repetată de două ori, sub 5 minute. Rămân întrebările pentru profesor:
  data prezentării și ce cere la predare (spec §14, Î2 și Î3).
```

În `Facultate\flota-rc\CLAUDE.md`, „Stadiu” devine:
`Stadiu: gata de prezentare, pe AAAA-LL-ZZ.`

- [ ] **Pasul 10: Commit în AIS-OS, după ce Tudor spune da**

```bash
cd "/c/Users/40747/OneDrive/Documents/AIS-OS"
git add Facultate/memory/stare.md Facultate/flota-rc/CLAUDE.md
git commit -m "flota-rc gata de prezentare

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
