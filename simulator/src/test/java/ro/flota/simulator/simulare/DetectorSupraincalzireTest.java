package ro.flota.simulator.simulare;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DetectorSupraincalzireTest {

    @Test
    void daAlarmaOSinguraDataPeEpisod() {
        DetectorSupraincalzire d = new DetectorSupraincalzire();
        assertFalse(d.verifica(105));
        assertTrue(d.verifica(111));
        assertFalse(d.verifica(115));
        assertFalse(d.verifica(105)); // între 100 și 110, alarma rămâne consumată
    }

    @Test
    void seRearmeazaSub100() {
        DetectorSupraincalzire d = new DetectorSupraincalzire();
        assertTrue(d.verifica(111));
        assertFalse(d.verifica(99));
        assertTrue(d.verifica(111));
    }
}
