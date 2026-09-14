package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import jakarta.jms.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import co.wethinkcode.healthsafe.mq.MqConfig;


public class WardServiceApp {

    private static final String INGESTION_URL = "http://localhost:7030/records";
    private static final ObjectMapper mapper = new ObjectMapper();

    private static Connection mqConnection;
    private static Session mqSession;
    private static MessageProducer mqProducer;

    public static void main(String[] args) throws Exception {
        Javalin app = Javalin.create(config -> {
            config.routes.get("/health", ctx -> ctx.result("OK"));
            config.routes.get("/wards", ctx -> {
                try {
                    List<Map<String, Object>> allRecords = fetchIngestionRecords();

                    List<Map<String, Object>> wardsOnly = allRecords.stream()
                            .map(WardServiceApp::toWardView)
                            .collect(Collectors.toList());

                    ctx.json(wardsOnly);
                } catch (Exception e) {
                    e.printStackTrace();
                    ctx.status(502).json(Map.of("error", "Could not reach ingestion service"));
                }
            });

            config.routes.get("/wards/{id}", ctx -> {
                String id = ctx.pathParam("id");

                try {
                    List<Map<String, Object>> allRecords = fetchIngestionRecords();
                    Optional<Map<String, Object>> match = allRecords.stream()
                            .filter(record -> id.equalsIgnoreCase(String.valueOf(record.get("ward_id"))))
                            .findFirst();
                    if (match.isEmpty()) {
                        ctx.status(404).json(Map.of("error", "No ward found with ID: " + id));
                        return;
                    }

                    ctx.json(toWardView(match.get()));
                } catch (Exception e) {
                    e.printStackTrace();
                    ctx.status(502).json(Map.of("error", "Could not reach ingestion service"));
                }
            });

        }).start(7031);

        // TODO (Provides lists of wards and departments.)
        // Add domain endpoints for ward-service here.
        setupMessageQueue();
    }

    private static List<Map<String, Object>> fetchIngestionRecords() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(INGESTION_URL))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ingestion returned: " + response.statusCode());
        }

        return mapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {});
    }

    private static Map<String, Object> toWardView(Map<String, Object> record) {
        Map<String, Object> ward = new HashMap<>();
        ward.put("department", record.get("department"));
        return ward;
    }

    private static void setupMessageQueue() throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
        mqConnection = factory.createConnection();
        mqConnection.start();
        mqSession = mqConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Topic topic = mqSession.createTopic(MqConfig.TOPIC);
        MessageConsumer consumer = mqSession.createConsumer(topic);
        consumer.setMessageListener(WardServiceApp::onStaffingEventMessage);

        Queue queue = (Queue) mqSession.createQueue(MqConfig.QUEUE);
        mqProducer = mqSession.createProducer((Destination) queue);
    }


    private static void onStaffingEventMessage(jakarta.jms.Message message) {
        try {
            if (message instanceof TextMessage textMessage) {
                String body = textMessage.getText();
                System.out.println("Received staffing update: " + body);
            }

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private static void publishEquipmentFailure(String wardId, String description) {
        try {
            String payload = mapper.writeValueAsString(Map.of(
                    "ward_id", wardId,
                    "description", description,
                    "timestamp", System.currentTimeMillis()
            ));
            TextMessage message = mqSession.createTextMessage(payload);
            mqProducer.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
// MQ TODO: publishes to ActiveMQ queue MqConfig.QUEUE when it detects an equipment failure on one of its wards.
