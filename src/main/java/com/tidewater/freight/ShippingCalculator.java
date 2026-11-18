package com.tidewater.freight;

public class ShippingCalculator {

    private final RateRepository repository;
    private final HeavyItemSurcharge heavyItemSurcharge;

    public ShippingCalculator(RateRepository repository, HeavyItemSurcharge heavyItemSurcharge) {
        this.repository = repository;
        this.heavyItemSurcharge = heavyItemSurcharge;
    }

    public double calculateCost(String zone, double weightKg, String serviceLevel) {
            double rate = repository.findRate(zone);
            double cost = rate * weightKg;

            // zone-specific fees
            if (zone.equals("DOMESTIC")) {
                // no extra fee
            } else if (zone.equals("REGIONAL")) {
                cost = cost + 10; // handling fee
            } else if (zone.equals("INTERNATIONAL")) {
                cost = cost + 25; // customs handling
            }

            // heavy-item surcharge
            cost = cost + heavyItemSurcharge.forWeight(weightKg);

            // express costs more
            if (serviceLevel.equals("EXPRESS")) {
                cost = cost * 1.5;
            }

            return cost;
    }
}
