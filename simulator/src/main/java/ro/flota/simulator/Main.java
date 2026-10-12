package ro.flota.simulator;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import ro.flota.simulator.config.DefinitieMasina;
import ro.flota.simulator.config.EroareConfigurare;
import ro.flota.simulator.config.Flota;
import ro.flota.simulator.config.Setari;
import ro.flota.simulator.masina.ConexiuneHiveMq;
import ro.flota.simulator.masina.Masina;

/** Pornește flota din date\ și o oprește curat la Ctrl+C. Argumentul e dosarul cu date. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        Path dosarDate = Path.of(args.length > 0 ? args[0] : "date");
        Setari setari = Setari.implicite();
        List<DefinitieMasina> flota;
        try {
            flota = Flota.citeste(dosarDate);
        } catch (EroareConfigurare e) {
            System.err.println("Configurare greșită, nu pornesc nicio mașină. " + e.getMessage());
            System.exit(1);
            return;
        }
        System.out.printf("Pornesc %d mașini spre %s:%d, cu keepalive de %d s%n",
                flota.size(), setari.brokerHost(), setari.brokerPort(), setari.keepaliveSecunde());

        List<Masina> masini = new ArrayList<>();
        for (DefinitieMasina def : flota) {
            masini.add(new Masina(def, setari, new ConexiuneHiveMq(def.id(), setari), Clock.systemUTC(), new Random()));
        }
        CountDownLatch oprita = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Opresc flota...");
            for (Masina m : masini) {
                try {
                    m.opreste();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            oprita.countDown();
        }));
        masini.forEach(Masina::porneste);
        oprita.await();
    }
}
