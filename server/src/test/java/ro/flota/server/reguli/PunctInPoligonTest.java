package ro.flota.server.reguli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.server.date.Punct;

class PunctInPoligonTest {

    /** Un pătrat de 0,01° pe latură, închis cum îl salvează geojson.io: primul punct repetat la capăt. */
    private static final List<Punct> PATRAT = List.of(
            new Punct(46.00, 23.00), new Punct(46.00, 23.01), new Punct(46.01, 23.01),
            new Punct(46.01, 23.00), new Punct(46.00, 23.00));

    /** Un L: pătratul fără sfertul din dreapta sus. */
    private static final List<Punct> L = List.of(
            new Punct(46.00, 23.00), new Punct(46.00, 23.01), new Punct(46.005, 23.01),
            new Punct(46.005, 23.005), new Punct(46.01, 23.005), new Punct(46.01, 23.00));

    @Test
    void centrulEsteInauntru() {
        assertTrue(PunctInPoligon.contine(PATRAT, new Punct(46.005, 23.005)));
    }

    @Test
    void puncteleDinAfaraSintAfara() {
        assertFalse(PunctInPoligon.contine(PATRAT, new Punct(46.02, 23.005)));
        assertFalse(PunctInPoligon.contine(PATRAT, new Punct(46.005, 22.99)));
    }

    @Test
    void margineaEsteInauntru() {
        assertTrue(PunctInPoligon.contine(PATRAT, new Punct(46.00, 23.005)));
        assertTrue(PunctInPoligon.contine(PATRAT, new Punct(46.005, 23.01)));
    }

    @Test
    void coltulEsteInauntru() {
        assertTrue(PunctInPoligon.contine(PATRAT, new Punct(46.01, 23.01)));
    }

    @Test
    void scobituraUnuiPoligonConcavEsteAfara() {
        assertTrue(PunctInPoligon.contine(L, new Punct(46.002, 23.008)));
        assertFalse(PunctInPoligon.contine(L, new Punct(46.008, 23.008)));
    }

    @Test
    void inelulNeinchisMergeLaFel() {
        List<Punct> deschis = PATRAT.subList(0, 4);
        assertTrue(PunctInPoligon.contine(deschis, new Punct(46.005, 23.005)));
        assertFalse(PunctInPoligon.contine(deschis, new Punct(46.02, 23.005)));
    }
}
