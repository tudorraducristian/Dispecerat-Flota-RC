package ro.flota.simulator.simulare;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import ro.flota.simulator.testutil.AleatorScriptat;

class VitezaTest {

    private static final AleatorScriptat FARA_FRANARE = new AleatorScriptat(0.99);

    /** 0.5 înseamnă „nu e 90", iar al doilea 0.5 dă ținta 30 + 40 × 0.5 = 50. */
    private static Viteza spre50(double initiala) {
        return new Viteza(new AleatorScriptat(0.99, 0.5, 0.5), FARA_FRANARE, initiala);
    }

    @Test
    void primaTintaEsteIntre30Si70() {
        Viteza v = spre50(0);
        v.tick();
        assertEquals(50.0, v.tinta(), 1e-9);
    }

    @Test
    void rarTintaSare90() {
        Viteza v = new Viteza(new AleatorScriptat(0.99, 0.05), FARA_FRANARE, 0);
        v.tick();
        assertEquals(90.0, v.tinta(), 1e-9);
    }

    @Test
    void accelereazaCuCelMult5PeSecunda() {
        Viteza v = spre50(0);
        assertEquals(5.0, v.tick(), 1e-9);
        for (int i = 0; i < 9; i++) {
            v.tick();
        }
        assertEquals(50.0, v.viteza(), 1e-9);
        assertEquals(50.0, v.tick(), 1e-9);
    }

    @Test
    void incetinesteSpreTinta() {
        assertEquals(75.0, spre50(80).tick(), 1e-9);
    }

    @Test
    void tintaNouaVineDupa30DeSecunde() {
        Viteza v = new Viteza(new AleatorScriptat(0.99, 0.5, 0.5, 0.5, 0.0), FARA_FRANARE, 0);
        for (int i = 0; i < 30; i++) {
            v.tick();
        }
        assertEquals(50.0, v.tinta(), 1e-9);
        v.tick();
        assertEquals(30.0, v.tinta(), 1e-9);
    }

    @Test
    void franareaBruscaScade30() {
        Viteza v = new Viteza(new AleatorScriptat(0.99, 0.5, 0.75), new AleatorScriptat(0.99, 0.0), 60);
        assertEquals(30.0, v.tick(), 1e-9);
    }

    @Test
    void subViteza30NuFraneazaBrusc() {
        Viteza v = new Viteza(new AleatorScriptat(0.99, 0.5, 0.5), new AleatorScriptat(0.99, 0.0), 20);
        assertEquals(25.0, v.tick(), 1e-9);
    }

    @Test
    void limitaTaieTintaDeAcumSiPeCeleViitoare() {
        Viteza v = new Viteza(new AleatorScriptat(0.99, 0.05, 0.05), FARA_FRANARE, 0); // ținta 90, apoi tot 90
        v.tick();
        v.limiteaza(50);
        assertEquals(50.0, v.tinta(), 1e-9);
        for (int i = 0; i < 30; i++) {
            v.tick(); // la a 31-a secundă vine o țintă nouă
        }
        assertEquals(50.0, v.tinta(), 1e-9);
    }
}
