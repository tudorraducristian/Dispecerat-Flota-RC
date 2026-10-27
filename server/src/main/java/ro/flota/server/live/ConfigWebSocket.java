package ro.flota.server.live;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Pune CanalLive pe /ws. Din faza 6, handshake-ul cere un token valid în /ws?token=.
 *
 * <p>Originile acceptate sînt paginile de pe laptop, inclusiv Vite pe 5173, și, din redesign (§10.2), cele din
 * rețelele private. Telefonul deschide UI-ul pe adresa laptopului din hotspot, de exemplu 192.168.137.1, iar Vite
 * trimite mai departe originea aceea; fără ea, harta de pe telefon n-ar primi date live. Spring Boot rămâne pe
 * 127.0.0.1, deci la el ajunge doar Vite. Din 172.* trec doar 172.16–172.31, restul sînt adrese publice.
 */
@Configuration
@EnableWebSocket
public class ConfigWebSocket implements WebSocketConfigurer {

    public static final String[] ORIGINI = Stream.concat(
                    Stream.of("http://localhost:*", "http://127.0.0.1:*", "http://192.168.*:*", "http://10.*:*"),
                    IntStream.rangeClosed(16, 31).mapToObj(n -> "http://172." + n + ".*:*"))
            .toArray(String[]::new);

    private final CanalLive canal;
    private final TokenLaHandshake tokenLaHandshake;

    public ConfigWebSocket(CanalLive canal, TokenLaHandshake tokenLaHandshake) {
        this.canal = canal;
        this.tokenLaHandshake = tokenLaHandshake;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(canal, "/ws")
                .addInterceptors(tokenLaHandshake)
                .setAllowedOriginPatterns(ORIGINI);
    }
}
