package ro.flota.server.conturi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import ro.flota.server.api.CerereGresita;
import ro.flota.server.api.Neautorizat;
import ro.flota.server.live.CanalLive;

class ServiciuAutentificareTest {

    private static final Instant T0 = Instant.parse("2026-12-10T10:00:00Z");

    private final Utilizatori utilizatori = mock(Utilizatori.class);
    private final Tokenuri tokenuri = mock(Tokenuri.class);
    private final CanalLive live = mock(CanalLive.class);
    private final Parole parole = new Parole();
    private final ServiciuAutentificare serviciu =
            new ServiciuAutentificare(utilizatori, tokenuri, parole, live, Clock.fixed(T0, ZoneOffset.UTC));

    @BeforeEach
    void contulAna() {
        Utilizator ana = new Utilizator(1, "ana", parole.hash("parola-buna"), Rol.DISPECER, T0);
        when(utilizatori.dupaNume("ana")).thenReturn(Optional.of(ana));
        when(utilizatori.dupaId(1)).thenReturn(Optional.of(ana));
    }

    @Test
    void loginBunDaUnTokenDe43DeCaractereValabil8Ore() {
        RaspunsLogin r = serviciu.login("ana", "parola-buna");
        assertTrue(r.token().matches("[A-Za-z0-9_-]{43}"));
        assertEquals(new RaspunsLogin(r.token(), "ana", Rol.DISPECER), r);
        verify(tokenuri).salveaza(r.token(), 1L, T0, T0.plus(Duration.ofHours(8)));
    }

    @Test
    void loginStergeIntaiTokenurileExpirate() {
        serviciu.login("ana", "parola-buna");
        InOrder ordine = inOrder(tokenuri);
        ordine.verify(tokenuri).stergeExpirate(T0);
        ordine.verify(tokenuri).salveaza(any(), anyLong(), any(), any());
    }

    @Test
    void numeNecunoscutSiParolaGresitaDauAcelasiMesaj() {
        assertEquals("nume sau parolă greșită",
                assertThrows(Neautorizat.class, () -> serviciu.login("ion", "parola-buna")).getMessage());
        assertEquals("nume sau parolă greșită",
                assertThrows(Neautorizat.class, () -> serviciu.login("ana", "alta-parola")).getMessage());
    }

    @Test
    void numeleSeCautaFaraMajusculeSiSpatii() {
        serviciu.login("  Ana ", "parola-buna");
        verify(utilizatori).dupaNume("ana");
    }

    /** Review-ul final: BCrypt nu primește peste 72 de octeți; o parolă lungă tastată la login e doar greșită. */
    @Test
    void parolaPeste72DeOctetiLaLoginDa401() {
        assertEquals("nume sau parolă greșită",
                assertThrows(Neautorizat.class, () -> serviciu.login("ana", "ș".repeat(37))).getMessage());
        assertEquals("nume sau parolă greșită",
                assertThrows(Neautorizat.class, () -> serviciu.login("ion", "a".repeat(100))).getMessage());
    }

    @Test
    void parolaVechePeste72DeOctetiDa400() {
        UtilizatorCurent eu = new UtilizatorCurent(1, "ana", Rol.DISPECER, "t");
        assertEquals("parola veche e greșită", assertThrows(CerereGresita.class,
                () -> serviciu.schimbaParola(eu, "a".repeat(100), "parola-noua")).getMessage());
    }

    @Test
    void logoutStergeTokenulSiInchideCanalele() {
        serviciu.logout(new UtilizatorCurent(1, "ana", Rol.DISPECER, "t"));
        verify(tokenuri).sterge("t");
        verify(live).inchideSesiunileTokenului("t");
    }

    @Test
    void tokenGolNuAjungeLaBaza() {
        assertEquals(Optional.empty(), serviciu.valideaza(null));
        assertEquals(Optional.empty(), serviciu.valideaza(" "));
        verifyNoInteractions(tokenuri);
    }

    @Test
    void schimbaParolaCereParolaVeche() {
        UtilizatorCurent eu = new UtilizatorCurent(1, "ana", Rol.DISPECER, "t");
        assertEquals("parola veche e greșită",
                assertThrows(CerereGresita.class, () -> serviciu.schimbaParola(eu, "gresita-1", "parola-noua")).getMessage());
        serviciu.schimbaParola(eu, "parola-buna", "parola-noua");
        verify(utilizatori).schimbaParola(eq(1L), startsWith("$2a$10$"));
    }

    @Test
    void schimbaParolaVerificaParolaNoua() {
        UtilizatorCurent eu = new UtilizatorCurent(1, "ana", Rol.DISPECER, "t");
        assertThrows(CerereGresita.class, () -> serviciu.schimbaParola(eu, "parola-buna", "scurta"));
    }
}
