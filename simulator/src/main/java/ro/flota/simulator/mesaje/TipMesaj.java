package ro.flota.simulator.mesaje;

/** Felul unui mesaj. Bufferul aruncă doar TELEMETRIE. */
public enum TipMesaj {
    TELEMETRIE,
    ALARMA,
    STARE,
    CONFIRMARE
}
