package ro.flota.server.conturi;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ro.flota.server.flota.Ore;

/** Tabelul tokenuri: brățările date la login. Un token e valid cât timp n-a expirat și contul există. */
@Repository
public class Tokenuri {

    private final JdbcClient jdbc;

    public Tokenuri(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void salveaza(String token, long utilizatorId, Instant creatLa, Instant expiraLa) {
        jdbc.sql("""
                        INSERT INTO tokenuri (token, utilizator_id, creat_la, expira_la)
                        VALUES (:token, :utilizator, :creatLa, :expiraLa)""")
                .param("token", token)
                .param("utilizator", utilizatorId)
                .param("creatLa", Ore.ts(creatLa))
                .param("expiraLa", Ore.ts(expiraLa))
                .update();
    }

    /** Cine ține token-ul, dacă e încă valabil. Rolul vine din contul de acum, nu din ziua login-ului. */
    public Optional<UtilizatorCurent> valid(String token, Instant acum) {
        return jdbc.sql("""
                        SELECT u.id, u.nume, u.rol FROM tokenuri t JOIN utilizatori u ON u.id = t.utilizator_id
                        WHERE t.token = :token AND t.expira_la > :acum""")
                .param("token", token)
                .param("acum", Ore.ts(acum))
                .query((rs, nr) -> new UtilizatorCurent(rs.getLong("id"), rs.getString("nume"),
                        Rol.valueOf(rs.getString("rol")), token))
                .optional();
    }

    public void sterge(String token) {
        jdbc.sql("DELETE FROM tokenuri WHERE token = :token").param("token", token).update();
    }

    public void stergeAleUtilizatorului(long id) {
        jdbc.sql("DELETE FROM tokenuri WHERE utilizator_id = :id").param("id", id).update();
    }

    /** Cele expirate, ca CanalLive să le închidă canalele înainte să dispară din bază (Task 4). */
    public List<String> expirate(Instant acum) {
        return jdbc.sql("SELECT token FROM tokenuri WHERE expira_la <= :acum")
                .param("acum", Ore.ts(acum))
                .query(String.class)
                .list();
    }

    public void stergeExpirate(Instant acum) {
        jdbc.sql("DELETE FROM tokenuri WHERE expira_la <= :acum").param("acum", Ore.ts(acum)).update();
    }
}
