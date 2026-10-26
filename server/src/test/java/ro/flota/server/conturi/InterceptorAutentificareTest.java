package ro.flota.server.conturi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import ro.flota.server.api.Interzis;
import ro.flota.server.api.Neautorizat;

class InterceptorAutentificareTest {

    private static final UtilizatorCurent ADMIN_CURENT = new UtilizatorCurent(1, "admin", Rol.ADMIN, "a");
    private static final UtilizatorCurent DISPECER_CURENT = new UtilizatorCurent(2, "ana", Rol.DISPECER, "d");

    /** Două metode de controller: una pentru oricine e logat, una doar pentru admin. */
    static class ControllerDeTest {
        public void obisnuita() {
        }

        @DoarAdmin
        public void deAdmin() {
        }
    }

    private final ServiciuAutentificare serviciu = mock(ServiciuAutentificare.class);
    private final InterceptorAutentificare interceptor = new InterceptorAutentificare(serviciu);
    private final MockHttpServletResponse raspuns = new MockHttpServletResponse();
    private HandlerMethod obisnuita;
    private HandlerMethod deAdmin;

    @BeforeEach
    void pregateste() throws NoSuchMethodException {
        when(serviciu.valideaza(anyString())).thenReturn(Optional.empty());
        when(serviciu.valideaza("a")).thenReturn(Optional.of(ADMIN_CURENT));
        when(serviciu.valideaza("d")).thenReturn(Optional.of(DISPECER_CURENT));
        ControllerDeTest c = new ControllerDeTest();
        obisnuita = new HandlerMethod(c, ControllerDeTest.class.getMethod("obisnuita"));
        deAdmin = new HandlerMethod(c, ControllerDeTest.class.getMethod("deAdmin"));
    }

    private static MockHttpServletRequest cerere(String header) {
        MockHttpServletRequest c = new MockHttpServletRequest("GET", "/api/masini");
        if (header != null) {
            c.addHeader("Authorization", header);
        }
        return c;
    }

    @Test
    void faraHeaderDa401() {
        assertEquals("trebuie să te autentifici",
                assertThrows(Neautorizat.class, () -> interceptor.preHandle(cerere(null), raspuns, obisnuita)).getMessage());
    }

    @Test
    void headerFaraBearerDa401() {
        assertThrows(Neautorizat.class, () -> interceptor.preHandle(cerere("Basic abc"), raspuns, obisnuita));
    }

    @Test
    void tokenNecunoscutDa401() {
        assertThrows(Neautorizat.class, () -> interceptor.preHandle(cerere("Bearer necunoscut"), raspuns, obisnuita));
    }

    @Test
    void dispecerPeMetodaDeAdminDa403() {
        assertEquals("doar administratorul are voie",
                assertThrows(Interzis.class, () -> interceptor.preHandle(cerere("Bearer d"), raspuns, deAdmin)).getMessage());
    }

    @Test
    void adminulTreceSiPrimesteUtilizatorulInCerere() throws Exception {
        MockHttpServletRequest c = cerere("Bearer a");
        assertTrue(interceptor.preHandle(c, raspuns, deAdmin));
        assertEquals(ADMIN_CURENT, c.getAttribute(InterceptorAutentificare.ATRIBUT));
    }

    @Test
    void dispecerulTrecePeMetodaObisnuita() throws Exception {
        assertTrue(interceptor.preHandle(cerere("Bearer d"), raspuns, obisnuita));
    }
}
