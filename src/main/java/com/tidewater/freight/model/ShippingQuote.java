package com.tidewater.freight.model;

import java.math.BigDecimal;

/**
 * The price of a shipment together with the steps that produced it, so a
 * charge can be explained and checked.
 *
 * <p>The components are exact and are not rounded, so they can carry more than
 * two decimal places. Only {@code total} is rounded, to two decimal places.
 *
 * @param zonePrice          rate per kg times weight, plus the zone's fixed fee
 * @param heavyItemSurcharge surcharge added after the zone price, zero if none applies
 * @param serviceLevel       the multiplier applied to zone price plus surcharge
 * @param total              (zonePrice + heavyItemSurcharge) x the service-level multiplier, rounded
 */
public record ShippingQuote(
        BigDecimal zonePrice,
        BigDecimal heavyItemSurcharge,
        ServiceLevel serviceLevel,
        BigDecimal total) {
}