package ro.flota.server.conturi;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.server.live.CanalLive;

class CuratareTokenuriTest {

    private static final Instant T0 = Instant.parse("2026-12-10T10:00:00Z");

    private final Tokenuri tokenuri = mock(Tokenuri.class);
    private final CanalLive live = mock(CanalLive.class);
    private final CuratareTokenuri curatare = new CuratareTokenuri(tokenuri, live, Clock.fixed(T0, ZoneOffset.UTC));

    @Test
    void tokenulExpiratInchideCanalulSiSeSterge() {
        when(tokenuri.expirate(T0)).thenReturn(List.of("t"));
        curatare.curata();
        verify(live).inchideSesiunileTokenului("t");
        verify(tokenuri).stergeExpirate(T0);
    }
}
