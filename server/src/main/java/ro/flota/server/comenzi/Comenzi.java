package ro.flota.server.comenzi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
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

    ComandaDto rand(ResultSet rs, int nr) throws SQLException {
        return new ComandaDto(rs.getLong("id"), rs.getString("masina_id"), rs.getString("tip"),
                json.readTree(rs.getString("parametri")), Ore.instant(rs.getTimestamp("ora_trimiterii")),
                Ore.instant(rs.getTimestamp("ora_confirmarii")), rs.getString("stare"), rs.getString("motiv"));
    }
}
