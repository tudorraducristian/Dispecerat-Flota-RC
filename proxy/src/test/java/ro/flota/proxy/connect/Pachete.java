package ro.flota.proxy.connect;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Pachete CONNECT construite de mână, octet cu octet, după tabelul din spec §5.2. Le folosesc și alte pachete. */
public final class Pachete {

    private Pachete() {
    }

    public static byte[] connect(String clientId) {
        return connect(clientId, 4, new byte[0]);
    }

    static byte[] connect(String clientId, int nivel, byte[] dupaClientId) {
        ByteArrayOutputStream corp = new ByteArrayOutputStream();
        text(corp, "MQTT");
        corp.write(nivel);
        corp.write(0x02);   // flag-urile: clean session
        corp.write(0x00);   // keepalive-ul, pe 2 octeți: 5 secunde
        corp.write(0x05);
        text(corp, clientId);
        corp.writeBytes(dupaClientId);

        ByteArrayOutputStream pachet = new ByteArrayOutputStream();
        pachet.write(0x10); // tipul: CONNECT
        lungime(pachet, corp.size());
        pachet.writeBytes(corp.toByteArray());
        return pachet.toByteArray();
    }

    /** Lungimea rămasă: 7 biți pe octet, cei mai puțin semnificativi întâi; bitul de sus = „mai urmează”. */
    static void lungime(ByteArrayOutputStream out, int n) {
        do {
            int octet = n % 128;
            n /= 128;
            if (n > 0) {
                octet |= 0x80;
            }
            out.write(octet);
        } while (n > 0);
    }

    private static void text(ByteArrayOutputStream out, String s) {
        byte[] octeti = s.getBytes(StandardCharsets.UTF_8);
        out.write(octeti.length >> 8);
        out.write(octeti.length & 0xFF);
        out.writeBytes(octeti);
    }
}
