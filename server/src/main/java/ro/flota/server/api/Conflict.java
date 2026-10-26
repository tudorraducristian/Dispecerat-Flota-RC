package ro.flota.server.api;

/** Cererea e corectă, dar se bate cu ce există deja: un nume luat, ultimul admin, propriul cont. Devine 409. */
public final class Conflict extends RuntimeException {

    public Conflict(String mesaj) {
        super(mesaj);
    }
}
