package ro.flota.simulator.mesaje;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class OreTest {

    @Test
    void scrieMilisecundeleChiarCandSintZero() {
        assertEquals("2026-12-10T10:15:03.000Z", Ore.format(Instant.parse("2026-12-10T10:15:03Z")));
    }

    @Test
    void taieTotCeEDupaMilisecunde() {
        assertEquals("2026-12-10T10:15:03.120Z", Ore.format(Instant.parse("2026-12-10T10:15:03.120987Z")));
    }

    @Test
    void scrieMereuInUtc() {
        assertEquals("2026-07-01T21:30:00.500Z", Ore.format(Instant.parse("2026-07-02T00:30:00.500+03:00")));
    }
}
