package ro.flota.server.live;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
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
 * Canalul live spre UI (spec §6.6): un handler simplu, fără STOMP. Token-ul îl verifică TokenLaHandshake, o dată,
 * la deschidere (faza 6). Serverul doar trimite; ce scrie browserul pe canal se ignoră.
 */
@Component
public class CanalLive extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CanalLive.class);
    private static final int TIMP_MAXIM_TRIMITERE_MS = 2_000;
    private static final int COADA_MAXIMA_OCTETI = 512 * 1024;
    /**
     * Codurile 4000–4999 sînt lăsate aplicațiilor (RFC 6455, §7.4.2). Cu 4001, UI-ul știe că sesiunea s-a încheiat
     * de tot și nu mai încearcă să se reconecteze, cum face după o cădere a serverului.
     */
    static final CloseStatus SESIUNE_INCHEIATA = new CloseStatus(4001, "sesiune încheiată");

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

    /** La log off sau la expirare: închide canalele deschise cu token-ul ăsta. */
    public void inchideSesiunileTokenului(String token) {
        inchideUnde(s -> token.equals(s.getAttributes().get("token")));
    }

    /** La ștergerea unui cont sau la resetarea parolei lui: omul e scos și din canalul live. */
    public void inchideSesiunileUtilizatorului(long id) {
        inchideUnde(s -> Long.valueOf(id).equals(s.getAttributes().get("utilizatorId")));
    }

    /** Decoratorul dă mai departe atributele puse de TokenLaHandshake; sesiunile fără ele nu se potrivesc. */
    private void inchideUnde(Predicate<WebSocketSession> potrivita) {
        for (WebSocketSession s : sesiuni) {
            if (potrivita.test(s)) {
                sesiuni.remove(s);
                inchideLinistit(s, SESIUNE_INCHEIATA);
            }
        }
    }

    private static void inchideLinistit(WebSocketSession s) {
        inchideLinistit(s, CloseStatus.SERVER_ERROR);
    }

    private static void inchideLinistit(WebSocketSession s, CloseStatus stare) {
        try {
            s.close(stare);
        } catch (IOException | RuntimeException e) {
            // Era deja închisă.
        }
    }
}
