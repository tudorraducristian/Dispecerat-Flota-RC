package ro.flota.server.reguli;

import java.util.List;
import ro.flota.server.date.Punct;

/**
 * Algoritmul razei (spec §6.4): din punct pornește o rază spre est. Dacă taie marginea poligonului de un
 * număr impar de ori, punctul e înăuntru. Un punct chiar pe margine se socotește înăuntru. Longitudinea e
 * x, iar latitudinea e y; pe o zonă de câțiva kilometri, curbura Pământului nu contează.
 */
public final class PunctInPoligon {

    private static final double EPSILON = 1e-12;

    private PunctInPoligon() {
    }

    public static boolean contine(List<Punct> poligon, Punct p) {
        int n = poligon.size();
        boolean inauntru = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Punct a = poligon.get(i);
            Punct b = poligon.get(j);
            if (peSegment(a, b, p)) {
                return true;
            }
            boolean taieOrizontala = (a.lat() > p.lat()) != (b.lat() > p.lat());
            if (taieOrizontala) {
                double lonIntersectie = a.lon() + (p.lat() - a.lat()) * (b.lon() - a.lon()) / (b.lat() - a.lat());
                if (p.lon() < lonIntersectie) {
                    inauntru = !inauntru;
                }
            }
        }
        return inauntru;
    }

    /** p e pe segmentul ab: coliniar cu el și în dreptunghiul dintre capete. */
    private static boolean peSegment(Punct a, Punct b, Punct p) {
        double produs = (b.lon() - a.lon()) * (p.lat() - a.lat()) - (b.lat() - a.lat()) * (p.lon() - a.lon());
        if (Math.abs(produs) > EPSILON) {
            return false;
        }
        return p.lat() >= Math.min(a.lat(), b.lat()) - EPSILON && p.lat() <= Math.max(a.lat(), b.lat()) + EPSILON
                && p.lon() >= Math.min(a.lon(), b.lon()) - EPSILON && p.lon() <= Math.max(a.lon(), b.lon()) + EPSILON;
    }
}
