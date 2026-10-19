package ro.flota.proxy;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Clock;
import ro.flota.proxy.transport.AscultatorMasini;
import ro.flota.proxy.vreme.Registru;

/** Proxy-ul de rețea proastă: mașinile pe 1884, legate mai departe de Mosquitto pe 1883 (spec §3). */
public final class Main {

    static final int PORT_MASINI = 1884;
    static final InetSocketAddress BROKER = new InetSocketAddress("127.0.0.1", 1883);

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        InetAddress local = InetAddress.getByName("127.0.0.1");
        Registru registru = new Registru(Clock.systemUTC());
        ServerSocket masini = deschide(PORT_MASINI, local);
        System.out.printf("Proxy: mașinile pe %d → Mosquitto pe %d%n", PORT_MASINI, BROKER.getPort());
        new AscultatorMasini(masini, BROKER, registru).run();
    }

    static ServerSocket deschide(int port, InetAddress adresa) throws IOException {
        try {
            return new ServerSocket(port, 50, adresa);
        } catch (BindException e) {
            System.err.printf("Portul %d e ocupat. Mai rulează un proxy?%n", port);
            System.exit(1);
            throw e;
        }
    }
}
