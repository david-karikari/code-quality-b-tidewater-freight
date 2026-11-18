package com.tidewater.freight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;


/**
 * Works out the cost of a shipment in GHS.
 *
 * <p>Order of operations: zone price, plus heavy-item surcharge, then the
 * service-level multiplier. Only the final total is rounded, to two decimal
 * places, half up.
 *
 * <p>Failures are never swallowed: an unknown zone raises
 * {@link UnknownZoneException}. Invalid input cannot reach this class because
 * {@link Shipment} rejects it on creation.
 */

public class ShippingCalculator {

    private static final int CURRENCY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final ZoneRuleRepository zoneRules;
    private final HeavyItemSurcharge heavyItemSurcharge;

    public ShippingCalculator(ZoneRuleRepository zoneRules, HeavyItemSurcharge heavyItemSurcharge) {
        this.zoneRules = Objects.requireNonNull(zoneRules, "zoneRules must not be null");
        this.heavyItemSurcharge = Objects.requireNonNull(heavyItemSurcharge, "heavyItemSurcharge must not be null");
    }

    /** @throws UnknownZoneException if no pricing rule exists for the shipment's zone */
    public BigDecimal calculateCost(Shipment shipment) {
        Objects.requireNonNull(shipment, "shipment must not be null");
        BigDecimal zonePrice = zoneRules.findRule(shipment.zone()).price(shipment.weightKg());
        BigDecimal withSurcharge = zonePrice.add(heavyItemSurcharge.forWeight(shipment.weightKg()));
        BigDecimal total = shipment.serviceLevel().apply(withSurcharge);
        return total.setScale(CURRENCY_SCALE, ROUNDING);
    }
}
