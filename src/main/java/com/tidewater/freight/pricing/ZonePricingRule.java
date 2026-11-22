package com.tidewater.freight.pricing;

import java.math.BigDecimal;

public interface ZonePricingRule {

    String zone();

    BigDecimal price(BigDecimal weightKg);
}
