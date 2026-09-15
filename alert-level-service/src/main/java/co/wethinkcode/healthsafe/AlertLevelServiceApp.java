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

        app.put("alert-level", ctx -> {
            Map<?, ?> body;
            try {
                body = ctx.bodyAsClass(Map.class);
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "Request body must be JSON with int 'level' field"));
                return;
            }

            Object rawLevel = body.get("level");
            if (!(rawLevel instanceof Number)) {
                ctx.status(400).json(Map.of("error", "level must be a number between " + MIN_LEVEL + " and " + MAX_LEVEL));
                return;
            }

            int newLevel = ((Number) rawLevel).intValue();
            if (newLevel < MIN_LEVEL || newLevel > MAX_LEVEL) {
                ctx.status(400).json(Map.of("error", "level must be between 0 and 8"));
                return;
            }

            currentLevel.set(newLevel);
            ctx.json(Map.of("level", currentLevel));
        });

        // TODO (Tracks the hospital Emergency Status (0-8, 8 = full Code Blue).)
        // Add domain endpoints for alert-level-service here.
    }
}
