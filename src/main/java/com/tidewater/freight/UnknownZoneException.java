package com.tidewater.freight;

public class UnknownZoneException extends ShippingException{

    private final String zone;

    public UnknownZoneException(String zone) {
        super("No pricing rule for zone: " + zone);
        this.zone = zone;
    }

    public String getZone() {
        return zone;
    }
}
