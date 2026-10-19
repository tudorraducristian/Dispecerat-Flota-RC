package ro.flota.proxy.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ro.flota.proxy.vreme.Registru;

class LegaturaTest {

    private final List<Closeable> deInchis = new ArrayList<>();
    private final Registru registru = new Registru(Clock.systemUTC());

    /** Două socket-uri legate între ele, pe un port ales de sistem: [capătul din test, capătul dat legăturii]. */
    private Socket[] pereche() throws IOException {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Socket client = new Socket(InetAddress.getLoopbackAddress(), server.getLocalPort());
            Socket acceptat = server.accept();
            deInchis.add(client);
            deInchis.add(acceptat);
            return new Socket[] {client, acceptat};
        }
    }

    @AfterEach
    void curata() {
        for (Closeable c : deInchis) {
            try {
                c.close();
            } catch (IOException e) {
                // nimic
            }
        }
    }

    private static String citeste(Socket s, int n) throws IOException {
        s.setSoTimeout(2_000);
        return new String(s.getInputStream().readNBytes(n), StandardCharsets.US_ASCII);
    }

    private static void scrie(Socket s, String text) throws IOException {
        s.getOutputStream().write(text.getBytes(StandardCharsets.US_ASCII));
        s.getOutputStream().flush();
    }

    /** Un socket închis de celălalt capăt dă -1 la citire, sau „connection reset”. Ambele înseamnă închis. */
    private static void assertInchis(Socket s) throws IOException {
        s.setSoTimeout(2_000);
        try {
            assertEquals(-1, s.getInputStream().read());
        } catch (SocketException e) {
            // reset: tot închis
        }
    }

    @Test
    void octetiiTrecInAmbeleSensuriIarConnectPleacaPrimul() throws Exception {
        Socket[] masina = pereche();
        Socket[] broker = pereche();
        Legatura legatura = new Legatura("masina-3", masina[1], broker[0], registru);
        legatura.porneste("CONNECT".getBytes(StandardCharsets.US_ASCII));

        scrie(masina[0], "PUBLISH");
        assertEquals("CONNECTPUBLISH", citeste(broker[1], 14));
        scrie(broker[1], "CONNACK");
        assertEquals("CONNACK", citeste(masina[0], 7));
    }

    @Test
    void candBrokerulInchideSeInchideSiMasina() throws Exception {
        Socket[] masina = pereche();
        Socket[] broker = pereche();
        Legatura legatura = new Legatura("masina-3", masina[1], broker[0], registru);
        legatura.porneste(new byte[0]);

        broker[1].close();
        assertInchis(masina[0]);
        assertTrue(legatura.inchisa());
    }

    @Test
    void candMasinaInchideSeInchideSiBrokerul() throws Exception {
        Socket[] masina = pereche();
        Socket[] broker = pereche();
        new Legatura("masina-3", masina[1], broker[0], registru).porneste(new byte[0]);

        masina[0].close();
        assertInchis(broker[1]);
    }

    @Test
    void tunelulDinRegistruInchideAmbeleCapete() throws Exception {
        Socket[] masina = pereche();
        Socket[] broker = pereche();
        new Legatura("masina-3", masina[1], broker[0], registru).porneste(new byte[0]);

        registru.inchideLegaturile("masina-3");
        assertInchis(masina[0]);
        assertInchis(broker[1]);
    }

    @Test
    void aDouaInchidereNuStricaNimic() throws Exception {
        Socket[] masina = pereche();
        Socket[] broker = pereche();
        Legatura legatura = new Legatura("masina-3", masina[1], broker[0], registru);
        legatura.porneste(new byte[0]);
        legatura.close();
        legatura.close();
        assertTrue(legatura.inchisa());
    }
}
