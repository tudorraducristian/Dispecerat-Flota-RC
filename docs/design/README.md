# Designul și planurile pentru flota-rc

Proiectul de Rețele de Calculatoare, anul 4, semestrul 1. Zece mașini simulate
raportează prin MQTT la un dispecerat. Între ele și broker stă un proxy care
strică rețeaua la comandă, iar dispeceratul arată live, pe hartă, cum rezistă
sistemul.

## Ce e aici

| Fișier | Ce conține |
|---|---|
| `2026-10-08-flota-rc-design.md` | spec-ul: deciziile D1–D34, arhitectura, mesajele, proxy-ul, serverul, UI-ul, fazele, testele și, în §16, faza 6 |
| `planuri\2026-10-08-flota-rc-faza-0-1.md` | fazele 0 și 1: pregătirea și mașinile care vorbesc, în 13 task-uri |
| `planuri\2026-10-08-flota-rc-faza-2.md` | faza 2: serverul și harta, în 11 task-uri |
| `planuri\2026-10-08-flota-rc-faza-3.md` | faza 3: proxy-ul de rețea proastă, în 9 task-uri |
| `planuri\2026-10-08-flota-rc-faza-4.md` | faza 4: mașina rezistă, în 9 task-uri |
| `planuri\2026-10-08-flota-rc-faza-5.md` | faza 5: restul, în 16 task-uri; repetiția s-a mutat în faza 6 |
| `planuri\2026-10-19-flota-rc-faza-6.md` | faza 6: login, conturi, setări, meniu și documentele de predare, în 14 task-uri |
| `sesiuni\2026-10-08-flota-rc-spec-si-plan.md` | rezumatul sesiunii în care s-au scris spec-ul și primul plan |

## De unde vin

Sînt copii. Originalele stau în AIS-OS, în `docs\superpowers\specs\`,
`docs\superpowers\plans\` și `sesiuni\` (D27). Căile din interiorul lor, de
exemplu `docs\superpowers\specs\…`, se referă la AIS-OS, nu la repo-ul ăsta.

O schimbare se face întâi în AIS-OS, apoi se copiază aici, în același commit cu
codul care o folosește.
