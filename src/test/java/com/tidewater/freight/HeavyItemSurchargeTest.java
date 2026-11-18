package com.tidewater.freight;

import static com.tidewater.freight.Amounts.assertAmountEquals;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HeavyItemSurchargeTest {

    private final HeavyItemSurcharge surcharge = new HeavyItemSurcharge();

    @ParameterizedTest(name = "{0} kg -> surcharge {1}")
    @CsvSource({
            "0.1,   0",
            "29.99, 0",
            "30,    0",
            "30.00, 0",
            "30.01, 20",
            "31,    20",
            "250,   20"
    })
    void chargesOnlyAboveThirtyKilograms(String weightKg, String expectedSurcharge) {
        assertAmountEquals(expectedSurcharge, surcharge.forWeight(new BigDecimal(weightKg)));
    }
}