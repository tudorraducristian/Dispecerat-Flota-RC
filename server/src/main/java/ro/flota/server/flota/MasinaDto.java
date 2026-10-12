package ro.flota.server.flota;

import java.time.Instant;

/** O mașină din registru, cu ultima poziție știută, ca să apară pe hartă și când e offline (D23). */
public record MasinaDto(String id, String zonaId, String stare, Instant ultimaVedere,
                        long recuperate, long pierdute, PozitieDto ultimaPozitie) {

    MasinaDto cuPozitie(PozitieDto pozitie) {
        return new MasinaDto(id, zonaId, stare, ultimaVedere, recuperate, pierdute, pozitie);
    }
}
