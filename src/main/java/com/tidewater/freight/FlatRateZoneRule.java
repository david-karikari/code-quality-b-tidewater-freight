package com.tidewater.freight;

public final class FlatRateZoneRule implements ZonePricingRule {

    private final String zone;
    private final double ratePerKg;
    private final double fixedFee;

    public FlatRateZoneRule(String zone, double ratePerKg, double fixedFee) {
        this.zone = zone;
        this.ratePerKg = ratePerKg;
        this.fixedFee = fixedFee;
    }

    @Override
    public String zone() {
        return zone;
    }

    @Override
    public double price(double weightKg) {
        return ratePerKg * weightKg + fixedFee;
    }
}
