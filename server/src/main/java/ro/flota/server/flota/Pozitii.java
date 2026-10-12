package ro.flota.server.flota;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Tabelul pozitii: o linie pe mesaj de telemetrie. */
@Repository
public class Pozitii {

    private static final String COLOANE =
            "masina_id, seq, ora_masinii, ora_sosirii, lat, lon, viteza, temperatura, baterie, din_buffer";

    private final JdbcClient jdbc;

    public Pozitii(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void salveaza(PozitieDto p) {
        jdbc.sql("INSERT INTO pozitii (" + COLOANE + ") VALUES "
                        + "(:masina, :seq, :oraMasinii, :oraSosirii, :lat, :lon, :viteza, :temperatura, :baterie, :dinBuffer)")
                .param("masina", p.masinaId())
                .param("seq", p.seq())
                .param("oraMasinii", Ore.ts(p.oraMasinii()))
                .param("oraSosirii", Ore.ts(p.oraSosirii()))
                .param("lat", p.lat())
                .param("lon", p.lon())
                .param("viteza", p.viteza())
                .param("temperatura", p.temperatura())
                .param("baterie", p.baterie())
                .param("dinBuffer", p.dinBuffer())
                .update();
    }

    /** Ultima poziție după ora mașinii, nu după ora sosirii: o poziție recuperată nu e „ultima”. */
    public Optional<PozitieDto> ultima(String masinaId) {
        return jdbc.sql("SELECT " + COLOANE + " FROM pozitii WHERE masina_id = :id "
                        + "ORDER BY ora_masinii DESC, seq DESC LIMIT 1")
                .param("id", masinaId)
                .query(Pozitii::rand)
                .optional();
    }

    public List<PozitieDto> intre(String masinaId, Instant de, Instant pana) {
        return jdbc.sql("SELECT " + COLOANE + " FROM pozitii WHERE masina_id = :id "
                        + "AND ora_masinii BETWEEN :de AND :pana ORDER BY ora_masinii, seq")
                .param("id", masinaId)
                .param("de", Ore.ts(de))
                .param("pana", Ore.ts(pana))
                .query(Pozitii::rand)
                .list();
    }

    static PozitieDto rand(ResultSet rs, int nr) throws SQLException {
        return new PozitieDto(rs.getString("masina_id"), rs.getLong("seq"),
                Ore.instant(rs.getTimestamp("ora_masinii")), Ore.instant(rs.getTimestamp("ora_sosirii")),
                rs.getDouble("lat"), rs.getDouble("lon"), rs.getDouble("viteza"), rs.getDouble("temperatura"),
                rs.getInt("baterie"), rs.getBoolean("din_buffer"));
    }
}
