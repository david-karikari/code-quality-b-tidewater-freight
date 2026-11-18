package com.tidewater.freight;

public enum ServiceLevel {

    STANDARD(1.0),
    EXPRESS(1.5);

    private final double multiplier;

    ServiceLevel(double multiplier) {
        this.multiplier = multiplier;
    }

    public double apply(double cost) {
        return cost * multiplier;
    }
}
