package ro.flota.server.date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class CititorDateTest {

    private static final String ZONE = """
            {"type": "FeatureCollection", "features": [
              {"type": "Feature", "properties": {"id": "z1", "nume": "Centru", "viteza_maxima": 50},
               "geometry": {"type": "Polygon", "coordinates": [[
                 [23.00, 46.00], [23.01, 46.00], [23.01, 46.01], [23.00, 46.01], [23.00, 46.00]]]}}
            ]}""";

    private static final String FLOTA = """
            {"masini": [
              {"id": "masina-1", "traseu": "t1", "start": 0.0, "zona": "z1"},
              {"id": "masina-2", "traseu": "t1", "start": 0.5, "zona": "z1"}
            ]}""";

    private static final Set<String> DOAR_Z1 = Set.of("z1");

    @TempDir
    Path dosar;

    private final CititorDate cititor = new CititorDate(JsonMapper.builder().build());

    private Path scrie(String fisier, String continut) throws IOException {
        Path p = dosar.resolve(fisier);
        Files.writeString(p, continut);
        return p;
    }

    /** Registrul fazei 2: pornit din alt folder, serverul spunea doar „NoSuchFileException”, fără unde a căutat. */
    @Test
    void fisierulLipsaSpuneUndeACautat() {
        Path lipsa = dosar.resolve("zone.json");
        String mesaj = assertThrows(EroareDate.class, () -> cititor.zone(lipsa)).getMessage();
        assertTrue(mesaj.startsWith("zone.json: "), mesaj);
        assertTrue(mesaj.contains(lipsa.toAbsolutePath().normalize().toString()), mesaj);
    }

    private String eroareZone(String continut) throws IOException {
        Path p = scrie("zone.json", continut);
        return assertThrows(EroareDate.class, () -> cititor.zone(p)).getMessage();
    }

    @Test
    void citesteZonaCuCoordonateleIntoarse() throws IOException {
        List<ZonaDefinita> zone = cititor.zone(scrie("zone.json", ZONE));
        assertEquals(1, zone.size());
        ZonaDefinita z = zone.get(0);
        assertEquals("z1", z.id());
        assertEquals("Centru", z.nume());
        assertEquals(50, z.vitezaMaxima());
        assertEquals(5, z.poligon().size());
        assertEquals(new Punct(46.00, 23.00), z.poligon().get(0));
        assertEquals(new Punct(46.00, 23.01), z.poligon().get(1));
    }

    @Test
    void acceptaZoneSalvateCuBom() throws IOException {
        assertEquals(1, cititor.zone(scrie("zone.json", "\uFEFF" + ZONE)).size());
    }

    @Test
    void unElementCareNuEPoligonEsteRefuzat() throws IOException {
        String mesaj = eroareZone(ZONE.replace("\"Polygon\"", "\"LineString\""));
        assertTrue(mesaj.startsWith("zone.json") && mesaj.contains("Polygon"), mesaj);
    }

    @Test
    void vitezaMaximaScrisaCaTextEsteRefuzata() throws IOException {
        String mesaj = eroareZone(ZONE.replace("\"viteza_maxima\": 50", "\"viteza_maxima\": \"50\""));
        assertTrue(mesaj.contains("z1") && mesaj.contains("viteza_maxima"), mesaj);
    }

    @Test
    void poligonulCuDouaColturiEsteRefuzat() throws IOException {
        String doarDoua = ZONE.replace("[23.01, 46.01], [23.00, 46.01], ", "");
        assertTrue(eroareZone(doarDoua).contains("3 colțuri"));
    }

    @Test
    void jsonStricatSpuneFisierul() throws IOException {
        String mesaj = eroareZone("{\"type\": ");
        assertTrue(mesaj.startsWith("zone.json") && mesaj.contains("nu e JSON valid"), mesaj);
    }

    @Test
    void zonelePropriiSeIncarca() {
        List<ZonaDefinita> zone = cititor.zone(Path.of("..", "date", "zone.json"));
        assertFalse(zone.isEmpty());
        for (ZonaDefinita z : zone) {
            assertTrue(z.vitezaMaxima() > 0, z.id());
        }
    }

    private String eroareFlota(String continut) throws IOException {
        Path p = scrie("flota.json", continut);
        return assertThrows(EroareDate.class, () -> cititor.flota(p, DOAR_Z1)).getMessage();
    }

    @Test
    void citesteIdSiZona() throws IOException {
        List<MasinaDefinita> flota = cititor.flota(scrie("flota.json", FLOTA), DOAR_Z1);
        assertEquals(List.of(new MasinaDefinita("masina-1", "z1"), new MasinaDefinita("masina-2", "z1")), flota);
    }

    @Test
    void zonaNecunoscutaEstePusaPeNume() throws IOException {
        String mesaj = eroareFlota(FLOTA.replaceFirst("\"zona\": \"z1\"", "\"zona\": \"z9\""));
        assertTrue(mesaj.startsWith("flota.json") && mesaj.contains("masina-1") && mesaj.contains("z9"), mesaj);
    }

    @Test
    void idDublatEsteRefuzat() throws IOException {
        assertTrue(eroareFlota(FLOTA.replace("masina-2", "masina-1")).contains("de două ori"));
    }

    @Test
    void flotaFaraMasiniEsteRefuzata() throws IOException {
        assertTrue(eroareFlota("{\"masini\": []}").contains("masini"));
    }

    @Test
    void flotaProprieSeIncarca() {
        Set<String> zone = new HashSet<>();
        cititor.zone(Path.of("..", "date", "zone.json")).forEach(z -> zone.add(z.id()));
        assertEquals(10, cititor.flota(Path.of("..", "date", "flota.json"), zone).size());
    }
}
