package ro.flota.simulator.masina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.Json;
import ro.flota.simulator.config.DefinitieMasina;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.TipMesaj;
import ro.flota.simulator.simulare.Temperatura;
import ro.flota.simulator.simulare.Viteza;
import ro.flota.simulator.testutil.AleatorScriptat;
import ro.flota.simulator.testutil.Trasee;
import tools.jackson.databind.JsonNode;

class MotorTest {

    private static final Clock CEAS = Clock.fixed(Instant.parse("2026-12-10T10:15:03.120Z"), ZoneOffset.UTC);

    private final Buffer coada = new Buffer(300);

    private static Viteza spre50() {
        return new Viteza(new AleatorScriptat(0.99, 0.5, 0.5), new AleatorScriptat(0.99), 0);
    }

    private static Temperatura normala(double initiala) {
        return new Temperatura(new AleatorScriptat(0.99), new AleatorScriptat(0.5), initiala);
    }

    private Motor motor(double start, Viteza viteza, Temperatura temperatura, boolean conectat) {
        DefinitieMasina def = new DefinitieMasina("masina-1", Trasee.patrat(), start, "z1");
        return new Motor(def, viteza, temperatura, CEAS, coada, () -> conectat, 1.0);
    }

    private static JsonNode json(Mesaj m) {
        return Json.MAPPER.readTree(m.payload());
    }

    @Test
    void primulTickPuneOTelemetrieCuSeq1() {
        motor(0, spre50(), normala(85), true).tick();
        assertEquals(1, coada.marime());
        Mesaj m = coada.continut().getFirst();
        assertEquals(TipMesaj.TELEMETRIE, m.tip());
        assertEquals("flota/masina-1/telemetrie", m.topic());
        JsonNode j = json(m);
        assertEquals(1, j.get("seq").asLong());
        assertEquals("2026-12-10T10:15:03.120Z", j.get("ora").asString());
        assertEquals(5.0, j.get("viteza").asDouble(), 1e-9);
        assertEquals(100, j.get("baterie").asInt());
        assertFalse(j.get("din_buffer").asBoolean());
    }

    @Test
    void seqCresteCuUnu() {
        Motor motor = motor(0, spre50(), normala(85), true);
        motor.tick();
        motor.tick();
        motor.tick();
        assertEquals(3, json(coada.continut().getLast()).get("seq").asLong());
    }

    @Test
    void dinBufferSpuneCaRadioulEraDeconectat() {
        motor(0, spre50(), normala(85), false).tick();
        assertTrue(json(coada.continut().getFirst()).get("din_buffer").asBoolean());
    }

    @Test
    void masinaInainteazaPeTraseuCuVitezaEi() {
        Motor motor = motor(0, spre50(), normala(85), true);
        for (int i = 0; i < 10; i++) {
            motor.tick(); // 5 + 10 + ... + 50 = 275 km/h·s, adică 76,389 m spre nord
        }
        JsonNode j = json(coada.continut().getLast());
        assertEquals(46.00069, j.get("lat").asDouble(), 1e-9);
        assertEquals(23.0, j.get("lon").asDouble(), 1e-9);
    }

    @Test
    void startulDinFlotaMutaMasinaPeBucla() {
        motor(0.25, spre50(), normala(85), true).tick(); // 94,218 m + 1,389 m
        assertEquals(46.00086, json(coada.continut().getFirst()).get("lat").asDouble(), 1e-9);
    }

    @Test
    void bateriaScadeCuUnProcentPeMinut() {
        Motor motor = motor(0, spre50(), normala(85), true);
        for (int i = 0; i < 61; i++) {
            motor.tick();
        }
        assertEquals(99, json(coada.continut().getLast()).get("baterie").asInt());
    }

    @Test
    void supraincalzireaPuneOAlarmaDupaTelemetrie() {
        Temperatura cuDefect = new Temperatura(new AleatorScriptat(0.99, 0.0), new AleatorScriptat(0.5), 109.8);
        motor(0, spre50(), cuDefect, true).tick();
        assertEquals(2, coada.marime());
        Mesaj alarma = coada.continut().getLast();
        assertEquals(TipMesaj.ALARMA, alarma.tip());
        assertEquals(2, alarma.qos());
        assertEquals("SUPRAINCALZIRE", json(alarma).get("tip").asString());
        assertEquals(111.8, json(alarma).get("valoare").asDouble(), 1e-9);
    }

    @Test
    void franareaBruscaPuneOAlarmaCuScaderea() {
        Viteza franeaza = new Viteza(new AleatorScriptat(0.99, 0.5, 0.75), new AleatorScriptat(0.99, 0.99, 0.0), 60);
        Motor motor = motor(0, franeaza, normala(85), true);
        motor.tick(); // 60
        motor.tick(); // 30
        Mesaj alarma = coada.continut().getLast();
        assertEquals(TipMesaj.ALARMA, alarma.tip());
        assertEquals("FRANARE_BRUSCA", json(alarma).get("tip").asString());
        assertEquals(30.0, json(alarma).get("valoare").asDouble(), 1e-9);
    }
}
