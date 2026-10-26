package ro.flota.server.date;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Citește date\zone.json (GeoJSON, D18) și date\flota.json. Coordonatele ies ca [lat, lon]. */
public final class CititorDate {

    private final JsonMapper json;

    public CititorDate(JsonMapper json) {
        this.json = json;
    }

    public List<ZonaDefinita> zone(Path fisier) {
        JsonNode radacina = citesteJson(fisier);
        JsonNode elemente = radacina.path("features");
        if (!eText(radacina.path("type"), "FeatureCollection") || !elemente.isArray() || elemente.isEmpty()) {
            throw new EroareDate(fisier, "trebuie să fie un FeatureCollection GeoJSON cu cel puțin o zonă");
        }
        List<ZonaDefinita> zone = new ArrayList<>();
        Set<String> iduri = new HashSet<>();
        int nr = 0;
        for (JsonNode element : elemente) {
            nr++;
            JsonNode geometrie = element.path("geometry");
            if (!eText(geometrie.path("type"), "Polygon")) {
                throw new EroareDate(fisier, "elementul nr. " + nr + " nu e Polygon; zonele se desenează cu poligonul");
            }
            JsonNode proprietati = element.path("properties");
            String id = text(proprietati, "id", fisier);
            if (!iduri.add(id)) {
                throw new EroareDate(fisier, "zona \"" + id + "\" apare de două ori");
            }
            JsonNode viteza = proprietati.path("viteza_maxima");
            if (!viteza.isIntegralNumber() || viteza.asInt() <= 0) {
                throw new EroareDate(fisier,
                        "zona \"" + id + "\": \"viteza_maxima\" trebuie să fie un număr întreg pozitiv, scris fără ghilimele");
            }
            // Doar inelul exterior, coordinates[0]; găurile din poligon nu se folosesc.
            List<Punct> inel = new ArrayList<>();
            for (JsonNode c : geometrie.path("coordinates").path(0)) {
                if (!c.isArray() || c.size() < 2 || !c.get(0).isNumber() || !c.get(1).isNumber()) {
                    throw new EroareDate(fisier, "zona \"" + id + "\" are o coordonată greșită");
                }
                inel.add(new Punct(c.get(1).asDouble(), c.get(0).asDouble())); // GeoJSON: [lon, lat]
            }
            if (inel.size() < 4) { // un inel închis repetă primul punct la capăt
                throw new EroareDate(fisier, "zona \"" + id + "\" are mai puțin de 3 colțuri");
            }
            zone.add(new ZonaDefinita(id, text(proprietati, "nume", fisier), List.copyOf(inel), viteza.asInt()));
        }
        return zone;
    }

    public List<MasinaDefinita> flota(Path fisier, Set<String> zone) {
        JsonNode masini = citesteJson(fisier).path("masini");
        if (!masini.isArray() || masini.isEmpty()) {
            throw new EroareDate(fisier, "lipsește lista \"masini\" sau e goală");
        }
        List<MasinaDefinita> rezultat = new ArrayList<>();
        Set<String> iduri = new HashSet<>();
        for (JsonNode m : masini) {
            String id = text(m, "id", fisier);
            if (!iduri.add(id)) {
                throw new EroareDate(fisier, "id-ul \"" + id + "\" apare de două ori");
            }
            String zona = text(m, "zona", fisier);
            if (!zone.contains(zona)) {
                throw new EroareDate(fisier, id + " cere zona \"" + zona + "\", care nu există în zone.json");
            }
            rezultat.add(new MasinaDefinita(id, zona));
        }
        return rezultat;
    }

    private JsonNode citesteJson(Path fisier) {
        String continut;
        try {
            continut = Files.readString(fisier);
        } catch (NoSuchFileException e) {
            // Calea e relativă la folderul din care pornește serverul, deci spun unde am căutat de fapt.
            throw new EroareDate(fisier, "nu există; l-am căutat la " + fisier.toAbsolutePath().normalize());
        } catch (IOException e) {
            throw new EroareDate(fisier, "nu pot citi fișierul " + fisier.toAbsolutePath().normalize()
                    + " (" + e.getClass().getSimpleName() + ")");
        }
        if (continut.startsWith("\uFEFF")) {
            continut = continut.substring(1); // BOM-ul pus de PowerShell 5.1
        }
        try {
            return json.readTree(continut);
        } catch (JacksonException e) {
            throw new EroareDate(fisier, "nu e JSON valid: " + e.getOriginalMessage());
        }
    }

    private static String text(JsonNode nod, String camp, Path fisier) {
        JsonNode valoare = nod.path(camp);
        if (!valoare.isString() || valoare.asString().isBlank()) {
            throw new EroareDate(fisier, "lipsește câmpul text \"" + camp + "\"");
        }
        return valoare.asString();
    }

    private static boolean eText(JsonNode nod, String asteptat) {
        return nod.isString() && nod.asString().equals(asteptat);
    }
}
