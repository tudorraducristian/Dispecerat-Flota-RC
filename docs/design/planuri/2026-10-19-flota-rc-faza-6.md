# flota-rc, faza 6: login și documentele — plan de implementare

> **Pentru agenți:** SUB-SKILL OBLIGATORIU: superpowers:subagent-driven-development
> (recomandat) sau superpowers:executing-plans, ca planul să fie executat task cu
> task. Pașii folosesc căsuțe (`- [ ]`) pentru urmărire.

**Ținta:** proiectul acoperă grila profesorului (spec §16.1), cu tot ce ține de
el:
- dispeceratul are login, cu `ADMIN` și `DISPECER`, un meniu cu setări,
  conturi și „Ieșire”;
- există articolul științific, pe template-ul IEEE, PowerPoint-ul și manualul;
- prezentarea a fost repetată cu tot cu ele.

**Arhitectura:**
- **În server,** pachetul nou `conturi`: parolele cu BCrypt, token-urile în
  MySQL și un singur interceptor pe `/api/**`. Metodele doar pentru admin poartă
  adnotarea `@DoarAdmin`. Canalul live verifică token-ul la handshake și își
  închide sesiunile la log off. Setările stau în tabelul `setari`.
- **În UI:** token-ul stă în `localStorage`. `api.js` îl pune la fiecare
  cerere, iar o gardă în router trimite la `/login`. Canalul live pornește abia
  după login. Meniul e un sertar lateral.
- **Documentele:** textele stau ca Markdown în git, iar Word și PowerPoint,
  pornite din PowerShell, fac din ele `.docx` și `.pptx`. Figurile sînt SVG,
  transformate în PNG cu Edge, fără browser deschis.

**Stack:** cel din fazele 1–5, plus:
- `spring-security-crypto`, doar pentru BCrypt;
- Word 16 și PowerPoint 16, prin COM, din PowerShell 5.1;
- Edge, pentru PNG-uri;
- Python 3.14, doar cu biblioteca standard.

**Spec:** `docs\superpowers\specs\2026-10-08-flota-rc-design.md`, §16 și
deciziile D28–D34.

Planul se sprijină pe planurile fazelor 0–5, din același dosar. Dacă la execuție
codul real diferă de ce presupune un task, se ajustează task-ul, nu codul
fazelor de dinainte.

## Constrângeri globale

- **Folderul proiectului:** `C:\Users\40747\OneDrive\Documents\Facultate\An 4\Semestrul 1\RC-Proiect\`.
- **Remote-ul:** `origin`, repo-ul privat `ProiectRC` (D27). După commit-urile
  unui task urmează `git push`.
- **Java 21,** Maven doar prin `.\mvnw.cmd`. Singura dependență Java nouă e
  `org.springframework.security:spring-security-crypto`, fără versiune scrisă,
  fiindcă o dă Boot 4.1.1. Se descarcă după ce Tudor spune da.
- **UI-ul:** nicio dependență npm nouă în `web\`.
- **Rolurile:** `ADMIN` și `DISPECER`, fixe după creare (spec §16.2).
- **Token-ul:**
  - 32 de octeți din `SecureRandom`, în Base64 URL fără padding, deci 43 de
    caractere;
  - expiră la 8 ore după login, fără prelungire;
  - pleacă în header-ul `Authorization: Bearer <token>`, iar la WebSocket ca
    `/ws?token=`.
- **Conturile:**
  - numele respectă `^[a-z0-9._-]{3,20}$` și se normalizează (`trim`, litere
    mici) la login și la creare;
  - parola are cel puțin 8 caractere și cel mult 72 de octeți UTF-8.
- **Mesajele de eroare,** exact așa:
  - 401 la login: `nume sau parolă greșită`;
  - 401 de la interceptor: `trebuie să te autentifici`;
  - 403: `doar administratorul are voie`;
  - 400: `parola veche e greșită`;
  - 409: `există deja contul <nume>`, `nu-ți poți șterge propriul cont`,
    `ultimul administrator nu se poate șterge` și
    `parola ta se schimbă din Setări`.
- **Setările:** `offline_prelungit_secunde`, implicit 60, între 10 și 3600;
  `traseu_minute`, implicit 5, între 1 și 360. În JSON se numesc
  `offlinePrelungitSecunde` și `traseuMinute`.
- **Endpoint-urile noi** au numerele 13–22, din spec §16.2. Cele 1–12 cer un
  token valid, de orice rol.
- **Limbile:** articolul e în engleză. PowerPoint-ul, manualul și UI-ul sînt în
  română.
- **Fișierele documentelor:**
  - articolul: `docs\articol\articol-flota-rc.docx`;
  - PowerPoint-ul: `docs\prezentare\flota-rc.pptx`;
  - manualul: `docs\manual\manual-flota-rc.docx`;
  - pentru fiecare, PDF-ul alături, făcut de Word sau PowerPoint.
- **Uneltele de documente** stau în `docs\unelte\`. Cele în Python folosesc doar
  biblioteca standard și se rulează cu `python -I`.
- **Metoda mixtă (spec §9):**
  - **Claude (exemplu explicat)** scrie codul și îl explică, întâi analogia,
    apoi partea tehnică;
  - **Tudor (repetiție)** scrie după teste, iar Claude revizuiește fără să
    rescrie;
  - documentele le scrie Claude (D31, D32).
- **Verificările pe viu** le face Claude, cu piesele pornite în fundal și
  oprite după, fără tab-uri de terminal. Tudor a ales așa pe 19 octombrie.
- **Commit-urile lui Claude** se termină cu
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Faza 6 nu se taie** (spec §8): fără documente, grila nu ia în calcul
  celelalte puncte.

## Completări la spec, făcute la scrierea planului

Intră în spec ca D34, în același commit cu planul:

- **D34.** Detaliile fazei 6, pe care spec-ul nu le hotăra:
  - numele de cont se normalizează la login și la creare, ca „Admin ” să
    ajungă la `admin`;
  - parola are cel mult 72 de octeți UTF-8, limita BCrypt. Altfel se întoarce
    400, nu 500;
  - adminul nu-și resetează propria parolă din Conturi, ci din Setări (409);
  - pe lângă ștergerea de la login, token-urile expirate se caută o dată pe
    minut. Se șterg, iar canalele lor live se închid;
  - metodele doar pentru admin poartă adnotarea `@DoarAdmin`;
  - serverul închide canalul live cu codul 4001, „sesiune încheiată”;
  - în `localStorage`, cheia token-ului e `flota.token`;
  - documentele se fac cu Word și PowerPoint prin COM, din surse Markdown, iar
    figurile, din SVG cu Edge. Template-ul articolului se mută în
    `docs\articol\model\`;
  - măsurătorile pentru articol: tunelul și cele două găuri, de câte 3 ori;
    întârzierea, o rulare de 60 de secunde;
  - la repetiție, demonstrația ține sub 5 minute, iar totul sub 8, până
    răspunde profesorul la Î2.

## Ajustări după codul real, făcute pe 2026-10-26

Fazele 4 și 5 le-a scris colegul pe 22 octombrie, după planurile lor. Pe 26
octombrie, Claude a reparat alarma QoS 2 dublă și cele 15 lucruri mici amânate
din fazele 2 și 3. Codul de la care pornește faza 6 e `11b3026`, cu 291 de teste
care trec. Diferențele care ating task-urile de mai jos:

- **Înainte de fiecare task,** `git pull`: colegul lucrează pe același `main`.
- **Task 2:** `api\MasiniControllerTest` și `api\ZoneControllerTest` trec prin
  `@WebMvcTest`, deci încarcă și `ConfigWeb`, cu `InterceptorAutentificare`. Din
  Task 2, ambele primesc `@MockitoBean ServiciuAutentificare`, cu
  `valideaza("t")` care întoarce un dispecer, iar cererile pun
  `Authorization: Bearer t`. În `MasiniControllerTest` intră încă un test: fără
  header, 401 ca JSON `{status, mesaj}`.
- **Task 6:** `live.js` are, din 26 octombrie, `stariLive` și `imbinaMasina`:
  instantaneul REST de la redeschiderea canalului se îmbină cu ce a venit live.
  `pornesteLive()` cu `?token=` păstrează `stariLive.clear()` în `onopen`.
- **Task 7:** `PaginaHarta.alege(id)` are deja `try/catch`, iar un `watch` pe
  `conectat` o cheamă din nou după o cădere a serverului, alături de
  `incarcaZone()`. Cererea cu `de = acum − traseuMinute` intră în același
  `try`.
- **Task 9:** pasul 2 se rulează din instrumentul PowerShell, cu
  `& docs\articol\masuratori\masoara.ps1`, nu prin
  `powershell -ExecutionPolicy Bypass -File`: pe 26 octombrie, clasificatorul
  auto mode a refuzat forma asta. Pentru `masoara.ps1`: în PowerShell 5.1, un
  tablou JSON venit din `Invoke-RestMethod` trece pe pipeline ca un singur
  obiect, deci se desface cu `foreach`. Cifrele de referință cu Mosquitto sînt în
  `docs\scenariu-test.md`, „Măsurători”: gaura de 10 s pierde 7 și recuperează
  3, gaura de 6 s nu pierde nimic, întârzierea dă 436–572 ms.
- **Task 13:** manualul preia din `README.md` secțiunea „Prima pornire” și
  rezerva HiveMQ CE, la depanare.
- **Task 14:** `docs\scenariu-prezentare.md` are de pe 26 octombrie cifrele cu
  Mosquitto: gri la 6,6 s, nu la 7,5.

## Unde trebuie să fie atent review-ul

Cinci situații pe care un om le întâlnește repede, cu comportamentul așteptat:

1. **Token-ul expiră cu pagina deschisă.** În cel mult un minut, serverul
   închide canalul live, iar UI-ul duce la `/login`. Testul e în Task 4:
   `CuratareTokenuriTest.tokenulExpiratInchideCanalulSiSeSterge`. Se verifică
   și pe viu, în Task 8.
2. **Numele e scris cu majuscule sau spații,** de exemplu „Admin ”. Login-ul
   merge, iar contul nou se salvează cu litere mici. Testele sînt
   `ServiciuAutentificareTest.numeleSeCautaFaraMajusculeSiSpatii`, în Task 2, și
   `ServiciuConturiTest.numeleContuluiSeSalveazaCuLitereMici`, în Task 3.
3. **Parola are peste 72 de octeți,** de exemplu 37 de „ș”. Răspunsul e 400, cu
   mesaj, nu 500. Testul e în Task 1: `ReguliContTest.parolaPeste72DeOctetiDa400`.
4. **Două taburi sînt deschise, iar log off-ul se dă într-unul.** Serverul
   închide ambele canale, iar celălalt tab ajunge la `/login`. Testul e în Task 4:
   `CanalLiveTest.inchideToateSesiunileAceluiasiToken`. Se verifică pe viu în
   Task 8.
5. **Adminul își alege propriul cont în pagina Conturi.** Ștergerea și
   resetarea dau 409, cu mesajul lor. Testele sînt în Task 3:
   `nuPotiStergePropriulCont` și `adminulNuSiReseteazaPropriaParolaDeAici`.

---

## Structura fișierelor

```
RC-Proiect\
├── server\pom.xml                                   + spring-security-crypto
├── server\src\main\
│   ├── resources\schema.sql                         + utilizatori, tokenuri, setari
│   └── java\ro\flota\server\
│       ├── conturi\ (nou)  Rol, Utilizator, UtilizatorDto, UtilizatorCurent, RaspunsLogin,
│       │                   CerereCont, Parole, ReguliCont, Utilizatori, Tokenuri, PrimulAdmin,
│       │                   ServiciuAutentificare, ServiciuConturi, DoarAdmin,
│       │                   InterceptorAutentificare, ConfigWeb, CuratareTokenuri
│       ├── setari\ (nou)   SetariDto, Setari
│       ├── live\           CanalLive, ConfigWebSocket, TokenLaHandshake (nou)
│       ├── reguli\         RegulaOffline, VerificareOffline
│       └── api\            Neautorizat, Interzis, Conflict (noi), EroriApi,
│                           AuthController, ConturiController, SetariController (noi)
├── web\src\
│   ├── main.js, App.vue, router.js, api.js, live.js, sesiune.js (nou)
│   └── pagini\PaginaLogin.vue, PaginaSetari.vue, PaginaConturi.vue (noi), PaginaHarta.vue
└── docs\
    ├── design\                  copiile spec-ului și ale planurilor (D27)
    ├── unelte\  (nou)           grafice.py, test_grafice.py, svg-la-png.ps1,
    │                            docx-din-md.ps1, stiluri_docx.py, exemplu.md,
    │                            exemplu-asteptat.txt, pptx-la-png.ps1
    ├── articol\ (nou)           model\, masuratori\, date\, figuri\, articol.md,
    │                            masuratori.md, articol-flota-rc.docx/.pdf
    ├── prezentare\ (nou)        flota-rc.pptx/.pdf, slide-uri\
    └── manual\ (nou)            baza-manual.docx, manual.md, capturi\,
                                 manual-flota-rc.docx/.pdf
```

---

### Task 1: Tabelele, parolele și primul admin

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „o parolă care nu se
poate citi din bază”.

**Fișiere:**
- Modifică: `server\pom.xml`, `server\src\main\resources\schema.sql`
- Creează, în `server\src\main\java\ro\flota\server\conturi\`: `Rol.java`,
  `Utilizator.java`, `UtilizatorDto.java`, `Parole.java`, `ReguliCont.java`,
  `Utilizatori.java` și `PrimulAdmin.java`
- Teste, în `server\src\test\java\ro\flota\server\conturi\`: `ParoleTest.java`,
  `ReguliContTest.java` și `PrimulAdminTest.java`
- Copiază, după D27: spec-ul și planurile fazelor 5 și 6 din AIS-OS în
  `docs\design\`, plus un rând nou în `docs\design\README.md`

**Interfețe:**
- Consumă: `CerereGresita`, din `api`, și bean-ul `Clock`.
- Produce:
  - `enum Rol { ADMIN, DISPECER }`;
  - `record Utilizator(long id, String nume, String hashParola, Rol rol, Instant creatLa)`;
  - `record UtilizatorDto(long id, String nume, Rol rol, Instant creatLa)`, cu
    `static UtilizatorDto din(Utilizator u)`;
  - `@Component Parole`, cu `String hash(String parola)` și
    `boolean potrivite(String parola, String hash)`;
  - `final class ReguliCont`, cu `static String normalizeaza(String nume)`,
    `static void verificaNume(String nume)` și
    `static void verificaParola(String parola)`, ultimele două aruncând
    `CerereGresita`;
  - `@Repository Utilizatori`, cu `Optional<Utilizator> dupaNume(String)`,
    `Optional<Utilizator> dupaId(long)`, `List<Utilizator> toti()`,
    `long adauga(String nume, String hash, Rol rol, Instant creatLa)`,
    `void schimbaParola(long id, String hash)`, `void sterge(long id)` și
    `int numarAdmini()`;
  - `PrimulAdmin`, un `ApplicationRunner` cu `@Order(2)`, după `IncarcareDate`,
    plus `static String parolaAleatoare(SecureRandom r)`.

- [ ] **Pasul 1: Adaugă dependența, după ce Tudor spune da**

Claude întreabă: „Adaug `spring-security-crypto`, cu versiunea dată de Boot, de
pe Maven Central. E un jar mic. E bine?”. Dacă da, în `server\pom.xml`, la
`dependencies`, fără `<version>`. Apoi:

Comanda: `.\mvnw.cmd -q -pl server dependency:tree "-Dincludes=org.springframework.security"`
Se așteaptă: un rând cu `spring-security-crypto:jar:7.`, plus versiunea. Claude o
notează în registru.

- [ ] **Pasul 2: Adaugă tabelele în `schema.sql`**

```sql
CREATE TABLE IF NOT EXISTS utilizatori (
  id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nume        VARCHAR(20)  NOT NULL UNIQUE,
  hash_parola VARCHAR(100) NOT NULL,
  rol         VARCHAR(10)  NOT NULL,
  creat_la    DATETIME(3)  NOT NULL
);

CREATE TABLE IF NOT EXISTS tokenuri (
  token         CHAR(43)    NOT NULL PRIMARY KEY,
  utilizator_id BIGINT      NOT NULL,
  creat_la      DATETIME(3) NOT NULL,
  expira_la     DATETIME(3) NOT NULL,
  INDEX idx_tokenuri_expira (expira_la),
  CONSTRAINT fk_tokenuri_utilizator FOREIGN KEY (utilizator_id) REFERENCES utilizatori (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS setari (
  cheie   VARCHAR(40) NOT NULL PRIMARY KEY,
  valoare VARCHAR(40) NOT NULL
);
```

- [ ] **Pasul 3: Scrie testele care pică**

```java
class ParoleTest {
    private final Parole parole = new Parole();

    @Test void parolaBunaSePotriveste() {
        assertTrue(parole.potrivite("dispecer-2026", parole.hash("dispecer-2026")));
    }
    @Test void parolaGresitaNuSePotriveste() {
        assertFalse(parole.potrivite("alta-parola", parole.hash("dispecer-2026")));
    }
    @Test void acelasiTextDaHashuriDiferite() {          // sarea e alta de fiecare dată
        assertNotEquals(parole.hash("dispecer-2026"), parole.hash("dispecer-2026"));
    }
    @Test void hashulEsteBCrypt() {
        assertTrue(parole.hash("dispecer-2026").startsWith("$2a$10$"));
    }
}
```

```java
class ReguliContTest {
    @Test void numeBune() {
        for (String n : List.of("ana", "dispecer.1", "tudor_r-2", "a".repeat(20)))
            assertDoesNotThrow(() -> ReguliCont.verificaNume(n));
    }
    @Test void numeGresite() {
        for (String n : List.of("", "ab", "a".repeat(21), "Ana", "ion popescu", "ștefan"))
            assertThrows(CerereGresita.class, () -> ReguliCont.verificaNume(n));
    }
    @Test void normalizeazaTaieSpatiileSiMicsoreazaLiterele() {
        assertEquals("admin", ReguliCont.normalizeaza("  Admin "));
    }
    @Test void parolaSubOptCaractereDa400() {
        CerereGresita e = assertThrows(CerereGresita.class, () -> ReguliCont.verificaParola("1234567"));
        assertEquals("parola are cel puțin 8 caractere", e.getMessage());
    }
    @Test void parolaPeste72DeOctetiDa400() {           // 37 × „ș” = 74 de octeți în UTF-8
        CerereGresita e = assertThrows(CerereGresita.class, () -> ReguliCont.verificaParola("ș".repeat(37)));
        assertEquals("parola are cel mult 72 de octeți", e.getMessage());
    }
    @Test void parolaDeExact72DeOctetiTrece() {
        assertDoesNotThrow(() -> ReguliCont.verificaParola("a".repeat(72)));
    }
}
```

Mesajul pentru nume: `numele are 3–20 caractere: litere mici, cifre, punct, _ sau -`.

```java
class PrimulAdminTest {
    private static final Instant T0 = Instant.parse("2026-12-10T10:00:00Z");
    private final Utilizatori utilizatori = mock(Utilizatori.class);
    private final PrimulAdmin primul = new PrimulAdmin(utilizatori, new Parole(), Clock.fixed(T0, ZoneOffset.UTC));

    @Test void faceAdminCandNuExistaNiciunul() {
        when(utilizatori.numarAdmini()).thenReturn(0);
        primul.run(null);
        verify(utilizatori).adauga(eq("admin"), startsWith("$2a$10$"), eq(Rol.ADMIN), eq(T0));
    }
    @Test void nuFaceNimicCandExistaUnAdmin() {
        when(utilizatori.numarAdmini()).thenReturn(1);
        primul.run(null);
        verify(utilizatori, never()).adauga(any(), any(), any(), any());
    }
    @Test void parolaAleatoareAre16LitereSauCifre() {
        assertTrue(PrimulAdmin.parolaAleatoare(new SecureRandom()).matches("[A-Za-z0-9]{16}"));
    }
}
```

- [ ] **Pasul 4: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q -pl server test`
Se așteaptă: eroare de compilare, fiindcă `Parole`, `ReguliCont` și
`PrimulAdmin` lipsesc.

- [ ] **Pasul 5: Scrie clasele**

- `Parole` învelește un singur `BCryptPasswordEncoder()`, cu puterea implicită,
  10.
- `ReguliCont.verificaParola` numără octeții cu
  `parola.getBytes(StandardCharsets.UTF_8).length`.
- `Utilizatori` folosește `JdbcClient`, cu SQL scris de mână (D19). Orele trec
  prin `Timestamp`, ca în `flota\Ore`.
- `PrimulAdmin.run`:
  1. dacă `numarAdmini() == 0`, alege o parolă cu `parolaAleatoare`, din 62 de
     litere și cifre, și apelează `adauga("admin", hash, ADMIN, acum)`;
  2. apoi scrie în log, ca `WARN`:
     `Am creat contul admin. Parola, afișată o singură dată: {}. Schimb-o din Setări.`

- [ ] **Pasul 6: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 13 teste noi trecute.

- [ ] **Pasul 7: Verifică pe bază**

Pornește S în fundal. Se așteaptă:
- în log, rândul cu parola lui `admin`. Claude i-o dă lui Tudor și nu o scrie
  nicăieri altundeva;
- `SHOW TABLES` arată în plus `setari`, `tokenuri` și `utilizatori`;
- `SELECT nume, rol, LEFT(hash_parola, 7) FROM utilizatori` dă
  `admin | ADMIN | $2a$10$`.

Repornește S: în log nu mai apare nicio parolă.

- [ ] **Pasul 8: Claude explică.** Analogia: hash-ul e ca o amprentă. Din parolă
  faci amprenta, dar din amprentă nu poți reface parola. La login se compară
  amprentele, nu parolele. Apoi:
  - sarea, adică de ce același text dă hash-uri diferite;
  - de ce BCrypt e lent intenționat și ce înseamnă puterea 10;
  - limita de 72 de octeți;
  - de ce parola lui `admin` apare doar în log, nu în git.

- [ ] **Pasul 9: Commit**

```bash
git add server/pom.xml server/src docs/design
git commit -m "Server: conturile, parolele cu BCrypt si primul admin

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git push
```

---

### Task 2: Login-ul, token-ul și interceptorul

**Cine scrie:** Claude, ca exemplu explicat. E tiparul 8 din spec §9.

**Fișiere:**
- Creează, în `conturi\`: `UtilizatorCurent.java`, `RaspunsLogin.java`,
  `Tokenuri.java`, `ServiciuAutentificare.java`, `DoarAdmin.java`,
  `InterceptorAutentificare.java` și `ConfigWeb.java`
- Creează, în `api\`: `Neautorizat.java`, `Interzis.java` și
  `AuthController.java`
- Modifică: `api\EroriApi.java`, cu 401 și 403
- Teste: `conturi\ServiciuAutentificareTest.java` și
  `conturi\InterceptorAutentificareTest.java`

**Interfețe:**
- Consumă: din Task 1, `Utilizatori`, `Parole` și `ReguliCont`; din faza 2,
  `CanalLive`.
- Produce:
  - `record UtilizatorCurent(long id, String nume, Rol rol, String token)`;
  - `record RaspunsLogin(String token, String nume, Rol rol)`;
  - `@Repository Tokenuri`, cu
    `void salveaza(String token, long utilizatorId, Instant creatLa, Instant expiraLa)`,
    `Optional<UtilizatorCurent> valid(String token, Instant acum)`, făcut cu un
    `JOIN` pe `utilizatori` și condiția `expira_la > acum`,
    `void sterge(String token)`, `void stergeAleUtilizatorului(long id)`,
    `List<String> expirate(Instant acum)` și `void stergeExpirate(Instant acum)`;
  - `@Service ServiciuAutentificare`, cu
    `RaspunsLogin login(String nume, String parola)`,
    `void logout(UtilizatorCurent eu)`,
    `Optional<UtilizatorCurent> valideaza(String token)` și
    `void schimbaParola(UtilizatorCurent eu, String veche, String noua)`;
  - `static final Duration DURATA_TOKEN = Duration.ofHours(8)` și
    `static String tokenNou(SecureRandom r)`, în `ServiciuAutentificare`;
  - adnotarea `@DoarAdmin`, pe metode, cu `RetentionPolicy.RUNTIME`;
  - `InterceptorAutentificare`, cu
    `static final String ATRIBUT = "utilizator"`, sub care pune
    `UtilizatorCurent` în cerere;
  - `CanalLive.inchideSesiunileTokenului(String token)` și
    `CanalLive.inchideSesiunileUtilizatorului(long id)`, aici doar cu corpul gol,
    ca Task 2 și Task 3 să compileze. Corpurile vin în Task 4;
  - endpoint-urile 13–16.

- [ ] **Pasul 1: Scrie testele care pică**

`ServiciuAutentificareTest` folosește mock-uri pentru `Utilizatori`, `Tokenuri`
și `CanalLive`, `Parole` adevărat și un ceas fix pe `T0`. Contul de test e
`new Utilizator(1, "ana", parole.hash("parola-buna"), Rol.DISPECER, T0)`.

```java
@Test void loginBunDaUnTokenDe43DeCaractereValabil8Ore() {
    RaspunsLogin r = serviciu.login("ana", "parola-buna");
    assertTrue(r.token().matches("[A-Za-z0-9_-]{43}"));
    assertEquals(new RaspunsLogin(r.token(), "ana", Rol.DISPECER), r);
    verify(tokenuri).salveaza(r.token(), 1L, T0, T0.plus(Duration.ofHours(8)));
}
@Test void loginStergeIntaiTokenurileExpirate() {
    serviciu.login("ana", "parola-buna");
    InOrder ordine = inOrder(tokenuri);
    ordine.verify(tokenuri).stergeExpirate(T0);
    ordine.verify(tokenuri).salveaza(any(), anyLong(), any(), any());
}
@Test void numeNecunoscutSiParolaGresitaDauAcelasiMesaj() {
    assertEquals("nume sau parolă greșită", assertThrows(Neautorizat.class, () -> serviciu.login("ion", "parola-buna")).getMessage());
    assertEquals("nume sau parolă greșită", assertThrows(Neautorizat.class, () -> serviciu.login("ana", "alta-parola")).getMessage());
}
@Test void numeleSeCautaFaraMajusculeSiSpatii() {
    serviciu.login("  Ana ", "parola-buna");
    verify(utilizatori).dupaNume("ana");
}
@Test void logoutStergeTokenulSiInchideCanalele() {
    serviciu.logout(new UtilizatorCurent(1, "ana", Rol.DISPECER, "t"));
    verify(tokenuri).sterge("t");
    verify(live).inchideSesiunileTokenului("t");
}
@Test void tokenGolNuAjungeLaBaza() {
    assertEquals(Optional.empty(), serviciu.valideaza(null));
    assertEquals(Optional.empty(), serviciu.valideaza(" "));
    verifyNoInteractions(tokenuri);
}
@Test void schimbaParolaCereParolaVeche() {
    UtilizatorCurent eu = new UtilizatorCurent(1, "ana", Rol.DISPECER, "t");
    assertEquals("parola veche e greșită",
            assertThrows(CerereGresita.class, () -> serviciu.schimbaParola(eu, "gresita-1", "parola-noua")).getMessage());
    serviciu.schimbaParola(eu, "parola-buna", "parola-noua");
    verify(utilizatori).schimbaParola(eq(1L), startsWith("$2a$10$"));
}
@Test void schimbaParolaVerificaParolaNoua() {
    UtilizatorCurent eu = new UtilizatorCurent(1, "ana", Rol.DISPECER, "t");
    assertThrows(CerereGresita.class, () -> serviciu.schimbaParola(eu, "parola-buna", "scurta"));
}
```

`InterceptorAutentificareTest` folosește un mock pentru `ServiciuAutentificare`,
în care `valideaza("a")` întoarce `ADMIN_CURENT`, `valideaza("d")` întoarce un
dispecer, iar restul întoarce gol. Cererile sînt `MockHttpServletRequest`, iar
cele două `HandlerMethod`-uri vin dintr-o clasă de test cu o metodă obișnuită și
una cu `@DoarAdmin`:

```java
@Test void faraHeaderDa401() {
    assertEquals("trebuie să te autentifici", assertThrows(Neautorizat.class, () -> interceptor.preHandle(cerere(null), raspuns, obisnuita)).getMessage());
}
@Test void headerFaraBearerDa401() { assertThrows(Neautorizat.class, () -> interceptor.preHandle(cerere("Basic abc"), raspuns, obisnuita)); }
@Test void tokenNecunoscutDa401()  { assertThrows(Neautorizat.class, () -> interceptor.preHandle(cerere("Bearer necunoscut"), raspuns, obisnuita)); }
@Test void dispecerPeMetodaDeAdminDa403() {
    assertEquals("doar administratorul are voie", assertThrows(Interzis.class, () -> interceptor.preHandle(cerere("Bearer d"), raspuns, deAdmin)).getMessage());
}
@Test void adminulTreceSiPrimesteUtilizatorulInCerere() {
    MockHttpServletRequest c = cerere("Bearer a");
    assertTrue(interceptor.preHandle(c, raspuns, deAdmin));
    assertEquals(ADMIN_CURENT, c.getAttribute(InterceptorAutentificare.ATRIBUT));
}
@Test void dispecerulTrecePeMetodaObisnuita() { assertTrue(interceptor.preHandle(cerere("Bearer d"), raspuns, obisnuita)); }
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q -pl server test`
Se așteaptă: eroare de compilare, fiindcă clasele noi lipsesc.

- [ ] **Pasul 3: Scrie clasele**

- `Neautorizat` și `Interzis` sînt `RuntimeException`-uri, ca `Negasit`.
  `EroriApi` le dă 401 și 403, cu același JSON, `{status, mesaj}`.
- `InterceptorAutentificare.preHandle` aruncă `Neautorizat` sau `Interzis`.
  Excepțiile aruncate din `preHandle` ajung tot la `EroriApi`, deci JSON-ul
  erorilor rămâne unul singur. Un handler care nu e `HandlerMethod` trece.
- `ConfigWeb` e un `WebMvcConfigurer`, cu
  `addPathPatterns("/api/**").excludePathPatterns("/api/auth/login")`.
- `AuthController` primește utilizatorul curent cu
  `@RequestAttribute(InterceptorAutentificare.ATRIBUT) UtilizatorCurent eu`:

  | # | Metoda | Corpul | Răspunsul |
  |---|---|---|---|
  | 13 | `POST /api/auth/login` | `{nume, parola}` | `RaspunsLogin` |
  | 14 | `POST /api/auth/logout` | – | 204 |
  | 15 | `GET /api/auth/eu` | – | `{nume, rol}` |
  | 16 | `PUT /api/auth/parola` | `{parolaVeche, parolaNoua}` | 204 |

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 14 teste noi trecute.

- [ ] **Pasul 5: Verifică pe viu, cu `curl.exe`**

S pornit în fundal, cu parola lui `admin` din Task 1:

```powershell
curl.exe -s -i http://127.0.0.1:8080/api/masini
$corp = '{"nume":"Admin","parola":"<parola din log>"}'
curl.exe -s -H "Content-Type: application/json" -d $corp http://127.0.0.1:8080/api/auth/login
curl.exe -s -H "Authorization: Bearer <token>" http://127.0.0.1:8080/api/masini
curl.exe -s -i -X POST -H "Authorization: Bearer <token>" http://127.0.0.1:8080/api/auth/logout
curl.exe -s -i -H "Authorization: Bearer <token>" http://127.0.0.1:8080/api/auth/eu
```

Se așteaptă, în ordine:
1. `HTTP/1.1 401`, cu `{"status":401,"mesaj":"trebuie să te autentifici"}`;
2. `{"token":"…","nume":"admin","rol":"ADMIN"}`. Numele cu majusculă a mers;
3. lista celor 10 mașini;
4. `HTTP/1.1 204`;
5. `HTTP/1.1 401`.

- [ ] **Pasul 6: Claude explică.** Analogia: token-ul e brățara de la un
  festival. O primești o dată, la intrare, după ce arăți biletul. Apoi paznicii
  se uită doar la brățară, nu-ți mai cer biletul. Apoi:
  - de ce token-ul e aleator, nu numele criptat;
  - drumul unei cereri: interceptor → controller → serviciu → bază;
  - de ce 401 și 403 sînt diferite;
  - de ce mesajul de login e același pentru nume și pentru parolă: nu spune cuiva
    din afară ce conturi există;
  - ce vede cineva care ascultă rețeaua, fiindcă HTTP nu e criptat. Asta ajunge
    în articol.

- [ ] **Pasul 7: Commit**

```bash
git add server/src
git commit -m "Server: login, token si interceptorul pe /api

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git push
```

---

### Task 3: Conturile, pentru admin

**Cine scrie:**
- **`ServiciuConturi` și `ConturiController`:** Tudor, ca repetiție a unui
  serviciu cu reguli și a unui controller;
- **`Conflict` și rândul din `EroriApi`:** Claude.

**Fișiere:**
- Creează: `conturi\CerereCont.java`, `conturi\ServiciuConturi.java`,
  `api\Conflict.java` și `api\ConturiController.java`
- Modifică: `api\EroriApi.java`, cu 409
- Test: `conturi\ServiciuConturiTest.java`

**Interfețe:**
- Consumă: din Task 1, `Utilizatori`, `Parole`, `ReguliCont` și
  `UtilizatorDto`; din Task 2, `Tokenuri`, `UtilizatorCurent`, `@DoarAdmin` și
  `CanalLive.inchideSesiunileUtilizatorului(long id)`, încă goală până la Task 4.
- Produce:
  - `record CerereCont(String nume, String parola, Rol rol)`;
  - `@Service ServiciuConturi`, cu `List<UtilizatorDto> toti()`,
    `UtilizatorDto creeaza(CerereCont c)`,
    `void sterge(UtilizatorCurent eu, long id)` și
    `void reseteazaParola(UtilizatorCurent eu, long id, String noua)`;
  - endpoint-urile 17–20, toate `@DoarAdmin`. `POST` întoarce 201, iar
    `DELETE` și `PUT` întorc 204.

- [ ] **Pasul 1: Claude scrie testele care pică**

Mock-uri pentru `Utilizatori`, `Tokenuri` și `CanalLive`, `Parole` adevărat și
un ceas fix. Adminul curent e
`EU = new UtilizatorCurent(1, "admin", Rol.ADMIN, "t")`, iar contul țintă e
`ANA = new Utilizator(7, "ana", "h", Rol.DISPECER, T0)`.

```java
@Test void numeleContuluiSeSalveazaCuLitereMici() {
    serviciu.creeaza(new CerereCont(" Ana ", "parola-buna", Rol.DISPECER));
    verify(utilizatori).adauga(eq("ana"), startsWith("$2a$10$"), eq(Rol.DISPECER), eq(T0));
}
@Test void numeExistentDa409() {
    when(utilizatori.dupaNume("ana")).thenReturn(Optional.of(ANA));
    assertEquals("există deja contul ana", assertThrows(Conflict.class,
            () -> serviciu.creeaza(new CerereCont("ana", "parola-buna", Rol.DISPECER))).getMessage());
}
@Test void rolLipsaDa400() {
    assertEquals("rolul e ADMIN sau DISPECER", assertThrows(CerereGresita.class,
            () -> serviciu.creeaza(new CerereCont("ana", "parola-buna", null))).getMessage());
}
@Test void nuPotiStergePropriulCont() {
    assertEquals("nu-ți poți șterge propriul cont", assertThrows(Conflict.class, () -> serviciu.sterge(EU, 1)).getMessage());
}
@Test void ultimulAdminNuSePoateSterge() {
    when(utilizatori.dupaId(9)).thenReturn(Optional.of(new Utilizator(9, "sef", "h", Rol.ADMIN, T0)));
    when(utilizatori.numarAdmini()).thenReturn(1);
    assertEquals("ultimul administrator nu se poate șterge", assertThrows(Conflict.class, () -> serviciu.sterge(EU, 9)).getMessage());
}
@Test void contInexistentDa404() {
    assertEquals("contul 99 nu există", assertThrows(Negasit.class, () -> serviciu.sterge(EU, 99)).getMessage());
}
@Test void stergereaInchideCanaleleContului() {
    when(utilizatori.dupaId(7)).thenReturn(Optional.of(ANA));
    serviciu.sterge(EU, 7);
    verify(utilizatori).sterge(7);
    verify(live).inchideSesiunileUtilizatorului(7);
}
@Test void resetareaSchimbaHashulSiScoateOmulDinAplicatie() {
    when(utilizatori.dupaId(7)).thenReturn(Optional.of(ANA));
    serviciu.reseteazaParola(EU, 7, "parola-noua");
    verify(utilizatori).schimbaParola(eq(7L), startsWith("$2a$10$"));
    verify(tokenuri).stergeAleUtilizatorului(7);
    verify(live).inchideSesiunileUtilizatorului(7);
}
@Test void adminulNuSiReseteazaPropriaParolaDeAici() {
    assertEquals("parola ta se schimbă din Setări", assertThrows(Conflict.class,
            () -> serviciu.reseteazaParola(EU, 1, "parola-noua")).getMessage());
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q -pl server test`
Se așteaptă: eroare de compilare.

- [ ] **Pasul 3: Tudor scrie `ServiciuConturi` și `ConturiController`.** Indicii:
  - ordinea în `creeaza`: rolul, numele normalizat și verificat, parola, apoi
    `dupaNume`;
  - ordinea în `sterge` și `reseteazaParola`: propriul cont (409), apoi
    `dupaId` (404), apoi regula ultimului admin;
  - controller-ul primește `eu` ca în `AuthController`.

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 9 teste noi trecute.

- [ ] **Pasul 5: Claude revizuiește,** fără să rescrie.

- [ ] **Pasul 6: Verifică pe viu**

Ca admin: creează `dispecer1`, cu rolul `DISPECER`. Login ca `dispecer1`, apoi
`GET /api/utilizatori`. Se așteaptă: 403, cu `doar administratorul are voie`.
Ca admin, `DELETE` pe propriul id. Se așteaptă: 409.

- [ ] **Pasul 7: Commit-ul lui Tudor**

```bash
git add server/src
git commit -m "Server: conturile, pentru admin"
git push
```

---

### Task 4: Canalul live cu token și închiderea sesiunilor

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „verificarea la
handshake”.

**Fișiere:**
- Creează: `live\TokenLaHandshake.java` și `conturi\CuratareTokenuri.java`
- Modifică: `live\ConfigWebSocket.java` și `live\CanalLive.java`
- Teste: `live\TokenLaHandshakeTest.java`, `conturi\CuratareTokenuriTest.java`,
  plus două teste noi în `live\CanalLiveTest.java`

**Interfețe:**
- Consumă: `ServiciuAutentificare.valideaza` și `Tokenuri.expirate`,
  `Tokenuri.stergeExpirate`, din Task 2.
- Produce:
  - `TokenLaHandshake implements HandshakeInterceptor`. La token valid, pune în
    atributele sesiunii `"utilizatorId"` (`Long`) și `"token"` (`String`);
  - `CanalLive.inchideSesiunileTokenului(String token)` și
    `CanalLive.inchideSesiunileUtilizatorului(long id)`, care închid sesiunile
    potrivite cu `new CloseStatus(4001, "sesiune încheiată")` și le scot din
    listă;
  - `CuratareTokenuri`, cu `@Scheduled(fixedRate = 60_000) void curata()`.

- [ ] **Pasul 1: Scrie testele care pică**

```java
// TokenLaHandshakeTest: cererea vine dintr-un MockHttpServletRequest, prin ServletServerHttpRequest.
@Test void faraTokenRefuzaCu401() {
    assertFalse(handshake.beforeHandshake(cerere(""), raspuns, handler, atribute));
    assertEquals(401, servletRaspuns.getStatus());
}
@Test void tokenInvalidRefuza() { assertFalse(handshake.beforeHandshake(cerere("token=necunoscut"), raspuns, handler, atribute)); }
@Test void tokenValidPuneUtilizatorulInAtribute() {
    assertTrue(handshake.beforeHandshake(cerere("token=t"), raspuns, handler, atribute));
    assertEquals(Map.of("utilizatorId", 1L, "token", "t"), atribute);
}
```

În `CanalLiveTest`, o variantă nouă a ajutorului, `sesiune(id, token,
utilizatorId)`, întoarce atributele din handshake. Sesiunile vechi, fără
atribute, nu se potrivesc cu nimic.

```java
// CanalLiveTest, în plus: sesiunile au atributele puse la handshake.
@Test void inchideToateSesiunileAceluiasiToken() throws Exception {
    WebSocketSession a = sesiune("1", "t", 1L), b = sesiune("2", "t", 1L), c = sesiune("3", "u", 2L);
    List.of(a, b, c).forEach(canal::afterConnectionEstablished);
    canal.inchideSesiunileTokenului("t");
    verify(a).close(new CloseStatus(4001, "sesiune încheiată"));
    verify(b).close(new CloseStatus(4001, "sesiune încheiată"));
    verify(c, never()).close(any());
    assertEquals(1, canal.numarSesiuni());
}
@Test void inchideSesiunileUtilizatorului() throws Exception {
    WebSocketSession a = sesiune("1", "t", 7L), c = sesiune("3", "u", 2L);
    List.of(a, c).forEach(canal::afterConnectionEstablished);
    canal.inchideSesiunileUtilizatorului(7);
    verify(a).close(new CloseStatus(4001, "sesiune încheiată"));
    assertEquals(1, canal.numarSesiuni());
}
```

```java
// CuratareTokenuriTest
@Test void tokenulExpiratInchideCanalulSiSeSterge() {
    when(tokenuri.expirate(T0)).thenReturn(List.of("t"));
    curatare.curata();
    verify(live).inchideSesiunileTokenului("t");
    verify(tokenuri).stergeExpirate(T0);
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q -pl server test`
Se așteaptă: teste picate sau eroare de compilare.

- [ ] **Pasul 3: Scrie clasele**

- Token-ul se ia din query cu
  `UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token")`.
- La refuz, handshake-ul pune `HttpStatus.UNAUTHORIZED`.
- `ConfigWebSocket` adaugă `.addInterceptors(tokenLaHandshake)`. Originile
  rămân cele din faza 2.
- Decoratorul din `CanalLive` dă mai departe `getAttributes()`, deci căutarea se
  face pe atributele sesiunii decorate.

- [ ] **Pasul 4: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 6 teste noi trecute și cu testele vechi
din `CanalLiveTest` trecute la fel.

- [ ] **Pasul 5: Claude explică.** Analogia: handshake-ul e paznicul de la ușa
  sălii de control. Îți verifică brățara o singură dată, la intrare. Cine are
  brățara tăiată e scos și din sală. Apoi:
  - de ce token-ul stă în adresă la WebSocket și ce risc aduce asta;
  - codurile de închidere, de ce 4001;
  - de ce curățenia rulează o dată pe minut.

- [ ] **Pasul 6: Commit**

```bash
git add server/src
git commit -m "Server: canalul live cere token si se inchide la log off

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git push
```

---

### Task 5: Setările

**Cine scrie:**
- **`SetariDto`, `Setari` și `SetariController`:** Tudor, ca repetiție a unui
  repo cu valori implicite și a unui controller;
- **schimbarea din `RegulaOffline` și `VerificareOffline`:** Claude.

**Fișiere:**
- Creează: `setari\SetariDto.java`, `setari\Setari.java` și
  `api\SetariController.java`
- Modifică: `reguli\RegulaOffline.java` și `reguli\VerificareOffline.java`
- Teste: `setari\SetariDtoTest.java`; în `reguli\RegulaOfflineTest.java`, apelurile
  primesc pragul

**Interfețe:**
- Consumă: `@DoarAdmin`, din Task 2; `RegulaOffline` și `VerificareOffline`, din
  faza 5, Task 7.
- Produce:
  - `record SetariDto(int offlinePrelungitSecunde, int traseuMinute)`, cu
    `static final SetariDto IMPLICITE = new SetariDto(60, 5)` și
    `SetariDto valideaza()`, care aruncă `CerereGresita`;
  - `Setari`, un `@Repository` și un `ApplicationRunner` cu `@Order(3)`. La
    pornire scrie valorile implicite cu `INSERT IGNORE`. Are
    `SetariDto citeste()`, `SetariDto salveaza(SetariDto s)` și
    `Duration offlinePrelungit()`;
  - endpoint-urile 21 și 22, al doilea cu `@DoarAdmin`;
  - `RegulaOffline.deAlarmat(Instant acum, Duration prag)` înlocuiește
    `deAlarmat(Instant)`, iar constanta `PRAG` dispare.

- [ ] **Pasul 1: Claude scrie testele care pică**

```java
class SetariDtoTest {
    @Test void implicitele() { assertEquals(new SetariDto(60, 5), SetariDto.IMPLICITE); }
    @Test void limiteleSintIncluse() {
        for (SetariDto s : List.of(new SetariDto(10, 1), new SetariDto(3600, 360)))
            assertDoesNotThrow(s::valideaza);
    }
    @Test void offlineInAfaraLimitelorDa400() {
        for (int v : new int[] {9, 3601})
            assertEquals("offlinePrelungitSecunde e între 10 și 3600",
                    assertThrows(CerereGresita.class, () -> new SetariDto(v, 5).valideaza()).getMessage());
    }
    @Test void traseuInAfaraLimitelorDa400() {
        for (int v : new int[] {0, 361})
            assertEquals("traseuMinute e între 1 și 360",
                    assertThrows(CerereGresita.class, () -> new SetariDto(60, v).valideaza()).getMessage());
    }
}
```

În `RegulaOfflineTest`, testele vechi trec `Duration.ofSeconds(60)`, iar unul
nou fixează pragul din apel:

```java
@Test void pragulVineDinApel() {
    regula.stare("masina-3", false, T0);
    assertEquals(List.of(), regula.deAlarmat(dupa(30), Duration.ofSeconds(30)));
    assertEquals(List.of("masina-3"), regula.deAlarmat(dupa(31), Duration.ofSeconds(30)));
}
```

- [ ] **Pasul 2: Rulează testele și vezi că pică**

Comanda: `.\mvnw.cmd -q -pl server test`
Se așteaptă: eroare de compilare.

- [ ] **Pasul 3: Tudor scrie `SetariDto`, `Setari` și `SetariController`.**
  Indicii:
  - cheile din bază sînt `offline_prelungit_secunde` și `traseu_minute`;
  - valorile se țin ca text;
  - `salveaza` validează întâi, apoi scrie ambele chei.

- [ ] **Pasul 4: Claude schimbă `RegulaOffline` și `VerificareOffline`.**
  `VerificareOffline` primește `Setari` în constructor și trimite pragul
  `setari.offlinePrelungit()`. Mesajul alarmei rămâne
  `offline de N de secunde`.

- [ ] **Pasul 5: Rulează testele și vezi că trec**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu cele 5 teste noi trecute și testele vechi din
`RegulaOfflineTest` trecute.

- [ ] **Pasul 6: Claude revizuiește,** fără să rescrie.

- [ ] **Pasul 7: Verifică pe viu**

1. Ca admin, `PUT /api/setari` cu `{"offlinePrelungitSecunde":20,"traseuMinute":5}`.
2. Oprește C.
3. Așteaptă 30 de secunde.

Se așteaptă: alarme `OFFLINE_PRELUNGIT` pentru toate mașinile, cu
`offline de 2x de secunde`. Pune la loc 60. Ca dispecer, același `PUT` dă 403.

- [ ] **Pasul 8: Commit-urile**

Întâi cel al lui Tudor, fiindcă regula are nevoie de `Setari`:

```bash
git add server/src/main/java/ro/flota/server/setari server/src/main/java/ro/flota/server/api/SetariController.java server/src/test/java/ro/flota/server/setari
git commit -m "Server: setarile"
git add server/src
git commit -m "Server: pragul offline vine din setari

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git push
```

---

### Task 6: UI-ul: login, garda și token-ul

**Cine scrie:** Claude, ca exemplu explicat. Tiparul e „o gardă în router și un
singur loc pentru token”.

**Fișiere:**
- Creează: `web\src\sesiune.js` și `web\src\pagini\PaginaLogin.vue`
- Modifică: `web\src\api.js`, `live.js`, `router.js`, `main.js` și `App.vue`

**Interfețe:**
- Consumă: endpoint-urile 13–15, din Task 2; `/ws?token=`, din Task 4.
- Produce:
  - în `sesiune.js`:
    - `sesiune`, un `reactive({ token, nume, rol })`, cu token-ul citit din
      `localStorage['flota.token']`;
    - `salveaza({ token, nume, rol })`, `uita()`, `esteAdmin()` și
      `async asiguraUtilizatorul()`, care cheamă `api.eu()` o singură dată,
      când `rol` lipsește;
  - în `api.js`:
    - `seteazaLaNeautorizat(f)`;
    - `login(nume, parola)`, `logout()`, `eu()` și
      `schimbaParola(veche, noua)`;
    - `cere` pune `Authorization`, iar la 401, în afară de login, cheamă
      `uita()` și apoi `f()`;
  - în `live.js`: `pornesteLive()` cu `?token=`, plus `opresteLive()` și
    `opresteVremea()`. La `onclose`, dacă nu s-a cerut oprirea, cheamă
    `api.eu()`:
    - dacă răspunde, se reconectează la 2 secunde;
    - la 401, a redirecționat deja `api.js`, deci nu se mai reconectează;
    - la o eroare de rețea, cu serverul oprit, se reconectează;
  - rute noi: `/login`, cu `meta: { public: true }`; `/setari`; `/conturi`, cu
    `meta: { doarAdmin: true }`. Paginile pentru ultimele două vin în Task 7;
  - garda `router.beforeEach`, asincronă:
    - fără token, pe o rută care nu e publică → `/login?inapoi=<ruta>`;
    - cu token, pe `/login` → `/`;
    - pe o rută `doarAdmin`, după `asiguraUtilizatorul()`, fără `ADMIN` → `/`.

- [ ] **Pasul 1: Scrie `sesiune.js` și schimbă `api.js`.** În `main.js`,
  `seteazaLaNeautorizat(() => router.push('/login'))`. Astfel `api.js` nu
  importă router-ul și nu apare un import circular.

- [ ] **Pasul 2: Scrie `PaginaLogin.vue`**
  - un `v-card` centrat, cu titlul „Dispecerat flotă”;
  - câmpurile „Nume” și „Parolă”, al doilea cu `type="password"`;
  - butonul „Intră”, care merge și cu Enter, și un `v-alert` cu mesajul
    serverului;
  - la succes, `salveaza`, apoi ruta din `inapoi` sau `/`.

- [ ] **Pasul 3: Schimbă `live.js`, `router.js` și `App.vue`**
  - `App.vue` pornește canalul live și vremea doar cu token. Un `watch` pe
    `sesiune.token` le pornește și le oprește. Ascultarea notificărilor rămâne
    în `mounted`;
  - pe `/login`, `App.vue` arată doar `<router-view />`, fără bară;
  - în bară, deocamdată, „Ieșire” cheamă `api.logout()`, apoi `uita()` și duce
    la `/login`. Meniul complet vine în Task 7.

- [ ] **Pasul 4: Verifică în browserul aplicației**

Totul pornit în fundal. Se așteaptă:
1. http://127.0.0.1:5173 duce la `/login`;
2. o parolă greșită arată „nume sau parolă greșită”;
3. login ca `admin`: harta cu mașinile verzi, iar chip-ul arată „live”;
4. după reîncărcarea paginii, omul rămâne în aplicație;
5. „Ieșire” duce la `/login`, iar înapoi în browser trimite tot la `/login`;
6. în consolă: `localStorage['flota.token'] = 'x'.repeat(43)`, apoi reîncărcare:
   pagina ajunge la `/login`.

- [ ] **Pasul 5: Claude explică.** Analogia: garda din router e recepția.
  Înainte să te lase pe un hol, se uită dacă ai brățară și, pentru biroul
  șefului, dacă scrie „admin” pe ea. Apoi:
  - de ce `localStorage`, nu o variabilă, și ce se pierde la reîncărcare;
  - de ce `api.js` nu importă router-ul;
  - de ce canalul live pornește abia după login.

- [ ] **Pasul 6: Commit**

```bash
git add web/src
git commit -m "UI: login, garda din router si token-ul in cereri

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git push
```

---

### Task 7: UI-ul: meniul, Setări și Conturi

**Cine scrie:**
- **meniul din `App.vue` și traseul după setări:** Claude, ca exemplu explicat;
- **`PaginaSetari.vue` și `PaginaConturi.vue`:** Tudor, ca repetiție a unei
  pagini cu formular și a uneia cu tabel din REST, după `PaginaAlarme`.

**Fișiere:**
- Creează: `web\src\pagini\PaginaSetari.vue` și `PaginaConturi.vue`
- Modifică: `web\src\App.vue`, `api.js`, `router.js`, `pagini\PaginaHarta.vue`
  și `components\AlegereInterval.vue`

**Interfețe:**
- Consumă: endpoint-urile 16–22.
- Produce, în `api.js`: `utilizatori()`, `creeazaCont(cerere)`,
  `stergeCont(id)`, `reseteazaParola(id, noua)`, `setari()` și
  `salveazaSetari(s)`.

- [ ] **Pasul 1: Claude face meniul în `App.vue`**
  - un `v-app-bar-nav-icon` deschide un `v-navigation-drawer`;
  - în sertar: Harta (`mdi-map`), Rețea (`mdi-access-point-network`), Alarme
    (`mdi-bell`), Setări (`mdi-cog`) și, doar dacă `esteAdmin()`, Conturi
    (`mdi-account-multiple`);
  - în bară, în dreapta: chip-ul live, `nume · rol` și butonul „Ieșire”, cu
    `mdi-logout`;
  - butoanele de navigare din bară, puse în faza 5, dispar.

- [ ] **Pasul 2: Claude leagă traseul de setări.** `PaginaHarta` citește o dată
  `api.setari()` în `mounted`. `alege(id)` cere traseul cu
  `de = acum − traseuMinute`. Textul „ultimele 5 minute” din `AlegereInterval`
  devine „ultimele N minute”, cu N venit ca prop.

- [ ] **Pasul 3: Tudor scrie paginile.** Ce trebuie:
  - **`PaginaSetari.vue`:**
    - cardul „Contul meu”, cu parola veche, cea nouă și confirmarea ei. Cele două
      parole noi trebuie să fie egale înainte de cerere. La succes apare
      „Parola s-a schimbat”;
    - cardul „Flota”, cu cele două numere. „Salvează” apare doar pentru admin;
      dispecerul vede câmpurile `readonly`;
    - erorile serverului apar într-un `v-alert`;
  - **`PaginaConturi.vue`:**
    - un `v-table` cu numele, rolul și data creării;
    - pe fiecare rând, butoanele „Resetează parola” (un dialog cu parola nouă) și
      „Șterge” (un dialog de confirmare). Pe rândul propriu, butoanele lipsesc;
    - „Cont nou” deschide un dialog cu numele, parola și rolul, ales dintr-un
      `v-select`, implicit `DISPECER`;
    - un 409 apare cu mesajul serverului.

- [ ] **Pasul 4: Claude revizuiește,** fără să rescrie.

- [ ] **Pasul 5: Verifică în browserul aplicației.** Ca admin:
  1. creează `dispecer1`;
  2. schimbă-i parola, apoi încearcă să-ți ștergi propriul cont: butonul lipsește;
  3. schimbă `traseuMinute` la 2. Traseul lui `masina-3` arată ultimele 2
     minute.

  Ca `dispecer1`: Conturi lipsește din meniu, `/conturi` scrisă de mână duce la
  Harta, iar cardul „Flota” e doar de citit. Pune `traseuMinute` înapoi la 5.

- [ ] **Pasul 6: Commit-urile**

```bash
git add web/src/App.vue web/src/pagini/PaginaHarta.vue web/src/components/AlegereInterval.vue
git commit -m "UI: meniul lateral si traseul dupa setari

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git add web/src
git commit -m "UI: paginile Setari si Conturi"
git push
```

---

### Task 8: Verificarea login-ului, cap-coadă

**Cine face:** Claude, cu piesele pornite în fundal. Tudor se uită la final, în
browserul aplicației.

**Fișiere:**
- Modifică: `README.md`, cu „Primul login” și „Conturile”
- Modifică: `docs\scenariu-test.md`, cu pasul 7, „Login-ul”

**Interfețe:** nu are.

- [ ] **Pasul 1: Toate testele**

Comanda: `.\mvnw.cmd -q test`
Se așteaptă: `BUILD SUCCESS`, cu testele simulatorului, ale proxy-ului și ale
serverului trecute.

- [ ] **Pasul 2: Două taburi și log off (review, punctul 4).** Login ca
  `dispecer1`, apoi un al doilea tab. Taburile împart token-ul din
  `localStorage`. „Ieșire” în primul. Se așteaptă: în cel mult 3 secunde, al
  doilea tab e pe `/login`.

- [ ] **Pasul 3: Token-ul expirat cu pagina deschisă (review, punctul 1).** Cu
  un tab logat:

  ```powershell
  Sql "UPDATE tokenuri SET expira_la = UTC_TIMESTAMP(3) - INTERVAL 1 SECOND"
  ```

  Se așteaptă: în cel mult 70 de secunde, tab-ul e pe `/login`, iar tabelul
  `tokenuri` e gol.

- [ ] **Pasul 4: Resetarea parolei scoate omul din aplicație.** `dispecer1`
  e logat în browserul aplicației. Adminul îi resetează parola cu `curl.exe`, pe
  endpoint-ul 20, cu token-ul lui de admin, fiindcă același browser ține un
  singur login. Se așteaptă: tab-ul lui `dispecer1` ajunge pe `/login`, iar
  parola veche nu mai merge.

- [ ] **Pasul 5: Scrie în `README.md` și în `docs\scenariu-test.md`.**
  - **README:** parola lui `admin` apare o singură dată în log, la prima pornire.
    Dacă s-a pierdut, `DELETE FROM utilizatori WHERE nume = 'admin'`, iar la
    repornire serverul face din nou contul;
  - **scenariul de test:** pașii 2–4 de mai sus.

- [ ] **Pasul 6: Commit**

```bash
git add README.md docs/scenariu-test.md
git commit -m "Login-ul verificat cap-coada; README si scenariul de test

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git push
```

---

### Task 9: Măsurătorile pentru articol

**Cine face:** Claude, cu piesele pornite în fundal (R8).

**Fișiere:**
- Creează: `docs\articol\masuratori\masoara.ps1`, `interogari.sql` și
  `docs\articol\masuratori.md`
- Creează, la rulare: `docs\articol\date\*.csv`

**Interfețe:**
- Consumă: `unelte\ordine.ps1 -Ordine "<linie>"`, din faza 3, și tabelul
  `pozitii`.
- Produce:
  - `date\e1-tunel-r{1,2,3}.csv`, `date\e2-gaura10-r{1,2,3}.csv`,
    `date\e3-gaura4-r{1,2,3}.csv` și `date\e4-intarziere.csv`. Coloanele sînt
    `seq,ora_masinii,ora_sosirii,din_buffer,latenta_ms`, pentru mașina din
    experiment, de la 20 de secunde înainte de comandă până la 30 de secunde
    după revenire;
  - `date\rezumat.csv`, cu coloanele
    `experiment,repetitie,pierdute,recuperate,secunde_offline,latenta_medie_ms,latenta_p95_ms`.

- [ ] **Pasul 1: Scrie `interogari.sql` și `masoara.ps1`.** Pentru fiecare
  rulare:
  1. contoarele se pun pe 0;
  2. se așteaptă 20 de secunde;
  3. se dă comanda;
  4. se așteaptă durata ei plus 30 de secunde;
  5. se exportă cu `mysql -B`, transformat în CSV.

  `secunde_offline` vine din `ultima_vedere`, citită la fiecare secundă.
  Experimentele:
  - E1: `TUNEL masina-3 15`;
  - E2: `GAURA masina-5 10`;
  - E3: `GAURA masina-5 4`;
  - E4: `INTARZIERE TOATE 500 200`, 60 de secunde, apoi `NORMAL TOATE`.

- [ ] **Pasul 2: Rulează, totul pornit**

Comanda: `powershell -NoProfile -ExecutionPolicy Bypass -File docs\articol\masuratori\masoara.ps1`
Se așteaptă, după spec §10, în `rezumat.csv`:
- E1: `pierdute` ≤ 1 și `recuperate` > 0, la fiecare repetiție;
- E2: `pierdute` > 0 și `recuperate` > 0;
- E3: `pierdute` = 0 și `recuperate` = 0;
- E4: latența medie între 400 și 600 ms, fără rânduri `din_buffer`.

Dacă o așteptare nu se potrivește, se folosește superpowers:systematic-debugging
pe cod. Articolul nu raportează cifre care contrazic spec-ul fără explicație.

- [ ] **Pasul 3: Scrie `masuratori.md`.** Un tabel cu media și intervalul pe
  fiecare experiment, data, hash-ul commit-ului măsurat și o propoziție despre
  ce arată fiecare rând.

- [ ] **Pasul 4: Commit**

```bash
git add docs/articol/masuratori docs/articol/date docs/articol/masuratori.md
git commit -m "Articolul: masuratorile pentru tunel, gauri si latenta

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git push
```

---

### Task 10: Figurile

**Cine face:** Claude.

**Fișiere:**
- Creează: `docs\unelte\grafice.py`, `test_grafice.py` și `svg-la-png.ps1`
- Creează, în `docs\articol\figuri\`: `arhitectura.svg`, `conexiuni.svg`,
  `schema-reconectare.svg` și `schema-connect.svg`, scrise de mână
- Creează, la rulare: `tunel-gaura-en.svg`, `tunel-gaura-ro.svg`,
  `latenta-en.svg`, `latenta-ro.svg`, `harta.png` și câte un `.png` din fiecare
  `.svg`

**Interfețe:**
- Consumă: CSV-urile din Task 9.
- Produce:
  - `python -I docs\unelte\grafice.py <csv> <iesire.svg> --tip tunel-gaura|latenta --limba en|ro`;
  - `svg-la-png.ps1 -Svg <cale> -Png <cale> -Latime <px> -Inaltime <px>`, care
    cheamă Edge cu `--headless --screenshot`, la `--force-device-scale-factor=2`;
  - figurile, cu etichete în engleză pentru articol. Diagramele de arhitectură
    și conexiuni folosesc doar nume de piese și porturi, deci servesc și
    PowerPoint-ul.

- [ ] **Pasul 1: Scrie testele care pică, în `test_grafice.py`,** cu `unittest`:
  - `test_scara_duce_capetele_la_margini`: maparea valorilor pe pixeli pune
    minimul și maximul la marginile zonei de desen;
  - `test_tunel_gaura_are_cate_un_punct_pe_mesaj`: dintr-un CSV de 5 rânduri
    ies 5 cercuri, iar cele cu `din_buffer` sînt goale pe dinăuntru;
  - `test_eticheta_axei_in_limba_ceruta`: „Arrival time (s)” față de „Ora sosirii
    (s)”;
  - `test_iesirea_e_svg_valid`: `xml.etree` o citește, iar rădăcina e `svg`.

Comanda: `python -I -m unittest discover -s docs\unelte -p "test_*.py"`
Se așteaptă: întâi picat, fiindcă modulul lipsește, apoi `OK` după pasul 2.

- [ ] **Pasul 2: Scrie `grafice.py`,** doar cu biblioteca standard.
  - **`tunel-gaura`:** pe orizontală, ora sosirii, în secunde de la comandă; pe
    verticală, `seq`. Punctele live sînt pline, cele din buffer goale, iar
    golurile din `seq` se văd ca lipsă. Durata comenzii e o bandă gri;
  - **`latenta`:** latența fiecărui mesaj și media mobilă pe 10 mesaje.

- [ ] **Pasul 3: Desenează cele patru SVG-uri de mână**
  - **arhitectura:** cele cinci piese, cu MySQL, ca în spec §3;
  - **conexiunile:** porturile 1883, 1884, 1885, 3306, 8080 și 5173, protocoalele
    și topic-urile `flota/<id>/…`, cu QoS-ul fiecăruia;
  - **schemele logice:** reconectarea cu pauză care se dublează (1, 2, 4, 8, 16,
    30 de secunde, cu ±20%) și citirea pachetului `CONNECT`, ca în spec §5.2.

  Fontul e Times New Roman, ca în template, cu textul de minim 8 pt la lățimea
  unei coloane, 8,9 cm.

- [ ] **Pasul 4: Fă PNG-urile și captura hărții.** `harta.png` se face în
  browserul aplicației, la 1600 × 1000, cu `masina-3` aleasă și traseul ei.

- [ ] **Pasul 5: Privește fiecare PNG.** Claude citește imaginile și verifică:
  nimic tăiat, textul lizibil, nicio suprapunere.

- [ ] **Pasul 6: Commit**

```bash
git add docs/unelte docs/articol/figuri
git commit -m "Articolul: figurile si uneltele lor

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git push
```

---

### Task 11: Articolul științific

**Cine scrie:** Claude, tot textul (D31). Tudor îl citește și îl aprobă.

**Fișiere:**
- Mută: `Model articol stiintific RC.docx`, din rădăcina proiectului, în
  `docs\articol\model\`
- Creează: `docs\unelte\docx-din-md.ps1`, `stiluri-ieee.json`,
  `stiluri_docx.py`, `exemplu.md` și `exemplu-asteptat.txt`
- Creează: `docs\articol\articol.md`, `articol-flota-rc.docx` și
  `articol-flota-rc.pdf`

**Interfețe:**
- Consumă: `masuratori.md` și CSV-urile din Task 9, figurile din Task 10 și
  spec-ul.
- Produce:
  - `docx-din-md.ps1 -Baza <docx> -Text <md> -Iesire <docx> -Stiluri <json> [-Pdf]`.
    Deschide o copie a bazei în Word, prin COM, și păstrează stilurile și
    secțiunile ei. Golește corpul de după blocul cu titlul și autorii, apoi
    adaugă blocurile din Markdown, fiecare cu stilul lui. La `-Pdf` salvează și
    PDF-ul;
  - `python -I docs\unelte\stiluri_docx.py <docx>` scrie fiecare paragraf ca
    `[stil] text`. Citește și varianta „strict” a formatului Word, cum e
    template-ul;
  - Markdown-ul acceptat:

    | Ce scrii | Ce devine |
    |---|---|
    | antetul `---` cu `titlu:`, `autor:` (5 rânduri despărțite de ` | `), `abstract:` și `cuvinte-cheie:` | stilurile `paper title`, `Author`, `Abstract`, `Keywords` |
    | `#`, `##`, `###` | `Heading 1`, `2`, `3` |
    | un paragraf | `Body Text` |
    | `- ` | `bullet list` |
    | `![Legenda](figuri/x.png)` | imaginea, la lățimea coloanei, plus `figure caption` |
    | `Table: Legenda`, urmat de un tabel cu `|` | `table head`, apoi tabelul |
    | `# References`, apoi `[n] …` | `Heading 5`, apoi `references` |
    | `**gros**`, `*cursiv*`, `` `cod` `` | formatare pe text, cu `Courier New` la cod |

    Pentru manual, același script primește altă hartă de stiluri.

- [ ] **Pasul 1: Cere datele de autor (Î8) și răspunsul la Î9.** Claude îl
  întreabă pe Tudor:
  - numele complet, facultatea, universitatea, orașul și emailul;
  - dacă profesorul cere o mențiune despre folosirea AI.

  Până răspunde, articolul merge mai departe, cu `<de completat>` în antet.

- [ ] **Pasul 2: Testul uneltei, care pică.** `exemplu.md` folosește fiecare rând
  din tabelul de mai sus. `exemplu-asteptat.txt` e ieșirea așteptată a lui
  `stiluri_docx.py`, scrisă de mână.

Comanda:
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File docs\unelte\docx-din-md.ps1 -Baza "docs\articol\model\Model articol stiintific RC.docx" -Text docs\unelte\exemplu.md -Iesire $env:TEMP\exemplu.docx -Stiluri docs\unelte\stiluri-ieee.json
python -I docs\unelte\stiluri_docx.py $env:TEMP\exemplu.docx | Compare-Object (Get-Content docs\unelte\exemplu-asteptat.txt)
```
Se așteaptă: întâi eroare, fiindcă scriptul lipsește; după pasul 3, nicio
diferență.

- [ ] **Pasul 3: Scrie `docx-din-md.ps1`, `stiluri-ieee.json` și
  `stiluri_docx.py`,** până trece pasul 2. Word se deschide invizibil și se
  închide în `finally`, ca să nu rămână procese `WINWORD.EXE` agățate.

- [ ] **Pasul 4: Scrie `articol.md`, în engleză,** după structura din spec
  §16.5:
  - fiecare cifră vine din `masuratori.md`;
  - fiecare figură din Task 10 e citată în text înainte să apară;
  - cel puțin 8 referințe reale, verificate pe web: titlul, autorii, anul și
    DOI-ul sau URL-ul;
  - „montajul hardware” se tratează ca neaplicabil, într-o propoziție;
  - secțiunea VI discută HTTP necriptat, token-ul din adresa WebSocket-ului,
    laptopul unic și mașinile simulate.

- [ ] **Pasul 5: Fă documentul și PDF-ul**

Comanda: aceeași ca la pasul 2, cu `-Text docs\articol\articol.md`,
`-Iesire docs\articol\articol-flota-rc.docx` și `-Pdf`.
Se așteaptă: PDF-ul are 4–6 pagini.

- [ ] **Pasul 6: Citește PDF-ul pagină cu pagină.** Claude verifică:
  - nu a rămas niciun text de îndrumare din template;
  - figurile și tabelele sînt sus sau jos în coloană, cu legenda dedesubt la
    figuri și deasupra la tabele;
  - nimic nu iese din coloană;
  - referințele sînt numerotate în ordinea citării.

  Ce nu se potrivește se corectează în `articol.md`, apoi se reface pasul 5.

- [ ] **Pasul 7: Tudor citește articolul.** Corecturile lui intră în
  `articol.md`, apoi se reface pasul 5.

- [ ] **Pasul 8: Commit**

```bash
git add docs/unelte docs/articol
git commit -m "Articolul stiintific, din articol.md pe template-ul IEEE

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git push
```

---

### Task 12: PowerPoint-ul

**Cine face:** Claude (D32), cu skill-ul anthropic-skills:pptx.

**Fișiere:**
- Creează: `docs\prezentare\flota-rc.pptx` și `flota-rc.pdf`
- Creează: `docs\unelte\pptx-la-png.ps1` și `docs\prezentare\slide-uri\*.png`,
  acestea doar pentru verificare, nepuse în git
- Modifică: `.gitignore`, cu `docs/prezentare/slide-uri/` și
  `docs/unelte/node_modules/`

**Interfețe:**
- Consumă: `articol.md`, figurile din Task 10, în varianta `-ro` unde există, și
  pașii din `docs\scenariu-prezentare.md`.
- Produce: `pptx-la-png.ps1 -Pptx <cale> -Dosar <cale> [-Pdf]`. Exportă fiecare
  slide ca PNG prin PowerPoint, cu COM, și închide PowerPoint în `finally`.

- [ ] **Pasul 1: Încarcă skill-ul pptx și urmează-l.** Dacă cere o bibliotecă,
  de exemplu `pptxgenjs`, Claude întreabă întâi: „Instalez `<pachet>@<versiune>`
  de pe npm, în `docs\unelte\`, doar pentru PowerPoint. E bine?”. Instalarea se
  face cu `--save-exact`, în `docs\unelte\`, nu în `web\`.

- [ ] **Pasul 2: Fă cele 10 slide-uri din spec §16.6, în română.**
  - Slide-ul 6, „Demonstrație live”, are doar titlul și pașii, pe scurt.
  - Notele vorbitorului de pe fiecare slide iau textul „Spui” din
    `scenariu-prezentare.md`, unde există.
  - Pe fiecare slide stă o singură idee, cu figura mare. Textul are cel puțin
    20 pt.

- [ ] **Pasul 3: Exportă PNG-urile și PDF-ul și privește fiecare slide.** Claude
  verifică textul tăiat, contrastul, figurile întinse și ordinea.

- [ ] **Pasul 4: Tudor parcurge slide-urile.** Corecturile se fac, apoi se reface
  pasul 3.

- [ ] **Pasul 5: Commit**

```bash
git add .gitignore docs/prezentare/flota-rc.pptx docs/prezentare/flota-rc.pdf docs/unelte
git commit -m "Prezentarea PowerPoint, din articol

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git push
```

---

### Task 13: Manualul de utilizare

**Cine scrie:** Claude (D32).

**Fișiere:**
- Creează: `docs\manual\baza-manual.docx`, făcut o dată în Word, prin COM, din
  documentul gol, cu stilurile lui
- Creează: `docs\unelte\stiluri-manual.json`, `docs\manual\manual.md` și
  `docs\manual\capturi\*.png`
- Creează: `docs\manual\manual-flota-rc.docx` și `manual-flota-rc.pdf`

**Interfețe:**
- Consumă: `docx-din-md.ps1`, din Task 11, cu harta de stiluri a manualului:
  `Title`, `Heading 1–3`, `Normal`, `List Bullet`, `List Number`, `Caption` și
  un cuprins pus de Word după titlu.

- [ ] **Pasul 1: Fă capturile, în browserul aplicației, la 1600 × 1000.**
  Paginile: login, Harta cu panoul unei mașini, Rețea, Alarme, Setări ca
  dispecer, Setări ca admin și Conturi.

- [ ] **Pasul 2: Scrie `manual.md`, în română,** cu cele 9 capitole din spec
  §16.7. Instalarea și pornirea se iau din `README.md`. Problemele frecvente au
  fiecare un simptom, o cauză și pașii de rezolvare. „Access denied” la MySQL
  intră primul, fiindcă s-a întâmplat pe 19 octombrie.

- [ ] **Pasul 3: Fă documentul și PDF-ul, apoi citește PDF-ul pagină cu
  pagină.** Se așteaptă: cuprinsul cu toate capitolele, capturile întregi și
  lizibile, nicio pagină goală.

- [ ] **Pasul 4: Tudor citește manualul.** Corecturile se fac, apoi se reface
  pasul 3.

- [ ] **Pasul 5: Commit**

```bash
git add docs/manual docs/unelte/stiluri-manual.json
git commit -m "Manualul de utilizare

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git push
```

---

### Task 14: Repetiția și verificarea finală

Vine din Task 16 al planului fazei 5 (D33).

**Cine face:** Claude actualizează scenariul și README-ul și rulează
verificările. Tudor face repetiția, cu cronometrul.

**Fișiere:**
- Modifică: `docs\scenariu-prezentare.md` și `README.md`
- Modifică, în AIS-OS: `Facultate\memory\stare.md` și
  `Facultate\flota-rc\CLAUDE.md`

**Interfețe:** nu are.

- [ ] **Pasul 1: Actualizează `scenariu-prezentare.md`**
  - **„Cu 15 minute înainte”:** login ca `dispecer1` în tabul 1. PowerPoint-ul
    e deschis la slide-ul 1;
  - **pașii:** slide-urile 1–5, demonstrația, adică pașii 1–6 de acum, apoi
    slide-urile 7–10;
  - **ținta:** demonstrația sub 5 minute, totul sub 8 (D34);
  - **„Întrebări probabile”:** în plus, „De ce token și nu sesiune pe server?” și
    „Ce vede cineva care ascultă rețeaua?”.

- [ ] **Pasul 2: Actualizează `README.md`.** La „Documente”: articolul,
  PowerPoint-ul și manualul, cu căile lor.

- [ ] **Pasul 3: Toate testele**

Comanda: `.\mvnw.cmd -q test` și
`python -I -m unittest discover -s docs\unelte -p "test_*.py"`
Se așteaptă: `BUILD SUCCESS` și `OK`.

- [ ] **Pasul 4: Scenariul de test complet,** toți cei 7 pași din
  `docs\scenariu-test.md`.

- [ ] **Pasul 5: Repetiția, de două ori.** Tudor parcurge
  `scenariu-prezentare.md`, cu cronometrul, de la „Cu 15 minute înainte” până la
  ultimul slide. Claude notează ce s-a încurcat, iar scenariul se corectează după
  fiecare repetiție.

- [ ] **Pasul 6: Commit în proiect**

```bash
git add docs/scenariu-prezentare.md README.md
git commit -m "Scenariul prezentarii cu login si PowerPoint; README-ul final

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git push
```

- [ ] **Pasul 7: Actualizează starea în AIS-OS.** Data se pune ca AAAA-LL-ZZ, ziua
  în care s-a terminat pasul 5.
  - În `Facultate\memory\stare.md`, la RC, rândul „Unde am rămas” devine:

    ```markdown
    - **Unde am rămas:** proiectul e gata de prezentare pe AAAA-LL-ZZ: toate cele
      șase faze, scenariul de test trece, iar prezentarea, cu PowerPoint-ul, a
      fost repetată de două ori. Articolul, manualul și prezentarea stau în
      `RC-Proiect\docs\`. Rămân întrebările pentru profesor din spec §14
      (Î2, Î5–Î7, Î10).
    ```

  - În `Facultate\flota-rc\CLAUDE.md`, „Stadiu” devine
    `Stadiu: gata de prezentare, pe AAAA-LL-ZZ.`

- [ ] **Pasul 8: Commit în AIS-OS, după ce Tudor spune da**

```bash
cd "/c/Users/40747/OneDrive/Documents/AIS-OS"
git add Facultate/memory/stare.md Facultate/flota-rc/CLAUDE.md
git commit -m "flota-rc gata de prezentare

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
