package com.tidewater.freight.exception;

/** A required part of the shipment (zone or service level) is missing. */
public class InvalidShipmentException extends ShippingException {

    public InvalidShipmentException(String message) {
        super(message);
    }
}
