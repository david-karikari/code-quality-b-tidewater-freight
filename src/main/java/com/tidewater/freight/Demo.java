package com.tidewater.freight;

import java.math.BigDecimal;

public class Demo {

    public static void main(String[] args) {
        ShippingCalculator calculator = new ShippingCalculator(
                InMemoryZoneRuleRepository.withStandardRules(), new HeavyItemSurcharge());

        run(calculator, "DOMESTIC", "10", ServiceLevel.STANDARD);
        run(calculator, "INTERNATIONAL", "40", ServiceLevel.EXPRESS);
        run(calculator, "MOON", "10", ServiceLevel.STANDARD);
        run(calculator, "DOMESTIC", "-5", ServiceLevel.STANDARD);
    }

    /** The single place where a shipping failure is caught: it is reported, never hidden. */
    private static void run(ShippingCalculator calculator, String zone, String weightKg, ServiceLevel level) {
        String label = zone + " " + weightKg + "kg " + level;
        try {
            Shipment shipment = new Shipment(zone, new BigDecimal(weightKg), level);
            System.out.println(label + " -> GHS " + calculator.calculateCost(shipment));
        } catch (ShippingException e) {
            System.out.println(label + " -> rejected: " + e.getMessage());
        }
    }
}