package ro.flota.server.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/** Intervalul pentru traseu (endpoint-ul 3): implicit ultimele 5 minute, cel mult 6 ore. */
public record Interval(Instant de, Instant pana) {

    static final Duration IMPLICIT = Duration.ofMinutes(5);
    static final Duration MAXIM = Duration.ofHours(6);

    public static Interval din(String de, String pana, Clock ceas) {
        Instant p = gol(pana) ? ceas.instant() : ora("pana", pana);
        Instant d = gol(de) ? p.minus(IMPLICIT) : ora("de", de);
        if (!d.isBefore(p)) {
            throw new CerereGresita("„de” trebuie să fie înainte de „pana”");
        }
        if (Duration.between(d, p).compareTo(MAXIM) > 0) {
            throw new CerereGresita("intervalul e mai lung de 6 ore");
        }
        return new Interval(d, p);
    }

    private static boolean gol(String text) {
        return text == null || text.isBlank();
    }

    private static Instant ora(String camp, String text) {
        try {
            return Instant.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new CerereGresita("„" + camp + "” nu e o oră ISO-8601 în UTC: " + text);
        }
    }
}
