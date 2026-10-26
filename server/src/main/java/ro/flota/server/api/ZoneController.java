package ro.flota.server.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.date.RegistruFlota;
import ro.flota.server.flota.ZonaDto;
import ro.flota.server.flota.Zone;

/** Endpoint-ul 8 din spec §6.5: zonele, doar de citit, doar cele din zone.json de acum. */
@RestController
public class ZoneController {

    private final Zone zone;
    private final RegistruFlota registru;

    public ZoneController(Zone zone, RegistruFlota registru) {
        this.zone = zone;
        this.registru = registru;
    }

    @GetMapping("/api/zone")
    public List<ZonaDto> toate() {
        return zone.toate().stream().filter(z -> registru.zone().contains(z.id())).toList();
    }
}
