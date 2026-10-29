
package com.kkargas.enerlytics.etl;

import com.kkargas.enerlytics.model.MeasurementRecord;

public record CleaningResult(
        MeasurementRecord measurement,
        String rejectionReason
) {

    public boolean isValid() {
        return measurement != null;
    }

    public static CleaningResult accepted(
            MeasurementRecord measurement
    ) {
        return new CleaningResult(measurement, null);
    }

    public static CleaningResult rejected(
            String reason
    ) {
        return new CleaningResult(null, reason);
    }
}
