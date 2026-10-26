package ro.flota.server.conturi;

/** Răspunsul la login (endpoint-ul 13): token-ul, plus cine e omul, ca UI-ul să nu mai ceară o dată. */
public record RaspunsLogin(String token, String nume, Rol rol) {
}
