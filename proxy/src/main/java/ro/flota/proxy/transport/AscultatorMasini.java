package ro.flota.proxy.transport;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import ro.flota.proxy.connect.CititorConnect;
import ro.flota.proxy.connect.PachetConnect;
import ro.flota.proxy.vreme.Registru;
import ro.flota.proxy.vreme.Vreme;

/**
 * Primește mașinile pe 1884 (spec §5.1). Fiecare conexiune are firul ei: citește CONNECT, află cine e
 * mașina și o leagă de Mosquitto, cu excepția cazului în care mașina e în tunel.
 */
public final class AscultatorMasini implements Runnable {

    private static final int TIMP_CONNECT_MS = 10_000;
    private static final int TIMP_CONECTARE_BROKER_MS = 5_000;
    private static final int PAS_TUNEL_MS = 200;

    private final ServerSocket server;
    private final InetSocketAddress broker;
    private final Registru registru;

    public AscultatorMasini(ServerSocket server, InetSocketAddress broker, Registru registru) {
        this.server = server;
        this.broker = broker;
        this.registru = registru;
    }

    @Override
    public void run() {
        while (!server.isClosed()) {
            try {
                Socket masina = server.accept();
                Thread fir = new Thread(() -> trateaza(masina), "conexiune-" + masina.getPort());
                fir.setDaemon(true);
                fir.start();
            } catch (IOException e) {
                if (!server.isClosed()) {
                    System.out.println("Nu pot accepta o conexiune: " + e.getMessage());
                }
            }
        }
    }

    private void trateaza(Socket masina) {
        PachetConnect connect;
        try {
            masina.setSoTimeout(TIMP_CONNECT_MS);
            connect = CititorConnect.citeste(masina.getInputStream());
            masina.setSoTimeout(0);
        } catch (IOException e) {
            System.out.printf("Închid o conexiune de la %s: %s%n", masina.getRemoteSocketAddress(), e.getMessage());
            inchide(masina);
            return;
        }
        String id = connect.clientId();
        Vreme vreme = registru.vreme(id);
        if (vreme.inTunel()) {
            tineInTunel(id, masina, vreme);
            return;
        }
        Socket spreBroker = new Socket();
        try {
            spreBroker.connect(broker, TIMP_CONECTARE_BROKER_MS);
            Legatura legatura = new Legatura(id, masina, spreBroker, registru);
            legatura.porneste(connect.octeti());
            // Un TUNEL venit între verificarea de mai sus și înregistrarea legăturii n-ar fi găsit-o.
            if (vreme.inTunel()) {
                legatura.close();
                return;
            }
            System.out.printf("[%s] legată prin proxy%n", id);
        } catch (IOException e) {
            System.out.printf("[%s] broker-ul nu răspunde (%s); închid mașina%n", id, e.getMessage());
            inchide(spreBroker);
            inchide(masina);
        }
    }

    /**
     * În tunel (spec §5.3): conexiunea TCP e acceptată și CONNECT-ul citit, dar nu primește niciodată
     * răspuns. Mașina renunță singură după 5 secunde. Spre Mosquitto nu se deschide nimic.
     */
    private static void tineInTunel(String id, Socket masina, Vreme vreme) {
        System.out.printf("[%s] în tunel: țin conexiunea fără răspuns%n", id);
        try {
            masina.setSoTimeout(PAS_TUNEL_MS);
            InputStream in = masina.getInputStream();
            while (vreme.inTunel()) {
                try {
                    if (in.read() < 0) {
                        break; // mașina a renunțat
                    }
                } catch (SocketTimeoutException e) {
                    // Mașina încă așteaptă; verific din nou tunelul.
                }
            }
        } catch (IOException e) {
            // Mașina a închis.
        }
        inchide(masina);
    }

    private static void inchide(Socket s) {
        try {
            s.close();
        } catch (IOException e) {
            // Era deja închis.
        }
    }
}
