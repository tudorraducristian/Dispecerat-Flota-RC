package ro.flota.simulator.masina;

import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;

/**
 * Consumatorul (D9): ține conexiunea și golește bufferul, în ordine. Un mesaj care n-a putut pleca revine
 * primul la rând. Între încercările de reconectare, pauzele se dublează (spec §4.3).
 */
public final class Radio implements Runnable {

    static final long PAUZA_DUPA_EROARE_MS = 1_000;
    static final long ASTEPTARE_COADA_MS = 1_000;

    private final String id;
    private final Conexiune conexiune;
    private final Buffer coada;
    private final PauzaReconectare pauza;
    private final Somn somn;
    private volatile boolean oprit = false;

    public Radio(String id, Conexiune conexiune, Buffer coada, PauzaReconectare pauza, Somn somn) {
        this.id = id;
        this.conexiune = conexiune;
        this.coada = coada;
        this.pauza = pauza;
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

    /** Un pas: dacă nu e conectat, încearcă o dată; altfel trimite următorul mesaj din buffer. */
    void pas() throws InterruptedException {
        if (!conexiune.conectata()) {
            incearcaSaTeConectezi();
            return;
        }
        Mesaj mesaj = coada.ia(ASTEPTARE_COADA_MS);
        if (mesaj == null) {
            return;
        }
        try {
            conexiune.publica(mesaj);
        } catch (EroareConexiune e) {
            coada.puneInapoi(mesaj);
            System.out.printf("[%s] n-am putut trimite (%s); păstrez mesajul%n", id, e.getMessage());
            somn.dormi(PAUZA_DUPA_EROARE_MS);
        }
    }

    /** La reconectare: întâi „online”, apoi bufferul, în ordine; abia după aceea vin mesajele live. */
    private void incearcaSaTeConectezi() throws InterruptedException {
        try {
            conexiune.conecteaza();
            conexiune.publica(Mesaje.stare(id, true));
            System.out.printf("[%s] conectată; în buffer: %d mesaje, aruncate până acum: %d%n",
                    id, coada.marime(), coada.aruncate());
            pauza.reseteaza();
        } catch (EroareConexiune e) {
            if (conexiune.conectata()) {
                conexiune.deconecteaza(); // conectată, dar fără „online”: încearcă de la capăt (D25)
            }
            long ms = pauza.dupaEsec();
            if (pauza.incercari() == 1) {
                System.out.printf("[%s] nu mă pot conecta (%s); reîncerc cu pauze tot mai lungi%n", id, e.getMessage());
            }
            somn.dormi(ms);
        }
    }
}
