package ro.flota.server.live;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Pune CanalLive pe /ws. Originile acceptate sînt doar paginile de pe laptop, inclusiv Vite pe 5173.
 * Din faza 6, handshake-ul cere un token valid în /ws?token=.
 */
@Configuration
@EnableWebSocket
public class ConfigWebSocket implements WebSocketConfigurer {

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
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }
}
