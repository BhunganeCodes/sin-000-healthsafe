package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IngestionServiceApp.loadAndCleanWards(String), covering the
 * cleaning behaviour the current implementation actually performs:
 * casing normalisation, padding trim, and numeric validation on
 * beds_available. Each test uses its own small fixture CSV under
 * src/test/resources so it's isolated from changes to the real
 * wards-outdated.csv.
 *
 * NOTE: duplicate detection, date-format normalisation, and boolean/flag
 * normalisation are called for in the user stories but are not implemented
 * in IngestionServiceApp yet (those columns aren't even read from the CSV),
 * so there are no tests for them here. Add fixtures + tests for those once
 * the corresponding parsing logic exists.
 */
class IngestionServiceParsingTest {

    @Test
    void throwsIOExceptionWhenResourceIsMissing() {
        assertThrows(IOException.class,
                () -> IngestionServiceApp.loadAndCleanWards("does-not-exist.csv"));
    }

    @Test
    void skipsHeaderRow() throws IOException {
        List<ObjectNode> records = IngestionServiceApp.loadAndCleanWards("wards-basic.csv");

        assertEquals(1, records.size(), "header row should not be treated as data");
    }

    @Test
    void normalisesCasingAndTrimsPadding() throws IOException {
        List<ObjectNode> records = IngestionServiceApp.loadAndCleanWards("wards-basic.csv");

        ObjectNode record = records.get(0);
        assertEquals("W-01", record.get("ward_id").asText());
        assertEquals("East Wing", record.get("wing").asText(), "trailing space should be trimmed, casing title-cased");
        assertEquals("Paediatrics", record.get("department").asText(), "ALL CAPS should be normalised");
    }

    @Test
    void parsesValidNumericBedsAvailable() throws IOException {
        List<ObjectNode> records = IngestionServiceApp.loadAndCleanWards("wards-basic.csv");

        ObjectNode record = records.get(0);
        assertEquals(12, record.get("beds_available").asInt());
        assertEquals("N/A", record.get("notes").asText());
    }

    @Test
    void flagsNegativeBedsAvailableAndNullsTheValue() throws IOException {
        List<ObjectNode> records = IngestionServiceApp.loadAndCleanWards("wards-negative-beds.csv");

        ObjectNode record = records.get(0);
        assertTrue(record.get("beds_available").isNull(), "negative bed counts should be nulled out");
        assertEquals(
                "beds_available was negative ('-3') - flagged for follow up",
                record.get("notes").asText()
        );
    }

    @Test
    void flagsNonNumericBedsAvailableAndNullsTheValue() throws IOException {
        List<ObjectNode> records = IngestionServiceApp.loadAndCleanWards("wards-non-numeric-beds.csv");

        ObjectNode record = records.get(0);
        assertTrue(record.get("beds_available").isNull(), "non-numeric bed counts should be nulled out");
        assertEquals(
                "beds_available was non-numeric ('five') - flagged for follow up",
                record.get("notes").asText()
        );
    }

    @Test
    void skipsRowsWithTooFewColumns() throws IOException {
        List<ObjectNode> records = IngestionServiceApp.loadAndCleanWards("wards-malformed.csv");

        assertEquals(1, records.size(), "the malformed 3-column row should be skipped");
        assertEquals("W-05", records.get(0).get("ward_id").asText());
    }

    @Test
    void skipsBlankLines() throws IOException {
        List<ObjectNode> records = IngestionServiceApp.loadAndCleanWards("wards-blank-lines.csv");

        assertEquals(1, records.size(), "blank lines should be skipped, not counted as records");
        assertEquals("W-06", records.get(0).get("ward_id").asText());
    }
}