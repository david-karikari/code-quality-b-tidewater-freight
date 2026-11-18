package com.tidewater.freight;

public final class HeavyItemSurcharge {

    public static final double THRESHOLD_KG = 30;
    public static final double AMOUNT = 20;

    public double forWeight(double weightKg) {
        return weightKg > THRESHOLD_KG ? AMOUNT : 0;
    }
}
