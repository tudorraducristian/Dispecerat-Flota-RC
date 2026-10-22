package ro.flota.server.retea;

import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ro.flota.server.flota.Ore;

/** Tabelul evenimente_retea: jurnalul vremii trimise proxy-ului. */
@Repository
public class EvenimenteRetea {

    private final JdbcClient jdbc;

    public EvenimenteRetea(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public EvenimentDto salveaza(String tinta, String linie, String raspuns, Instant start, Instant sfarsit) {
        KeyHolder cheie = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO evenimente_retea (tinta, linie, raspuns, ora_start, ora_sfarsit) "
                        + "VALUES (:tinta, :linie, :raspuns, :start, :sfarsit)")
                .param("tinta", tinta)
                .param("linie", linie)
                .param("raspuns", raspuns)
                .param("start", Ore.ts(start))
                .param("sfarsit", Ore.ts(sfarsit))
                .update(cheie);
        return new EvenimentDto(cheie.getKey().longValue(), tinta, linie, raspuns, start, sfarsit);
    }
}
