package ro.flota.simulator.mesaje;

import java.nio.charset.StandardCharsets;
import ro.flota.simulator.Json;

/** Locul unde se hotărăște, pentru fiecare fel de mesaj, topic-ul, QoS-ul și reținerea (spec §4.1). */
public final class Mesaje {

    private Mesaje() {
    }

    public static String topic(String idMasina, String canal) {
        return "flota/" + idMasina + "/" + canal;
    }

    /** QoS 0: vine o dată pe secundă, iar dacă una se pierde, următoarea o înlocuiește. */
    public static Mesaj telemetrie(String idMasina, Telemetrie t) {
        return new Mesaj(TipMesaj.TELEMETRIE, topic(idMasina, "telemetrie"), Json.MAPPER.writeValueAsBytes(t), 0, false);
    }

    /** QoS 2: e rară și gravă, deci nu are voie nici să se piardă, nici să fie numărată de două ori. */
    public static Mesaj alarma(String idMasina, Alarma a) {
        return new Mesaj(TipMesaj.ALARMA, topic(idMasina, "alarma"), Json.MAPPER.writeValueAsBytes(a), 2, false);
    }

    /** QoS 1 și reținut: cine se abonează mai târziu află starea pe loc. */
    public static Mesaj stare(String idMasina, boolean online) {
        byte[] text = (online ? "online" : "offline").getBytes(StandardCharsets.UTF_8);
        return new Mesaj(TipMesaj.STARE, topic(idMasina, "stare"), text, 1, true);
    }
}
