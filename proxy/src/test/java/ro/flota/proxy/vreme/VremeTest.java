package ro.flota.proxy.vreme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ro.flota.proxy.testutil.CeasManual;

class VremeTest {

    private final CeasManual ceas = new CeasManual("2026-12-10T10:00:00Z");
    private final Vreme vreme = new Vreme(ceas);

    @Test
    void laInceputEsteNormal() {
        assertEquals("NORMAL", vreme.descriere());
        assertFalse(vreme.inTunel());
        assertNull(vreme.gauraPana());
        assertEquals(0, vreme.bandaKbps());
    }

    @Test
    void intarziereaSiBandaSeCombina() {
        vreme.intarziere(500, 200);
        vreme.banda(64);
        assertEquals("INTARZIERE 500 200 BANDA 64", vreme.descriere());
    }

    @Test
    void intarziereaFaraVariatieSeScrieCu0() {
        vreme.intarziere(500, 0);
        assertEquals("INTARZIERE 500 0", vreme.descriere());
    }

    @Test
    void doarBanda() {
        vreme.banda(64);
        assertEquals("BANDA 64", vreme.descriere());
    }

    @Test
    void tunelulTineCatSeCere() {
        vreme.tunel(12);
        assertTrue(vreme.inTunel());
        assertEquals("TUNEL 12", vreme.descriere());
        ceas.avanseaza(11_500);
        assertEquals("TUNEL 1", vreme.descriere());
        ceas.avanseaza(500);
        assertFalse(vreme.inTunel());
        assertEquals("NORMAL", vreme.descriere());
    }

    @Test
    void tunelulAscundeIntarziereaCatDureaza() {
        vreme.intarziere(500, 0);
        vreme.tunel(5);
        assertEquals("TUNEL 5", vreme.descriere());
        ceas.avanseaza(5_000);
        assertEquals("INTARZIERE 500 0", vreme.descriere());
    }

    @Test
    void gauraSpunePanaCand() {
        vreme.gaura(10);
        assertNotNull(vreme.gauraPana());
        assertEquals("GAURA 10", vreme.descriere());
        ceas.avanseaza(10_000);
        assertNull(vreme.gauraPana());
    }

    @Test
    void gauraDupaTunelInlocuiesteTunelul() {
        vreme.tunel(30);
        vreme.gaura(10);
        assertFalse(vreme.inTunel());
        assertEquals("GAURA 10", vreme.descriere());
    }

    @Test
    void tunelDupaGauraInlocuiesteGaura() {
        vreme.gaura(30);
        vreme.tunel(10);
        assertNull(vreme.gauraPana());
        assertEquals("TUNEL 10", vreme.descriere());
    }

    @Test
    void normalOpresteTotul() {
        vreme.intarziere(500, 200);
        vreme.banda(64);
        vreme.gaura(30);
        vreme.normal();
        assertEquals("NORMAL", vreme.descriere());
        assertNull(vreme.gauraPana());
        assertEquals(0, vreme.intarziereMs());
    }
}
