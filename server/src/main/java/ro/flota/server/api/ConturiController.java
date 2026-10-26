package ro.flota.server.api;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.conturi.CerereCont;
import ro.flota.server.conturi.DoarAdmin;
import ro.flota.server.conturi.InterceptorAutentificare;
import ro.flota.server.conturi.ServiciuConturi;
import ro.flota.server.conturi.UtilizatorCurent;
import ro.flota.server.conturi.UtilizatorDto;

/** Endpoint-urile 17–20 din spec §16.2, toate doar pentru ADMIN. */
@RestController
@RequestMapping("/api/utilizatori")
public class ConturiController {

    public record CerereParolaNoua(String parolaNoua) {
    }

    private final ServiciuConturi serviciu;

    public ConturiController(ServiciuConturi serviciu) {
        this.serviciu = serviciu;
    }

    @GetMapping
    @DoarAdmin
    public List<UtilizatorDto> toti() {
        return serviciu.toti();
    }

    @PostMapping
    @DoarAdmin
    @ResponseStatus(HttpStatus.CREATED)
    public UtilizatorDto creeaza(@RequestBody CerereCont cerere) {
        return serviciu.creeaza(cerere);
    }

    @DeleteMapping("/{id}")
    @DoarAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sterge(@RequestAttribute(InterceptorAutentificare.ATRIBUT) UtilizatorCurent eu, @PathVariable long id) {
        serviciu.sterge(eu, id);
    }

    @PutMapping("/{id}/parola")
    @DoarAdmin
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reseteazaParola(@RequestAttribute(InterceptorAutentificare.ATRIBUT) UtilizatorCurent eu,
                                @PathVariable long id, @RequestBody CerereParolaNoua cerere) {
        serviciu.reseteazaParola(eu, id, cerere.parolaNoua());
    }
}
