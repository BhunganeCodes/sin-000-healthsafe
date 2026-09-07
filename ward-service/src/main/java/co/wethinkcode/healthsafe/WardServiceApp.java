package co.wethinkcode.healthsafe;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import io.javalin.Javalin;

public class WardServiceApp {

    public static void main(String[] args) throws IOException, InterruptedException{
        Javalin app = Javalin.create(config -> {
            config.routes.get("/health", ctx -> ctx.result("OK"));
        //wards and departments lists

        }).start(7031);

        // TODO (Provides lists of wards and departments.)
        // Add domain endpoints for ward-service here.
        String INGESTION_URL = "http://localhost:7030/records";

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(INGESTION_URL))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.toString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ingestion returned: " + response.statusCode());
        }
        ObjectMapper mapper = new ObjectMapper();
        String[] respArray = response.body().split(",");

        for (String res : respArray) {
            System.out.println(res.toString());
        }
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
// MQ TODO: publishes to ActiveMQ queue MqConfig.QUEUE when it detects an equipment failure on one of its wards.
