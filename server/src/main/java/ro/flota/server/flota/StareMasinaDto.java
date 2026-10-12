package ro.flota.server.flota;

import java.time.Instant;

/** Ce pleacă pe canalul live când o mașină devine ONLINE sau OFFLINE. */
public record StareMasinaDto(String masinaId, String stare, Instant ultimaVedere) {
}
