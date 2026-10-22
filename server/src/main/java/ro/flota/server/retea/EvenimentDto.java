package ro.flota.server.retea;

import java.time.Instant;

/** Un rând din jurnalul rețelei: ce linie s-a trimis proxy-ului și ce a răspuns. */
public record EvenimentDto(long id, String tinta, String linie, String raspuns, Instant oraStart, Instant oraSfarsit) {
}
