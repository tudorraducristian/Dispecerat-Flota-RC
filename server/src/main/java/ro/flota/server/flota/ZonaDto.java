package ro.flota.server.flota;

import java.util.List;

/** O zonă pentru hartă: poligonul e o listă de [lat, lon], exact ce cere Leaflet. */
public record ZonaDto(String id, String nume, List<List<Double>> poligon, int vitezaMaxima) {
}
