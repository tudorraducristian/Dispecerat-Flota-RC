package ro.flota.server.conturi;

import java.time.Instant;

/** Un rând din tabelul utilizatori. Hash-ul parolei nu iese niciodată din server: în REST pleacă UtilizatorDto. */
public record Utilizator(long id, String nume, String hashParola, Rol rol, Instant creatLa) {
}
