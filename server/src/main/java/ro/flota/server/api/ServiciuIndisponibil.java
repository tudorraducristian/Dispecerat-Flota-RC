package ro.flota.server.api;

/** Un serviciu de care depinde cererea nu răspunde: broker-ul sau proxy-ul. Devine 503. */
public final class ServiciuIndisponibil extends RuntimeException {

    public ServiciuIndisponibil(String mesaj) {
        super(mesaj);
    }
}
