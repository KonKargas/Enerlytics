
package com.kkargas.enerlytics.model;

import java.time.LocalDateTime;

public record MeasurementRecord(
        LocalDateTime timestamp,
        double globalActivePower,
        double globalReactivePower,
        double voltage,
        double globalIntensity,
        double kitchen,
        double laundry,
        double heating
) {

    public double getTotalSubMetering() {
        return kitchen + laundry + heating;
    }

    public boolean isValid() {
        return timestamp != null
                && Double.isFinite(globalActivePower)
                && Double.isFinite(globalReactivePower)
                && Double.isFinite(voltage)
                && Double.isFinite(globalIntensity)
                && Double.isFinite(kitchen)
                && Double.isFinite(laundry)
                && Double.isFinite(heating)
                && globalActivePower >= 0
                && globalReactivePower >= 0
                && voltage > 0
                && globalIntensity >= 0
                && kitchen >= 0
                && laundry >= 0
                && heating >= 0;
    }
}
