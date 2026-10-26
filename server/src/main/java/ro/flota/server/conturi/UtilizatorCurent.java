package ro.flota.server.conturi;

/** Cine a trimis cererea, aflat din token. Interceptorul îl pune în cerere, sub InterceptorAutentificare.ATRIBUT. */
public record UtilizatorCurent(long id, String nume, Rol rol, String token) {
}
