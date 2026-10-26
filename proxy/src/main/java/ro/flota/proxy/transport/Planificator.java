package ro.flota.proxy.transport;

import java.util.random.RandomGenerator;

/**
 * Ora de plecare a fiecărei bucăți (spec §5.3): acum + întârziere ± variație, dar niciodată
 * înaintea bucății dinainte. Fără regula asta, variația ar inversa bucăți, iar TCP garantează ordinea.
 * Îl folosește firul care citește. Singura excepție e Sens.trimiteIntai, chemată din firul conexiunii
 * înainte de porneste(); Thread.start garantează că firul de citire vede tot ce s-a scris înainte să
 * pornească (happens-before), deci nici atunci nu e nevoie de synchronized.
 */
final class Planificator {

    private final RandomGenerator aleator;
    private long ultimaPlecare = Long.MIN_VALUE;

    Planificator(RandomGenerator aleator) {
        this.aleator = aleator;
    }

    long plecare(long acumMs, int intarziereMs, int variatieMs) {
        long abatere = variatieMs == 0 ? 0 : aleator.nextLong(-variatieMs, variatieMs + 1L);
        long plecare = Math.max(acumMs, acumMs + intarziereMs + abatere);
        plecare = Math.max(plecare, ultimaPlecare);
        ultimaPlecare = plecare;
        return plecare;
    }
}
