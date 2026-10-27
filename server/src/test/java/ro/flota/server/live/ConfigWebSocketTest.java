package ro.flota.server.live;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

/** Originile se verifică la fel ca la handshake: Spring folosește CorsConfiguration.checkOrigin (redesign §10.2). */
class ConfigWebSocketTest {

    private static String verifica(String origine) {
        var c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of(ConfigWebSocket.ORIGINI));
        return c.checkOrigin(origine);
    }

    @Test
    void laptopulMergeCaAcum() {
        assertNotNull(verifica("http://localhost:5173"));
        assertNotNull(verifica("http://127.0.0.1:5173"));
    }

    @Test
    void retelelePrivateTrec() {
        assertNotNull(verifica("http://192.168.137.1:5173"));   // hotspot-ul din Windows
        assertNotNull(verifica("http://192.168.43.12:5173"));   // hotspot de Android
        assertNotNull(verifica("http://10.0.0.5:5173"));
        assertNotNull(verifica("http://172.16.0.2:5173"));
        assertNotNull(verifica("http://172.31.255.1:5173"));
    }

    @Test
    void adreselePubliceSintRefuzate() {
        assertNull(verifica("http://172.32.0.1:5173"));
        assertNull(verifica("http://172.15.0.1:5173"));
        assertNull(verifica("http://8.8.8.8:5173"));
        assertNull(verifica("https://exemplu.ro"));
    }
}
