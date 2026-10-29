
package com.kkargas.enerlytics.etl;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.kkargas.enerlytics.importing.ImportStatus;
import com.kkargas.enerlytics.importing.MeasurementFileParser;
import com.kkargas.enerlytics.model.Dataset;
import com.kkargas.enerlytics.model.ImportJob;
import com.kkargas.enerlytics.model.MeasurementRecord;
import com.kkargas.enerlytics.repository.DatasetRepository;
import com.kkargas.enerlytics.repository.ImportJobRepository;

@Service
public class MeasurementBatchLoader {

    private static final int BATCH_SIZE = 1000;

    private static final String INSERT_SQL = """
            INSERT INTO energy_measurements (
                dataset_id,
                measured_at,
                kitchen_wh,
                laundry_wh,
                heating_wh,
                global_active_power_kw,
                global_reactive_power_kvar,
                global_intensity_a,
                voltage_v
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final DatasetRepository datasetRepository;
    private final ImportJobRepository importJobRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    private final MeasurementFileParser parser =
            new MeasurementFileParser();

    public MeasurementBatchLoader(
            DatasetRepository datasetRepository,
            ImportJobRepository importJobRepository,
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate) {

        this.datasetRepository = datasetRepository;
        this.importJobRepository = importJobRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    public synchronized ImportJob load(
            Path cleanedFile,
            String datasetName
    ) throws IOException {

        if (!Files.isRegularFile(cleanedFile)) {
            throw new IOException(
                    "Cleaned CSV not found: " + cleanedFile
            );
        }

        if (datasetName == null || datasetName.isBlank()) {
            throw new IllegalArgumentException(
                    "Dataset name is required"
            );
        }

        String filename =
                cleanedFile.getFileName().toString();

        if (datasetRepository.existsBySourceFileName(filename)) {
            throw new IllegalStateException(
                    "Dataset already imported: " + filename
            );
        }

        Dataset dataset = new Dataset();
        dataset.setName(datasetName.trim());
        dataset.setDescription(
                "Cleaned household electricity consumption dataset"
        );
        dataset.setSourceFileName(filename);
        dataset.setRecordCount(0L);

        dataset = datasetRepository.saveAndFlush(dataset);

        ImportJob job = new ImportJob();
        job.setDataset(dataset);
        job.setFileName(filename);
        job.setStatus(ImportStatus.PROCESSING);
        job.setStartedAt(LocalDateTime.now());

        job = importJobRepository.saveAndFlush(job);

        Long datasetId = dataset.getId();

        List<MeasurementRecord> batch =
                new ArrayList<>(BATCH_SIZE);

        long totalRows = 0;
        long importedRows = 0;

        try (BufferedReader reader = Files.newBufferedReader(
                cleanedFile, StandardCharsets.UTF_8)) {

            String header = reader.readLine();

            if (header == null ||
                    !MeasurementExtractor.HEADER.equals(header.trim())) {
                throw new IOException("Invalid cleaned CSV header");
            }

            String line;

            while ((line = reader.readLine()) != null) {

                totalRows++;

                MeasurementRecord record;

                try {
                    record = parser.parse(line);
                } catch (IllegalArgumentException ex) {
                    throw new IOException(
                            "Invalid cleaned record at line "
                            + (totalRows + 1), ex
                    );
                }

                batch.add(record);

                if (batch.size() >= BATCH_SIZE) {

                    insertBatch(datasetId, batch);

                    importedRows += batch.size();
                    batch.clear();

                    if (importedRows % 100_000 == 0) {
                        System.out.printf(
                                "Imported: %,d records%n",
                                importedRows
                        );
                    }
                }
            }

            if (!batch.isEmpty()) {
                insertBatch(datasetId, batch);
                importedRows += batch.size();
                batch.clear();
            }

            dataset.setRecordCount(importedRows);
            datasetRepository.saveAndFlush(dataset);

            job.setTotalRows(totalRows);
            job.setImportedRows(importedRows);
            job.setSkippedRows(0L);
            job.setStatus(ImportStatus.COMPLETED);
            job.setFinishedAt(LocalDateTime.now());

            return importJobRepository.saveAndFlush(job);

        } catch (IOException | RuntimeException ex) {

            try {
                jdbcTemplate.update(
                        "DELETE FROM energy_measurements WHERE dataset_id = ?",
                        datasetId
                );

                job.setDataset(null);
                importJobRepository.saveAndFlush(job);

                datasetRepository.deleteById(datasetId);
                datasetRepository.flush();

            } catch (RuntimeException cleanupError) {
                ex.addSuppressed(cleanupError);
            }

            job.setStatus(ImportStatus.FAILED);
            job.setTotalRows(totalRows);
            job.setImportedRows(importedRows);
            job.setFinishedAt(LocalDateTime.now());

            String message = ex.getMessage();

            job.setErrorMessage(
                    message == null
                            ? ex.getClass().getSimpleName()
                            : message.substring(
                                    0, Math.min(message.length(), 1000)
                            )
            );

            importJobRepository.saveAndFlush(job);

            throw ex;
        }
    }

    private void insertBatch(
            Long datasetId,
            List<MeasurementRecord> records
    ) {

        transactionTemplate.executeWithoutResult(status ->

            jdbcTemplate.batchUpdate(
                INSERT_SQL,
                new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(
                            PreparedStatement ps,
                            int index
                    ) throws SQLException {

                        MeasurementRecord record =
                                records.get(index);

                        ps.setLong(1, datasetId);

                        ps.setLong(
                        	    2,
                        	    record.timestamp()
                        	          .toInstant(ZoneOffset.UTC)
                        	          .toEpochMilli()
                        	);

                        ps.setDouble(3, record.kitchen());
                        ps.setDouble(4, record.laundry());
                        ps.setDouble(5, record.heating());

                        ps.setDouble(
                                6, record.globalActivePower()
                        );

                        ps.setDouble(
                                7, record.globalReactivePower()
                        );

                        ps.setDouble(
                                8, record.globalIntensity()
                        );

                        ps.setDouble(9, record.voltage());
                    }

                    @Override
                    public int getBatchSize() {
                        return records.size();
                    }
                }
            )
        );
    }
}
