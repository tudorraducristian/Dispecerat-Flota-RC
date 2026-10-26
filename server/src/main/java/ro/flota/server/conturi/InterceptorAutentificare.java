package ro.flota.server.conturi;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import ro.flota.server.api.Interzis;
import ro.flota.server.api.Neautorizat;

/**
 * Paznicul de pe /api/** (spec §16.2): fără un token valid, 401; pe o metodă @DoarAdmin, fără ADMIN, 403.
 * Excepțiile ajung la EroriApi, deci și ele ies ca JSON {status, mesaj}.
 */
@Component
public class InterceptorAutentificare implements HandlerInterceptor {

    public static final String ATRIBUT = "utilizator";
    private static final String PREFIX = "Bearer ";

    private final ServiciuAutentificare serviciu;

    public InterceptorAutentificare(ServiciuAutentificare serviciu) {
        this.serviciu = serviciu;
    }

    @Override
    public boolean preHandle(HttpServletRequest cerere, HttpServletResponse raspuns, Object handler) {
        if (!(handler instanceof HandlerMethod metoda)) {
            return true; // de exemplu fișiere statice: nu sînt endpoint-uri
        }
        String header = cerere.getHeader("Authorization");
        if (header == null || !header.startsWith(PREFIX)) {
            throw new Neautorizat("trebuie să te autentifici");
        }
        UtilizatorCurent eu = serviciu.valideaza(header.substring(PREFIX.length()).trim())
                .orElseThrow(() -> new Neautorizat("trebuie să te autentifici"));
        if (metoda.hasMethodAnnotation(DoarAdmin.class) && eu.rol() != Rol.ADMIN) {
            throw new Interzis("doar administratorul are voie");
        }
        cerere.setAttribute(ATRIBUT, eu);
        return true;
    }
}
