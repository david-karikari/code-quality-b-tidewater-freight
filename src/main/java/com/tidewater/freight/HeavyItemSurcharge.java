package com.tidewater.freight;

import java.math.BigDecimal;

public final class HeavyItemSurcharge {

    public static final BigDecimal THRESHOLD_KG = new BigDecimal("30");
    public static final BigDecimal AMOUNT = new BigDecimal("20");

    public BigDecimal forWeight(BigDecimal weightKg) {
        return weightKg.compareTo(THRESHOLD_KG) > 0 ? AMOUNT : BigDecimal.ZERO;
    }
}
