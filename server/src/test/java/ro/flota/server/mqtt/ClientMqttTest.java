package ro.flota.server.mqtt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Ce scrie serverul în log când pierde Mosquitto. Verific doar părțile fără diacritice, ca să nu depind de consolă. */
@ExtendWith(OutputCaptureExtension.class)
class ClientMqttTest {

    /** Registrul fazei 2: la Ctrl+C, serverul scria WARN „reîncerc singur”, deși tocmai se oprea. */
    @Test
    void oprireaCerutaDeServerNuEAvertisment(CapturedOutput iesire) {
        ClientMqtt.scrieDeconectarea(MqttDisconnectSource.USER, new RuntimeException("DISCONNECT trimis de noi"));
        assertFalse(iesire.getOut().contains("WARN"), iesire.getOut());
        assertTrue(iesire.getOut().contains("Mosquitto"), iesire.getOut());
    }

    @Test
    void legaturaPierdutaEAvertisment(CapturedOutput iesire) {
        ClientMqtt.scrieDeconectarea(MqttDisconnectSource.SERVER, new RuntimeException("Server closed connection"));
        assertTrue(iesire.getOut().contains("WARN"), iesire.getOut());
        assertTrue(iesire.getOut().contains("Server closed connection"), iesire.getOut());
    }
}
