package ro.flota.server.api;

/** Ce s-a cerut nu există. Devine 404. */
public final class Negasit extends RuntimeException {

    public Negasit(String mesaj) {
        super(mesaj);
    }
}
