package ro.flota.server.flota;

import java.time.Instant;
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
}
