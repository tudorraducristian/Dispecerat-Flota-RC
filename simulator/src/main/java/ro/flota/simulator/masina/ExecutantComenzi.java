package ro.flota.simulator.masina;

import java.util.Optional;
import java.util.function.Consumer;
import ro.flota.simulator.Json;
import ro.flota.simulator.mesaje.Confirmare;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;
import ro.flota.simulator.simulare.Viteza;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/**
 * Comenzile venite de la dispecer (spec §4.2): le verifică, le execută și pregătește confirmarea.
 * Rulează pe firul motorului, deci nu se bate cu el pe viteză sau pe poziție.
 */
public final class ExecutantComenzi {

    static final int KMH_MINIM = 10;
    static final int KMH_MAXIM = 130;
    static final int TEXT_MAXIM = 200;

    private final String idMasina;
    private final Viteza viteza;
    private final Runnable intoarceLaBaza;
    private final Consumer<String> sofer;

    public ExecutantComenzi(String idMasina, Viteza viteza, Runnable intoarceLaBaza, Consumer<String> sofer) {
        this.idMasina = idMasina;
        this.viteza = viteza;
        this.intoarceLaBaza = intoarceLaBaza;
        this.sofer = sofer;
    }

    /** Execută comanda și întoarce confirmarea de trimis. Gol dacă mesajul n-are nici măcar un id. */
    public Optional<Mesaj> executa(byte[] payload, String ora) {
        JsonNode j;
        try {
            j = Json.MAPPER.readTree(payload);
        } catch (JacksonException e) {
            System.out.printf("[%s] comandă care nu e JSON; o ignor%n", idMasina);
            return Optional.empty();
        }
        JsonNode idNod = j == null ? null : j.get("id");
        if (idNod == null || !idNod.isIntegralNumber()) {
            System.out.printf("[%s] comandă fără id; o ignor%n", idMasina);
            return Optional.empty();
        }
        long id = idNod.asLong();
        JsonNode tipNod = j.get("tip");
        String tip = tipNod != null && tipNod.isString() ? tipNod.asString() : "";
        JsonNode parametri = j.path("parametri");
        String motiv = switch (tip) {
            case "LIMITEAZA_VITEZA" -> limiteazaViteza(parametri);
            case "INTOARCE_LA_BAZA" -> {
                intoarceLaBaza.run();
                yield null;
            }
            default -> "tip necunoscut: " + tip;
        };
        Confirmare c = motiv == null ? Confirmare.executata(id, ora) : Confirmare.respinsa(id, ora, motiv);
        System.out.printf("[%s] comanda %d %s: %s%s%n", idMasina, id, tip, c.rezultat(), motiv == null ? "" : " (" + motiv + ")");
        return Optional.of(Mesaje.confirmare(idMasina, c));
    }

    /** Întoarce motivul respingerii, sau null dacă a executat-o. */
    private String limiteazaViteza(JsonNode p) {
        JsonNode kmh = p.get("kmh");
        if (kmh == null || !kmh.isIntegralNumber() || kmh.asInt() < KMH_MINIM || kmh.asInt() > KMH_MAXIM) {
            return "kmh în afara intervalului";
        }
        viteza.limiteaza(kmh.asInt());
        return null;
    }
}
