package ro.flota.server.statistici;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.server.date.MasinaDefinita;
import ro.flota.server.date.Punct;
import ro.flota.server.date.RegistruFlota;
import ro.flota.server.date.ZonaDefinita;
import ro.flota.server.flota.ContoareDto;
import ro.flota.server.flota.Masini;

class StatisticiTest {

    /** Registrul fazei 2: tabelul din pagina Rețea arăta și mașinile scoase din flota.json. */
    @Test
    void doarMasinileDinFlotaIntraInStatistici() {
        Masini masini = mock(Masini.class);
        when(masini.contoare()).thenReturn(List.of(new ContoareDto("masina-1", 0, 0), new ContoareDto("masina-7", 3, 4)));
        RegistruFlota registru = new RegistruFlota();
        registru.incarca(List.of(new ZonaDefinita("z1", "Centru", List.of(new Punct(46, 23)), 50)),
                List.of(new MasinaDefinita("masina-1", "z1")));
        Statistici statistici = new Statistici(masini, registru,
                Clock.fixed(Instant.parse("2026-12-10T10:15:00Z"), ZoneOffset.UTC));

        List<StatisticaDto> acum = statistici.acum();

        assertEquals(1, acum.size());
        assertEquals("masina-1", acum.get(0).masinaId());
    }
}
