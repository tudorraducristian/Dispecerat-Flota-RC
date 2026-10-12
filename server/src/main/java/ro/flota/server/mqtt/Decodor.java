package ro.flota.server.mqtt;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;
import ro.flota.server.date.RegistruFlota;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Transformă un mesaj MQTT, adică topic plus octeți, într-un MesajPrimit verificat.
 * Tot ce nu se potrivește aruncă MesajInvalid, cu motivul: serverul nu crede nimic din rețea până nu verifică.
 */
@Component
public class Decodor {

    private final JsonMapper json;
    private final RegistruFlota registru;

    public Decodor(JsonMapper json, RegistruFlota registru) {
        this.json = json;
        this.registru = registru;
    }

    public MesajPrimit decodeaza(String topic, byte[] payload) throws MesajInvalid {
        String[] parti = topic.split("/");
        if (parti.length != 3 || !parti[0].equals("flota")) {
            throw new MesajInvalid("topic necunoscut: " + topic);
        }
        String id = parti[1];
        if (!registru.cunoscuta(id)) {
            throw new MesajInvalid("mașină necunoscută: " + id);
        }
        return switch (parti[2]) {
            case "telemetrie" -> telemetrie(id, obiect(payload));
            default -> throw new MesajInvalid("canal necunoscut: " + parti[2]);
        };
    }

    private MesajPrimit.Telemetrie telemetrie(String id, JsonNode j) throws MesajInvalid {
        return new MesajPrimit.Telemetrie(id, intreg(j, "seq"), ora(j, "ora"), numar(j, "lat"), numar(j, "lon"),
                numar(j, "viteza"), numar(j, "temperatura"), (int) intreg(j, "baterie"), adevarat(j, "din_buffer"));
    }

    private JsonNode obiect(byte[] payload) throws MesajInvalid {
        JsonNode j;
        try {
            j = json.readTree(payload);
        } catch (JacksonException e) {
            throw new MesajInvalid("nu e JSON valid");
        }
        if (j == null || !j.isObject()) {
            throw new MesajInvalid("nu e un obiect JSON");
        }
        return j;
    }

    private static double numar(JsonNode j, String camp) throws MesajInvalid {
        JsonNode v = j.get(camp);
        if (v == null || !v.isNumber()) {
            throw new MesajInvalid("lipsește câmpul numeric " + camp);
        }
        return v.asDouble();
    }

    private static long intreg(JsonNode j, String camp) throws MesajInvalid {
        JsonNode v = j.get(camp);
        if (v == null || !v.isIntegralNumber()) {
            throw new MesajInvalid("lipsește câmpul întreg " + camp);
        }
        return v.asLong();
    }

    private static boolean adevarat(JsonNode j, String camp) throws MesajInvalid {
        JsonNode v = j.get(camp);
        if (v == null || !v.isBoolean()) {
            throw new MesajInvalid("lipsește câmpul true/false " + camp);
        }
        return v.asBoolean();
    }

    private static String text(JsonNode j, String camp) throws MesajInvalid {
        JsonNode v = j.get(camp);
        if (v == null || !v.isString()) {
            throw new MesajInvalid("lipsește câmpul text " + camp);
        }
        return v.asString();
    }

    private static Instant ora(JsonNode j, String camp) throws MesajInvalid {
        String t = text(j, camp);
        try {
            return Instant.parse(t);
        } catch (DateTimeParseException e) {
            throw new MesajInvalid(camp + " nu e o oră ISO-8601: " + t);
        }
    }
}
