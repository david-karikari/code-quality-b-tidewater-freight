package com.tidewater.freight.model;

import com.tidewater.freight.exception.InvalidShipmentException;
import com.tidewater.freight.exception.InvalidWeightException;

import java.math.BigDecimal;

/**
 * What the customer wants shipped. A Shipment cannot be created with a missing
 * zone, a missing service level, or a weight that is missing, zero or negative.
 *
 * @param zone         destination zone code, for example "DOMESTIC"
 * @param weightKg     weight in kilograms; must be greater than zero
 * @param serviceLevel delivery speed
 * @throws InvalidShipmentException if the zone or service level is missing
 * @throws InvalidWeightException   if the weight is missing, zero or negative
 */
public record Shipment(String zone, BigDecimal weightKg, ServiceLevel serviceLevel) {

    public Shipment {
        if (zone == null || zone.isBlank()) {
            throw new InvalidShipmentException(
                    "Zone is required (weight " + weightKg + ", service level " + serviceLevel + ")");
        }
        if (weightKg == null || weightKg.signum() <= 0) {
            throw new InvalidWeightException(zone, weightKg);
        }
        if (serviceLevel == null) {
            throw new InvalidShipmentException(
                    "Service level is required (zone " + zone + ", weight " + weightKg + ")");
        }
    }
}
