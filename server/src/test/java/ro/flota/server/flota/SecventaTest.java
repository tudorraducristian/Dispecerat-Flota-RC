package ro.flota.server.flota;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SecventaTest {

    @Test
    void primulMesajNuNumaraPierderi() {
        assertEquals(new Secventa(0, false), Secventa.dupa(null, 500));
    }

    @Test
    void urmatorulNuPierdeNimic() {
        assertEquals(new Secventa(0, false), Secventa.dupa(41L, 42));
    }

    @Test
    void golulNumaraCeLipseste() {
        assertEquals(new Secventa(3, false), Secventa.dupa(41L, 45));
    }

    @Test
    void repornireaNuNumaraPierderi() {
        assertEquals(new Secventa(0, true), Secventa.dupa(5_000L, 1));
    }

    @Test
    void acelasiNumarDeDouaOriNuNumaraNimic() {
        assertEquals(new Secventa(0, false), Secventa.dupa(42L, 42));
    }
}
