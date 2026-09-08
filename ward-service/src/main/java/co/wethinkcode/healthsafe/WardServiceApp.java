package co.wethinkcode.healthsafe;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import io.javalin.Javalin;

public class WardServiceApp {

    private static final String INGESTION_URL = "http://localhost:7030/records";

    public static void main(String[] args) throws IOException, InterruptedException{
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
                    //ctx.status(502).json(Map.of("error", "Could not reach ingestion service"));
                }
            });

        }).start(7031);

        // TODO (Provides lists of wards and departments.)
        // Add domain endpoints for ward-service here.

    }

    private static List<Map<String, Object>> fetchIngestionRecords() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

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
        ward.put("ward_id", record.get("ward_id"));
        ward.put("wing", record.get("wing"));
        ward.put("beds_available", record.get("beds_available"));
        ward.put("notes", record.get("notes"));
        return ward;
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
// MQ TODO: publishes to ActiveMQ queue MqConfig.QUEUE when it detects an equipment failure on one of its wards.
