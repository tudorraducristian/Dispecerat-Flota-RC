package ro.flota.server.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.flota.ZonaDto;
import ro.flota.server.flota.Zone;

/** Endpoint-ul 8 din spec §6.5: zonele, doar de citit. */
@RestController
public class ZoneController {

    private final Zone zone;

    public ZoneController(Zone zone) {
        this.zone = zone;
    }

    @GetMapping("/api/zone")
    public List<ZonaDto> toate() {
        return zone.toate();
    }
}
