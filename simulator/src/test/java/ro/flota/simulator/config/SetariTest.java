package ro.flota.simulator.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.junit.jupiter.api.Test;

class SetariTest {

    private static Properties bune() {
        Properties p = new Properties();
        p.setProperty("broker.host", "127.0.0.1");
        p.setProperty("broker.port", "1883");
        p.setProperty("keepalive.secunde", "5");
        p.setProperty("perioada.ms", "1000");
        p.setProperty("buffer.marime", "300");
        return p;
    }

    @Test
    void citesteToateCheile() {
        assertEquals(new Setari("127.0.0.1", 1883, 5, 1000, 300), Setari.din(bune()));
    }

    @Test
    void cheiaLipsaEstePusaPeNume() {
        Properties p = bune();
        p.remove("broker.port");
        String mesaj = assertThrows(IllegalArgumentException.class, () -> Setari.din(p)).getMessage();
        assertTrue(mesaj.contains("broker.port"), mesaj);
    }

    @Test
    void unPortCareNuENumarEstePusPeNume() {
        Properties p = bune();
        p.setProperty("broker.port", "abc");
        String mesaj = assertThrows(IllegalArgumentException.class, () -> Setari.din(p)).getMessage();
        assertTrue(mesaj.contains("broker.port") && mesaj.contains("abc"), mesaj);
    }

    @Test
    void unPortInAfaraIntervaluluiEsteRefuzat() {
        Properties p = bune();
        p.setProperty("broker.port", "70000");
        assertThrows(IllegalArgumentException.class, () -> Setari.din(p));
    }

    @Test
    void fisierulDinResurseTrecePrinProxy() {
        assertEquals(new Setari("127.0.0.1", 1884, 5, 1000, 300), Setari.implicite());
    }

    @Test
    void bufferulPreaMicEsteRefuzat() {
        Properties p = bune();
        p.setProperty("buffer.marime", "5");
        String mesaj = assertThrows(IllegalArgumentException.class, () -> Setari.din(p)).getMessage();
        assertTrue(mesaj.contains("buffer.marime"), mesaj);
    }
}
