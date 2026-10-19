package ro.flota.proxy.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class PlanificatorTest {

    @Test
    void faraVariatieEsteExactIntarzierea() {
        Planificator p = new Planificator(new Random(1));
        assertEquals(1_500, p.plecare(1_000, 500, 0));
    }

    @Test
    void variatiaNuSchimbaNiciodataOrdinea() {
        Planificator p = new Planificator(new Random(42));
        long acum = 0;
        long anterioara = Long.MIN_VALUE;
        for (int i = 0; i < 10_000; i++) {
            acum += i % 7; // bucăți venite la 0–6 ms una după alta
            long plecare = p.plecare(acum, 500, 400);
            assertTrue(plecare >= anterioara, "bucata " + i + " ar pleca înaintea celei dinainte");
            assertTrue(plecare >= acum + 100, "bucata " + i + " pleacă prea devreme");
            anterioara = plecare;
        }
    }

    @Test
    void variatiaStaInInterval() {
        Planificator p = new Planificator(new Random(7));
        for (long acum = 0; acum < 10_000_000; acum += 10_000) {
            long intarziere = p.plecare(acum, 500, 200) - acum;
            assertTrue(intarziere >= 300 && intarziere <= 700, "întârzierea " + intarziere);
        }
    }

    @Test
    void nuPleacaNiciodataInainteDeAcum() {
        Planificator p = new Planificator(new Random(3));
        for (long acum = 0; acum < 1_000_000; acum += 10_000) {
            assertTrue(p.plecare(acum, 100, 300) >= acum);
        }
    }
}
