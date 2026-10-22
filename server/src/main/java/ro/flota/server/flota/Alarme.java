package ro.flota.server.flota;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** Tabelul alarme. */
@Repository
public class Alarme {

    private final JdbcClient jdbc;

    public Alarme(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Salvează alarma și întoarce rândul, cu id-ul dat de MySQL. */
    public AlarmaDto salveaza(String masinaId, String tip, String sursa, Instant ora, Instant sosire, String detalii) {
        KeyHolder cheie = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO alarme (masina_id, tip, sursa, ora, ora_sosirii, detalii) "
                        + "VALUES (:masina, :tip, :sursa, :ora, :sosire, :detalii)")
                .param("masina", masinaId)
                .param("tip", tip)
                .param("sursa", sursa)
                .param("ora", Ore.ts(ora))
                .param("sosire", Ore.ts(sosire))
                .param("detalii", detalii)
                .update(cheie);
        return new AlarmaDto(cheie.getKey().longValue(), masinaId, tip, sursa, ora, sosire, detalii, false);
    }

    public List<AlarmaDto> lista(Boolean vazute) {
        String unde = vazute == null ? "" : "WHERE vazuta = :vazute ";
        JdbcClient.StatementSpec cerere = jdbc.sql("SELECT * FROM alarme " + unde + "ORDER BY ora_sosirii DESC, id DESC LIMIT 200");
        if (vazute != null) {
            cerere = cerere.param("vazute", vazute);
        }
        return cerere.query(Alarme::rand).list();
    }

    public Optional<AlarmaDto> marcheazaVazuta(long id) {
        jdbc.sql("UPDATE alarme SET vazuta = TRUE WHERE id = :id").param("id", id).update();
        return jdbc.sql("SELECT * FROM alarme WHERE id = :id").param("id", id).query(Alarme::rand).optional();
    }

    private static AlarmaDto rand(ResultSet rs, int nr) throws SQLException {
        return new AlarmaDto(rs.getLong("id"), rs.getString("masina_id"), rs.getString("tip"), rs.getString("sursa"),
                Ore.instant(rs.getTimestamp("ora")), Ore.instant(rs.getTimestamp("ora_sosirii")),
                rs.getString("detalii"), rs.getBoolean("vazuta"));
    }
}
