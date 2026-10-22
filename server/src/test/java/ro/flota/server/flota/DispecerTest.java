package ro.flota.server.flota;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import ro.flota.server.comenzi.Comenzi;
import ro.flota.server.live.CanalLive;
import ro.flota.server.mqtt.MesajPrimit;
import ro.flota.server.reguli.RegulaOffline;
import ro.flota.server.reguli.ReguliAlarma;
import ro.flota.server.statistici.Statistici;

class DispecerTest {

    private static final Instant ACUM = Instant.parse("2026-12-10T10:15:00Z");

    private final Masini masini = mock(Masini.class);
    private final CanalLive live = mock(CanalLive.class);
    private final Dispecer dispecer = new Dispecer(mock(Pozitii.class), masini, mock(Alarme.class), mock(Comenzi.class), mock(ReguliAlarma.class), mock(RegulaOffline.class), mock(Statistici.class), live,
            Clock.fixed(ACUM, ZoneOffset.UTC));

    @Test
    void oStareNouaPleacaPeCanal() {
        when(masini.schimbaStare("masina-3", true, ACUM)).thenReturn(true);
        dispecer.primeste(new MesajPrimit.Stare("masina-3", true));
        verify(live).trimite("STARE", new StareMasinaDto("masina-3", "ONLINE", ACUM));
    }

    /** Starea e reținută: broker-ul o trimite din nou la fiecare abonare, deci și la fiecare pornire a serverului. */
    @Test
    void oStarePrimitaDinNouNuMaiPleacaPeCanal() {
        when(masini.schimbaStare("masina-3", false, ACUM)).thenReturn(false);
        dispecer.primeste(new MesajPrimit.Stare("masina-3", false));
        verify(live, never()).trimite(anyString(), any());
    }
}
