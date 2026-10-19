package ro.flota.proxy.testutil;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Un ceas care stă pe loc până îl muți tu. Așa, un tunel de 15 secunde se testează într-o clipă. */
public final class CeasManual extends Clock {

    private Instant acum;

    public CeasManual(String inceputIso) {
        this.acum = Instant.parse(inceputIso);
    }

    public void avanseaza(long ms) {
        acum = acum.plusMillis(ms);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }

    @Override
    public Instant instant() {
        return acum;
    }
}
