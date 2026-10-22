package ro.flota.server.comenzi;

import tools.jackson.databind.JsonNode;

/** Ce trimite UI-ul la POST /api/masini/{id}/comenzi: {"tip": "...", "parametri": {...}}. */
public record CerereComanda(String tip, JsonNode parametri) {
}
