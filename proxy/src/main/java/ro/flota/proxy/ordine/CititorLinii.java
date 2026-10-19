package ro.flota.proxy.ordine;

import java.io.IOException;
import java.io.Reader;

/**
 * Citește linii terminate cu \n; se acceptă și \r\n (spec §5.4). O linie are cel mult 200 de caractere.
 * Ce trece de limită nu se ține în memorie, ci se aruncă până la capătul liniei.
 */
public final class CititorLinii {

    public static final int MAXIM = 200;

    private final Reader in;

    public CititorLinii(Reader in) {
        this.in = in;
    }

    /** Următoarea linie, sau null dacă conexiunea s-a închis înainte de orice caracter. */
    public Linie urmatoarea() throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean depasita = false;
        int c;
        while ((c = in.read()) >= 0 && c != '\n') {
            if (sb.length() <= MAXIM) {
                sb.append((char) c); // cel mult 201 caractere: 200, plus un \r
            } else {
                depasita = true;
            }
        }
        if (c < 0 && sb.length() == 0 && !depasita) {
            return null;
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\r') {
            sb.setLength(sb.length() - 1);
        }
        boolean preaLunga = depasita || sb.length() > MAXIM;
        return new Linie(preaLunga ? "" : sb.toString(), preaLunga);
    }
}
