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

## Testele

```powershell
.\mvnw.cmd -q test
```

## Baza, pentru verificări

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
& $mysql -h 127.0.0.1 -u flota -pflota flota
```
