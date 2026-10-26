package ro.flota.server.setari;

import ro.flota.server.api.CerereGresita;

/** Setările din spec §16.3, cu numele din JSON. */
public record SetariDto(int offlinePrelungitSecunde, int traseuMinute) {

    public static final SetariDto IMPLICITE = new SetariDto(60, 5);

    /** Aruncă 400 la o valoare în afara limitelor; altfel întoarce setările neschimbate. */
    public SetariDto valideaza() {
        if (offlinePrelungitSecunde < 10 || offlinePrelungitSecunde > 3600) {
            throw new CerereGresita("offlinePrelungitSecunde e între 10 și 3600");
        }
        if (traseuMinute < 1 || traseuMinute > 360) {
            throw new CerereGresita("traseuMinute e între 1 și 360");
        }
        return this;
    }
}
