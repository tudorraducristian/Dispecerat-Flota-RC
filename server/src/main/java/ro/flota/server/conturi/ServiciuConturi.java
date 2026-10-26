package ro.flota.server.conturi;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import ro.flota.server.api.CerereGresita;
import ro.flota.server.api.Conflict;
import ro.flota.server.api.Negasit;
import ro.flota.server.live.CanalLive;

/** Conturile, pentru admin (spec §16.2): creare, ștergere și resetarea parolei altcuiva. */
@Service
public class ServiciuConturi {

    private final Utilizatori utilizatori;
    private final Tokenuri tokenuri;
    private final Parole parole;
    private final CanalLive live;
    private final Clock ceas;

    public ServiciuConturi(Utilizatori utilizatori, Tokenuri tokenuri, Parole parole, CanalLive live, Clock ceas) {
        this.utilizatori = utilizatori;
        this.tokenuri = tokenuri;
        this.parole = parole;
        this.live = live;
        this.ceas = ceas;
    }

    public List<UtilizatorDto> toti() {
        return utilizatori.toti().stream().map(UtilizatorDto::din).toList();
    }

    /** Întâi tot ce se poate verifica fără bază, apoi numele luat. */
    public UtilizatorDto creeaza(CerereCont c) {
        if (c.rol() == null) {
            throw new CerereGresita("rolul e ADMIN sau DISPECER");
        }
        String nume = ReguliCont.normalizeaza(c.nume());
        ReguliCont.verificaNume(nume);
        ReguliCont.verificaParola(c.parola());
        if (utilizatori.dupaNume(nume).isPresent()) {
            throw new Conflict("există deja contul " + nume);
        }
        Instant acum = ceas.instant();
        try {
            long id = utilizatori.adauga(nume, parole.hash(c.parola()), c.rol(), acum);
            return new UtilizatorDto(id, nume, c.rol(), acum);
        } catch (DuplicateKeyException e) {
            // Două cereri cu același nume în aceeași clipă: a doua se lovește de UNIQUE din bază.
            throw new Conflict("există deja contul " + nume);
        }
    }

    /** Token-urile contului dispar odată cu el (ON DELETE CASCADE); canalele lui live se închid. */
    public void sterge(UtilizatorCurent eu, long id) {
        if (id == eu.id()) {
            throw new Conflict("nu-ți poți șterge propriul cont");
        }
        Utilizator u = gaseste(id);
        if (u.rol() == Rol.ADMIN && utilizatori.numarAdmini() <= 1) {
            throw new Conflict("ultimul administrator nu se poate șterge");
        }
        utilizatori.sterge(id);
        live.inchideSesiunileUtilizatorului(id);
    }

    /** Parola nouă, plus omul scos din aplicație: token-urile lui se șterg, canalele se închid. */
    public void reseteazaParola(UtilizatorCurent eu, long id, String noua) {
        if (id == eu.id()) {
            throw new Conflict("parola ta se schimbă din Setări");
        }
        gaseste(id);
        ReguliCont.verificaParola(noua);
        utilizatori.schimbaParola(id, parole.hash(noua));
        tokenuri.stergeAleUtilizatorului(id);
        live.inchideSesiunileUtilizatorului(id);
    }

    private Utilizator gaseste(long id) {
        return utilizatori.dupaId(id).orElseThrow(() -> new Negasit("contul " + id + " nu există"));
    }
}
