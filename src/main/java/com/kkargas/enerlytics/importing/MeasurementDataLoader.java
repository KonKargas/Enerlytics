package com.kkargas.enerlytics.importing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.kkargas.enerlytics.model.MeasurementRecord;

@Service
public class MeasurementDataLoader {

    private final MeasurementFileParser parser = new MeasurementFileParser();

    public MeasurementLoadResult load(String filename, Consumer<MeasurementRecord> consumer)
            throws IOException {

        Objects.requireNonNull(consumer, "consumer");
        if (filename == null || !filename.matches("[a-zA-Z0-9._-]+")
                || filename.equals(".") || filename.equals("..")) {
            throw new IllegalArgumentException("Invalid filename");
        }

        ClassPathResource resource = new ClassPathResource("data/" + filename);
        if (!resource.exists()) {
            throw new IOException("Dataset not found: " + filename);
        }

        long total = 0;
        long valid = 0;
        long rejected = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null || !header.startsWith("Date;Time;")) {
                throw new IOException("Unexpected or missing dataset header");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                MeasurementRecord record;
                try {
                    record = parser.parse(line);
                } catch (IllegalArgumentException invalidRow) {
                    rejected++;
                    continue;
                }
                // Database failures must NOT be classified as invalid source rows.
                consumer.accept(record);
                valid++;
            }
        }
        return new MeasurementLoadResult(total, valid, rejected);
    }
}
