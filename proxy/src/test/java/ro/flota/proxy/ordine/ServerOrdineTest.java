package ro.flota.proxy.ordine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Ce scrie portul de ordine în consolă. STARE vine la 3 secunde din fiecare tab deschis (faza 5). */
class ServerOrdineTest {

    @Test
    void comandaIntraInJurnal() {
        assertEquals("Ordine: TUNEL masina-3 15 → OK",
                ServerOrdine.randJurnal(new Linie("TUNEL masina-3 15", false), List.of("OK")));
    }

    @Test
    void stareaReusitaNuIntraInJurnal() {
        assertNull(ServerOrdine.randJurnal(new Linie("STARE", false), List.of("masina-1 NORMAL", ".")));
        assertNull(ServerOrdine.randJurnal(new Linie(" stare ", false), List.of(".")));
    }

    @Test
    void stareaGresitaIntraInJurnal() {
        assertEquals("Ordine: STARE x → ERR parametri gresiti",
                ServerOrdine.randJurnal(new Linie("STARE x", false), List.of(Ordine.ERR_PARAMETRI)));
    }

    @Test
    void liniaPreaLungaIntraInJurnal() {
        assertEquals("Ordine: (linie prea lungă) → ERR linie prea lunga",
                ServerOrdine.randJurnal(new Linie("", true), List.of(Ordine.ERR_LINIE)));
    }
}
