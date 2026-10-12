package ro.flota.simulator.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ro.flota.simulator.Json;
import ro.flota.simulator.geo.Punct;
import ro.flota.simulator.geo.Traseu;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/** Citește flota din date\flota.json și traseele din date\trasee.json, un fișier GeoJSON (D18). */
public final class Flota {

    public static final int MAXIM_MASINI = 20;

    private Flota() {
    }

    public static List<DefinitieMasina> citeste(Path dosarDate) {
        Map<String, Traseu> trasee = citesteTrasee(dosarDate.resolve("trasee.json"));
        Path fisier = dosarDate.resolve("flota.json");
        JsonNode masini = citesteJson(fisier).path("masini");
        if (!masini.isArray() || masini.isEmpty()) {
            throw new EroareConfigurare(fisier, "lipsește lista \"masini\" sau e goală");
        }
        if (masini.size() > MAXIM_MASINI) {
            throw new EroareConfigurare(fisier, "are " + masini.size() + " mașini, iar limita e " + MAXIM_MASINI);
        }
        List<DefinitieMasina> rezultat = new ArrayList<>();
        Set<String> iduri = new HashSet<>();
        for (JsonNode m : masini) {
            String id = text(m, "id", fisier);
            if (!id.matches("masina-[0-9]+")) {
                throw new EroareConfigurare(fisier, "id-ul \"" + id + "\" nu are forma masina-N");
            }
            if (!iduri.add(id)) {
                throw new EroareConfigurare(fisier, "id-ul \"" + id + "\" apare de două ori");
            }
            String idTraseu = text(m, "traseu", fisier);
            Traseu traseu = trasee.get(idTraseu);
            if (traseu == null) {
                throw new EroareConfigurare(fisier,
                        id + " cere traseul \"" + idTraseu + "\", care nu există în trasee.json");
            }
            JsonNode start = m.path("start");
            if (!start.isNumber() || start.asDouble() < 0 || start.asDouble() >= 1) {
                throw new EroareConfigurare(fisier,
                        id + ": \"start\" trebuie să fie un număr de la 0 (inclusiv) la 1 (exclusiv)");
            }
            rezultat.add(new DefinitieMasina(id, traseu, start.asDouble(), text(m, "zona", fisier)));
        }
        return rezultat;
    }

    private static Map<String, Traseu> citesteTrasee(Path fisier) {
        JsonNode radacina = citesteJson(fisier);
        JsonNode elemente = radacina.path("features");
        if (!eText(radacina.path("type"), "FeatureCollection") || !elemente.isArray() || elemente.isEmpty()) {
            throw new EroareConfigurare(fisier, "trebuie să fie un FeatureCollection GeoJSON cu cel puțin un traseu");
        }
        Map<String, Traseu> trasee = new HashMap<>();
        int nr = 0;
        for (JsonNode element : elemente) {
            nr++;
            JsonNode geometrie = element.path("geometry");
            if (!eText(geometrie.path("type"), "LineString")) {
                throw new EroareConfigurare(fisier,
                        "elementul nr. " + nr + " nu e LineString; traseele se desenează cu linia");
            }
            String id = text(element.path("properties"), "id", fisier);
            if (trasee.containsKey(id)) {
                throw new EroareConfigurare(fisier, "traseul \"" + id + "\" apare de două ori");
            }
            List<Punct> puncte = new ArrayList<>();
            for (JsonNode c : geometrie.path("coordinates")) {
                if (!c.isArray() || c.size() < 2 || !c.get(0).isNumber() || !c.get(1).isNumber()) {
                    throw new EroareConfigurare(fisier, "traseul \"" + id + "\" are o coordonată greșită");
                }
                puncte.add(new Punct(c.get(1).asDouble(), c.get(0).asDouble())); // GeoJSON: [lon, lat]
            }
            try {
                trasee.put(id, new Traseu(id, puncte));
            } catch (IllegalArgumentException e) {
                throw new EroareConfigurare(fisier, e.getMessage());
            }
        }
        return trasee;
    }

    private static JsonNode citesteJson(Path fisier) {
        String continut;
        try {
            continut = Files.readString(fisier);
        } catch (IOException e) {
            throw new EroareConfigurare(fisier, "nu pot citi fișierul (" + e.getClass().getSimpleName() + ")");
        }
        if (continut.startsWith("\uFEFF")) {
            continut = continut.substring(1); // BOM-ul pus de PowerShell 5.1
        }
        try {
            return Json.MAPPER.readTree(continut);
        } catch (JacksonException e) {
            throw new EroareConfigurare(fisier, "nu e JSON valid: " + e.getOriginalMessage());
        }
    }

    private static String text(JsonNode nod, String camp, Path fisier) {
        JsonNode valoare = nod.path(camp);
        if (!valoare.isString() || valoare.asString().isBlank()) {
            throw new EroareConfigurare(fisier, "lipsește câmpul text \"" + camp + "\"");
        }
        return valoare.asString();
    }

    private static boolean eText(JsonNode nod, String asteptat) {
        return nod.isString() && nod.asString().equals(asteptat);
    }
}
