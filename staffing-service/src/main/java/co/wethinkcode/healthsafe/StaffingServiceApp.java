package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StaffingServiceApp {

    private static final String WARD_SERVICE_URL = "http://localhost:7031/wards";
    private static final String ALERT_LEVEL_URL = "http:localhost:7032/alert-level";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();

    //placeholder on-call roster for testing, keyed by department
    private static final Map<String, List<String>> ON_CALL_ROSTER = Map.of(
            "cardiology", List.of("Dr. Patel", "Dr. Nguyen", "Dr. Okafor"),
            "paediatrics", List.of("Dr. Smith", "Dr. Lee", "Dr. Garcia"),
            "oncology", List.of("Dr. Brown", "Dr. Kim"),
            "radiology", List.of("Dr. Chen", "Dr. Adeyemi"),
            "maternity", List.of("Dr. Johnson", "Dr. Rossi"),
            "icu", List.of("Dr. Novak", "Dr. Haddad", "Dr. Yamamoto")
    );

    private static final List<String> FALLBACK_ROSTER = List.of("Dr. On-Call");

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7033);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/schedule/{wardId}", ctx -> {
            String wardId = ctx.pathParam("wardId");

            Map<String, Object> ward;
            try {
                ward = fetchWard(wardId);
            } catch (WardNotFoundException e) {
                ctx.status(404).json(Map.of("error", "No ward found with id '" + wardId + "'"));
                return;
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(502).json(Map.of("error", "Could not reach ward-service"));
                return;
            }

            int alertLevel;
            try {
                alertLevel = fetchAlertLevel();
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(502).json(Map.of("error", "Could not reach alert-level-service"));
                return;
            }

            List<String> onCall = buildOnCallList(String.valueOf(ward.get("department")), alertLevel);

            ctx.json(Map.of(
                    "ward_id", ward.get("ward_id"),
                    "department", ward.get("department"),
                    "alert_level", alertLevel,
                    "on_call", onCall
            ));
        });

        // TODO (Provides on-call schedules for doctors based on ward and status.)
        // Add domain endpoints for staffing-service here.
    }

    private static Map<String, Object> fetchWard(String wardId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WARD_SERVICE_URL + wardId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            throw new WardNotFoundException(wardId);
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("ward-service returned: " + response.statusCode());
        }

        return mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
    }

    private static int fetchAlertLevel() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ALERT_LEVEL_URL))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("alert-level-service returned: " + response.statusCode());
        }

        Map<String, Object> body = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        return ((Number) body.get("level")).intValue();
    }

    /**
     * Sizes the on-call list from the department roster based on the current
     * Emergency Status: higher status pulls in more doctors from the same
     * department roster (capped at the roster's actual size).
     */
    private static List<String> buildOnCallList(String department, int alertLevel) {
        List<String> roster = ON_CALL_ROSTER.getOrDefault(
                department == null ? "" : department.toLowerCase(),
                FALLBACK_ROSTER
        );

        int count = Math.min(roster.size(), 1 + (alertLevel / 2));
        return roster.subList(0, count);
    }

    private static class WardNotFoundException extends RuntimeException {
        WardNotFoundException(String wardId) {
            super("No ward found with id '" + wardId + "'");
        }
    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
