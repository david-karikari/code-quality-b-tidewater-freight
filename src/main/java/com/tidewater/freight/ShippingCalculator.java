package com.tidewater.freight;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ShippingCalculator {

    private static final int CURRENCY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final ZoneRuleRepository zoneRules;
    private final HeavyItemSurcharge heavyItemSurcharge;

    public ShippingCalculator(ZoneRuleRepository zoneRules, HeavyItemSurcharge heavyItemSurcharge) {
        this.zoneRules = zoneRules;
        this.heavyItemSurcharge = heavyItemSurcharge;
    }

    public BigDecimal calculateCost(String zone, BigDecimal weightKg, ServiceLevel serviceLevel) {
        BigDecimal zonePrice = zoneRules.findRule(zone).price(weightKg);
        BigDecimal withSurcharge = zonePrice.add(heavyItemSurcharge.forWeight(weightKg));
        BigDecimal total = serviceLevel.apply(withSurcharge);
        return total.setScale(CURRENCY_SCALE, ROUNDING);
    }
}
