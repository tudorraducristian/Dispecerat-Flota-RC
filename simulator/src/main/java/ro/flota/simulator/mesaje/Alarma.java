package ro.flota.simulator.mesaje;

/** Ce trimite mașina pe flota/<id>/alarma (spec §4.2). */
public record Alarma(TipAlarma tip, String ora, double valoare) {
}
