# Măsurătorile pentru articol

Rulate pe 2026-10-26, pe laptopul lui Tudor, cu Mosquitto 2.1.2, pe codul de la
commit-ul `6fda862`, cu `docs\articol\masuratori\masoara.ps1`. Datele brute sînt
în `docs\articol\date\`: câte un CSV pe rulare și `rezumat.csv`.

## Cum s-a măsurat

Pentru fiecare rulare:
1. contoarele mașinii se pun pe 0;
2. 20 de secunde liniștite;
3. comanda, prin portul de ordine al proxy-ului;
4. durata ei, apoi revenirea mașinii, apoi încă 30 de secunde.

`secunde_offline` numără secundele în care mașina a fost `OFFLINE` în bază,
citită o dată pe secundă, de la comandă până la final. Latența unui mesaj e ora
sosirii minus ora mașinii. În rezumat, media și p95 se calculează doar pe
mesajele live plecate cât a ținut perturbarea, de la comandă până la capătul ei.
CSV-urile acoperă o fereastră mai largă, pentru grafice.

## Rezultatele

Media pe cele trei repetiții; în paranteză, cel mai mic și cel mai mare.

| Experiment | Pierdute | Recuperate | Secunde offline | Latența în perturbare |
|---|---|---|---|---|
| E1 · `TUNEL masina-3 15` | 0 (0–0) | 19 (18–20) | 17,7 (17–18) | niciun mesaj live |
| E2 · `GAURA masina-5 10` | 6,3 (6–7) | 3,7 (3–4) | 3 (3–3) | un singur mesaj live, 107 ms, într-o repetiție |
| E3 · `GAURA masina-5 4` | 0 (0–0) | 0 (0–0) | 0 (0–0) | medie 1 983 ms (1 668–2 147), p95 3 149–4 124 ms |
| E4 · `INTARZIERE TOATE 500 200`, 60 s | 0 | 0 | 0 | medie 532 ms, p95 700 ms; niciun rând din buffer |

Ce arată fiecare rând:
- **E1:** tunelul închide conexiunea pe loc, deci mașina știe imediat că e
  offline: nu pierde nimic, iar cele vreo 19 mesaje din tunel ajung din buffer.
- **E2:** în gaură, conexiunea pare vie până o închide keepalive-ul, la vreo 6,6
  secunde; ce a plecat până atunci se pierde, iar restul vine din buffer.
- **E3:** o gaură mai scurtă decât pragul keepalive-ului nu pierde nimic: mesajele
  stau în gaură și ajung după ea, cu o întârziere de până la 4 secunde.
- **E4:** întârzierea de 500 ± 200 ms se vede întreagă în latență, fără să schimbe
  ordinea mesajelor și fără să ducă vreo mașină în buffer.

Toate patru se potrivesc cu așteptările din spec §10.
