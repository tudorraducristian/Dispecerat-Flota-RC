package ro.flota.server.api;

import java.time.Clock;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.date.RegistruFlota;
import ro.flota.server.flota.MasinaDto;
import ro.flota.server.flota.Masini;
import ro.flota.server.flota.PozitieDto;
import ro.flota.server.flota.Pozitii;

/**
 * Endpoint-urile 1, 2 și 3 din spec §6.5. Arată doar flota din flota.json: o mașină scoasă de acolo
 * rămâne în bază, cu istoricul ei, dar nu mai apare pe hartă.
 */
@RestController
@RequestMapping("/api/masini")
public class MasiniController {

    private final Masini masini;
    private final Pozitii pozitii;
    private final RegistruFlota registru;
    private final Clock ceas;

    public MasiniController(Masini masini, Pozitii pozitii, RegistruFlota registru, Clock ceas) {
        this.masini = masini;
        this.pozitii = pozitii;
        this.registru = registru;
        this.ceas = ceas;
    }

    @GetMapping
    public List<MasinaDto> toate() {
        return masini.toate().stream().filter(m -> registru.cunoscuta(m.id())).toList();
    }

    @GetMapping("/{id}")
    public MasinaDto una(@PathVariable String id) {
        return masini.una(id)
                .filter(m -> registru.cunoscuta(id))
                .orElseThrow(() -> new Negasit("mașina " + id + " nu există"));
    }

    @GetMapping("/{id}/traseu")
    public List<PozitieDto> traseu(@PathVariable String id,
                                   @RequestParam(required = false) String de,
                                   @RequestParam(required = false) String pana) {
        una(id);
        Interval interval = Interval.din(de, pana, ceas);
        return pozitii.intre(id, interval.de(), interval.pana());
    }
}
