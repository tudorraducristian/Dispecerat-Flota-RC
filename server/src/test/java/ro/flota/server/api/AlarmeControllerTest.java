package ro.flota.server.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ro.flota.server.conturi.Rol;
import ro.flota.server.conturi.ServiciuAutentificare;
import ro.flota.server.conturi.UtilizatorCurent;
import ro.flota.server.flota.Alarme;
import ro.flota.server.flota.NumarAlarme;

/**
 * Cererile adăugate pentru pagina Alarme (redesign §5): numărul, „toate văzute” și filtrul după tip.
 * Alarme e înlocuit, deci testul nu are nevoie de MySQL; SQL-ul are testele lui, în AlarmeTest.
 */
@WebMvcTest(AlarmeController.class)
class AlarmeControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private Alarme alarme;

    @MockitoBean
    private ServiciuAutentificare autentificare;

    @BeforeEach
    void dispecer() {
        when(autentificare.valideaza("t")).thenReturn(Optional.of(new UtilizatorCurent(2, "ana", Rol.DISPECER, "t")));
    }

    private static MockHttpServletRequestBuilder get(String url) {
        return MockMvcRequestBuilders.get(url).header("Authorization", "Bearer t");
    }

    private static MockHttpServletRequestBuilder post(String url) {
        return MockMvcRequestBuilders.post(url).header("Authorization", "Bearer t");
    }

    /** 27 octombrie e pe ora de iarnă, UTC+2: ziua românească începe la 22:00 UTC, cu o zi înainte. */
    @Test
    void inceputulZileiIarna() {
        assertEquals(Instant.parse("2026-10-26T22:00:00Z"),
                AlarmeController.inceputulZilei(Instant.parse("2026-10-27T21:30:00Z")));
        assertEquals(Instant.parse("2026-10-27T22:00:00Z"),
                AlarmeController.inceputulZilei(Instant.parse("2026-10-27T22:30:00Z")));
    }

    /** 2 iulie e pe ora de vară, UTC+3. */
    @Test
    void inceputulZileiVara() {
        assertEquals(Instant.parse("2026-07-01T21:00:00Z"),
                AlarmeController.inceputulZilei(Instant.parse("2026-07-01T21:30:00Z")));
    }

    /** Pe 25 octombrie ora se schimbă la 04:00, dar miezul nopții era încă pe UTC+3. */
    @Test
    void inceputulZileiInZiuaSchimbariiOrei() {
        assertEquals(Instant.parse("2026-10-24T21:00:00Z"),
                AlarmeController.inceputulZilei(Instant.parse("2026-10-25T12:00:00Z")));
    }

    @Test
    void numarulIeseCaJson() throws Exception {
        when(alarme.numar(any())).thenReturn(new NumarAlarme(12, 148));
        mvc.perform(get("/api/alarme/numar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nevazute").value(12))
                .andExpect(jsonPath("$.azi").value(148));
    }

    @Test
    void listaTrimiteTipulMaiDeparte() throws Exception {
        when(alarme.lista(false, "FRANARE_BRUSCA")).thenReturn(List.of());
        mvc.perform(get("/api/alarme?vazute=false&tip=FRANARE_BRUSCA")).andExpect(status().isOk());
        verify(alarme).lista(false, "FRANARE_BRUSCA");
    }

    @Test
    void listaFaraTipTrimiteNull() throws Exception {
        mvc.perform(get("/api/alarme")).andExpect(status().isOk());
        verify(alarme).lista(null, null);
    }

    @Test
    void tipNecunoscutDa400() throws Exception {
        mvc.perform(get("/api/alarme?tip=CEVA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mesaj").value("tip de alarmă necunoscut: CEVA"));
        verifyNoInteractions(alarme);
    }

    @Test
    void toateVazuteIntoarceCate() throws Exception {
        when(alarme.marcheazaToateVazute(null)).thenReturn(7);
        mvc.perform(post("/api/alarme/vazute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marcate").value(7));
    }

    @Test
    void toateVazuteCuTip() throws Exception {
        mvc.perform(post("/api/alarme/vazute?tip=IESIRE_DIN_ZONA")).andExpect(status().isOk());
        verify(alarme).marcheazaToateVazute("IESIRE_DIN_ZONA");
    }

    @Test
    void toateVazuteCuTipNecunoscutDa400() throws Exception {
        mvc.perform(post("/api/alarme/vazute?tip=CEVA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mesaj").value("tip de alarmă necunoscut: CEVA"));
        verifyNoInteractions(alarme);
    }

    @Test
    void numarulCereToken() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/alarme/numar"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mesaj").value("trebuie să te autentifici"));
    }
}
