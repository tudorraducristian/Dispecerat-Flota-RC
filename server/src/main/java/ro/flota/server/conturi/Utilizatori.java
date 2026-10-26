package ro.flota.server.conturi;

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

/** Tabelul utilizatori, cu SQL scris de mână (D19). */
@Repository
public class Utilizatori {

    private static final String COLOANE = "SELECT id, nume, hash_parola, rol, creat_la FROM utilizatori";

    private final JdbcClient jdbc;

    public Utilizatori(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Utilizator> dupaNume(String nume) {
        return jdbc.sql(COLOANE + " WHERE nume = :nume").param("nume", nume).query(Utilizatori::rand).optional();
    }

    public Optional<Utilizator> dupaId(long id) {
        return jdbc.sql(COLOANE + " WHERE id = :id").param("id", id).query(Utilizatori::rand).optional();
    }

    public List<Utilizator> toti() {
        return jdbc.sql(COLOANE + " ORDER BY id").query(Utilizatori::rand).list();
    }

    /** Întoarce id-ul dat de MySQL. */
    public long adauga(String nume, String hash, Rol rol, Instant creatLa) {
        KeyHolder cheie = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO utilizatori (nume, hash_parola, rol, creat_la) VALUES (:nume, :hash, :rol, :creatLa)")
                .param("nume", nume)
                .param("hash", hash)
                .param("rol", rol.name())
                .param("creatLa", Ore.ts(creatLa))
                .update(cheie);
        return cheie.getKey().longValue();
    }

    public void schimbaParola(long id, String hash) {
        jdbc.sql("UPDATE utilizatori SET hash_parola = :hash WHERE id = :id").param("hash", hash).param("id", id).update();
    }

    /** Token-urile contului se șterg odată cu el, prin ON DELETE CASCADE. */
    public void sterge(long id) {
        jdbc.sql("DELETE FROM utilizatori WHERE id = :id").param("id", id).update();
    }

    public int numarAdmini() {
        return jdbc.sql("SELECT COUNT(*) FROM utilizatori WHERE rol = 'ADMIN'").query(Integer.class).single();
    }

    private static Utilizator rand(ResultSet rs, int nr) throws SQLException {
        return new Utilizator(rs.getLong("id"), rs.getString("nume"), rs.getString("hash_parola"),
                Rol.valueOf(rs.getString("rol")), Ore.instant(rs.getTimestamp("creat_la")));
    }
}
