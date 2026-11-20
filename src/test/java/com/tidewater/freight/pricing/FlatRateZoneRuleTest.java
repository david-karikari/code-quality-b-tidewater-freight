package com.tidewater.freight.pricing;

import static com.tidewater.freight.testsupport.Amounts.assertAmountEquals;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FlatRateZoneRuleTest {

    private final FlatRateZoneRule regional =
            new FlatRateZoneRule("REGIONAL", new BigDecimal("8"), new BigDecimal("10"));

    @Test
    void pricesRatePerKgTimesWeightPlusFixedFee() {
        // 8 x 3 + 10 = 34 (not 8 + 3 + 10 = 21, and not 8 x 3 - 10 = 14)
        assertAmountEquals("34", regional.price(new BigDecimal("3")));
    }

    @Test
    void zoneWithNoFixedFeeIsRateTimesWeightOnly() {
        FlatRateZoneRule domestic = new FlatRateZoneRule("DOMESTIC", new BigDecimal("5"), BigDecimal.ZERO);

        assertAmountEquals("50", domestic.price(new BigDecimal("10")));
    }

    @Test
    void fractionalWeightsAreExact() {
        // 8 x 0.1 + 10 = 10.8 exactly, with no floating-point residue
        assertAmountEquals("10.8", regional.price(new BigDecimal("0.1")));
    }

    @Test
    void reportsItsZoneCode() {
        assertEquals("REGIONAL", regional.zone());
    }

    @Test
    void rejectsANegativeRateAndNamesTheZone() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new FlatRateZoneRule("ARCTIC", new BigDecimal("-1"), BigDecimal.ZERO));

        assertTrue(e.getMessage().contains("ARCTIC"));
        assertTrue(e.getMessage().contains("-1"));
    }

    @Test
    void rejectsANegativeFeeAndNamesTheZone() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new FlatRateZoneRule("ARCTIC", BigDecimal.ONE, new BigDecimal("-0.5")));

        assertTrue(e.getMessage().contains("ARCTIC"));
        assertTrue(e.getMessage().contains("-0.5"));
    }

    @Test
    void acceptsAZeroRateAndAZeroFee() {
        assertDoesNotThrow(() -> new FlatRateZoneRule("FREE", BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void rejectsMissingValues() {
        assertThrows(NullPointerException.class,
                () -> new FlatRateZoneRule(null, BigDecimal.ONE, BigDecimal.ZERO));
        assertThrows(NullPointerException.class,
                () -> new FlatRateZoneRule("ARCTIC", null, BigDecimal.ZERO));
        assertThrows(NullPointerException.class,
                () -> new FlatRateZoneRule("ARCTIC", BigDecimal.ONE, null));
    }
}