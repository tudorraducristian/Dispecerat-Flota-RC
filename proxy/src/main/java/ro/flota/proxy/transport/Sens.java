package ro.flota.proxy.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.random.RandomGenerator;
import ro.flota.proxy.vreme.Vreme;

/**
 * Un sens al unei conexiuni, de exemplu mașină → broker (spec §5.1). Un fir citește și pune bucățile în
 * coadă, cu ora de plecare; alt fir le scrie la ora lor. Un singur fir care ar citi, apoi ar dormi cât
 * întârzierea, ar aduna întârzierile una peste alta.
 */
final class Sens {

    private static final int MARIME_BUCATA = 4_096;
    private static final long PAS_ASTEPTARE_MS = 100;

    private final InputStream in;
    private final OutputStream out;
    private final Vreme vreme;
    private final Runnable laSfarsit;
    private final Planificator planificator;
    private final BlockingQueue<Bucata> coada = new LinkedBlockingQueue<>();
    private final Thread cititor;
    private final Thread scriitor;
    private volatile boolean oprit = false;

    Sens(String nume, InputStream in, OutputStream out, Vreme vreme, Runnable laSfarsit, RandomGenerator aleator) {
        this.in = in;
        this.out = out;
        this.vreme = vreme;
        this.laSfarsit = laSfarsit;
        this.planificator = new Planificator(aleator);
        this.cititor = new Thread(this::citeste, nume + "-citire");
        this.scriitor = new Thread(this::scrie, nume + "-scriere");
        cititor.setDaemon(true);
        scriitor.setDaemon(true);
    }

    /** Octeții dați pleacă primii, cu vremea aplicată. Așa trece CONNECT-ul deja citit de proxy. */
    void trimiteIntai(byte[] octeti) {
        coada.add(new Bucata(octeti, planificator.plecare(System.currentTimeMillis(),
                vreme.intarziereMs(), vreme.variatieMs())));
    }

    void porneste() {
        cititor.start();
        scriitor.start();
    }

    /** Oprește scrierea și aruncă ce era în coadă. Citirea se oprește când Legatura închide socket-ul. */
    void opreste() {
        oprit = true;
        coada.clear();
        scriitor.interrupt();
    }

    private void citeste() {
        byte[] buf = new byte[MARIME_BUCATA];
        boolean inchisCurat = false;
        try {
            int n;
            while (!oprit && (n = in.read(buf)) >= 0) {
                long plecare = planificator.plecare(System.currentTimeMillis(), vreme.intarziereMs(), vreme.variatieMs());
                coada.add(new Bucata(Arrays.copyOf(buf, n), plecare));
            }
            inchisCurat = true;
        } catch (IOException e) {
            // Conexiunea s-a rupt; mai jos se anunță sfârșitul.
        }
        if (oprit) {
            return;
        }
        if (inchisCurat && vreme.gauraPana() == null) {
            // Ca la TCP: ce era deja pe drum ajunge, apoi se închide (D5). Sfârșitul îl anunță firul care scrie.
            long plecare = planificator.plecare(System.currentTimeMillis(), vreme.intarziereMs(), vreme.variatieMs());
            coada.add(Bucata.sfarsit(plecare));
        } else {
            // Conexiune ruptă, sau închisă în gaură (spec §5.3): ce stătea în coadă se aruncă.
            laSfarsit.run();
        }
    }

    private void scrie() {
        try {
            while (!oprit) {
                Bucata bucata = coada.take();
                asteaptaPana(bucata.plecareMs());
                asteaptaSaTreacaGaura();
                if (bucata.esteSfarsit()) {
                    break; // tot ce trimisese celălalt capăt a plecat
                }
                out.write(bucata.octeti());
                out.flush();
                asteaptaBanda(bucata.octeti().length);
            }
        } catch (InterruptedException e) {
            return; // oprit de Legatura
        } catch (IOException e) {
            // Celălalt capăt e închis.
        }
        if (!oprit) {
            laSfarsit.run();
        }
    }

    private static void asteaptaPana(long plecareMs) throws InterruptedException {
        long ramas;
        while ((ramas = plecareMs - System.currentTimeMillis()) > 0) {
            Thread.sleep(Math.min(ramas, PAS_ASTEPTARE_MS));
        }
    }

    /**
     * Banda îngustă: după o bucată, așteaptă cât i-ar lua să treacă prin bandă (kbps = biți pe milisecundă).
     * Pe pași de 100 ms, cu banda citită din nou la fiecare pas, ca NORMAL sau o bandă nouă să se vadă repede.
     */
    private void asteaptaBanda(int octeti) throws InterruptedException {
        long bitiRamasi = octeti * 8L;
        int kbps;
        while (bitiRamasi > 0 && (kbps = vreme.bandaKbps()) > 0) {
            long pas = Math.min(PAS_ASTEPTARE_MS, (bitiRamasi + kbps - 1) / kbps);
            Thread.sleep(pas);
            bitiRamasi -= pas * kbps;
        }
    }

    /** Cât ține gaura, octeții stau în coadă. Verific la 100 ms, ca NORMAL să deblocheze repede. */
    private void asteaptaSaTreacaGaura() throws InterruptedException {
        while (vreme.gauraPana() != null) {
            Thread.sleep(PAS_ASTEPTARE_MS);
        }
    }
}
