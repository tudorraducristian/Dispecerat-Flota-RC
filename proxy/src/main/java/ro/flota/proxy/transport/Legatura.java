package ro.flota.proxy.transport;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import ro.flota.proxy.vreme.Registru;
import ro.flota.proxy.vreme.Vreme;

/**
 * O mașină legată de broker prin proxy: două socket-uri și două sensuri (spec §5.1).
 * Când oricare capăt se închide, se închide tot, iar octeții din cozi se aruncă.
 */
public final class Legatura implements Closeable {

    private final String masina;
    private final Socket spreMasina;
    private final Socket spreBroker;
    private final Registru registru;
    private final Sens urcare;
    private final Sens coborare;
    private final AtomicBoolean inchisa = new AtomicBoolean(false);

    public Legatura(String masina, Socket spreMasina, Socket spreBroker, Registru registru) throws IOException {
        this.masina = masina;
        this.spreMasina = spreMasina;
        this.spreBroker = spreBroker;
        this.registru = registru;
        Vreme vreme = registru.vreme(masina);
        this.urcare = new Sens(masina + "-urcare", spreMasina.getInputStream(), spreBroker.getOutputStream(),
                vreme, this::close, new Random());
        this.coborare = new Sens(masina + "-coborare", spreBroker.getInputStream(), spreMasina.getOutputStream(),
                vreme, this::close, new Random());
    }

    public void porneste(byte[] connect) {
        registru.adaugaLegatura(masina, this);
        urcare.trimiteIntai(connect);
        urcare.porneste();
        coborare.porneste();
    }

    @Override
    public void close() {
        if (!inchisa.compareAndSet(false, true)) {
            return;
        }
        urcare.opreste();
        coborare.opreste();
        inchide(spreMasina);
        inchide(spreBroker);
        registru.scoateLegatura(masina, this);
        System.out.printf("[%s] legătura închisă%n", masina);
    }

    public boolean inchisa() {
        return inchisa.get();
    }

    private static void inchide(Socket s) {
        try {
            s.close();
        } catch (IOException e) {
            // Era deja închis.
        }
    }
}
