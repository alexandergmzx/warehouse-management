package com.alexandergomez.wms.mfc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * TELEGRAMS.md gate 1 proof: every example payload it references
 * ({@code src/test/resources/telegrams/}) parses and carries the fields the
 * contract documents. No Spring context needed — this is pure JSON shape
 * verification, not a behavioral test.
 */
class TelegramFixturesIT {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void transportMissionFixtureParsesWithDocumentedFields() throws IOException {
        JsonNode telegram = readFixture("transport-mission.json");
        assertTrue(telegram.get("missionId").isIntegralNumber());
        assertNotNull(java.util.UUID.fromString(telegram.get("eventId").asString()));
        assertEquals("TRANSPORT", telegram.get("missionType").asString());
        assertTrue(telegram.get("orderId").isIntegralNumber());
        assertFalse(telegram.get("orderNumber").asString().isBlank());
        assertFalse(telegram.get("sourceLocationCode").asString().isBlank());
        assertFalse(telegram.get("destinationLocationCode").asString().isBlank());
        assertEquals("DISPATCHED", telegram.get("state").asString());
        assertNotNull(OffsetDateTime.parse(telegram.get("dispatchedAt").asString()));
    }

    @Test
    void confirmationFixturesParseWithDocumentedStates() throws IOException {
        assertConfirmationFixture("confirmation-accepted.json", "ACCEPTED", false);
        assertConfirmationFixture("confirmation-completed.json", "COMPLETED", false);
        assertConfirmationFixture("confirmation-failed.json", "FAILED", true);
    }

    private void assertConfirmationFixture(String fileName, String expectedState, boolean reasonRequired)
            throws IOException {
        JsonNode confirmation = readFixture(fileName);
        assertEquals(expectedState, confirmation.get("state").asString());
        assertNotNull(OffsetDateTime.parse(confirmation.get("occurredAt").asString()));
        if (reasonRequired) {
            assertTrue(confirmation.has("reason") && !confirmation.get("reason").asString().isBlank(),
                    fileName + " must carry a non-blank reason (TELEGRAMS.md: required when state is FAILED)");
        }
    }

    private JsonNode readFixture(String fileName) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/telegrams/" + fileName)) {
            assertNotNull(in, "fixture not found: " + fileName);
            return mapper.readTree(in);
        }
    }
}
