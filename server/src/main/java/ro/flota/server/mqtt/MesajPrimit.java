package ro.flota.server.mqtt;

import java.time.Instant;

/** Un mesaj de la o mașină, deja verificat de Decodor. Orice altceva nu trece mai departe. */
public sealed interface MesajPrimit {

    String masinaId();

    /** Ce trimite mașina o dată pe secundă (spec §4.2). */
    record Telemetrie(String masinaId, long seq, Instant oraMasinii, double lat, double lon,
                      double viteza, double temperatura, int baterie, boolean dinBuffer) implements MesajPrimit {
    }

    /** O alarmă dată de mașină: SUPRAINCALZIRE sau FRANARE_BRUSCA. */
    record Alarma(String masinaId, String tip, Instant ora, double valoare) implements MesajPrimit {
    }

    /** online sau offline; offline poate veni și de la broker, din Last Will. */
    record Stare(String masinaId, boolean online) implements MesajPrimit {
    }

    /** Răspunsul mașinii la o comandă: EXECUTATA sau RESPINSA, cu motiv. */
    record Confirmare(String masinaId, long comandaId, Instant ora, String rezultat, String motiv) implements MesajPrimit {
    }
}
