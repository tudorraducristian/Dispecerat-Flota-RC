package ro.flota.server.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.statistici.StatisticaDto;
import ro.flota.server.statistici.Statistici;

/** Endpoint-ul 12 din spec §6.5. */
@RestController
public class StatisticiController {

    private final Statistici statistici;

    public StatisticiController(Statistici statistici) {
        this.statistici = statistici;
    }

    @GetMapping("/api/statistici")
    public List<StatisticaDto> toate() {
        return statistici.acum();
    }
}
