package com.kkargas.enerlytics.etl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;

@Service
public class EtlService {

    private final MeasurementExtractor extractor;
    private final MeasurementCleaner cleaner;

    public EtlService(
            MeasurementExtractor extractor,
            MeasurementCleaner cleaner
    ) {
        this.extractor = extractor;
        this.cleaner = cleaner;
    }

    public EtlResult execute(Path dataDirectory)
            throws IOException {

        Path directory =
                dataDirectory.toAbsolutePath().normalize();

        Path original = directory.resolve(
                "household_power_consumption.txt"
        );

        Path cleaned = directory.resolve(
                "household_power_cleaned.csv"
        );

        Path rejected = directory.resolve(
                "household_power_rejected.csv"
        );

        if (!Files.isRegularFile(original)) {
            throw new IOException(
                    "Original dataset not found: " + original
            );
        }

        Path tempCleaned = Files.createTempFile(
                directory, "enerlytics-cleaned-", ".tmp"
        );

        Path tempRejected = null;

        try {
            tempRejected = Files.createTempFile(
                    directory, "enerlytics-rejected-", ".tmp"
            );

            long[] counters = new long[3];

            try (MeasurementCsvWriter writer =
                    new MeasurementCsvWriter(
                            tempCleaned, tempRejected
                    )) {

                extractor.extract(original, (number, line) -> {

                    counters[0]++;

                    CleaningResult result = cleaner.clean(line);

                    if (result.isValid()) {

                        writer.writeCleaned(line);
                        counters[1]++;

                    } else {

                        writer.writeRejected(
                                number,
                                line,
                                result.rejectionReason()
                        );

                        counters[2]++;
                    }
                });
            }

            Files.move(
                    tempCleaned,
                    cleaned,
                    StandardCopyOption.REPLACE_EXISTING
            );

            Files.move(
                    tempRejected,
                    rejected,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return new EtlResult(
                    counters[0],
                    counters[1],
                    counters[2],
                    cleaned,
                    rejected
            );

        } finally {

            Files.deleteIfExists(tempCleaned);

            if (tempRejected != null) {
                Files.deleteIfExists(tempRejected);
            }
        }
    }
}
