package ro.flota.server.conturi;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PrimulAdminTest {

    private static final Instant T0 = Instant.parse("2026-12-10T10:00:00Z");

    private final Utilizatori utilizatori = mock(Utilizatori.class);
    private final PrimulAdmin primul = new PrimulAdmin(utilizatori, new Parole(), Clock.fixed(T0, ZoneOffset.UTC));

    @Test
    void faceAdminCandNuExistaNiciunul() {
        when(utilizatori.numarAdmini()).thenReturn(0);
        primul.run(null);
        verify(utilizatori).adauga(eq("admin"), startsWith("$2a$10$"), eq(Rol.ADMIN), eq(T0));
    }

    @Test
    void nuFaceNimicCandExistaUnAdmin() {
        when(utilizatori.numarAdmini()).thenReturn(1);
        primul.run(null);
        verify(utilizatori, never()).adauga(any(), any(), any(), any());
    }

    @Test
    void parolaAleatoareAre16LitereSauCifre() {
        assertTrue(PrimulAdmin.parolaAleatoare(new SecureRandom()).matches("[A-Za-z0-9]{16}"));
    }
}
