package ro.flota.proxy.connect;

import java.io.IOException;

/** Primul pachet de pe o conexiune nu e un CONNECT bun. Proxy-ul închide conexiunea și scrie motivul. */
public final class PachetStricat extends IOException {

    public PachetStricat(String motiv) {
        super(motiv);
    }
}
