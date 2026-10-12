package ro.flota.simulator.masina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import ro.flota.simulator.config.DefinitieMasina;
import ro.flota.simulator.config.Setari;
import ro.flota.simulator.mesaje.TipMesaj;
import ro.flota.simulator.testutil.Trasee;

class MasinaTest {

    private static final Setari RAPID = new Setari("127.0.0.1", 1883, 5, 100);
    private final DefinitieMasina def = new DefinitieMasina("masina-1", Trasee.patrat(), 0.0, "z1");

    private static List<String> ultimele2(List<String> evenimente) {
        return evenimente.subList(evenimente.size() - 2, evenimente.size());
    }

    @Test
    void oprireaAnuntaOfflineInainteDeDeconectare() throws Exception {
        ConexiuneFalsa conexiune = new ConexiuneFalsa();
        conexiune.conecteazaDirect();
        new Masina(def, RAPID, conexiune, Clock.systemUTC(), new Random(1)).opreste();
        assertEquals(List.of("publică flota/masina-1/stare offline", "deconectată"), ultimele2(conexiune.evenimente));
    }

    @Test
    void pornitaTrimiteTelemetrieSiSeOpresteCurat() throws Exception {
        ConexiuneFalsa conexiune = new ConexiuneFalsa();
        Masina masina = new Masina(def, RAPID, conexiune, Clock.systemUTC(), new Random(1));
        masina.porneste();
        long limita = System.currentTimeMillis() + 3_000;
        while (conexiune.publicate.stream().noneMatch(m -> m.tip() == TipMesaj.TELEMETRIE)
                && System.currentTimeMillis() < limita) {
            Thread.sleep(20);
        }
        masina.opreste();
        assertTrue(conexiune.publicate.stream().anyMatch(m -> m.tip() == TipMesaj.TELEMETRIE),
                "nicio telemetrie în 3 secunde");
        assertEquals(List.of("publică flota/masina-1/stare offline", "deconectată"), ultimele2(conexiune.evenimente));
    }
}
