package ro.flota.simulator.mesaje;

import java.nio.charset.StandardCharsets;

/** Un mesaj gata de trimis: unde merge, ce conține și cum se livrează. */
public record Mesaj(TipMesaj tip, String topic, byte[] payload, int qos, boolean retain) {

    public String text() {
        return new String(payload, StandardCharsets.UTF_8);
    }
}
