package ro.flota.simulator.mesaje;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Orele din mesaje: UTC, ISO-8601, mereu cu milisecunde (spec §4.2). */
public final class Ore {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private Ore() {
    }

    public static String format(Instant ora) {
        return FORMAT.format(ora);
    }
}
