package ro.flota.server.date;

/** Un punct pe hartă, în grade. Atenție: GeoJSON scrie [lon, lat], invers. */
public record Punct(double lat, double lon) {
}
