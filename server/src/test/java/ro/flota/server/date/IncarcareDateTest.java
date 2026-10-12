package ro.flota.server.date;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

class IncarcareDateTest {

    /**
     * Dacă serverul s-a oprit înaintea mașinilor, în bază au rămas ONLINE. La pornire nu știe cine e online;
     * starea adevărată vine imediat după abonare, din mesajele reținute (spec §4.1).
     */
    @Test
    void laPornireToateMasinileIncepOffline() {
        JdbcClient jdbc = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
        new IncarcareDate(jdbc, JsonMapper.builder().build(), new RegistruFlota(), "../date").run(null);
        verify(jdbc).sql("UPDATE masini SET stare = 'OFFLINE'");
    }
}
