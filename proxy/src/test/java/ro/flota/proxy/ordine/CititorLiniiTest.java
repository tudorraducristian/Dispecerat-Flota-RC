package ro.flota.proxy.ordine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class CititorLiniiTest {

    private static CititorLinii din(String text) {
        return new CititorLinii(new StringReader(text));
    }

    @Test
    void citesteLiniiCuLfSiCuCrLf() throws IOException {
        CititorLinii c = din("A\nB\r\nC\n");
        assertEquals(new Linie("A", false), c.urmatoarea());
        assertEquals(new Linie("B", false), c.urmatoarea());
        assertEquals(new Linie("C", false), c.urmatoarea());
        assertNull(c.urmatoarea());
    }

    @Test
    void liniaDe200EsteAcceptataChiarCuCr() throws IOException {
        String l = "x".repeat(200);
        assertEquals(new Linie(l, false), din(l + "\r\n").urmatoarea());
    }

    @Test
    void liniaDe201EstePreaLungaIarUrmatoareaSeCitesteNormal() throws IOException {
        CititorLinii c = din("x".repeat(201) + "\nSTARE\n");
        assertTrue(c.urmatoarea().preaLunga());
        assertEquals(new Linie("STARE", false), c.urmatoarea());
    }

    @Test
    void oLinieUriasaEsteAruncataFaraSaStriceConexiunea() throws IOException {
        CititorLinii c = din("x".repeat(100_000) + "\nSTARE\n");
        Linie lunga = c.urmatoarea();
        assertTrue(lunga.preaLunga());
        assertEquals("", lunga.text());
        assertEquals("STARE", c.urmatoarea().text());
    }

    @Test
    void liniaGoalaEsteOLinieGoala() throws IOException {
        assertEquals(new Linie("", false), din("\n").urmatoarea());
    }

    @Test
    void ultimaLinieFaraLfSeCitesteOricum() throws IOException {
        CititorLinii c = din("STARE");
        assertEquals(new Linie("STARE", false), c.urmatoarea());
        assertNull(c.urmatoarea());
    }

    @Test
    void conexiuneaInchisaDaNull() throws IOException {
        assertNull(din("").urmatoarea());
    }
}
