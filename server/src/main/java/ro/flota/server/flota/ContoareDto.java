package ro.flota.server.flota;

/** Contoarele unei mașini, fără nimic altceva: se citesc o dată pe secundă. */
public record ContoareDto(String id, long recuperate, long pierdute) {
}
