package ro.flota.server.flota;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.flota.server.comenzi.ComandaDto;
import ro.flota.server.comenzi.Comenzi;
import ro.flota.server.live.CanalLive;
import ro.flota.server.mqtt.MesajPrimit;

@ExtendWith(MockitoExtension.class)
class DispecerConfirmariTest {

    private static final Instant ORA = Instant.parse("2026-12-10T10:15:04.002Z");

    @Mock
    Pozitii pozitii;
    @Mock
    Masini masini;
    @Mock
    Alarme alarme;
    @Mock
    Comenzi comenzi;
    @Mock
    CanalLive live;
    @Mock
    Clock ceas;

    @InjectMocks
    Dispecer dispecer;

    @Test
    void aceeasiConfirmareDeDouaOriSeAnuntaOData() {
        MesajPrimit.Confirmare confirmare = new MesajPrimit.Confirmare("masina-3", 57, ORA, "EXECUTATA", null);
        ComandaDto confirmata = new ComandaDto(57, "masina-3", "LIMITEAZA_VITEZA", null,
                Instant.parse("2026-12-10T10:15:03Z"), ORA, "CONFIRMATA", null);
        when(comenzi.confirmaDacaTrimisa(57, "masina-3", "CONFIRMATA", ORA, null)).thenReturn(true, false);
        when(comenzi.una(57)).thenReturn(Optional.of(confirmata));

        dispecer.primeste(confirmare);
        dispecer.primeste(confirmare);

        verify(live, times(1)).trimite("CONFIRMARE", confirmata);
    }

    @Test
    void respinsaDevineRespinsaCuMotiv() {
        dispecer.primeste(new MesajPrimit.Confirmare("masina-3", 58, ORA, "RESPINSA", "kmh în afara intervalului"));
        verify(comenzi).confirmaDacaTrimisa(58, "masina-3", "RESPINSA", ORA, "kmh în afara intervalului");
        verify(live, never()).trimite(anyString(), any());
    }
}
