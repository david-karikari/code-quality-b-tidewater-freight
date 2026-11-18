package com.tidewater.freight;

public class ShippingCalculator {

    private final RateRepository repository;

    public ShippingCalculator(RateRepository repository) {
        this.repository = repository;
    }

    public double calculateCost(String zone, double weightKg, String serviceLevel) {
        try {
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
            if (weightKg > 30) {
                cost = cost + 20;
            }

            // express costs more
            if (serviceLevel.equals("EXPRESS")) {
                cost = cost * 1.5;
            }

            System.out.println("Shipping " + zone + " (" + weightKg + "kg, " + serviceLevel + "): GHS " + cost);
            return cost;
        } catch (Exception e) {
            // could not work out a rate (for example an unknown zone) - return zero
            return 0;
        }
    }
}
