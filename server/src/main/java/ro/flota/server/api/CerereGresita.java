package ro.flota.server.api;

/** Cererea are o greșeală pe care UI-ul o poate repara. Devine 400. */
public final class CerereGresita extends RuntimeException {

    public CerereGresita(String mesaj) {
        super(mesaj);
    }
}
