package ro.flota.server.reguli;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.flota.server.flota.Alarme;
import ro.flota.server.live.CanalLive;

/** La 5 secunde, caută mașinile offline de peste 60 de secunde (spec §6.4). */
@Component
public class VerificareOffline {

    private final RegulaOffline regula;
    private final Alarme alarme;
    private final CanalLive live;
    private final Clock ceas;

    public VerificareOffline(RegulaOffline regula, Alarme alarme, CanalLive live, Clock ceas) {
        this.regula = regula;
        this.alarme = alarme;
        this.live = live;
        this.ceas = ceas;
    }

    @Scheduled(fixedRate = 5_000)
    public void verifica() {
        Instant acum = ceas.instant();
        for (String id : regula.deAlarmat(acum)) {
            long secunde = Duration.between(regula.offlineDin(id), acum).toSeconds();
            live.trimite("ALARMA", alarme.salveaza(id, "OFFLINE_PRELUNGIT", "SERVER", acum, acum,
                    "offline de " + secunde + " de secunde"));
        }
    }
}
