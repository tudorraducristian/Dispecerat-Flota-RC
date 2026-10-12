package ro.flota.simulator.mesaje;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Ce trimite mașina o dată pe secundă pe flota/<id>/telemetrie (spec §4.2). */
public record Telemetrie(
        long seq,
        String ora,
        double lat,
        double lon,
        double viteza,
        double temperatura,
        int baterie,
        @JsonProperty("din_buffer") boolean dinBuffer) {
}
