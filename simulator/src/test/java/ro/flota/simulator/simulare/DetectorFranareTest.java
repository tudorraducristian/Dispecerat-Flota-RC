package ro.flota.simulator.simulare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DetectorFranareTest {

    @Test
    void primaVitezaNuPoateFiFranare() {
        assertFalse(new DetectorFranare().verifica(60));
    }

    @Test
    void oScadereDePeste25IntrOSecundaEsteFranareBrusca() {
        DetectorFranare d = new DetectorFranare();
        d.verifica(60);
        assertTrue(d.verifica(30));
        assertEquals(30.0, d.ultimaScadere(), 1e-9);
    }

    @Test
    void exact25NuEsteFranareBrusca() {
        DetectorFranare d = new DetectorFranare();
        d.verifica(60);
        assertFalse(d.verifica(35));
    }

    @Test
    void oIncetinireTreptataNuEsteFranare() {
        DetectorFranare d = new DetectorFranare();
        for (double v = 60; v >= 0; v -= 5) {
            assertFalse(d.verifica(v));
        }
    }

    @Test
    void dupaFranareUrmatoareaSecundaNuMaiDaAlarma() {
        DetectorFranare d = new DetectorFranare();
        d.verifica(60);
        d.verifica(30);
        assertFalse(d.verifica(30));
    }
}
