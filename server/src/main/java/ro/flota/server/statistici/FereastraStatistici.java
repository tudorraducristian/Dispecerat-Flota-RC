package ro.flota.server.statistici;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Statisticile unei mașini (spec §6.5, endpoint-ul 12): câte mesaje au sosit în ultimele 10 secunde și
 * latența medie a ultimelor 10 mesaje live. Mesajele recuperate nu intră în latență: ora lor de sosire
 * spune cât a stat mașina în tunel, nu cât e de lentă rețeaua (D26).
 */
final class FereastraStatistici {

    static final Duration FEREASTRA = Duration.ofSeconds(10);
    static final int ULTIMELE = 10;

    private final Deque<Instant> sosiri = new ArrayDeque<>();
    private final Deque<Long> latente = new ArrayDeque<>();

    void mesaj(Instant oraMasinii, Instant oraSosirii, boolean dinBuffer) {
        sosiri.addLast(oraSosirii);
        if (!dinBuffer) {
            latente.addLast(Duration.between(oraMasinii, oraSosirii).toMillis());
            if (latente.size() > ULTIMELE) {
                latente.removeFirst();
            }
        }
    }

    double mesajePeSecunda(Instant acum) {
        Instant limita = acum.minus(FEREASTRA);
        while (!sosiri.isEmpty() && sosiri.peekFirst().isBefore(limita)) {
            sosiri.removeFirst();
        }
        return sosiri.size() / (double) FEREASTRA.toSeconds();
    }

    Double latentaMedieMs() {
        return latente.isEmpty() ? null : latente.stream().mapToLong(Long::longValue).average().orElseThrow();
    }
}
