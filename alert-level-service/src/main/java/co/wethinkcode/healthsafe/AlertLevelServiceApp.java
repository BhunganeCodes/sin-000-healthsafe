package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AlertLevelServiceApp {

    private static final int MIN_LEVEL = 0;
    private static final int MAX_LEVEL = 8;

    private static final AtomicInteger currentLevel = new AtomicInteger(MIN_LEVEL);

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7032);

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/alert-level", ctx -> ctx.json(Map.of("level", currentLevel.get())));

        // TODO (Tracks the hospital Emergency Status (0-8, 8 = full Code Blue).)
        // Add domain endpoints for alert-level-service here.
    }
}
