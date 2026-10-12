package ro.flota.server.flota;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Tabelul zone, doar de citit (D13). */
@Repository
public class Zone {

    private static final TypeReference<List<List<Double>>> POLIGON = new TypeReference<>() {
    };

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public Zone(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<ZonaDto> toate() {
        return jdbc.sql("SELECT id, nume, poligon, viteza_maxima FROM zone ORDER BY id")
                .query((rs, nr) -> new ZonaDto(rs.getString("id"), rs.getString("nume"),
                        json.readValue(rs.getString("poligon"), POLIGON), rs.getInt("viteza_maxima")))
                .list();
    }
}
