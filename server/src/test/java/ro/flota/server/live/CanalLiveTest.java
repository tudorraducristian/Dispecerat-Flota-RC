package ro.flota.server.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class CanalLiveTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final CanalLive canal = new CanalLive(json);

    /** O sesiune falsă, făcută de Mockito: răspunde doar la ce îi spunem. */
    private static WebSocketSession sesiune(String id) {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getId()).thenReturn(id);
        when(s.isOpen()).thenReturn(true);
        return s;
    }

    @Test
    void trimiteTipulSiDatele() throws Exception {
        WebSocketSession s = sesiune("1");
        canal.afterConnectionEstablished(s);
        canal.trimite("STARE", Map.of("masinaId", "masina-3", "stare", "ONLINE"));

        ArgumentCaptor<TextMessage> trimis = ArgumentCaptor.forClass(TextMessage.class);
        verify(s).sendMessage(trimis.capture());
        JsonNode j = json.readTree(trimis.getValue().getPayload());
        assertEquals("STARE", j.get("tip").asString());
        assertEquals("masina-3", j.get("date").get("masinaId").asString());
    }

    @Test
    void oSesiuneMoartaEsteScoasaFaraSaLeStricePeCelelalte() throws Exception {
        WebSocketSession moarta = sesiune("1");
        doThrow(new IOException("conexiune închisă")).when(moarta).sendMessage(any());
        WebSocketSession vie = sesiune("2");
        canal.afterConnectionEstablished(moarta);
        canal.afterConnectionEstablished(vie);

        canal.trimite("STARE", Map.of());
        canal.trimite("STARE", Map.of());

        assertEquals(1, canal.numarSesiuni());
        verify(vie, times(2)).sendMessage(any());
    }

    @Test
    void inchidereaScoateSesiunea() throws Exception {
        WebSocketSession s = sesiune("1");
        canal.afterConnectionEstablished(s);
        canal.afterConnectionClosed(s, CloseStatus.NORMAL);
        assertEquals(0, canal.numarSesiuni());
    }
}
