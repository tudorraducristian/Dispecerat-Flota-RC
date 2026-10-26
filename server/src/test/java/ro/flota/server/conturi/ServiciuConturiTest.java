package ro.flota.server.conturi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ro.flota.server.api.CerereGresita;
import ro.flota.server.api.Conflict;
import ro.flota.server.api.Negasit;
import ro.flota.server.live.CanalLive;

class ServiciuConturiTest {

    private static final Instant T0 = Instant.parse("2026-12-10T10:00:00Z");
    private static final UtilizatorCurent EU = new UtilizatorCurent(1, "admin", Rol.ADMIN, "t");
    private static final Utilizator ANA = new Utilizator(7, "ana", "h", Rol.DISPECER, T0);

    private final Utilizatori utilizatori = mock(Utilizatori.class);
    private final Tokenuri tokenuri = mock(Tokenuri.class);
    private final CanalLive live = mock(CanalLive.class);
    private final ServiciuConturi serviciu =
            new ServiciuConturi(utilizatori, tokenuri, new Parole(), live, Clock.fixed(T0, ZoneOffset.UTC));

    @Test
    void numeleContuluiSeSalveazaCuLitereMici() {
        serviciu.creeaza(new CerereCont(" Ana ", "parola-buna", Rol.DISPECER));
        verify(utilizatori).adauga(eq("ana"), startsWith("$2a$10$"), eq(Rol.DISPECER), eq(T0));
    }

    @Test
    void numeExistentDa409() {
        when(utilizatori.dupaNume("ana")).thenReturn(Optional.of(ANA));
        assertEquals("există deja contul ana", assertThrows(Conflict.class,
                () -> serviciu.creeaza(new CerereCont("ana", "parola-buna", Rol.DISPECER))).getMessage());
    }

    @Test
    void rolLipsaDa400() {
        assertEquals("rolul e ADMIN sau DISPECER", assertThrows(CerereGresita.class,
                () -> serviciu.creeaza(new CerereCont("ana", "parola-buna", null))).getMessage());
    }

    @Test
    void nuPotiStergePropriulCont() {
        assertEquals("nu-ți poți șterge propriul cont",
                assertThrows(Conflict.class, () -> serviciu.sterge(EU, 1)).getMessage());
    }

    @Test
    void ultimulAdminNuSePoateSterge() {
        when(utilizatori.dupaId(9)).thenReturn(Optional.of(new Utilizator(9, "sef", "h", Rol.ADMIN, T0)));
        when(utilizatori.numarAdmini()).thenReturn(1);
        assertEquals("ultimul administrator nu se poate șterge",
                assertThrows(Conflict.class, () -> serviciu.sterge(EU, 9)).getMessage());
    }

    @Test
    void contInexistentDa404() {
        assertEquals("contul 99 nu există", assertThrows(Negasit.class, () -> serviciu.sterge(EU, 99)).getMessage());
    }

    @Test
    void stergereaInchideCanaleleContului() {
        when(utilizatori.dupaId(7)).thenReturn(Optional.of(ANA));
        serviciu.sterge(EU, 7);
        verify(utilizatori).sterge(7);
        verify(live).inchideSesiunileUtilizatorului(7);
    }

    @Test
    void resetareaSchimbaHashulSiScoateOmulDinAplicatie() {
        when(utilizatori.dupaId(7)).thenReturn(Optional.of(ANA));
        serviciu.reseteazaParola(EU, 7, "parola-noua");
        verify(utilizatori).schimbaParola(eq(7L), startsWith("$2a$10$"));
        verify(tokenuri).stergeAleUtilizatorului(7);
        verify(live).inchideSesiunileUtilizatorului(7);
    }

    @Test
    void adminulNuSiReseteazaPropriaParolaDeAici() {
        assertEquals("parola ta se schimbă din Setări", assertThrows(Conflict.class,
                () -> serviciu.reseteazaParola(EU, 1, "parola-noua")).getMessage());
    }
}
