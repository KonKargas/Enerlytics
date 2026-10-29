package com.kkargas.enerlytics.analytics;

public record AggregationResult(

        String period,
        long measurementCount,
        double kitchen,
        double laundry,
        double heating

) {

    public double totalSubMetering() {
        return kitchen + laundry + heating;
    }
}
