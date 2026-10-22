package ro.flota.server.retea;

import java.time.Instant;
import java.util.List;
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

    public List<EvenimentDto> ultimele(int n) {
        return jdbc.sql("SELECT * FROM evenimente_retea ORDER BY id DESC LIMIT :n")
                .param("n", n)
                .query((rs, nr) -> new EvenimentDto(rs.getLong("id"), rs.getString("tinta"), rs.getString("linie"),
                        rs.getString("raspuns"), Ore.instant(rs.getTimestamp("ora_start")),
                        Ore.instant(rs.getTimestamp("ora_sfarsit"))))
                .list();
    }
}
