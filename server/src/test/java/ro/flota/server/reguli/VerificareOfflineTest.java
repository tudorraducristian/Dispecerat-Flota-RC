package ro.flota.server.reguli;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import ro.flota.server.flota.Alarme;
import ro.flota.server.live.CanalLive;

class VerificareOfflineTest {

    private static final Instant T0 = Instant.parse("2026-12-10T10:00:00Z");

    /** O alarmă care nu se poate salva, de exemplu pentru o mașină necunoscută, nu le oprește pe celelalte. */
    @Test
    void oAlarmaCareNuSeSalveazaNuLeOprestePeCelelalte() {
        RegulaOffline regula = new RegulaOffline();
        regula.stare("masina-1", false, T0);
        regula.stare("masina-2", false, T0);
        Alarme alarme = mock(Alarme.class);
        when(alarme.salveaza(eq("masina-1"), anyString(), anyString(), any(), any(), anyString()))
                .thenThrow(new IllegalStateException("cheie străină"));
        VerificareOffline verificare = new VerificareOffline(regula, alarme, mock(CanalLive.class),
                Clock.fixed(T0.plusSeconds(61), ZoneOffset.UTC));

        verificare.verifica();

        verify(alarme).salveaza(eq("masina-2"), eq("OFFLINE_PRELUNGIT"), eq("SERVER"), any(), any(), anyString());
    }
}
