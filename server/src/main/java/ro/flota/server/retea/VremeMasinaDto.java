package ro.flota.server.retea;

/** Un rând din răspunsul la STARE, despărțit în mașină și vreme: „masina-3 TUNEL 12”. */
public record VremeMasinaDto(String masina, String descriere) {

    public static VremeMasinaDto din(String rand) {
        int spatiu = rand.indexOf(' ');
        return spatiu < 0 ? new VremeMasinaDto(rand, "") : new VremeMasinaDto(rand.substring(0, spatiu), rand.substring(spatiu + 1));
    }
}
