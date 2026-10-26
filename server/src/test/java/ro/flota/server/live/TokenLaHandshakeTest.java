package ro.flota.server.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;
import ro.flota.server.conturi.Rol;
import ro.flota.server.conturi.ServiciuAutentificare;
import ro.flota.server.conturi.UtilizatorCurent;

/** Cererea vine dintr-un MockHttpServletRequest, prin ServletServerHttpRequest, ca la un handshake adevărat. */
class TokenLaHandshakeTest {

    private final ServiciuAutentificare serviciu = mock(ServiciuAutentificare.class);
    private final TokenLaHandshake handshake = new TokenLaHandshake(serviciu);
    private final MockHttpServletResponse servletRaspuns = new MockHttpServletResponse();
    private final ServerHttpResponse raspuns = new ServletServerHttpResponse(servletRaspuns);
    private final WebSocketHandler handler = mock(WebSocketHandler.class);
    private final Map<String, Object> atribute = new HashMap<>();

    TokenLaHandshakeTest() {
        when(serviciu.valideaza("t")).thenReturn(Optional.of(new UtilizatorCurent(1, "admin", Rol.ADMIN, "t")));
    }

    private static ServerHttpRequest cerere(String query) {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", "/ws");
        r.setQueryString(query);
        return new ServletServerHttpRequest(r);
    }

    @Test
    void faraTokenRefuzaCu401() throws Exception {
        assertFalse(handshake.beforeHandshake(cerere(""), raspuns, handler, atribute));
        assertEquals(401, servletRaspuns.getStatus());
    }

    @Test
    void tokenInvalidRefuza() throws Exception {
        assertFalse(handshake.beforeHandshake(cerere("token=necunoscut"), raspuns, handler, atribute));
    }

    @Test
    void tokenValidPuneUtilizatorulInAtribute() throws Exception {
        assertTrue(handshake.beforeHandshake(cerere("token=t"), raspuns, handler, atribute));
        assertEquals(Map.of("utilizatorId", 1L, "token", "t"), atribute);
    }
}
