package ro.flota.server.retea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ro.flota.server.api.ServiciuIndisponibil;

class ClientProxyTest {

    private ServerSocket server;
    private final List<String> primite = new CopyOnWriteArrayList<>();

    /** Un proxy fals: primește o conexiune, citește o linie și răspunde cu textul dat. */
    private ClientProxy proxyCareRaspunde(String raspuns) throws IOException {
        server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        Thread fir = new Thread(() -> {
            try (Socket s = server.accept()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII));
                primite.add(in.readLine());
                s.getOutputStream().write(raspuns.getBytes(StandardCharsets.US_ASCII));
                s.getOutputStream().flush();
                Thread.sleep(200);
            } catch (IOException | InterruptedException e) {
                // testul s-a terminat
            }
        });
        fir.setDaemon(true);
        fir.start();
        return new ClientProxy("127.0.0.1", server.getLocalPort());
    }

    @AfterEach
    void inchide() throws IOException {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void raspunsulUnuiRand() throws IOException {
        ClientProxy client = proxyCareRaspunde("OK\n");
        assertEquals(List.of("OK"), client.trimite("TUNEL masina-3 15"));
        assertEquals(List.of("TUNEL masina-3 15"), primite);
    }

    @Test
    void stareaCitestePanaLaPunctFaraEl() throws IOException {
        ClientProxy client = proxyCareRaspunde("masina-1 NORMAL\nmasina-3 TUNEL 12\n.\n");
        assertEquals(List.of("masina-1 NORMAL", "masina-3 TUNEL 12"), client.trimite("STARE"));
    }

    @Test
    void proxyOpritDa503() throws IOException {
        int port;
        try (ServerSocket liber = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = liber.getLocalPort();
        }
        ClientProxy client = new ClientProxy("127.0.0.1", port);
        assertThrows(ServiciuIndisponibil.class, () -> client.trimite("STARE"));
    }

    @Test
    void proxyCareTaceDa503InDouaSecunde() throws IOException {
        // Sistemul acceptă conexiunea din coada de așteptare, dar nimeni nu citește și nu răspunde.
        server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        ClientProxy client = new ClientProxy("127.0.0.1", server.getLocalPort());
        long inceput = System.nanoTime();
        assertThrows(ServiciuIndisponibil.class, () -> client.trimite("STARE"));
        long ms = (System.nanoTime() - inceput) / 1_000_000;
        assertTrue(ms >= 1_900 && ms < 3_500, "a durat " + ms + " ms");
    }
}
