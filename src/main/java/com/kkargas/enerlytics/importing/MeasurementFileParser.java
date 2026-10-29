
package com.kkargas.enerlytics.importing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

import com.kkargas.enerlytics.model.MeasurementRecord;

public class MeasurementFileParser {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d/M/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final int EXPECTED_COLUMNS = 9;

    public MeasurementRecord parse(String line) {

        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("Empty measurement line");
        }

        String[] columns = line.split(";", -1);

        if (columns.length != EXPECTED_COLUMNS) {
            throw new IllegalArgumentException(
                    "Expected 9 columns, found " + columns.length
            );
        }

        try {
            LocalDate date = LocalDate.parse(
                    columns[0].trim(), DATE_FORMAT
            );

            LocalTime time = LocalTime.parse(
                    columns[1].trim(), TIME_FORMAT
            );

            LocalDateTime timestamp = LocalDateTime.of(date, time);

            MeasurementRecord record = new MeasurementRecord(
                    timestamp,
                    parseNumber(columns[2]),
                    parseNumber(columns[3]),
                    parseNumber(columns[4]),
                    parseNumber(columns[5]),
                    parseNumber(columns[6]),
                    parseNumber(columns[7]),
                    parseNumber(columns[8])
            );

            if (!record.isValid()) {
                throw new IllegalArgumentException(
                        "Measurement contains invalid values"
                );
            }

            return record;

        } catch (DateTimeParseException | NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Invalid measurement format: " + line, ex
            );
        }
    }

    private double parseNumber(String value) {

        String normalized = value.trim();

        if (normalized.isEmpty() || normalized.equals("?")) {
            throw new NumberFormatException("Missing numeric value");
        }

        return Double.parseDouble(normalized);
    }
}
