package ro.flota.simulator.simulare;

/** Dă alarma la schimbare (D12): o dată, când temperatura trece de 110. Se rearmează sub 100. */
public final class DetectorSupraincalzire {

    static final double PRAG_ALARMA = 110.0;
    static final double PRAG_REARMARE = 100.0;

    private boolean armat = true;

    public boolean verifica(double temperatura) {
        if (armat && temperatura > PRAG_ALARMA) {
            armat = false;
            return true;
        }
        if (!armat && temperatura < PRAG_REARMARE) {
            armat = true;
        }
        return false;
    }
}
