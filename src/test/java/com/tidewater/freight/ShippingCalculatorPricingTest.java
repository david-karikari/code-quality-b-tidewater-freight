package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ShippingCalculatorPricingTest {

    private final ShippingCalculator calculator = new ShippingCalculator(
            InMemoryZoneRuleRepository.withStandardRules(), new HeavyItemSurcharge());

    @ParameterizedTest(name = "{0}, {1} kg, {2} -> GHS {3}")
    @CsvSource({
            "DOMESTIC,      10,    STANDARD, 50.00",     // 5 x 10
            "DOMESTIC,      10,    EXPRESS,  75.00",     // 50 x 1.5
            "REGIONAL,      5,     STANDARD, 50.00",     // 8 x 5 + 10
            "REGIONAL,      5,     EXPRESS,  75.00",
            "INTERNATIONAL, 10,    STANDARD, 175.00",    // 15 x 10 + 25
            "INTERNATIONAL, 40,    EXPRESS,  967.50",    // (600 + 25 + 20) x 1.5
            "DOMESTIC,      30,    STANDARD, 150.00",    // exactly 30 kg: no surcharge
            "DOMESTIC,      30.01, STANDARD, 170.05",    // just over: 150.05 + 20
            "REGIONAL,      31,    STANDARD, 278.00",    // 248 + 10 fee + 20 surcharge
            "DOMESTIC,      7.77,  EXPRESS,  58.28",     // 58.275 rounds half up
            "DOMESTIC,      29.99, EXPRESS,  224.93",    // 224.925 rounds half up
            "REGIONAL,      0.1,   EXPRESS,  16.20",     // no floating-point residue
            "INTERNATIONAL, 99.99, EXPRESS,  2317.28"    // 2317.275 rounds half up
    })
    void pricesEachShipmentExactly(String zone, String weightKg, ServiceLevel level, String expected) {
        BigDecimal cost = calculator.calculateCost(new Shipment(zone, new BigDecimal(weightKg), level));

        assertEquals(new BigDecimal(expected), cost);
    }

    @Test
    void aNewZoneWithItsOwnRulesIsPricedWithoutChangingTheCalculator() {
        ShippingCalculator withArctic = calculatorWithArcticZone();

        // 5 kg x 20 + 3 kg x 12 + 40 fee = 176
        assertEquals(new BigDecimal("176.00"),
                withArctic.calculateCost(new Shipment("ARCTIC", new BigDecimal("8"), ServiceLevel.STANDARD)));
        assertEquals(new BigDecimal("264.00"),
                withArctic.calculateCost(new Shipment("ARCTIC", new BigDecimal("8"), ServiceLevel.EXPRESS)));
        // 100 + 35 x 12 + 40 = 560, plus the heavy-item surcharge of 20
        assertEquals(new BigDecimal("580.00"),
                withArctic.calculateCost(new Shipment("ARCTIC", new BigDecimal("40"), ServiceLevel.STANDARD)));
    }

    @Test
    void existingZonesArePricedTheSameAfterANewZoneIsAdded() {
        ShippingCalculator withArctic = calculatorWithArcticZone();

        assertEquals(new BigDecimal("50.00"),
                withArctic.calculateCost(new Shipment("DOMESTIC", new BigDecimal("10"), ServiceLevel.STANDARD)));
    }

    private static ShippingCalculator calculatorWithArcticZone() {
        InMemoryZoneRuleRepository repository = new InMemoryZoneRuleRepository(List.of(
                new FlatRateZoneRule("DOMESTIC", new BigDecimal("5"), BigDecimal.ZERO),
                new TieredArcticRule()));
        return new ShippingCalculator(repository, new HeavyItemSurcharge());
    }

    /** Illustrative new zone with different logic: 20 per kg for the first 5 kg, 12 per kg after, plus a 40 fee. */
    private static final class TieredArcticRule implements ZonePricingRule {

        private static final BigDecimal TIER_LIMIT_KG = new BigDecimal("5");

        @Override
        public String zone() {
            return "ARCTIC";
        }

        @Override
        public BigDecimal price(BigDecimal weightKg) {
            BigDecimal firstTier = weightKg.min(TIER_LIMIT_KG);
            BigDecimal secondTier = weightKg.subtract(firstTier);
            return firstTier.multiply(new BigDecimal("20"))
                    .add(secondTier.multiply(new BigDecimal("12")))
                    .add(new BigDecimal("40"));
        }
    }
}