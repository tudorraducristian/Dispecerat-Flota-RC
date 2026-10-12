package ro.flota.simulator.testutil;

import java.util.ArrayDeque;
import java.util.Deque;
import ro.flota.simulator.simulare.Aleator;

/** Dă pe rând valorile primite, apoi doar valoarea implicită. Cu 0.99, „nu se întâmplă nimic rar". */
public final class AleatorScriptat implements Aleator {

    private final Deque<Double> valori = new ArrayDeque<>();
    private final double implicit;

    public AleatorScriptat(double implicit, double... valori) {
        this.implicit = implicit;
        for (double v : valori) {
            this.valori.addLast(v);
        }
    }

    @Override
    public double urmator() {
        return valori.isEmpty() ? implicit : valori.removeFirst();
    }
}
