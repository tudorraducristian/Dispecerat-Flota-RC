package ro.flota.server.live;

import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import ro.flota.server.conturi.ServiciuAutentificare;
import ro.flota.server.conturi.UtilizatorCurent;

/**
 * Paznicul de la ușa canalului live (spec §16.2): verifică token-ul o singură dată, la handshake. Browserul nu
 * poate pune header-e la WebSocket, de aceea token-ul vine în adresă, ca /ws?token=.
 */
@Component
public class TokenLaHandshake implements HandshakeInterceptor {

    private final ServiciuAutentificare serviciu;

    public TokenLaHandshake(ServiciuAutentificare serviciu) {
        this.serviciu = serviciu;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest cerere, ServerHttpResponse raspuns, WebSocketHandler handler,
                                   Map<String, Object> atribute) {
        String token = UriComponentsBuilder.fromUri(cerere.getURI()).build().getQueryParams().getFirst("token");
        Optional<UtilizatorCurent> eu = serviciu.valideaza(token);
        if (eu.isEmpty()) {
            raspuns.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        // Atributele rămân pe sesiune; după ele CanalLive găsește canalele de închis la log off.
        atribute.put("utilizatorId", eu.get().id());
        atribute.put("token", token);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest cerere, ServerHttpResponse raspuns, WebSocketHandler handler,
                               Exception exceptie) {
        // Nimic de făcut după.
    }
}
