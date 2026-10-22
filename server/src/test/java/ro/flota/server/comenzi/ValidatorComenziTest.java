package ro.flota.server.comenzi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ro.flota.server.api.CerereGresita;
import tools.jackson.databind.json.JsonMapper;

class ValidatorComenziTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static CerereComanda cerere(String tip, String parametri) {
        return new CerereComanda(tip, parametri == null ? null : JSON.readTree(parametri));
    }

    private static String eroare(String tip, String parametri) {
        return assertThrows(CerereGresita.class, () -> ValidatorComenzi.verifica(cerere(tip, parametri))).getMessage();
    }

    @Test
    void limiteazaVitezaBunaPastreazaDoarKmh() {
        assertEquals(JSON.readTree("{\"kmh\": 50}"),
                ValidatorComenzi.verifica(cerere("LIMITEAZA_VITEZA", "{\"kmh\": 50, \"altceva\": 1}")));
    }

    @Test
    void kmhGresitEsteRefuzat() {
        for (String p : List.of("{}", "{\"kmh\": 9}", "{\"kmh\": 131}", "{\"kmh\": 200}", "{\"kmh\": \"50\"}", "{\"kmh\": 50.5}")) {
            assertTrue(eroare("LIMITEAZA_VITEZA", p).contains("kmh"), p);
        }
    }

    @Test
    void intoarceLaBazaNuAreParametri() {
        assertEquals(JSON.readTree("{}"), ValidatorComenzi.verifica(cerere("INTOARCE_LA_BAZA", null)));
    }

    @Test
    void tipNecunoscutEsteRefuzat() {
        assertTrue(eroare("ZBOARA", "{}").contains("ZBOARA"));
    }

    @Test
    void tipLipsaEsteRefuzat() {
        assertTrue(eroare(null, "{}").contains("tipul"));
    }
}
