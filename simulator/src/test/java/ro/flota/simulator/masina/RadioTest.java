package ro.flota.simulator.masina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.Json;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;
import ro.flota.simulator.mesaje.Telemetrie;
import ro.flota.simulator.mesaje.TipMesaj;

class RadioTest {

    private final ConexiuneFalsa conexiune = new ConexiuneFalsa();
    private final BlockingDeque<Mesaj> coada = new LinkedBlockingDeque<>();
    private final List<Long> somnuri = new ArrayList<>();
    private final Radio radio = new Radio("masina-1", conexiune, coada, somnuri::add);

    private static Mesaj telemetrie(long seq) {
        return Mesaje.telemetrie("masina-1",
                new Telemetrie(seq, "2026-12-10T10:15:03.120Z", 46, 23, 50, 90, 100, false));
    }

    private List<Long> seqTrimise() {
        return conexiune.publicate.stream()
                .filter(m -> m.tip() == TipMesaj.TELEMETRIE)
                .map(m -> Json.MAPPER.readTree(m.payload()).get("seq").asLong())
                .toList();
    }

    @Test
    void primulPasSeConecteazaSiAnuntaOnline() throws Exception {
        radio.pas();
        assertEquals(1, conexiune.conectari);
        Mesaj stare = conexiune.publicate.get(0);
        assertEquals(TipMesaj.STARE, stare.tip());
        assertEquals("online", stare.text());
        assertTrue(stare.retain());
    }

    @Test
    void golesteCoadaInOrdine() throws Exception {
        coada.add(telemetrie(1));
        coada.add(telemetrie(2));
        radio.pas();
        radio.pas();
        radio.pas();
        assertEquals(List.of(1L, 2L), seqTrimise());
        assertTrue(coada.isEmpty());
    }

    @Test
    void brokerIndisponibilLaPornire() throws Exception {
        conexiune.esueazaLaConectare(2);
        coada.add(telemetrie(1));
        radio.pas();
        radio.pas();
        assertEquals(List.of(5_000L, 5_000L), somnuri);
        assertTrue(conexiune.publicate.isEmpty());
        radio.pas(); // a treia încercare reușește
        radio.pas();
        assertEquals(3, conexiune.conectari);
        assertEquals(List.of(1L), seqTrimise());
    }

    @Test
    void mesajulNuSePierdeCandPublicareaEsueaza() throws Exception {
        radio.pas(); // conectat
        Mesaj primul = telemetrie(1);
        coada.add(primul);
        coada.add(telemetrie(2));
        conexiune.esueazaLaPublicare(1);
        radio.pas();
        assertSame(primul, coada.peekFirst());
        assertEquals(List.of(1_000L), somnuri);
        conexiune.pierdeConexiunea();
        radio.pas(); // reconectare și „online"
        radio.pas();
        radio.pas();
        assertEquals(List.of(1L, 2L), seqTrimise());
    }

    @Test
    void oprireaIeseDinBucla() throws Exception {
        Radio real = new Radio("masina-1", conexiune, coada, Thread::sleep);
        Thread fir = new Thread(real);
        fir.start();
        long limita = System.currentTimeMillis() + 2_000;
        while (!conexiune.conectata() && System.currentTimeMillis() < limita) {
            Thread.sleep(10);
        }
        real.opreste();
        fir.join(3_000);
        assertFalse(fir.isAlive());
    }
}
