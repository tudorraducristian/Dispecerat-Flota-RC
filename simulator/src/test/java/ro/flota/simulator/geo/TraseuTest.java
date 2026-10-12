package ro.flota.simulator.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.testutil.Trasee;

class TraseuTest {

    private static final double LATURA_NS = 111.19493;
    private static final double LATURA_EV_SUS = 77.24109;
    private static final double LATURA_EV_JOS = 77.24249;

    private static final Punct A = new Punct(46.000, 23.000);
    private static final Punct B = new Punct(46.001, 23.000);
    private static final Punct C = new Punct(46.001, 23.001);
    private static final Punct D = new Punct(46.000, 23.001);

    @Test
    void oMiimeDeGradPeMeridianInseamna111Metri() {
        assertEquals(LATURA_NS, A.distantaPana(B), 0.001);
    }

    @Test
    void lungimeaIncludeDrumulInapoiLaStart() {
        assertEquals(2 * LATURA_NS + LATURA_EV_SUS + LATURA_EV_JOS, Trasee.patrat().lungime(), 0.01);
    }

    @Test
    void laDistanta0EsteStartul() {
        assertEquals(A, Trasee.patrat().punctLa(0));
    }

    @Test
    void laJumatateaPrimeiLaturiEsteMijloculEi() {
        Punct p = Trasee.patrat().punctLa(LATURA_NS / 2);
        assertEquals(46.0005, p.lat(), 1e-7);
        assertEquals(23.0, p.lon(), 1e-9);
    }

    @Test
    void dupaOTuraCompletaOiaDeLaCapat() {
        Traseu t = Trasee.patrat();
        assertAcelasiPunct(t.punctLa(10), t.punctLa(t.lungime() + 10));
    }

    @Test
    void distantaNegativaSeNumaraDeLaCapat() {
        Traseu t = Trasee.patrat();
        assertAcelasiPunct(t.punctLa(t.lungime() - 10), t.punctLa(-10));
    }

    @Test
    void puncteleDubluriNuStricaTraseul() {
        Traseu fara = Trasee.patrat();
        Traseu cuDubluri = new Traseu("dubluri", List.of(A, A, B, C, C, D));
        assertEquals(fara.lungime(), cuDubluri.lungime(), 1e-9);
        for (double d = 0; d < fara.lungime(); d += 7.3) {
            assertAcelasiPunct(fara.punctLa(d), cuDubluri.punctLa(d));
        }
    }

    @Test
    void buclaInchisaDeManaAreAceeasiLungime() {
        Traseu inchis = new Traseu("inchis", List.of(A, B, C, D, A));
        assertEquals(Trasee.patrat().lungime(), inchis.lungime(), 1e-9);
        assertAcelasiPunct(Trasee.patrat().punctLa(300), inchis.punctLa(300));
    }

    @Test
    void refuzaTraseulCuUnSingurPunct() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new Traseu("t7", List.of(A)));
        assertTrue(e.getMessage().contains("t7"));
    }

    @Test
    void refuzaTraseulCuToatePuncteleIdentice() {
        assertThrows(IllegalArgumentException.class, () -> new Traseu("t8", List.of(A, A, A)));
    }

    private static void assertAcelasiPunct(Punct asteptat, Punct real) {
        assertEquals(asteptat.lat(), real.lat(), 1e-9);
        assertEquals(asteptat.lon(), real.lon(), 1e-9);
    }
}
