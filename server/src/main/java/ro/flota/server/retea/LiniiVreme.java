package ro.flota.server.retea;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import ro.flota.server.api.CerereGresita;

/**
 * Traduce o cerere din pagina Rețea într-o linie a protocolului proxy-ului (spec §5.4). Verifică doar
 * forma: comanda, ținta și numerele cerute. Intervalele le verifică proxy-ul, care răspunde cu ERR (D26).
 * Ținta e verificată strict, ca nimeni să nu poată strecura un \n și o a doua comandă în aceeași linie.
 */
public final class LiniiVreme {

    private static final Pattern TINTA = Pattern.compile("TOATE|masina-[0-9]+", Pattern.CASE_INSENSITIVE);

    private LiniiVreme() {
    }

    public static String linie(CerereVreme c) {
        if (c == null || c.comanda() == null) {
            throw new CerereGresita("lipsește comanda");
        }
        if (c.tinta() == null || !TINTA.matcher(c.tinta()).matches()) {
            throw new CerereGresita("ținta trebuie să fie TOATE sau masina-N");
        }
        String comanda = c.comanda().toUpperCase(Locale.ROOT);
        String tinta = c.tinta().equalsIgnoreCase("TOATE") ? "TOATE" : c.tinta().toLowerCase(Locale.ROOT);
        Map<String, Integer> p = c.parametri() == null ? Map.of() : c.parametri();
        return switch (comanda) {
            case "INTARZIERE" -> "INTARZIERE " + tinta + " " + numar(p, "ms") + " " + p.getOrDefault("variatie", 0);
            case "BANDA" -> "BANDA " + tinta + " " + numar(p, "kbps");
            case "TUNEL", "GAURA" -> comanda + " " + tinta + " " + numar(p, "secunde");
            case "NORMAL" -> "NORMAL " + tinta;
            default -> throw new CerereGresita("comandă de rețea necunoscută: " + c.comanda());
        };
    }

    /** Când se termină un TUNEL sau o GAURA cerute acum; null pentru celelalte comenzi. */
    public static Instant sfarsit(CerereVreme c, Instant start) {
        String comanda = c.comanda().toUpperCase(Locale.ROOT);
        if (!comanda.equals("TUNEL") && !comanda.equals("GAURA")) {
            return null;
        }
        return start.plusSeconds(c.parametri().get("secunde"));
    }

    private static int numar(Map<String, Integer> p, String cheie) {
        Integer n = p.get(cheie);
        if (n == null) {
            throw new CerereGresita("lipsește „" + cheie + "”");
        }
        return n;
    }
}
