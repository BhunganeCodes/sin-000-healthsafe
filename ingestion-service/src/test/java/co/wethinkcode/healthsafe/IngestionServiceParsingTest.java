package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IngestionServiceApp.loadAndCleanWards(String), covering the
 * cleaning behaviour the current implementation actually performs:
 * casing normalisation, padding trim, numeric validation (negative /
 * non-numeric / unrealistically large) on beds_available, department
 * spelling-variant normalisation, and duplicate ward_id merging.
 * Each test uses its own small fixture CSV under src/test/resources so
 * it's isolated from changes to the real wards-outdated.csv.
 *
 * NOTE: date-format normalisation and boolean/flag normalisation are
 * called for in the user stories but are not implemented in
 * IngestionServiceApp yet (no such columns are read from the CSV at all),
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

    @Test
    void mergesDuplicateWardIdsKeepingTheValidBedCount() throws IOException {
        List<ObjectNode> records = IngestionServiceApp.loadAndCleanWards("wards-duplicate.csv");

        assertEquals(1, records.size(), "two rows for the same ward should merge into one record");
        ObjectNode record = records.get(0);
        assertEquals("W-05", record.get("ward_id").asText());
        assertEquals(5, record.get("beds_available").asInt(), "the valid bed count from either row should win");
        assertEquals("N/A (duplicate ward_id merged)", record.get("notes").asText());
    }

    @Test
    void flagsConflictingDuplicateBedCountsInsteadOfPickingOneSilently() throws IOException {
        List<ObjectNode> records = IngestionServiceApp.loadAndCleanWards("wards-duplicate-conflict.csv");

        assertEquals(1, records.size());
        ObjectNode record = records.get(0);
        assertEquals(4, record.get("beds_available").asInt(), "first-seen valid value is kept when both rows disagree");
        assertEquals(
                "duplicate ward_id with conflicting beds_available values - flagged for follow up (duplicate ward_id merged)",
                record.get("notes").asText()
        );
    }

    @Test
    void flagsUnrealisticallyLargeBedsAvailableAndNullsTheValue() throws IOException {
        List<ObjectNode> records = IngestionServiceApp.loadAndCleanWards("wards-unrealistic-beds.csv");

        ObjectNode record = records.get(0);
        assertTrue(record.get("beds_available").isNull(), "an implausibly large bed count should be nulled out");
        assertEquals(
                "beds_available was unrealistically large ('2023') - flagged for follow up",
                record.get("notes").asText()
        );
    }

    @Test
    void normalisesAmericanSpellingDepartmentVariants() throws IOException {
        List<ObjectNode> records = IngestionServiceApp.loadAndCleanWards("wards-spelling-variant.csv");

        assertEquals("Paediatrics", records.get(0).get("department").asText(),
                "American 'Pediatrics' spelling should normalise to the canonical 'Paediatrics'");
    }
}