package ro.flota.server.flota;

/** Cifrele din antetul paginii Alarme și din meniu. Lista e tăiată la 200, deci UI-ul nu le poate număra singur. */
public record NumarAlarme(long nevazute, long azi) {
}
