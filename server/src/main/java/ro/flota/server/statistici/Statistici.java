package ro.flota.server.statistici;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import ro.flota.server.date.RegistruFlota;
import ro.flota.server.flota.ContoareDto;
import ro.flota.server.flota.Masini;

/**
 * Ferestrele tuturor mașinilor. Scrie firul MQTT, iar citesc firul programat și cererile REST, deci
 * accesul la ferestre e synchronized. Interogarea bazei stă în afara lacătului.
 */
@Component
public class Statistici {

    private final Masini masini;
    private final RegistruFlota registru;
    private final Clock ceas;
    private final Map<String, FereastraStatistici> ferestre = new HashMap<>();

    public Statistici(Masini masini, RegistruFlota registru, Clock ceas) {
        this.masini = masini;
        this.registru = registru;
        this.ceas = ceas;
    }

    public synchronized void mesaj(String masinaId, Instant oraMasinii, Instant oraSosirii, boolean dinBuffer) {
        ferestre.computeIfAbsent(masinaId, id -> new FereastraStatistici()).mesaj(oraMasinii, oraSosirii, dinBuffer);
    }

    public List<StatisticaDto> acum() {
        Instant acum = ceas.instant();
        return masini.contoare().stream()
                .filter(c -> registru.cunoscuta(c.id())) // doar flota de acum, nu și mașinile scoase din flota.json
                .map(c -> statistica(c, acum))
                .toList();
    }

    private synchronized StatisticaDto statistica(ContoareDto c, Instant acum) {
        FereastraStatistici f = ferestre.get(c.id());
        return new StatisticaDto(c.id(), f == null ? 0 : f.mesajePeSecunda(acum),
                f == null ? null : f.latentaMedieMs(), c.recuperate(), c.pierdute());
    }
}
