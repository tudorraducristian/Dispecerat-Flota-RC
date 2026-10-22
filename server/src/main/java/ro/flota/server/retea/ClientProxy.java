package ro.flota.server.retea;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ro.flota.server.api.ServiciuIndisponibil;

/**
 * Clientul protocolului text al proxy-ului (spec §5.4, §6.5): câte o conexiune TCP pe 1885 pentru fiecare
 * cerere. Dacă proxy-ul nu răspunde în 2 secunde, aruncă ServiciuIndisponibil, care devine 503.
 */
@Component
public class ClientProxy {

    static final int TIMP_MAXIM_MS = 2_000;

    private final String host;
    private final int port;

    public ClientProxy(@Value("${flota.proxy.host}") String host, @Value("${flota.proxy.port}") int port) {
        this.host = host;
        this.port = port;
    }

    /** Trimite o linie și întoarce răspunsul: un rând, sau, la STARE, toate rândurile până la „.”, fără el. */
    public List<String> trimite(String linie) {
        boolean stare = linie.trim().equalsIgnoreCase("STARE");
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), TIMP_MAXIM_MS);
            s.setSoTimeout(TIMP_MAXIM_MS);
            Writer out = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.US_ASCII);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII));
            out.write(linie + "\n");
            out.flush();
            List<String> raspuns = new ArrayList<>();
            String rand;
            while ((rand = in.readLine()) != null) {
                if (stare && rand.equals(".")) {
                    return raspuns;
                }
                raspuns.add(rand);
                if (!stare) {
                    return raspuns;
                }
            }
            throw new ServiciuIndisponibil("proxy-ul a închis conexiunea înainte să răspundă");
        } catch (IOException e) {
            throw new ServiciuIndisponibil("proxy indisponibil: " + e.getMessage());
        }
    }
}
