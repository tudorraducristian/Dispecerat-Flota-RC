package ro.flota.server.conturi;

/** Ce trimite pagina Conturi la POST /api/utilizatori (endpoint-ul 18). */
public record CerereCont(String nume, String parola, Rol rol) {
}
