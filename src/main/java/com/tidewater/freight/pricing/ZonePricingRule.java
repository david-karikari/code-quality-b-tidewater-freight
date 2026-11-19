package com.tidewater.freight.pricing;

import java.math.BigDecimal;

/**
 * The pricing rules of one destination zone. A new zone is added by registering
 * another implementation; existing rules and the calculator do not change.
 */

public interface ZonePricingRule {

    String zone();

    BigDecimal price(BigDecimal weightKg);
}
