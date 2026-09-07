package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class IngestionServiceApp {

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7030);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO: read and clean src/main/resources/wards-outdated.csv (wards, wings, specialist departments data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.
        String csvFile = "src/main/resources/wards-outdated.csv";
        String line = "";
        String csvSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            HashMap<String, String> map = new HashMap<>();

            while ((line = br.readLine()) != null) {
                String[] data = line.trim().split(csvSplitBy);

                if (data.length > 0) {
                    for (int i = 0; i < data.length; i++) {
                        data[i] = data[i].trim().toLowerCase();
                    }
                    System.out.println("Ward ID: " + data[0]);
                    System.out.println("Wing: " + data[1]);
                    System.out.println("Department: " + data[2]);
                    map.put("ward_id", data[0]);
                    map.put("wing", data[1]);
                    map.put("department", data[2]);
                    map.put("beds_available", data[3]);
                    // System.out.println("Beds Available: " + data[3]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
