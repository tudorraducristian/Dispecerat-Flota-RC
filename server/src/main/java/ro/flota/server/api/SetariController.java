package ro.flota.server.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.conturi.DoarAdmin;
import ro.flota.server.setari.Setari;
import ro.flota.server.setari.SetariDto;

/** Endpoint-urile 21 și 22 din spec §16.2: oricine logat le vede, doar adminul le schimbă. */
@RestController
@RequestMapping("/api/setari")
public class SetariController {

    private final Setari setari;

    public SetariController(Setari setari) {
        this.setari = setari;
    }

    @GetMapping
    public SetariDto citeste() {
        return setari.citeste();
    }

    @PutMapping
    @DoarAdmin
    public SetariDto salveaza(@RequestBody SetariDto s) {
        return setari.salveaza(s);
    }
}
