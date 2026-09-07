package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

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

            while ((line = br.readLine()) != null) {
                String[] data = line.trim().split(csvSplitBy);

                if (data.length > 0) {
                    for (int i = 0; i < data.length; i++) {
                        data[i] = data[i].trim();
                    }
                    System.out.println(data[0]);
                    System.out.println(data[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
