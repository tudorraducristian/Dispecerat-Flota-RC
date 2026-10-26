package ro.flota.server.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.flota.server.flota.MasinaDto;
import ro.flota.server.flota.Masini;
import ro.flota.server.flota.Pozitii;

/**
 * Registrul fazei 2: lipsea un test care să treacă prin Spring MVC, cu JSON-ul și codurile adevărate.
 * Masini și Pozitii sînt înlocuite, deci testul nu are nevoie de MySQL.
 */
@WebMvcTest(MasiniController.class)
class MasiniControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private Masini masini;

    @MockitoBean
    private Pozitii pozitii;

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
}
