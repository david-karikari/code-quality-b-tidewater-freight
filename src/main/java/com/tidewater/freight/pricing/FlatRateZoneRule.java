package com.tidewater.freight.pricing;

import java.math.BigDecimal;

public final class FlatRateZoneRule implements ZonePricingRule {

    private final String zone;
    private final BigDecimal ratePerKg;
    private final BigDecimal fixedFee;

    public FlatRateZoneRule(String zone, BigDecimal ratePerKg, BigDecimal fixedFee) {
        this.zone = zone;
        this.ratePerKg = ratePerKg;
        this.fixedFee = fixedFee;
    }

    @Override
    public String zone() {
        return zone;
    }

    @Override
    public BigDecimal price(BigDecimal weightKg) {
        return ratePerKg.multiply(weightKg).add(fixedFee);
    }
}
