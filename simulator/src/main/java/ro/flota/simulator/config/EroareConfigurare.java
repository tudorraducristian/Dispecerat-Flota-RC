package ro.flota.simulator.config;

import java.nio.file.Path;

/** Un fișier din date\ e greșit. Mesajul începe mereu cu numele fișierului. */
public final class EroareConfigurare extends RuntimeException {

    public EroareConfigurare(Path fisier, String motiv) {
        super(fisier.getFileName() + ": " + motiv);
    }
}
