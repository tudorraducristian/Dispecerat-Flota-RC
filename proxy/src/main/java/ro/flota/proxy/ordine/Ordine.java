package ro.flota.proxy.ordine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import ro.flota.proxy.vreme.Registru;

/**
 * Protocolul text de pe portul 1885 (spec §5.4): primește o linie și întoarce răspunsul, rând cu rând.
 * Cuvântul de comandă și TOATE nu țin cont de litere mari sau mici.
 */
public final class Ordine {

    public static final String OK = "OK";
    public static final String ERR_COMANDA = "ERR comanda necunoscuta";
    public static final String ERR_MASINA = "ERR masina necunoscuta";
    public static final String ERR_PARAMETRI = "ERR parametri gresiti";
    public static final String ERR_LINIE = "ERR linie prea lunga";
    public static final String SFARSIT = ".";

    private final Registru registru;

    public Ordine(Registru registru) {
        this.registru = registru;
    }

    public List<String> executa(String linie) {
        String[] cuvinte = linie.trim().split("\\s+");
        String comanda = cuvinte[0].toUpperCase(Locale.ROOT);
        String[] p = Arrays.copyOfRange(cuvinte, 1, cuvinte.length);
        try {
            return switch (comanda) {
                case "STARE" -> stare(p);
                case "INTARZIERE" -> intarziere(p);
                case "BANDA" -> banda(p);
                case "TUNEL" -> tunel(p);
                case "GAURA" -> gaura(p);
                case "NORMAL" -> normal(p);
                default -> List.of(ERR_COMANDA);
            };
        } catch (MasinaNecunoscuta e) {
            return List.of(ERR_MASINA);
        } catch (ParametriGresiti e) {
            return List.of(ERR_PARAMETRI);
        }
    }

    /** INTARZIERE <masina|TOATE> <ms> [<variatie_ms>] */
    private List<String> intarziere(String[] p) {
        numarParametri(p, 2, 3);
        List<String> masini = tinte(p[0]);
        int ms = numar(p[1], 0, 10_000);
        int variatie = p.length == 3 ? numar(p[2], 0, 10_000) : 0;
        masini.forEach(m -> registru.vreme(m).intarziere(ms, variatie));
        return List.of(OK);
    }

    /** BANDA <masina|TOATE> <kbps> */
    private List<String> banda(String[] p) {
        numarParametri(p, 2, 2);
        List<String> masini = tinte(p[0]);
        int kbps = numar(p[1], 1, 100_000);
        masini.forEach(m -> registru.vreme(m).banda(kbps));
        return List.of(OK);
    }

    /** TUNEL <masina|TOATE> <secunde>: conexiunile deschise se taie pe loc. */
    private List<String> tunel(String[] p) {
        numarParametri(p, 2, 2);
        List<String> masini = tinte(p[0]);
        int secunde = numar(p[1], 1, 600);
        for (String m : masini) {
            registru.vreme(m).tunel(secunde);
            registru.inchideLegaturile(m);
        }
        return List.of(OK);
    }

    /** GAURA <masina|TOATE> <secunde>: conexiunile rămân deschise, dar nu mai trece nimic. */
    private List<String> gaura(String[] p) {
        numarParametri(p, 2, 2);
        List<String> masini = tinte(p[0]);
        int secunde = numar(p[1], 1, 600);
        masini.forEach(m -> registru.vreme(m).gaura(secunde));
        return List.of(OK);
    }

    /** NORMAL <masina|TOATE> */
    private List<String> normal(String[] p) {
        numarParametri(p, 1, 1);
        tinte(p[0]).forEach(m -> registru.vreme(m).normal());
        return List.of(OK);
    }

    /** STARE: câte un rând pe mașină, apoi un rând care are doar „.”, ca la SMTP. */
    private List<String> stare(String[] p) {
        numarParametri(p, 0, 0);
        List<String> raspuns = new ArrayList<>();
        for (String masina : registru.masini()) {
            raspuns.add(masina + " " + registru.vreme(masina).descriere());
        }
        raspuns.add(SFARSIT);
        return raspuns;
    }

    /** Ținta unei comenzi: o mașină văzută de proxy, sau TOATE, adică toate cele văzute. */
    private List<String> tinte(String tinta) {
        if (tinta.equalsIgnoreCase("TOATE")) {
            return registru.masini();
        }
        String masina = tinta.toLowerCase(Locale.ROOT);
        if (!registru.cunoscuta(masina)) {
            throw new MasinaNecunoscuta();
        }
        return List.of(masina);
    }

    private static void numarParametri(String[] p, int minim, int maxim) {
        if (p.length < minim || p.length > maxim) {
            throw new ParametriGresiti();
        }
    }

    private static int numar(String text, int minim, int maxim) {
        int n;
        try {
            n = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new ParametriGresiti();
        }
        if (n < minim || n > maxim) {
            throw new ParametriGresiti();
        }
        return n;
    }

    /** Excepțiile duc greșeala direct la răspuns, din orice adâncime a parsării. */
    private static final class MasinaNecunoscuta extends RuntimeException {
    }

    private static final class ParametriGresiti extends RuntimeException {
    }
}
