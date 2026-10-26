package ro.flota.server.conturi;

import java.security.SecureRandom;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * La pornire, dacă nu există niciun ADMIN, face contul „admin” cu o parolă aleatoare, scrisă o singură dată în
 * log (spec §16.2). Astfel nicio parolă de cont nu stă în git. Rulează după IncarcareDate (@Order(2)).
 */
@Component
@Order(2)
public class PrimulAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PrimulAdmin.class);
    private static final String LITERE_SI_CIFRE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int LUNGIME_PAROLA = 16;

    private final Utilizatori utilizatori;
    private final Parole parole;
    private final Clock ceas;
    private final SecureRandom aleator = new SecureRandom();

    public PrimulAdmin(Utilizatori utilizatori, Parole parole, Clock ceas) {
        this.utilizatori = utilizatori;
        this.parole = parole;
        this.ceas = ceas;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (utilizatori.numarAdmini() > 0) {
            return;
        }
        String parola = parolaAleatoare(aleator);
        utilizatori.adauga("admin", parole.hash(parola), Rol.ADMIN, ceas.instant());
        log.warn("Am creat contul admin. Parola, afișată o singură dată: {}. Schimb-o din Setări.", parola);
    }

    /** 16 caractere din 62 de litere și cifre: vreo 95 de biți de întâmplare. */
    static String parolaAleatoare(SecureRandom r) {
        StringBuilder sb = new StringBuilder(LUNGIME_PAROLA);
        for (int i = 0; i < LUNGIME_PAROLA; i++) {
            sb.append(LITERE_SI_CIFRE.charAt(r.nextInt(LITERE_SI_CIFRE.length())));
        }
        return sb.toString();
    }
}
