package ro.flota.server.date;

import java.nio.file.Path;

/** Un fișier din date\ e greșit. Mesajul începe mereu cu numele fișierului. */
public final class EroareDate extends RuntimeException {

    public EroareDate(Path fisier, String motiv) {
        super(fisier.getFileName() + ": " + motiv);
    }
}
