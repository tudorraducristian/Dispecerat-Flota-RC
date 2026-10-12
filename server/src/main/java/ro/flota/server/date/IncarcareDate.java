package ro.flota.server.date;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * La pornire: citește zone.json și flota.json, le scrie în bază și umple registrul.
 * Rulează primul (@Order(1)), ca mesajele MQTT să găsească mașinile în bază.
 * O greșeală în date oprește serverul, cu fișierul și motivul.
 */
@Component
@Order(1)
public class IncarcareDate implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IncarcareDate.class);

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final RegistruFlota registru;
    private final Path dosar;

    public IncarcareDate(JdbcClient jdbc, JsonMapper json, RegistruFlota registru,
                         @Value("${flota.dosar-date}") String dosar) {
        this.jdbc = jdbc;
        this.json = json;
        this.registru = registru;
        this.dosar = Path.of(dosar);
    }

    @Override
    public void run(ApplicationArguments args) {
        CititorDate cititor = new CititorDate(json);
        List<ZonaDefinita> zone = cititor.zone(dosar.resolve("zone.json"));
        Set<String> iduriZone = zone.stream().map(ZonaDefinita::id).collect(Collectors.toSet());
        List<MasinaDefinita> masini = cititor.flota(dosar.resolve("flota.json"), iduriZone);

        // „AS nou ... ON DUPLICATE KEY UPDATE”: dacă rândul există, se actualizează în loc să se dubleze.
        for (ZonaDefinita z : zone) {
            jdbc.sql("""
                    INSERT INTO zone (id, nume, poligon, viteza_maxima) VALUES (:id, :nume, :poligon, :viteza) AS nou
                    ON DUPLICATE KEY UPDATE nume = nou.nume, poligon = nou.poligon, viteza_maxima = nou.viteza_maxima""")
                    .param("id", z.id())
                    .param("nume", z.nume())
                    .param("poligon", json.writeValueAsString(z.poligon().stream().map(p -> List.of(p.lat(), p.lon())).toList()))
                    .param("viteza", z.vitezaMaxima())
                    .update();
        }
        // La mașini se schimbă doar zona; starea și contoarele rămân cum erau.
        for (MasinaDefinita m : masini) {
            jdbc.sql("""
                    INSERT INTO masini (id, zona_id) VALUES (:id, :zona) AS nou
                    ON DUPLICATE KEY UPDATE zona_id = nou.zona_id""")
                    .param("id", m.id())
                    .param("zona", m.zona())
                    .update();
        }
        registru.incarca(zone, masini);
        log.info("Am încărcat {} zone și {} mașini din {}", zone.size(), masini.size(), dosar.toAbsolutePath().normalize());
    }
}
