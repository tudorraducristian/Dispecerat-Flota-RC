package ro.flota.server.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class IntervalTest {

    private static final Clock CEAS = Clock.fixed(Instant.parse("2026-12-10T10:15:00Z"), ZoneOffset.UTC);

    private static Instant ora(String text) {
        return Instant.parse(text);
    }

    private static String eroare(String de, String pana) {
        return assertThrows(CerereGresita.class, () -> Interval.din(de, pana, CEAS)).getMessage();
    }

    @Test
    void implicitSintUltimele5Minute() {
        assertEquals(new Interval(ora("2026-12-10T10:10:00Z"), ora("2026-12-10T10:15:00Z")), Interval.din(null, null, CEAS));
    }

    @Test
    void campurileGoaleInseamnaImplicit() {
        assertEquals(Interval.din(null, null, CEAS), Interval.din("", " ", CEAS));
    }

    @Test
    void doarDeInseamnaPanaAcum() {
        assertEquals(new Interval(ora("2026-12-10T10:00:00Z"), ora("2026-12-10T10:15:00Z")),
                Interval.din("2026-12-10T10:00:00Z", null, CEAS));
    }

    @Test
    void doarPanaInseamnaCu5MinuteInainte() {
        assertEquals(new Interval(ora("2026-12-10T08:55:00Z"), ora("2026-12-10T09:00:00Z")),
                Interval.din(null, "2026-12-10T09:00:00Z", CEAS));
    }

    @Test
    void deDupaPanaEsteRefuzat() {
        assertTrue(eroare("2026-12-10T10:00:00Z", "2026-12-10T09:00:00Z").contains("înainte"));
    }

    /** Registrul fazei 2: înainte verifica doar că mesajul conține „de”, ceea ce trece aproape orice text. */
    @Test
    void oraScrisaGresitEstePusaPeNume() {
        assertEquals("„de” nu e o oră ISO-8601 în UTC: ieri", eroare("ieri", null));
        assertEquals("„pana” nu e o oră ISO-8601 în UTC: mâine", eroare(null, "mâine"));
    }

    @Test
    void maiMultDe6OreEsteRefuzat() {
        assertTrue(eroare("2026-12-10T04:14:59Z", "2026-12-10T10:15:00Z").contains("6 ore"));
    }

    @Test
    void exact6OreEsteAcceptat() {
        assertEquals(new Interval(ora("2026-12-10T04:15:00Z"), ora("2026-12-10T10:15:00Z")),
                Interval.din("2026-12-10T04:15:00Z", "2026-12-10T10:15:00Z", CEAS));
    }
}
