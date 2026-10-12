package ro.flota.server.flota;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import ro.flota.server.live.CanalLive;
import ro.flota.server.mqtt.MesajPrimit;

/**
 * Ce face serverul cu fiecare mesaj (spec §6.3). Rulează pe firul unic al lui ClientMqtt,
 * deci mesajele vin pe rând, în ordinea în care au sosit.
 */
@Service
public class Dispecer {

    private final Pozitii pozitii;
    private final Masini masini;
    private final Alarme alarme;
    private final CanalLive live;
    private final Clock ceas;

    public Dispecer(Pozitii pozitii, Masini masini, Alarme alarme, CanalLive live, Clock ceas) {
        this.pozitii = pozitii;
        this.masini = masini;
        this.alarme = alarme;
        this.live = live;
        this.ceas = ceas;
    }

    /** Interfața e sealed, deci compilatorul verifică că fiecare fel de mesaj are un caz. */
    public void primeste(MesajPrimit mesaj) {
        switch (mesaj) {
            case MesajPrimit.Telemetrie t -> laTelemetrie(t);
            case MesajPrimit.Alarma a -> laAlarma(a);
            case MesajPrimit.Stare s -> laStare(s);
        }
    }

    private void laTelemetrie(MesajPrimit.Telemetrie t) {
        Instant sosire = ceas.instant();
        PozitieDto pozitie = PozitieDto.din(t, sosire);
        pozitii.salveaza(pozitie);
        masini.vazuta(t.masinaId(), sosire);
        live.trimite("POZITIE", pozitie);
    }

    /** QoS 2 garantează că alarma vine o singură dată, deci nu caut dubluri (spec §6.3). */
    private void laAlarma(MesajPrimit.Alarma a) {
        AlarmaDto alarma = alarme.salveaza(a.masinaId(), a.tip(), "MASINA", a.ora(), ceas.instant(), detalii(a));
        live.trimite("ALARMA", alarma);
    }

    /** O stare primită din nou, de exemplu cea reținută, la o nouă abonare, nu e o schimbare: UI-ul o are deja. */
    private void laStare(MesajPrimit.Stare s) {
        Instant acum = ceas.instant();
        if (masini.schimbaStare(s.masinaId(), s.online(), acum)) {
            live.trimite("STARE", new StareMasinaDto(s.masinaId(), s.online() ? "ONLINE" : "OFFLINE", acum));
        }
    }

    static String detalii(MesajPrimit.Alarma a) {
        return switch (a.tip()) {
            case "SUPRAINCALZIRE" -> String.format(Locale.ROOT, "temperatura a ajuns la %.1f °C", a.valoare());
            case "FRANARE_BRUSCA" -> String.format(Locale.ROOT, "viteza a scăzut cu %.1f km/h într-o secundă", a.valoare());
            default -> String.format(Locale.ROOT, "valoarea %.1f", a.valoare());
        };
    }
}
