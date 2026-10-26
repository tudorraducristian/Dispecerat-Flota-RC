package ro.flota.proxy.transport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ro.flota.proxy.connect.Pachete;
import ro.flota.proxy.vreme.Registru;

/** Ascultătorul de pe 1884, cu un broker fals: un ServerSocket care doar acceptă și citește. */
class AscultatorMasiniTest {

    private static final InetAddress LOCAL = InetAddress.getLoopbackAddress();

    private final List<Closeable> deInchis = new ArrayList<>();
    private final Registru registru = new Registru(Clock.systemUTC());

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

    private <T extends Closeable> T tine(T c) {
        deInchis.add(c);
        return c;
    }

    /** Pornește ascultătorul pe un port ales de sistem și întoarce o mașină conectată la el. */
    private Socket masinaSpre(InetSocketAddress broker) throws IOException {
        ServerSocket server = tine(new ServerSocket(0, 50, LOCAL));
        Thread fir = new Thread(new AscultatorMasini(server, broker, registru), "ascultator-test");
        fir.setDaemon(true);
        fir.start();
        Socket masina = tine(new Socket(LOCAL, server.getLocalPort()));
        masina.setSoTimeout(2_000);
        return masina;
    }

    private ServerSocket brokerFals() throws IOException {
        ServerSocket broker = tine(new ServerSocket(0, 50, LOCAL));
        broker.setSoTimeout(2_000);
        return broker;
    }

    private static InetSocketAddress adresa(ServerSocket s) {
        return new InetSocketAddress(LOCAL, s.getLocalPort());
    }

    private static void trimite(Socket s, byte[] octeti) throws IOException {
        s.getOutputStream().write(octeti);
        s.getOutputStream().flush();
    }

    /** Închis înseamnă -1 la citire sau „connection reset”. */
    private static boolean inchis(Socket s) throws IOException {
        try {
            return s.getInputStream().read() < 0;
        } catch (SocketException e) {
            return true;
        }
    }

    @Test
    void masinaAjungeLaBrokerCuConnectulEi() throws Exception {
        ServerSocket broker = brokerFals();
        Socket masina = masinaSpre(adresa(broker));
        byte[] connect = Pachete.connect("masina-3");
        trimite(masina, connect);
        Socket laBroker = tine(broker.accept());
        laBroker.setSoTimeout(2_000);
        assertArrayEquals(connect, laBroker.getInputStream().readNBytes(connect.length));
    }

    /** Registrul fazei 3: un mosquitto_sub pe 1884 rămânea ca „auto-XXXX” în STARE și în TOATE. */
    @Test
    void altClientDecatOMasinaEsteRefuzatCuConnack2() throws Exception {
        ServerSocket broker = brokerFals();
        broker.setSoTimeout(500);
        Socket client = masinaSpre(adresa(broker));
        trimite(client, Pachete.connect("auto-1A2B"));
        assertArrayEquals(new byte[] {0x20, 0x02, 0x00, 0x02}, client.getInputStream().readNBytes(4),
                "CONNACK cu codul 2: identifier rejected");
        assertEquals(true, inchis(client));
        assertThrows(SocketTimeoutException.class, broker::accept, "spre broker nu trebuia deschis nimic");
        assertFalse(registru.cunoscuta("auto-1A2B"), "clientul refuzat n-are ce căuta în STARE");
    }

    @Test
    void inTunelConnectulNuPrimesteRaspunsSiSpreBrokerNuSeDeschideNimic() throws Exception {
        registru.vreme("masina-3").tunel(5);
        ServerSocket broker = brokerFals();
        broker.setSoTimeout(500);
        Socket masina = masinaSpre(adresa(broker));
        masina.setSoTimeout(500);
        trimite(masina, Pachete.connect("masina-3"));
        assertThrows(SocketTimeoutException.class, () -> masina.getInputStream().read(), "în tunel nu vine niciun răspuns");
        assertThrows(SocketTimeoutException.class, broker::accept);
    }

    @Test
    void brokerulLipsaInchideMasina() throws Exception {
        InetSocketAddress nimeni;
        try (ServerSocket liber = new ServerSocket(0, 1, LOCAL)) {
            nimeni = adresa(liber);
        }
        Socket masina = masinaSpre(nimeni);
        masina.setSoTimeout(7_000);
        trimite(masina, Pachete.connect("masina-3"));
        assertEquals(true, inchis(masina));
    }
}
