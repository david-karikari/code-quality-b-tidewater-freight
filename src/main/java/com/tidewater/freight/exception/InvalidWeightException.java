package com.tidewater.freight.exception;

import java.math.BigDecimal;

/** The shipment weight is missing, zero or negative. */
public class InvalidWeightException extends ShippingException {

    private final String zone;
    private final BigDecimal weightKg;

    public InvalidWeightException(String zone, BigDecimal weightKg) {
        super("Weight must be greater than zero for zone " + zone + " but was " + weightKg);
        this.zone = zone;
        this.weightKg = weightKg;
    }

    public String getZone() {
        return zone;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }
}
