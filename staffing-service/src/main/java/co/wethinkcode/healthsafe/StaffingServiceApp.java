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

    private static final String WARD_SERVICE = "http://localhost:7031/wards";
    private static final String ALERT_SERVICE = "http:localhost:7032/alert-level";
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

        // TODO (Provides on-call schedules for doctors based on ward and status.)
        // Add domain endpoints for staffing-service here.
    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.healthsafe.mq.MqConfig)
