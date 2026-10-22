package ro.flota.server.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.comenzi.CerereComanda;
import ro.flota.server.comenzi.ComandaDto;
import ro.flota.server.comenzi.Comenzi;
import ro.flota.server.comenzi.ServiciuComenzi;
import ro.flota.server.date.RegistruFlota;

/** Endpoint-urile 6 și 7 din spec §6.5: comenzile unei mașini. */
@RestController
@RequestMapping("/api/masini/{id}/comenzi")
public class ComenziController {

    private final ServiciuComenzi serviciu;
    private final Comenzi comenzi;
    private final RegistruFlota registru;

    public ComenziController(ServiciuComenzi serviciu, Comenzi comenzi, RegistruFlota registru) {
        this.serviciu = serviciu;
        this.comenzi = comenzi;
        this.registru = registru;
    }

    @PostMapping
    public ComandaDto trimite(@PathVariable String id, @RequestBody CerereComanda cerere) {
        return serviciu.trimite(id, cerere);
    }

    @GetMapping
    public List<ComandaDto> istoric(@PathVariable String id) {
        if (!registru.cunoscuta(id)) {
            throw new Negasit("mașina " + id + " nu există");
        }
        return comenzi.pentruMasina(id);
    }
}
