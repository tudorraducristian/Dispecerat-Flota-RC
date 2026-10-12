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

3. Simulatorul, într-un al treilea terminal:

   ```powershell
   .\mvnw.cmd -q -pl simulator compile exec:java
   ```

   Se oprește cu Ctrl+C. La „Terminate batch job (Y/N)?" răspunzi Y.

## Testele

```powershell
.\mvnw.cmd -q test
```
