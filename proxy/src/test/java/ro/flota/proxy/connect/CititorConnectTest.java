package ro.flota.proxy.connect;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CititorConnectTest {

    private static InputStream flux(byte[]... bucati) {
        int total = Arrays.stream(bucati).mapToInt(b -> b.length).sum();
        byte[] tot = new byte[total];
        int pozitie = 0;
        for (byte[] b : bucati) {
            System.arraycopy(b, 0, tot, pozitie, b.length);
            pozitie += b.length;
        }
        return new ByteArrayInputStream(tot);
    }

    private static InputStream octeti(int... valori) {
        byte[] b = new byte[valori.length];
        for (int i = 0; i < valori.length; i++) {
            b[i] = (byte) valori[i];
        }
        return new ByteArrayInputStream(b);
    }

    private static String stricat(InputStream in) {
        return assertThrows(PachetStricat.class, () -> CititorConnect.citeste(in)).getMessage();
    }

    @Test
    void citesteClientId() throws IOException {
        assertEquals("masina-3", CititorConnect.citeste(flux(Pachete.connect("masina-3"))).clientId());
    }

    @Test
    void octetiiSintCeiPrimitiIntocmai() throws IOException {
        byte[] cuWill = Pachete.connect("masina-3", 4, new byte[] {0, 3, 'a', '/', 'b', 0, 1, 'x'});
        assertArrayEquals(cuWill, CititorConnect.citeste(flux(cuWill)).octeti());
    }

    @Test
    void nuCitesteNimicDupaPachet() throws IOException {
        InputStream in = flux(Pachete.connect("masina-3"), "XYZ".getBytes(StandardCharsets.US_ASCII));
        CititorConnect.citeste(in);
        assertEquals('X', in.read());
    }

    @Test
    void lungimeaRamasaPeDoiOcteti() throws IOException {
        String lung = "masina-" + "9".repeat(200);
        byte[] pachet = Pachete.connect(lung);
        assertTrue((pachet[1] & 0x80) != 0, "lungimea ar trebui să aibă al doilea octet");
        PachetConnect p = CititorConnect.citeste(flux(pachet));
        assertEquals(lung, p.clientId());
        assertEquals(pachet.length, p.octeti().length);
    }

    @Test
    void lungimeaRamasaPeUnuPinaLaPatruOcteti() throws IOException {
        assertEquals(0, CititorConnect.lungimeRamasa(octeti(0x00)));
        assertEquals(127, CititorConnect.lungimeRamasa(octeti(0x7F)));
        assertEquals(128, CititorConnect.lungimeRamasa(octeti(0x80, 0x01)));
        assertEquals(16_383, CititorConnect.lungimeRamasa(octeti(0xFF, 0x7F)));
        assertEquals(16_384, CititorConnect.lungimeRamasa(octeti(0x80, 0x80, 0x01)));
        assertEquals(268_435_455, CititorConnect.lungimeRamasa(octeti(0xFF, 0xFF, 0xFF, 0x7F)));
    }

    @Test
    void lungimeaRamasaPeCinciOctetiEsteStricata() {
        assertThrows(PachetStricat.class, () -> CititorConnect.lungimeRamasa(octeti(0x80, 0x80, 0x80, 0x80, 0x01)));
    }

    @Test
    void primulPachetCareNuEConnectEsteRefuzat() {
        assertTrue(stricat(octeti(0x30, 0x00)).contains("0x30"));
    }

    @Test
    void unBrowserNuEsteMqtt() {
        String mesaj = stricat(flux("GET / HTTP/1.1\r\n".getBytes(StandardCharsets.US_ASCII)));
        assertTrue(mesaj.contains("nu e CONNECT"), mesaj);
    }

    @Test
    void nivelulGresitEsteRefuzat() {
        assertTrue(stricat(flux(Pachete.connect("masina-3", 5, new byte[0]))).contains("MQTT 3.1.1"));
    }

    @Test
    void pachetulTaiatEsteRefuzat() {
        byte[] pachet = Pachete.connect("masina-3");
        assertTrue(stricat(flux(Arrays.copyOf(pachet, pachet.length - 3))).contains("tăiat"));
    }

    @Test
    void clientIdGolEsteRefuzat() {
        assertTrue(stricat(flux(Pachete.connect(""))).contains("gol"));
    }

    @Test
    void lungimeaUriasaEsteRefuzataFaraSaCiteasca() {
        assertTrue(stricat(octeti(0x10, 0xFF, 0xFF, 0x7F)).contains("maximul"));
    }

    @Test
    void conexiuneaInchisaInainteDeOrice() {
        assertThrows(EOFException.class, () -> CititorConnect.citeste(octeti()));
    }
}
