package ro.flota.server.flota;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Textul SQL trimis de Alarme. Pe MySQL adevărat se verifică pe viu, prin REST. */
class AlarmeTest {

    private final JdbcClient jdbc = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
    private final Alarme alarme = new Alarme(jdbc);

    @Test
    void listaCuTipFiltreazaSiDupaTip() {
        alarme.lista(false, "FRANARE_BRUSCA");
        verify(jdbc).sql("SELECT * FROM alarme WHERE vazuta = :vazute AND tip = :tip "
                + "ORDER BY ora_sosirii DESC, id DESC LIMIT 200");
    }

    @Test
    void listaDoarCuTip() {
        alarme.lista(null, "FRANARE_BRUSCA");
        verify(jdbc).sql("SELECT * FROM alarme WHERE tip = :tip ORDER BY ora_sosirii DESC, id DESC LIMIT 200");
    }

    @Test
    void listaFaraFiltreRamaneCaAcum() {
        alarme.lista(null, null);
        verify(jdbc).sql("SELECT * FROM alarme ORDER BY ora_sosirii DESC, id DESC LIMIT 200");
    }

    @Test
    void toateVazuteCuTip() {
        alarme.marcheazaToateVazute("X");
        verify(jdbc).sql("UPDATE alarme SET vazuta = TRUE WHERE vazuta = FALSE AND tip = :tip");
    }

    @Test
    void toateVazuteFaraTip() {
        alarme.marcheazaToateVazute(null);
        verify(jdbc).sql("UPDATE alarme SET vazuta = TRUE WHERE vazuta = FALSE");
    }
}
