package com.tidewater.freight.app;

import com.tidewater.freight.exception.UnknownZoneException;
import com.tidewater.freight.model.Shipment;
import com.tidewater.freight.model.ShippingQuote;
import com.tidewater.freight.pricing.HeavyItemSurcharge;
import com.tidewater.freight.repository.ZoneRuleRepository;

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

    private static final int CURRENCY_DECIMAL_PLACES = 2;
    private static final RoundingMode CURRENCY_ROUNDING_MODE = RoundingMode.HALF_UP;

    private final ZoneRuleRepository zoneRules;
    private final HeavyItemSurcharge heavyItemSurcharge;

    public ShippingCalculator(ZoneRuleRepository zoneRules, HeavyItemSurcharge heavyItemSurcharge) {
        this.zoneRules = Objects.requireNonNull(zoneRules, "zoneRules must not be null");
        this.heavyItemSurcharge = Objects.requireNonNull(heavyItemSurcharge, "heavyItemSurcharge must not be null");
    }

    /** @throws UnknownZoneException if no pricing rule exists for the shipment's zone */
    public BigDecimal calculateCost(Shipment shipment) {
        return quote(shipment).total();
    }

    /**
     * Prices the shipment and returns each step of the calculation with the total.
     *
     * @throws UnknownZoneException if no pricing rule exists for the shipment's zone
     */
    public ShippingQuote quote(Shipment shipment) {
        Objects.requireNonNull(shipment, "shipment must not be null");
        BigDecimal zonePrice = zoneRules.findRule(shipment.zone()).price(shipment.weightKg());
        BigDecimal surcharge = heavyItemSurcharge.forWeight(shipment.weightKg());
        BigDecimal total = shipment.serviceLevel().apply(zonePrice.add(surcharge))
                .setScale(CURRENCY_DECIMAL_PLACES, CURRENCY_ROUNDING_MODE);
        return new ShippingQuote(zonePrice, surcharge, shipment.serviceLevel(), total);
    }
}
