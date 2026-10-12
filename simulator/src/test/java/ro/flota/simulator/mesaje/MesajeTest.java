package ro.flota.simulator.mesaje;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ro.flota.simulator.Json;

class MesajeTest {

    private static void assertJson(String asteptat, Mesaj mesaj) {
        assertEquals(Json.MAPPER.readTree(asteptat), Json.MAPPER.readTree(mesaj.payload()));
    }

    @Test
    void telemetriaMergePeQos0SiNuERetinuta() {
        Mesaj m = Mesaje.telemetrie("masina-3", new Telemetrie(
                1042, "2026-12-10T10:15:03.120Z", 46.77121, 23.6236, 47.5, 92.1, 81, false));
        assertEquals(TipMesaj.TELEMETRIE, m.tip());
        assertEquals("flota/masina-3/telemetrie", m.topic());
        assertEquals(0, m.qos());
        assertFalse(m.retain());
        assertJson("""
                {"seq": 1042, "ora": "2026-12-10T10:15:03.120Z", "lat": 46.77121, "lon": 23.6236,
                 "viteza": 47.5, "temperatura": 92.1, "baterie": 81, "din_buffer": false}""", m);
    }

    @Test
    void stareaOnlineEsteRetinutaPeQos1() {
        Mesaj m = Mesaje.stare("masina-3", true);
        assertEquals(TipMesaj.STARE, m.tip());
        assertEquals("flota/masina-3/stare", m.topic());
        assertEquals(1, m.qos());
        assertTrue(m.retain());
        assertEquals("online", m.text());
    }

    @Test
    void stareaOfflineSeScrieOffline() {
        assertEquals("offline", Mesaje.stare("masina-3", false).text());
    }

    @Test
    void alarmaMergePeQos2SiNuERetinuta() {
        Mesaj m = Mesaje.alarma("masina-3", new Alarma(TipAlarma.SUPRAINCALZIRE, "2026-12-10T10:15:03.120Z", 112.4));
        assertEquals(TipMesaj.ALARMA, m.tip());
        assertEquals("flota/masina-3/alarma", m.topic());
        assertEquals(2, m.qos());
        assertFalse(m.retain());
        assertJson("""
                {"tip": "SUPRAINCALZIRE", "ora": "2026-12-10T10:15:03.120Z", "valoare": 112.4}""", m);
    }
}
