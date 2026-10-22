package ro.flota.server.reguli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import ro.flota.server.date.Punct;
import ro.flota.server.date.RegistruFlota;
import ro.flota.server.date.ZonaDefinita;

/**
 * Regulile de alarmă ale serverului, aplicate fiecărei poziții (spec §6.4). Se dau la schimbare (D12): o
 * alarmă când mașina iese din zonă, nu una pe secundă cât stă afară. Ține ultima stare a fiecărei mașini.
 * Rulează doar pe firul MQTT (D22), deci n-are nevoie de synchronized.
 */
@Component
public class ReguliAlarma {

    public record AlarmaNoua(String tip, String detalii) {
    }

    private final RegistruFlota registru;
    private final Map<String, Boolean> inZona = new HashMap<>();

    public ReguliAlarma(RegistruFlota registru) {
        this.registru = registru;
    }

    public List<AlarmaNoua> verifica(String masinaId, Punct p, double viteza) {
        Optional<ZonaDefinita> zona = registru.zonaMasinii(masinaId);
        if (zona.isEmpty()) {
            return List.of();
        }
        ZonaDefinita z = zona.get();
        List<AlarmaNoua> alarme = new ArrayList<>();

        // IESIRE_DIN_ZONA: doar trecerea din zonă în afara ei. O mașină pornită afară nu dă alarmă (D26).
        boolean acumInZona = PunctInPoligon.contine(z.poligon(), p);
        Boolean inainte = inZona.put(masinaId, acumInZona);
        if (Boolean.TRUE.equals(inainte) && !acumInZona) {
            alarme.add(new AlarmaNoua("IESIRE_DIN_ZONA", "a ieșit din zona " + z.nume()));
        }
        return alarme;
    }
}
