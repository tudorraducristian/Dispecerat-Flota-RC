package ro.flota.simulator.simulare;

/** Frânare bruscă: viteza a scăzut cu peste 25 km/h într-o singură secundă (spec §4.4). */
public final class DetectorFranare {

    static final double PRAG_SCADERE = 25.0;

    private Double vitezaAnterioara = null;
    private double ultimaScadere = 0;

    public boolean verifica(double viteza) {
        boolean franare = vitezaAnterioara != null && vitezaAnterioara - viteza > PRAG_SCADERE;
        if (franare) {
            ultimaScadere = vitezaAnterioara - viteza;
        }
        vitezaAnterioara = viteza;
        return franare;
    }

    /** Cu cât a scăzut viteza la ultima frânare bruscă, în km/h. */
    public double ultimaScadere() {
        return ultimaScadere;
    }
}
