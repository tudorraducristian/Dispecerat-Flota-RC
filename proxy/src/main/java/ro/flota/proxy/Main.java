package ro.flota.proxy;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Clock;
import ro.flota.proxy.ordine.Ordine;
import ro.flota.proxy.ordine.ServerOrdine;
import ro.flota.proxy.transport.AscultatorMasini;
import ro.flota.proxy.vreme.Registru;

/** Proxy-ul de rețea proastă: mașinile pe 1884, legate de Mosquitto pe 1883; ordinele pe 1885 (spec §3). */
public final class Main {

    static final int PORT_MASINI = 1884;
    static final int PORT_ORDINE = 1885;
    static final InetSocketAddress BROKER = new InetSocketAddress("127.0.0.1", 1883);

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        InetAddress local = InetAddress.getByName("127.0.0.1");
        Registru registru = new Registru(Clock.systemUTC());
        ServerSocket ordine = deschide(PORT_ORDINE, local);
        ServerSocket masini = deschide(PORT_MASINI, local);
        new Thread(new ServerOrdine(ordine, new Ordine(registru)), "ordine").start();
        System.out.printf("Proxy: mașinile pe %d → Mosquitto pe %d; ordinele pe %d%n",
                PORT_MASINI, BROKER.getPort(), PORT_ORDINE);
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
