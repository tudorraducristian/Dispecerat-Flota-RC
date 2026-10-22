package ro.flota.server.reguli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegulaOfflineTest {

    private static final Instant T0 = Instant.parse("2026-12-10T10:00:00Z");

    private final RegulaOffline regula = new RegulaOffline();

    private static Instant dupa(int secunde) {
        return T0.plusSeconds(secunde);
    }

    @Test
    void exact60DeSecundeNuDaIncaAlarma() {
        regula.stare("masina-3", false, T0);
        assertEquals(List.of(), regula.deAlarmat(dupa(60)));
    }

    @Test
    void pestePragDaOSinguraData() {
        regula.stare("masina-3", false, T0);
        assertEquals(List.of("masina-3"), regula.deAlarmat(dupa(61)));
        assertEquals(List.of(), regula.deAlarmat(dupa(70)));
    }

    @Test
    void revenireaOnlineRearmeaza() {
        regula.stare("masina-3", false, T0);
        regula.deAlarmat(dupa(61));
        regula.stare("masina-3", true, dupa(80));
        regula.stare("masina-3", false, dupa(90));
        assertEquals(List.of(), regula.deAlarmat(dupa(140)));
        assertEquals(List.of("masina-3"), regula.deAlarmat(dupa(151)));
    }

    @Test
    void doarMasinileOfflineSintVerificate() {
        regula.stare("masina-3", false, T0);
        regula.stare("masina-5", true, T0);
        assertEquals(List.of("masina-3"), regula.deAlarmat(dupa(61)));
    }

    @Test
    void unOfflineRepetatNuMutaOra() {
        regula.stare("masina-3", false, T0);
        regula.stare("masina-3", false, dupa(30)); // de exemplu, starea reținută primită din nou
        assertEquals(List.of("masina-3"), regula.deAlarmat(dupa(61)));
    }
}
