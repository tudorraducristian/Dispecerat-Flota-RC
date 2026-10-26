package ro.flota.server.conturi;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
import ro.flota.server.api.CerereGresita;

/** Regulile conturilor din spec §16.2 și din constrângerile fazei 6. Încălcarea lor devine 400. */
public final class ReguliCont {

    private static final Pattern NUME = Pattern.compile("^[a-z0-9._-]{3,20}$");
    private static final int PAROLA_MINIM_CARACTERE = 8;
    /** BCrypt citește doar primii 72 de octeți; restul s-ar ignora în tăcere. */
    private static final int PAROLA_MAXIM_OCTETI = 72;

    private ReguliCont() {
    }

    /** La login și la creare: „  Admin ” și „admin” sînt același cont. */
    public static String normalizeaza(String nume) {
        return nume == null ? "" : nume.trim().toLowerCase(Locale.ROOT);
    }

    public static void verificaNume(String nume) {
        if (nume == null || !NUME.matcher(nume).matches()) {
            throw new CerereGresita("numele are 3–20 caractere: litere mici, cifre, punct, _ sau -");
        }
    }

    public static void verificaParola(String parola) {
        if (parola == null || parola.codePointCount(0, parola.length()) < PAROLA_MINIM_CARACTERE) {
            throw new CerereGresita("parola are cel puțin 8 caractere");
        }
        if (parola.getBytes(StandardCharsets.UTF_8).length > PAROLA_MAXIM_OCTETI) {
            throw new CerereGresita("parola are cel mult 72 de octeți");
        }
    }
}
