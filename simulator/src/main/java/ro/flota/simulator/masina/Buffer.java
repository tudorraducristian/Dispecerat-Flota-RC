package ro.flota.simulator.masina;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.TipMesaj;

/**
 * Coada dintre motor și radio (spec §4.3): un monitor, adică o clasă în care un singur fir intră pe rând.
 * Cât mașina e conectată, e aproape goală. În tunel se umple; plină, aruncă întâi telemetria cea mai veche.
 * Alarmele și celelalte mesaje nu se aruncă niciodată.
 */
public final class Buffer {

    private final int capacitate;
    private final Deque<Mesaj> mesaje = new ArrayDeque<>();
    private long aruncate = 0;

    public Buffer(int capacitate) {
        if (capacitate < 1) {
            throw new IllegalArgumentException("capacitatea bufferului trebuie să fie cel puțin 1");
        }
        this.capacitate = capacitate;
    }

    /** Producătorul: pune la coadă. Dacă e plin, face loc aruncând cea mai veche telemetrie. */
    public synchronized void adauga(Mesaj mesaj) {
        if (mesaje.size() >= capacitate && aruncaCeaMaiVecheTelemetrie()) {
            aruncate++;
        }
        mesaje.addLast(mesaj);
        notifyAll();
    }

    /** Consumatorul: primul mesaj, așteptând cel mult ms milisecunde. null dacă nu vine nimic. */
    public synchronized Mesaj ia(long ms) throws InterruptedException {
        long limita = System.currentTimeMillis() + ms;
        while (mesaje.isEmpty()) {
            long ramas = limita - System.currentTimeMillis();
            if (ramas <= 0) {
                return null;
            }
            wait(ramas);
        }
        return mesaje.pollFirst();
    }

    /** Consumatorul: un mesaj care n-a putut pleca revine primul la rând. Nu aruncă nimic. */
    public synchronized void puneInapoi(Mesaj mesaj) {
        mesaje.addFirst(mesaj);
        notifyAll();
    }

    public synchronized int marime() {
        return mesaje.size();
    }

    public synchronized long aruncate() {
        return aruncate;
    }

    public synchronized List<Mesaj> continut() {
        return List.copyOf(mesaje);
    }

    private boolean aruncaCeaMaiVecheTelemetrie() {
        Iterator<Mesaj> it = mesaje.iterator();
        while (it.hasNext()) {
            if (it.next().tip() == TipMesaj.TELEMETRIE) {
                it.remove();
                return true;
            }
        }
        return false;
    }
}
