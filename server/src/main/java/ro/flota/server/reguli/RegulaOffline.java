package ro.flota.server.reguli;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * OFFLINE_PRELUNGIT (spec §6.4): o mașină OFFLINE de peste prag, dată o singură dată pe episod și rearmată când
 * mașina revine ONLINE. Pragul vine din setări (spec §16.3), implicit 60 de secunde. Starea o scrie firul MQTT
 * și o citește verificarea programată, deci metodele sînt synchronized. Starea stă doar în memorie (D26).
 */
@Component
public class RegulaOffline {

    private final Map<String, Instant> offlineDin = new HashMap<>();
    private final Set<String> alarmate = new HashSet<>();

    public synchronized void stare(String masinaId, boolean online, Instant ora) {
        if (online) {
            offlineDin.remove(masinaId);
            alarmate.remove(masinaId);
        } else {
            offlineDin.putIfAbsent(masinaId, ora);
        }
    }

    /** Mașinile care au trecut acum de prag și n-au primit încă alarma, în ordine alfabetică. */
    public synchronized List<String> deAlarmat(Instant acum, Duration prag) {
        List<String> rezultat = new ArrayList<>();
        for (Map.Entry<String, Instant> e : offlineDin.entrySet()) {
            boolean pestePrag = Duration.between(e.getValue(), acum).compareTo(prag) > 0;
            if (pestePrag && alarmate.add(e.getKey())) {
                rezultat.add(e.getKey());
            }
        }
        rezultat.sort(Comparator.naturalOrder());
        return rezultat;
    }

    public synchronized Instant offlineDin(String masinaId) {
        return offlineDin.get(masinaId);
    }
}
