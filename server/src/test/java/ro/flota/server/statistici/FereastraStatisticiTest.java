package ro.flota.server.statistici;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class FereastraStatisticiTest {

    private static final Instant T0 = Instant.parse("2026-12-10T10:00:00Z");

    private final FereastraStatistici f = new FereastraStatistici();

    private static Instant la(long ms) {
        return T0.plusMillis(ms);
    }

    @Test
    void zeceMesajeInZeceSecundeInseamnaUnulPeSecunda() {
        for (int i = 0; i < 10; i++) {
            f.mesaj(la(i * 1_000L), la(i * 1_000L + 20), false);
        }
        assertEquals(1.0, f.mesajePeSecunda(la(9_500)), 1e-9);
    }

    @Test
    void mesajeleVechiIesDinFereastra() {
        for (int i = 0; i < 10; i++) {
            f.mesaj(la(i * 1_000L), la(i * 1_000L + 20), false);
        }
        assertEquals(0.5, f.mesajePeSecunda(la(15_000)), 1e-9);
    }

    @Test
    void latentaEsteMediaUltimelorZeceMesajeLive() {
        for (int i = 1; i <= 12; i++) {
            f.mesaj(la(i * 1_000L), la(i * 1_000L + i * 100L), false); // latențe de 100, 200, ..., 1200 ms
        }
        assertEquals(750.0, f.latentaMedieMs(), 1e-9); // media lui 300..1200
    }

    @Test
    void mesajeleRecuperateNuIntraInLatenta() {
        f.mesaj(la(0), la(100), false);
        f.mesaj(la(1_000), la(30_000), true);
        assertEquals(100.0, f.latentaMedieMs(), 1e-9);
    }

    @Test
    void mesajeleRecuperateIntraInMesajePeSecunda() {
        f.mesaj(la(0), la(100), false);
        f.mesaj(la(1_000), la(200), true);
        assertEquals(0.2, f.mesajePeSecunda(la(1_000)), 1e-9);
    }

    @Test
    void faraMesajeLiveLatentaLipseste() {
        assertNull(f.latentaMedieMs());
        f.mesaj(la(0), la(5_000), true);
        assertNull(f.latentaMedieMs());
    }
}
