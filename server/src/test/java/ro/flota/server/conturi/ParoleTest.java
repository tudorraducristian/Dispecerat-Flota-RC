package ro.flota.server.conturi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParoleTest {

    private final Parole parole = new Parole();

    @Test
    void parolaBunaSePotriveste() {
        assertTrue(parole.potrivite("dispecer-2026", parole.hash("dispecer-2026")));
    }

    @Test
    void parolaGresitaNuSePotriveste() {
        assertFalse(parole.potrivite("alta-parola", parole.hash("dispecer-2026")));
    }

    /** Sarea e alta de fiecare dată. */
    @Test
    void acelasiTextDaHashuriDiferite() {
        assertNotEquals(parole.hash("dispecer-2026"), parole.hash("dispecer-2026"));
    }

    @Test
    void hashulEsteBCrypt() {
        assertTrue(parole.hash("dispecer-2026").startsWith("$2a$10$"));
    }
}
