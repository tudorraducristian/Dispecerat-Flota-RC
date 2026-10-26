package ro.flota.server.api;

/** Omul e logat, dar rolul lui nu ajunge. Devine 403. */
public final class Interzis extends RuntimeException {

    public Interzis(String mesaj) {
        super(mesaj);
    }
}
