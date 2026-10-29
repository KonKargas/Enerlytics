
package com.kkargas.enerlytics.etl;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class MeasurementExtractor {

    public static final String HEADER =
            "Date;Time;Global_active_power;Global_reactive_power;"
            + "Voltage;Global_intensity;Sub_metering_1;"
            + "Sub_metering_2;Sub_metering_3";

    @FunctionalInterface
    public interface RowConsumer {
        void accept(long lineNumber, String line)
                throws IOException;
    }

    public void extract(
            Path source,
            RowConsumer consumer
    ) throws IOException {

        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(consumer, "consumer");

        if (!Files.isRegularFile(source)) {
            throw new IOException(
                    "Dataset not found: " + source
            );
        }

        try (BufferedReader reader = Files.newBufferedReader(
                source, StandardCharsets.UTF_8)) {

            String header = reader.readLine();

            if (header == null
                    || !HEADER.equals(header.trim())) {
                throw new IOException(
                        "Invalid or missing dataset header"
                );
            }

            long lineNumber = 1;
            String line;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                consumer.accept(lineNumber, line);
            }
        }
    }
}
