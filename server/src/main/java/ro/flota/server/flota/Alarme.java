package ro.flota.server.flota;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
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

    /** tip poate fi null, adică toate tipurile. */
    public List<AlarmaDto> lista(Boolean vazute, String tip) {
        List<String> conditii = new ArrayList<>();
        if (vazute != null) {
            conditii.add("vazuta = :vazute");
        }
        if (tip != null) {
            conditii.add("tip = :tip");
        }
        String unde = conditii.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditii) + " ";
        JdbcClient.StatementSpec cerere = jdbc.sql("SELECT * FROM alarme " + unde + "ORDER BY ora_sosirii DESC, id DESC LIMIT 200");
        if (vazute != null) {
            cerere = cerere.param("vazute", vazute);
        }
        if (tip != null) {
            cerere = cerere.param("tip", tip);
        }
        return cerere.query(Alarme::rand).list();
    }

    /** „Azi” începe la deLa, miezul nopții românesc, calculat de controller. */
    public NumarAlarme numar(Instant deLa) {
        // Pe un tabel gol, SUM dă NULL, nu 0.
        return jdbc.sql("SELECT COALESCE(SUM(vazuta = FALSE), 0) AS nevazute, "
                        + "COALESCE(SUM(ora_sosirii >= :deLa), 0) AS azi FROM alarme")
                .param("deLa", Ore.ts(deLa))
                .query((rs, nr) -> new NumarAlarme(rs.getLong("nevazute"), rs.getLong("azi")))
                .single();
    }

    /** Întoarce câte alarme a marcat. tip poate fi null, adică toate tipurile. */
    public int marcheazaToateVazute(String tip) {
        if (tip == null) {
            return jdbc.sql("UPDATE alarme SET vazuta = TRUE WHERE vazuta = FALSE").update();
        }
        return jdbc.sql("UPDATE alarme SET vazuta = TRUE WHERE vazuta = FALSE AND tip = :tip").param("tip", tip).update();
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
