package ro.flota.server.flota;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Tabelul masini: starea și contoarele fiecărei mașini. Rândurile le creează IncarcareDate. */
@Repository
public class Masini {

    private final JdbcClient jdbc;
    private final Pozitii pozitii;

    public Masini(JdbcClient jdbc, Pozitii pozitii) {
        this.jdbc = jdbc;
        this.pozitii = pozitii;
    }

    public void vazuta(String id, Instant ora) {
        jdbc.sql("UPDATE masini SET ultima_vedere = :ora WHERE id = :id")
                .param("ora", Ore.ts(ora))
                .param("id", id)
                .update();
    }

    public void schimbaStare(String id, boolean online, Instant ora) {
        jdbc.sql("UPDATE masini SET stare = :stare, ultima_vedere = :ora WHERE id = :id")
                .param("stare", online ? "ONLINE" : "OFFLINE")
                .param("ora", Ore.ts(ora))
                .param("id", id)
                .update();
    }

    /** În ordinea numerelor: masina-2 înainte de masina-10. „masina-” are 7 caractere. */
    public List<MasinaDto> toate() {
        return jdbc.sql("SELECT * FROM masini ORDER BY CAST(SUBSTRING(id, 8) AS UNSIGNED)")
                .query(Masini::rand)
                .list()
                .stream()
                .map(m -> m.cuPozitie(pozitii.ultima(m.id()).orElse(null)))
                .toList();
    }

    public Optional<MasinaDto> una(String id) {
        return jdbc.sql("SELECT * FROM masini WHERE id = :id")
                .param("id", id)
                .query(Masini::rand)
                .optional()
                .map(m -> m.cuPozitie(pozitii.ultima(id).orElse(null)));
    }

    private static MasinaDto rand(ResultSet rs, int nr) throws SQLException {
        return new MasinaDto(rs.getString("id"), rs.getString("zona_id"), rs.getString("stare"),
                Ore.instant(rs.getTimestamp("ultima_vedere")), rs.getLong("recuperate"), rs.getLong("pierdute"), null);
    }
}
