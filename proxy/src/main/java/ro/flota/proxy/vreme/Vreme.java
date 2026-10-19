package ro.flota.proxy.vreme;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.StringJoiner;

/**
 * Vremea unei mașini (spec §5.3). O schimbă firul de ordine și o citesc firele care scriu,
 * deci toate metodele sînt synchronized: un singur fir pe rând are voie înăuntru.
 */
public final class Vreme {

    private final Clock ceas;
    private int intarziereMs;
    private int variatieMs;
    private int bandaKbps;
    private Instant tunelPana;
    private Instant gauraPana;

    public Vreme(Clock ceas) {
        this.ceas = ceas;
    }

    public synchronized void intarziere(int ms, int variatie) {
        intarziereMs = ms;
        variatieMs = variatie;
    }

    public synchronized void banda(int kbps) {
        bandaKbps = kbps;
    }

    /** Un tunel nou înlocuiește o gaură în curs (D24). */
    public synchronized void tunel(int secunde) {
        tunelPana = ceas.instant().plusSeconds(secunde);
        gauraPana = null;
    }

    /** O gaură nouă înlocuiește un tunel în curs (D24). */
    public synchronized void gaura(int secunde) {
        gauraPana = ceas.instant().plusSeconds(secunde);
        tunelPana = null;
    }

    public synchronized void normal() {
        intarziereMs = 0;
        variatieMs = 0;
        bandaKbps = 0;
        tunelPana = null;
        gauraPana = null;
    }

    public synchronized boolean inTunel() {
        return activ(tunelPana);
    }

    /** Până când ține gaura, sau null dacă acum nu e gaură. */
    public synchronized Instant gauraPana() {
        return activ(gauraPana) ? gauraPana : null;
    }

    public synchronized int intarziereMs() {
        return intarziereMs;
    }

    public synchronized int variatieMs() {
        return variatieMs;
    }

    /** 0 înseamnă fără limită. */
    public synchronized int bandaKbps() {
        return bandaKbps;
    }

    /** Rândul din răspunsul la STARE, fără numele mașinii. Tunelul și gaura le ascund pe celelalte cât durează. */
    public synchronized String descriere() {
        if (activ(tunelPana)) {
            return "TUNEL " + secundeRamase(tunelPana);
        }
        if (activ(gauraPana)) {
            return "GAURA " + secundeRamase(gauraPana);
        }
        StringJoiner d = new StringJoiner(" ");
        if (intarziereMs > 0 || variatieMs > 0) {
            d.add("INTARZIERE " + intarziereMs + " " + variatieMs);
        }
        if (bandaKbps > 0) {
            d.add("BANDA " + bandaKbps);
        }
        return d.length() == 0 ? "NORMAL" : d.toString();
    }

    private boolean activ(Instant pana) {
        return pana != null && ceas.instant().isBefore(pana);
    }

    /** Rotunjit în sus: cu 0,5 secunde rămase, scrie 1, nu 0. */
    private long secundeRamase(Instant pana) {
        long ms = Duration.between(ceas.instant(), pana).toMillis();
        return (ms + 999) / 1000;
    }
}
