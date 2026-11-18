package com.tidewater.freight;


/**
 * Base type for every failure the shipping service reports on purpose.
 * Unchecked, so callers can catch this one type at the edge of the program.
 */

public class ShippingException extends RuntimeException {

    public ShippingException(String message) {
        super(message);
    }
}
