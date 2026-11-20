package com.tidewater.freight.pricing;

import java.math.BigDecimal;

/** Flat surcharge for shipments heavier than {@link #THRESHOLD_KG}. Exactly the threshold is not heavy. */
public interface ZonePricingRule {

    String zone();

    BigDecimal price(BigDecimal weightKg);
}
