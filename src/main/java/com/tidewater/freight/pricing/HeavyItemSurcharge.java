package com.tidewater.freight.pricing;

import java.math.BigDecimal;

/** Flat surcharge for shipments heavier than {@link #THRESHOLD_KG}. Exactly the threshold is not heavy. */
public final class HeavyItemSurcharge {

    public static final BigDecimal THRESHOLD_KG = new BigDecimal("30");
    public static final BigDecimal AMOUNT = new BigDecimal("20");

    public BigDecimal forWeight(BigDecimal weightKg) {
        return weightKg.compareTo(THRESHOLD_KG) > 0 ? AMOUNT : BigDecimal.ZERO;
    }
}
