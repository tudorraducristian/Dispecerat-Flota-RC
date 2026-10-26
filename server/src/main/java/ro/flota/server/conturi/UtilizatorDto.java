package ro.flota.server.conturi;

import java.time.Instant;

/** Contul, așa cum îl vede UI-ul: fără hash-ul parolei. */
public record UtilizatorDto(long id, String nume, Rol rol, Instant creatLa) {

    public static UtilizatorDto din(Utilizator u) {
        return new UtilizatorDto(u.id(), u.nume(), u.rol(), u.creatLa());
    }
}
