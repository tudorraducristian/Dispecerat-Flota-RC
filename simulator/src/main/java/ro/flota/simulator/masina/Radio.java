package ro.flota.simulator.masina;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;

/**
 * Consumatorul (D9): ține conexiunea și golește coada, în ordine. Un mesaj care n-a putut pleca revine
 * primul la rând. Faza 1 reîncearcă la 5 secunde fix; faza 4 aduce pauza care se dublează.
 */
public final class Radio implements Runnable {

    static final long PAUZA_RECONECTARE_MS = 5_000;
    static final long PAUZA_DUPA_EROARE_MS = 1_000;
    static final long ASTEPTARE_COADA_MS = 1_000;

    private final String id;
    private final Conexiune conexiune;
    private final BlockingDeque<Mesaj> coada;
    private final Somn somn;
    private volatile boolean oprit = false;
    private boolean anuntatIndisponibil = false;

    public Radio(String id, Conexiune conexiune, BlockingDeque<Mesaj> coada, Somn somn) {
        this.id = id;
        this.conexiune = conexiune;
        this.coada = coada;
        this.somn = somn;
    }

    @Override
    public void run() {
        while (!oprit) {
            try {
                pas();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void opreste() {
        oprit = true;
    }

    /** Un pas: dacă nu e conectat, încearcă o dată; altfel trimite următorul mesaj din coadă. */
    void pas() throws InterruptedException {
        if (!conexiune.conectata()) {
            incearcaSaTeConectezi();
            return;
        }
        Mesaj mesaj = coada.pollFirst(ASTEPTARE_COADA_MS, TimeUnit.MILLISECONDS);
        if (mesaj == null) {
            return;
        }
        try {
            conexiune.publica(mesaj);
        } catch (EroareConexiune e) {
            coada.offerFirst(mesaj);
            System.out.printf("[%s] n-am putut trimite (%s); păstrez mesajul%n", id, e.getMessage());
            somn.dormi(PAUZA_DUPA_EROARE_MS);
        }
    }

    private void incearcaSaTeConectezi() throws InterruptedException {
        try {
            conexiune.conecteaza();
            conexiune.publica(Mesaje.stare(id, true));
            System.out.printf("[%s] conectată%n", id);
            anuntatIndisponibil = false;
        } catch (EroareConexiune e) {
            if (!anuntatIndisponibil) {
                System.out.printf("[%s] broker indisponibil (%s); reîncerc la 5 secunde%n", id, e.getMessage());
                anuntatIndisponibil = true;
            }
            somn.dormi(PAUZA_RECONECTARE_MS);
        }
    }
}
