package ro.flota.server.date;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Ce știe serverul despre flotă, în memorie: ce mașini există și în ce zonă e fiecare.
 * Se umple o dată, la pornire; după aceea doar se citește, din mai multe fire.
 */
@Component
public class RegistruFlota {

    private volatile Map<String, MasinaDefinita> masini = Map.of();
    private volatile Map<String, ZonaDefinita> zone = Map.of();

    public void incarca(List<ZonaDefinita> zoneNoi, List<MasinaDefinita> masiniNoi) {
        Map<String, ZonaDefinita> z = new LinkedHashMap<>();
        zoneNoi.forEach(zona -> z.put(zona.id(), zona));
        Map<String, MasinaDefinita> m = new LinkedHashMap<>();
        masiniNoi.forEach(masina -> m.put(masina.id(), masina));
        this.zone = Map.copyOf(z);
        this.masini = Map.copyOf(m);
    }

    public boolean cunoscuta(String id) {
        return masini.containsKey(id);
    }

    public Set<String> masini() {
        return masini.keySet();
    }

    public Optional<ZonaDefinita> zonaMasinii(String id) {
        MasinaDefinita m = masini.get(id);
        return m == null ? Optional.empty() : Optional.ofNullable(zone.get(m.zona()));
    }
}
