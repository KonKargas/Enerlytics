
package com.kkargas.enerlytics.service.importing;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.kkargas.enerlytics.importing.MeasurementFileParser;
import com.kkargas.enerlytics.model.MeasurementRecord;

class MeasurementFileParserTest {

    private final MeasurementFileParser parser =
            new MeasurementFileParser();

    @Test
    void shouldParseValidMeasurement() {

        String line =
                "16/12/2006;17:24:00;4.216;0.418;234.840;18.400;0.000;1.000;17.000";

        MeasurementRecord record = parser.parse(line);

        assertEquals(
                LocalDateTime.of(2006, 12, 16, 17, 24),
                record.timestamp()
        );

        assertEquals(4.216, record.globalActivePower());
        assertEquals(0.0, record.kitchen());
        assertEquals(1.0, record.laundry());
        assertEquals(17.0, record.heating());
    }

    @Test
    void shouldRejectMissingValues() {

        String line =
                "16/12/2006;17:24:00;?;0.418;234.840;18.400;0.000;1.000;17.000";

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(line)
        );
    }

    @Test
    void shouldRejectIncorrectColumnCount() {

        String line = "16/12/2006;17:24:00;4.216";

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(line)
        );
    }

    @Test
    void shouldRejectNegativeConsumption() {

        String line =
                "16/12/2006;17:24:00;4.216;0.418;234.840;18.400;-1.000;1.000;17.000";

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(line)
        );
    }
    
    @Test
    void shouldParseDateWithoutLeadingZeros() {

        String line =
                "1/1/2007;00:00:00;2.580;0.136;241.970;10.600;0.000;0.000;0.000";

        MeasurementRecord record = parser.parse(line);

        assertEquals(
                LocalDateTime.of(2007, 1, 1, 0, 0),
                record.timestamp()
        );
    }
}
