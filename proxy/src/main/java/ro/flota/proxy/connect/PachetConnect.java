package ro.flota.proxy.connect;

/** Primul pachet al unei mașini: cine e și octeții lui, exact cum au venit. */
public record PachetConnect(String clientId, byte[] octeti) {
}
