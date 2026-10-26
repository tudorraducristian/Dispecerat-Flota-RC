package ro.flota.server.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.flota.server.date.MasinaDefinita;
import ro.flota.server.date.Punct;
import ro.flota.server.date.RegistruFlota;
import ro.flota.server.date.ZonaDefinita;
import ro.flota.server.flota.MasinaDto;
import ro.flota.server.flota.Masini;
import ro.flota.server.flota.Pozitii;

/**
 * Registrul fazei 2: lipsea un test care să treacă prin Spring MVC, cu JSON-ul și codurile adevărate.
 * Masini și Pozitii sînt înlocuite, deci testul nu are nevoie de MySQL; registrul flotei e cel real.
 */
@WebMvcTest(MasiniController.class)
@Import(RegistruFlota.class)
class MasiniControllerTest {

    static final ZonaDefinita Z1 = new ZonaDefinita("z1", "Centru",
            List.of(new Punct(46, 23), new Punct(46, 23.01), new Punct(46.01, 23), new Punct(46, 23)), 50);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RegistruFlota registru;

    @MockitoBean
    private Masini masini;

    @MockitoBean
    private Pozitii pozitii;

    @BeforeEach
    void flota() {
        registru.incarca(List.of(Z1), List.of(new MasinaDefinita("masina-1", "z1"), new MasinaDefinita("masina-3", "z1")));
    }

    private static MasinaDto masina(String id) {
        return new MasinaDto(id, "z1", "ONLINE", Instant.parse("2026-12-10T10:15:03.120Z"), 2, 1, null);
    }

    @Test
    void listaIeseCaJsonCuOraInIso() throws Exception {
        when(masini.toate()).thenReturn(List.of(masina("masina-1")));
        mvc.perform(get("/api/masini"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("masina-1"))
                .andExpect(jsonPath("$[0].stare").value("ONLINE"))
                .andExpect(jsonPath("$[0].ultimaVedere").value("2026-12-10T10:15:03.120Z"))
                .andExpect(jsonPath("$[0].pierdute").value(1));
    }

    @Test
    void masinaNecunoscutaDa404CaJson() throws Exception {
        when(masini.una("masina-99")).thenReturn(Optional.empty());
        mvc.perform(get("/api/masini/masina-99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mesaj").value("mașina masina-99 nu există"));
    }

    @Test
    void oraScrisaGresitDa400CaJson() throws Exception {
        when(masini.una("masina-3")).thenReturn(Optional.of(masina("masina-3")));
        mvc.perform(get("/api/masini/masina-3/traseu").param("de", "ieri"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mesaj").value("„de” nu e o oră ISO-8601 în UTC: ieri"));
    }

    /** Registrul fazei 2: o mașină scoasă din flota.json rămâne în bază, cu istoricul ei, dar nu pe hartă. */
    @Test
    void masinaScoasaDinFlotaNuMaiApareInLista() throws Exception {
        when(masini.toate()).thenReturn(List.of(masina("masina-1"), masina("masina-7")));
        mvc.perform(get("/api/masini"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("masina-1"));
    }

    @Test
    void masinaScoasaDinFlotaDa404() throws Exception {
        when(masini.una("masina-7")).thenReturn(Optional.of(masina("masina-7")));
        mvc.perform(get("/api/masini/masina-7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mesaj").value("mașina masina-7 nu există"));
    }
}
