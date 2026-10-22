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

    /** Ultimul număr de ordine văzut, sau null dacă mașina n-a trimis încă telemetrie (D25). */
    public Long ultimulSeq(String id) {
        List<Long> rezultat = jdbc.sql("SELECT ultimul_seq FROM masini WHERE id = :id")
                .param("id", id)
                .query((rs, nr) -> rs.getObject("ultimul_seq", Long.class))
                .list();
        return rezultat.isEmpty() ? null : rezultat.get(0);
    }

    /** Contoarele după o telemetrie, într-un singur UPDATE (spec §6.3). */
    public void dupaTelemetrie(String id, long seq, long pierdute, boolean recuperata, Instant ora) {
        jdbc.sql("""
                        UPDATE masini SET ultimul_seq = :seq, pierdute = pierdute + :pierdute,
                                          recuperate = recuperate + :recuperata, ultima_vedere = :ora
                        WHERE id = :id""")
                .param("seq", seq)
                .param("pierdute", pierdute)
                .param("recuperata", recuperata ? 1 : 0)
                .param("ora", Ore.ts(ora))
                .param("id", id)
                .update();
    }

    /**
     * Schimbă starea doar dacă e alta decât cea din bază și spune dacă a schimbat-o. Starea e un mesaj reținut,
     * deci broker-ul o trimite din nou la fiecare abonare; repetată, nu mută „văzută ultima dată” (D23).
     */
    public boolean schimbaStare(String id, boolean online, Instant ora) {
        return jdbc.sql("UPDATE masini SET stare = :stare, ultima_vedere = :ora WHERE id = :id AND stare <> :stare")
                .param("stare", online ? "ONLINE" : "OFFLINE")
                .param("ora", Ore.ts(ora))
                .param("id", id)
                .update() == 1;
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
