package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for the HTTP layer built by IngestionServiceApp.createApp().
 * Starts the app on an ephemeral port (0) so tests don't collide with a real
 * instance running on 7030, and serves a small hand-built set of records
 * rather than the real CSV, so these tests only exercise the routing/
 * serialisation wiring, independent of the parsing logic (covered separately
 * in IngestionServiceParsingTest).
 */
class IngestionServiceHttpTest {

    private Javalin app;
    private String baseUrl;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void startApp() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode record = mapper.createObjectNode();
        record.put("ward_id", "W-01");
        record.put("wing", "East Wing");
        record.put("department", "Paediatrics");
        record.put("beds_available", 12);
        record.put("notes", "N/A");

        app = IngestionServiceApp.createApp(List.of(record));
        app.start(0); // ephemeral port
        baseUrl = "http://localhost:" + app.port();
    }

    @AfterEach
    void stopApp() {
        app.stop();
    }

    @Test
    void healthEndpointReturnsOk() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/health")).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("OK", response.body());
    }

    @Test
    void recordsEndpointReturnsCleanedRecordsAsJson() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/records")).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode body = mapper.readTree(response.body());

        assertEquals(1, body.size());
        JsonNode record = body.get(0);
        assertEquals("W-01", record.get("ward_id").asText());
        assertEquals("East Wing", record.get("wing").asText());
        assertEquals("Paediatrics", record.get("department").asText());
        assertEquals(12, record.get("beds_available").asInt());
        assertEquals("N/A", record.get("notes").asText());
    }
}