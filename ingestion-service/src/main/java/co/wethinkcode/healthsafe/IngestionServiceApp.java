package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.text.WordUtils;

public class IngestionServiceApp {

    private static final String CSV_RESOURCE = "wards-outdated.csv";

    public static void main(String[] args) throws IOException {
        List<ObjectNode> cleanedRecords = loadAndCleanWards();

        Javalin app = Javalin.create(config -> {
            config.routes.get("/health", ctx -> ctx.result("OK"));
            config.routes.get("/records", ctx -> ctx.json(cleanedRecords));
        }).start(7030);
    }

    private static List<ObjectNode> loadAndCleanWards() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<ObjectNode> results = new ArrayList<>();
        String csvSplitBy = ",";

        // Loaded from the classpath (src/main/resources) so it works
        // regardless of the working directory the JVM was launched from.
        try (InputStream is = IngestionServiceApp.class.getClassLoader().getResourceAsStream(CSV_RESOURCE)) {
            if (is == null) {
                throw new IOException("Could not find " + CSV_RESOURCE + " on the classpath");
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line = br.readLine(); // skip header row

                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }

                    String[] data = line.trim().split(csvSplitBy);
                    if (data.length < 4) {
                        System.out.println("Skipping malformed row: " + line);
                        continue;
                    }

                    for (int i = 0; i < data.length; i++) {
                        data[i] = WordUtils.capitalizeFully(data[i].trim());
                    }

                    Integer beds = null;
                    String notes;
                    try {
                        beds = Integer.parseInt(data[3]);
                        notes = "N/A";
                        if (beds < 0) {
                            notes = "beds_available was negative ('" + beds + "') - flagged for follow up";
                            beds = null;
                        }
                    } catch (NumberFormatException e) {
                        notes = "beds_available was non-numeric ('" + data[3].toLowerCase() + "') - flagged for follow up";
                    }

                    ObjectNode jsonObject = mapper.createObjectNode();
                    jsonObject.put("ward_id", data[0]);
                    jsonObject.put("wing", data[1]);
                    jsonObject.put("department", data[2]);
                    jsonObject.put("beds_available", beds);
                    jsonObject.put("notes", notes);

                    results.add(jsonObject);
                }
            }
        }

        return results;
    }
}