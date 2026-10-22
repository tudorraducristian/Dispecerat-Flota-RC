package ro.flota.server.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.flota.AlarmaDto;
import ro.flota.server.flota.Alarme;

/** Endpoint-urile 4 și 5 din spec §6.5. */
@RestController
@RequestMapping("/api/alarme")
public class AlarmeController {

    private final Alarme alarme;

    public AlarmeController(Alarme alarme) {
        this.alarme = alarme;
    }

    @GetMapping
    public List<AlarmaDto> lista(@RequestParam(required = false) Boolean vazute) {
        return alarme.lista(vazute);
    }

    @PatchMapping("/{id}")
    public AlarmaDto vazuta(@PathVariable long id) {
        return alarme.marcheazaVazuta(id).orElseThrow(() -> new Negasit("alarma " + id + " nu există"));
    }
}
