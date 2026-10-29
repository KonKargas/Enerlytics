
package com.kkargas.enerlytics.etl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MeasurementCsvWriter implements AutoCloseable {

    private final BufferedWriter cleanedWriter;
    private final BufferedWriter rejectedWriter;

    public MeasurementCsvWriter(
            Path cleanedPath,
            Path rejectedPath
    ) throws IOException {

        cleanedWriter = Files.newBufferedWriter(
                cleanedPath, StandardCharsets.UTF_8
        );

        try {
            rejectedWriter = Files.newBufferedWriter(
                    rejectedPath, StandardCharsets.UTF_8
            );
        } catch (IOException ex) {
            cleanedWriter.close();
            throw ex;
        }

        cleanedWriter.write(MeasurementExtractor.HEADER);
        cleanedWriter.newLine();

        rejectedWriter.write(
                "LineNumber;Reason;OriginalLine"
        );
        rejectedWriter.newLine();
    }

    public void writeCleaned(String originalLine)
            throws IOException {

        cleanedWriter.write(originalLine);
        cleanedWriter.newLine();
    }

    public void writeRejected(
            long lineNumber,
            String originalLine,
            String reason
    ) throws IOException {

        rejectedWriter.write(
                lineNumber + ";"
                + escape(reason) + ";"
                + escape(originalLine)
        );

        rejectedWriter.newLine();
    }

    private String escape(String value) {

        if (value == null) {
            return "";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @Override
    public void close() throws IOException {

        try {
            cleanedWriter.close();
        } finally {
            rejectedWriter.close();
        }
    }
}
