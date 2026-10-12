package ro.flota.server.date;

import java.util.List;

/** O zonă din date\zone.json: poligonul e inelul exterior, ca listă de [lat, lon]. */
public record ZonaDefinita(String id, String nume, List<Punct> poligon, int vitezaMaxima) {
}
