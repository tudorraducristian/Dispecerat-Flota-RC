package ro.flota.server.statistici;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.flota.server.live.CanalLive;

/** STATISTICI pe canalul live, o dată pe secundă (spec §6.6), doar dacă e cineva conectat. */
@Component
public class TrimitereStatistici {

    private final Statistici statistici;
    private final CanalLive live;

    public TrimitereStatistici(Statistici statistici, CanalLive live) {
        this.statistici = statistici;
        this.live = live;
    }

    @Scheduled(fixedRate = 1_000)
    public void trimite() {
        if (live.numarSesiuni() > 0) {
            live.trimite("STATISTICI", statistici.acum());
        }
    }
}
