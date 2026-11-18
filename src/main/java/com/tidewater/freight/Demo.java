package com.tidewater.freight;

public class Demo {
    public static void main(String[] args) {
        ShippingCalculator calculator = new ShippingCalculator(InMemoryZoneRuleRepository.withStandardRules(), new HeavyItemSurcharge());

        System.out.println("Result DOMESTIC 10kg STANDARD       -> "
                + calculator.calculateCost("DOMESTIC", 10, ServiceLevel.STANDARD));
        System.out.println("Result INTERNATIONAL 40kg EXPRESS   -> "
                + calculator.calculateCost("INTERNATIONAL", 40, ServiceLevel.EXPRESS));

        try {
            System.out.println("Result unknown zone MOON 10kg STD   -> "
                    + calculator.calculateCost("MOON", 10, ServiceLevel.STANDARD));
        } catch (IllegalArgumentException e) {
            System.out.println("Result unknown zone MOON 10kg STD   -> rejected: " + e.getMessage());
        }
    }
}
