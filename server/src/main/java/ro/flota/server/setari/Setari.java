package ro.flota.server.setari;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Tabelul setari: cheie și valoare, ambele text (spec §16.3). La pornire scrie implicitele care lipsesc, cu
 * INSERT IGNORE, deci o valoare schimbată de admin rămâne. Rulează după PrimulAdmin (@Order(3)).
 */
@Repository
@Order(3)
public class Setari implements ApplicationRunner {

    static final String OFFLINE = "offline_prelungit_secunde";
    static final String TRASEU = "traseu_minute";

    private final JdbcClient jdbc;

    public Setari(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        scrie("INSERT IGNORE INTO setari (cheie, valoare) VALUES (:cheie, :valoare)", SetariDto.IMPLICITE);
    }

    /** O cheie lipsă, de exemplu înainte de prima pornire, își ia valoarea implicită. */
    public SetariDto citeste() {
        Map<String, String> valori = new HashMap<>();
        jdbc.sql("SELECT cheie, valoare FROM setari")
                .query((rs, nr) -> valori.put(rs.getString("cheie"), rs.getString("valoare")))
                .list();
        return new SetariDto(
                numar(valori.get(OFFLINE), SetariDto.IMPLICITE.offlinePrelungitSecunde()),
                numar(valori.get(TRASEU), SetariDto.IMPLICITE.traseuMinute()));
    }

    /** Validează întâi, apoi scrie ambele chei. */
    public SetariDto salveaza(SetariDto s) {
        s.valideaza();
        scrie("""
                INSERT INTO setari (cheie, valoare) VALUES (:cheie, :valoare) AS nou
                ON DUPLICATE KEY UPDATE valoare = nou.valoare""", s);
        return s;
    }

    /** Pragul regulii OFFLINE_PRELUNGIT, citit la fiecare verificare, ca o schimbare să se vadă pe loc. */
    public Duration offlinePrelungit() {
        return Duration.ofSeconds(citeste().offlinePrelungitSecunde());
    }

    private void scrie(String sql, SetariDto s) {
        jdbc.sql(sql).param("cheie", OFFLINE).param("valoare", String.valueOf(s.offlinePrelungitSecunde())).update();
        jdbc.sql(sql).param("cheie", TRASEU).param("valoare", String.valueOf(s.traseuMinute())).update();
    }

    private static int numar(String text, int implicit) {
        try {
            return text == null ? implicit : Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return implicit; // o valoare stricată de mână în bază nu oprește regula
        }
    }
}
