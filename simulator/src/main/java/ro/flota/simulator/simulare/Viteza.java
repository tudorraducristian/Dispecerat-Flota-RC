package ro.flota.simulator.simulare;

/**
 * Viteza unei mașini (spec §4.4). Ținta se schimbă la 30 de secunde; viteza se apropie de ea cu cel mult
 * 5 km/h pe secundă. Rar, mașina frânează brusc. Aleatorul de frânare se consultă doar peste 30 km/h.
 */
public final class Viteza {

    static final double PAS_MAXIM = 5.0;
    static final int SECUNDE_PE_TINTA = 30;
    static final double SANSA_TINTA_90 = 0.10;
    static final double SANSA_FRANARE = 1.0 / 600; // cam o dată la 10 minute
    static final double SCADERE_FRANARE = 30.0;
    static final double VITEZA_MINIMA_FRANARE = 30.0;

    private final Aleator pentruTinta;
    private final Aleator pentruFranare;
    private double viteza;
    private double tinta;
    private double limita = Double.POSITIVE_INFINITY;
    private int secundePanaLaTintaNoua = 0;

    public Viteza(Aleator pentruTinta, Aleator pentruFranare, double vitezaInitiala) {
        this.pentruTinta = pentruTinta;
        this.pentruFranare = pentruFranare;
        this.viteza = vitezaInitiala;
    }

    /** Trece o secundă. Întoarce viteza nouă, în km/h. */
    public double tick() {
        if (secundePanaLaTintaNoua == 0) {
            tinta = pentruTinta.urmator() < SANSA_TINTA_90 ? 90.0 : 30.0 + 40.0 * pentruTinta.urmator();
            secundePanaLaTintaNoua = SECUNDE_PE_TINTA;
        }
        tinta = Math.min(tinta, limita);
        secundePanaLaTintaNoua--;
        if (viteza >= VITEZA_MINIMA_FRANARE && pentruFranare.urmator() < SANSA_FRANARE) {
            viteza -= SCADERE_FRANARE;
        } else {
            viteza += Math.max(-PAS_MAXIM, Math.min(PAS_MAXIM, tinta - viteza));
        }
        return viteza;
    }

    /** LIMITEAZA_VITEZA (spec §4.2): ținta de acum și toate cele viitoare nu mai trec de limită. */
    public void limiteaza(double kmh) {
        limita = kmh;
        tinta = Math.min(tinta, limita);
    }

    public double viteza() {
        return viteza;
    }

    public double tinta() {
        return tinta;
    }
}
