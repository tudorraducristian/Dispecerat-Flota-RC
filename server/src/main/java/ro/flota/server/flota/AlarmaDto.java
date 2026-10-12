package ro.flota.server.flota;

import java.time.Instant;

/** O alarmă din tabelul alarme. sursa e MASINA sau SERVER. */
public record AlarmaDto(long id, String masinaId, String tip, String sursa, Instant ora, Instant oraSosirii,
                        String detalii, boolean vazuta) {
}
