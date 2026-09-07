package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.text.WordUtils;

public class IngestionServiceApp {

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.routes.get("/health", ctx -> ctx.result("OK"));
        }).start(7030);

        // TODO: read and clean src/main/resources/wards-outdated.csv (wards, wings, specialist departments data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.
        String csvFile = "src/main/resources/wards-outdated.csv";
        String line = "";
        String csvSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonObject = mapper.createObjectNode();
            ArrayList<String> results = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                String[] data = line.trim().split(csvSplitBy);
                String notes;
                Integer beds = null;

                if (data.length > 0) {
                    for (int i = 0; i < data.length; i++) {
                        data[i] = WordUtils.capitalizeFully(data[i].trim());
                    }
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
                    jsonObject.put("ward_id", data[0]);
                    jsonObject.put("wing", data[1]);
                    jsonObject.put("department", data[2]);
                    jsonObject.put("beds_available", beds);
                    jsonObject.put("notes", notes);

                    String jsonString = mapper.writeValueAsString(jsonObject);
                    results.add(jsonString);
                    //System.out.println(jsonString);
                }
            }

            for (int i=1; i < results.toArray().length; i++) {
                System.out.println(results.toArray()[i]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
