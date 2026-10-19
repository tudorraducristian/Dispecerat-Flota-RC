package ro.flota.proxy.ordine;

/** O linie din protocolul text, fără \r\n. Dacă a depășit 200 de caractere, textul e gol (D24). */
public record Linie(String text, boolean preaLunga) {
}
