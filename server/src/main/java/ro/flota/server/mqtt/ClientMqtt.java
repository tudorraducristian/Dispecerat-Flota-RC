package ro.flota.server.mqtt;

import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ro.flota.server.flota.Dispecer;

/**
 * Singura clasă din server care știe de HiveMQ (D17, R4). Clientul „dispecer” se conectează direct la
 * Mosquitto, cu sesiune persistentă și reconectare automată (D11). Pornește după IncarcareDate (@Order(2)).
 */
@Component
@Order(2)
public class ClientMqtt implements ApplicationRunner, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ClientMqtt.class);

    private final Decodor decodor;
    private final Dispecer dispecer;
    /** Un singur fir: mesajele se procesează pe rând, în ordinea sosirii (D22). */
    private final ExecutorService fir = Executors.newSingleThreadExecutor(r -> new Thread(r, "mqtt-dispecer"));
    private final Mqtt3AsyncClient client;

    public ClientMqtt(Decodor decodor, Dispecer dispecer,
                      @Value("${flota.mqtt.host}") String host, @Value("${flota.mqtt.port}") int port) {
        this.decodor = decodor;
        this.dispecer = dispecer;
        this.client = Mqtt3Client.builder()
                .identifier("dispecer")
                .serverHost(host)
                .serverPort(port)
                .automaticReconnectWithDefaultConfig()
                .addConnectedListener(context -> {
                    log.info("Conectat la Mosquitto");
                    aboneaza();
                })
                .addDisconnectedListener(context ->
                        log.warn("Deconectat de la Mosquitto ({}); reîncerc singur", context.getCause().getMessage()))
                .buildAsync();
        // Filtrul global prinde și mesajele păstrate în sesiunea persistentă, care sosesc imediat după CONNACK (D22).
        client.publishes(MqttGlobalPublishFilter.ALL, this::primeste, fir);
    }

    /** Conectarea e asincronă: dacă Mosquitto e oprit, serverul pornește oricum și reîncearcă singur. */
    @Override
    public void run(ApplicationArguments args) {
        client.connectWith().cleanSession(false).keepAlive(30).send();
    }

    /** La fiecare conectare: Mosquitto fără persistență uită abonările când repornește (D22). */
    private void aboneaza() {
        client.subscribeWith()
                .addSubscription().topicFilter("flota/+/telemetrie").qos(MqttQos.AT_MOST_ONCE).applySubscription()
                .addSubscription().topicFilter("flota/+/alarma").qos(MqttQos.EXACTLY_ONCE).applySubscription()
                .addSubscription().topicFilter("flota/+/stare").qos(MqttQos.AT_LEAST_ONCE).applySubscription()
                .send()
                .whenComplete((ack, eroare) -> {
                    if (eroare != null) {
                        log.warn("Abonarea a eșuat: {}", eroare.getMessage());
                    }
                });
    }

    public boolean conectat() {
        return client.getState().isConnected();
    }

    /** Publică pe QoS 1 (spec §4.1, comenzile). Nu așteaptă; o eroare se scrie în log. */
    public void publica(String topic, byte[] payload) {
        client.publishWith()
                .topic(topic)
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(payload)
                .send()
                .whenComplete((rezultat, eroare) -> {
                    if (eroare != null) {
                        log.warn("N-am putut publica pe {}: {}", topic, eroare.getMessage());
                    }
                });
    }

    /** Un mesaj stricat se notează și se ignoră; orice altă eroare nu oprește ascultarea (spec §6.1). */
    private void primeste(Mqtt3Publish publicare) {
        String topic = publicare.getTopic().toString();
        try {
            dispecer.primeste(decodor.decodeaza(topic, publicare.getPayloadAsBytes()));
        } catch (MesajInvalid e) {
            log.warn("Ignor mesajul de pe {}: {}", topic, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Eroare la mesajul de pe {}; ascult mai departe", topic, e);
        }
    }

    @Override
    public void destroy() {
        client.disconnect();
        fir.shutdown();
    }
}
