package ro.flota.simulator.mesaje;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Ce trimite mașina pe flota/<id>/confirmari după o comandă (spec §4.2). motiv apare doar la RESPINSA. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Confirmare(@JsonProperty("comanda_id") long comandaId, String ora, String rezultat, String motiv) {

    public static Confirmare executata(long comandaId, String ora) {
        return new Confirmare(comandaId, ora, "EXECUTATA", null);
    }

    public static Confirmare respinsa(long comandaId, String ora, String motiv) {
        return new Confirmare(comandaId, ora, "RESPINSA", motiv);
    }
}
