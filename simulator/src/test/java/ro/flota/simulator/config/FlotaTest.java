package ro.flota.simulator.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ro.flota.simulator.geo.Punct;

class FlotaTest {

    private static final String TRASEE = """
            {"type": "FeatureCollection", "features": [
              {"type": "Feature", "properties": {"id": "t1"},
               "geometry": {"type": "LineString",
                            "coordinates": [[23.000, 46.000], [23.000, 46.001], [23.001, 46.001]]}}
            ]}""";

    private static final String FLOTA = """
            {"masini": [
              {"id": "masina-1", "traseu": "t1", "start": 0.25, "zona": "z1"},
              {"id": "masina-2", "traseu": "t1", "start": 0.0,  "zona": "z1"}
            ]}""";

    @TempDir
    Path dosar;

    private void scrie(String fisier, String continut) throws IOException {
        Files.writeString(dosar.resolve(fisier), continut);
    }

    private String eroare(String trasee, String flota) throws IOException {
        scrie("trasee.json", trasee);
        if (flota != null) {
            scrie("flota.json", flota);
        }
        return assertThrows(EroareConfigurare.class, () -> Flota.citeste(dosar)).getMessage();
    }

    @Test
    void citesteFlotaSiTraseele() throws IOException {
        scrie("trasee.json", TRASEE);
        scrie("flota.json", FLOTA);
        List<DefinitieMasina> flota = Flota.citeste(dosar);
        assertEquals(2, flota.size());
        DefinitieMasina prima = flota.get(0);
        assertEquals("masina-1", prima.id());
        assertEquals("t1", prima.traseu().id());
        assertEquals(0.25, prima.start());
        assertEquals("z1", prima.zona());
    }

    @Test
    void geoJsonScrieIntaiLongitudinea() throws IOException {
        scrie("trasee.json", TRASEE);
        scrie("flota.json", FLOTA);
        assertEquals(new Punct(46.000, 23.000), Flota.citeste(dosar).get(0).traseu().punctLa(0));
    }

    @Test
    void acceptaFisierSalvatCuBom() throws IOException {
        scrie("trasee.json", "﻿" + TRASEE);
        scrie("flota.json", "﻿" + FLOTA);
        assertEquals(2, Flota.citeste(dosar).size());
    }

    @Test
    void traseulInexistentEstePusPeNume() throws IOException {
        String mesaj = eroare(TRASEE, FLOTA.replace("\"traseu\": \"t1\", \"start\": 0.25", "\"traseu\": \"t9\", \"start\": 0.25"));
        assertTrue(mesaj.startsWith("flota.json"), mesaj);
        assertTrue(mesaj.contains("masina-1") && mesaj.contains("t9"), mesaj);
    }

    @Test
    void jsonStricatSpuneFisierul() throws IOException {
        String mesaj = eroare(TRASEE, "{\"masini\": [");
        assertTrue(mesaj.startsWith("flota.json") && mesaj.contains("nu e JSON valid"), mesaj);
    }

    @Test
    void fisierulLipsaSpuneFisierul() throws IOException {
        String mesaj = eroare(TRASEE, null);
        assertTrue(mesaj.startsWith("flota.json") && mesaj.contains("nu pot citi"), mesaj);
    }

    @Test
    void idDublatEsteRefuzat() throws IOException {
        String mesaj = eroare(TRASEE, FLOTA.replace("masina-2", "masina-1"));
        assertTrue(mesaj.contains("de două ori"), mesaj);
    }

    @Test
    void startulTrebuieSaFieSub1() throws IOException {
        String mesaj = eroare(TRASEE, FLOTA.replace("0.25", "1.0"));
        assertTrue(mesaj.contains("start"), mesaj);
    }

    @Test
    void unElementCareNuELinieEsteRefuzat() throws IOException {
        String mesaj = eroare(TRASEE.replace("LineString", "Point"), FLOTA);
        assertTrue(mesaj.startsWith("trasee.json") && mesaj.contains("LineString"), mesaj);
    }

    @Test
    void datelePropriiSeIncarca() {
        List<DefinitieMasina> flota = Flota.citeste(Path.of("..", "date"));
        assertEquals(10, flota.size());
        for (DefinitieMasina m : flota) {
            assertTrue(m.traseu().lungime() > 500, m.traseu().id() + " are sub 500 m");
        }
    }
}
