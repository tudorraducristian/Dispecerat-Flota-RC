package ro.flota.server.comenzi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ro.flota.server.flota.Ore;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Tabelul comenzi. id-ul dat de MySQL e și id-ul din mesajul trimis mașinii (spec §4.2). */
@Repository
public class Comenzi {

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public Comenzi(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public ComandaDto salveaza(String masinaId, String tip, JsonNode parametri, Instant ora) {
        KeyHolder cheie = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO comenzi (masina_id, tip, parametri, ora_trimiterii, stare) "
                        + "VALUES (:masina, :tip, :parametri, :ora, 'TRIMISA')")
                .param("masina", masinaId)
                .param("tip", tip)
                .param("parametri", json.writeValueAsString(parametri))
                .param("ora", Ore.ts(ora))
                .update(cheie);
        return new ComandaDto(cheie.getKey().longValue(), masinaId, tip, parametri, ora, null, "TRIMISA", null);
    }

    public List<ComandaDto> pentruMasina(String masinaId) {
        return jdbc.sql("SELECT * FROM comenzi WHERE masina_id = :masina ORDER BY id DESC LIMIT 50")
                .param("masina", masinaId)
                .query(this::rand)
                .list();
    }

    /**
     * Comanda trece în CONFIRMATA sau RESPINSA doar dacă era TRIMISA (spec §6.3). A doua confirmare
     * nu mai găsește niciun rând de schimbat: asta e idempotența, garantată chiar de MySQL.
     */
    public boolean confirmaDacaTrimisa(long id, String masinaId, String stare, Instant ora, String motiv) {
        int schimbate = jdbc.sql("""
                        UPDATE comenzi SET stare = :stare, ora_confirmarii = :ora, motiv = :motiv
                        WHERE id = :id AND masina_id = :masina AND stare = 'TRIMISA'""")
                .param("stare", stare)
                .param("ora", Ore.ts(ora))
                .param("motiv", motiv)
                .param("id", id)
                .param("masina", masinaId)
                .update();
        return schimbate == 1;
    }

    public Optional<ComandaDto> una(long id) {
        return jdbc.sql("SELECT * FROM comenzi WHERE id = :id").param("id", id).query(this::rand).optional();
    }

    ComandaDto rand(ResultSet rs, int nr) throws SQLException {
        return new ComandaDto(rs.getLong("id"), rs.getString("masina_id"), rs.getString("tip"),
                json.readTree(rs.getString("parametri")), Ore.instant(rs.getTimestamp("ora_trimiterii")),
                Ore.instant(rs.getTimestamp("ora_confirmarii")), rs.getString("stare"), rs.getString("motiv"));
    }
}
