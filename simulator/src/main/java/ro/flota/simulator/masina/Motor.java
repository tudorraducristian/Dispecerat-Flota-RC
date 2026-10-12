package ro.flota.simulator.masina;

import java.time.Clock;
import java.util.concurrent.BlockingDeque;
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
 */
public final class Motor {

    private final String id;
    private final Traseu traseu;
    private final Viteza viteza;
    private final Temperatura temperatura;
    private final DetectorSupraincalzire supraincalzire = new DetectorSupraincalzire();
    private final DetectorFranare franare = new DetectorFranare();
    private final Clock ceas;
    private final BlockingDeque<Mesaj> coada;
    private final BooleanSupplier conectat;
    private final double secundePeTick;
    private double distanta;
    private double secundeScurse = 0;
    private long seq = 0;

    public Motor(DefinitieMasina def, Viteza viteza, Temperatura temperatura, Clock ceas,
                 BlockingDeque<Mesaj> coada, BooleanSupplier conectat, double secundePeTick) {
        this.id = def.id();
        this.traseu = def.traseu();
        this.viteza = viteza;
        this.temperatura = temperatura;
        this.ceas = ceas;
        this.coada = coada;
        this.conectat = conectat;
        this.secundePeTick = secundePeTick;
        this.distanta = def.start() * traseu.lungime();
    }

    public void tick() {
        double v = viteza.tick();
        double t = temperatura.tick(v);
        distanta = traseu.normalizeaza(distanta + v / 3.6 * secundePeTick);
        secundeScurse += secundePeTick;
        Punct p = traseu.punctLa(distanta);
        String ora = Ore.format(ceas.instant());
        seq++;
        coada.offerLast(Mesaje.telemetrie(id, new Telemetrie(seq, ora,
                rotunjit(p.lat(), 5), rotunjit(p.lon(), 5), rotunjit(v, 1), rotunjit(t, 1),
                baterie(), !conectat.getAsBoolean())));
        if (supraincalzire.verifica(t)) {
            coada.offerLast(Mesaje.alarma(id, new Alarma(TipAlarma.SUPRAINCALZIRE, ora, rotunjit(t, 1))));
        }
        if (franare.verifica(v)) {
            coada.offerLast(Mesaje.alarma(id, new Alarma(TipAlarma.FRANARE_BRUSCA, ora,
                    rotunjit(franare.ultimaScadere(), 1))));
        }
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
