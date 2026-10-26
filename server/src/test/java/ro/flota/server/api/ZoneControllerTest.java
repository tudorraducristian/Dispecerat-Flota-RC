package ro.flota.server.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ro.flota.server.date.RegistruFlota;
import ro.flota.server.flota.ZonaDto;
import ro.flota.server.flota.Zone;

@WebMvcTest(ZoneController.class)
@Import(RegistruFlota.class)
class ZoneControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RegistruFlota registru;

    @MockitoBean
    private Zone zone;

    private static ZonaDto zona(String id) {
        return new ZonaDto(id, "Zona " + id, List.of(List.of(46.0, 23.0), List.of(46.0, 23.01), List.of(46.01, 23.0)), 50);
    }

    /** Registrul fazei 2: o zonă scoasă din zone.json rămânea desenată pe hartă. */
    @Test
    void zonaScoasaDinDateNuMaiApare() throws Exception {
        registru.incarca(List.of(MasiniControllerTest.Z1), List.of());
        when(zone.toate()).thenReturn(List.of(zona("z1"), zona("z9")));
        mvc.perform(get("/api/zone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("z1"));
    }
}
