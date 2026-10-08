---
data: 2026-10-08
categorii: [Facultate]
proiecte: [flota-rc]
etichete: [rc, retele, mqtt, mosquitto, hivemq, iot, brainstorming, spec, plan]
sursa: rezumat la cerere
---

# flota-rc, proiectul de Rețele de Calculatoare: de la idee la plan

**TL;DR:** tema e aleasă (o flotă de mașini conectate), spec-ul e aprobat și comis, iar planul fazelor 0–1 e scris și comis. Lipsește alegerea modului de execuție; niciun task nu e început.

## Pentru sesiunea care continuă, de exemplu din VS Code

- **Citește în ordine:**
  1. fișierul ăsta;
  2. `Facultate\memory\stare.md`, blocul despre RC;
  3. spec-ul, `docs\superpowers\specs\2026-10-08-flota-rc-design.md`;
  4. planul, `docs\superpowers\plans\2026-10-08-flota-rc-faza-0-1.md`.
- **Primul lucru:** îl întrebi pe Tudor ce mod de execuție alege. Recomandarea a
  fost **Native**, adică `superpowers:executing-plans` în sesiune, fiindcă Tudor
  scrie o parte din cod, iar Claude îi explică tiparele. Abia apoi începe Task 1.
- **Task 1 e în AIS-OS:** creează `Facultate\flota-rc\CLAUDE.md`, rescrie
  `Facultate\CLAUDE.md`, pune `flota-rc` în tabelul E al mamei și restrânge
  blocul de RC din `stare.md`. Task-urile 2–13 se fac în
  `C:\Users\40747\OneDrive\Documents\Facultate\An 4\Semestrul 1\RC-Proiect\`,
  care e încă gol, fără git.
- **Regulile planului:**
  - **Metoda mixtă:** task-urile marcate „Tudor (repetiție)" le scrie Tudor;
    Claude revizuiește fără să rescrie. Soluția de referință stă în blocul
    ascuns, pe care Tudor nu-l deschide înainte să încerce.
  - **Descărcările** se fac doar cu acordul lui Tudor: `starter.zip`, Maven-ul
    adus de `mvnw` și instalatorul Mosquitto.
  - **Setările de sistem le face Tudor,** de exemplu serviciul Mosquitto.
- **Explicațiile:** întâi analogia și de ce contează, apoi partea tehnică. Vezi
  memoria `explica-pe-inteles`.

## Ce am discutat

- Tudor a creat folderul `RC-Proiect` și l-a anunțat ca proiectul lui viitor.
  Cerința de la RC: o aplicație client-server cu UI, pe o temă IoT, doar software.
  Profesorul acceptă orice tehnologie de rețea.
- Patru teme propuse: flotă de mașini, sală de fitness, seră, casă inteligentă.
  Tudor a ales flota, fiindcă tunelul face din problemele de rețea chiar subiectul.
- Designul a fost prezentat în șase secțiuni, fiecare aprobată de Tudor:
  1. piesele și drumurile;
  2. mașina și mesajele;
  3. proxy-ul;
  4. serverul;
  5. UI-ul;
  6. fazele, metoda, testele și prezentarea.
- La scrierea planului s-au verificat pe Maven Central, în javadoc și pe
  mosquitto.org versiunile și API-urile folosite.

## Ce am decis

- **Mărimea e ambițioasă,** iar termenul e înainte de vacanța de iarnă, deci vreo 10
  săptămâni, împărțite cu SCD.
- **Stack-ul, varianta A, „Java peste tot":**
  - Mosquitto e broker-ul;
  - simulatorul, proxy-ul și serverul sînt în Java 21, serverul cu Spring Boot
    4.1;
  - baza e MySQL, iar UI-ul e Vue 3 cu Vuetify și Leaflet;
  - totul se scrie în VS Code.
- **Singura piesă peste bază e un proxy de rețea proastă.** Simulează întârziere,
  bandă îngustă, tunel și gaură neagră. Nu pierde pachete, fiindcă pe TCP ar
  strica fluxul. Ordinele le primește printr-un protocol text scris de Tudor, pe
  portul 1885 (management out-of-band).
- **QoS pe tip de mesaj:** telemetria pe 0, alarma pe 2, iar starea, comenzile și
  confirmările pe 1. Starea e reținută și are Last Will.
- **Mașina are două fire,** motorul și radioul, iar bufferul e coada dintre ele.
  Fiecare mesaj are număr de ordine și semnul `din_buffer`.
- **Fără login.** Zonele vin din GeoJSON și doar se afișează.
- **Corecturi făcute la scrierea planului:**
  - D17: clientul MQTT e HiveMQ 1.4.0, nu Paho, a cărui ultimă versiune e din
    iulie 2020;
  - D18: traseele și zonele sînt GeoJSON, desenate în geojson.io;
  - testele merg pe JUnit 6.
- **Numele proiectului în AIS-OS e `flota-rc`.**
- **Câte un plan pe fază.** Planul fazelor 0–1 are 13 task-uri. Fazele 2–5 primesc
  planuri proprii, când se ajunge la ele.
- **Commit-urile din AIS-OS:**
  - `6685115`, spec-ul;
  - `ab40fc6`, planul și corecturile din spec.

  `Facultate\memory\stare.md` și `sesiuni\` sînt încă necomise.

## Ce urmează

1. Tudor alege modul de execuție. Recomandarea e Native.
2. Task 1, în AIS-OS. La commit, Tudor hotărăște dacă `stare.md` intră cu tot cu
   rezumatul SCD din 3 octombrie.
3. Task-urile 2–13, în `RC-Proiect`. Ce trebuie să facă Tudor:
   - instalează Mosquitto 2.1.2, în Task 3;
   - alege orașul traseelor, adică Î1, și desenează în geojson.io, în Task 4;
   - scrie `Setari`, `Alarma` și `DetectorFranare`.
4. Întrebările încă deschise, care se pun la curs: data prezentării și ce cere
   profesorul la predare, inclusiv ce acceptă la folosirea AI.
