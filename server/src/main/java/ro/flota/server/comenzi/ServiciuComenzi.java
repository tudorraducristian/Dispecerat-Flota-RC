package ro.flota.server.comenzi;

import java.time.Clock;
import org.springframework.stereotype.Service;
import ro.flota.server.api.Negasit;
import ro.flota.server.api.ServiciuIndisponibil;
import ro.flota.server.date.RegistruFlota;
import ro.flota.server.mqtt.ClientMqtt;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Trimiterea unei comenzi (spec §6.5, endpoint-ul 6): o verifică, o salvează TRIMISA, apoi o publică pe
 * QoS 1. Dacă mașina e offline, broker-ul o păstrează în sesiunea ei persistentă până revine.
 */
@Service
public class ServiciuComenzi {

    private final Comenzi comenzi;
    private final RegistruFlota registru;
    private final ClientMqtt mqtt;
    private final JsonMapper json;
    private final Clock ceas;

    public ServiciuComenzi(Comenzi comenzi, RegistruFlota registru, ClientMqtt mqtt, JsonMapper json, Clock ceas) {
        this.comenzi = comenzi;
        this.registru = registru;
        this.mqtt = mqtt;
        this.json = json;
        this.ceas = ceas;
    }

    public ComandaDto trimite(String masinaId, CerereComanda cerere) {
        if (!registru.cunoscuta(masinaId)) {
            throw new Negasit("mașina " + masinaId + " nu există");
        }
        ObjectNode parametri = ValidatorComenzi.verifica(cerere);
        if (!mqtt.conectat()) {
            throw new ServiciuIndisponibil("serverul nu e conectat la Mosquitto; comanda n-a fost trimisă");
        }
        ComandaDto comanda = comenzi.salveaza(masinaId, cerere.tip(), parametri, ceas.instant());
        ObjectNode mesaj = json.createObjectNode();
        mesaj.put("id", comanda.id());
        mesaj.put("tip", comanda.tip());
        mesaj.set("parametri", parametri);
        mqtt.publica("flota/" + masinaId + "/comenzi", json.writeValueAsBytes(mesaj));
        return comanda;
    }
}
