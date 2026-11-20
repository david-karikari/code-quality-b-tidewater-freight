package com.tidewater.freight.exception;

/** No pricing rule exists for the requested zone. */
public class UnknownZoneException extends ShippingException {

    private final String zone;

    public UnknownZoneException(String zone) {
        super("No pricing rule for zone: " + zone);
        this.zone = zone;
    }

    public String getZone() {
        return zone;
    }
}
