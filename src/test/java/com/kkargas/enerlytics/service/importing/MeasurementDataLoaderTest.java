
package com.kkargas.enerlytics.service.importing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.kkargas.enerlytics.importing.MeasurementDataLoader;
import com.kkargas.enerlytics.importing.MeasurementLoadResult;

class MeasurementDataLoaderTest {

    private final MeasurementDataLoader loader =
            new MeasurementDataLoader();

    @Test
    void shouldLoadMeasurements() throws IOException {

        AtomicLong processed = new AtomicLong();

        MeasurementLoadResult result = loader.load(
                "household_power_consumption.txt",
                record -> processed.incrementAndGet()
        );

        assertTrue(result.totalRows() > 0);

        assertEquals(
                result.successfulRows(),
                processed.get()
        );

        assertEquals(
                result.totalRows(),
                result.successfulRows() + result.rejectedRows()
        );

        System.out.println("=== ENERLYTICS DATASET ===");
        System.out.println("Total rows: " + result.totalRows());
        System.out.println("Valid rows: " + result.successfulRows());
        System.out.println("Rejected rows: " + result.rejectedRows());
    }
}
