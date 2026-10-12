package ro.flota.simulator.geo;

/** Un punct pe hartă, în grade. Atenție: GeoJSON scrie [lon, lat], invers. */
public record Punct(double lat, double lon) {

    private static final double RAZA_PAMANT_M = 6_371_000.0;

    /** Distanța pe suprafața Pământului, în metri (formula haversine). */
    public double distantaPana(Punct altul) {
        double fi1 = Math.toRadians(lat);
        double fi2 = Math.toRadians(altul.lat);
        double dFi = fi2 - fi1;
        double dLambda = Math.toRadians(altul.lon - lon);
        double a = Math.sin(dFi / 2) * Math.sin(dFi / 2)
                + Math.cos(fi1) * Math.cos(fi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        return 2 * RAZA_PAMANT_M * Math.asin(Math.sqrt(a));
    }

    /** Punctul aflat la fracțiunea f, de la 0 la 1, din drumul de aici până la altul. */
    public Punct spre(Punct altul, double f) {
        return new Punct(lat + (altul.lat - lat) * f, lon + (altul.lon - lon) * f);
    }
}
