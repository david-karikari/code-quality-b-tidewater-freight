package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;

/** Test helper: compares money by numeric value, ignoring scale (50 equals 50.00). */
final class Amounts {

    private Amounts() {
    }

    static void assertAmountEquals(String expected, BigDecimal actual) {
        assertNotNull(actual, "amount must not be null");
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }
}