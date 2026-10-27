package ro.flota.server.api;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.flota.AlarmaDto;
import ro.flota.server.flota.Alarme;
import ro.flota.server.flota.NumarAlarme;
import ro.flota.server.flota.TipuriAlarma;

/** Endpoint-urile 4 și 5 din spec §6.5, plus numărul, „toate văzute” și filtrul după tip (redesign §5). */
@RestController
@RequestMapping("/api/alarme")
public class AlarmeController {

    private static final ZoneId ROMANIA = ZoneId.of("Europe/Bucharest");

    private final Alarme alarme;
    private final Clock ceas;

    public AlarmeController(Alarme alarme, Clock ceas) {
        this.alarme = alarme;
        this.ceas = ceas;
    }

    /** Miezul nopții, ora României, pentru „azi”. Baza e pe UTC, iar dispecerul numără după ceasul lui. */
    static Instant inceputulZilei(Instant acum) {
        return acum.atZone(ROMANIA).toLocalDate().atStartOfDay(ROMANIA).toInstant();
    }

    @GetMapping
    public List<AlarmaDto> lista(@RequestParam(required = false) Boolean vazute,
                                 @RequestParam(required = false) String tip) {
        return alarme.lista(vazute, tipCunoscut(tip));
    }

    @GetMapping("/numar")
    public NumarAlarme numar() {
        return alarme.numar(inceputulZilei(ceas.instant()));
    }

    @PostMapping("/vazute")
    public Map<String, Integer> vazute(@RequestParam(required = false) String tip) {
        return Map.of("marcate", alarme.marcheazaToateVazute(tipCunoscut(tip)));
    }

    @PatchMapping("/{id}")
    public AlarmaDto vazuta(@PathVariable long id) {
        return alarme.marcheazaVazuta(id).orElseThrow(() -> new Negasit("alarma " + id + " nu există"));
    }

    private static String tipCunoscut(String tip) {
        if (tip != null && !TipuriAlarma.TOATE.contains(tip)) {
            throw new CerereGresita("tip de alarmă necunoscut: " + tip);
        }
        return tip;
    }
}
