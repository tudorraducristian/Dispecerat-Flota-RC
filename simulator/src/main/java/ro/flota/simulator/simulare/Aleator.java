package ro.flota.simulator.simulare;

import java.util.Random;

/** Sursa de întâmplare: un număr de la 0 (inclusiv) la 1 (exclusiv). În teste se înlocuiește cu o listă. */
@FunctionalInterface
public interface Aleator {

    double urmator();

    static Aleator din(Random random) {
        return random::nextDouble;
    }
}
