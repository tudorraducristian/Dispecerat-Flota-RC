package ro.flota.simulator.masina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.Json;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;
import ro.flota.simulator.mesaje.Telemetrie;
import ro.flota.simulator.mesaje.TipMesaj;
import ro.flota.simulator.testutil.AleatorScriptat;

class RadioTest {

    private final ConexiuneFalsa conexiune = new ConexiuneFalsa();
    private final Buffer coada = new Buffer(300);
    private final List<Long> somnuri = new ArrayList<>();

    /** Cu 0.5, factorul la întâmplare e exact 1, deci pauzele sînt 1, 2, 4... secunde. */
    private static PauzaReconectare pauzaFixa() {
        return new PauzaReconectare(new AleatorScriptat(0.5));
    }

    private final Radio radio = new Radio("masina-1", conexiune, coada, pauzaFixa(), somnuri::add);

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
        coada.adauga(telemetrie(1));
        coada.adauga(telemetrie(2));
        radio.pas();
        radio.pas();
        radio.pas();
        assertEquals(List.of(1L, 2L), seqTrimise());
        assertEquals(0, coada.marime());
    }

    @Test
    void brokerIndisponibilLaPornire() throws Exception {
        conexiune.esueazaLaConectare(3);
        coada.adauga(telemetrie(1));
        radio.pas();
        radio.pas();
        radio.pas();
        assertEquals(List.of(1_000L, 2_000L, 4_000L), somnuri);
        assertTrue(conexiune.publicate.isEmpty());
        radio.pas(); // a patra încercare reușește
        radio.pas();
        assertEquals(4, conexiune.conectari);
        assertEquals(List.of(1L), seqTrimise());
    }

    @Test
    void dupaConectarePauzaOIaDeLaCapat() throws Exception {
        conexiune.esueazaLaConectare(2);
        radio.pas();
        radio.pas();
        radio.pas(); // conectată la a treia
        conexiune.pierdeConexiunea();
        conexiune.esueazaLaConectare(1);
        radio.pas();
        assertEquals(List.of(1_000L, 2_000L, 1_000L), somnuri);
    }

    @Test
    void mesajulNuSePierdeCandPublicareaEsueaza() throws Exception {
        radio.pas(); // conectat
        Mesaj primul = telemetrie(1);
        coada.adauga(primul);
        coada.adauga(telemetrie(2));
        conexiune.esueazaLaPublicare(1);
        radio.pas();
        assertSame(primul, coada.continut().getFirst());
        assertEquals(List.of(1_000L), somnuri);
        conexiune.pierdeConexiunea();
        radio.pas(); // reconectare și „online”
        radio.pas();
        radio.pas();
        assertEquals(List.of(1L, 2L), seqTrimise());
    }

    private static Mesaj alarma() {
        return new Mesaj(TipMesaj.ALARMA, "flota/masina-1/alarma", "{}".getBytes(), 2, false);
    }

    /** Bug-ul din review-ul fazei 3: alarma QoS 2 ajungea de două ori după o reconectare. */
    @Test
    void mesajulTinutDeClientNuMaiPleacaSiDinBuffer() throws Exception {
        radio.pas(); // conectat
        coada.adauga(alarma());
        conexiune.neconfirmaLaPublicare(1);
        radio.pas();
        assertEquals(0, coada.marime(), "clientul MQTT îl retrimite singur; o copie din buffer ar fi o alarmă dublă");
        assertEquals(List.of(1_000L), somnuri);
    }

    /** Broker-ul repornit fără persistență a uitat sesiunea, deci clientul renunță: alarma nu are voie să se piardă. */
    @Test
    void mesajulLaCareClientulRenuntaRevinePrimulInBuffer() throws Exception {
        radio.pas(); // conectat
        Mesaj alarma = alarma();
        coada.adauga(alarma);
        coada.adauga(telemetrie(1));
        conexiune.neconfirmaLaPublicare(1);
        radio.pas();
        conexiune.renuntaLaMesajelePastrate();
        assertEquals(2, coada.marime());
        assertSame(alarma, coada.continut().getFirst());
    }

    @Test
    void onlineEsuatInseamnaReconectare() throws Exception {
        conexiune.esueazaLaPublicare(1);
        radio.pas();
        assertFalse(conexiune.conectata(), "fără „online”, mașina nu are voie să rămână conectată");
        radio.pas();
        assertEquals(2, conexiune.conectari);
        assertEquals("online", conexiune.publicate.get(0).text());
    }

    @Test
    void oprireaIeseDinBucla() throws Exception {
        Radio real = new Radio("masina-1", conexiune, coada, pauzaFixa(), Thread::sleep);
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
