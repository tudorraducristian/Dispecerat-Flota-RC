package ro.flota.simulator.masina;

import java.time.Clock;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;
import ro.flota.simulator.config.DefinitieMasina;
import ro.flota.simulator.geo.Punct;
import ro.flota.simulator.geo.Traseu;
import ro.flota.simulator.mesaje.Alarma;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;
import ro.flota.simulator.mesaje.Ore;
import ro.flota.simulator.mesaje.Telemetrie;
import ro.flota.simulator.mesaje.TipAlarma;
import ro.flota.simulator.simulare.DetectorFranare;
import ro.flota.simulator.simulare.DetectorSupraincalzire;
import ro.flota.simulator.simulare.Temperatura;
import ro.flota.simulator.simulare.Viteza;

/**
 * Producătorul (D9): la fiecare bătaie mută mașina, calculează valorile și pune mesajele în coadă.
 * Nu știe nimic de rețea, ci doar dacă radioul e conectat, ca să marcheze din_buffer.
 * Coada e un Buffer: cât radioul e offline, se umple, iar plină aruncă telemetria cea mai veche.
 */
public final class Motor {

    private final String id;
    private final Traseu traseu;
    private final Viteza viteza;
    private final Temperatura temperatura;
    private final DetectorSupraincalzire supraincalzire = new DetectorSupraincalzire();
    private final DetectorFranare franare = new DetectorFranare();
    private final Clock ceas;
    private final Buffer coada;
    private final BooleanSupplier conectat;
    private final double secundePeTick;
    private double distanta;
    private double secundeScurse = 0;
    private long seq = 0;
    /** Comenzile vin pe firul clientului MQTT și așteaptă aici următoarea bătaie a motorului. */
    private final Queue<byte[]> comenziPrimite = new ConcurrentLinkedQueue<>();
    private final ExecutantComenzi executant;
    private boolean spreBaza = false;
    private boolean laBaza = false;
    private double panaLaBaza;
    /** +1 înainte pe tură, -1 înapoi; spre bază, mașina merge pe drumul mai scurt (D54). */
    private int sensSpreBaza;

    public Motor(DefinitieMasina def, Viteza viteza, Temperatura temperatura, Clock ceas,
                 Buffer coada, BooleanSupplier conectat, double secundePeTick) {
        this.id = def.id();
        this.traseu = def.traseu();
        this.viteza = viteza;
        this.temperatura = temperatura;
        this.ceas = ceas;
        this.coada = coada;
        this.conectat = conectat;
        this.secundePeTick = secundePeTick;
        this.distanta = def.start() * traseu.lungime();
        this.executant = new ExecutantComenzi(id, viteza, this::intoarceLaBaza,
                text -> System.out.printf("[%s] mesaj pentru șofer: %s%n", id, text));
    }

    public void tick() {
        String ora = Ore.format(ceas.instant());
        executaComenzile(ora);
        double v = laBaza ? 0 : viteza.tick();
        double t = temperatura.tick(v);
        double pas = v / 3.6 * secundePeTick;
        if (spreBaza) {
            panaLaBaza -= pas;
            if (panaLaBaza <= 0) {
                distanta = 0;
                spreBaza = false;
                laBaza = true;
                v = 0;
                System.out.printf("[%s] a ajuns la bază%n", id);
            } else {
                distanta = traseu.normalizeaza(distanta + sensSpreBaza * pas);
            }
        } else if (!laBaza) {
            distanta = traseu.normalizeaza(distanta + pas);
        }
        secundeScurse += secundePeTick;
        Punct p = traseu.punctLa(distanta);
        seq++;
        coada.adauga(Mesaje.telemetrie(id, new Telemetrie(seq, ora,
                rotunjit(p.lat(), 5), rotunjit(p.lon(), 5), rotunjit(v, 1), rotunjit(t, 1),
                baterie(), !conectat.getAsBoolean())));
        if (supraincalzire.verifica(t)) {
            coada.adauga(Mesaje.alarma(id, new Alarma(TipAlarma.SUPRAINCALZIRE, ora, rotunjit(t, 1))));
        }
        // Oprirea la bază nu e o frânare bruscă (D26).
        if (!laBaza && franare.verifica(v)) {
            coada.adauga(Mesaje.alarma(id, new Alarma(TipAlarma.FRANARE_BRUSCA, ora,
                    rotunjit(franare.ultimaScadere(), 1))));
        }
    }

    /** Vine din orice fir, de obicei de la clientul MQTT. Se execută la următoarea bătaie. */
    public void primesteComanda(byte[] payload) {
        comenziPrimite.add(payload);
    }

    private void executaComenzile(String ora) {
        byte[] comanda;
        while ((comanda = comenziPrimite.poll()) != null) {
            executant.executa(comanda, ora).ifPresent(coada::adauga);
        }
    }

    /**
     * INTOARCE_LA_BAZA (spec §4.2): baza e primul punct al turei (D26). Tura pleacă din bază și se întoarce în ea,
     * deci mașina alege drumul mai scurt, înapoi pe unde a venit sau înainte, pe restul turei (D54).
     */
    private void intoarceLaBaza() {
        if (laBaza) {
            return;
        }
        spreBaza = true;
        double inapoi = distanta;
        double inainte = traseu.lungime() - distanta;
        sensSpreBaza = inainte < inapoi ? 1 : -1;
        panaLaBaza = Math.min(inainte, inapoi);
    }

    /** Scade cu 1% pe minut, de la 100 la 5, apoi rămâne la 5. */
    private int baterie() {
        return (int) Math.max(5, 100 - Math.floor(secundeScurse / 60.0));
    }

    private static double rotunjit(double x, int zecimale) {
        double f = Math.pow(10, zecimale);
        return Math.round(x * f) / f;
    }
}
