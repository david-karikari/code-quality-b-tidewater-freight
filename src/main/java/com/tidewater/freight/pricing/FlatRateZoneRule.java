package com.tidewater.freight.pricing;

import java.math.BigDecimal;
import java.util.Objects;

/** A zone priced as a rate per kilogram plus a fixed fee (zero if the zone has none). */
public final class FlatRateZoneRule implements ZonePricingRule {

    private final String zone;
    private final BigDecimal ratePerKg;
    private final BigDecimal fixedFee;

    public FlatRateZoneRule(String zone, BigDecimal ratePerKg, BigDecimal fixedFee) {
        this.zone = Objects.requireNonNull(zone, "zone must not be null");
        this.ratePerKg = requireNotNegative(ratePerKg, "ratePerKg", zone);
        this.fixedFee = requireNotNegative(fixedFee, "fixedFee", zone);
    }

    private static BigDecimal requireNotNegative(BigDecimal value, String name, String zone) {
        Objects.requireNonNull(value, name + " must not be null (zone " + zone + ")");
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative for zone " + zone + " but was " + value);
        }
        return value;
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
