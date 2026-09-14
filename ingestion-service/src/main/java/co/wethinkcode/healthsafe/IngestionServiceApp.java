package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.text.WordUtils;

public class IngestionServiceApp {

    private static final String CSV_RESOURCE = "wards-outdated.csv";
    private static final int MAX_BEDS = 100;

    public static void main(String[] args) throws IOException {
        List<ObjectNode> cleanedRecords = loadAndCleanWards(CSV_RESOURCE);

        createApp(cleanedRecords).start(7030);
    }

    static Javalin createApp(List<ObjectNode> cleanedRecords) {
        return Javalin.create(config -> {
            config.routes.get("/health", ctx -> ctx.result("OK"));
            config.routes.get("/records", ctx -> ctx.json(cleanedRecords));
        });
    }

    protected static List<ObjectNode> loadAndCleanWards(String s) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, ObjectNode> resultsByWardsId = new LinkedHashMap<>();
        String csvSplitBy = ",";

        // Loaded from the classpath (src/main/resources) so it works
        // regardless of the working directory the JVM was launched from.
        try (InputStream is = IngestionServiceApp.class.getClassLoader().getResourceAsStream(s)) {
            if (is == null) {
                throw new IOException("Could not find " + s + " on the classpath");
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
                        } else if (beds > MAX_BEDS) {
                            notes = "beds_available was unrealistically large ('" + beds + "') - flagged for follow up";
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

                    String wardId = data[0];
                    ObjectNode existing = resultsByWardsId.get(wardId);
                    if (existing == null) {
                        resultsByWardsId.put(wardId, jsonObject);
                    } else {
                        mergeDuplicate(existing, jsonObject);
                    }
                }
            }
        }

        return new ArrayList<>(resultsByWardsId.values());
    }

    private static void mergeDuplicate(ObjectNode existing, ObjectNode duplicate) {
        boolean existingHasBeds = existing.hasNonNull("beds_available");
        boolean duplicateHasBeds = duplicate.hasNonNull("beds_available");

        if (!existingHasBeds && duplicateHasBeds) {
            existing.set("beds_available", duplicate.get("beds_available"));
            existing.put("notes", "N/A");
        } else if (existingHasBeds && duplicateHasBeds
                && existing.get("beds_available").asInt() != duplicate.get("beds_available").asInt()) {
            existing.put("notes", "duplicate ward_id with conflicting beds_available values - flagged for follow up");
        }

        String mergedFlag = " (duplicate ward_id merged)";
        String currentNotes = existing.get("notes").asText();
        if (!currentNotes.endsWith(mergedFlag)) {
            existing.put("notes", currentNotes + mergedFlag);
        }
    }

}