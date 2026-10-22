package ro.flota.simulator.masina;

import ro.flota.simulator.simulare.Aleator;

/**
 * Pauza dintre încercările de reconectare (spec §4.3): min(30, 2ⁿ⁻¹) secunde după încercarea n, adică
 * 1, 2, 4, 8, 16, 30, 30... Fiecare se înmulțește cu un factor între 0,8 și 1,2, ca mașinile ieșite
 * deodată din tunel să nu bată toate la ușă în aceeași secundă.
 */
public final class PauzaReconectare {

    static final long MAXIM_MS = 30_000;
    static final double ABATERE = 0.2;

    private final Aleator aleator;
    private int incercari = 0;

    public PauzaReconectare(Aleator aleator) {
        this.aleator = aleator;
    }

    /** Încă o încercare a eșuat. Întoarce cât să aștepte radioul până la următoarea, în milisecunde. */
    public long dupaEsec() {
        incercari++;
        long baza = incercari >= 6 ? MAXIM_MS : Math.min(MAXIM_MS, 1_000L << (incercari - 1));
        double factor = 1 - ABATERE + 2 * ABATERE * aleator.urmator();
        return Math.round(baza * factor);
    }

    /** După o conectare reușită, numărătoarea o ia de la capăt. */
    public void reseteaza() {
        incercari = 0;
    }

    public int incercari() {
        return incercari;
    }
}
