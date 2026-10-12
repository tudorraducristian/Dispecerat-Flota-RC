package ro.flota.simulator.testutil;

import java.util.List;
import ro.flota.simulator.geo.Punct;
import ro.flota.simulator.geo.Traseu;

/** Trasee mici, cu lungimi știute, pentru teste. */
public final class Trasee {

    private Trasee() {
    }

    /**
     * Un pătrat lângă 46°N 23°E: spre nord, spre est, spre sud, apoi înapoi spre vest.
     * Laturile nord-sud au 111,19493 m; cele est-vest, 77,24109 m sus și 77,24249 m jos.
     */
    public static Traseu patrat() {
        return new Traseu("patrat", List.of(
                new Punct(46.000, 23.000),
                new Punct(46.001, 23.000),
                new Punct(46.001, 23.001),
                new Punct(46.000, 23.001)));
    }
}
