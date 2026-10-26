package ro.flota.server.reguli;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.flota.server.flota.Alarme;
import ro.flota.server.live.CanalLive;
import ro.flota.server.setari.Setari;

/** La 5 secunde, caută mașinile offline de peste pragul din setări, implicit 60 de secunde (spec §6.4, §16.3). */
@Component
public class VerificareOffline {

    private static final Logger log = LoggerFactory.getLogger(VerificareOffline.class);

    private final RegulaOffline regula;
    private final Alarme alarme;
    private final CanalLive live;
    private final Setari setari;
    private final Clock ceas;

    public VerificareOffline(RegulaOffline regula, Alarme alarme, CanalLive live, Setari setari, Clock ceas) {
        this.regula = regula;
        this.alarme = alarme;
        this.live = live;
        this.setari = setari;
        this.ceas = ceas;
    }

    @Scheduled(fixedRate = 5_000)
    public void verifica() {
        Instant acum = ceas.instant();
        for (String id : regula.deAlarmat(acum, setari.offlinePrelungit())) {
            Instant din = regula.offlineDin(id);
            if (din == null) {
                continue; // a revenit ONLINE chiar acum, între cele două citiri
            }
            // O alarmă care nu se poate salva, de exemplu pentru o mașină necunoscută, nu le oprește pe celelalte.
            try {
                long secunde = Duration.between(din, acum).toSeconds();
                live.trimite("ALARMA", alarme.salveaza(id, "OFFLINE_PRELUNGIT", "SERVER", acum, acum,
                        "offline de " + secunde + " de secunde"));
            } catch (RuntimeException e) {
                log.warn("{}: n-am putut da alarma OFFLINE_PRELUNGIT: {}", id, e.getMessage());
            }
        }
    }
}
