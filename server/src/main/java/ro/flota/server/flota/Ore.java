package ro.flota.server.flota;

import java.sql.Timestamp;
import java.time.Instant;

/** Trecerea orelor între Java și MySQL. Conexiunea e pe UTC, deci în bază ajunge ora UTC. */
final class Ore {

    private Ore() {
    }

    static Timestamp ts(Instant ora) {
        return ora == null ? null : Timestamp.from(ora);
    }

    static Instant instant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
