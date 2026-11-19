package com.tidewater.freight.model;

import static com.tidewater.freight.testsupport.Amounts.assertAmountEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ServiceLevelTest {

    @Test
    void standardLeavesTheCostUnchanged() {
        assertAmountEquals("50", ServiceLevel.STANDARD.apply(new BigDecimal("50")));
    }

    @Test
    void expressMultipliesTheCostByOnePointFive() {
        assertAmountEquals("75", ServiceLevel.EXPRESS.apply(new BigDecimal("50")));
    }

    @Test
    void expressOnALargeCostIsExact() {
        assertAmountEquals("967.5", ServiceLevel.EXPRESS.apply(new BigDecimal("645")));
    }

    @Test
    void applyDoesNotRound() {
        // rounding happens once, in the calculator, not here
        assertAmountEquals("0.015", ServiceLevel.EXPRESS.apply(new BigDecimal("0.01")));
    }
}