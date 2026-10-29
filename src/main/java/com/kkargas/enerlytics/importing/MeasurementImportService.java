package com.kkargas.enerlytics.importing;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.kkargas.enerlytics.model.MeasurementRecord;
import com.kkargas.enerlytics.model.Dataset;
import com.kkargas.enerlytics.model.ImportJob;
import com.kkargas.enerlytics.repository.DatasetRepository;
import com.kkargas.enerlytics.repository.ImportJobRepository;

@Service
public class MeasurementImportService {

    private static final int BATCH_SIZE = 1_000;
    private static final String INSERT_SQL = """
            INSERT INTO energy_measurements (
                dataset_id, measured_at, kitchen_wh, laundry_wh, heating_wh,
                global_active_power_kw, global_reactive_power_kvar,
                global_intensity_a, voltage_v
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final MeasurementDataLoader loader;
    private final DatasetRepository datasetRepository;
    private final ImportJobRepository importJobRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public MeasurementImportService(MeasurementDataLoader loader,
                                    DatasetRepository datasetRepository,
                                    ImportJobRepository importJobRepository,
                                    JdbcTemplate jdbcTemplate,
                                    TransactionTemplate transactionTemplate) {
        this.loader = loader;
        this.datasetRepository = datasetRepository;
        this.importJobRepository = importJobRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    // Invoke explicitly (e.g. from a controller later), not at application startup.
    // Run imports sequentially in v1 to avoid concurrent writes to SQLite.
    public synchronized ImportJob importClasspathFile(String filename, String datasetName)
            throws IOException {

        if (datasetName == null || datasetName.isBlank()) {
            throw new IllegalArgumentException("Dataset name is required");
        }
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename is required");
        }
        if (datasetRepository.existsBySourceFileName(filename)) {
            throw new IllegalStateException("This source filename has already been imported: " + filename);
        }

        Dataset dataset = new Dataset();
        dataset.setName(datasetName.trim());
        dataset.setSourceFileName(filename);
        dataset = datasetRepository.saveAndFlush(dataset);

        ImportJob job = new ImportJob();
        job.setDataset(dataset);
        job.setFileName(filename);
        job.setStatus(ImportStatus.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        job = importJobRepository.saveAndFlush(job);

        final Long datasetId = dataset.getId();
        List<MeasurementRecord> batch = new ArrayList<>(BATCH_SIZE);

        try {
            MeasurementLoadResult result = loader.load(filename, measurement -> {
                batch.add(measurement);
                if (batch.size() >= BATCH_SIZE) {
                    insertBatch(datasetId, batch);
                    batch.clear();
                }
            });
            if (!batch.isEmpty()) {
                insertBatch(datasetId, batch);
                batch.clear();
            }

            dataset.setRecordCount(result.successfulRows());
            datasetRepository.saveAndFlush(dataset);

            job.setTotalRows(result.totalRows());
            job.setImportedRows(result.successfulRows());
            job.setSkippedRows(result.rejectedRows());
            job.setStatus(ImportStatus.COMPLETED);
            job.setFinishedAt(LocalDateTime.now());
            return importJobRepository.saveAndFlush(job);

        } catch (IOException | RuntimeException ex) {
            // Previous committed batches must not leave a half-imported dataset.
            // Cleanup is a best-effort operation: if it fails, preserve the original error.
            try {
                jdbcTemplate.update("DELETE FROM energy_measurements WHERE dataset_id = ?", datasetId);
                job.setDataset(null);
                importJobRepository.saveAndFlush(job);
                datasetRepository.deleteById(datasetId);
                datasetRepository.flush();
            } catch (RuntimeException cleanupError) {
                ex.addSuppressed(cleanupError);
            }
            job.setStatus(ImportStatus.FAILED);
            job.setErrorMessage(ex.getMessage() == null ? ex.getClass().getSimpleName()
                    : ex.getMessage().substring(0, Math.min(ex.getMessage().length(), 1000)));
            job.setFinishedAt(LocalDateTime.now());
            importJobRepository.saveAndFlush(job);
            throw ex;
        }
    }

    private void insertBatch(Long datasetId, List<MeasurementRecord> measurements) {
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.batchUpdate(
                INSERT_SQL,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        MeasurementRecord r = measurements.get(i);
                        ps.setLong(1, datasetId);
                        ps.setTimestamp(2, Timestamp.valueOf(r.timestamp()));
                        ps.setDouble(3, r.kitchen());
                        ps.setDouble(4, r.laundry());
                        ps.setDouble(5, r.heating());
                        ps.setDouble(6, r.globalActivePower());
                        ps.setDouble(7, r.globalReactivePower());
                        ps.setDouble(8, r.globalIntensity());
                        ps.setDouble(9, r.voltage());
                    }
                    @Override
                    public int getBatchSize() {
                        return measurements.size();
                    }
                }
        ));
    }
}
