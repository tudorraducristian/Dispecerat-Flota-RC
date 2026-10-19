package ro.flota.proxy.vreme;

import java.io.Closeable;
import java.io.IOException;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mașinile văzute de proxy măcar o dată, cu vremea fiecăreia și cu conexiunile ei deschise.
 * Hărțile sînt concurente: le folosesc firele de conexiune și firele de ordine în același timp.
 */
public final class Registru {

    private final Clock ceas;
    private final Map<String, Vreme> vremi = new ConcurrentHashMap<>();
    private final Map<String, Set<Closeable>> legaturi = new ConcurrentHashMap<>();

    public Registru(Clock ceas) {
        this.ceas = ceas;
    }

    /** Vremea mașinii. Prima cerere o face „văzută”, deci intră în TOATE și în STARE. */
    public Vreme vreme(String masina) {
        return vremi.computeIfAbsent(masina, id -> new Vreme(ceas));
    }

    public boolean cunoscuta(String masina) {
        return vremi.containsKey(masina);
    }

    /** În ordinea numerelor: masina-2 înainte de masina-10. Un nume fără număr vine la sfârșit. */
    public List<String> masini() {
        return vremi.keySet().stream()
                .sorted(Comparator.comparingInt(Registru::numar).thenComparing(Comparator.naturalOrder()))
                .toList();
    }

    public void adaugaLegatura(String masina, Closeable legatura) {
        legaturi.computeIfAbsent(masina, id -> ConcurrentHashMap.newKeySet()).add(legatura);
    }

    public void scoateLegatura(String masina, Closeable legatura) {
        Set<Closeable> deschise = legaturi.get(masina);
        if (deschise != null) {
            deschise.remove(legatura);
        }
    }

    /** Închide toate conexiunile deschise ale mașinii: așa începe un tunel. */
    public void inchideLegaturile(String masina) {
        Set<Closeable> deschise = legaturi.get(masina);
        if (deschise == null) {
            return;
        }
        for (Closeable legatura : List.copyOf(deschise)) {
            try {
                legatura.close();
            } catch (IOException e) {
                // Era deja închisă.
            }
        }
    }

    private static int numar(String id) {
        try {
            return Integer.parseInt(id.substring(id.lastIndexOf('-') + 1));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
