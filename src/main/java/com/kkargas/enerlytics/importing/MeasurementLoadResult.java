
package com.kkargas.enerlytics.importing;

public record MeasurementLoadResult(
        long totalRows,
        long successfulRows,
        long rejectedRows
) {

    public boolean hasErrors() {
        return rejectedRows > 0;
    }
}
