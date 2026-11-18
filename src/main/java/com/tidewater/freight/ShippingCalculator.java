package com.tidewater.freight;

public class ShippingCalculator {

    private final ZoneRuleRepository zoneRules;
    private final HeavyItemSurcharge heavyItemSurcharge;

    public ShippingCalculator(ZoneRuleRepository zoneRules, HeavyItemSurcharge heavyItemSurcharge) {
        this.zoneRules = zoneRules;
        this.heavyItemSurcharge = heavyItemSurcharge;
    }

    public double calculateCost(String zone, double weightKg, String serviceLevel) {
            double cost = zoneRules.findRule(zone).price(weightKg);

            // express costs more
            if (serviceLevel.equals("EXPRESS")) {
                cost = cost * 1.5;
            }

            return cost;
    }
}
