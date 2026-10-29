package com.kkargas.enerlytics.etl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EtlServiceTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldGenerateCleanedAndRejectedCsv()
            throws IOException {

        Path original = dataDirectory.resolve(
                "household_power_consumption.txt"
        );

        String content = String.join("\n",
                MeasurementExtractor.HEADER,
                "16/12/2006;17:24:00;4.216;0.418;234.840;18.400;0.000;1.000;17.000",
                "21/12/2006;11:23:00;?;?;?;?;?;?;?",
                "1/1/2007;00:00:00;2.580;0.136;241.970;10.600;0.000;0.000;0.000"
        );

        Files.writeString(
                original, content, StandardCharsets.UTF_8
        );

        EtlService service = new EtlService(
                new MeasurementExtractor(),
                new MeasurementCleaner()
        );

        EtlResult result = service.execute(dataDirectory);

        assertEquals(3, result.totalRows());
        assertEquals(2, result.cleanedRows());
        assertEquals(1, result.rejectedRows());
        assertTrue(result.isConsistent());

        assertTrue(Files.exists(result.cleanedFile()));
        assertTrue(Files.exists(result.rejectedFile()));

        assertEquals(
                3,
                Files.readAllLines(result.cleanedFile()).size()
        );

        assertEquals(
                2,
                Files.readAllLines(result.rejectedFile()).size()
        );
    }
}
