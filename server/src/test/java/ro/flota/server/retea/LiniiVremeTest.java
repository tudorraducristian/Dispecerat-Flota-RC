package ro.flota.server.retea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ro.flota.server.api.CerereGresita;

class LiniiVremeTest {

    private static String linie(String comanda, String tinta, Map<String, Integer> parametri) {
        return LiniiVreme.linie(new CerereVreme(comanda, tinta, parametri));
    }

    @Test
    void fiecareComandaDevineLiniaEi() {
        assertEquals("TUNEL masina-3 15", linie("TUNEL", "masina-3", Map.of("secunde", 15)));
        assertEquals("GAURA masina-5 10", linie("GAURA", "masina-5", Map.of("secunde", 10)));
        assertEquals("INTARZIERE TOATE 500 200", linie("INTARZIERE", "TOATE", Map.of("ms", 500, "variatie", 200)));
        assertEquals("INTARZIERE TOATE 500 0", linie("INTARZIERE", "TOATE", Map.of("ms", 500)));
        assertEquals("BANDA masina-3 64", linie("BANDA", "masina-3", Map.of("kbps", 64)));
        assertEquals("NORMAL TOATE", linie("NORMAL", "TOATE", null));
    }

    @Test
    void literelemariSauMiciSeNormalizeaza() {
        assertEquals("TUNEL TOATE 15", linie("tunel", "toate", Map.of("secunde", 15)));
        assertEquals("NORMAL masina-3", linie("normal", "Masina-3", Map.of()));
    }

    @Test
    void tintaCuSpatiiSauRandNouEsteRefuzata() {
        for (String tinta : new String[] {"masina-3\nSTARE", "masina 3", "", null, "masina-"}) {
            assertThrows(CerereGresita.class, () -> linie("NORMAL", tinta, Map.of()), String.valueOf(tinta));
        }
    }

    @Test
    void numarulLipsaEsteRefuzat() {
        assertThrows(CerereGresita.class, () -> linie("TUNEL", "masina-3", Map.of()));
        assertThrows(CerereGresita.class, () -> linie("BANDA", "masina-3", null));
    }

    @Test
    void comandaNecunoscutaEsteRefuzata() {
        assertThrows(CerereGresita.class, () -> linie("PLOAIE", "masina-3", Map.of()));
        assertThrows(CerereGresita.class, () -> linie(null, "masina-3", Map.of()));
    }

    @Test
    void intervaleleLeVerificaProxyulNuServerul() {
        assertEquals("TUNEL masina-3 9999", linie("TUNEL", "masina-3", Map.of("secunde", 9_999)));
    }

    @Test
    void sfarsitulExistaDoarLaTunelSiGaura() {
        Instant t0 = Instant.parse("2026-12-10T10:00:00Z");
        assertEquals(t0.plusSeconds(15), LiniiVreme.sfarsit(new CerereVreme("TUNEL", "masina-3", Map.of("secunde", 15)), t0));
        assertNull(LiniiVreme.sfarsit(new CerereVreme("NORMAL", "masina-3", Map.of()), t0));
    }

    @Test
    void stareaSeImparteLaPrimulSpatiu() {
        assertEquals(new VremeMasinaDto("masina-5", "INTARZIERE 500 200 BANDA 64"),
                VremeMasinaDto.din("masina-5 INTARZIERE 500 200 BANDA 64"));
        assertEquals(List.of(new VremeMasinaDto("masina-1", "NORMAL")), List.of(VremeMasinaDto.din("masina-1 NORMAL")));
    }
}
