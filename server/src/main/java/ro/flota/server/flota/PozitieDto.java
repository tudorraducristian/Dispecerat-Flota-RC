package ro.flota.server.flota;

import java.time.Instant;
import ro.flota.server.mqtt.MesajPrimit;

/** O poziție, așa cum o văd baza și UI-ul: are ambele ore, a mașinii și a sosirii (spec §4). */
public record PozitieDto(String masinaId, long seq, Instant oraMasinii, Instant oraSosirii, double lat, double lon,
                         double viteza, double temperatura, int baterie, boolean dinBuffer) {

    public static PozitieDto din(MesajPrimit.Telemetrie t, Instant sosire) {
        return new PozitieDto(t.masinaId(), t.seq(), t.oraMasinii(), sosire, t.lat(), t.lon(),
                t.viteza(), t.temperatura(), t.baterie(), t.dinBuffer());
    }
}
