package ro.flota.server.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.server.date.MasinaDefinita;
import ro.flota.server.date.RegistruFlota;
import tools.jackson.databind.json.JsonMapper;

class DecodorTest {

    private static final String TELEMETRIE = """
            {"seq": 1042, "ora": "2026-12-10T10:15:03.120Z", "lat": 46.77121, "lon": 23.6236,
             "viteza": 47.5, "temperatura": 92.1, "baterie": 81, "din_buffer": false}""";

    private final Decodor decodor;

    DecodorTest() {
        RegistruFlota registru = new RegistruFlota();
        registru.incarca(List.of(), List.of(new MasinaDefinita("masina-3", "z1")));
        decodor = new Decodor(JsonMapper.builder().build(), registru);
    }

    private MesajPrimit decodeaza(String topic, String payload) throws MesajInvalid {
        return decodor.decodeaza(topic, payload.getBytes(StandardCharsets.UTF_8));
    }

    private String motiv(String topic, String payload) {
        return assertThrows(MesajInvalid.class, () -> decodeaza(topic, payload)).getMessage();
    }

    @Test
    void telemetriaBunaDevineUnRecord() throws MesajInvalid {
        assertEquals(new MesajPrimit.Telemetrie("masina-3", 1042, Instant.parse("2026-12-10T10:15:03.120Z"),
                        46.77121, 23.6236, 47.5, 92.1, 81, false),
                decodeaza("flota/masina-3/telemetrie", TELEMETRIE));
    }

    @Test
    void jsonStricatEsteRefuzat() {
        assertEquals("nu e JSON valid", motiv("flota/masina-3/telemetrie", "{\"seq\": 1"));
    }

    @Test
    void textulSimpluNuEJson() {
        assertEquals("nu e JSON valid", motiv("flota/masina-3/telemetrie", "nu e json"));
    }

    @Test
    void campulLipsaEstePusPeNume() {
        String mesaj = motiv("flota/masina-3/telemetrie", TELEMETRIE.replace("\"viteza\": 47.5,", ""));
        assertTrue(mesaj.contains("viteza"), mesaj);
    }

    @Test
    void unNumarScrisCaTextEsteRefuzat() {
        String mesaj = motiv("flota/masina-3/telemetrie", TELEMETRIE.replace("\"seq\": 1042", "\"seq\": \"1042\""));
        assertTrue(mesaj.contains("seq"), mesaj);
    }

    @Test
    void oraGresitaEsteRefuzata() {
        String mesaj = motiv("flota/masina-3/telemetrie", TELEMETRIE.replace("2026-12-10T10:15:03.120Z", "ieri"));
        assertTrue(mesaj.contains("ISO-8601"), mesaj);
    }

    @Test
    void masinaNecunoscutaEsteRefuzata() {
        assertEquals("mașină necunoscută: masina-99", motiv("flota/masina-99/telemetrie", TELEMETRIE));
    }

    @Test
    void topicStrainEsteRefuzat() {
        assertEquals("topic necunoscut: test/salut", motiv("test/salut", "merge"));
    }

    @Test
    void canalNecunoscutEsteRefuzat() {
        assertEquals("canal necunoscut: radio", motiv("flota/masina-3/radio", "{}"));
    }

    @Test
    void alarmaBunaDevineUnRecord() throws MesajInvalid {
        assertEquals(new MesajPrimit.Alarma("masina-3", "SUPRAINCALZIRE", Instant.parse("2026-12-10T10:15:03.120Z"), 112.4),
                decodeaza("flota/masina-3/alarma",
                        "{\"tip\": \"SUPRAINCALZIRE\", \"ora\": \"2026-12-10T10:15:03.120Z\", \"valoare\": 112.4}"));
    }

    @Test
    void tipulDeAlarmaNecunoscutEsteRefuzat() {
        String mesaj = motiv("flota/masina-3/alarma",
                "{\"tip\": \"INCENDIU\", \"ora\": \"2026-12-10T10:15:03.120Z\", \"valoare\": 1}");
        assertTrue(mesaj.contains("INCENDIU"), mesaj);
    }

    @Test
    void stareaOnlineSiOffline() throws MesajInvalid {
        assertEquals(new MesajPrimit.Stare("masina-3", true), decodeaza("flota/masina-3/stare", "online"));
        assertEquals(new MesajPrimit.Stare("masina-3", false), decodeaza("flota/masina-3/stare", "offline"));
    }

    @Test
    void stareaGoalaEsteRefuzata() {
        assertTrue(motiv("flota/masina-3/stare", "").contains("stare necunoscută"));
    }

    @Test
    void confirmareaExecutata() throws MesajInvalid {
        assertEquals(new MesajPrimit.Confirmare("masina-3", 57, Instant.parse("2026-12-10T10:15:04.002Z"), "EXECUTATA", null),
                decodeaza("flota/masina-3/confirmari",
                        "{\"comanda_id\": 57, \"ora\": \"2026-12-10T10:15:04.002Z\", \"rezultat\": \"EXECUTATA\"}"));
    }

    @Test
    void confirmareaRespinsaAreMotiv() throws MesajInvalid {
        MesajPrimit.Confirmare c = (MesajPrimit.Confirmare) decodeaza("flota/masina-3/confirmari",
                "{\"comanda_id\": 58, \"ora\": \"2026-12-10T10:15:04.002Z\", \"rezultat\": \"RESPINSA\", \"motiv\": \"kmh în afara intervalului\"}");
        assertEquals("kmh în afara intervalului", c.motiv());
    }

    @Test
    void rezultatulNecunoscutEsteRefuzat() {
        assertTrue(motiv("flota/masina-3/confirmari",
                "{\"comanda_id\": 59, \"ora\": \"2026-12-10T10:15:04.002Z\", \"rezultat\": \"POATE\"}").contains("POATE"));
    }
}
