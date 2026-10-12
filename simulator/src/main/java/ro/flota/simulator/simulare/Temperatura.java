package ro.flota.simulator.simulare;

/**
 * Temperatura motorului (spec §4.4): urcă odată cu viteza, cu un mic zgomot. Rar, o defecțiune o urcă
 * cu 2 °C pe secundă timp de 20 de secunde. Aleatorul de defect nu se consultă cât ține defectul.
 */
public final class Temperatura {

    public static final double DE_BAZA = 85.0;
    static final double PE_KMH = 0.1;
    static final double PAS_MAXIM = 0.5;
    static final double ZGOMOT_MAXIM = 0.3;
    static final double SANSA_DEFECT = 1.0 / 1200;
    static final int SECUNDE_DEFECT = 20;
    static final double URCARE_DEFECT = 2.0;

    private final Aleator pentruDefect;
    private final Aleator pentruZgomot;
    private double temperatura;
    private int secundeDefectRamase = 0;

    public Temperatura(Aleator pentruDefect, Aleator pentruZgomot, double initiala) {
        this.pentruDefect = pentruDefect;
        this.pentruZgomot = pentruZgomot;
        this.temperatura = initiala;
    }

    /** Trece o secundă la viteza dată. Întoarce temperatura nouă, în °C. */
    public double tick(double viteza) {
        if (secundeDefectRamase == 0 && pentruDefect.urmator() < SANSA_DEFECT) {
            secundeDefectRamase = SECUNDE_DEFECT;
        }
        if (secundeDefectRamase > 0) {
            secundeDefectRamase--;
            temperatura += URCARE_DEFECT;
        } else {
            double tinta = DE_BAZA + PE_KMH * viteza;
            temperatura += Math.max(-PAS_MAXIM, Math.min(PAS_MAXIM, tinta - temperatura));
            temperatura += (pentruZgomot.urmator() * 2 - 1) * ZGOMOT_MAXIM;
        }
        return temperatura;
    }
}
