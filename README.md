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
