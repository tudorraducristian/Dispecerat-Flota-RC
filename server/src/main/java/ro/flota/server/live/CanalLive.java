package ro.flota.server.live;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * Canalul live spre UI (spec §6.6): un handler simplu, fără STOMP și fără token.
 * Serverul doar trimite; ce scrie browserul pe canal se ignoră.
 */
@Component
public class CanalLive extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CanalLive.class);
    private static final int TIMP_MAXIM_TRIMITERE_MS = 2_000;
    private static final int COADA_MAXIMA_OCTETI = 512 * 1024;

    private final JsonMapper json;
    private final Set<WebSocketSession> sesiuni = ConcurrentHashMap.newKeySet();

    public CanalLive(JsonMapper json) {
        this.json = json;
    }

    /**
     * O sesiune WebSocket nu suportă două trimiteri în același timp, iar aici trimit mai multe fire.
     * Decoratorul le pune la rând: un singur fir trimite, iar celelalte își lasă mesajul în coadă și pleacă.
     * Limitele (2 s, 512 KB) le verifică doar un fir care găsește o trimitere în curs; dacă sînt depășite,
     * închide sesiunea. Firul care trimite efectiv rămâne însă blocat cât durează trimiterea spre un browser
     * lent, iar dacă nu mai vine niciun mesaj, nimeni nu verifică limitele. Cu zece mașini, mesajele vin des.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession sesiune) {
        sesiuni.add(new ConcurrentWebSocketSessionDecorator(sesiune, TIMP_MAXIM_TRIMITERE_MS, COADA_MAXIMA_OCTETI));
        log.info("UI conectat ({} conexiuni)", sesiuni.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession sesiune, CloseStatus stare) {
        sesiuni.removeIf(s -> s.getId().equals(sesiune.getId()));
    }

    public void trimite(String tip, Object date) {
        TextMessage mesaj = new TextMessage(json.writeValueAsString(new MesajLive(tip, date)));
        for (WebSocketSession s : sesiuni) {
            try {
                s.sendMessage(mesaj);
            } catch (IOException | RuntimeException e) {
                sesiuni.remove(s);
                inchideLinistit(s);
                log.info("Scot o conexiune UI moartă: {}", e.getMessage());
            }
        }
    }

    public int numarSesiuni() {
        return sesiuni.size();
    }

    /** La log off: închide canalele deschise cu token-ul ăsta. Corpul vine în Task 4 al fazei 6. */
    public void inchideSesiunileTokenului(String token) {
    }

    /** La ștergerea unui cont sau la resetarea parolei lui. Corpul vine în Task 4 al fazei 6. */
    public void inchideSesiunileUtilizatorului(long id) {
    }

    private static void inchideLinistit(WebSocketSession s) {
        try {
            s.close(CloseStatus.SERVER_ERROR);
        } catch (IOException | RuntimeException e) {
            // Era deja închisă.
        }
    }
}
