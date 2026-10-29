package com.kkargas.enerlytics.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.kkargas.enerlytics.importing.ImportStatus;
import com.kkargas.enerlytics.importing.MeasurementFileParser;
import com.kkargas.enerlytics.model.Dataset;
import com.kkargas.enerlytics.model.ImportJob;
import com.kkargas.enerlytics.model.MeasurementRecord;
import com.kkargas.enerlytics.repository.DatasetRepository;
import com.kkargas.enerlytics.repository.ImportJobRepository;

@Service
public class WebDatasetImportService {
    private static final int BATCH_SIZE = 1000;
    private static final long MAX_SIZE = 250L * 1024 * 1024;
    private static final String HEADER = "Date;Time;Global_active_power;Global_reactive_power;Voltage;Global_intensity;Sub_metering_1;Sub_metering_2;Sub_metering_3";
    private static final String INSERT = """
        INSERT INTO energy_measurements (
            dataset_id, measured_at, kitchen_wh, laundry_wh, heating_wh,
            global_active_power_kw, global_reactive_power_kvar,
            global_intensity_a, voltage_v
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,254}");

    private final DatasetRepository datasets;
    private final ImportJobRepository jobs;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final MeasurementFileParser parser = new MeasurementFileParser();

    public WebDatasetImportService(DatasetRepository datasets, ImportJobRepository jobs,
                                   JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.datasets = datasets;
        this.jobs = jobs;
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    // SQLite is single-writer; serialize imports within this JVM.
    public synchronized ImportJob importFile(String requestedName, MultipartFile file) throws IOException {
        if (requestedName == null || requestedName.isBlank() || requestedName.length() > 150)
            throw new IllegalArgumentException("Dataset name is required (maximum 150 characters).");
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Please choose a non-empty TXT/CSV file.");
        if (file.getSize() > MAX_SIZE)
            throw new IllegalArgumentException("File exceeds the 250 MB limit.");

        String filename = file.getOriginalFilename();
        if (filename == null || !SAFE_NAME.matcher(filename).matches() ||
                !(filename.toLowerCase(Locale.ROOT).endsWith(".txt") ||
                  filename.toLowerCase(Locale.ROOT).endsWith(".csv")))
            throw new IllegalArgumentException("Filename must be a simple .txt or .csv name using Latin letters, numbers, dot, dash or underscore.");
        if (datasets.existsBySourceFileName(filename))
            throw new IllegalArgumentException("A dataset with this source filename already exists.");

        try {
            // Entire database change either commits or rolls back.
            return transactions.execute(status -> {
                Dataset dataset = new Dataset();
                dataset.setName(requestedName.trim());
                dataset.setSourceFileName(filename);
                dataset = datasets.saveAndFlush(dataset);

                ImportJob job = new ImportJob();
                job.setDataset(dataset);
                job.setFileName(filename);
                job.setStatus(ImportStatus.PROCESSING);
                job = jobs.saveAndFlush(job);

                long total = 0, accepted = 0, rejected = 0;
                List<MeasurementRecord> batch = new ArrayList<>(BATCH_SIZE);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                    String header = reader.readLine();
                    if (header == null || !HEADER.equals(header.replace("\uFEFF", "").trim()))
                        throw new IllegalArgumentException("Unsupported header. Expected the original UCI 9-column semicolon format.");

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) continue;
                        total++;
                        try {
                            batch.add(parser.parse(line));
                            accepted++;
                        } catch (IllegalArgumentException invalidRow) {
                            rejected++;
                        }
                        if (batch.size() == BATCH_SIZE) {
                            insertBatch(dataset.getId(), batch);
                            batch.clear();
                        }
                    }
                    if (!batch.isEmpty()) insertBatch(dataset.getId(), batch);
                } catch (IOException ex) {
                    throw new ImportReadException(ex);
                }
                if (accepted == 0)
                    throw new IllegalArgumentException("No valid measurements were found. Import rolled back.");

                dataset.setRecordCount(accepted);
                datasets.save(dataset);
                job.setTotalRows(total);
                job.setImportedRows(accepted);
                job.setSkippedRows(rejected);
                job.setStatus(ImportStatus.COMPLETED);
                job.setFinishedAt(LocalDateTime.now());
                return jobs.save(job);
            });
        } catch (RuntimeException ex) {
            // Persist only the failed job, not an incomplete dataset or its measurements.
            ImportJob failure = new ImportJob();
            failure.setFileName(filename);
            failure.setStatus(ImportStatus.FAILED);
            failure.setFinishedAt(LocalDateTime.now());
            String reason = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            failure.setErrorMessage(reason.substring(0, Math.min(reason.length(), 1000)));
            try { jobs.saveAndFlush(failure); } catch (RuntimeException logError) { ex.addSuppressed(logError); }
            if (ex instanceof ImportReadException && ex.getCause() instanceof IOException io) throw io;
            throw ex;
        }
    }

    private void insertBatch(Long datasetId, List<MeasurementRecord> batch) {
        jdbc.batchUpdate(INSERT, new BatchPreparedStatementSetter() {
            @Override public int getBatchSize() { return batch.size(); }
            @Override public void setValues(PreparedStatement ps, int index) throws SQLException {
                MeasurementRecord r = batch.get(index);
                ps.setLong(1, datasetId);
                ps.setLong(2, r.timestamp().toInstant(ZoneOffset.UTC).toEpochMilli());
                ps.setDouble(3, r.kitchen());
                ps.setDouble(4, r.laundry());
                ps.setDouble(5, r.heating());
                ps.setDouble(6, r.globalActivePower());
                ps.setDouble(7, r.globalReactivePower());
                ps.setDouble(8, r.globalIntensity());
                ps.setDouble(9, r.voltage());
            }
        });
    }
    private static class ImportReadException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        ImportReadException(IOException cause) { super(cause); }
    }
}
