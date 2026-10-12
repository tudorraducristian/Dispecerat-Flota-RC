package ro.flota.simulator.simulare;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import ro.flota.simulator.testutil.AleatorScriptat;

class TemperaturaTest {

    /** Zgomotul 0.5 înseamnă „fără zgomot": (0.5 × 2 − 1) × 0,3 = 0. */
    private static Temperatura faraDefect(double initiala) {
        return new Temperatura(new AleatorScriptat(0.99), new AleatorScriptat(0.5), initiala);
    }

    @Test
    void seApropieCuJumatateDeGradSpreTintaDupaViteza() {
        Temperatura t = faraDefect(85);
        assertEquals(85.5, t.tick(50), 1e-9); // ținta e 85 + 0,1 × 50 = 90
        for (int i = 0; i < 19; i++) {
            t.tick(50);
        }
        assertEquals(90.0, t.tick(50), 1e-9);
    }

    @Test
    void zgomotulEsteCelMult0_3() {
        Temperatura t = new Temperatura(new AleatorScriptat(0.99), new AleatorScriptat(0.0), 90);
        assertEquals(89.7, t.tick(50), 1e-9);
    }

    @Test
    void defectulUrca2GradePeSecunda20DeSecunde() {
        Temperatura t = new Temperatura(new AleatorScriptat(0.99, 0.0), new AleatorScriptat(0.5), 90);
        double dupa20 = 0;
        for (int i = 0; i < 20; i++) {
            dupa20 = t.tick(50);
        }
        assertEquals(130.0, dupa20, 1e-9);
    }

    @Test
    void dupaDefectSeRacesteCuJumatateDeGrad() {
        Temperatura t = new Temperatura(new AleatorScriptat(0.99, 0.0), new AleatorScriptat(0.5), 90);
        for (int i = 0; i < 20; i++) {
            t.tick(50);
        }
        assertEquals(129.5, t.tick(0), 1e-9); // ținta la viteza 0 e 85
    }
}
