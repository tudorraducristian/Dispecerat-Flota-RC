package ro.flota.server.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.comenzi.CerereComanda;
import ro.flota.server.comenzi.ComandaDto;
import ro.flota.server.comenzi.ServiciuComenzi;

/** Endpoint-urile 6 și 7 din spec §6.5: comenzile unei mașini. */
@RestController
@RequestMapping("/api/masini/{id}/comenzi")
public class ComenziController {

    private final ServiciuComenzi serviciu;

    public ComenziController(ServiciuComenzi serviciu) {
        this.serviciu = serviciu;
    }

    @PostMapping
    public ComandaDto trimite(@PathVariable String id, @RequestBody CerereComanda cerere) {
        return serviciu.trimite(id, cerere);
    }
}
