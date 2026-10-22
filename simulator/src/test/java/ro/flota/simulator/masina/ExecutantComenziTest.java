package ro.flota.simulator.masina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.Json;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.TipMesaj;
import ro.flota.simulator.simulare.Viteza;
import ro.flota.simulator.testutil.AleatorScriptat;
import tools.jackson.databind.JsonNode;

class ExecutantComenziTest {

    private static final String ORA = "2026-12-10T10:15:04.002Z";

    private final Viteza viteza = new Viteza(new AleatorScriptat(0.99, 0.05), new AleatorScriptat(0.99), 0);
    private final AtomicInteger intoarceri = new AtomicInteger();
    private final List<String> spuseSoferului = new ArrayList<>();
    private final ExecutantComenzi executant =
            new ExecutantComenzi("masina-3", viteza, intoarceri::incrementAndGet, spuseSoferului::add);

    private static byte[] octeti(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    /** Execută comanda și întoarce confirmarea, după ce verifică unde și cum pleacă. */
    private JsonNode confirmare(String comanda) {
        Mesaj m = executant.executa(octeti(comanda), ORA).orElseThrow();
        assertEquals(TipMesaj.CONFIRMARE, m.tip());
        assertEquals("flota/masina-3/confirmari", m.topic());
        assertEquals(1, m.qos());
        assertFalse(m.retain());
        return Json.MAPPER.readTree(m.payload());
    }

    @Test
    void limiteazaVitezaTaieTinta() {
        viteza.tick(); // ținta devine 90
        JsonNode c = confirmare("{\"id\": 57, \"tip\": \"LIMITEAZA_VITEZA\", \"parametri\": {\"kmh\": 50}}");
        assertEquals(57, c.get("comanda_id").asLong());
        assertEquals(ORA, c.get("ora").asString());
        assertEquals("EXECUTATA", c.get("rezultat").asString());
        assertFalse(c.has("motiv"));
        assertEquals(50.0, viteza.tinta(), 1e-9);
    }

    @Test
    void kmhInAfaraIntervaluluiEsteRespins() {
        for (String kmh : List.of("9", "131", "\"50\"", "50.5")) {
            JsonNode c = confirmare("{\"id\": 58, \"tip\": \"LIMITEAZA_VITEZA\", \"parametri\": {\"kmh\": " + kmh + "}}");
            assertEquals("RESPINSA", c.get("rezultat").asString(), kmh);
            assertEquals("kmh în afara intervalului", c.get("motiv").asString(), kmh);
        }
    }

    @Test
    void intoarceLaBaza() {
        JsonNode c = confirmare("{\"id\": 59, \"tip\": \"INTOARCE_LA_BAZA\", \"parametri\": {}}");
        assertEquals("EXECUTATA", c.get("rezultat").asString());
        assertEquals(1, intoarceri.get());
    }

    @Test
    void tipNecunoscutEsteRespins() {
        JsonNode c = confirmare("{\"id\": 60, \"tip\": \"ZBOARA\", \"parametri\": {}}");
        assertEquals("RESPINSA", c.get("rezultat").asString());
        assertTrue(c.get("motiv").asString().contains("ZBOARA"));
    }

    @Test
    void comandaFaraIdNuPrimesteConfirmare() {
        assertTrue(executant.executa(octeti("nu e json"), ORA).isEmpty());
        assertTrue(executant.executa(octeti("{\"tip\": \"INTOARCE_LA_BAZA\"}"), ORA).isEmpty());
        assertEquals(0, intoarceri.get());
    }

    @Test
    void mesajulAjungeLaSofer() {
        JsonNode c = confirmare("{\"id\": 61, \"tip\": \"MESAJ_SOFER\", \"parametri\": {\"text\": \"Treci pe la bază\"}}");
        assertEquals("EXECUTATA", c.get("rezultat").asString());
        assertEquals(List.of("Treci pe la bază"), spuseSoferului);
    }

    @Test
    void mesajulPreaLungEsteRespins() {
        String lung = "x".repeat(201);
        JsonNode c = confirmare("{\"id\": 62, \"tip\": \"MESAJ_SOFER\", \"parametri\": {\"text\": \"" + lung + "\"}}");
        assertEquals("RESPINSA", c.get("rezultat").asString());
        assertTrue(c.get("motiv").asString().contains("200"));
        assertTrue(spuseSoferului.isEmpty());
    }

    @Test
    void mesajulGolEsteRespins() {
        JsonNode c = confirmare("{\"id\": 63, \"tip\": \"MESAJ_SOFER\", \"parametri\": {\"text\": \"  \"}}");
        assertEquals("RESPINSA", c.get("rezultat").asString());
        assertTrue(spuseSoferului.isEmpty());
    }
}
