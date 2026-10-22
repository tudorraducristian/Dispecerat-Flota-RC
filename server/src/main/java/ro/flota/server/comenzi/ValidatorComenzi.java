package ro.flota.server.comenzi;

import ro.flota.server.api.CerereGresita;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Verifică o comandă înainte să plece spre mașină (spec §4.2) și întoarce parametrii curățați, adică doar
 * câmpurile pe care le înțelege mașina. Mașina îi verifică și ea, a doua oară (D26).
 */
public final class ValidatorComenzi {

    static final int KMH_MINIM = 10;
    static final int KMH_MAXIM = 130;
    static final int TEXT_MAXIM = 200;

    private ValidatorComenzi() {
    }

    public static ObjectNode verifica(CerereComanda cerere) {
        if (cerere == null || cerere.tip() == null) {
            throw new CerereGresita("lipsește tipul comenzii");
        }
        JsonNode p = cerere.parametri() == null ? JsonNodeFactory.instance.objectNode() : cerere.parametri();
        ObjectNode curati = JsonNodeFactory.instance.objectNode();
        switch (cerere.tip()) {
            case "LIMITEAZA_VITEZA" -> {
                JsonNode kmh = p.get("kmh");
                if (kmh == null || !kmh.isIntegralNumber() || kmh.asInt() < KMH_MINIM || kmh.asInt() > KMH_MAXIM) {
                    throw new CerereGresita("kmh trebuie să fie un număr întreg între 10 și 130");
                }
                curati.put("kmh", kmh.asInt());
            }
            case "INTOARCE_LA_BAZA" -> {
                // fără parametri
            }
            default -> throw new CerereGresita("tip de comandă necunoscut: " + cerere.tip());
        }
        return curati;
    }
}
