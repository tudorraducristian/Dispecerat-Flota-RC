package ro.flota.server.api;

/** Lipsește un token valid, sau login-ul n-a mers. Devine 401. */
public final class Neautorizat extends RuntimeException {

    public Neautorizat(String mesaj) {
        super(mesaj);
    }
}
