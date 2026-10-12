package ro.flota.simulator.masina;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import ro.flota.simulator.config.Setari;
import ro.flota.simulator.mesaje.Mesaj;
import ro.flota.simulator.mesaje.Mesaje;

/** Singura clasă din simulator care știe de biblioteca HiveMQ (D17, R4). MQTT 3.1.1. */
public final class ConexiuneHiveMq implements Conexiune {

    private static final long SECUNDE_CONECTARE = 5;
    private static final long SECUNDE_PUBLICARE = 10;

    private final String id;
    private final int keepalive;
    private final Mqtt3BlockingClient client;
    private volatile boolean conectata = false;

    public ConexiuneHiveMq(String id, Setari setari) {
        this.id = id;
        this.keepalive = setari.keepaliveSecunde();
        this.client = Mqtt3Client.builder()
                .identifier(id)
                .transportConfig()
                    .serverHost(setari.brokerHost())
                    .serverPort(setari.brokerPort())
                    .socketConnectTimeout(SECUNDE_CONECTARE, TimeUnit.SECONDS)
                    .mqttConnectTimeout(SECUNDE_CONECTARE, TimeUnit.SECONDS)
                    .applyTransportConfig()
                .addConnectedListener(context -> conectata = true)
                .addDisconnectedListener(context -> {
                    if (conectata) {
                        System.out.printf("[%s] conexiune pierdută: %s%n", id, context.getCause().getMessage());
                    }
                    conectata = false;
                })
                .buildBlocking();
    }

    @Override
    public void conecteaza() throws EroareConexiune {
        try {
            client.connectWith()
                    .cleanSession(false)
                    .keepAlive(keepalive)
                    .willPublish()
                        .topic(Mesaje.topic(id, "stare"))
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .payload("offline".getBytes(StandardCharsets.UTF_8))
                        .retain(true)
                        .applyWillPublish()
                    .send();
        } catch (RuntimeException e) {
            throw new EroareConexiune(e.getMessage(), e);
        }
    }

    @Override
    public void publica(Mesaj mesaj) throws EroareConexiune {
        try {
            client.toAsync().publishWith()
                    .topic(mesaj.topic())
                    .qos(qos(mesaj.qos()))
                    .retain(mesaj.retain())
                    .payload(mesaj.payload())
                    .send()
                    .get(SECUNDE_PUBLICARE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EroareConexiune("publicare întreruptă", e);
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            throw new EroareConexiune(e.getMessage(), e);
        }
    }

    @Override
    public boolean conectata() {
        return conectata;
    }

    @Override
    public void deconecteaza() {
        try {
            client.disconnect();
        } catch (RuntimeException e) {
            // Era deja deconectată.
        }
        conectata = false;
    }

    private static MqttQos qos(int nivel) {
        return switch (nivel) {
            case 0 -> MqttQos.AT_MOST_ONCE;
            case 1 -> MqttQos.AT_LEAST_ONCE;
            case 2 -> MqttQos.EXACTLY_ONCE;
            default -> throw new IllegalArgumentException("QoS necunoscut: " + nivel);
        };
    }
}
