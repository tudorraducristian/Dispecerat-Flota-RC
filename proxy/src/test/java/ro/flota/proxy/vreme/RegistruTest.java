package ro.flota.proxy.vreme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RegistruTest {

    private final Registru registru = new Registru(Clock.systemUTC());

    @Test
    void masinileVinInOrdineaNumerelor() {
        registru.vreme("masina-10");
        registru.vreme("masina-2");
        registru.vreme("masina-1");
        assertEquals(List.of("masina-1", "masina-2", "masina-10"), registru.masini());
    }

    @Test
    void doarCeleVazuteSintCunoscute() {
        registru.vreme("masina-3");
        assertTrue(registru.cunoscuta("masina-3"));
        assertFalse(registru.cunoscuta("masina-4"));
    }

    @Test
    void aceeasiMasinaAreAceeasiVreme() {
        assertSame(registru.vreme("masina-3"), registru.vreme("masina-3"));
    }

    @Test
    void inchideDoarLegaturileMasiniiCerute() {
        AtomicInteger inchise3 = new AtomicInteger();
        AtomicInteger inchise4 = new AtomicInteger();
        Closeable a = inchise3::incrementAndGet;
        Closeable b = inchise3::incrementAndGet;
        Closeable c = inchise4::incrementAndGet;
        registru.adaugaLegatura("masina-3", a);
        registru.adaugaLegatura("masina-3", b);
        registru.adaugaLegatura("masina-4", c);
        registru.inchideLegaturile("masina-3");
        assertEquals(2, inchise3.get());
        assertEquals(0, inchise4.get());
    }

    @Test
    void oLegaturaScoasaNuMaiEInchisa() {
        AtomicInteger inchise = new AtomicInteger();
        Closeable a = inchise::incrementAndGet;
        registru.adaugaLegatura("masina-3", a);
        registru.scoateLegatura("masina-3", a);
        registru.inchideLegaturile("masina-3");
        assertEquals(0, inchise.get());
    }
}
