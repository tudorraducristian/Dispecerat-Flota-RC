package ro.flota.server.live;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Pune CanalLive pe /ws. Originile acceptate sînt doar paginile de pe laptop, inclusiv Vite pe 5173. */
@Configuration
@EnableWebSocket
public class ConfigWebSocket implements WebSocketConfigurer {

    private final CanalLive canal;

    public ConfigWebSocket(CanalLive canal) {
        this.canal = canal;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(canal, "/ws").setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }
}
