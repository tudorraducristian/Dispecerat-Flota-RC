package ro.flota.server.conturi;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ro.flota.server.api.CerereGresita;
import ro.flota.server.api.Neautorizat;
import ro.flota.server.live.CanalLive;

/**
 * Login-ul, log off-ul și verificarea token-ului (spec §16.2). Token-ul e ca brățara de la festival: o primești
 * o dată, după ce arăți biletul, adică parola, iar apoi se verifică doar brățara.
 */
@Service
public class ServiciuAutentificare {

    /** Cât o zi de laborator; folosirea nu prelungește termenul. */
    static final Duration DURATA_TOKEN = Duration.ofHours(8);
    private static final int OCTETI_TOKEN = 32;
    private static final String LOGIN_GRESIT = "nume sau parolă greșită";

    private final Utilizatori utilizatori;
    private final Tokenuri tokenuri;
    private final Parole parole;
    private final CanalLive live;
    private final Clock ceas;
    private final SecureRandom aleator = new SecureRandom();
    /** Pentru un nume necunoscut se verifică totuși o parolă, ca durata să nu spună ce conturi există. */
    private final String hashFals;

    public ServiciuAutentificare(Utilizatori utilizatori, Tokenuri tokenuri, Parole parole, CanalLive live, Clock ceas) {
        this.utilizatori = utilizatori;
        this.tokenuri = tokenuri;
        this.parole = parole;
        this.live = live;
        this.ceas = ceas;
        this.hashFals = parole.hash("nu-e-parola-nimanui");
    }

    /** Același mesaj pentru nume și pentru parolă: cine încearcă din afară nu află ce conturi există. */
    public RaspunsLogin login(String nume, String parola) {
        Instant acum = ceas.instant();
        tokenuri.stergeExpirate(acum);
        Optional<Utilizator> cont = utilizatori.dupaNume(ReguliCont.normalizeaza(nume));
        String text = parola == null ? "" : parola;
        boolean potrivita = parole.potrivite(text, cont.map(Utilizator::hashParola).orElse(hashFals));
        if (cont.isEmpty() || !potrivita) {
            throw new Neautorizat(LOGIN_GRESIT);
        }
        Utilizator u = cont.get();
        String token = tokenNou(aleator);
        tokenuri.salveaza(token, u.id(), acum, acum.plus(DURATA_TOKEN));
        return new RaspunsLogin(token, u.nume(), u.rol());
    }

    /** Șterge brățara și închide canalele live deschise cu ea. */
    public void logout(UtilizatorCurent eu) {
        tokenuri.sterge(eu.token());
        live.inchideSesiunileTokenului(eu.token());
    }

    public Optional<UtilizatorCurent> valideaza(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return tokenuri.valid(token, ceas.instant());
    }

    public void schimbaParola(UtilizatorCurent eu, String veche, String noua) {
        Utilizator u = utilizatori.dupaId(eu.id()).orElseThrow(() -> new Neautorizat("trebuie să te autentifici"));
        if (veche == null || !parole.potrivite(veche, u.hashParola())) {
            throw new CerereGresita("parola veche e greșită");
        }
        ReguliCont.verificaParola(noua);
        utilizatori.schimbaParola(u.id(), parole.hash(noua));
    }

    /** 32 de octeți la întâmplare, în Base64 URL fără padding: 43 de caractere, din care nu se poate ghici nimic. */
    static String tokenNou(SecureRandom r) {
        byte[] octeti = new byte[OCTETI_TOKEN];
        r.nextBytes(octeti);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octeti);
    }
}
