package ro.flota.proxy.transport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import ro.flota.proxy.vreme.Vreme;

class SensTest {

    private final Vreme vreme = new Vreme(Clock.systemUTC());
    private final PipedOutputStream intrare = new PipedOutputStream();
    private final ByteArrayOutputStream iesire = new ByteArrayOutputStream();
    private final CountDownLatch sfarsit = new CountDownLatch(1);

    private Sens sens() throws IOException {
        PipedInputStream dinIntrare = new PipedInputStream(intrare, 64 * 1024);
        return new Sens("test", dinIntrare, iesire, vreme, sfarsit::countDown, new Random(7));
    }

    private static void asteapta(BooleanSupplier conditie, long ms) throws InterruptedException {
        long limita = System.currentTimeMillis() + ms;
        while (!conditie.getAsBoolean() && System.currentTimeMillis() < limita) {
            Thread.sleep(10);
        }
    }

    @Test
    void octetiiAjungInOrdineCuIntarziereSiVariatie() throws Exception {
        vreme.intarziere(50, 40);
        Sens s = sens();
        s.porneste();
        byte[] trimis = new byte[2_000];
        for (int i = 0; i < trimis.length; i++) {
            trimis[i] = (byte) i;
            intrare.write(trimis[i]);
            intrare.flush();
            if (i % 100 == 0) {
                Thread.sleep(1);
            }
        }
        asteapta(() -> iesire.size() == trimis.length, 3_000);
        assertArrayEquals(trimis, iesire.toByteArray());
        intrare.close();
        assertTrue(sfarsit.await(2, TimeUnit.SECONDS), "laSfarsit n-a fost chemat la închidere");
    }

    @Test
    void connectPleacaPrimul() throws Exception {
        Sens s = sens();
        s.trimiteIntai("CONNECT".getBytes(StandardCharsets.US_ASCII));
        s.porneste();
        intrare.write("PUBLISH".getBytes(StandardCharsets.US_ASCII));
        intrare.flush();
        asteapta(() -> iesire.size() == 14, 2_000);
        assertEquals("CONNECTPUBLISH", iesire.toString(StandardCharsets.US_ASCII));
        intrare.close();
    }

    @Test
    void gauraTineOctetiiPanaTrece() throws Exception {
        vreme.gaura(1);
        Sens s = sens();
        s.porneste();
        intrare.write("abc".getBytes(StandardCharsets.US_ASCII));
        intrare.flush();
        Thread.sleep(500);
        assertEquals(0, iesire.size(), "în gaură nu trebuie să treacă nimic");
        asteapta(() -> iesire.size() == 3, 2_000);
        assertEquals("abc", iesire.toString(StandardCharsets.US_ASCII));
        intrare.close();
    }

    @Test
    void laOInchidereCurataUltimiiOctetiPleacaInainteDeSfarsit() throws Exception {
        vreme.intarziere(200, 0);
        Sens s = sens();
        s.porneste();
        intrare.write("DISCONNECT".getBytes(StandardCharsets.US_ASCII));
        intrare.flush();
        intrare.close();
        assertTrue(sfarsit.await(2, TimeUnit.SECONDS), "laSfarsit n-a fost chemat la închidere");
        assertEquals("DISCONNECT", iesire.toString(StandardCharsets.US_ASCII), "laSfarsit a venit înaintea ultimilor octeți");
    }

    @Test
    void inGauraInchidereaAruncaOctetiiPeLoc() throws Exception {
        vreme.gaura(5);
        Sens s = sens();
        s.porneste();
        intrare.write("abc".getBytes(StandardCharsets.US_ASCII));
        intrare.flush();
        intrare.close();
        assertTrue(sfarsit.await(1, TimeUnit.SECONDS), "în gaură, închiderea trebuie anunțată pe loc (spec §5.3)");
        assertEquals(0, iesire.size(), "octeții ținuți în gaură se aruncă");
    }
}
