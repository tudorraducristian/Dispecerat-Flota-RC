package ro.flota.simulator.masina;

import java.time.Clock;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import ro.flota.simulator.config.DefinitieMasina;
import ro.flota.simulator.config.Setari;
import ro.flota.simulator.mesaje.Mesaje;
import ro.flota.simulator.simulare.Aleator;
import ro.flota.simulator.simulare.Temperatura;
import ro.flota.simulator.simulare.Viteza;

/** O mașină: motorul pe firul lui, radioul pe al lui, iar între ele bufferul (D9). */
public final class Masina {

    private final String id;
    private final Conexiune conexiune;
    private final Buffer coada;
    private final Motor motor;
    private final Radio radio;
    private final long perioadaMs;
    private ScheduledExecutorService firMotor;
    private Thread firRadio;

    public Masina(DefinitieMasina def, Setari setari, Conexiune conexiune, Clock ceas, Random random) {
        this.id = def.id();
        this.conexiune = conexiune;
        this.perioadaMs = setari.perioadaMs();
        this.coada = new Buffer(setari.marimeBuffer());
        Viteza viteza = new Viteza(Aleator.din(random), Aleator.din(random), 0);
        Temperatura temperatura = new Temperatura(Aleator.din(random), Aleator.din(random), Temperatura.DE_BAZA);
        this.motor = new Motor(def, viteza, temperatura, ceas, coada, conexiune::conectata, perioadaMs / 1000.0);
        this.radio = new Radio(id, conexiune, coada, new PauzaReconectare(Aleator.din(random)), Thread::sleep);
        conexiune.laComanda(motor::primesteComanda);
    }

    public void porneste() {
        firRadio = new Thread(radio, id + "-radio");
        firRadio.start();
        firMotor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, id + "-motor"));
        firMotor.scheduleAtFixedRate(this::tickProtejat, 0, perioadaMs, TimeUnit.MILLISECONDS);
    }

    /** O excepție scăpată dintr-o sarcină programată o oprește în tăcere, de aceea o prindem aici. */
    private void tickProtejat() {
        try {
            motor.tick();
        } catch (RuntimeException e) {
            System.out.printf("[%s] eroare în motor: %s%n", id, e);
        }
    }

    /** Oprirea curată: motorul, apoi radioul, apoi mașina își anunță singură „offline” și se deconectează. */
    public void opreste() throws InterruptedException {
        if (firMotor != null) {
            firMotor.shutdownNow();
            firMotor.awaitTermination(2, TimeUnit.SECONDS);
        }
        radio.opreste();
        if (firRadio != null) {
            firRadio.interrupt();
            firRadio.join(2_000);
        }
        if (conexiune.conectata()) {
            try {
                conexiune.publica(Mesaje.stare(id, false));
            } catch (EroareConexiune e) {
                System.out.printf("[%s] n-am putut anunța offline: %s%n", id, e.getMessage());
            }
        }
        conexiune.deconecteaza();
    }
}
