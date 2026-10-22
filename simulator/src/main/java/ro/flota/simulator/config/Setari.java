package ro.flota.simulator.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Setările de rețea ale simulatorului, din simulator.properties. */
public record Setari(String brokerHost, int brokerPort, int keepaliveSecunde, long perioadaMs, int marimeBuffer) {

    public static Setari implicite() {
        Properties p = new Properties();
        try (InputStream in = Setari.class.getResourceAsStream("/simulator.properties")) {
            if (in == null) {
                throw new IllegalStateException("lipsește simulator.properties din resurse");
            }
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return din(p);
    }

    public static Setari din(Properties p) {
        return new Setari(
                obligatoriu(p, "broker.host"),
                intreg(p, "broker.port", 1, 65_535),
                intreg(p, "keepalive.secunde", 1, 3_600),
                intreg(p, "perioada.ms", 10, 60_000),
                intreg(p, "buffer.marime", 10, 100_000));
    }

    private static String obligatoriu(Properties p, String cheie) {
        String valoare = p.getProperty(cheie);
        if (valoare == null || valoare.isBlank()) {
            throw new IllegalArgumentException("simulator.properties: lipsește " + cheie);
        }
        return valoare.trim();
    }

    private static int intreg(Properties p, String cheie, int minim, int maxim) {
        String valoare = obligatoriu(p, cheie);
        int n;
        try {
            n = Integer.parseInt(valoare);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("simulator.properties: " + cheie + " nu e număr: " + valoare);
        }
        if (n < minim || n > maxim) {
            throw new IllegalArgumentException(
                    "simulator.properties: " + cheie + " trebuie să fie între " + minim + " și " + maxim);
        }
        return n;
    }
}
