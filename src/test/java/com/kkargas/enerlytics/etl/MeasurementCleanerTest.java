
package com.kkargas.enerlytics.etl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MeasurementCleanerTest {

    private final MeasurementCleaner cleaner =
            new MeasurementCleaner();

    @Test
    void shouldAcceptValidMeasurement() {

        String line =
                "1/1/2007;00:00:00;2.580;0.136;"
                + "241.970;10.600;0.000;0.000;0.000";

        CleaningResult result = cleaner.clean(line);

        assertTrue(result.isValid());
        assertNotNull(result.measurement());
        assertNull(result.rejectionReason());
    }

    @Test
    void shouldRejectMissingValues() {

        String line =
                "21/12/2006;11:23:00;?;?;?;?;?;?;?";

        CleaningResult result = cleaner.clean(line);

        assertFalse(result.isValid());
        assertNull(result.measurement());
        assertNotNull(result.rejectionReason());
    }

    @Test
    void shouldRejectInvalidDate() {

        String line =
                "31/2/2007;00:00:00;2.580;0.136;"
                + "241.970;10.600;0.000;0.000;0.000";

        CleaningResult result = cleaner.clean(line);

        assertFalse(result.isValid());
    }
}
