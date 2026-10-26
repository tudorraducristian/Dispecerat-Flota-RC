package ro.flota.server.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.conturi.InterceptorAutentificare;
import ro.flota.server.conturi.RaspunsLogin;
import ro.flota.server.conturi.Rol;
import ro.flota.server.conturi.ServiciuAutentificare;
import ro.flota.server.conturi.UtilizatorCurent;

/** Endpoint-urile 13–16 din spec §16.2. Doar login-ul merge fără token. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record CerereLogin(String nume, String parola) {
    }

    public record CerereParola(String parolaVeche, String parolaNoua) {
    }

    public record Eu(String nume, Rol rol) {
    }

    private final ServiciuAutentificare serviciu;

    public AuthController(ServiciuAutentificare serviciu) {
        this.serviciu = serviciu;
    }

    @PostMapping("/login")
    public RaspunsLogin login(@RequestBody CerereLogin cerere) {
        return serviciu.login(cerere.nume(), cerere.parola());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestAttribute(InterceptorAutentificare.ATRIBUT) UtilizatorCurent eu) {
        serviciu.logout(eu);
    }

    @GetMapping("/eu")
    public Eu eu(@RequestAttribute(InterceptorAutentificare.ATRIBUT) UtilizatorCurent eu) {
        return new Eu(eu.nume(), eu.rol());
    }

    @PutMapping("/parola")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void parola(@RequestAttribute(InterceptorAutentificare.ATRIBUT) UtilizatorCurent eu,
                       @RequestBody CerereParola cerere) {
        serviciu.schimbaParola(eu, cerere.parolaVeche(), cerere.parolaNoua());
    }
}
