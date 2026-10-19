package ro.flota.proxy.connect;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Citește primul pachet de pe o conexiune nouă, care trebuie să fie CONNECT din MQTT 3.1.1 (spec §5.2).
 * E singurul loc din proiect care se uită înăuntrul unui pachet MQTT.
 */
public final class CititorConnect {

    static final int TIP_CONNECT = 0x10;
    static final int NIVEL_MQTT_311 = 4;
    static final int LUNGIME_MAXIMA = 64 * 1024;

    private CititorConnect() {
    }

    /**
     * Citește exact un pachet, niciun octet în plus: restul conexiunii rămâne pentru Legatura.
     * Întoarce client ID-ul și octeții pachetului, neschimbați, ca să plece mai departe la broker.
     */
    public static PachetConnect citeste(InputStream in) throws IOException {
        CopieStream copie = new CopieStream(in);
        int tip = copie.read();
        if (tip < 0) {
            throw new EOFException("conexiunea s-a închis înainte de CONNECT");
        }
        if (tip != TIP_CONNECT) {
            throw new PachetStricat(String.format("primul pachet nu e CONNECT (0x%02X)", tip));
        }
        int lungime = lungimeRamasa(copie);
        if (lungime > LUNGIME_MAXIMA) {
            throw new PachetStricat("CONNECT ar avea " + lungime + " octeți; maximul e " + LUNGIME_MAXIMA);
        }
        byte[] rest = copie.readNBytes(lungime);
        if (rest.length < lungime) {
            throw new PachetStricat("CONNECT tăiat: " + rest.length + " din " + lungime + " octeți");
        }
        DataInputStream corp = new DataInputStream(new ByteArrayInputStream(rest));
        try {
            String protocol = text(corp);
            if (!protocol.equals("MQTT")) {
                throw new PachetStricat("protocolul e \"" + protocol + "\", nu MQTT");
            }
            int nivel = corp.readUnsignedByte();
            if (nivel != NIVEL_MQTT_311) {
                throw new PachetStricat("nivelul protocolului e " + nivel + ", aștept 4 (MQTT 3.1.1)");
            }
            corp.readUnsignedByte();  // flag-urile conexiunii
            corp.readUnsignedShort(); // keepalive-ul
            String clientId = text(corp);
            if (clientId.isEmpty()) {
                throw new PachetStricat("client ID-ul e gol");
            }
            return new PachetConnect(clientId, copie.octeti());
        } catch (EOFException e) {
            throw new PachetStricat("CONNECT se termină înainte de client ID");
        }
    }

    /**
     * Lungimea rămasă: câte 7 biți pe octet, cei mai puțin semnificativi întâi. Bitul de sus, 0x80,
     * spune că mai urmează un octet. Cel mult 4 octeți.
     */
    static int lungimeRamasa(InputStream in) throws IOException {
        int valoare = 0;
        int multiplicator = 1;
        for (int i = 0; i < 4; i++) {
            int b = in.read();
            if (b < 0) {
                throw new EOFException("lungimea rămasă e tăiată");
            }
            valoare += (b & 0x7F) * multiplicator;
            if ((b & 0x80) == 0) {
                return valoare;
            }
            multiplicator *= 128;
        }
        throw new PachetStricat("lungimea rămasă are mai mult de 4 octeți");
    }

    /** Un text MQTT: lungimea pe 2 octeți, cel mai semnificativ întâi, apoi textul în UTF-8. */
    private static String text(DataInputStream in) throws IOException {
        int n = in.readUnsignedShort();
        byte[] octeti = in.readNBytes(n);
        if (octeti.length < n) {
            throw new EOFException();
        }
        return new String(octeti, StandardCharsets.UTF_8);
    }
}
