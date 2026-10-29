
package com.kkargas.enerlytics.etl;

import org.springframework.stereotype.Component;

import com.kkargas.enerlytics.importing.MeasurementFileParser;
import com.kkargas.enerlytics.model.MeasurementRecord;

@Component
public class MeasurementCleaner {

    private final MeasurementFileParser parser =
            new MeasurementFileParser();

    public CleaningResult clean(String line) {

        try {

            MeasurementRecord measurement = parser.parse(line);

            return CleaningResult.accepted(measurement);

        } catch (IllegalArgumentException ex) {

            return CleaningResult.rejected(
                    ex.getMessage() == null
                            ? "Invalid measurement"
                            : ex.getMessage()
            );
        }
    }
}
