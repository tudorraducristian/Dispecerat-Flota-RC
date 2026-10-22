package ro.flota.server.api;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.flota.server.live.CanalLive;
import ro.flota.server.retea.CerereVreme;
import ro.flota.server.retea.ClientProxy;
import ro.flota.server.retea.EvenimentDto;
import ro.flota.server.retea.EvenimenteRetea;
import ro.flota.server.retea.LiniiVreme;
import ro.flota.server.retea.VremeMasinaDto;

/** Endpoint-urile 9, 10 și 11 din spec §6.5: vremea din rețea, prin proxy. */
@RestController
@RequestMapping("/api/retea")
public class ReteaController {

    private final ClientProxy proxy;
    private final EvenimenteRetea evenimente;
    private final CanalLive live;
    private final Clock ceas;

    public ReteaController(ClientProxy proxy, EvenimenteRetea evenimente, CanalLive live, Clock ceas) {
        this.proxy = proxy;
        this.evenimente = evenimente;
        this.live = live;
        this.ceas = ceas;
    }

    /** Endpoint-ul 9: STARE, despărțit pe mașini. */
    @GetMapping("/stare")
    public List<VremeMasinaDto> stare() {
        return proxy.trimite("STARE").stream().map(VremeMasinaDto::din).toList();
    }

    /** Endpoint-ul 10: construiește linia, o trimite, salvează evenimentul și întoarce răspunsul, OK sau ERR. */
    @PostMapping("/vreme")
    public EvenimentDto vreme(@RequestBody CerereVreme cerere) {
        String linie = LiniiVreme.linie(cerere);
        Instant start = ceas.instant();
        String raspuns = proxy.trimite(linie).get(0);
        Instant sfarsit = raspuns.equals("OK") ? LiniiVreme.sfarsit(cerere, start) : null;
        EvenimentDto eveniment = evenimente.salveaza(linie.split(" ")[1], linie, raspuns, start, sfarsit);
        live.trimite("EVENIMENT_RETEA", eveniment);
        return eveniment;
    }
}
