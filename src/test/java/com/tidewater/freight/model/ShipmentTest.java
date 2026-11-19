package com.tidewater.freight.model;

import static com.tidewater.freight.testsupport.Amounts.assertAmountEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import com.tidewater.freight.exception.InvalidShipmentException;
import com.tidewater.freight.exception.InvalidWeightException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ShipmentTest {

    private static final BigDecimal TWO_KG = new BigDecimal("2");

    @Test
    void keepsTheValuesItWasCreatedWith() {
        Shipment shipment = new Shipment("DOMESTIC", TWO_KG, ServiceLevel.EXPRESS);

        assertEquals("DOMESTIC", shipment.zone());
        assertSame(TWO_KG, shipment.weightKg());
        assertEquals(ServiceLevel.EXPRESS, shipment.serviceLevel());
    }

    @Test
    void acceptsAVerySmallPositiveWeight() {
        Shipment shipment = new Shipment("DOMESTIC", new BigDecimal("0.001"), ServiceLevel.STANDARD);

        assertAmountEquals("0.001", shipment.weightKg());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsAMissingOrBlankZoneAndSaysWhatElseWasGiven(String zone) {
        InvalidShipmentException e = assertThrows(InvalidShipmentException.class,
                () -> new Shipment(zone, TWO_KG, ServiceLevel.EXPRESS));

        assertTrue(e.getMessage().contains("2"));
        assertTrue(e.getMessage().contains("EXPRESS"));
    }

    @ParameterizedTest(name = "weight {0} is rejected")
    @ValueSource(strings = {"0", "0.00", "-0.1", "-10"})
    void rejectsZeroAndNegativeWeights(String weightKg) {
        BigDecimal weight = new BigDecimal(weightKg);

        InvalidWeightException e = assertThrows(InvalidWeightException.class,
                () -> new Shipment("DOMESTIC", weight, ServiceLevel.STANDARD));

        assertEquals("DOMESTIC", e.getZone());
        assertAmountEquals(weightKg, e.getWeightKg());
        assertTrue(e.getMessage().contains("DOMESTIC"));
        assertTrue(e.getMessage().contains(weightKg));
    }

    @Test
    void rejectsAMissingWeight() {
        InvalidWeightException e = assertThrows(InvalidWeightException.class,
                () -> new Shipment("DOMESTIC", null, ServiceLevel.STANDARD));

        assertEquals("DOMESTIC", e.getZone());
        assertNull(e.getWeightKg());
    }

    @Test
    void rejectsAMissingServiceLevelAndSaysWhatElseWasGiven() {
        InvalidShipmentException e = assertThrows(InvalidShipmentException.class,
                () -> new Shipment("DOMESTIC", TWO_KG, null));

        assertTrue(e.getMessage().contains("DOMESTIC"));
        assertTrue(e.getMessage().contains("2"));
    }

    @Test
    void reportsTheMissingZoneFirstWhenEverythingIsMissing() {
        assertThrows(InvalidShipmentException.class, () -> new Shipment(null, null, null));
    }
}