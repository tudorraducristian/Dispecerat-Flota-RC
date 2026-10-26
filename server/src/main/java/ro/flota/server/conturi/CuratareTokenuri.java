package ro.flota.server.conturi;

import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.flota.server.live.CanalLive;

/**
 * O dată pe minut: canalele live deschise cu un token expirat se închid, apoi token-urile expirate se șterg.
 * Fără ea, un tab lăsat deschis ar primi date live și după cele 8 ore.
 */
@Component
public class CuratareTokenuri {

    private final Tokenuri tokenuri;
    private final CanalLive live;
    private final Clock ceas;

    public CuratareTokenuri(Tokenuri tokenuri, CanalLive live, Clock ceas) {
        this.tokenuri = tokenuri;
        this.live = live;
        this.ceas = ceas;
    }

    @Scheduled(fixedRate = 60_000)
    public void curata() {
        Instant acum = ceas.instant();
        for (String token : tokenuri.expirate(acum)) {
            live.inchideSesiunileTokenului(token);
        }
        tokenuri.stergeExpirate(acum);
    }
}
