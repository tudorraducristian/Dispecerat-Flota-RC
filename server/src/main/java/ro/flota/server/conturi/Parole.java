package ro.flota.server.conturi;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Parolele, cu BCrypt (D28). În bază ajunge doar amprenta: din ea nu se poate reface parola, iar la login se
 * compară amprentele. Fiecare hash are sarea lui, deci același text dă de fiecare dată alt hash.
 */
@Component
public class Parole {

    /** Puterea implicită, 10: 2¹⁰ runde, cam o zecime de secundă pe hash, ca ghicitul pe rând să fie scump. */
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public String hash(String parola) {
        return bcrypt.encode(parola);
    }

    public boolean potrivite(String parola, String hash) {
        return bcrypt.matches(parola, hash);
    }
}
